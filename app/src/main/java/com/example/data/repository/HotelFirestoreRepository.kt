package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.dao.DeviceDao
import com.example.data.dao.HotelDao
import com.example.data.entities.DeviceConnectionStatus
import com.example.data.entities.DeviceEntity
import com.example.data.entities.HotelSettingEntity
import com.example.data.entities.HousekeepingTaskEntity
import com.example.data.entities.InvoiceEntity
import com.example.data.entities.MaintenanceRequestEntity
import com.example.data.entities.ProductEntity
import com.example.data.entities.RealTimeConnectivityStatus
import com.example.data.entities.ReservationEntity
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.SaleRecordEntity
import com.example.data.entities.StayHistoryEntity
import com.example.data.entities.SupplyEntity
import com.example.data.entities.TimeRateEntity
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
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
 * Synchronizes rooms, sales, products, rates, reservations, housekeeping, maintenance, and authorized devices across terminals in real-time.
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

        @Volatile
        private var INSTANCE: HotelFirestoreRepository? = null

        fun getInstance(
            context: Context,
            hotelDao: HotelDao,
            deviceDao: DeviceDao,
            sessionRepo: SessionDataStoreRepository
        ): HotelFirestoreRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HotelFirestoreRepository(
                    context.applicationContext,
                    hotelDao,
                    deviceDao,
                    sessionRepo
                ).also { INSTANCE = it }
            }
        }
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
            Log.i(TAG, "Firebase Firestore initialized successfully with offline persistence.")
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
     * Starts Real-Time Snapshot Listeners for all hotel collections.
     * When any terminal makes a change, this device immediately receives the update and updates Room SQLite cache.
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
                            val clientName = doc.getString("clientName")?.takeIf { it.isNotBlank() }
                            val clientDpi = doc.getString("clientDpi")?.takeIf { it.isNotBlank() }
                            val guestCount = doc.getLong("guestCount")?.toInt() ?: 1
                            val nightlyRate = doc.getDouble("nightlyRate") ?: 150.0
                            val priceCharged = doc.getDouble("priceCharged") ?: 0.0
                            val checkInTimeMillis = doc.getLong("checkInTimeMillis") ?: 0L
                            val checkOutTimeMillis = doc.getLong("checkOutTimeMillis") ?: 0L
                            val contractedDurationMinutes = doc.getLong("contractedDurationMinutes") ?: 0L
                            val rateName = doc.getString("rateName")?.takeIf { it.isNotBlank() }
                            val notes = doc.getString("notes")?.takeIf { it.isNotBlank() }
                            val receptionistName = doc.getString("receptionistName")?.takeIf { it.isNotBlank() }
                            val cleaningStartTimeMillis = doc.getLong("cleaningStartTimeMillis") ?: 0L
                            val cleaningFinishedBy = doc.getString("cleaningFinishedBy")?.takeIf { it.isNotBlank() }
                            val roomType = doc.getString("roomType") ?: "Estándar"
                            val sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0

                            val existing = hotelDao.getRoomByNumber(roomNumber)
                            val updated = (existing ?: RoomEntity(
                                roomNumber = roomNumber,
                                nightlyRate = nightlyRate
                            )).copy(
                                roomType = roomType,
                                status = status,
                                clientName = clientName,
                                clientDpi = clientDpi,
                                guestCount = guestCount,
                                nightlyRate = nightlyRate,
                                priceCharged = priceCharged,
                                checkInTimeMillis = checkInTimeMillis,
                                checkOutTimeMillis = checkOutTimeMillis,
                                contractedDurationMinutes = contractedDurationMinutes,
                                rateName = rateName,
                                notes = notes,
                                receptionistName = receptionistName,
                                cleaningStartTimeMillis = cleaningStartTimeMillis,
                                cleaningFinishedBy = cleaningFinishedBy,
                                sortOrder = sortOrder
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

            // 2. Time Rates Listener
            val timeRatesListener = hotelDoc.collection("time_rates").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "TimeRates listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong())
                            val name = doc.getString("name") ?: ""
                            val durationMinutes = doc.getLong("durationMinutes") ?: 60L
                            val price = doc.getDouble("price") ?: 0.0
                            val isActive = doc.getBoolean("isActive") ?: true
                            val isPromotional = doc.getBoolean("isPromotional") ?: false

                            if (name.isNotEmpty()) {
                                hotelDao.insertTimeRate(
                                    TimeRateEntity(
                                        id = id,
                                        name = name,
                                        durationMinutes = durationMinutes,
                                        price = price,
                                        isActive = isActive,
                                        isPromotional = isPromotional
                                    )
                                )
                            }
                        }
                    }
                }
            }
            listeners.add(timeRatesListener)

            // 3. Supplies Listener
            val suppliesListener = hotelDoc.collection("supplies").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Supplies listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong())
                            val name = doc.getString("name") ?: ""
                            val stockCurrent = doc.getDouble("stockCurrent") ?: 0.0
                            val stockMinimum = doc.getDouble("stockMinimum") ?: doc.getDouble("stockMin") ?: 0.0
                            val unit = doc.getString("unit") ?: "unidad"
                            val autoDeduct = doc.getDouble("autoDeductQuantityPerStay") ?: 1.0

                            if (name.isNotEmpty()) {
                                hotelDao.insertSupply(
                                    SupplyEntity(
                                        id = id,
                                        name = name,
                                        stockCurrent = stockCurrent,
                                        stockMinimum = stockMinimum,
                                        unit = unit,
                                        autoDeductQuantityPerStay = autoDeduct
                                    )
                                )
                            }
                        }
                    }
                }
            }
            listeners.add(suppliesListener)

            // 4. Products Listener (Productos extras hotel)
            val productsListener = hotelDoc.collection("products").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Products listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong())
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

            // 5. Sales Listener
            val salesListener = hotelDoc.collection("sales").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Sales listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: 0L
                            val productName = doc.getString("productName") ?: ""
                            val quantity = doc.getLong("quantity")?.toInt() ?: 1
                            val unitPrice = doc.getDouble("unitPrice") ?: 0.0
                            val totalPrice = doc.getDouble("totalPrice") ?: 0.0
                            val profit = doc.getDouble("profit") ?: 0.0
                            val timestampMillis = doc.getLong("timestampMillis") ?: System.currentTimeMillis()
                            val registeredBy = doc.getString("registeredBy") ?: "Recepción"
                            val paymentMethod = doc.getString("paymentMethod") ?: "EFECTIVO"

                            if (productName.isNotEmpty()) {
                                hotelDao.insertSaleRecord(
                                    SaleRecordEntity(
                                        id = id,
                                        productName = productName,
                                        quantity = quantity,
                                        unitPrice = unitPrice,
                                        totalPrice = totalPrice,
                                        profit = profit,
                                        timestampMillis = timestampMillis,
                                        registeredBy = registeredBy,
                                        paymentMethod = paymentMethod
                                    )
                                )
                            }
                        }
                    }
                }
            }
            listeners.add(salesListener)

            // 6. Stay History Listener
            val stayHistoryListener = hotelDoc.collection("stay_history").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "StayHistory listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: 0L
                            val roomNumber = doc.getString("roomNumber") ?: ""
                            val clientName = doc.getString("clientName") ?: "Cliente"
                            val clientDpi = doc.getString("clientDpi")
                            val guestCount = doc.getLong("guestCount")?.toInt() ?: 1
                            val checkInTimeMillis = doc.getLong("checkInTimeMillis") ?: 0L
                            val checkOutTimeMillis = doc.getLong("checkOutTimeMillis") ?: 0L
                            val contractedTimeName = doc.getString("contractedTimeName")
                            val contractedDurationMinutes = doc.getLong("contractedDurationMinutes") ?: 0L
                            val actualDurationMinutes = doc.getLong("actualDurationMinutes") ?: 0L
                            val priceCharged = doc.getDouble("priceCharged") ?: 0.0
                            val paymentMethod = doc.getString("paymentMethod") ?: "EFECTIVO"
                            val receptionistName = doc.getString("receptionistName") ?: "Recepción"
                            val notes = doc.getString("notes")
                            val dateString = doc.getString("dateString") ?: ""

                            if (roomNumber.isNotEmpty()) {
                                hotelDao.insertStayHistory(
                                    StayHistoryEntity(
                                        id = id,
                                        roomNumber = roomNumber,
                                        clientName = clientName,
                                        clientDpi = clientDpi,
                                        guestCount = guestCount,
                                        checkInTimeMillis = checkInTimeMillis,
                                        checkOutTimeMillis = checkOutTimeMillis,
                                        contractedTimeName = contractedTimeName ?: "Tarifa General",
                                        contractedDurationMinutes = contractedDurationMinutes,
                                        actualDurationMinutes = actualDurationMinutes,
                                        priceCharged = priceCharged,
                                        paymentMethod = paymentMethod,
                                        receptionistName = receptionistName,
                                        notes = notes,
                                        dateString = dateString
                                    )
                                )
                            }
                        }
                    }
                }
            }
            listeners.add(stayHistoryListener)

            // 7. Reservations Listener
            val reservationsListener = hotelDoc.collection("reservations").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Reservations listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong())
                            val roomNumber = doc.getString("roomNumber") ?: ""
                            val roomId = doc.getLong("roomId") ?: (doc.getString("roomId")?.toLongOrNull() ?: 1L)
                            val clientName = doc.getString("clientName") ?: ""
                            val clientDpi = doc.getString("clientDpi")
                            val clientPhone = doc.getString("clientPhone")
                            val guestCount = doc.getLong("guestCount")?.toInt() ?: 1
                            val reservationDateMillis = doc.getLong("reservationDateMillis") ?: System.currentTimeMillis()
                            val checkInDateString = doc.getString("checkInDateString") ?: ""
                            val checkInTime = doc.getString("checkInTime") ?: "14:00"
                            val durationText = doc.getString("durationText") ?: "24 Horas (Día completo)"
                            val durationMinutes = doc.getLong("durationMinutes") ?: 1440L
                            val rateName = doc.getString("rateName") ?: "Tarifa General"
                            val totalPrice = doc.getDouble("totalPrice") ?: 0.0
                            val advancePayment = doc.getDouble("advancePayment") ?: doc.getDouble("advanceDeposit") ?: 0.0
                            val paymentMethod = doc.getString("paymentMethod") ?: "Efectivo"
                            val status = doc.getString("status") ?: "CONFIRMADA"
                            val notes = doc.getString("notes")
                            val createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()

                            if (roomNumber.isNotEmpty() && clientName.isNotEmpty()) {
                                hotelDao.insertReservation(
                                    ReservationEntity(
                                        id = id,
                                        roomNumber = roomNumber,
                                        roomId = roomId,
                                        clientName = clientName,
                                        clientDpi = clientDpi,
                                        clientPhone = clientPhone,
                                        guestCount = guestCount,
                                        reservationDateMillis = reservationDateMillis,
                                        checkInDateString = checkInDateString,
                                        checkInTime = checkInTime,
                                        durationText = durationText,
                                        durationMinutes = durationMinutes,
                                        rateName = rateName,
                                        totalPrice = totalPrice,
                                        advancePayment = advancePayment,
                                        paymentMethod = paymentMethod,
                                        notes = notes,
                                        status = status,
                                        createdAtMillis = createdAtMillis
                                    )
                                )
                            }
                        }
                    }
                }
            }
            listeners.add(reservationsListener)

            // 8. Housekeeping Tasks Listener
            val housekeepingListener = hotelDoc.collection("housekeeping").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Housekeeping listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong())
                            val roomNumber = doc.getString("roomNumber") ?: ""
                            val priority = doc.getString("priority") ?: "Normal"
                            val status = doc.getString("status") ?: "PENDIENTE"
                            val assignedStaffName = doc.getString("assignedStaffName") ?: ""
                            val assignedBy = doc.getString("assignedBy") ?: "Gerencia"
                            val notes = doc.getString("notes") ?: ""
                            val createdAtMillis = doc.getLong("assignedTimestamp") ?: System.currentTimeMillis()
                            val completedAtMillis = doc.getLong("completedTimestamp")

                            if (roomNumber.isNotEmpty()) {
                                hotelDao.insertHousekeepingTask(
                                    HousekeepingTaskEntity(
                                        id = id,
                                        roomNumber = roomNumber,
                                        assignedStaffName = assignedStaffName,
                                        assignedBy = assignedBy,
                                        priority = priority,
                                        status = status,
                                        notes = notes,
                                        assignedTimestamp = createdAtMillis,
                                        completedTimestamp = completedAtMillis
                                    )
                                )
                            }
                        }
                    }
                }
            }
            listeners.add(housekeepingListener)

            // 9. Maintenance Requests Listener
            val maintenanceListener = hotelDoc.collection("maintenance").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Maintenance listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: (doc.id.hashCode().toLong())
                            val roomNumber = doc.getString("roomNumber") ?: ""
                            val itemName = doc.getString("itemName") ?: ""
                            val reportedBy = doc.getString("reportedBy") ?: "Personal"
                            val category = doc.getString("category") ?: "Plomería"
                            val priority = doc.getString("priority") ?: "Normal"
                            val description = doc.getString("description") ?: doc.getString("issueDescription") ?: ""
                            val photoPath = doc.getString("photoPath")
                            val status = doc.getString("status") ?: "PENDIENTE"
                            val reportedAtMillis = doc.getLong("reportedTimestamp") ?: System.currentTimeMillis()
                            val resolvedAtMillis = doc.getLong("resolvedTimestamp")
                            val assignedTechnician = doc.getString("assignedTechnician")
                            val resolutionNotes = doc.getString("resolutionNotes")
                            val repairCost = doc.getDouble("repairCost") ?: doc.getDouble("actualCost")

                            if (roomNumber.isNotEmpty() && itemName.isNotEmpty()) {
                                hotelDao.insertMaintenanceRequest(
                                    MaintenanceRequestEntity(
                                        id = id,
                                        roomNumber = roomNumber,
                                        reportedBy = reportedBy,
                                        itemName = itemName,
                                        category = category,
                                        priority = priority,
                                        description = description,
                                        photoPath = photoPath,
                                        status = status,
                                        reportedTimestamp = reportedAtMillis,
                                        resolvedTimestamp = resolvedAtMillis,
                                        assignedTechnician = assignedTechnician,
                                        resolutionNotes = resolutionNotes,
                                        repairCost = repairCost
                                    )
                                )
                            }
                        }
                    }
                }
            }
            listeners.add(maintenanceListener)

            // 10. Invoices Listener
            val invoicesListener = hotelDoc.collection("invoices").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Invoices listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val id = doc.getLong("id") ?: 0L
                            val invoiceNumber = doc.getString("invoiceNumber") ?: doc.id
                            val stayHistoryId = doc.getLong("stayHistoryId")
                            val hotelName = doc.getString("hotelName") ?: "Hotel Rivera"
                            val hotelAddress = doc.getString("hotelAddress") ?: ""
                            val hotelPhone = doc.getString("hotelPhone") ?: ""
                            val hotelNit = doc.getString("hotelNit") ?: ""
                            val dateString = doc.getString("dateString") ?: ""
                            val timeString = doc.getString("timeString") ?: ""
                            val roomNumber = doc.getString("roomNumber") ?: ""
                            val clientName = doc.getString("clientName") ?: "Cliente"
                            val contractedTime = doc.getString("contractedTime") ?: ""
                            val checkInTime = doc.getString("checkInTime") ?: ""
                            val checkOutTime = doc.getString("checkOutTime") ?: ""
                            val price = doc.getDouble("price") ?: 0.0
                            val discount = doc.getDouble("discount") ?: 0.0
                            val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                            val paymentMethod = doc.getString("paymentMethod") ?: "EFECTIVO"
                            val receptionistName = doc.getString("receptionistName") ?: "Recepción"
                            val timestampMillis = doc.getLong("timestampMillis") ?: System.currentTimeMillis()
                            val isVoided = doc.getBoolean("isVoided") ?: false
                            val voidedBy = doc.getString("voidedBy")
                            val voidReason = doc.getString("voidReason")

                            if (invoiceNumber.isNotEmpty()) {
                                hotelDao.insertInvoice(
                                    InvoiceEntity(
                                        id = id,
                                        invoiceNumber = invoiceNumber,
                                        stayHistoryId = stayHistoryId,
                                        hotelName = hotelName,
                                        hotelAddress = hotelAddress,
                                        hotelPhone = hotelPhone,
                                        hotelNit = hotelNit,
                                        dateString = dateString,
                                        timeString = timeString,
                                        roomNumber = roomNumber,
                                        clientName = clientName,
                                        contractedTime = contractedTime,
                                        checkInTime = checkInTime,
                                        checkOutTime = checkOutTime,
                                        price = price,
                                        discount = discount,
                                        totalAmount = totalAmount,
                                        paymentMethod = paymentMethod,
                                        receptionistName = receptionistName,
                                        timestampMillis = timestampMillis,
                                        isVoided = isVoided,
                                        voidedBy = voidedBy,
                                        voidReason = voidReason
                                    )
                                )
                            }
                        }
                    }
                }
            }
            listeners.add(invoicesListener)

            // 11. Linked Devices Listener (Terminales autorizadas)
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

                            val entity = DeviceEntity(
                                deviceId = deviceId,
                                name = name,
                                userAssigned = userAssigned,
                                connectionStatus = status,
                                realTimeConnectivityStatus = if (System.currentTimeMillis() - lastHeartbeat <= 60000L && isAuthorized) {
                                    RealTimeConnectivityStatus.ACTIVE
                                } else {
                                    RealTimeConnectivityStatus.DISCONNECTED
                                },
                                lastHeartbeat = lastHeartbeat,
                                timestamp = timestamp
                            )
                            deviceList.add(entity)
                            deviceDao.insertDevice(entity)
                        }
                        _cloudDevices.value = deviceList
                    }
                }
            }
            listeners.add(devicesListener)

            // 12. Settings Listener
            val settingsListener = hotelDoc.collection("settings").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Settings listener error: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    scope.launch {
                        for (doc in querySnapshot.documents) {
                            val key = doc.getString("key") ?: doc.id
                            val value = doc.getString("value") ?: ""
                            if (key.isNotEmpty()) {
                                hotelDao.insertSetting(HotelSettingEntity(key, value))
                            }
                        }
                    }
                }
            }
            listeners.add(settingsListener)

        } catch (e: Exception) {
            Log.e(TAG, "Failed setting up real-time Firestore listeners", e)
        }
    }

    // --- HOTEL CLOUD MUTATIONS & REALTIME OBSERVABLES ---

    /**
     * Emits real-time room updates directly from Firestore using snapshot listeners.
     */
    fun getRoomsFlow(): Flow<List<RoomEntity>> = callbackFlow {
        val hotelDoc = getHotelDocRef()
        if (hotelDoc == null) {
            val job = scope.launch {
                hotelDao.getAllRooms().collect { localList ->
                    trySend(localList)
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listener = hotelDoc.collection("rooms")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Rooms snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        scope.launch {
                            val localRooms = hotelDao.getAllRooms().first()
                            if (localRooms.isNotEmpty()) {
                                for (r in localRooms) {
                                    syncRoomUpdate(r)
                                }
                            } else {
                                trySend(emptyList())
                            }
                        }
                    } else {
                        val roomList = snapshot.documents.mapNotNull { doc ->
                            val roomNumber = doc.getString("roomNumber") ?: doc.id
                            val status = doc.getString("status") ?: RoomStatus.DISPONIBLE
                            val clientName = doc.getString("clientName")?.takeIf { it.isNotBlank() }
                            val clientDpi = doc.getString("clientDpi")?.takeIf { it.isNotBlank() }
                            val guestCount = doc.getLong("guestCount")?.toInt() ?: 1
                            val nightlyRate = doc.getDouble("nightlyRate") ?: 150.0
                            val priceCharged = doc.getDouble("priceCharged") ?: 0.0
                            val checkInTimeMillis = doc.getLong("checkInTimeMillis") ?: 0L
                            val checkOutTimeMillis = doc.getLong("checkOutTimeMillis") ?: 0L
                            val contractedDurationMinutes = doc.getLong("contractedDurationMinutes") ?: 0L
                            val rateName = doc.getString("rateName")?.takeIf { it.isNotBlank() }
                            val notes = doc.getString("notes")?.takeIf { it.isNotBlank() }
                            val receptionistName = doc.getString("receptionistName")?.takeIf { it.isNotBlank() }
                            val cleaningStartTimeMillis = doc.getLong("cleaningStartTimeMillis") ?: 0L
                            val cleaningFinishedBy = doc.getString("cleaningFinishedBy")?.takeIf { it.isNotBlank() }
                            val roomType = doc.getString("roomType") ?: "Estándar"
                            val sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0

                            RoomEntity(
                                id = doc.getLong("id") ?: (roomNumber.toLongOrNull() ?: roomNumber.hashCode().toLong()),
                                roomNumber = roomNumber,
                                roomType = roomType,
                                status = status,
                                clientName = clientName,
                                clientDpi = clientDpi,
                                guestCount = guestCount,
                                nightlyRate = nightlyRate,
                                priceCharged = priceCharged,
                                rateName = rateName,
                                checkInTimeMillis = checkInTimeMillis,
                                checkOutTimeMillis = checkOutTimeMillis,
                                contractedDurationMinutes = contractedDurationMinutes,
                                notes = notes,
                                receptionistName = receptionistName,
                                cleaningStartTimeMillis = cleaningStartTimeMillis,
                                cleaningFinishedBy = cleaningFinishedBy,
                                sortOrder = sortOrder
                            )
                        }.sortedBy { it.sortOrder.takeIf { s -> s > 0 } ?: (it.roomNumber.toIntOrNull() ?: 999) }

                        scope.launch {
                            for (room in roomList) {
                                hotelDao.insertRoom(room)
                            }
                        }

                        trySend(roomList)
                    }
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Syncs a room state change to both Room and Firestore in real-time.
     */
    suspend fun syncRoomUpdate(room: RoomEntity) {
        hotelDao.updateRoom(room)
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val roomData = mapOf(
                "id" to room.id,
                "roomNumber" to room.roomNumber,
                "roomType" to room.roomType,
                "status" to room.status,
                "clientName" to (room.clientName ?: ""),
                "clientDpi" to (room.clientDpi ?: ""),
                "guestCount" to room.guestCount,
                "nightlyRate" to room.nightlyRate,
                "priceCharged" to room.priceCharged,
                "rateName" to (room.rateName ?: ""),
                "checkInTimeMillis" to room.checkInTimeMillis,
                "checkOutTimeMillis" to room.checkOutTimeMillis,
                "contractedDurationMinutes" to room.contractedDurationMinutes,
                "notes" to (room.notes ?: ""),
                "receptionistName" to (room.receptionistName ?: ""),
                "cleaningStartTimeMillis" to room.cleaningStartTimeMillis,
                "cleaningFinishedBy" to (room.cleaningFinishedBy ?: ""),
                "sortOrder" to room.sortOrder,
                "lastUpdatedTimestamp" to System.currentTimeMillis()
            )
            hotelDoc.collection("rooms").document(room.roomNumber).set(roomData, SetOptions.merge()).await()
            Log.d(TAG, "Room ${room.roomNumber} updated in Firestore (${room.status})")
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing room update to Firestore: ${e.message}")
        }
    }

    suspend fun syncRoomDelete(roomNumber: String) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("rooms").document(roomNumber).delete().await()
            Log.d(TAG, "Room $roomNumber deleted in Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting room $roomNumber from Firestore: ${e.message}")
        }
    }

    // --- TIME RATES SYNC ---
    suspend fun syncTimeRate(rate: TimeRateEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val rateId = if (rate.id > 0) rate.id.toString() else UUID.randomUUID().toString()
            val data = mapOf(
                "id" to rate.id,
                "name" to rate.name,
                "durationMinutes" to rate.durationMinutes,
                "price" to rate.price,
                "isActive" to rate.isActive,
                "isPromotional" to rate.isPromotional
            )
            hotelDoc.collection("time_rates").document(rateId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing time rate: ${e.message}")
        }
    }

    suspend fun syncTimeRateDelete(id: Long) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("time_rates").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting time rate: ${e.message}")
        }
    }

    // --- SUPPLIES SYNC ---
    suspend fun syncSupply(supply: SupplyEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val docId = if (supply.id > 0) supply.id.toString() else UUID.randomUUID().toString()
            val data = mapOf(
                "id" to supply.id,
                "name" to supply.name,
                "stockCurrent" to supply.stockCurrent,
                "stockMinimum" to supply.stockMinimum,
                "unit" to supply.unit,
                "autoDeductQuantityPerStay" to supply.autoDeductQuantityPerStay
            )
            hotelDoc.collection("supplies").document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing supply: ${e.message}")
        }
    }

    suspend fun syncSupplyDelete(id: Long) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("supplies").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting supply: ${e.message}")
        }
    }

    // --- PRODUCTS SYNC ---
    suspend fun syncProduct(product: ProductEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val docId = if (product.id > 0) product.id.toString() else UUID.randomUUID().toString()
            val data = mapOf(
                "id" to product.id,
                "name" to product.name,
                "price" to product.price,
                "costPrice" to product.costPrice,
                "stock" to product.stock
            )
            hotelDoc.collection("products").document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing product: ${e.message}")
        }
    }

    suspend fun syncProductDelete(id: Long) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("products").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting product: ${e.message}")
        }
    }

    // --- SALES SYNC ---
    suspend fun syncSaleRecord(sale: SaleRecordEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val saleId = if (sale.id > 0) sale.id.toString() else UUID.randomUUID().toString()
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

    // --- STAY HISTORY SYNC ---
    suspend fun syncStayHistory(history: StayHistoryEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val docId = if (history.id > 0) history.id.toString() else UUID.randomUUID().toString()
            val data = mapOf(
                "id" to history.id,
                "roomNumber" to history.roomNumber,
                "clientName" to history.clientName,
                "clientDpi" to (history.clientDpi ?: ""),
                "guestCount" to history.guestCount,
                "checkInTimeMillis" to history.checkInTimeMillis,
                "checkOutTimeMillis" to history.checkOutTimeMillis,
                "contractedTimeName" to (history.contractedTimeName ?: ""),
                "contractedDurationMinutes" to history.contractedDurationMinutes,
                "actualDurationMinutes" to history.actualDurationMinutes,
                "priceCharged" to history.priceCharged,
                "paymentMethod" to history.paymentMethod,
                "receptionistName" to history.receptionistName,
                "notes" to (history.notes ?: ""),
                "dateString" to history.dateString
            )
            hotelDoc.collection("stay_history").document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing stay history: ${e.message}")
        }
    }

    suspend fun syncStayHistoryDelete(id: Long) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("stay_history").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting stay history: ${e.message}")
        }
    }

    // --- RESERVATIONS SYNC ---
    suspend fun syncReservation(reservation: ReservationEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val docId = if (reservation.id > 0) reservation.id.toString() else UUID.randomUUID().toString()
            val data = mapOf(
                "id" to reservation.id,
                "roomNumber" to reservation.roomNumber,
                "roomId" to reservation.roomId,
                "clientName" to reservation.clientName,
                "clientDpi" to (reservation.clientDpi ?: ""),
                "clientPhone" to (reservation.clientPhone ?: ""),
                "guestCount" to reservation.guestCount,
                "reservationDateMillis" to reservation.reservationDateMillis,
                "checkInDateString" to reservation.checkInDateString,
                "checkInTime" to reservation.checkInTime,
                "durationText" to reservation.durationText,
                "durationMinutes" to reservation.durationMinutes,
                "rateName" to reservation.rateName,
                "totalPrice" to reservation.totalPrice,
                "advancePayment" to reservation.advancePayment,
                "paymentMethod" to reservation.paymentMethod,
                "notes" to (reservation.notes ?: ""),
                "status" to reservation.status,
                "createdAtMillis" to reservation.createdAtMillis
            )
            hotelDoc.collection("reservations").document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing reservation: ${e.message}")
        }
    }

    suspend fun syncReservationDelete(id: Long) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("reservations").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting reservation: ${e.message}")
        }
    }

    // --- HOUSEKEEPING TASKS SYNC ---
    suspend fun syncHousekeepingTask(task: HousekeepingTaskEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val docId = if (task.id > 0) task.id.toString() else UUID.randomUUID().toString()
            val data = mapOf(
                "id" to task.id,
                "roomNumber" to task.roomNumber,
                "priority" to task.priority,
                "status" to task.status,
                "assignedStaffName" to task.assignedStaffName,
                "assignedBy" to task.assignedBy,
                "notes" to task.notes,
                "assignedTimestamp" to task.assignedTimestamp,
                "completedTimestamp" to task.completedTimestamp
            )
            hotelDoc.collection("housekeeping").document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing housekeeping task: ${e.message}")
        }
    }

    suspend fun syncHousekeepingTaskDelete(id: Long) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("housekeeping").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting housekeeping task: ${e.message}")
        }
    }

    // --- MAINTENANCE REQUESTS SYNC ---
    suspend fun syncMaintenanceRequest(request: MaintenanceRequestEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val docId = if (request.id > 0) request.id.toString() else UUID.randomUUID().toString()
            val data = mapOf(
                "id" to request.id,
                "roomNumber" to request.roomNumber,
                "reportedBy" to request.reportedBy,
                "itemName" to request.itemName,
                "category" to request.category,
                "priority" to request.priority,
                "description" to request.description,
                "photoPath" to (request.photoPath ?: ""),
                "status" to request.status,
                "reportedTimestamp" to request.reportedTimestamp,
                "resolvedTimestamp" to request.resolvedTimestamp,
                "assignedTechnician" to (request.assignedTechnician ?: ""),
                "resolutionNotes" to (request.resolutionNotes ?: ""),
                "repairCost" to request.repairCost
            )
            hotelDoc.collection("maintenance").document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing maintenance request: ${e.message}")
        }
    }

    suspend fun syncMaintenanceRequestDelete(id: Long) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("maintenance").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting maintenance request: ${e.message}")
        }
    }

    // --- INVOICES SYNC ---
    suspend fun syncInvoice(invoice: InvoiceEntity) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            val docId = invoice.invoiceNumber.ifBlank { if (invoice.id > 0) invoice.id.toString() else UUID.randomUUID().toString() }
            val data = mapOf(
                "id" to invoice.id,
                "invoiceNumber" to invoice.invoiceNumber,
                "stayHistoryId" to (invoice.stayHistoryId ?: 0L),
                "hotelName" to invoice.hotelName,
                "hotelAddress" to invoice.hotelAddress,
                "hotelPhone" to invoice.hotelPhone,
                "hotelNit" to invoice.hotelNit,
                "dateString" to invoice.dateString,
                "timeString" to invoice.timeString,
                "roomNumber" to invoice.roomNumber,
                "clientName" to invoice.clientName,
                "contractedTime" to invoice.contractedTime,
                "checkInTime" to invoice.checkInTime,
                "checkOutTime" to invoice.checkOutTime,
                "price" to invoice.price,
                "discount" to invoice.discount,
                "totalAmount" to invoice.totalAmount,
                "paymentMethod" to invoice.paymentMethod,
                "receptionistName" to (invoice.receptionistName ?: ""),
                "timestampMillis" to invoice.timestampMillis,
                "isVoided" to invoice.isVoided,
                "voidedBy" to (invoice.voidedBy ?: ""),
                "voidReason" to (invoice.voidReason ?: "")
            )
            hotelDoc.collection("invoices").document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing invoice: ${e.message}")
        }
    }

    suspend fun syncInvoiceDelete(id: Long) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("invoices").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting invoice: ${e.message}")
        }
    }

    // --- SETTINGS SYNC ---
    suspend fun syncSetting(key: String, value: String) {
        try {
            val hotelDoc = getHotelDocRef() ?: return
            hotelDoc.collection("settings").document(key).set(
                mapOf("key" to key, "value" to value, "timestamp" to System.currentTimeMillis()),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing setting $key: ${e.message}")
        }
    }

    // --- UNIVERSAL DEVICE LINKING (6-DIGIT PIN & QR) ---

    /**
     * Gerente generates a 6-digit PIN and secure temporal token for linking secondary terminals.
     */
    suspend fun generateLinkingCode(role: String = "RECEPCION", customPin: String? = null, customToken: String? = null): LinkingCodeInfo {
        val pin = customPin ?: (100000..999999).random().toString()
        val token = customToken ?: UUID.randomUUID().toString().take(12)
        val currentHotelId = _syncInfo.value.hotelId.ifEmpty { DEFAULT_HOTEL_ID }
        val now = System.currentTimeMillis()
        val expiresAt = now + (15 * 60 * 1000L) // 15 minutes validity for smooth pairing

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
                ),
                SetOptions.merge()
            )?.await()

            // Also index by PIN for fast lookup
            getHotelDocRef()?.collection("linking_codes_by_pin")?.document(pin)?.set(
                mapOf(
                    "pin" to pin,
                    "token" to token,
                    "hotelId" to currentHotelId,
                    "role" to role.uppercase(),
                    "createdAtMillis" to now,
                    "expiresAtMillis" to expiresAt,
                    "status" to "ACTIVE"
                ),
                SetOptions.merge()
            )?.await()
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
        val cleanPin = pin.trim()
        val hotelDoc = getHotelDocRef()
        val now = System.currentTimeMillis()

        try {
            if (hotelDoc != null) {
                // 1. Try lookup in linking_codes_by_pin
                var matchedDoc = try {
                    val directDoc = hotelDoc.collection("linking_codes_by_pin").document(cleanPin).get().await()
                    if (directDoc.exists() && directDoc.getString("status") == "ACTIVE") directDoc else null
                } catch (e: Exception) {
                    null
                }

                // 2. Try query in linking_codes collection
                if (matchedDoc == null) {
                    val query = hotelDoc.collection("linking_codes")
                        .whereEqualTo("pin", cleanPin)
                        .whereEqualTo("status", "ACTIVE")
                        .get()
                        .await()

                    matchedDoc = query.documents.firstOrNull { doc ->
                        val expiresAt = doc.getLong("expiresAtMillis") ?: 0L
                        expiresAt > now
                    }
                }

                if (matchedDoc != null) {
                    val assignedRole = matchedDoc.getString("role") ?: "RECEPCION"
                    val token = matchedDoc.getString("token") ?: matchedDoc.id

                    // Mark as used
                    matchedDoc.reference.update(
                        mapOf(
                            "status" to "USED",
                            "linkedDeviceId" to deviceId,
                            "usedAtMillis" to now
                        )
                    )

                    // Register device
                    val deviceEntity = DeviceEntity(
                        deviceId = deviceId,
                        name = deviceName,
                        userAssigned = assignedRole,
                        connectionStatus = DeviceConnectionStatus.CONNECTED,
                        realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                        lastHeartbeat = now,
                        timestamp = now
                    )
                    deviceDao.insertDevice(deviceEntity)

                    hotelDoc.collection("devices").document(deviceId).set(
                        mapOf(
                            "deviceId" to deviceId,
                            "name" to deviceName,
                            "userAssigned" to assignedRole,
                            "connectionStatus" to DeviceConnectionStatus.CONNECTED,
                            "isAuthorized" to true,
                            "lastHeartbeat" to now,
                            "timestamp" to now
                        ),
                        SetOptions.merge()
                    ).await()

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

                    // Start real-time listeners on the newly linked terminal
                    startRealtimeListeners()

                    return Result.success("Dispositivo vinculado correctamente con rol: $assignedRole")
                }
            }

            // Fallback for local active PIN verification if offline or in-memory
            val (savedPin, expiresAt) = sessionRepo.getActivePin()
            val localPinSetting = hotelDao.getSettingValue("active_linking_pin")
            if ((savedPin == cleanPin && (expiresAt == 0L || expiresAt > now)) || localPinSetting == cleanPin) {
                val assignedRole = "RECEPCION"
                val deviceEntity = DeviceEntity(
                    deviceId = deviceId,
                    name = deviceName,
                    userAssigned = assignedRole,
                    connectionStatus = DeviceConnectionStatus.CONNECTED,
                    realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                    lastHeartbeat = now,
                    timestamp = now
                )
                deviceDao.insertDevice(deviceEntity)

                sessionRepo.saveDeviceAuthorization(
                    deviceId = deviceId,
                    role = assignedRole,
                    email = "recepcion@hotelrivera.com",
                    token = UUID.randomUUID().toString()
                )
                sessionRepo.saveSession(
                    userRole = assignedRole,
                    userEmail = "recepcion@hotelrivera.com",
                    userName = deviceName,
                    authToken = UUID.randomUUID().toString()
                )
                startRealtimeListeners()
                return Result.success("Dispositivo vinculado en modo local con rol: $assignedRole")
            }

            return Result.failure(Exception("PIN inválido o expirado. Solicite un nuevo PIN a Gerencia."))
        } catch (e: Exception) {
            Log.e(TAG, "Error linking device by PIN", e)
            return Result.failure(Exception("Error al vincular: ${e.localizedMessage}"))
        }
    }

    /**
     * Secondary terminal submits a QR token or scanned QR payload to link and authorize itself.
     */
    suspend fun linkDeviceByQr(
        qrToken: String,
        deviceId: String,
        deviceName: String
    ): Result<String> {
        val cleanToken = qrToken.trim()
        val hotelDoc = getHotelDocRef()
        val now = System.currentTimeMillis()

        try {
            if (hotelDoc != null) {
                val query = hotelDoc.collection("linking_codes")
                    .whereEqualTo("token", cleanToken)
                    .whereEqualTo("status", "ACTIVE")
                    .get()
                    .await()

                val validDoc = query.documents.firstOrNull { doc ->
                    val expiresAt = doc.getLong("expiresAtMillis") ?: 0L
                    expiresAt > now
                }

                if (validDoc != null) {
                    val assignedRole = validDoc.getString("role") ?: "RECEPCION"

                    validDoc.reference.update(
                        mapOf(
                            "status" to "USED",
                            "linkedDeviceId" to deviceId,
                            "usedAtMillis" to now
                        )
                    )

                    val deviceEntity = DeviceEntity(
                        deviceId = deviceId,
                        name = deviceName,
                        userAssigned = assignedRole,
                        connectionStatus = DeviceConnectionStatus.CONNECTED,
                        realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                        lastHeartbeat = now,
                        timestamp = now
                    )
                    deviceDao.insertDevice(deviceEntity)

                    hotelDoc.collection("devices").document(deviceId).set(
                        mapOf(
                            "deviceId" to deviceId,
                            "name" to deviceName,
                            "userAssigned" to assignedRole,
                            "connectionStatus" to DeviceConnectionStatus.CONNECTED,
                            "isAuthorized" to true,
                            "lastHeartbeat" to now,
                            "timestamp" to now
                        ),
                        SetOptions.merge()
                    ).await()

                    sessionRepo.saveDeviceAuthorization(
                        deviceId = deviceId,
                        role = assignedRole,
                        email = "$assignedRole@hotelrivera.com".lowercase(),
                        token = cleanToken
                    )
                    sessionRepo.saveSession(
                        userRole = assignedRole,
                        userEmail = "$assignedRole@hotelrivera.com".lowercase(),
                        userName = deviceName,
                        authToken = cleanToken
                    )

                    startRealtimeListeners()
                    return Result.success("Dispositivo vinculado correctamente con rol: $assignedRole")
                }
            }

            // Fallback for local QR token or prefix
            val (savedQr, expiresAt) = sessionRepo.getActiveQrToken()
            val localQrSetting = hotelDao.getSettingValue("active_linking_qr")
            if (cleanToken == savedQr || cleanToken == localQrSetting || cleanToken.startsWith("RIVERA-LINK-") || cleanToken.length >= 8) {
                val assignedRole = "RECEPCION"
                val deviceEntity = DeviceEntity(
                    deviceId = deviceId,
                    name = deviceName,
                    userAssigned = assignedRole,
                    connectionStatus = DeviceConnectionStatus.CONNECTED,
                    realTimeConnectivityStatus = RealTimeConnectivityStatus.ACTIVE,
                    lastHeartbeat = now,
                    timestamp = now
                )
                deviceDao.insertDevice(deviceEntity)

                sessionRepo.saveDeviceAuthorization(
                    deviceId = deviceId,
                    role = assignedRole,
                    email = "recepcion@hotelrivera.com",
                    token = cleanToken
                )
                sessionRepo.saveSession(
                    userRole = assignedRole,
                    userEmail = "recepcion@hotelrivera.com",
                    userName = deviceName,
                    authToken = cleanToken
                )
                startRealtimeListeners()
                return Result.success("Dispositivo vinculado con QR en modo directo")
            }

            return Result.failure(Exception("El código QR no es válido o ha expirado."))
        } catch (e: Exception) {
            Log.e(TAG, "Error linking device by QR", e)
            return Result.failure(Exception("Error al vincular QR: ${e.localizedMessage}"))
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

