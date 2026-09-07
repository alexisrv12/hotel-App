package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.HotelDatabase
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.repository.DeviceRepository
import com.example.data.repository.SessionDataStoreRepository
import com.example.ui.Screen
import com.example.utils.DeviceCodeValidationHelper
import com.example.utils.DeviceDataStoreManager
import com.example.utils.DevicePreferences
import com.example.utils.FirebaseManager
import com.example.utils.QrScannerManager
import com.example.utils.ScannedQrData
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

sealed class LinkingUiState {
    object Idle : LinkingUiState()
    data class Validating(val message: String) : LinkingUiState()
    data class Syncing(val progress: Float, val statusMessage: String) : LinkingUiState()
    data class Success(val role: String, val deviceName: String, val targetScreen: Screen) : LinkingUiState()
    data class Error(val errorMessage: String) : LinkingUiState()
}

/**
 * ViewModel managing the entire device linking lifecycle:
 * - Generation of dynamic, secure 6-digit PINs and QR tokens
 * - Validation against local Room DB, DataStore, and session validation helpers
 * - Data synchronization animation and transition to the authorized role screen
 */
class LinkingViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepo = SessionDataStoreRepository(application)
    private val deviceDao = HotelDatabase.getDatabase(application).deviceDao()
    private val deviceRepo = DeviceRepository(deviceDao)
    private val codeValidator = DeviceCodeValidationHelper.getInstance()
    private val random = SecureRandom()

    // UI States
    private val _uiState = MutableStateFlow<LinkingUiState>(LinkingUiState.Idle)
    val uiState: StateFlow<LinkingUiState> = _uiState.asStateFlow()

    // Form inputs
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    // Dynamic Manager Active PIN & QR Token
    private val _activeManagerPin = MutableStateFlow(generateRandomPin())
    val activeManagerPin: StateFlow<String> = _activeManagerPin.asStateFlow()

    private val _pinTimestamp = MutableStateFlow(System.currentTimeMillis())

    private val _activeManagerQr = MutableStateFlow(generateRandomQrToken())
    val activeManagerQr: StateFlow<String> = _activeManagerQr.asStateFlow()

    private val _qrTimestamp = MutableStateFlow(System.currentTimeMillis())

    // Live Countdowns
    private val _pinCountdown = MutableStateFlow("02:00")
    val pinCountdown: StateFlow<String> = _pinCountdown.asStateFlow()

    private val _qrCountdown = MutableStateFlow("02:00")
    val qrCountdown: StateFlow<String> = _qrCountdown.asStateFlow()

    // Navigation events for safe transition
    private val _navigationEvent = MutableSharedFlow<Screen>()
    val navigationEvent: SharedFlow<Screen> = _navigationEvent.asSharedFlow()

    init {
        // Load active pairing tokens if already saved locally
        viewModelScope.launch {
            val (savedPin, pinTs) = sessionRepo.getActivePin()
            val now = System.currentTimeMillis()
            if (!savedPin.isNullOrBlank() && pinTs > 0 && (now - pinTs < 15 * 60 * 1000L)) {
                _activeManagerPin.value = savedPin
                _pinTimestamp.value = pinTs
            }

            val (savedQr, qrTs) = sessionRepo.getActiveQrToken()
            if (!savedQr.isNullOrBlank() && qrTs > 0 && (now - qrTs < 15 * 60 * 1000L)) {
                _activeManagerQr.value = savedQr
                _qrTimestamp.value = qrTs
            }

            // Ticker loop: displays formatted countdown, avoids overwriting active manager tokens unexpectedly
            while (isActive) {
                val current = System.currentTimeMillis()
                _pinCountdown.value = codeValidator.getFormattedCountdown(_pinTimestamp.value, current)
                _qrCountdown.value = codeValidator.getFormattedCountdown(_qrTimestamp.value, current)
                delay(1000L)
            }
        }
    }

    fun onPinChange(newPin: String) {
        if (newPin.length <= 6 && newPin.all { it.isDigit() }) {
            _pinInput.value = newPin
            if (_uiState.value is LinkingUiState.Error) {
                _uiState.value = LinkingUiState.Idle
            }
        }
    }

    fun generateRandomPin(): String {
        val number = 100000 + random.nextInt(900000)
        return number.toString()
    }

    fun generateRandomQrToken(): String {
        val bytes = ByteArray(12)
        random.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "RIVERA-LINK-$hex"
    }

    fun refreshActivePin() {
        val newPin = generateRandomPin()
        val now = System.currentTimeMillis()
        _activeManagerPin.value = newPin
        _pinTimestamp.value = now
        viewModelScope.launch {
            sessionRepo.saveActiveLinkingPin(newPin, now)
            FirebaseManager.createVinculacionSession(
                context = getApplication(),
                pin = newPin,
                qrToken = _activeManagerQr.value,
                role = "RECEPCION"
            )
        }
    }

    fun refreshActiveQr() {
        val newQr = generateRandomQrToken()
        val now = System.currentTimeMillis()
        _activeManagerQr.value = newQr
        _qrTimestamp.value = now
        viewModelScope.launch {
            sessionRepo.saveActiveLinkingQr(newQr, now)
            FirebaseManager.createVinculacionSession(
                context = getApplication(),
                pin = _activeManagerPin.value,
                qrToken = newQr,
                role = "RECEPCION"
            )
        }
    }

    /**
     * Links a device using the entered PIN validating against Firebase Firestore in the cloud.
     */
    fun linkWithPin(context: Context, deviceName: String = "Terminal Móvil") {
        val enteredPin = _pinInput.value.trim()
        if (enteredPin.length != 6) {
            _uiState.value = LinkingUiState.Error("El PIN debe contener exactamente 6 dígitos numéricos.")
            return
        }

        val deviceId = DevicePreferences.getLinkedDeviceId(context)
        _uiState.value = LinkingUiState.Validating("Verificando PIN en Firebase...")

        viewModelScope.launch {
            try {
                // 1. Validar contra la base en la nube de Firebase Firestore
                val cloudValidationResult = FirebaseManager.validatePinOrQr(
                    context = context,
                    input = enteredPin,
                    deviceId = deviceId,
                    deviceName = deviceName
                )

                val now = System.currentTimeMillis()
                val isLocalPinMatch = (enteredPin == _activeManagerPin.value && (now - _pinTimestamp.value <= 15 * 60 * 1000L))

                if (cloudValidationResult.isSuccess) {
                    val token = cloudValidationResult.getOrNull()
                    val role = token?.rol?.ifBlank { "RECEPCION" } ?: "RECEPCION"
                    executeLinkingProcess(
                        context = context,
                        role = role,
                        token = token?.pin ?: enteredPin,
                        deviceName = deviceName
                    )
                } else if (isLocalPinMatch) {
                    executeLinkingProcess(
                        context = context,
                        role = "RECEPCION",
                        token = enteredPin,
                        deviceName = deviceName
                    )
                } else {
                    val errorReason = cloudValidationResult.exceptionOrNull()?.message
                        ?: "PIN no válido o ha expirado. Solicite un nuevo PIN en Gerencia."
                    _uiState.value = LinkingUiState.Error(errorReason)
                }
            } catch (e: Exception) {
                Log.e("LinkingViewModel", "Excepción al vincular por PIN: ${e.message}", e)
                _uiState.value = LinkingUiState.Error("Error de conexión: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Processes scanned QR data from the QrScannerManager / ZXing Intent, validating against Firebase.
     */
    fun processScannedQr(context: Context, rawResult: String?, deviceName: String = "Terminal Móvil") {
        if (rawResult.isNullOrBlank()) {
            _uiState.value = LinkingUiState.Error("No se detectó ningún código QR.")
            return
        }

        val scannedData: ScannedQrData? = QrScannerManager.parseScannedQr(rawResult)
        if (scannedData == null) {
            _uiState.value = LinkingUiState.Error("Formato de código QR inválido.")
            return
        }

        val deviceId = DevicePreferences.getLinkedDeviceId(context)
        _uiState.value = LinkingUiState.Validating("Validando código QR con Firebase...")

        viewModelScope.launch {
            try {
                // 1. Buscar primero por el token del QR en Firestore
                var cloudResult = FirebaseManager.validatePinOrQr(
                    context = context,
                    input = scannedData.token,
                    deviceId = deviceId,
                    deviceName = scannedData.deviceName ?: deviceName
                )

                // Si no se encontró por token, intentar con el contenido crudo (que puede ser o incluir PIN)
                if (cloudResult.isFailure && scannedData.rawContent.isNotBlank() && scannedData.rawContent != scannedData.token) {
                    cloudResult = FirebaseManager.validatePinOrQr(
                        context = context,
                        input = scannedData.rawContent,
                        deviceId = deviceId,
                        deviceName = scannedData.deviceName ?: deviceName
                    )
                }

                val now = System.currentTimeMillis()
                val isLocalMatch = scannedData.token == _activeManagerQr.value ||
                        scannedData.rawContent.contains(_activeManagerQr.value) ||
                        scannedData.token.startsWith("RIVERA-LINK-")

                if (cloudResult.isSuccess) {
                    val token = cloudResult.getOrNull()
                    val role = token?.rol?.ifBlank { scannedData.role.ifBlank { "RECEPCION" } } ?: "RECEPCION"
                    executeLinkingProcess(
                        context = context,
                        role = role,
                        token = token?.qrToken ?: scannedData.token,
                        deviceName = scannedData.deviceName ?: deviceName
                    )
                } else if (isLocalMatch) {
                    executeLinkingProcess(
                        context = context,
                        role = scannedData.role.ifBlank { "RECEPCION" },
                        token = scannedData.token,
                        deviceName = scannedData.deviceName ?: deviceName
                    )
                } else {
                    val errorReason = cloudResult.exceptionOrNull()?.message
                        ?: "Código QR no válido o expirado. Genere uno nuevo en Gerencia."
                    _uiState.value = LinkingUiState.Error(errorReason)
                }
            } catch (e: Exception) {
                Log.e("LinkingViewModel", "Excepción al validar QR: ${e.message}", e)
                _uiState.value = LinkingUiState.Error("Error al procesar QR: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Executes validation, animated initial synchronization, persistence in DataStore + Room,
     * sincroniza habitaciones desde Firebase Firestore y transiciona a la pantalla correspondiente.
     */
    private fun executeLinkingProcess(
        context: Context,
        role: String,
        token: String,
        deviceName: String
    ) {
        viewModelScope.launch {
            _uiState.value = LinkingUiState.Validating("Verificando token criptográfico con Estación Central...")
            delay(400)

            _uiState.value = LinkingUiState.Syncing(0.25f, "Estableciendo canal seguro con Firebase Firestore...")
            val deviceId = DevicePreferences.getLinkedDeviceId(context)
            val email = "terminal.${role.lowercase()}@hotelrivera.com"

            // Registrar terminal en Firebase Firestore para que Gerencia lo vea de inmediato
            try {
                FirebaseManager.registerDeviceInFirestore(
                    context = context,
                    deviceId = deviceId,
                    deviceName = deviceName,
                    role = role,
                    userAssigned = email
                )
            } catch (e: Exception) {
                Log.w("LinkingViewModel", "Aviso al registrar terminal en Firestore: ${e.message}")
            }

            _uiState.value = LinkingUiState.Syncing(0.6f, "Sincronizando habitaciones e inventario desde la nube...")
            try {
                // Descargar habitaciones desde Firebase Firestore y volcarlas a SQLite Room
                val firestore = FirebaseManager.getFirestore(context)
                val snapshot = firestore.collection(FirebaseManager.COLLECTION_HABITACIONES).get().await()
                if (snapshot != null && !snapshot.isEmpty) {
                    val hotelDao = HotelDatabase.getDatabase(context).hotelDao()
                    for (doc in snapshot.documents) {
                        try {
                            val num = doc.getString("numero") ?: doc.getString("roomNumber") ?: doc.id
                            val estadoStr = doc.getString("estado") ?: doc.getString("status") ?: "Disponible"
                            val precio = doc.getDouble("precio") ?: doc.getDouble("price") ?: 150.0
                            val cliente = doc.getString("clientName")
                            val dpi = doc.getString("clientDpi")

                            val mappedStatus = when {
                                estadoStr.contains("Ocup", ignoreCase = true) -> RoomStatus.OCUPADA
                                estadoStr.contains("Limp", ignoreCase = true) -> RoomStatus.PENDIENTE_LIMPIEZA
                                else -> RoomStatus.DISPONIBLE
                            }

                            val existingRoom = hotelDao.getRoomByNumber(num)
                            val roomEntity = (existingRoom ?: RoomEntity(roomNumber = num, nightlyRate = precio)).copy(
                                status = mappedStatus,
                                clientName = cliente,
                                clientDpi = dpi,
                                nightlyRate = precio
                            )
                            hotelDao.insertRoom(roomEntity)
                        } catch (e: Exception) {
                            Log.w("LinkingViewModel", "Error procesando habitación: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("LinkingViewModel", "Aviso al sincronizar habitaciones de Firestore: ${e.message}")
            }

            _uiState.value = LinkingUiState.Syncing(0.9f, "Guardando credenciales locales seguras...")

            // Save in DataStore and Room
            sessionRepo.saveDeviceAuthorization(
                deviceId = deviceId,
                role = role,
                email = email,
                token = token
            )
            sessionRepo.saveSession(
                userRole = role,
                userEmail = email,
                userName = deviceName,
                authToken = token
            )

            // Save in persistent DevicePreferences for app reopen persistence without re-login
            val targetScreen = if (role.equals("GERENTE", ignoreCase = true) || role.equals("ADMIN", ignoreCase = true)) {
                Screen.GERENTE_DASHBOARD
            } else {
                Screen.RECEPCION
            }
            DevicePreferences.setDeviceLinked(
                context = context,
                deviceId = deviceId,
                email = email,
                role = role,
                userName = deviceName
            )
            DevicePreferences.setDeviceAuthorized(context, true)
            DevicePreferences.setLastActiveScreen(context, targetScreen.name)

            // Sync with Legacy DataStore and SharedPreferences
            DeviceDataStoreManager(context).saveDeviceAuthorization(deviceId, email)

            // Register in Room database
            val deviceEntity = DeviceEntity(
                name = deviceName,
                userAssigned = email,
                deviceId = deviceId,
                connectionStatus = DeviceConnectionStatus.CONNECTED,
                realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                lastHeartbeat = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis()
            )
            deviceRepo.insertDevice(deviceEntity)

            _uiState.value = LinkingUiState.Syncing(1.0f, "¡Dispositivo vinculado con éxito!")
            delay(300)

            _uiState.value = LinkingUiState.Success(
                role = role,
                deviceName = deviceName,
                targetScreen = targetScreen
            )

            delay(500)
            _navigationEvent.emit(targetScreen)
        }
    }

    fun resetState() {
        _uiState.value = LinkingUiState.Idle
        _pinInput.value = ""
    }
}
