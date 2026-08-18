package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.dao.HotelDao
import com.example.data.entities.AuditLogEntity
import com.example.data.entities.ComandaEntity
import com.example.data.entities.ComandaStatus
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.HotelSettingEntity
import com.example.data.entities.OfflineSyncQueueEntity
import com.example.data.entities.ProductEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.SaleRecordEntity
import com.example.data.entities.StayHistoryEntity
import com.example.data.entities.SyncOperationStatus
import com.example.data.entities.SyncOperationType
import com.example.data.entities.TableEntity
import com.example.data.entities.TableStatus
import com.example.data.entities.TimeRateEntity
import com.example.data.entities.UserEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val pendingQueueCount: Int = 0,
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
 * Central Cloud & Offline-First Repository using Cloud Firestore and Firebase Auth.
 * Connects all devices (Gerente, Recepción, Mesero, Cocina, Caja) across 4G/5G mobile networks
 * and any Wi-Fi networks in real-time, scoped securely by hotelId.
 */
class HotelFirestoreRepository(
    private val context: Context,
    private val hotelDao: HotelDao,
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
    private var queueSyncJob: Job? = null
    private var heartbeatJob: Job? = null

    init {
        initializeFirebase()
        observeLocalQueue()
        startPeriodicSync()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                // Initialize default Firebase App if not already present
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId("hotel-rivera-cloud")
                    .setApiKey("AIzaSyHotelRiveraDefaultKeyPlaceholder")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }

            val db = FirebaseFirestore.getInstance()
            // Enable offline persistence with unlimited cache
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(PersistentCacheSettings.CACHE_SIZE_UNLIMITED)
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
            Log.i(TAG, "Firebase Firestore & Auth initialized successfully with offline cache.")
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
     * Starts Real-Time Snapshot Listeners for all collections scoped by hotelId.
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

            // 2. Tables Listener (Mesas)
            val tablesListener = hotelDoc.collection("tables").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Tables listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val tableNumber = doc.getString("tableNumber") ?: doc.id
                            val status = doc.getString("status") ?: TableStatus.LIBRE
                            val capacity = doc.getLong("capacity")?.toInt() ?: 4
                            val currentWaiter = doc.getString("currentWaiter")
                            val activeComandaId = doc.getLong("activeComandaId")
                            val occupiedSinceMillis = doc.getLong("occupiedSinceMillis")

                            val existing = hotelDao.getTableByNumber(tableNumber)
                            val updated = (existing ?: TableEntity(
                                tableNumber = tableNumber,
                                capacity = capacity
                            )).copy(
                                status = status,
                                capacity = capacity,
                                currentWaiter = currentWaiter,
                                activeComandaId = activeComandaId,
                                occupiedSinceMillis = occupiedSinceMillis
                            )
                            hotelDao.insertTable(updated)
                        }
                    }
                }
            }
            listeners.add(tablesListener)

            // 3. Comandas Listener (Restaurante / Meseros / Cocina / Caja)
            val comandasListener = hotelDoc.collection("comandas").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Comandas listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong().let { if (it < 0) -it else it })
                            val comandaNumber = doc.getString("comandaNumber") ?: "CMD-${doc.id.take(4)}"
                            val tableNumber = doc.getString("tableNumber") ?: "1"
                            val waiterName = doc.getString("waiterName") ?: "Mesero"
                            val status = doc.getString("status") ?: ComandaStatus.PENDIENTE
                            val itemsJson = doc.getString("itemsJson") ?: "[]"
                            val notes = doc.getString("notes")
                            val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                            val createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                            val updatedAtMillis = doc.getLong("updatedAtMillis") ?: System.currentTimeMillis()
                            val roomId = doc.getLong("roomId")

                            val comanda = ComandaEntity(
                                id = id,
                                comandaNumber = comandaNumber,
                                tableNumber = tableNumber,
                                waiterName = waiterName,
                                status = status,
                                itemsJson = itemsJson,
                                notes = notes,
                                totalAmount = totalAmount,
                                createdAtMillis = createdAtMillis,
                                updatedAtMillis = updatedAtMillis,
                                roomId = roomId,
                                isSynced = true
                            )
                            hotelDao.insertComanda(comanda)
                        }
                    }
                }
            }
            listeners.add(comandasListener)

            // 4. Products Listener (Menú / Productos)
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

            // 5. Linked Devices Listener (Dispositivos Autorizados)
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

    /**
     * Periodically observes the local Room sync queue and syncs pending operations to Cloud Firestore.
     */
    private fun observeLocalQueue() {
        scope.launch {
            hotelDao.getPendingSyncCount().collect { count ->
                _syncInfo.value = _syncInfo.value.copy(pendingQueueCount = count)
            }
        }
    }

    private fun startPeriodicSync() {
        queueSyncJob?.cancel()
        queueSyncJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000L) // every 10 seconds
                syncPendingQueueToCloud()
            }
        }
    }

    /**
     * Processes pending operations from Room's OfflineSyncQueueEntity and uploads them to Firestore.
     */
    suspend fun syncPendingQueueToCloud() {
        val hotelDoc = getHotelDocRef() ?: return
        val pendingList = hotelDao.getPendingSyncOperationsList()
        if (pendingList.isEmpty()) return

        _syncInfo.value = _syncInfo.value.copy(status = CloudSyncStatus.SYNCING)

        for (op in pendingList) {
            try {
                val json = JSONObject(op.payloadJson)
                when (op.operationType) {
                    SyncOperationType.ROOM_CHECKIN, SyncOperationType.ROOM_STATUS_UPDATE, SyncOperationType.ROOM_CHECKOUT -> {
                        val roomNumber = json.optString("roomNumber", op.entityId)
                        val roomData = mutableMapOf<String, Any?>()
                        json.keys().forEach { k -> roomData[k] = json.get(k) }
                        hotelDoc.collection("rooms").document(roomNumber).set(roomData, SetOptions.merge()).await()
                    }
                    SyncOperationType.COMANDA_CREATE, SyncOperationType.COMANDA_STATUS_UPDATE -> {
                        val comandaId = op.entityId
                        val comandaData = mutableMapOf<String, Any?>()
                        json.keys().forEach { k -> comandaData[k] = json.get(k) }
                        hotelDoc.collection("comandas").document(comandaId).set(comandaData, SetOptions.merge()).await()
                    }
                    SyncOperationType.TABLE_STATUS_UPDATE -> {
                        val tableNumber = op.entityId
                        val tableData = mutableMapOf<String, Any?>()
                        json.keys().forEach { k -> tableData[k] = json.get(k) }
                        hotelDoc.collection("tables").document(tableNumber).set(tableData, SetOptions.merge()).await()
                    }
                    SyncOperationType.PAYMENT_REGISTER -> {
                        val saleId = op.entityId.ifEmpty { UUID.randomUUID().toString() }
                        val saleData = mutableMapOf<String, Any?>()
                        json.keys().forEach { k -> saleData[k] = json.get(k) }
                        hotelDoc.collection("sales").document(saleId).set(saleData, SetOptions.merge()).await()
                    }
                }
                // Mark complete and remove from queue
                hotelDao.deleteSyncOperationByOperationId(op.operationId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing operation ${op.operationId}: ${e.message}")
                hotelDao.updateSyncOperation(
                    op.copy(
                        retryCount = op.retryCount + 1,
                        errorMessage = e.message
                    )
                )
            }
        }

        _syncInfo.value = _syncInfo.value.copy(
            status = CloudSyncStatus.ONLINE_SYNCED,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Enqueues an offline-first operation locally and attempts immediate cloud push.
     */
    suspend fun enqueueAndSync(
        operationType: String,
        entityType: String,
        entityId: String,
        payload: JSONObject
    ) {
        val op = OfflineSyncQueueEntity(
            operationId = UUID.randomUUID().toString(),
            operationType = operationType,
            entityType = entityType,
            entityId = entityId,
            payloadJson = payload.toString(),
            timestampMillis = System.currentTimeMillis(),
            status = SyncOperationStatus.PENDING
        )
        hotelDao.insertSyncOperation(op)
        syncPendingQueueToCloud()
    }

    // --- CLOUD MUTATIONS ---

    /**
     * Syncs a room state change to both Room and Firestore
     */
    suspend fun syncRoomUpdate(room: RoomEntity) {
        hotelDao.updateRoom(room)
        val payload = JSONObject().apply {
            put("id", room.id)
            put("roomNumber", room.roomNumber)
            put("status", room.status)
            put("clientName", room.clientName ?: "")
            put("clientDpi", room.clientDpi ?: "")
            put("nightlyRate", room.nightlyRate)
            put("priceCharged", room.priceCharged)
            put("rateName", room.rateName ?: "")
            put("checkInTimeMillis", room.checkInTimeMillis)
            put("checkOutTimeMillis", room.checkOutTimeMillis)
            put("notes", room.notes ?: "")
            put("receptionistName", room.receptionistName ?: "")
        }
        enqueueAndSync(SyncOperationType.ROOM_STATUS_UPDATE, "ROOM", room.roomNumber, payload)
    }

    /**
     * Syncs a Comanda creation or update across Mesero, Cocina, Caja, and Gerente.
     */
    suspend fun syncComanda(comanda: ComandaEntity) {
        hotelDao.insertComanda(comanda)
        val payload = JSONObject().apply {
            put("id", comanda.id)
            put("comandaNumber", comanda.comandaNumber)
            put("tableNumber", comanda.tableNumber)
            put("waiterName", comanda.waiterName)
            put("status", comanda.status)
            put("itemsJson", comanda.itemsJson)
            put("notes", comanda.notes ?: "")
            put("totalAmount", comanda.totalAmount)
            put("createdAtMillis", comanda.createdAtMillis)
            put("updatedAtMillis", comanda.updatedAtMillis)
            put("roomId", comanda.roomId ?: 0L)
        }
        enqueueAndSync(SyncOperationType.COMANDA_CREATE, "COMANDA", comanda.id.toString(), payload)
    }

    /**
     * Syncs a Table (Mesa) status update.
     */
    suspend fun syncTableStatus(table: TableEntity) {
        hotelDao.updateTable(table)
        val payload = JSONObject().apply {
            put("id", table.id)
            put("tableNumber", table.tableNumber)
            put("capacity", table.capacity)
            put("status", table.status)
            put("activeComandaId", table.activeComandaId ?: 0L)
            put("currentWaiter", table.currentWaiter ?: "")
            put("occupiedSinceMillis", table.occupiedSinceMillis ?: 0L)
        }
        enqueueAndSync(SyncOperationType.TABLE_STATUS_UPDATE, "TABLE", table.tableNumber, payload)
    }

    /**
     * Syncs a Sale record to Cashier/Caja and Gerencia.
     */
    suspend fun syncSaleRecord(sale: SaleRecordEntity) {
        hotelDao.insertSaleRecord(sale)
        val payload = JSONObject().apply {
            put("id", sale.id)
            put("productName", sale.productName)
            put("quantity", sale.quantity)
            put("unitPrice", sale.unitPrice)
            put("totalPrice", sale.totalPrice)
            put("profit", sale.profit)
            put("timestampMillis", sale.timestampMillis)
            put("registeredBy", sale.registeredBy)
            put("paymentMethod", sale.paymentMethod)
        }
        enqueueAndSync(SyncOperationType.PAYMENT_REGISTER, "SALE", sale.id.toString(), payload)
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

        // Persist to Cloud Firestore under hotels/{hotelId}/linking_codes/{token}
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

                    // Mark linking code as USED
                    validDoc.reference.update(
                        mapOf(
                            "status" to "USED",
                            "linkedDeviceId" to deviceId,
                            "usedAtMillis" to now
                        )
                    )

                    // Register Device in Cloud Firestore
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

                    // Save session in local DataStore
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
        val hotelDoc = getHotelDocRef() ?: return
        try {
            hotelDoc.collection("devices").document(deviceId).update(
                mapOf(
                    "isAuthorized" to false,
                    "connectionStatus" to DeviceConnectionStatus.DISCONNECTED
                )
            ).await()
            hotelDao.deleteDeviceByDeviceId(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking device $deviceId", e)
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
        queueSyncJob?.cancel()
        heartbeatJob?.cancel()
    }
}
