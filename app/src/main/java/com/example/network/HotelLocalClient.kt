package com.example.network

import android.content.Context
import android.util.Log
import com.example.data.dao.HotelDao
import com.example.data.entities.ComandaEntity
import com.example.data.entities.OfflineSyncQueueEntity
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.SyncOperationStatus
import com.example.data.entities.SyncOperationType
import com.example.data.entities.TableEntity
import com.example.data.entities.TableStatus
import com.example.data.repository.SessionDataStoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class ClientConnectionState {
    CONECTADO,
    SINCRONIZANDO,
    DESCONECTADO
}

data class ClientSyncInfo(
    val state: ClientConnectionState = ClientConnectionState.DESCONECTADO,
    val serverAddress: String = "",
    val lastSyncTimestamp: Long = 0L,
    val pendingOperationsCount: Int = 0,
    val latencyMs: Long = 0L,
    val errorMessage: String? = null
)

class HotelLocalClient(
    private val context: Context,
    private val hotelDao: HotelDao,
    private val sessionRepository: SessionDataStoreRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "HotelLocalClient"

        @Volatile
        private var INSTANCE: HotelLocalClient? = null

        fun getInstance(
            context: Context,
            hotelDao: HotelDao,
            sessionRepository: SessionDataStoreRepository
        ): HotelLocalClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HotelLocalClient(
                    context.applicationContext,
                    hotelDao,
                    sessionRepository
                ).also { INSTANCE = it }
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var syncJob: Job? = null

    private val _syncInfo = MutableStateFlow(ClientSyncInfo())
    val syncInfo: StateFlow<ClientSyncInfo> = _syncInfo.asStateFlow()

    private var serverIp: String = "192.168.1.100"
    private var serverPort: Int = 8080
    private var reconnectAttempts = 0

    init {
        // Collect pending sync count from Room Database
        scope.launch {
            hotelDao.getPendingSyncCount().collect { count ->
                _syncInfo.value = _syncInfo.value.copy(pendingOperationsCount = count)
            }
        }
    }

    fun configureServer(ip: String, port: Int) {
        serverIp = ip.trim()
        serverPort = port
        _syncInfo.value = _syncInfo.value.copy(
            serverAddress = "http://$serverIp:$serverPort"
        )
    }

    fun connect() {
        if (_syncInfo.value.state == ClientConnectionState.CONECTADO && activeWebSocket != null) {
            return
        }

        reconnectJob?.cancel()
        startWebSocketConnection()
    }

    fun disconnect() {
        reconnectJob?.cancel()
        activeWebSocket?.close(1000, "Client disconnect")
        activeWebSocket = null
        _syncInfo.value = _syncInfo.value.copy(
            state = ClientConnectionState.DESCONECTADO,
            errorMessage = "Desconectado manualmente"
        )
    }

    fun triggerSync() {
        scope.launch {
            pushPendingOfflineOperations()
            pullDataFromServer()
        }
    }

    private fun startWebSocketConnection() {
        val wsUrl = "ws://$serverIp:$serverPort/ws"
        Log.i(TAG, "Connecting WebSocket to $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected successfully!")
                reconnectAttempts = 0
                _syncInfo.value = _syncInfo.value.copy(
                    state = ClientConnectionState.CONECTADO,
                    serverAddress = "http://$serverIp:$serverPort",
                    errorMessage = null
                )

                // Register client identity with server
                scope.launch {
                    val deviceId = sessionRepository.getDeviceId() ?: UUID.randomUUID().toString()
                    val deviceName = sessionRepository.getUserName() ?: "Terminal Móvil"
                    val userRole = sessionRepository.getUserRole() ?: "RECEPCION"

                    val registerObj = JSONObject().apply {
                        put("type", "REGISTER")
                        put("deviceId", deviceId)
                        put("deviceName", deviceName)
                        put("role", userRole)
                    }
                    webSocket.send(registerObj.toString())

                    // Trigger initial data pull and push any pending offline operations
                    pullDataFromServer()
                    pushPendingOfflineOperations()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleIncomingWebSocketMessage(text)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                activeWebSocket = null
                _syncInfo.value = _syncInfo.value.copy(
                    state = ClientConnectionState.DESCONECTADO,
                    errorMessage = "Sin conexión al servidor: ${t.message}"
                )
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $reason")
                activeWebSocket = null
                _syncInfo.value = _syncInfo.value.copy(
                    state = ClientConnectionState.DESCONECTADO
                )
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            reconnectAttempts++
            // Progressive exponential backoff: 1s, 2s, 5s, 10s, max 30s
            val delaySeconds = when (reconnectAttempts) {
                1 -> 1L
                2 -> 2L
                3 -> 5L
                4 -> 10L
                else -> 30L
            }
            Log.d(TAG, "Scheduling reconnect in $delaySeconds seconds (attempt $reconnectAttempts)")
            delay(delaySeconds * 1000L)
            if (isActive) {
                startWebSocketConnection()
            }
        }
    }

    private suspend fun handleIncomingWebSocketMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            if (type == "EVENT") {
                val eventName = json.optString("event")
                val payload = json.optJSONObject("payload") ?: JSONObject()
                Log.d(TAG, "Received real-time event: $eventName with payload: $payload")

                when (eventName) {
                    "ROOM_STATUS_CHANGED" -> {
                        val roomId = payload.optLong("roomId")
                        val newStatus = payload.optString("status")
                        val clientName = payload.optString("clientName", "")
                        val room = hotelDao.getRoomById(roomId)
                        if (room != null) {
                            hotelDao.updateRoom(room.copy(status = newStatus, clientName = clientName.ifEmpty { room.clientName }))
                        }
                    }
                    "NEW_COMANDA" -> {
                        val id = payload.optLong("id")
                        val comandaNumber = payload.optString("comandaNumber")
                        val tableNumber = payload.optString("tableNumber")
                        val waiterName = payload.optString("waiterName")
                        val status = payload.optString("status")
                        val itemsJson = payload.optString("itemsJson", "[]")
                        val totalAmount = payload.optDouble("totalAmount", 0.0)

                        val comanda = ComandaEntity(
                            id = id,
                            comandaNumber = comandaNumber,
                            tableNumber = tableNumber,
                            waiterName = waiterName,
                            status = status,
                            itemsJson = itemsJson,
                            totalAmount = totalAmount
                        )
                        hotelDao.insertComanda(comanda)
                        hotelDao.updateTableStatus(tableNumber, TableStatus.OCUPADA, id, waiterName)
                    }
                    "COMANDA_STATUS_CHANGED" -> {
                        val id = payload.optLong("id")
                        val status = payload.optString("status")
                        hotelDao.updateComandaStatus(id, status)
                    }
                    "TABLE_STATUS_CHANGED" -> {
                        val tableNumber = payload.optString("tableNumber")
                        val status = payload.optString("status")
                        hotelDao.updateTableStatus(tableNumber, status, null, null)
                    }
                }
            } else if (type == "DEVICE_REVOKED") {
                sessionRepository.clearSession()
                disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming WS message", e)
        }
    }

    /**
     * Enqueues an operation to be synced to the central server.
     * If online, immediately pushes it; if offline, stores safely in local Room DB.
     */
    fun enqueueSyncOperation(
        operationType: String,
        entityType: String,
        entityId: String,
        payload: JSONObject
    ) {
        scope.launch {
            val operation = OfflineSyncQueueEntity(
                operationId = UUID.randomUUID().toString(),
                operationType = operationType,
                entityType = entityType,
                entityId = entityId,
                payloadJson = payload.toString(),
                status = SyncOperationStatus.PENDING
            )
            hotelDao.insertSyncOperation(operation)

            // Try to push immediately if connected
            if (_syncInfo.value.state == ClientConnectionState.CONECTADO) {
                pushPendingOfflineOperations()
            }
        }
    }

    /**
     * Pushes all pending offline operations to the central server via REST POST /api/sync/push
     */
    suspend fun pushPendingOfflineOperations() {
        val pending = hotelDao.getPendingSyncOperationsList()
        if (pending.isEmpty()) return

        _syncInfo.value = _syncInfo.value.copy(state = ClientConnectionState.SINCRONIZANDO)

        try {
            val operationsArray = JSONArray().apply {
                pending.forEach { op ->
                    put(JSONObject().apply {
                        put("operationId", op.operationId)
                        put("operationType", op.operationType)
                        put("entityType", op.entityType)
                        put("entityId", op.entityId)
                        put("payloadJson", op.payloadJson)
                        put("timestampMillis", op.timestampMillis)
                    })
                }
            }

            val requestBody = JSONObject().apply {
                put("operations", operationsArray)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("http://$serverIp:$serverPort/api/sync/push")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseJson = JSONObject(response.body?.string() ?: "{}")
                val confirmedIds = responseJson.optJSONArray("confirmedOperationIds") ?: JSONArray()

                for (i in 0 until confirmedIds.length()) {
                    val opId = confirmedIds.getString(i)
                    hotelDao.deleteSyncOperationByOperationId(opId)
                }

                _syncInfo.value = _syncInfo.value.copy(
                    state = ClientConnectionState.CONECTADO,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    errorMessage = null
                )
            } else {
                _syncInfo.value = _syncInfo.value.copy(
                    state = ClientConnectionState.CONECTADO,
                    errorMessage = "Error sincronizando: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed pushing offline sync operations", e)
            _syncInfo.value = _syncInfo.value.copy(
                state = ClientConnectionState.DESCONECTADO,
                errorMessage = "Fallo de conexión al sincronizar: ${e.message}"
            )
        }
    }

    /**
     * Pulls the latest snapshot of rooms, tables, and comandas from central server
     */
    suspend fun pullDataFromServer() {
        _syncInfo.value = _syncInfo.value.copy(state = ClientConnectionState.SINCRONIZANDO)

        try {
            val request = Request.Builder()
                .url("http://$serverIp:$serverPort/api/sync/pull")
                .get()
                .build()

            val startTime = System.currentTimeMillis()
            val response = okHttpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")

                // Update Rooms
                val roomsArray = json.optJSONArray("rooms") ?: JSONArray()
                for (i in 0 until roomsArray.length()) {
                    val r = roomsArray.getJSONObject(i)
                    val existing = hotelDao.getRoomById(r.getLong("id"))
                    val updated = (existing ?: RoomEntity(
                        id = r.getLong("id"),
                        roomNumber = r.getString("roomNumber")
                    )).copy(
                        id = r.getLong("id"),
                        roomNumber = r.getString("roomNumber"),
                        status = r.getString("status"),
                        clientName = r.optString("clientName", "").ifEmpty { null },
                        priceCharged = r.optDouble("priceCharged", 0.0),
                        nightlyRate = r.optDouble("nightlyRate", 150.0),
                        rateName = r.optString("rateName", "").ifEmpty { null },
                        checkInTimeMillis = r.optLong("checkInTimeMillis", 0L),
                        checkOutTimeMillis = r.optLong("checkOutTimeMillis", 0L)
                    )
                    hotelDao.insertRoom(updated)
                }

                // Update Tables
                val tablesArray = json.optJSONArray("tables") ?: JSONArray()
                for (i in 0 until tablesArray.length()) {
                    val t = tablesArray.getJSONObject(i)
                    val table = TableEntity(
                        id = t.getLong("id"),
                        tableNumber = t.getString("tableNumber"),
                        capacity = t.optInt("capacity", 4),
                        status = t.optString("status", TableStatus.LIBRE),
                        activeComandaId = if (t.has("activeComandaId") && t.getLong("activeComandaId") != 0L) t.getLong("activeComandaId") else null,
                        currentWaiter = t.optString("currentWaiter", "").ifEmpty { null }
                    )
                    hotelDao.insertTable(table)
                }

                // Update Comandas
                val comandasArray = json.optJSONArray("comandas") ?: JSONArray()
                for (i in 0 until comandasArray.length()) {
                    val c = comandasArray.getJSONObject(i)
                    val comanda = ComandaEntity(
                        id = c.getLong("id"),
                        comandaNumber = c.getString("comandaNumber"),
                        tableNumber = c.getString("tableNumber"),
                        waiterName = c.getString("waiterName"),
                        status = c.getString("status"),
                        itemsJson = c.optString("itemsJson", "[]"),
                        notes = c.optString("notes", ""),
                        totalAmount = c.optDouble("totalAmount", 0.0),
                        createdAtMillis = c.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                    hotelDao.insertComanda(comanda)
                }

                _syncInfo.value = _syncInfo.value.copy(
                    state = ClientConnectionState.CONECTADO,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    latencyMs = latency,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling data from server", e)
            _syncInfo.value = _syncInfo.value.copy(
                state = ClientConnectionState.DESCONECTADO,
                errorMessage = e.message
            )
        }
    }
}
