package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.util.*

/**
 * Data model representing local device Wi-Fi and IP connection details.
 */
data class WifiIpInfo(
    val isWifiConnected: Boolean = false,
    val localIpAddress: String = "127.0.0.1",
    val subnetMask: String = "255.255.255.0",
    val gatewayIp: String = "192.168.1.1",
    val wifiSsid: String = "Hotel-Rivera-Network",
    val serverPort: Int = 8888,
    val isServerRunning: Boolean = false
)

/**
 * Data model for devices discovered across the local Wi-Fi / LAN network.
 */
data class DiscoveredLanDevice(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int,
    val role: String,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val responseTimeMs: Long = 0L,
    val isHostServer: Boolean = false
)

/**
 * Manages local network (Wi-Fi / Ethernet LAN) discovery, IP socket communication,
 * and direct device-to-device linking for Hotel Rivera terminals.
 */
class LocalNetworkManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalNetworkManager"
        const val DEFAULT_PORT = 8888
        const val BROADCAST_PORT = 8889
        private const val SOCKET_TIMEOUT_MS = 2500
        private const val BROADCAST_INTERVAL_MS = 4000L
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _wifiIpInfo = MutableStateFlow(WifiIpInfo())
    val wifiIpInfo: StateFlow<WifiIpInfo> = _wifiIpInfo.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredLanDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredLanDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var isServerActive = false
    private var broadcastJob: Job? = null
    private var broadcastListenerJob: Job? = null
    private var udpSocket: DatagramSocket? = null

    // Callback when a remote device pairs via Wi-Fi IP socket
    var onDevicePairingReceived: ((DiscoveredLanDevice) -> Unit)? = null

    init {
        refreshNetworkInfo()
        startLocalServer(DEFAULT_PORT)
        startBroadcastListener()
    }

    /**
     * Refreshes local network connectivity and IP address.
     */
    fun refreshNetworkInfo() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

        val ip = getLocalIpAddress()
        val ssid = getWifiSsid()

        _wifiIpInfo.value = _wifiIpInfo.value.copy(
            isWifiConnected = isWifi,
            localIpAddress = ip,
            wifiSsid = ssid,
            isServerRunning = isServerActive
        )
    }

    /**
     * Starts the lightweight TCP local server to listen for pairing requests and heartbeats.
     */
    fun startLocalServer(port: Int = DEFAULT_PORT) {
        if (isServerActive && serverSocket?.isClosed == false) return

        coroutineScope.launch {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                    soTimeout = 0 // Infinite accept timeout
                }
                isServerActive = true
                _wifiIpInfo.value = _wifiIpInfo.value.copy(
                    isServerRunning = true,
                    serverPort = port
                )
                Log.i(TAG, "Local TCP Server started on port $port")

                while (isServerActive && serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        handleClientConnection(clientSocket)
                    } catch (e: Exception) {
                        if (isServerActive) {
                            Log.w(TAG, "Server socket accept warning: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting local TCP server on port $port: ${e.message}")
                isServerActive = false
                _wifiIpInfo.value = _wifiIpInfo.value.copy(isServerRunning = false)
            }
        }
    }

    /**
     * Handles incoming client connection from another hotel terminal over Wi-Fi.
     */
    private fun handleClientConnection(socket: Socket) {
        coroutineScope.launch {
            try {
                socket.soTimeout = SOCKET_TIMEOUT_MS
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val message = reader.readLine()
                if (!message.isNullOrBlank()) {
                    Log.d(TAG, "Received TCP message from ${socket.inetAddress.hostAddress}: $message")
                    val json = JSONObject(message)
                    val action = json.optString("action", "PING")

                    when (action) {
                        "PING" -> {
                            val response = JSONObject().apply {
                                put("status", "PONG")
                                put("deviceId", getDeviceId())
                                put("deviceName", "Hotel Rivera Host")
                                put("role", "GERENTE")
                                put("timestamp", System.currentTimeMillis())
                            }
                            writer.println(response.toString())
                        }
                        "PAIR_REQUEST" -> {
                            val clientDeviceId = json.optString("deviceId", "DEV-${System.currentTimeMillis() % 10000}")
                            val clientDeviceName = json.optString("deviceName", "Terminal Wi-Fi")
                            val clientRole = json.optString("role", "RECEPCIÓN")
                            val clientIp = socket.inetAddress.hostAddress ?: json.optString("ipAddress", "192.168.1.1")
                            val clientPort = json.optInt("port", DEFAULT_PORT)

                            val pairedDevice = DiscoveredLanDevice(
                                deviceId = clientDeviceId,
                                deviceName = clientDeviceName,
                                ipAddress = clientIp,
                                port = clientPort,
                                role = clientRole,
                                isHostServer = false
                            )

                            // Send ACK
                            val ack = JSONObject().apply {
                                put("status", "SUCCESS")
                                put("message", "Dispositivo vinculado correctamente vía IP/Wi-Fi")
                                put("hostDeviceId", getDeviceId())
                                put("timestamp", System.currentTimeMillis())
                            }
                            writer.println(ack.toString())

                            withContext(Dispatchers.Main) {
                                onDevicePairingReceived?.invoke(pairedDevice)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error handling client socket connection: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Starts listening for UDP broadcast packets from other terminals on the Wi-Fi network.
     */
    private fun startBroadcastListener() {
        broadcastListenerJob?.cancel()
        broadcastListenerJob = coroutineScope.launch {
            try {
                udpSocket?.close()
                udpSocket = DatagramSocket(BROADCAST_PORT).apply {
                    broadcast = true
                    reuseAddress = true
                }

                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)

                    val senderIp = packet.address.hostAddress ?: continue
                    val localIp = _wifiIpInfo.value.localIpAddress
                    // Ignore our own broadcast
                    if (senderIp == localIp || senderIp == "127.0.0.1") continue

                    val message = String(packet.data, 0, packet.length)
                    try {
                        val json = JSONObject(message)
                        val deviceId = json.optString("deviceId", "DEV-${senderIp.takeLast(4)}")
                        val deviceName = json.optString("deviceName", "Dispositivo $senderIp")
                        val role = json.optString("role", "RECEPCIÓN")
                        val port = json.optInt("port", DEFAULT_PORT)
                        val isHost = json.optBoolean("isHost", false)

                        val device = DiscoveredLanDevice(
                            deviceId = deviceId,
                            deviceName = deviceName,
                            ipAddress = senderIp,
                            port = port,
                            role = role,
                            lastSeenTimestamp = System.currentTimeMillis(),
                            isHostServer = isHost
                        )

                        updateDiscoveredDevice(device)
                    } catch (e: Exception) {
                        Log.d(TAG, "Non-JSON broadcast packet received: $message")
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(TAG, "UDP broadcast listener notice: ${e.message}")
                }
            }
        }
    }

    /**
     * Broadcasts this device's presence across the local subnet so other terminals find it instantly.
     */
    fun startBroadcastingPresence(deviceName: String, role: String, isHost: Boolean = true) {
        broadcastJob?.cancel()
        broadcastJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val broadcastAddress = getBroadcastAddress()
                    val payload = JSONObject().apply {
                        put("action", "DISCOVERY")
                        put("deviceId", getDeviceId())
                        put("deviceName", deviceName)
                        put("role", role)
                        put("port", _wifiIpInfo.value.serverPort)
                        put("isHost", isHost)
                        put("timestamp", System.currentTimeMillis())
                    }.toString()

                    val data = payload.toByteArray()
                    val sendPacket = DatagramPacket(data, data.size, broadcastAddress, BROADCAST_PORT)

                    val socket = DatagramSocket()
                    socket.broadcast = true
                    socket.send(sendPacket)
                    socket.close()
                } catch (e: Exception) {
                    Log.d(TAG, "Broadcast presence error: ${e.message}")
                }
                delay(BROADCAST_INTERVAL_MS)
            }
        }
    }

    fun stopBroadcastingPresence() {
        broadcastJob?.cancel()
        broadcastJob = null
    }

    /**
     * Performs an active scan of the local /24 subnet (e.g. 192.168.1.1 to 192.168.1.254)
     * checking for responding Hotel Rivera terminals on default port.
     */
    fun scanLocalSubnet(onComplete: (() -> Unit)? = null) {
        if (_isScanning.value) return
        _isScanning.value = true

        coroutineScope.launch {
            refreshNetworkInfo()
            val currentIp = _wifiIpInfo.value.localIpAddress
            if (currentIp == "127.0.0.1" || !currentIp.contains(".")) {
                _isScanning.value = false
                withContext(Dispatchers.Main) { onComplete?.invoke() }
                return@launch
            }

            val subnetPrefix = currentIp.substringBeforeLast(".") + "."
            val myLastOctet = currentIp.substringAfterLast(".").toIntOrNull() ?: 1

            // Also send UDP discovery ping
            startBroadcastingPresence("Escaneando...", "DISCOVERY", false)

            val scanJobs = mutableListOf<Job>()
            for (i in 1..254) {
                if (i == myLastOctet) continue
                val targetIp = "$subnetPrefix$i"

                val job = launch {
                    pingDevice(targetIp, DEFAULT_PORT, timeoutMs = 800)?.let { device ->
                        updateDiscoveredDevice(device)
                    }
                }
                scanJobs.add(job)
            }

            scanJobs.joinAll()
            _isScanning.value = false
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    /**
     * Pings a specific IP and port over TCP to check if it's reachable and running the hotel service.
     */
    suspend fun pingDevice(ip: String, port: Int = DEFAULT_PORT, timeoutMs: Int = SOCKET_TIMEOUT_MS): DiscoveredLanDevice? {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                socket.soTimeout = timeoutMs

                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                val pingMsg = JSONObject().apply {
                    put("action", "PING")
                    put("deviceId", getDeviceId())
                    put("timestamp", System.currentTimeMillis())
                }
                writer.println(pingMsg.toString())

                val responseStr = reader.readLine()
                val responseTime = System.currentTimeMillis() - startTime

                if (!responseStr.isNullOrBlank()) {
                    val json = JSONObject(responseStr)
                    DiscoveredLanDevice(
                        deviceId = json.optString("deviceId", "DEV-${ip.takeLast(4)}"),
                        deviceName = json.optString("deviceName", "Dispositivo $ip"),
                        ipAddress = ip,
                        port = port,
                        role = json.optString("role", "RECEPCIÓN"),
                        lastSeenTimestamp = System.currentTimeMillis(),
                        responseTimeMs = responseTime,
                        isHostServer = true
                    )
                } else {
                    // Socket opened successfully even if raw TCP ping
                    DiscoveredLanDevice(
                        deviceId = "DEV-${ip.replace(".", "").takeLast(6)}",
                        deviceName = "Terminal ($ip)",
                        ipAddress = ip,
                        port = port,
                        role = "RECEPCIÓN",
                        lastSeenTimestamp = System.currentTimeMillis(),
                        responseTimeMs = responseTime,
                        isHostServer = false
                    )
                }
            } catch (e: Exception) {
                null
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Sends a pairing request to a host/manager device at a specific IP and port.
     */
    suspend fun sendPairingRequest(
        targetIp: String,
        port: Int = DEFAULT_PORT,
        myDeviceName: String,
        myRole: String
    ): Result<String> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(targetIp, port), 4000)
            socket.soTimeout = 4000

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            val payload = JSONObject().apply {
                put("action", "PAIR_REQUEST")
                put("deviceId", getDeviceId())
                put("deviceName", myDeviceName)
                put("role", myRole)
                put("ipAddress", _wifiIpInfo.value.localIpAddress)
                put("port", _wifiIpInfo.value.serverPort)
                put("timestamp", System.currentTimeMillis())
            }

            writer.println(payload.toString())
            val responseStr = reader.readLine()

            if (!responseStr.isNullOrBlank()) {
                val json = JSONObject(responseStr)
                if (json.optString("status") == "SUCCESS") {
                    val message = json.optString("message", "Dispositivo vinculado correctamente vía IP.")
                    Result.success(message)
                } else {
                    Result.failure(Exception(json.optString("message", "Error de autorización del host.")))
                }
            } else {
                Result.failure(Exception("El servidor remoto no respondió al mensaje de vinculación."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con $targetIp:$port. Verifique que ambos dispositivos estén en la misma red Wi-Fi."))
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun updateDiscoveredDevice(device: DiscoveredLanDevice) {
        val currentList = _discoveredDevices.value.toMutableList()
        val index = currentList.indexOfFirst { it.ipAddress == device.ipAddress }
        if (index >= 0) {
            currentList[index] = device
        } else {
            currentList.add(device)
        }
        _discoveredDevices.value = currentList
    }

    /**
     * Retrieves the device's local Wi-Fi / LAN IP Address.
     */
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                // Ignore loopback and cellular/vpn if wifi preferred
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val sAddr = addr.hostAddress ?: continue
                        // Prioritize local network ranges (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
                        if (sAddr.startsWith("192.168.") || sAddr.startsWith("10.") || sAddr.startsWith("172.")) {
                            return sAddr
                        }
                    }
                }
            }
            // Fallback: check WifiManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.ipAddress?.let { ipInt ->
                if (ipInt != 0) {
                    @Suppress("DEPRECATION")
                    return Formatter.formatIpAddress(ipInt)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting IP: ${e.message}")
        }
        return "192.168.1.105" // Clean fallback for testing/preview
    }

    private fun getWifiSsid(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wifiManager?.connectionInfo
            val ssid = info?.ssid?.replace("\"", "")
            if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") {
                return ssid
            }
        } catch (_: Exception) {}
        return "Wi-Fi Hotel Rivera"
    }

    private fun getBroadcastAddress(): InetAddress {
        try {
            val ip = _wifiIpInfo.value.localIpAddress
            if (ip.contains(".")) {
                val prefix = ip.substringBeforeLast(".")
                return InetAddress.getByName("$prefix.255")
            }
        } catch (_: Exception) {}
        return InetAddress.getByName("255.255.255.255")
    }

    private fun getDeviceId(): String {
        return "HOTEL-DEV-${(Build.MODEL + Build.BOARD).hashCode().toString().takeLast(6).replace("-", "0")}"
    }

    fun cleanup() {
        isServerActive = false
        broadcastJob?.cancel()
        broadcastListenerJob?.cancel()
        try {
            serverSocket?.close()
            udpSocket?.close()
        } catch (_: Exception) {}
    }
}
