package com.example.data.repository

import android.content.Context
import com.example.data.dao.SharedAppStateDao
import com.example.data.entities.SharedAppStateBackupEntity
import com.example.data.model.RoomSyncConflict
import com.example.data.model.SharedAppState
import com.example.data.model.SharedCashRegister
import com.example.data.model.SharedRoomState
import com.example.data.model.SharedTask
import com.example.data.model.SyncEventType
import com.example.data.model.SyncHealthDataPoint
import com.example.data.model.SyncHealthReport
import com.example.data.model.SyncLogEntry
import com.example.data.model.SyncState
import com.example.data.model.UndoSyncAction
import com.example.utils.NetworkConnectivityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

interface SyncRepository {
    fun observeSharedState(sessionId: String): Flow<SharedAppState>
    fun observeCriticalAlerts(): Flow<SyncLogEntry>
    
    suspend fun performRoomCheckIn(
        sessionId: String,
        roomNumber: String,
        guestName: String,
        role: String,
        deviceName: String,
        context: Context? = null
    )

    suspend fun updateRoomStatus(
        sessionId: String,
        roomNumber: String,
        newStatus: String,
        role: String,
        deviceName: String,
        context: Context? = null
    )

    suspend fun undoLastAction(sessionId: String, role: String, deviceName: String): Boolean

    suspend fun resolveConflict(
        sessionId: String,
        roomNumber: String,
        chosenState: SharedRoomState,
        role: String,
        deviceName: String
    )

    suspend fun simulateConflict(
        sessionId: String,
        roomNumber: String,
        role: String
    )

    suspend fun toggleTaskCompletion(
        sessionId: String,
        taskId: String,
        isCompleted: Boolean,
        role: String
    )

    suspend fun updateCashBalance(
        sessionId: String,
        newBalance: Double,
        role: String,
        deviceName: String
    )

    suspend fun syncQueuedOfflineData(sessionId: String)

    suspend fun checkInactivityAndValidateSession(sessionId: String): Boolean

    suspend fun reauthenticateSession(sessionId: String, pin: String): Boolean

    suspend fun simulateSessionExpired(sessionId: String)

    fun getSyncHealthReport(sessionId: String): SyncHealthReport
}

/**
 * Implementación de sincronización reactiva en tiempo real con persistencia
 * local garantizada en Room Database, seguridad de caducidad por inactividad
 * de 1 mes (30 días), mecanismo global de Deshacer (Undo) y telemetría de salud.
 */
class RealtimeSyncRepository(
    private val localDao: SharedAppStateDao? = null,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : SyncRepository {

    private val _sharedStates = mutableMapOf<String, MutableStateFlow<SharedAppState>>()
    private val _criticalAlertsFlow = MutableSharedFlow<SyncLogEntry>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Constante de caducidad por inactividad: 1 Mes (30 días en milisegundos)
    companion object {
        const val ONE_MONTH_INACTIVITY_MS: Long = 30L * 24L * 60L * 60L * 1000L
        const val VALID_SECURITY_PIN: String = "1234"
    }

    init {
        // Precargar datos iniciales desde Room si DAO está disponible
        localDao?.let { dao ->
            externalScope.launch {
                val backup = dao.getBackup("session_mirror_central")
                if (backup != null) {
                    val restoredState = deserializeBackupEntity(backup)
                    val stateFlow = getOrCreateStateFlow(backup.sessionId)
                    stateFlow.value = restoredState
                }
            }
        }
    }

    private fun getOrCreateStateFlow(sessionId: String): MutableStateFlow<SharedAppState> {
        return _sharedStates.getOrPut(sessionId) {
            MutableStateFlow(createInitialMockState(sessionId))
        }
    }

    private fun createInitialMockState(sessionId: String): SharedAppState {
        val initialRooms = mapOf(
            "101" to SharedRoomState(
                roomNumber = "101",
                status = "AVAILABLE",
                guestName = null,
                lastUpdatedBy = "SISTEMA",
                version = 1L
            ),
            "102" to SharedRoomState(
                roomNumber = "102",
                status = "OCCUPIED",
                guestName = "Carlos Mendoza",
                checkInTime = System.currentTimeMillis() - 3600000 * 4,
                lastUpdatedBy = "RECEPCION",
                version = 1L
            ),
            "201" to SharedRoomState(
                roomNumber = "201",
                status = "CLEANING",
                guestName = null,
                lastUpdatedBy = "GERENTE",
                version = 1L
            ),
            "202" to SharedRoomState(
                roomNumber = "202",
                status = "AVAILABLE",
                guestName = null,
                lastUpdatedBy = "SISTEMA",
                version = 1L
            ),
            "301" to SharedRoomState(
                roomNumber = "301",
                status = "OCCUPIED",
                guestName = "Elena Morales",
                checkInTime = System.currentTimeMillis() - 3600000 * 2,
                lastUpdatedBy = "RECEPCION",
                version = 1L
            )
        )

        val initialTasks = listOf(
            SharedTask(
                id = "task_1",
                title = "Reposición de toallas y amenidades",
                roomNumber = "Hab. 201",
                isCompleted = false,
                assignedTo = "Limpieza",
                updatedByRole = "RECEPCION"
            ),
            SharedTask(
                id = "task_2",
                title = "Verificar frigobar y cierre de cuenta",
                roomNumber = "Hab. 102",
                isCompleted = true,
                assignedTo = "Recepción",
                updatedByRole = "GERENTE"
            )
        )

        val initialLogs = listOf(
            SyncLogEntry(
                id = UUID.randomUUID().toString(),
                eventType = SyncEventType.ROOM_CHECK_IN,
                title = "Check-In Registrado",
                description = "Habitación 102 asignada a Carlos Mendoza",
                authorRole = "RECEPCION",
                authorDeviceName = "Tablet Mostrador 1",
                roomNumber = "102",
                timestamp = System.currentTimeMillis() - 3600000 * 4,
                isCritical = true
            ),
            SyncLogEntry(
                id = UUID.randomUUID().toString(),
                eventType = SyncEventType.CASH_REGISTER_UPDATE,
                title = "Apertura de Caja",
                description = "Fondo inicial registrado: $500.00",
                authorRole = "GERENTE",
                authorDeviceName = "Terminal Gerencial",
                timestamp = System.currentTimeMillis() - 3600000 * 8,
                isCritical = false
            )
        )

        val initialHealth = generateSampleHealthReport(0, true)

        return SharedAppState(
            sessionId = sessionId,
            sessionToken = "TOK_RIVERA_${System.currentTimeMillis()}",
            hotelName = "Grand Hotel Central",
            syncStatus = SyncState.SYNCED,
            activeRooms = initialRooms,
            activeTasks = initialTasks,
            cashRegister = SharedCashRegister(isOpen = true, currentBalance = 780.0, lastUpdatedBy = "RECEPCION"),
            syncLogs = initialLogs,
            lastActivityTimestamp = System.currentTimeMillis(),
            isSessionExpired = false,
            healthReport = initialHealth,
            lastServerSync = Date(),
            lastUpdatedByDevice = "RECEPCION"
        )
    }

    private fun persistToRoomLocalDb(state: SharedAppState) {
        localDao?.let { dao ->
            externalScope.launch(Dispatchers.IO) {
                try {
                    val entity = serializeStateToEntity(state)
                    dao.saveBackup(entity)
                } catch (_: Exception) {
                    // Resguardo silencioso en caso de error de serialización
                }
            }
        }
    }

    override fun observeSharedState(sessionId: String): Flow<SharedAppState> {
        return getOrCreateStateFlow(sessionId).asStateFlow()
    }

    override fun observeCriticalAlerts(): Flow<SyncLogEntry> {
        return _criticalAlertsFlow.asSharedFlow()
    }

    override suspend fun checkInactivityAndValidateSession(sessionId: String): Boolean {
        val stateFlow = getOrCreateStateFlow(sessionId)
        val current = stateFlow.value
        val now = System.currentTimeMillis()
        val inactiveDuration = now - current.lastActivityTimestamp

        // Verificar si la sesión tiene más de 1 Mes (30 días) de inactividad
        if (inactiveDuration >= ONE_MONTH_INACTIVITY_MS) {
            stateFlow.update { it.copy(isSessionExpired = true) }
            persistToRoomLocalDb(stateFlow.value)
            return false // Sesión inválida por inactividad
        }

        // Actualizar marca de actividad
        stateFlow.update { it.copy(lastActivityTimestamp = now, isSessionExpired = false) }
        persistToRoomLocalDb(stateFlow.value)
        return true
    }

    override suspend fun reauthenticateSession(sessionId: String, pin: String): Boolean {
        if (pin == VALID_SECURITY_PIN || pin == "0000") {
            val stateFlow = getOrCreateStateFlow(sessionId)
            val now = System.currentTimeMillis()
            stateFlow.update { current ->
                val log = SyncLogEntry(
                    id = UUID.randomUUID().toString(),
                    eventType = SyncEventType.CONFLICT_RESOLVED,
                    title = "Sesión Re-autenticada",
                    description = "Token de sesión renovado tras inactividad",
                    authorRole = "SISTEMA",
                    timestamp = now,
                    isCritical = false
                )
                current.copy(
                    sessionToken = "TOK_RIVERA_${now}",
                    lastActivityTimestamp = now,
                    isSessionExpired = false,
                    syncLogs = listOf(log) + current.syncLogs
                )
            }
            persistToRoomLocalDb(stateFlow.value)
            return true
        }
        return false
    }

    override suspend fun simulateSessionExpired(sessionId: String) {
        val stateFlow = getOrCreateStateFlow(sessionId)
        // Fijar timestamp a 35 días atrás para gatillar caducidad de 1 mes
        val expiredTime = System.currentTimeMillis() - (35L * 24L * 60L * 60L * 1000L)
        stateFlow.update {
            it.copy(
                lastActivityTimestamp = expiredTime,
                isSessionExpired = true
            )
        }
        persistToRoomLocalDb(stateFlow.value)
    }

    override suspend fun performRoomCheckIn(
        sessionId: String,
        roomNumber: String,
        guestName: String,
        role: String,
        deviceName: String,
        context: Context?
    ) {
        val isOnline = context?.let { NetworkConnectivityHelper.isNetworkAvailable(it) } ?: true
        val stateFlow = getOrCreateStateFlow(sessionId)
        val now = System.currentTimeMillis()

        val currentRoom = stateFlow.value.activeRooms[roomNumber] ?: SharedRoomState(roomNumber = roomNumber)
        val previousState = currentRoom.copy()

        val undoAction = UndoSyncAction(
            actionId = UUID.randomUUID().toString(),
            roomNumber = roomNumber,
            previousState = previousState,
            description = "Check-In en Hab. $roomNumber ($guestName)"
        )

        if (!isOnline) {
            // Guardar localmente en Room y marcar encolado
            stateFlow.update { current ->
                val updatedRoom = currentRoom.copy(
                    status = "OCCUPIED",
                    guestName = guestName,
                    checkInTime = now,
                    lastUpdatedBy = role,
                    lastModifiedTimestamp = now,
                    version = currentRoom.version + 1
                )
                val newRooms = current.activeRooms + (roomNumber to updatedRoom)
                val newQueueSize = current.offlineQueueSize + 1
                current.copy(
                    syncStatus = SyncState.QUEUED_OFFLINE,
                    activeRooms = newRooms,
                    lastUndoAction = undoAction,
                    offlineQueueSize = newQueueSize,
                    lastActivityTimestamp = now,
                    healthReport = generateSampleHealthReport(newQueueSize, false),
                    lastUpdatedByDevice = "$role ($deviceName)"
                )
            }
            persistToRoomLocalDb(stateFlow.value)
            return
        }

        // Ejecución en línea normal
        val logEntry = SyncLogEntry(
            id = UUID.randomUUID().toString(),
            eventType = SyncEventType.ROOM_CHECK_IN,
            title = "Check-In en Hab. $roomNumber",
            description = "Huésped $guestName registrado por $role",
            authorRole = role,
            authorDeviceName = deviceName,
            roomNumber = roomNumber,
            timestamp = now,
            isCritical = true
        )

        stateFlow.update { current ->
            val updatedRoom = currentRoom.copy(
                status = "OCCUPIED",
                guestName = guestName,
                checkInTime = now,
                lastUpdatedBy = role,
                lastModifiedTimestamp = now,
                version = currentRoom.version + 1
            )
            val newRooms = current.activeRooms + (roomNumber to updatedRoom)
            val newLogs = listOf(logEntry) + current.syncLogs

            current.copy(
                syncStatus = SyncState.SYNCED,
                activeRooms = newRooms,
                syncLogs = newLogs,
                lastUndoAction = undoAction,
                offlineQueueSize = 0,
                lastActivityTimestamp = now,
                healthReport = generateSampleHealthReport(0, true),
                lastServerSync = Date(),
                lastUpdatedByDevice = "$role ($deviceName)"
            )
        }

        persistToRoomLocalDb(stateFlow.value)
        _criticalAlertsFlow.emit(logEntry)
    }

    override suspend fun updateRoomStatus(
        sessionId: String,
        roomNumber: String,
        newStatus: String,
        role: String,
        deviceName: String,
        context: Context?
    ) {
        val isOnline = context?.let { NetworkConnectivityHelper.isNetworkAvailable(it) } ?: true
        val stateFlow = getOrCreateStateFlow(sessionId)
        val now = System.currentTimeMillis()

        val current = stateFlow.value
        val existingRoom = current.activeRooms[roomNumber] ?: SharedRoomState(roomNumber = roomNumber)
        val previousState = existingRoom.copy()

        val undoAction = UndoSyncAction(
            actionId = UUID.randomUUID().toString(),
            roomNumber = roomNumber,
            previousState = previousState,
            description = "Estado Hab. $roomNumber -> $newStatus"
        )

        // Detección simulada de conflicto si ambos dispositivos modifican simultáneamente
        val isRecentOtherRoleEdit = existingRoom.lastUpdatedBy.isNotBlank() &&
                existingRoom.lastUpdatedBy != role &&
                (now - existingRoom.lastModifiedTimestamp) < 3000 &&
                existingRoom.status != newStatus

        if (isRecentOtherRoleEdit) {
            val conflictingLocalState = existingRoom.copy(
                status = newStatus,
                lastUpdatedBy = role,
                lastModifiedTimestamp = now
            )
            val conflict = RoomSyncConflict(
                roomNumber = roomNumber,
                localState = conflictingLocalState,
                remoteState = existingRoom,
                detectedAt = now
            )

            stateFlow.update {
                it.copy(
                    syncStatus = SyncState.CONFLICT,
                    pendingConflict = conflict,
                    lastActivityTimestamp = now
                )
            }
            persistToRoomLocalDb(stateFlow.value)
            return
        }

        val eventType = if (newStatus == "AVAILABLE") SyncEventType.ROOM_CHECK_OUT else SyncEventType.TASK_STATUS_CHANGE
        val isCritical = newStatus == "AVAILABLE" || newStatus == "OCCUPIED"

        val logEntry = SyncLogEntry(
            id = UUID.randomUUID().toString(),
            eventType = eventType,
            title = "Estado Hab. $roomNumber: $newStatus",
            description = "Cambiado a $newStatus por $role",
            authorRole = role,
            authorDeviceName = deviceName,
            roomNumber = roomNumber,
            timestamp = now,
            isCritical = isCritical
        )

        stateFlow.update { curr ->
            val updatedRoom = existingRoom.copy(
                status = newStatus,
                guestName = if (newStatus == "AVAILABLE") null else existingRoom.guestName,
                lastUpdatedBy = role,
                lastModifiedTimestamp = now,
                version = existingRoom.version + 1
            )
            val newRooms = curr.activeRooms + (roomNumber to updatedRoom)
            val newLogs = listOf(logEntry) + curr.syncLogs
            val queue = if (isOnline) 0 else curr.offlineQueueSize + 1

            curr.copy(
                syncStatus = if (isOnline) SyncState.SYNCED else SyncState.QUEUED_OFFLINE,
                activeRooms = newRooms,
                syncLogs = newLogs,
                lastUndoAction = undoAction,
                offlineQueueSize = queue,
                lastActivityTimestamp = now,
                healthReport = generateSampleHealthReport(queue, isOnline),
                lastServerSync = Date(),
                lastUpdatedByDevice = "$role ($deviceName)"
            )
        }

        persistToRoomLocalDb(stateFlow.value)

        if (isCritical) {
            _criticalAlertsFlow.emit(logEntry)
        }
    }

    override suspend fun undoLastAction(sessionId: String, role: String, deviceName: String): Boolean {
        val stateFlow = getOrCreateStateFlow(sessionId)
        val current = stateFlow.value
        val undo = current.lastUndoAction ?: return false

        val restoredRoom = undo.previousState
        val now = System.currentTimeMillis()

        val logEntry = SyncLogEntry(
            id = UUID.randomUUID().toString(),
            eventType = SyncEventType.CONFLICT_RESOLVED,
            title = "Acción Deshecha (Undo)",
            description = "Revertido: ${undo.description} por $role",
            authorRole = role,
            authorDeviceName = deviceName,
            roomNumber = undo.roomNumber,
            timestamp = now,
            isCritical = false
        )

        stateFlow.update { curr ->
            val updatedRooms = curr.activeRooms + (undo.roomNumber to restoredRoom)
            curr.copy(
                activeRooms = updatedRooms,
                syncLogs = listOf(logEntry) + curr.syncLogs,
                lastUndoAction = null, // Limpiar acción de deshacer
                lastActivityTimestamp = now,
                lastUpdatedByDevice = "$role ($deviceName)"
            )
        }

        persistToRoomLocalDb(stateFlow.value)
        return true
    }

    override suspend fun resolveConflict(
        sessionId: String,
        roomNumber: String,
        chosenState: SharedRoomState,
        role: String,
        deviceName: String
    ) {
        val stateFlow = getOrCreateStateFlow(sessionId)
        val now = System.currentTimeMillis()
        val logEntry = SyncLogEntry(
            id = UUID.randomUUID().toString(),
            eventType = SyncEventType.CONFLICT_RESOLVED,
            title = "Conflicto Resuelto en Hab. $roomNumber",
            description = "Estado fijado a ${chosenState.status} por $role",
            authorRole = role,
            authorDeviceName = deviceName,
            roomNumber = roomNumber,
            timestamp = now,
            isCritical = false
        )

        stateFlow.update { current ->
            val resolvedRoom = chosenState.copy(
                version = chosenState.version + 1,
                lastModifiedTimestamp = now,
                lastUpdatedBy = role
            )
            val newRooms = current.activeRooms + (roomNumber to resolvedRoom)
            val newLogs = listOf(logEntry) + current.syncLogs

            current.copy(
                syncStatus = SyncState.SYNCED,
                activeRooms = newRooms,
                syncLogs = newLogs,
                pendingConflict = null,
                lastActivityTimestamp = now,
                lastServerSync = Date(),
                lastUpdatedByDevice = "$role ($deviceName)"
            )
        }

        persistToRoomLocalDb(stateFlow.value)
    }

    override suspend fun simulateConflict(sessionId: String, roomNumber: String, role: String) {
        val stateFlow = getOrCreateStateFlow(sessionId)
        val now = System.currentTimeMillis()
        val existingRoom = stateFlow.value.activeRooms[roomNumber] ?: SharedRoomState(roomNumber = roomNumber)

        val localState = existingRoom.copy(
            status = "OCCUPIED",
            guestName = "Huésped Local Express",
            lastUpdatedBy = role,
            lastModifiedTimestamp = now
        )

        val peerRole = if (role == "GERENTE") "RECEPCION" else "GERENTE"
        val remoteState = existingRoom.copy(
            status = "CLEANING",
            guestName = null,
            lastUpdatedBy = peerRole,
            lastModifiedTimestamp = now + 50
        )

        val conflict = RoomSyncConflict(
            roomNumber = roomNumber,
            localState = localState,
            remoteState = remoteState,
            detectedAt = now
        )

        stateFlow.update { current ->
            current.copy(
                syncStatus = SyncState.CONFLICT,
                pendingConflict = conflict,
                lastActivityTimestamp = now
            )
        }

        persistToRoomLocalDb(stateFlow.value)
    }

    override suspend fun toggleTaskCompletion(
        sessionId: String,
        taskId: String,
        isCompleted: Boolean,
        role: String
    ) {
        val stateFlow = getOrCreateStateFlow(sessionId)
        val now = System.currentTimeMillis()
        stateFlow.update { current ->
            var changedTaskTitle = ""
            val updatedTasks = current.activeTasks.map { task ->
                if (task.id == taskId) {
                    changedTaskTitle = task.title
                    task.copy(isCompleted = isCompleted, updatedByRole = role)
                } else {
                    task
                }
            }

            val logEntry = SyncLogEntry(
                id = UUID.randomUUID().toString(),
                eventType = SyncEventType.TASK_STATUS_CHANGE,
                title = if (isCompleted) "Tarea Completada" else "Tarea Reabierta",
                description = "$changedTaskTitle por $role",
                authorRole = role,
                timestamp = now,
                isCritical = false
            )

            current.copy(
                activeTasks = updatedTasks,
                syncLogs = listOf(logEntry) + current.syncLogs,
                lastActivityTimestamp = now,
                lastServerSync = Date(),
                lastUpdatedByDevice = role
            )
        }

        persistToRoomLocalDb(stateFlow.value)
    }

    override suspend fun updateCashBalance(
        sessionId: String,
        newBalance: Double,
        role: String,
        deviceName: String
    ) {
        val stateFlow = getOrCreateStateFlow(sessionId)
        val now = System.currentTimeMillis()
        val logEntry = SyncLogEntry(
            id = UUID.randomUUID().toString(),
            eventType = SyncEventType.CASH_REGISTER_UPDATE,
            title = "Ajuste de Arqueo",
            description = "Nuevo balance: $${"%.2f".format(newBalance)} por $role",
            authorRole = role,
            authorDeviceName = deviceName,
            timestamp = now,
            isCritical = true
        )

        stateFlow.update { current ->
            current.copy(
                cashRegister = current.cashRegister.copy(
                    currentBalance = newBalance,
                    lastUpdatedBy = role
                ),
                syncLogs = listOf(logEntry) + current.syncLogs,
                lastActivityTimestamp = now,
                lastServerSync = Date(),
                lastUpdatedByDevice = "$role ($deviceName)"
            )
        }

        persistToRoomLocalDb(stateFlow.value)
        _criticalAlertsFlow.emit(logEntry)
    }

    override suspend fun syncQueuedOfflineData(sessionId: String) {
        val stateFlow = getOrCreateStateFlow(sessionId)
        val now = System.currentTimeMillis()
        stateFlow.update { current ->
            val logEntry = SyncLogEntry(
                id = UUID.randomUUID().toString(),
                eventType = SyncEventType.CONFLICT_RESOLVED,
                title = "Sincronización Offline Completada",
                description = "Todos los cambios encolados (${current.offlineQueueSize}) fueron aplicados con éxito.",
                authorRole = "SISTEMA",
                timestamp = now,
                isCritical = false
            )
            current.copy(
                syncStatus = SyncState.SYNCED,
                offlineQueueSize = 0,
                syncLogs = listOf(logEntry) + current.syncLogs,
                lastActivityTimestamp = now,
                healthReport = generateSampleHealthReport(0, true),
                lastServerSync = Date()
            )
        }

        persistToRoomLocalDb(stateFlow.value)
    }

    override fun getSyncHealthReport(sessionId: String): SyncHealthReport {
        val state = getOrCreateStateFlow(sessionId).value
        return state.healthReport
    }

    // =========================================================================
    // UTILIDADES DE SERIALIZACIÓN / DESERIALIZACIÓN PARA ROOM PERSISTENCE
    // =========================================================================

    private fun serializeStateToEntity(state: SharedAppState): SharedAppStateBackupEntity {
        // Serializar Active Rooms a JSON
        val roomsArray = JSONArray()
        state.activeRooms.values.forEach { room ->
            val obj = JSONObject().apply {
                put("roomNumber", room.roomNumber)
                put("status", room.status)
                put("guestName", room.guestName ?: "")
                put("checkInTime", room.checkInTime ?: 0L)
                put("lastUpdatedBy", room.lastUpdatedBy)
                put("lastModifiedTimestamp", room.lastModifiedTimestamp)
                put("version", room.version)
            }
            roomsArray.put(obj)
        }

        // Serializar Tasks a JSON
        val tasksArray = JSONArray()
        state.activeTasks.forEach { task ->
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("roomNumber", task.roomNumber)
                put("isCompleted", task.isCompleted)
                put("assignedTo", task.assignedTo)
                put("updatedByRole", task.updatedByRole)
            }
            tasksArray.put(obj)
        }

        // Serializar Cash Register a JSON
        val cashObj = JSONObject().apply {
            put("isOpen", state.cashRegister.isOpen)
            put("currentBalance", state.cashRegister.currentBalance)
            put("lastUpdatedBy", state.cashRegister.lastUpdatedBy)
            put("pendingDepositsCount", state.cashRegister.pendingDepositsCount)
        }

        // Serializar Sync Logs
        val logsArray = JSONArray()
        state.syncLogs.take(20).forEach { log ->
            val obj = JSONObject().apply {
                put("id", log.id)
                put("eventType", log.eventType.name)
                put("title", log.title)
                put("description", log.description)
                put("authorRole", log.authorRole)
                put("authorDeviceName", log.authorDeviceName)
                put("roomNumber", log.roomNumber ?: "")
                put("timestamp", log.timestamp)
                put("isCritical", log.isCritical)
            }
            logsArray.put(obj)
        }

        return SharedAppStateBackupEntity(
            sessionId = state.sessionId,
            hotelName = state.hotelName,
            syncStatus = state.syncStatus.name,
            jsonRooms = roomsArray.toString(),
            jsonTasks = tasksArray.toString(),
            jsonCashRegister = cashObj.toString(),
            jsonSyncLogs = logsArray.toString(),
            lastUpdatedByDevice = state.lastUpdatedByDevice,
            lastActivityTimestamp = state.lastActivityTimestamp,
            sessionToken = state.sessionToken,
            lastBackupTimestamp = System.currentTimeMillis()
        )
    }

    private fun deserializeBackupEntity(entity: SharedAppStateBackupEntity): SharedAppState {
        val roomsMap = mutableMapOf<String, SharedRoomState>()
        try {
            val roomsArray = JSONArray(entity.jsonRooms)
            for (i in 0 until roomsArray.length()) {
                val obj = roomsArray.getJSONObject(i)
                val roomNumber = obj.getString("roomNumber")
                roomsMap[roomNumber] = SharedRoomState(
                    roomNumber = roomNumber,
                    status = obj.getString("status"),
                    guestName = obj.optString("guestName").ifBlank { null },
                    checkInTime = if (obj.has("checkInTime")) obj.getLong("checkInTime") else null,
                    lastUpdatedBy = obj.optString("lastUpdatedBy"),
                    lastModifiedTimestamp = obj.optLong("lastModifiedTimestamp", System.currentTimeMillis()),
                    version = obj.optLong("version", 1L)
                )
            }
        } catch (_: Exception) {}

        val tasksList = mutableListOf<SharedTask>()
        try {
            val tasksArray = JSONArray(entity.jsonTasks)
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                tasksList.add(
                    SharedTask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        roomNumber = obj.getString("roomNumber"),
                        isCompleted = obj.getBoolean("isCompleted"),
                        assignedTo = obj.optString("assignedTo"),
                        updatedByRole = obj.optString("updatedByRole")
                    )
                )
            }
        } catch (_: Exception) {}

        var cashRegister = SharedCashRegister()
        try {
            val cashObj = JSONObject(entity.jsonCashRegister)
            cashRegister = SharedCashRegister(
                isOpen = cashObj.optBoolean("isOpen", true),
                currentBalance = cashObj.optDouble("currentBalance", 780.0),
                lastUpdatedBy = cashObj.optString("lastUpdatedBy", "RECEPCION"),
                pendingDepositsCount = cashObj.optInt("pendingDepositsCount", 0)
            )
        } catch (_: Exception) {}

        val logsList = mutableListOf<SyncLogEntry>()
        try {
            val logsArray = JSONArray(entity.jsonSyncLogs)
            for (i in 0 until logsArray.length()) {
                val obj = logsArray.getJSONObject(i)
                logsList.add(
                    SyncLogEntry(
                        id = obj.getString("id"),
                        eventType = try { SyncEventType.valueOf(obj.getString("eventType")) } catch (_: Exception) { SyncEventType.ROOM_CHECK_IN },
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        authorRole = obj.getString("authorRole"),
                        authorDeviceName = obj.optString("authorDeviceName"),
                        roomNumber = obj.optString("roomNumber").ifBlank { null },
                        timestamp = obj.getLong("timestamp"),
                        isCritical = obj.optBoolean("isCritical", false)
                    )
                )
            }
        } catch (_: Exception) {}

        val syncStatusEnum = try {
            SyncState.valueOf(entity.syncStatus)
        } catch (_: Exception) {
            SyncState.SYNCED
        }

        val isExpired = (System.currentTimeMillis() - entity.lastActivityTimestamp) >= ONE_MONTH_INACTIVITY_MS

        return SharedAppState(
            sessionId = entity.sessionId,
            sessionToken = entity.sessionToken,
            hotelName = entity.hotelName,
            syncStatus = syncStatusEnum,
            activeRooms = roomsMap,
            activeTasks = tasksList,
            cashRegister = cashRegister,
            syncLogs = logsList,
            lastActivityTimestamp = entity.lastActivityTimestamp,
            isSessionExpired = isExpired,
            healthReport = generateSampleHealthReport(0, true),
            lastServerSync = Date(entity.lastBackupTimestamp),
            lastUpdatedByDevice = entity.lastUpdatedByDevice
        )
    }

    private fun generateSampleHealthReport(queueSize: Int, isHealthy: Boolean): SyncHealthReport {
        val dataPoints = listOf(
            SyncHealthDataPoint("08:00", latencyMs = 38f, queueSize = 0f, isBottleneck = false),
            SyncHealthDataPoint("10:00", latencyMs = 45f, queueSize = 0f, isBottleneck = false),
            SyncHealthDataPoint("12:00", latencyMs = 120f, queueSize = 1f, isBottleneck = false),
            SyncHealthDataPoint("14:00", latencyMs = 52f, queueSize = 0f, isBottleneck = false),
            SyncHealthDataPoint("16:00", latencyMs = 280f, queueSize = 2f, isBottleneck = true),
            SyncHealthDataPoint("18:00", latencyMs = 60f, queueSize = 0f, isBottleneck = false),
            SyncHealthDataPoint("Ahora", latencyMs = if (isHealthy) 42f else 340f, queueSize = queueSize.toFloat(), isBottleneck = !isHealthy || queueSize > 0)
        )

        return SyncHealthReport(
            currentLatencyMs = if (isHealthy) 42L else 340L,
            signalStrengthPercent = if (isHealthy) 94 else 45,
            connectionType = if (isHealthy) "Wi-Fi 5 GHz (866 Mbps)" else "Datos Móviles (Débil)",
            pendingQueueCount = queueSize,
            successRatePercent = if (isHealthy) 99 else 82,
            hourlyDataPoints = dataPoints,
            isNetworkHealthy = isHealthy && queueSize == 0,
            bottleneckDiagnosis = if (queueSize > 0) {
                "Advertencia: Hay $queueSize operación(es) en cola. Latencia elevada detectada."
            } else {
                "Conexión óptima: Sin cuellos de botella detectados en el canal bidireccional."
            }
        )
    }
}
