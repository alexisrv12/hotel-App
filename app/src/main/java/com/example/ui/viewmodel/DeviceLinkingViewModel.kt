package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.HotelDatabase
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.data.repository.DeviceRepository
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.HotelFirestoreRepository
import com.example.data.repository.SessionDataStoreRepository
import com.example.utils.CodeValidationResult
import com.example.utils.DeviceCodeValidationHelper
import com.example.utils.DeviceDataStoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.utils.DeviceLinkingUtility
import com.example.utils.DeviceLinkingUtils
import com.example.utils.DeviceNotificationManager
import com.example.utils.DevicePreferences
import com.example.utils.PinValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FirebaseConnectionState {
    CONNECTED,
    DISCONNECTED,
    SYNCING
}

enum class PairingStatus {
    IDLE,
    PAIRING,
    SUCCESS,
    FAILED
}

/**
 * ViewModel managing state and operations for device linking workflow and local notifications.
 */
class DeviceLinkingViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: DeviceRepository = DeviceRepository(HotelDatabase.getDatabase(application).deviceDao())
) : AndroidViewModel(application) {

    private val linkingUtility: DeviceLinkingUtility = DeviceLinkingUtility.getInstance()

    companion object {
        const val HEARTBEAT_TIMEOUT_MS = 30000L // 30s threshold for active connectivity
    }

    private val hotelDao = HotelDatabase.getDatabase(application).hotelDao()
    private val deviceDao = HotelDatabase.getDatabase(application).deviceDao()
    private val sessionRepo = SessionDataStoreRepository(application)
    private val firestoreRepo = HotelFirestoreRepository(application, hotelDao, deviceDao, sessionRepo)

    // Real-time Firebase Connection State (Connected, Disconnected, Syncing)
    val firebaseConnectionState: StateFlow<FirebaseConnectionState> = firestoreRepo.syncInfo
        .map { syncInfo ->
            when (syncInfo.status) {
                CloudSyncStatus.ONLINE_SYNCED -> FirebaseConnectionState.CONNECTED
                CloudSyncStatus.SYNCING -> FirebaseConnectionState.SYNCING
                CloudSyncStatus.OFFLINE -> FirebaseConnectionState.DISCONNECTED
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FirebaseConnectionState.CONNECTED
        )

    // State Flow emitting list of linked devices from Room database
    val linkedDevices: StateFlow<List<DeviceEntity>> = repository.allDevices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // State Flow reflecting if each linked device is currently 'Active' or 'Disconnected' based on its last heartbeat
    val deviceRealTimeStatuses: StateFlow<Map<String, String>> = repository.allDevices
        .map { list ->
            list.associate { device ->
                val status = if (device.isCurrentlyActive(HEARTBEAT_TIMEOUT_MS)) {
                    RealTimeConnectivityStatus.ACTIVE
                } else {
                    RealTimeConnectivityStatus.DISCONNECTED
                }
                device.deviceId to status
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // State Flow emitting list of devices with updated real-time status according to last heartbeat
    val realTimeDevicesList: StateFlow<List<DeviceEntity>> = repository.allDevices
        .map { devices ->
            devices.map { device ->
                val computed = device.computedRealTimeStatus(HEARTBEAT_TIMEOUT_MS)
                device.copy(realTimeConnectivityStatus = computed)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // State Flow emitting pending access requests for manager notifications
    val pendingAccessRequests: StateFlow<List<DeviceEntity>> = repository.allDevices
        .map { devices ->
            devices.filter { it.connectionStatus == DeviceConnectionStatus.PENDING }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val codeValidator = DeviceCodeValidationHelper.getInstance()

    // Current active 6-digit secure PIN for device pairing
    private val _currentPin = MutableStateFlow(DeviceLinkingUtility.generate6DigitPin())
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    // Creation timestamp for active PIN
    private val _pinCreationTimestamp = MutableStateFlow(System.currentTimeMillis())
    val pinCreationTimestamp: StateFlow<Long> = _pinCreationTimestamp.asStateFlow()

    // Current active Base64 QR code session token
    private val _currentQrSessionToken = MutableStateFlow(linkingUtility.generateTemporarySessionQrString())
    val currentQrSessionToken: StateFlow<String> = _currentQrSessionToken.asStateFlow()

    // Creation timestamp for active QR token
    private val _qrCreationTimestamp = MutableStateFlow(System.currentTimeMillis())
    val qrCreationTimestamp: StateFlow<Long> = _qrCreationTimestamp.asStateFlow()

    // Live formatted countdowns for 2-minute window
    private val _pinCountdownText = MutableStateFlow("02:00")
    val pinCountdownText: StateFlow<String> = _pinCountdownText.asStateFlow()

    private val _qrCountdownText = MutableStateFlow("02:00")
    val qrCountdownText: StateFlow<String> = _qrCountdownText.asStateFlow()

    // Decoded payload from scanned QR code string
    private val _decodedQrSessionPayload = MutableStateFlow<String?>(null)
    val decodedQrSessionPayload: StateFlow<String?> = _decodedQrSessionPayload.asStateFlow()

    // State for PIN validation result
    private val _pinValidationResult = MutableStateFlow<PinValidationResult?>(null)
    val pinValidationResult: StateFlow<PinValidationResult?> = _pinValidationResult.asStateFlow()

    // Pairing workflow and failure states
    private val _pairingStatus = MutableStateFlow(PairingStatus.IDLE)
    val pairingStatus: StateFlow<PairingStatus> = _pairingStatus.asStateFlow()

    private val _pairingErrorMessage = MutableStateFlow<String?>(null)
    val pairingErrorMessage: StateFlow<String?> = _pairingErrorMessage.asStateFlow()

    private val _hasPairingFailed = MutableStateFlow(false)
    val hasPairingFailed: StateFlow<Boolean> = _hasPairingFailed.asStateFlow()

    // Feedback messages for UI operations
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dbPin = hotelDao.getSettingValue("active_linking_pin")
            val dbPinTs = hotelDao.getSettingValue("active_linking_pin_ts")?.toLongOrNull()
            if (!dbPin.isNullOrBlank() && dbPinTs != null && !codeValidator.isExpired(dbPinTs, now)) {
                _currentPin.value = dbPin
                _pinCreationTimestamp.value = dbPinTs
            } else {
                generateNewPin()
            }

            val dbQr = hotelDao.getSettingValue("active_linking_qr")
            val dbQrTs = hotelDao.getSettingValue("active_linking_qr_ts")?.toLongOrNull()
            if (!dbQr.isNullOrBlank() && dbQrTs != null && !codeValidator.isExpired(dbQrTs, now)) {
                _currentQrSessionToken.value = dbQr
                _qrCreationTimestamp.value = dbQrTs
            } else {
                generateNewQrToken()
            }

            // Ticker loop updating countdown strings every second
            while (isActive) {
                val currTime = System.currentTimeMillis()
                val pinRemaining = codeValidator.getRemainingMillis(_pinCreationTimestamp.value, currTime)
                if (pinRemaining <= 0L) {
                    generateNewPin()
                }
                val qrRemaining = codeValidator.getRemainingMillis(_qrCreationTimestamp.value, currTime)
                if (qrRemaining <= 0L) {
                    generateNewQrToken()
                }
                _pinCountdownText.value = codeValidator.getFormattedCountdown(_pinCreationTimestamp.value, currTime)
                _qrCountdownText.value = codeValidator.getFormattedCountdown(_qrCreationTimestamp.value, currTime)
                delay(1000L)
            }
        }
    }

    /**
     * Generates a new 6-digit secure PIN for device pairing session and persists it in DB.
     */
    fun generateNewPin(): String {
        val pin = DeviceLinkingUtility.generate6DigitPin()
        val now = System.currentTimeMillis()
        _currentPin.value = pin
        _pinCreationTimestamp.value = now
        viewModelScope.launch {
            hotelDao.insertSetting(com.example.data.entities.HotelSettingEntity("active_linking_pin", pin))
            hotelDao.insertSetting(com.example.data.entities.HotelSettingEntity("active_linking_pin_ts", now.toString()))
        }
        return pin
    }

    /**
     * Generates a new Base64 temporary QR code session string token and persists it in DB.
     */
    fun generateNewQrToken(deviceId: String = "DEV-" + (1000..9999).random()): String {
        val qrToken = linkingUtility.generateTemporarySessionQrString(deviceId)
        val now = System.currentTimeMillis()
        _currentQrSessionToken.value = qrToken
        _qrCreationTimestamp.value = now
        viewModelScope.launch {
            hotelDao.insertSetting(com.example.data.entities.HotelSettingEntity("active_linking_qr", qrToken))
            hotelDao.insertSetting(com.example.data.entities.HotelSettingEntity("active_linking_qr_ts", now.toString()))
        }
        return qrToken
    }

    /**
     * Validates input PIN code against the expected PIN or current session PIN.
     */
    fun validatePin(inputPin: String, expectedPin: String = _currentPin.value): PinValidationResult {
        val result = DeviceLinkingUtils.validatePinCode(inputPin, expectedPin)
        _pinValidationResult.value = result
        when (result) {
            is PinValidationResult.Valid -> {
                _pairingStatus.value = PairingStatus.SUCCESS
                _pairingErrorMessage.value = null
                _hasPairingFailed.value = false
            }
            is PinValidationResult.IncorrectPin -> {
                recordPairingFailure("El PIN ingresado es incorrecto. Verifique el código.")
            }
            is PinValidationResult.InvalidFormat -> {
                recordPairingFailure("Formato de PIN inválido. Ingrese entre 4 y 8 dígitos.")
            }
            is PinValidationResult.RateLimited -> {
                recordPairingFailure("Demasiados intentos fallidos. Espere ${result.remainingSeconds} segundos.")
            }
        }
        return result
    }

    /**
     * Decodes a Base64-encoded QR token string and updates state.
     */
    fun decodeQrToken(token: String): String? {
        val decoded = linkingUtility.decodeTemporarySessionQrString(token)
        _decodedQrSessionPayload.value = decoded
        if (decoded == null) {
            recordPairingFailure("El código QR escaneado no es válido o ha expirado.")
        } else {
            _pairingStatus.value = PairingStatus.SUCCESS
            _pairingErrorMessage.value = null
            _hasPairingFailed.value = false
        }
        return decoded
    }

    /**
     * Records a pairing failure state with error explanation and user notification.
     */
    fun recordPairingFailure(reason: String) {
        _pairingStatus.value = PairingStatus.FAILED
        _pairingErrorMessage.value = reason
        _hasPairingFailed.value = true
        _userMessage.value = reason
    }

    /**
     * Clears failure state without regenerating tokens.
     */
    fun clearPairingFailure() {
        _pairingStatus.value = PairingStatus.IDLE
        _pairingErrorMessage.value = null
        _hasPairingFailed.value = false
    }

    /**
     * Retries pairing by regenerating active QR code and PIN, and restarting pairing logic smoothly.
     */
    fun retryPairing() {
        _pairingStatus.value = PairingStatus.IDLE
        _pairingErrorMessage.value = null
        _hasPairingFailed.value = false
        _pinValidationResult.value = null
        _decodedQrSessionPayload.value = null
        generateNewQrToken()
        generateNewPin()
        _userMessage.value = "Nuevo código QR y PIN generados. Listo para reintentar vinculación."
    }

    /**
     * Links a new device and persists its entity in the database via repository.
     */
    fun linkDevice(
        name: String,
        userAssigned: String,
        deviceId: String = "DEV-" + System.currentTimeMillis().toString().takeLast(6),
        ipAddress: String? = null
    ) {
        viewModelScope.launch {
            val device = DeviceEntity(
                name = name,
                userAssigned = userAssigned,
                deviceId = deviceId,
                connectionStatus = DeviceConnectionStatus.CONNECTED,
                realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                lastHeartbeat = System.currentTimeMillis(),
                ipAddress = ipAddress,
                timestamp = System.currentTimeMillis()
            )
            repository.insertDevice(device)
            _userMessage.value = "Dispositivo '$name' vinculado con éxito."
            generateNewPin()
            generateNewQrToken(deviceId)
        }
    }

    /**
     * Requests access for a new device and fires a local notification to alert the Manager.
     */
    fun requestDeviceAccess(
        context: Context,
        name: String,
        userAssigned: String,
        deviceId: String = "DEV-" + System.currentTimeMillis().toString().takeLast(6),
        ipAddress: String? = null
    ) {
        viewModelScope.launch {
            val pendingDevice = DeviceEntity(
                name = name,
                userAssigned = userAssigned,
                deviceId = deviceId,
                connectionStatus = DeviceConnectionStatus.PENDING,
                realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                lastHeartbeat = System.currentTimeMillis(),
                ipAddress = ipAddress,
                timestamp = System.currentTimeMillis()
            )
            val newId = repository.insertDevice(pendingDevice)
            val created = pendingDevice.copy(id = newId)

            // Local system notification trigger
            DeviceNotificationManager.notifyNewDeviceAccessRequest(context, created)
            _userMessage.value = "Solicitud de acceso enviada para '$name'. Notificación creada para el Gerente."
        }
    }

    /**
     * Approves a pending device access request.
     */
    fun approveDeviceRequest(device: DeviceEntity) {
        viewModelScope.launch {
            val updated = device.copy(
                connectionStatus = DeviceConnectionStatus.CONNECTED,
                realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                lastHeartbeat = System.currentTimeMillis()
            )
            repository.updateDevice(updated)
            _userMessage.value = "Acceso APROBADO para '${device.name}'."
        }
    }

    /**
     * Rejects a pending device access request.
     */
    fun rejectDeviceRequest(device: DeviceEntity) {
        viewModelScope.launch {
            val updated = device.copy(
                connectionStatus = DeviceConnectionStatus.DISCONNECTED,
                realTimeConnectivityStatus = RealTimeConnectivityStatus.DISCONNECTED
            )
            repository.updateDevice(updated)
            _userMessage.value = "Solicitud RECHAZADA para '${device.name}'."
        }
    }

    /**
     * Updates connection status of a linked device.
     */
    fun updateDeviceStatus(device: DeviceEntity, newStatus: String) {
        viewModelScope.launch {
            val updated = device.copy(
                connectionStatus = newStatus,
                realTimeConnectivityStatus = if (newStatus == DeviceConnectionStatus.CONNECTED) RealTimeConnectivityStatus.ACTIVE else RealTimeConnectivityStatus.DISCONNECTED
            )
            repository.updateDevice(updated)
            _userMessage.value = "Estado del dispositivo actualizado a $newStatus."
        }
    }

    // Setup Wizard State Variables
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _generatedVerificationCode = MutableStateFlow("")
    val generatedVerificationCode: StateFlow<String> = _generatedVerificationCode.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _wizardStep = MutableStateFlow(1) // 1 = Email, 2 = Code Verification, 3 = Method Linking
    val wizardStep: StateFlow<Int> = _wizardStep.asStateFlow()

    private val _linkingMethod = MutableStateFlow("PIN") // "PIN" or "QR"
    val linkingMethod: StateFlow<String> = _linkingMethod.asStateFlow()

    fun setWizardStep(step: Int) {
        _wizardStep.value = step
    }

    fun setLinkingMethod(method: String) {
        _linkingMethod.value = method
    }

    /**
     * Paso 1: Generates and sends a 6-digit verification code to the requested email address.
     */
    fun sendVerificationCode(email: String): String {
        val code = DeviceLinkingUtility.generate6DigitPin()
        _userEmail.value = email.trim()
        _generatedVerificationCode.value = code
        _wizardStep.value = 2
        _userMessage.value = "Código de verificación ($code) enviado a $email"
        return code
    }

    /**
     * Paso 2: Validates the verification code entered by the user.
     */
    fun verifyEmailCode(inputCode: String): Boolean {
        return if (inputCode.trim() == _generatedVerificationCode.value && inputCode.isNotBlank()) {
            _isEmailVerified.value = true
            _wizardStep.value = 3
            _userMessage.value = "¡Correo verificado con éxito!"
            true
        } else {
            _userMessage.value = "Código de verificación incorrecto."
            false
        }
    }

    /**
     * Paso 3: Validates PIN and registers device as authorized, syncing with Room database and SharedPreferences.
     */
    fun completeLinkingWithPin(context: Context, inputPin: String, deviceName: String = "Dispositivo Móvil"): Boolean {
        val now = System.currentTimeMillis()
        val validationResult = codeValidator.validatePinCode(
            inputPin = inputPin,
            expectedPin = _currentPin.value,
            createdTimestampMs = _pinCreationTimestamp.value,
            currentTimeMs = now
        )

        when (validationResult) {
            is CodeValidationResult.Empty -> {
                recordPairingFailure("Ingrese el PIN de 6 dígitos.")
                return false
            }
            is CodeValidationResult.InvalidFormat -> {
                recordPairingFailure(validationResult.reason)
                return false
            }
            is CodeValidationResult.Expired -> {
                recordPairingFailure("El PIN ha expirado (válido por 2 min). Genere uno nuevo desde el panel de Gerente.")
                return false
            }
            is CodeValidationResult.Incorrect -> {
                recordPairingFailure("El PIN ingresado es incorrecto. Verifique el PIN con Gerencia.")
                return false
            }
            is CodeValidationResult.Valid -> {
                _pairingStatus.value = PairingStatus.SUCCESS
                _pairingErrorMessage.value = null
                _hasPairingFailed.value = false
            }
        }

        val deviceId = DevicePreferences.getLinkedDeviceId(context)
        val email = _userEmail.value.ifBlank { DevicePreferences.getLinkedEmail(context) ?: "usuario@hotel.com" }

        viewModelScope.launch {
            val device = DeviceEntity(
                name = deviceName,
                userAssigned = email,
                deviceId = deviceId,
                connectionStatus = DeviceConnectionStatus.CONNECTED,
                realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                lastHeartbeat = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis()
            )
            repository.insertDevice(device)
            DeviceDataStoreManager(context).saveDeviceAuthorization(deviceId, email)
            generateNewPin()
            _userMessage.value = "Dispositivo autorizado y vinculado con éxito."
        }
        return true
    }

    /**
     * Paso 3: Validates QR Code token and registers device as authorized.
     */
    fun completeLinkingWithQr(context: Context, qrToken: String, deviceName: String = "Dispositivo Móvil"): Boolean {
        val now = System.currentTimeMillis()
        val validationResult = codeValidator.validateQrToken(
            inputQrToken = qrToken,
            expectedQrToken = _currentQrSessionToken.value,
            createdTimestampMs = _qrCreationTimestamp.value,
            currentTimeMs = now
        )

        when (validationResult) {
            is CodeValidationResult.Empty -> {
                recordPairingFailure("Ingrese o escanee un código QR.")
                return false
            }
            is CodeValidationResult.Expired -> {
                recordPairingFailure("El token QR ha expirado (válido por 2 min). Genere uno nuevo en la consola.")
                return false
            }
            is CodeValidationResult.Incorrect, is CodeValidationResult.InvalidFormat -> {
                recordPairingFailure("El código QR es inválido o no corresponde al hotel.")
                return false
            }
            is CodeValidationResult.Valid -> {
                _pairingStatus.value = PairingStatus.SUCCESS
                _pairingErrorMessage.value = null
                _hasPairingFailed.value = false
            }
        }

        val deviceId = DevicePreferences.getLinkedDeviceId(context)
        val email = _userEmail.value.ifBlank { DevicePreferences.getLinkedEmail(context) ?: "usuario@hotel.com" }

        viewModelScope.launch {
            val device = DeviceEntity(
                name = deviceName,
                userAssigned = email,
                deviceId = deviceId,
                connectionStatus = DeviceConnectionStatus.CONNECTED,
                realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                lastHeartbeat = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis()
            )
            repository.insertDevice(device)
            DeviceDataStoreManager(context).saveDeviceAuthorization(deviceId, email)
            generateNewQrToken()
            _userMessage.value = "Dispositivo autorizado mediante QR con éxito."
        }
        return true
    }

    /**
     * Unlinks/Deletes a device from the database and updates preferences if it is the local device.
     */
    fun unlinkDevice(device: DeviceEntity, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteDevice(device)
            if (context != null) {
                val currentLocalId = DevicePreferences.getLinkedDeviceId(context)
                if (device.deviceId == currentLocalId) {
                    DeviceDataStoreManager(context).clearDeviceAuthorization()
                }
            }
            _userMessage.value = "Dispositivo '${device.name}' desvinculado."
        }
    }

    /**
     * Unlinks/Deletes a device by its ID and clears preferences if applicable.
     */
    fun unlinkDeviceById(id: Long, context: Context? = null) {
        viewModelScope.launch {
            val device = repository.getDeviceById(id)
            repository.deleteDeviceById(id)
            if (device != null) {
                if (context != null) {
                    val currentLocalId = DevicePreferences.getLinkedDeviceId(context)
                    if (device.deviceId == currentLocalId) {
                        DeviceDataStoreManager(context).clearDeviceAuthorization()
                    }
                }
                _userMessage.value = "Dispositivo '${device.name}' desvinculado."
            }
        }
    }

    /**
     * Deletes all linked device history records from the database and clears local preferences.
     */
    fun clearAllHistory(context: Context? = null) {
        viewModelScope.launch {
            repository.deleteAllDevices()
            if (context != null) {
                DeviceDataStoreManager(context).clearDeviceAuthorization()
            }
            _userMessage.value = "Historial de vinculaciones borrado por completo."
        }
    }

    /**
     * Records a new heartbeat timestamp for a linked device.
     */
    fun recordHeartbeat(deviceId: String) {
        viewModelScope.launch {
            val device = repository.getDeviceByDeviceId(deviceId)
            if (device != null) {
                val updated = device.copy(
                    lastHeartbeat = System.currentTimeMillis(),
                    realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                    connectionStatus = DeviceConnectionStatus.CONNECTED
                )
                repository.updateDevice(updated)
            }
        }
    }

    /**
     * Fires a local notification explicitly for a given device entity.
     */
    fun sendLocalNotificationForDevice(context: Context, device: DeviceEntity) {
        DeviceNotificationManager.notifyNewDeviceAccessRequest(context, device)
        _userMessage.value = "Notificación emitida para '${device.name}'."
    }

    /**
     * Clears current user feedback message.
     */
    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Clears PIN validation state.
     */
    fun clearPinValidationResult() {
        _pinValidationResult.value = null
    }
}
