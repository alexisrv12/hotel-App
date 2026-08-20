package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.HotelDatabase
import com.example.data.model.RoomSyncConflict
import com.example.data.model.SharedAppState
import com.example.data.model.SharedRoomState
import com.example.data.model.SyncHealthReport
import com.example.data.model.SyncLogEntry
import com.example.data.model.SyncState
import com.example.data.model.UndoSyncAction
import com.example.data.repository.RealtimeSyncRepository
import com.example.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Evento de notificación crítica para la UI (banner sutil, snackbar, badge sonoro/visual).
 */
data class CriticalAlertNotification(
    val title: String,
    val description: String,
    val authorRole: String,
    val roomNumber: String?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * SharedViewModel que centraliza el estado compartido y la lógica reactiva en espejo.
 * Integra la capa Room Database para soporte offline permanente, mecanismo de Deshacer (Undo),
 * caducidad de sesión por inactividad de 1 mes y telemetría de Sync Health.
 */
class SharedSyncViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val localDao = HotelDatabase.getDatabase(application).sharedAppStateDao()
    private val repository: SyncRepository = RealtimeSyncRepository(localDao = localDao)

    // Identificador de sesión vinculada (por defecto 'session_mirror_central')
    private val _currentSessionId = MutableStateFlow("session_mirror_central")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    // Rol del dispositivo local ("GERENTE" o "RECEPCION")
    private val _deviceRole = MutableStateFlow("GERENTE")
    val deviceRole: StateFlow<String> = _deviceRole.asStateFlow()

    // Nombre de la terminal local
    private val _deviceName = MutableStateFlow("Terminal Gerente Principal")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    // Flujo de Alertas Críticas recibidas desde el dispositivo par (Peer Device)
    private val _peerCriticalAlert = MutableSharedFlow<CriticalAlertNotification>(extraBufferCapacity = 5)
    val peerCriticalAlert: SharedFlow<CriticalAlertNotification> = _peerCriticalAlert.asSharedFlow()

    // Control de visibilidad del Overlay de Registro de Sincronización (Sync Log)
    private val _isSyncLogOverlayVisible = MutableStateFlow(false)
    val isSyncLogOverlayVisible: StateFlow<Boolean> = _isSyncLogOverlayVisible.asStateFlow()

    // Control de visibilidad de la pantalla de Salud de Sincronización (Sync Health)
    private val _isSyncHealthVisible = MutableStateFlow(false)
    val isSyncHealthVisible: StateFlow<Boolean> = _isSyncHealthVisible.asStateFlow()

    // Flujo para notificar la acción de Deshacer (Undo) al Snackbar
    private val _undoSnackbarEvent = MutableSharedFlow<UndoSyncAction>(extraBufferCapacity = 3)
    val undoSnackbarEvent: SharedFlow<UndoSyncAction> = _undoSnackbarEvent.asSharedFlow()

    // Mensaje de feedback o error para el usuario
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    /**
     * STATEFLOW PRINCIPAL (Single Source of Truth)
     * Conectado a Room y al flujo reactivo del repositorio.
     */
    val sharedState: StateFlow<SharedAppState> = _currentSessionId
        .flatMapLatest { sessionId ->
            repository.observeSharedState(sessionId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SharedAppState(sessionId = "session_mirror_central")
        )

    init {
        // Escuchar alertas críticas y filtrar solo las originadas por el dispositivo par (Peer)
        repository.observeCriticalAlerts()
            .onEach { logEntry ->
                if (logEntry.authorRole != _deviceRole.value && logEntry.isCritical) {
                    _peerCriticalAlert.emit(
                        CriticalAlertNotification(
                            title = logEntry.title,
                            description = logEntry.description,
                            authorRole = logEntry.authorRole,
                            roomNumber = logEntry.roomNumber,
                            timestamp = logEntry.timestamp
                        )
                    )
                }
            }
            .launchIn(viewModelScope)

        // Comprobar validez de sesión por inactividad
        viewModelScope.launch {
            repository.checkInactivityAndValidateSession(_currentSessionId.value)
        }
    }

    fun setDeviceRole(role: String, name: String) {
        _deviceRole.value = role
        _deviceName.value = name
    }

    fun setSessionId(sessionId: String) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            repository.checkInactivityAndValidateSession(sessionId)
        }
    }

    fun toggleSyncLogOverlay(visible: Boolean) {
        _isSyncLogOverlayVisible.value = visible
    }

    fun toggleSyncHealthScreen(visible: Boolean) {
        _isSyncHealthVisible.value = visible
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    /**
     * Realizar Check-In en una habitación
     */
    fun performCheckIn(roomNumber: String, guestName: String, context: Context? = null) {
        viewModelScope.launch {
            val valid = repository.checkInactivityAndValidateSession(_currentSessionId.value)
            if (!valid) return@launch

            repository.performRoomCheckIn(
                sessionId = _currentSessionId.value,
                roomNumber = roomNumber,
                guestName = guestName,
                role = _deviceRole.value,
                deviceName = _deviceName.value,
                context = context
            )

            // Disparar evento de Snackbar Undo
            val currentUndo = sharedState.value.lastUndoAction
            if (currentUndo != null) {
                _undoSnackbarEvent.emit(currentUndo)
            }
        }
    }

    /**
     * Cambiar estado de una habitación (p. ej. Check-Out, Limpieza, Mantenimiento)
     */
    fun changeRoomStatus(roomNumber: String, newStatus: String, context: Context? = null) {
        viewModelScope.launch {
            val valid = repository.checkInactivityAndValidateSession(_currentSessionId.value)
            if (!valid) return@launch

            repository.updateRoomStatus(
                sessionId = _currentSessionId.value,
                roomNumber = roomNumber,
                newStatus = newStatus,
                role = _deviceRole.value,
                deviceName = _deviceName.value,
                context = context
            )

            val currentUndo = sharedState.value.lastUndoAction
            if (currentUndo != null) {
                _undoSnackbarEvent.emit(currentUndo)
            }
        }
    }

    /**
     * Ejecutar Deshacer (Undo) de la última acción de sincronización de habitaciones
     */
    fun undoLastSyncAction() {
        viewModelScope.launch {
            val success = repository.undoLastAction(
                sessionId = _currentSessionId.value,
                role = _deviceRole.value,
                deviceName = _deviceName.value
            )
            if (success) {
                _userFeedbackMessage.value = "Cambio revertido con éxito (Undo aplicado)"
            }
        }
    }

    /**
     * Resolver un conflicto de concurrencia detectado entre ambos dispositivos
     */
    fun resolveConflict(roomNumber: String, chosenState: SharedRoomState) {
        viewModelScope.launch {
            repository.resolveConflict(
                sessionId = _currentSessionId.value,
                roomNumber = roomNumber,
                chosenState = chosenState,
                role = _deviceRole.value,
                deviceName = _deviceName.value
            )
            _userFeedbackMessage.value = "Conflicto en Hab. $roomNumber resuelto exitosamente"
        }
    }

    /**
     * Re-autenticar sesión tras expirar por 1 mes de inactividad
     */
    fun reauthenticateExpiredSession(pin: String): Boolean {
        var isSuccess = false
        viewModelScope.launch {
            val result = repository.reauthenticateSession(_currentSessionId.value, pin)
            isSuccess = result
            if (result) {
                _userFeedbackMessage.value = "Sesión re-autenticada y renovada correctamente"
            } else {
                _userFeedbackMessage.value = "PIN incorrecto. Ingrese el PIN de seguridad (1234)"
            }
        }
        return isSuccess
    }

    /**
     * Simular expiración de sesión por inactividad (> 1 mes)
     */
    fun simulateSessionExpiration() {
        viewModelScope.launch {
            repository.simulateSessionExpired(_currentSessionId.value)
            _userFeedbackMessage.value = "Simulación: Sesión marcada como inactiva por más de 30 días"
        }
    }

    /**
     * Forzar reintento de sincronización de datos encolados en modo Offline
     */
    fun forceSyncQueuedData() {
        viewModelScope.launch {
            repository.syncQueuedOfflineData(_currentSessionId.value)
            _userFeedbackMessage.value = "Cola de sincronización vaciada y sincronizada con el espejo"
        }
    }

    /**
     * Alternar estado de una tarea
     */
    fun toggleTask(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(
                sessionId = _currentSessionId.value,
                taskId = taskId,
                isCompleted = isCompleted,
                role = _deviceRole.value
            )
        }
    }

    /**
     * Disparar un conflicto simulado para probar la resolución de concurrencia
     */
    fun triggerSimulatedConflict(roomNumber: String) {
        viewModelScope.launch {
            repository.simulateConflict(
                sessionId = _currentSessionId.value,
                roomNumber = roomNumber,
                role = _deviceRole.value
            )
            _userFeedbackMessage.value = "Conflicto simulado disparado para Hab. $roomNumber"
        }
    }

    /**
     * Actualizar arqueo de caja
     */
    fun updateCashBalance(newBalance: Double) {
        viewModelScope.launch {
            repository.updateCashBalance(
                sessionId = _currentSessionId.value,
                newBalance = newBalance,
                role = _deviceRole.value,
                deviceName = _deviceName.value
            )
        }
    }
}
