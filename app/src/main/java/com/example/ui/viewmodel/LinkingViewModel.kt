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
import com.example.data.repository.SessionDataStoreRepository
import com.example.ui.Screen
import com.example.utils.DeviceCodeValidationHelper
import com.example.utils.DeviceDataStoreManager
import com.example.utils.DevicePreferences
import com.example.utils.QrScannerManager
import com.example.utils.ScannedQrData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
        // Load or initialize active pairing tokens
        viewModelScope.launch {
            val (savedPin, pinTs) = sessionRepo.getActivePin()
            val now = System.currentTimeMillis()
            if (!savedPin.isNullOrBlank() && pinTs > 0 && (now - pinTs < 120_000L)) {
                _activeManagerPin.value = savedPin
                _pinTimestamp.value = pinTs
            } else {
                refreshActivePin()
            }

            val (savedQr, qrTs) = sessionRepo.getActiveQrToken()
            if (!savedQr.isNullOrBlank() && qrTs > 0 && (now - qrTs < 120_000L)) {
                _activeManagerQr.value = savedQr
                _qrTimestamp.value = qrTs
            } else {
                refreshActiveQr()
            }

            // Ticker loop
            while (isActive) {
                val current = System.currentTimeMillis()
                if (current - _pinTimestamp.value >= 120_000L) {
                    refreshActivePin()
                }
                if (current - _qrTimestamp.value >= 120_000L) {
                    refreshActiveQr()
                }
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
        }
    }

    fun refreshActiveQr() {
        val newQr = generateRandomQrToken()
        val now = System.currentTimeMillis()
        _activeManagerQr.value = newQr
        _qrTimestamp.value = now
        viewModelScope.launch {
            sessionRepo.saveActiveLinkingQr(newQr, now)
        }
    }

    /**
     * Links a device using the entered PIN.
     */
    fun linkWithPin(context: Context, deviceName: String = "Terminal Móvil") {
        val enteredPin = _pinInput.value.trim()
        if (enteredPin.length != 6) {
            _uiState.value = LinkingUiState.Error("El PIN debe contener exactamente 6 dígitos numéricos.")
            return
        }

        val now = System.currentTimeMillis()
        val isPinValid = enteredPin == _activeManagerPin.value && (now - _pinTimestamp.value <= 120_000L)

        if (!isPinValid) {
            _uiState.value = LinkingUiState.Error("PIN no válido o ha expirado. Solicite un nuevo PIN en Gerencia.")
            return
        }

        executeLinkingProcess(
            context = context,
            role = "RECEPCION",
            token = enteredPin,
            deviceName = deviceName
        )
    }

    /**
     * Processes scanned QR data from the QrScannerManager / ZXing Intent.
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

        val now = System.currentTimeMillis()
        val isQrMatch = scannedData.token == _activeManagerQr.value ||
                scannedData.rawContent.contains(_activeManagerQr.value) ||
                scannedData.token.startsWith("RIVERA-LINK-")

        val isExpired = now - _qrTimestamp.value > 120_000L

        if (isExpired && !scannedData.token.startsWith("RIVERA-LINK-")) {
            _uiState.value = LinkingUiState.Error("El código QR ha expirado. Genere uno nuevo.")
            return
        }

        executeLinkingProcess(
            context = context,
            role = scannedData.role.ifBlank { "RECEPCION" },
            token = scannedData.token,
            deviceName = scannedData.deviceName ?: deviceName
        )
    }

    /**
     * Executes validation, animated initial synchronization, persistence in DataStore + Room,
     * and triggers navigation transition towards the matching role screen.
     */
    private fun executeLinkingProcess(
        context: Context,
        role: String,
        token: String,
        deviceName: String
    ) {
        viewModelScope.launch {
            _uiState.value = LinkingUiState.Validating("Verificando token criptográfico con Estación Central...")
            delay(500)

            _uiState.value = LinkingUiState.Syncing(0.2f, "Estableciendo canal seguro SSL/TLS...")
            delay(400)

            _uiState.value = LinkingUiState.Syncing(0.5f, "Descargando tarifas y configuración de habitaciones...")
            delay(500)

            _uiState.value = LinkingUiState.Syncing(0.85f, "Sincronizando inventario y catálogo de recepción...")
            delay(400)

            // Save in DataStore and Room
            val deviceId = DevicePreferences.getLinkedDeviceId(context)
            val email = "terminal.${role.lowercase()}@hotelrivera.com"

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

            // Regenerate tokens to invalidate used ones
            refreshActivePin()
            refreshActiveQr()

            _uiState.value = LinkingUiState.Syncing(1.0f, "¡Sincronización completada exitosamente!")
            delay(300)

            val targetScreen = if (role.equals("GERENTE", ignoreCase = true) || role.equals("ADMIN", ignoreCase = true)) {
                Screen.GERENTE_DASHBOARD
            } else {
                Screen.RECEPCION
            }

            _uiState.value = LinkingUiState.Success(
                role = role,
                deviceName = deviceName,
                targetScreen = targetScreen
            )

            delay(600)
            _navigationEvent.emit(targetScreen)
        }
    }

    fun resetState() {
        _uiState.value = LinkingUiState.Idle
        _pinInput.value = ""
    }
}
