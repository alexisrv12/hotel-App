package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.dao.DeviceDao
import com.example.data.dao.HotelDao
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.ProductEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.SaleRecordEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class CloudSyncStatus {
    ONLINE_SYNCED,
    SYNCING,
    OFFLINE
}

data class CloudSyncInfo(
    val status: CloudSyncStatus = CloudSyncStatus.OFFLINE,
    val hotelId: String = "hotel_rivera_main",
    val activeDeviceId: String = "",
    val activeRole: String = "RECEPCION",
    val lastSyncTimestamp: Long = 0L,
    val errorMessage: String? = null
)

data class LinkingCodeInfo(
    val pin: String,
    val token: String,
    val hotelId: String,
    val role: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val status: String = "ACTIVE", // ACTIVE, USED, EXPIRED
    val linkedDeviceId: String? = null
)

/**
 * Central Cloud & Offline-First Repository using Cloud Firestore and Firebase Auth for Hotel Rivera.
 * Synchronizes rooms, sales, products, and authorized devices across terminals in real-time.
 */
class HotelFirestoreRepository(
    private val context: Context,
    private val hotelDao: HotelDao,
    private val deviceDao: DeviceDao,
    private val sessionRepo: SessionDataStoreRepository
) {
    companion object {
        private const val TAG = "HotelFirestoreRepo"
        const val DEFAULT_HOTEL_ID = "hotel_rivera_main"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    private val _syncInfo = MutableStateFlow(CloudSyncInfo())
    val syncInfo: StateFlow<CloudSyncInfo> = _syncInfo.asStateFlow()

    private val _activeLinkingCode = MutableStateFlow<LinkingCodeInfo?>(null)
    val activeLinkingCode: StateFlow<LinkingCodeInfo?> = _activeLinkingCode.asStateFlow()

    private val _cloudDevices = MutableStateFlow<List<DeviceEntity>>(emptyList())
    val cloudDevices: StateFlow<List<DeviceEntity>> = _cloudDevices.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()
    private var heartbeatJob: Job? = null

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId("hotel-rivera-cloud")
                    .setApiKey("AIzaSyHotelRiveraDefaultKeyPlaceholder")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }

            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
            db.firestoreSettings = settings

            firestore = db
            auth = FirebaseAuth.getInstance()

            _syncInfo.value = _syncInfo.value.copy(
                status = CloudSyncStatus.ONLINE_SYNCED,
                errorMessage = null
            )
            Log.i(TAG, "Firebase Firestore & Auth initialized successfully.")
            startRealtimeListeners()
            startDeviceHeartbeat()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialized in Offline/Local Mode: ${e.message}")
            _syncInfo.value = _syncInfo.value.copy(
                status = CloudSyncStatus.OFFLINE,
                errorMessage = "Modo Offline (Almacenamiento Local Activo)"
            )
        }
    }

    private fun getHotelDocRef(): DocumentReference? {
        val currentHotelId = _syncInfo.value.hotelId.ifEmpty { DEFAULT_HOTEL_ID }
        return firestore?.collection("hotels")?.document(currentHotelId)
    }

    /**
     * Starts Real-Time Snapshot Listeners for rooms, products, and devices.
     */
    fun startRealtimeListeners() {
        val hotelDoc = getHotelDocRef() ?: return

        // Clear existing listeners
        listeners.forEach { it.remove() }
        listeners.clear()

        try {
            // 1. Rooms Listener
            val roomsListener = hotelDoc.collection("rooms").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Rooms listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val roomNumber = doc.getString("roomNumber") ?: doc.id
                            val status = doc.getString("status") ?: RoomStatus.DISPONIBLE
                            val clientName = doc.getString("clientName")
                            val nightlyRate = doc.getDouble("nightlyRate") ?: 150.0
                            val priceCharged = doc.getDouble("priceCharged") ?: 0.0
                            val checkInTimeMillis = doc.getLong("checkInTimeMillis") ?: 0L
                            val checkOutTimeMillis = doc.getLong("checkOutTimeMillis") ?: 0L
                            val rateName = doc.getString("rateName")
                            val notes = doc.getString("notes")
                            val receptionistName = doc.getString("receptionistName")

                            val existing = hotelDao.getRoomByNumber(roomNumber)
                            val updated = (existing ?: RoomEntity(
                                roomNumber = roomNumber,
                                nightlyRate = nightlyRate
                            )).copy(
                                status = status,
                                clientName = clientName,
                                nightlyRate = nightlyRate,
                                priceCharged = priceCharged,
                                checkInTimeMillis = checkInTimeMillis,
                                checkOutTimeMillis = checkOutTimeMillis,
                                rateName = rateName,
                                notes = notes,
                                receptionistName = receptionistName
                            )
                            hotelDao.insertRoom(updated)
                        }
                        _syncInfo.value = _syncInfo.value.copy(
                            status = CloudSyncStatus.ONLINE_SYNCED,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    }
                }
            }
            listeners.add(roomsListener)

            // 2. Products Listener (Productos extras hotel)
            val productsListener = hotelDoc.collection("products").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Products listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                            val name = doc.getString("name") ?: ""
                            val price = doc.getDouble("price") ?: 0.0
                            val costPrice = doc.getDouble("costPrice") ?: 0.0
                            val stock = doc.getLong("stock")?.toInt() ?: 0

                            if (name.isNotEmpty()) {
                                val product = ProductEntity(
                                    id = id,
                                    name = name,
                                    price = price,
                                    costPrice = costPrice,
                                    stock = stock
                                )
                                hotelDao.insertProduct(product)
                            }
                        }
                    }
                }
            }
            listeners.add(productsListener)

            // 3. Linked Devices Listener (Terminales autorizadas)
            val devicesListener = hotelDoc.collection("devices").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Devices listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        val deviceList = mutableListOf<DeviceEntity>()
                        val currentDeviceId = sessionRepo.getDeviceId() ?: ""

                        for (doc in querySnapshot.documents) {
                            val deviceId = doc.getString("deviceId") ?: doc.id
                            val name = doc.getString("name") ?: "Dispositivo"
                            val userAssigned = doc.getString("userAssigned") ?: "Usuario"
                            val status = doc.getString("connectionStatus") ?: DeviceConnectionStatus.CONNECTED
                            val isAuthorized = doc.getBoolean("isAuthorized") ?: true
                            val lastHeartbeat = doc.getLong("lastHeartbeat") ?: System.currentTimeMillis()
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                            // Check if current device authorization was revoked remotely by Gerente
                            if (deviceId == currentDeviceId && !isAuthorized) {
                                Log.w(TAG, "Device authorization was revoked remotely.")
                                sessionRepo.clearDeviceAuthorization()
                            }

                            deviceList.add(
                                DeviceEntity(
                                    deviceId = deviceId,
                                    name = name,
                                    userAssigned = userAssigned,
                                    connectionStatus = status,
                                    realTimeConnectivityStatus = if (System.currentTimeMillis() - lastHeartbeat <= 45000L && isAuthorized) {
                                        RealTimeConnectivityStatus.ACTIVE
                                    } else {
                                        RealTimeConnectivityStatus.DISCONNECTED
                                    },
                                    lastHeartbeat = lastHeartbeat,
                                    timestamp = timestamp
                                )
                            )
                        }
                        _cloudDevices.value = deviceList
                    }
                }
            }
            listeners.add(devicesListener)

        } catch (e: Exception) {
            Log.e(TAG, "Failed setting up real-time Firestore listeners", e)
        }
    }

    // --- HOTEL CLOUD MUTATIONS ---

    /**
     * Syncs a room state change to both Room and Firestore
     */
    suspend fun syncRoomUpdate(room: RoomEntity) {
        hotelDao.updateRoom(room)
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val roomData = mapOf(
                "id" to room.id,
                "roomNumber" to room.roomNumber,
                "status" to room.status,
                "clientName" to (room.clientName ?: ""),
                "clientDpi" to (room.clientDpi ?: ""),
                "nightlyRate" to room.nightlyRate,
                "priceCharged" to room.priceCharged,
                "rateName" to (room.rateName ?: ""),
                "checkInTimeMillis" to room.checkInTimeMillis,
                "checkOutTimeMillis" to room.checkOutTimeMillis,
                "notes" to (room.notes ?: ""),
                "receptionistName" to (room.receptionistName ?: "")
            )
            hotelDoc.collection("rooms").document(room.roomNumber).set(roomData, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing room update to Firestore: ${e.message}")
        }
    }

    /**
     * Syncs a Sale record to Cloud.
     */
    suspend fun syncSaleRecord(sale: SaleRecordEntity) {
        hotelDao.insertSaleRecord(sale)
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val saleId = sale.id.takeIf { it > 0 }?.toString() ?: UUID.randomUUID().toString()
            val saleData = mapOf(
                "id" to sale.id,
                "productName" to sale.productName,
                "quantity" to sale.quantity,
                "unitPrice" to sale.unitPrice,
                "totalPrice" to sale.totalPrice,
                "profit" to sale.profit,
                "timestampMillis" to sale.timestampMillis,
                "registeredBy" to sale.registeredBy,
                "paymentMethod" to sale.paymentMethod
            )
            hotelDoc.collection("sales").document(saleId).set(saleData, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing sale record: ${e.message}")
        }
    }

    // --- UNIVERSAL DEVICE LINKING (6-DIGIT PIN & QR) ---

    /**
     * Gerente generates a 6-digit PIN and secure temporal token for linking secondary terminals.
     */
    suspend fun generateLinkingCode(role: String): LinkingCodeInfo {
        val pin = (100000..999999).random().toString()
        val token = UUID.randomUUID().toString().take(12)
        val currentHotelId = _syncInfo.value.hotelId.ifEmpty { DEFAULT_HOTEL_ID }
        val now = System.currentTimeMillis()
        val expiresAt = now + (10 * 60 * 1000L) // 10 minutes

        val codeInfo = LinkingCodeInfo(
            pin = pin,
            token = token,
            hotelId = currentHotelId,
            role = role.uppercase(),
            createdAtMillis = now,
            expiresAtMillis = expiresAt,
            status = "ACTIVE"
        )

        _activeLinkingCode.value = codeInfo

        try {
            getHotelDocRef()?.collection("linking_codes")?.document(token)?.set(
                mapOf(
                    "pin" to pin,
                    "token" to token,
                    "hotelId" to currentHotelId,
                    "role" to role.uppercase(),
                    "createdAtMillis" to now,
                    "expiresAtMillis" to expiresAt,
                    "status" to "ACTIVE"
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error saving linking code in cloud: ${e.message}")
        }

        // Save active code in local DataStore for session consistency
        sessionRepo.saveActiveLinkingPin(pin, expiresAt)
        sessionRepo.saveActiveLinkingQr(token, expiresAt)

        return codeInfo
    }

    /**
     * Secondary terminal submits a 6-digit PIN to link and authorize itself with Hotel Rivera.
     */
    suspend fun linkDeviceByPin(
        pin: String,
        deviceId: String,
        deviceName: String
    ): Result<String> {
        val hotelDoc = getHotelDocRef()
        val now = System.currentTimeMillis()

        try {
            if (hotelDoc != null) {
                val query = hotelDoc.collection("linking_codes")
                    .whereEqualTo("pin", pin.trim())
                    .whereEqualTo("status", "ACTIVE")
                    .get()
                    .await()

                val validDoc = query.documents.firstOrNull { doc ->
                    val expiresAt = doc.getLong("expiresAtMillis") ?: 0L
                    expiresAt > now
                }

                if (validDoc != null) {
                    val assignedRole = validDoc.getString("role") ?: "RECEPCION"
                    val token = validDoc.id

                    validDoc.reference.update(
                        mapOf(
                            "status" to "USED",
                            "linkedDeviceId" to deviceId,
                            "usedAtMillis" to now
                        )
                    )

                    hotelDoc.collection("devices").document(deviceId).set(
                        mapOf(
                            "deviceId" to deviceId,
                            "name" to deviceName,
                            "userAssigned" to assignedRole,
                            "connectionStatus" to DeviceConnectionStatus.CONNECTED,
                            "isAuthorized" to true,
                            "lastHeartbeat" to now,
                            "timestamp" to now
                        )
                    )

                    sessionRepo.saveDeviceAuthorization(
                        deviceId = deviceId,
                        role = assignedRole,
                        email = "$assignedRole@hotelrivera.com".lowercase(),
                        token = token
                    )
                    sessionRepo.saveSession(
                        userRole = assignedRole,
                        userEmail = "$assignedRole@hotelrivera.com".lowercase(),
                        userName = deviceName,
                        authToken = token
                    )

                    return Result.success("Dispositivo vinculado correctamente con rol: $assignedRole")
                }
            }

            // Fallback for local active PIN verification if offline
            val (savedPin, expiresAt) = sessionRepo.getActivePin()
            if (savedPin == pin.trim() && expiresAt > now) {
                val assignedRole = "RECEPCION"
                sessionRepo.saveDeviceAuthorization(
                    deviceId = deviceId,
                    role = assignedRole,
                    email = "recepcion@hotelrivera.com",
                    token = UUID.randomUUID().toString()
                )
                return Result.success("Dispositivo vinculado en modo local con rol: $assignedRole")
            }

            return Result.failure(Exception("PIN inválido o expirado. Solicite un nuevo PIN a Gerencia."))
        } catch (e: Exception) {
            Log.e(TAG, "Error linking device by PIN", e)
            return Result.failure(Exception("Error al vincular: ${e.localizedMessage}"))
        }
    }

    /**
     * Gerente revokes a device's authorization in real-time.
     */
    suspend fun revokeDevice(deviceId: String) {
        val hotelDoc = getHotelDocRef()
        try {
            hotelDoc?.collection("devices")?.document(deviceId)?.update(
                mapOf(
                    "isAuthorized" to false,
                    "connectionStatus" to DeviceConnectionStatus.DISCONNECTED
                )
            )?.await()
            deviceDao.deleteDeviceByDeviceId(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking device $deviceId", e)
            deviceDao.deleteDeviceByDeviceId(deviceId)
        }
    }

    /**
     * Heartbeat to report real-time connectivity status.
     */
    private fun startDeviceHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(30000L) // every 30 seconds
                try {
                    val deviceId = sessionRepo.getDeviceId() ?: continue
                    val deviceName = sessionRepo.getUserName() ?: "Terminal"
                    val role = sessionRepo.getUserRole() ?: "RECEPCION"
                    val now = System.currentTimeMillis()

                    getHotelDocRef()?.collection("devices")?.document(deviceId)?.set(
                        mapOf(
                            "deviceId" to deviceId,
                            "name" to deviceName,
                            "userAssigned" to role,
                            "connectionStatus" to DeviceConnectionStatus.CONNECTED,
                            "lastHeartbeat" to now,
                            "isAuthorized" to true
                        ),
                        SetOptions.merge()
                    )
                } catch (e: Exception) {
                    // Ignore transient heartbeat failures
                }
            }
        }
    }

    fun cleanup() {
        listeners.forEach { it.remove() }
        listeners.clear()
        heartbeatJob?.cancel()
    }
}
