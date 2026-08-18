package com.example.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.dao.DeviceDao
import com.example.data.dao.HotelDao
import com.example.data.entities.ComandaEntity
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.InvoiceEntity
import com.example.data.entities.ProductEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.SaleRecordEntity
import com.example.data.entities.SupplyEntity
import com.example.data.entities.SyncOperationType
import com.example.data.entities.TableEntity
import com.example.data.entities.TableStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.MessageDigest
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

enum class LocalServerStatus {
    DETENIDO,
    INICIANDO,
    ESPERANDO_DISPOSITIVOS,
    ACTIVO,
    ERROR
}

data class ConnectedClientSession(
    val deviceId: String,
    val deviceName: String,
    val role: String,
    val ipAddress: String,
    val connectedAtMillis: Long = System.currentTimeMillis(),
    var lastHeartbeatMillis: Long = System.currentTimeMillis(),
    val socket: Socket,
    val outputStream: OutputStream
)

data class ActiveLinkingCode(
    val token: String,
    val pin: String,
    val role: String,
    val expiresAtMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAtMillis
    fun remainingSeconds(): Long = ((expiresAtMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
}

class HotelLocalServer(
    private val context: Context,
    private val hotelDao: HotelDao,
    private val deviceDao: DeviceDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "HotelLocalServer"
        const val DEFAULT_PORT = 8080
        const val BUSINESS_ID = "HOTEL_RIVERA"
        const val WEBSOCKET_MAGIC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

        @Volatile
        private var INSTANCE: HotelLocalServer? = null

        fun getInstance(
            context: Context,
            hotelDao: HotelDao,
            deviceDao: DeviceDao
        ): HotelLocalServer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HotelLocalServer(context.applicationContext, hotelDao, deviceDao).also {
                    INSTANCE = it
                }
            }
        }
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _status = MutableStateFlow(LocalServerStatus.DETENIDO)
    val status: StateFlow<LocalServerStatus> = _status.asStateFlow()

    private val _serverIp = MutableStateFlow("127.0.0.1")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(DEFAULT_PORT)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _activeLinkingCode = MutableStateFlow<ActiveLinkingCode?>(null)
    val activeLinkingCode: StateFlow<ActiveLinkingCode?> = _activeLinkingCode.asStateFlow()

    private val _connectedClients = MutableStateFlow<List<ConnectedClientSession>>(emptyList())
    val connectedClients: StateFlow<List<ConnectedClientSession>> = _connectedClients.asStateFlow()

    private val activeClientsMap = ConcurrentHashMap<String, ConnectedClientSession>()
    private val authorizedTokens = ConcurrentHashMap<String, String>() // token -> role

    init {
        _serverIp.value = NetworkUtils.getLocalIpAddress(context)
        // Generate an initial linking PIN & QR token
        generateNewLinkingCode(role = "RECEPCION", validityMinutes = 10)
    }

    /**
     * Generates a dynamic 6-digit PIN and secure token with expiration for device linking.
     */
    fun generateNewLinkingCode(role: String = "RECEPCION", validityMinutes: Int = 10): ActiveLinkingCode {
        val pin = String.format("%06d", Random.nextInt(100000, 999999))
        val token = "HR-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val expiresAt = System.currentTimeMillis() + (validityMinutes * 60 * 1000L)
        val linkingCode = ActiveLinkingCode(
            token = token,
            pin = pin,
            role = role,
            expiresAtMillis = expiresAt
        )
        _activeLinkingCode.value = linkingCode
        return linkingCode
    }

    fun startServer(port: Int = _serverPort.value) {
        if (_status.value == LocalServerStatus.ACTIVO || _status.value == LocalServerStatus.ESPERANDO_DISPOSITIVOS) {
            Log.d(TAG, "Server already running on port ${_serverPort.value}")
            return
        }

        _status.value = LocalServerStatus.INICIANDO
        _serverPort.value = port
        _serverIp.value = NetworkUtils.getLocalIpAddress(context)

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _status.value = if (activeClientsMap.isEmpty()) LocalServerStatus.ESPERANDO_DISPOSITIVOS else LocalServerStatus.ACTIVO
                Log.i(TAG, "Local Hotel Server listening at http://${_serverIp.value}:$port")

                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        launch {
                            handleIncomingConnection(clientSocket)
                        }
                    } catch (e: SocketException) {
                        if (!isActive) break
                    } catch (e: Exception) {
                        Log.e(TAG, "Error accepting client connection", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server on port $port", e)
                _status.value = LocalServerStatus.ERROR
            }
        }
    }

    fun stopServer() {
        try {
            activeClientsMap.values.forEach { client ->
                try {
                    client.socket.close()
                } catch (_: Exception) {}
            }
            activeClientsMap.clear()
            _connectedClients.value = emptyList()

            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
            _status.value = LocalServerStatus.DETENIDO
            Log.i(TAG, "Local Hotel Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
            _status.value = LocalServerStatus.DETENIDO
        }
    }

    fun setPort(newPort: Int) {
        if (newPort in 1024..65535) {
            val wasRunning = _status.value == LocalServerStatus.ACTIVO || _status.value == LocalServerStatus.ESPERANDO_DISPOSITIVOS
            if (wasRunning) {
                stopServer()
                startServer(newPort)
            } else {
                _serverPort.value = newPort
            }
        }
    }

    fun revokeDevice(deviceId: String) {
        scope.launch {
            val client = activeClientsMap[deviceId]
            if (client != null) {
                try {
                    val revokeEvent = JSONObject().apply {
                        put("type", "DEVICE_REVOKED")
                        put("message", "Este dispositivo ha sido desvinculado por el Gerente.")
                    }
                    sendWebSocketFrame(client.outputStream, revokeEvent.toString())
                    client.socket.close()
                } catch (_: Exception) {}
                activeClientsMap.remove(deviceId)
                _connectedClients.value = activeClientsMap.values.toList()
                updateServerStatus()
            }
            // Update in Room database
            deviceDao.deleteDeviceByDeviceId(deviceId)
        }
    }

    private fun updateServerStatus() {
        if (_status.value != LocalServerStatus.DETENIDO && _status.value != LocalServerStatus.ERROR) {
            _status.value = if (activeClientsMap.isEmpty()) LocalServerStatus.ESPERANDO_DISPOSITIVOS else LocalServerStatus.ACTIVO
        }
    }

    private suspend fun handleIncomingConnection(socket: Socket) {
        val clientIp = socket.inetAddress.hostAddress ?: "unknown"
        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()
        val reader = BufferedReader(InputStreamReader(inputStream))

        try {
            // Read HTTP request line
            val requestLine = reader.readLine() ?: run {
                socket.close()
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return
            }

            val method = parts[0]
            val path = parts[1]

            // Read Headers
            val headers = mutableMapOf<String, String>()
            var headerLine: String?
            while (reader.readLine().also { headerLine = it } != null) {
                if (headerLine.isNullOrBlank()) break
                val colonIdx = headerLine!!.indexOf(":")
                if (colonIdx > 0) {
                    val key = headerLine!!.substring(0, colonIdx).trim().lowercase()
                    val value = headerLine!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            // Check for WebSocket Upgrade
            val isWebSocket = headers["upgrade"]?.equals("websocket", ignoreCase = true) == true
            if (isWebSocket && path.startsWith("/ws")) {
                handleWebSocketHandshake(socket, inputStream, outputStream, headers, clientIp)
                return
            }

            // Handle HTTP REST Request
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val read = reader.read(buffer, readTotal, contentLength - readTotal)
                    if (read == -1) break
                    readTotal += read
                }
                String(buffer, 0, readTotal)
            } else ""

            handleRestApi(method, path, body, outputStream, clientIp)
            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in connection handler from $clientIp", e)
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun handleRestApi(
        method: String,
        path: String,
        body: String,
        outputStream: OutputStream,
        clientIp: String
    ) {
        try {
            when {
                // 1. Status endpoint
                method == "GET" && path.startsWith("/api/status") -> {
                    val rooms = hotelDao.getAllRooms().first()
                    val responseJson = JSONObject().apply {
                        put("status", "ONLINE")
                        put("hotelName", "Hotel Rivera")
                        put("businessId", BUSINESS_ID)
                        put("serverIp", _serverIp.value)
                        put("serverPort", _serverPort.value)
                        put("serverTime", System.currentTimeMillis())
                        put("totalRooms", rooms.size)
                        put("connectedDevicesCount", activeClientsMap.size)
                    }
                    sendHttpResponse(outputStream, 200, "application/json", responseJson.toString())
                }

                // 2. Device Linking endpoint (PIN / QR Token)
                method == "POST" && path.startsWith("/api/link") -> {
                    val requestJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                    val tokenOrPin = requestJson.optString("code", "").trim()
                    val deviceId = requestJson.optString("deviceId", UUID.randomUUID().toString())
                    val deviceName = requestJson.optString("deviceName", "Dispositivo Android")
                    val requestedRole = requestJson.optString("role", "RECEPCION")

                    val activeCode = _activeLinkingCode.value
                    val isValid = activeCode != null &&
                            !activeCode.isExpired() &&
                            (tokenOrPin.equals(activeCode.pin, ignoreCase = true) ||
                             tokenOrPin.equals(activeCode.token, ignoreCase = true) ||
                             tokenOrPin.contains(activeCode.token, ignoreCase = true))

                    if (isValid) {
                        val sessionToken = "AUTH_" + UUID.randomUUID().toString()
                        val assignedRole = activeCode?.role ?: requestedRole
                        authorizedTokens[sessionToken] = assignedRole

                        // Save in Room Database
                        val deviceEntity = DeviceEntity(
                            deviceId = deviceId,
                            name = deviceName,
                            userAssigned = assignedRole,
                            connectionStatus = DeviceConnectionStatus.CONNECTED,
                            realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                            ipAddress = clientIp,
                            lastHeartbeat = System.currentTimeMillis()
                        )
                        deviceDao.insertDevice(deviceEntity)

                        val responseJson = JSONObject().apply {
                            put("success", true)
                            put("message", "Dispositivo vinculado correctamente")
                            put("authToken", sessionToken)
                            put("role", assignedRole)
                            put("businessId", BUSINESS_ID)
                            put("serverIp", _serverIp.value)
                            put("serverPort", _serverPort.value)
                        }
                        sendHttpResponse(outputStream, 200, "application/json", responseJson.toString())
                    } else {
                        val errorJson = JSONObject().apply {
                            put("success", false)
                            put("message", "Código PIN o Token inválido o expirado.")
                        }
                        sendHttpResponse(outputStream, 401, "application/json", errorJson.toString())
                    }
                }

                // 3. Full Database Sync Pull
                method == "GET" && path.startsWith("/api/sync/pull") -> {
                    val rooms = hotelDao.getAllRooms().first()
                    val timeRates = hotelDao.getAllTimeRates().first()
                    val supplies = hotelDao.getAllSupplies().first()
                    val products = hotelDao.getAllProducts().first()
                    val tables = hotelDao.getAllTables().first()
                    val comandas = hotelDao.getActiveComandas().first()

                    val roomsArray = JSONArray().apply {
                        rooms.forEach { r ->
                            put(JSONObject().apply {
                                put("id", r.id)
                                put("roomNumber", r.roomNumber)
                                put("status", r.status)
                                put("clientName", r.clientName ?: "")
                                put("priceCharged", r.priceCharged)
                                put("nightlyRate", r.nightlyRate)
                                put("rateName", r.rateName ?: "")
                                put("checkInTimeMillis", r.checkInTimeMillis)
                                put("checkOutTimeMillis", r.checkOutTimeMillis)
                            })
                        }
                    }

                    val tablesArray = JSONArray().apply {
                        tables.forEach { t ->
                            put(JSONObject().apply {
                                put("id", t.id)
                                put("tableNumber", t.tableNumber)
                                put("capacity", t.capacity)
                                put("status", t.status)
                                put("activeComandaId", t.activeComandaId ?: 0L)
                                put("currentWaiter", t.currentWaiter ?: "")
                            })
                        }
                    }

                    val comandasArray = JSONArray().apply {
                        comandas.forEach { c ->
                            put(JSONObject().apply {
                                put("id", c.id)
                                put("comandaNumber", c.comandaNumber)
                                put("tableNumber", c.tableNumber)
                                put("waiterName", c.waiterName)
                                put("status", c.status)
                                put("itemsJson", c.itemsJson)
                                put("notes", c.notes ?: "")
                                put("totalAmount", c.totalAmount)
                                put("createdAtMillis", c.createdAtMillis)
                            })
                        }
                    }

                    val responseJson = JSONObject().apply {
                        put("success", true)
                        put("serverTime", System.currentTimeMillis())
                        put("rooms", roomsArray)
                        put("tables", tablesArray)
                        put("comandas", comandasArray)
                    }
                    sendHttpResponse(outputStream, 200, "application/json", responseJson.toString())
                }

                // 4. Offline Queue Push (Batch synchronization with idempotency)
                method == "POST" && path.startsWith("/api/sync/push") -> {
                    val requestJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                    val operationsArray = requestJson.optJSONArray("operations") ?: JSONArray()
                    val confirmedIds = JSONArray()

                    for (i in 0 until operationsArray.length()) {
                        val op = operationsArray.getJSONObject(i)
                        val opId = op.optString("operationId", UUID.randomUUID().toString())
                        val opType = op.optString("operationType")
                        val payloadJson = op.optString("payloadJson", "{}")

                        processSyncOperation(opType, payloadJson)
                        confirmedIds.put(opId)
                    }

                    val responseJson = JSONObject().apply {
                        put("success", true)
                        put("confirmedOperationIds", confirmedIds)
                    }
                    sendHttpResponse(outputStream, 200, "application/json", responseJson.toString())
                }

                // 5. REST Comandas
                method == "POST" && path.startsWith("/api/comandas") -> {
                    val req = JSONObject(body)
                    val comanda = ComandaEntity(
                        comandaNumber = req.optString("comandaNumber", "CMD-${System.currentTimeMillis() % 1000}"),
                        tableNumber = req.optString("tableNumber", "Mesa 1"),
                        waiterName = req.optString("waiterName", "Mesero"),
                        status = req.optString("status", "PENDIENTE"),
                        itemsJson = req.optString("itemsJson", "[]"),
                        notes = req.optString("notes", ""),
                        totalAmount = req.optDouble("totalAmount", 0.0)
                    )
                    val id = hotelDao.insertComanda(comanda)
                    hotelDao.updateTableStatus(comanda.tableNumber, TableStatus.OCUPADA, id, comanda.waiterName)

                    // Broadcast real-time event to kitchen and waiters
                    broadcastWebSocketEvent("NEW_COMANDA", JSONObject().apply {
                        put("id", id)
                        put("comandaNumber", comanda.comandaNumber)
                        put("tableNumber", comanda.tableNumber)
                        put("waiterName", comanda.waiterName)
                        put("status", comanda.status)
                        put("itemsJson", comanda.itemsJson)
                        put("totalAmount", comanda.totalAmount)
                    })

                    sendHttpResponse(outputStream, 200, "application/json", JSONObject().apply {
                        put("success", true)
                        put("id", id)
                    }.toString())
                }

                // 6. Update Room Status
                method == "POST" && path.startsWith("/api/rooms/status") -> {
                    val req = JSONObject(body)
                    val roomId = req.optLong("roomId")
                    val newStatus = req.optString("status", RoomStatus.DISPONIBLE)
                    val clientName = req.optString("clientName", "")

                    val room = hotelDao.getRoomById(roomId)
                    if (room != null) {
                        val updated = room.copy(status = newStatus, clientName = clientName.ifEmpty { room.clientName })
                        hotelDao.updateRoom(updated)

                        // Broadcast to all devices
                        broadcastWebSocketEvent("ROOM_STATUS_CHANGED", JSONObject().apply {
                            put("roomId", roomId)
                            put("roomNumber", updated.roomNumber)
                            put("status", newStatus)
                            put("clientName", updated.clientName ?: "")
                        })
                    }

                    sendHttpResponse(outputStream, 200, "application/json", JSONObject().apply { put("success", true) }.toString())
                }

                else -> {
                    sendHttpResponse(outputStream, 404, "application/json", JSONObject().apply { put("error", "Not Found") }.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in REST processing", e)
            sendHttpResponse(outputStream, 500, "application/json", JSONObject().apply { put("error", e.message) }.toString())
        }
    }

    private suspend fun processSyncOperation(opType: String, payloadJson: String) {
        try {
            val payload = JSONObject(payloadJson)
            when (opType) {
                SyncOperationType.ROOM_STATUS_UPDATE -> {
                    val roomId = payload.optLong("roomId")
                    val status = payload.optString("status")
                    val room = hotelDao.getRoomById(roomId)
                    if (room != null) {
                        hotelDao.updateRoom(room.copy(status = status))
                        broadcastWebSocketEvent("ROOM_STATUS_CHANGED", JSONObject().apply {
                            put("roomId", roomId)
                            put("roomNumber", room.roomNumber)
                            put("status", status)
                        })
                    }
                }
                SyncOperationType.COMANDA_CREATE -> {
                    val comanda = ComandaEntity(
                        comandaNumber = payload.optString("comandaNumber", "CMD-OFFLINE"),
                        tableNumber = payload.optString("tableNumber", "Mesa"),
                        waiterName = payload.optString("waiterName", "Mesero"),
                        status = payload.optString("status", "PENDIENTE"),
                        itemsJson = payload.optString("itemsJson", "[]"),
                        totalAmount = payload.optDouble("totalAmount", 0.0)
                    )
                    val id = hotelDao.insertComanda(comanda)
                    hotelDao.updateTableStatus(comanda.tableNumber, TableStatus.OCUPADA, id, comanda.waiterName)
                    broadcastWebSocketEvent("NEW_COMANDA", payload)
                }
                SyncOperationType.COMANDA_STATUS_UPDATE -> {
                    val id = payload.optLong("id")
                    val status = payload.optString("status")
                    hotelDao.updateComandaStatus(id, status)
                    broadcastWebSocketEvent("COMANDA_STATUS_CHANGED", payload)
                }
                SyncOperationType.TABLE_STATUS_UPDATE -> {
                    val tableNumber = payload.optString("tableNumber")
                    val status = payload.optString("status")
                    hotelDao.updateTableStatus(tableNumber, status, null, null)
                    broadcastWebSocketEvent("TABLE_STATUS_CHANGED", payload)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing sync operation", e)
        }
    }

    private fun sendHttpResponse(output: OutputStream, statusCode: Int, contentType: String, body: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Internal Server Error"
        }
        val bytes = body.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    // --- WEBSOCKET HANDSHAKE AND FRAME HANDLING (RFC 6455) ---
    private fun handleWebSocketHandshake(
        socket: Socket,
        input: InputStream,
        output: OutputStream,
        headers: Map<String, String>,
        clientIp: String
    ) {
        val wsKey = headers["sec-websocket-key"] ?: run {
            socket.close()
            return
        }

        // Accept Key Calculation
        val sha1 = MessageDigest.getInstance("SHA-1")
        val hash = sha1.digest((wsKey + WEBSOCKET_MAGIC_GUID).toByteArray(Charsets.UTF_8))
        val acceptKey = Base64.encodeToString(hash, Base64.NO_WRAP)

        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"

        output.write(response.toByteArray(Charsets.UTF_8))
        output.flush()

        // Create client session
        val tempId = UUID.randomUUID().toString()
        val session = ConnectedClientSession(
            deviceId = tempId,
            deviceName = "Terminal $clientIp",
            role = "CLIENT",
            ipAddress = clientIp,
            socket = socket,
            outputStream = output
        )
        activeClientsMap[tempId] = session
        _connectedClients.value = activeClientsMap.values.toList()
        updateServerStatus()

        Log.i(TAG, "WebSocket client connected from $clientIp")

        // Start listening to WebSocket binary/text frames
        scope.launch {
            try {
                while (isActive && !socket.isClosed) {
                    val frameText = readWebSocketTextFrame(input) ?: break
                    handleWebSocketMessage(session, frameText)
                }
            } catch (_: Exception) {}
            finally {
                activeClientsMap.remove(session.deviceId)
                _connectedClients.value = activeClientsMap.values.toList()
                updateServerStatus()
                try { socket.close() } catch (_: Exception) {}
                Log.i(TAG, "WebSocket client disconnected: ${session.deviceId}")
            }
        }
    }

    private fun handleWebSocketMessage(session: ConnectedClientSession, messageText: String) {
        session.lastHeartbeatMillis = System.currentTimeMillis()
        try {
            val json = JSONObject(messageText)
            val type = json.optString("type")
            when (type) {
                "REGISTER" -> {
                    val deviceId = json.optString("deviceId", session.deviceId)
                    val deviceName = json.optString("deviceName", session.deviceName)
                    val role = json.optString("role", session.role)

                    activeClientsMap.remove(session.deviceId)
                    val updatedSession = session.copy(deviceId = deviceId, deviceName = deviceName, role = role)
                    activeClientsMap[deviceId] = updatedSession
                    _connectedClients.value = activeClientsMap.values.toList()
                    updateServerStatus()
                }
                "PING" -> {
                    sendWebSocketFrame(session.outputStream, JSONObject().apply {
                        put("type", "PONG")
                        put("serverTime", System.currentTimeMillis())
                    }.toString())
                }
                "EVENT" -> {
                    val eventName = json.optString("event")
                    val payload = json.optJSONObject("payload") ?: JSONObject()
                    broadcastWebSocketEvent(eventName, payload)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling WS message", e)
        }
    }

    fun broadcastWebSocketEvent(eventType: String, payload: JSONObject) {
        val eventObj = JSONObject().apply {
            put("type", "EVENT")
            put("event", eventType)
            put("timestamp", System.currentTimeMillis())
            put("payload", payload)
        }
        val text = eventObj.toString()

        activeClientsMap.values.forEach { client ->
            scope.launch {
                try {
                    sendWebSocketFrame(client.outputStream, text)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send WS event to client ${client.deviceId}")
                }
            }
        }
    }

    private fun sendWebSocketFrame(output: OutputStream, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val frame = ByteArrayOutputStream()

        // Opcode 0x1 (Text Frame), FIN bit set (0x80) -> 0x81
        frame.write(0x81)

        val length = bytes.size
        when {
            length <= 125 -> {
                frame.write(length)
            }
            length <= 65535 -> {
                frame.write(126)
                frame.write((length shr 8) and 0xFF)
                frame.write(length and 0xFF)
            }
            else -> {
                frame.write(127)
                for (i in 7 downTo 0) {
                    frame.write(((length.toLong() shr (i * 8)) and 0xFF).toInt())
                }
            }
        }
        frame.write(bytes)
        synchronized(output) {
            output.write(frame.toByteArray())
            output.flush()
        }
    }

    private fun readWebSocketTextFrame(input: InputStream): String? {
        val b1 = input.read()
        if (b1 == -1) return null
        val b2 = input.read()
        if (b2 == -1) return null

        val opcode = b1 and 0x0F
        if (opcode == 0x8) { // Close frame
            return null
        }

        val isMasked = (b2 and 0x80) != 0
        var payloadLength = (b2 and 0x7F).toLong()

        if (payloadLength == 126L) {
            val byte1 = input.read()
            val byte2 = input.read()
            if (byte1 == -1 || byte2 == -1) return null
            payloadLength = ((byte1 shl 8) or byte2).toLong()
        } else if (payloadLength == 127L) {
            payloadLength = 0
            for (i in 0 until 8) {
                val b = input.read()
                if (b == -1) return null
                payloadLength = (payloadLength shl 8) or b.toLong()
            }
        }

        val maskingKey = ByteArray(4)
        if (isMasked) {
            var read = 0
            while (read < 4) {
                val r = input.read(maskingKey, read, 4 - read)
                if (r == -1) return null
                read += r
            }
        }

        val payload = ByteArray(payloadLength.toInt())
        var totalRead = 0
        while (totalRead < payloadLength) {
            val r = input.read(payload, totalRead, (payloadLength - totalRead).toInt())
            if (r == -1) return null
            totalRead += r
        }

        if (isMasked) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor maskingKey[i % 4].toInt()).toByte()
            }
        }

        return String(payload, Charsets.UTF_8)
    }
}
