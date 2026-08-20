package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Estado del canal de sincronización del dispositivo local / remoto.
 */
enum class SyncState {
    SYNCED,         // En línea y sincronizado con la nube
    QUEUED_OFFLINE, // Modificaciones en cola local mientras se recupera la red
    SYNCING,        // Enviando paquete de cambios a la nube
    CONFLICT        // Conflicto de concurrencia detectado entre terminales
}

/**
 * Tipo de evento o acción crítica en el espejo de datos.
 */
enum class SyncEventType {
    ROOM_CHECK_IN,
    ROOM_CHECK_OUT,
    CASH_REGISTER_UPDATE,
    TASK_STATUS_CHANGE,
    PAYMENT_REGISTERED,
    CONFLICT_RESOLVED
}

/**
 * Registro histórico de auditoría para el "Sync Log" en tiempo real.
 */
data class SyncLogEntry(
    val id: String = "",
    val eventType: SyncEventType = SyncEventType.ROOM_CHECK_IN,
    val title: String = "",
    val description: String = "",
    val authorRole: String = "",       // "GERENTE" o "RECEPCION"
    val authorDeviceName: String = "",
    val roomNumber: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isCritical: Boolean = false
)

/**
 * Modelo para representar una Habitación y su estado en el espejo de datos.
 */
data class SharedRoomState(
    val roomNumber: String = "",
    val status: String = "AVAILABLE", // "AVAILABLE", "OCCUPIED", "CLEANING", "MAINTENANCE"
    val guestName: String? = null,
    val checkInTime: Long? = null,
    val lastUpdatedBy: String = "",    // "GERENTE" o "RECEPCION"
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Modelo para encapsular un conflicto de concurrencia detectado entre terminales.
 */
data class RoomSyncConflict(
    val roomNumber: String = "",
    val localState: SharedRoomState = SharedRoomState(),
    val remoteState: SharedRoomState = SharedRoomState(),
    val detectedAt: Long = System.currentTimeMillis()
)

/**
 * Tarea compartida en el espejo.
 */
data class SharedTask(
    val id: String = "",
    val title: String = "",
    val roomNumber: String = "",
    val isCompleted: Boolean = false,
    val assignedTo: String = "",
    val updatedByRole: String = ""
)

/**
 * Estado en tiempo real de la caja registradora.
 */
data class SharedCashRegister(
    val isOpen: Boolean = true,
    val currentBalance: Double = 0.0,
    val lastUpdatedBy: String = "",
    val pendingDepositsCount: Int = 0
)

/**
 * Acción susceptible de ser deshecha mediante Snackbar (Undo).
 */
data class UndoSyncAction(
    val actionId: String = "",
    val roomNumber: String = "",
    val previousState: SharedRoomState = SharedRoomState(),
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Métricas de diagnóstico y salud de sincronización para el gráfico Recharts-style.
 */
data class SyncHealthDataPoint(
    val label: String,             // Ej: "10:00", "11:00", "RTT", "Payload"
    val latencyMs: Float,          // Milisegundos de respuesta
    val queueSize: Float,          // Cantidad de elementos encolados
    val isBottleneck: Boolean = false
)

data class SyncHealthReport(
    val currentLatencyMs: Long = 42L,
    val signalStrengthPercent: Int = 94,
    val connectionType: String = "Wi-Fi (5 GHz)",
    val pendingQueueCount: Int = 0,
    val successRatePercent: Int = 99,
    val hourlyDataPoints: List<SyncHealthDataPoint> = emptyList(),
    val isNetworkHealthy: Boolean = true,
    val bottleneckDiagnosis: String = "Conexión óptima y sin congestión en cola"
)

/**
 * MODELO CENTRALIZADO (Single Source of Truth)
 */
data class SharedAppState(
    @DocumentId
    val sessionId: String = "",
    val sessionToken: String = "TOK_SESSION_${System.currentTimeMillis()}",
    val hotelName: String = "Grand Hotel Central",
    val syncStatus: SyncState = SyncState.SYNCED,
    val activeRooms: Map<String, SharedRoomState> = emptyMap(),
    val activeTasks: List<SharedTask> = emptyList(),
    val cashRegister: SharedCashRegister = SharedCashRegister(),
    val syncLogs: List<SyncLogEntry> = emptyList(),
    val pendingConflict: RoomSyncConflict? = null,
    val lastUndoAction: UndoSyncAction? = null,
    val offlineQueueSize: Int = 0,
    val lastActivityTimestamp: Long = System.currentTimeMillis(),
    val isSessionExpired: Boolean = false,
    val healthReport: SyncHealthReport = SyncHealthReport(),
    @ServerTimestamp
    val lastServerSync: Date? = null,
    val lastUpdatedByDevice: String = ""
)
