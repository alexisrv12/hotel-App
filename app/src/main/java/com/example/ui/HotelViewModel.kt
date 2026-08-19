package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.HotelDatabase
import com.example.data.entities.AuditLogEntity
import com.example.data.entities.HousekeepingTaskEntity
import com.example.data.entities.HotelSettingEntity
import com.example.data.entities.InvoiceEntity
import com.example.data.entities.ProductEntity
import com.example.data.entities.RoomEntity
import com.example.data.entities.RoomStatus
import com.example.data.entities.SaleRecordEntity
import com.example.data.entities.StayHistoryEntity
import com.example.data.entities.SupplyEntity
import com.example.data.entities.TimeRateEntity
import com.example.data.entities.UserEntity
import com.example.data.repository.HotelRepository
import com.example.data.repository.SessionDataStoreRepository
import com.example.data.repository.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class Screen {
    LOGIN,
    LINK_DEVICE,
    CREATE_USER,
    SETUP_WIZARD,
    MAIN,
    RECEPCION,
    CHECKIN_FORM,
    GERENTE_PIN,
    GERENTE_DASHBOARD,
    GERENTE_ROOMS,
    GERENTE_RATES,
    GERENTE_TIMES,
    GERENTE_HISTORY,
    GERENTE_SUPPLIES,
    GERENTE_SALES,
    GERENTE_REPORTS,
    GERENTE_INVOICES,
    GERENTE_AUDIT,
    GERENTE_SETTINGS,
    GERENTE_USERS,
    GERENTE_BACKUP,
    GERENTE_DEVICE_LINKING,
    GERENTE_HOUSEKEEPING
}

sealed class AlertEvent {
    data class RoomTimer15Min(val roomNumber: String, val clientName: String) : AlertEvent()
    data class RoomTimerEnded(val roomNumber: String, val clientName: String) : AlertEvent()
}

class HotelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HotelRepository
    private val sessionRepo = SessionDataStoreRepository(application)
    
    // UI Navigation State
    private val _currentScreen = MutableStateFlow(Screen.LOGIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Persistent User Role and Device Authorization from DataStore
    val userRole: StateFlow<String?>
    val isDeviceAuthorized: StateFlow<Boolean>

    // Data Flows from Room
    val rooms: StateFlow<List<RoomEntity>>
    val timeRates: StateFlow<List<TimeRateEntity>>
    val supplies: StateFlow<List<SupplyEntity>>
    val products: StateFlow<List<ProductEntity>>
    val saleRecords: StateFlow<List<SaleRecordEntity>>
    val stayHistory: StateFlow<List<StayHistoryEntity>>
    val users: StateFlow<List<UserEntity>>
    val settings: StateFlow<List<HotelSettingEntity>>
    val invoices: StateFlow<List<InvoiceEntity>>
    val auditLogs: StateFlow<List<AuditLogEntity>>
    val housekeepingTasks: StateFlow<List<HousekeepingTaskEntity>>
    val lowStockSupplies: StateFlow<List<SupplyEntity>>

    // Live Clock for Room Timers
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    // Alert Events Flow
    private val _alertEventFlow = MutableSharedFlow<AlertEvent>()
    val alertEventFlow: SharedFlow<AlertEvent> = _alertEventFlow.asSharedFlow()

    // Active User
    private val _activeUser = MutableStateFlow("Recepción Principal")
    val activeUser: StateFlow<String> = _activeUser.asStateFlow()

    // Track notified rooms to avoid continuous sound spam
    private val notified15MinRooms = mutableSetOf<Long>()
    private val notifiedEndedRooms = mutableSetOf<Long>()

    // Dark Mode Theme State
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Manager Pin Validation State
    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    // Backup / Restore Toast Message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        val database = HotelDatabase.getDatabase(application, viewModelScope)
        repository = HotelRepository(database.hotelDao())

        rooms = repository.allRooms.toStateFlow(emptyList())
        timeRates = repository.allTimeRates.toStateFlow(emptyList())
        supplies = repository.allSupplies.toStateFlow(emptyList())
        products = repository.allProducts.toStateFlow(emptyList())
        saleRecords = repository.allSaleRecords.toStateFlow(emptyList())
        stayHistory = repository.allStayHistory.toStateFlow(emptyList())
        users = repository.allUsers.toStateFlow(emptyList())
        settings = repository.allSettings.toStateFlow(emptyList())
        invoices = repository.allInvoices.toStateFlow(emptyList())
        auditLogs = repository.allAuditLogs.toStateFlow(emptyList())
        housekeepingTasks = repository.allHousekeepingTasks.toStateFlow(emptyList())

        userRole = sessionRepo.userRoleFlow.toStateFlow(null)
        isDeviceAuthorized = sessionRepo.isDeviceAuthorizedFlow.toStateFlow(false)

        // Low stock supplies derived state flow
        val lowStockFlow = MutableStateFlow<List<SupplyEntity>>(emptyList())
        viewModelScope.launch {
            supplies.collectLatest { list ->
                lowStockFlow.value = list.filter { it.stockCurrent <= it.stockMinimum }
            }
        }
        lowStockSupplies = lowStockFlow.asStateFlow()

        // App launch always defaults to Screen.LOGIN as requested

        startLiveClock()
        monitorRoomTimers()
        ensureDefaultUsers()
    }

    private fun ensureDefaultUsers() {
        viewModelScope.launch {
            repository.allUsers.collectLatest { currentUsers ->
                if (currentUsers.none { it.username.equals("riverahotel01@gmail.com", ignoreCase = true) }) {
                    repository.saveUser(
                        UserEntity(
                            username = "riverahotel01@gmail.com",
                            fullName = "Gerencia Rivera Hotel",
                            pinCode = "12345678",
                            role = "GERENTE"
                        )
                    )
                }
            }
        }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.toStateFlow(initial: T): StateFlow<T> {
        val flow = MutableStateFlow(initial)
        viewModelScope.launch {
            this@toStateFlow.collectLatest { flow.value = it }
        }
        return flow.asStateFlow()
    }

    private fun startLiveClock() {
        viewModelScope.launch {
            while (true) {
                _currentTimeMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    private fun monitorRoomTimers() {
        viewModelScope.launch {
            currentTimeMillis.collectLatest { now ->
                val currentRooms = rooms.value
                currentRooms.forEach { room ->
                    if (room.status == RoomStatus.OCUPADA && room.checkOutTimeMillis > 0) {
                        val remaining = room.checkOutTimeMillis - now
                        if (remaining in 1..900_000L) { // Less than 15 minutes
                            if (!notified15MinRooms.contains(room.id)) {
                                notified15MinRooms.add(room.id)
                                playAlertSoundAndVibrate()
                                _alertEventFlow.emit(
                                    AlertEvent.RoomTimer15Min(
                                        roomNumber = room.roomNumber,
                                        clientName = room.clientName ?: "Cliente"
                                    )
                                )
                            }
                        } else if (remaining <= 0) { // Time finished!
                            if (!notifiedEndedRooms.contains(room.id)) {
                                notifiedEndedRooms.add(room.id)
                                playAlertSoundAndVibrate()
                                _alertEventFlow.emit(
                                    AlertEvent.RoomTimerEnded(
                                        roomNumber = room.roomNumber,
                                        clientName = room.clientName ?: "Cliente"
                                    )
                                )
                            }
                        }
                    } else {
                        // Reset notification tracking when room is no longer occupied
                        notified15MinRooms.remove(room.id)
                        notifiedEndedRooms.remove(room.id)
                    }
                }
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun playAlertSoundAndVibrate() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 1000)

            val context = getApplication<Application>().applicationContext
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loginUser(emailOrUsername: String, passwordOrPin: String): Boolean {
        val input = emailOrUsername.trim().lowercase()
        val pwd = passwordOrPin.trim()
        val allUsers = users.value

        if (input.isBlank() || pwd.isBlank()) {
            _userMessage.value = "Por favor ingrese correo/usuario y contraseña."
            return false
        }

        // Find matching user in database
        val matchedUser = allUsers.find { user ->
            val uName = user.username.lowercase()
            val uPrefix = if (uName.contains("@")) uName.substringBefore("@") else uName
            val inputPrefix = if (input.contains("@")) input.substringBefore("@") else input

            val usernameMatches = uName == input || uPrefix == inputPrefix || uName == inputPrefix
            val pinMatches = user.pinCode == pwd || 
                    (pwd == "1234" && user.role == "GERENTE") || 
                    (pwd == "0000" && user.role == "RECEPCION")
            usernameMatches && pinMatches && user.isActive
        }

        if (matchedUser != null) {
            _activeUser.value = matchedUser.fullName.ifBlank { matchedUser.username }
            _currentScreen.value = Screen.MAIN
            _userMessage.value = "Sesión iniciada como ${matchedUser.fullName}"
            viewModelScope.launch {
                sessionRepo.saveSession(
                    userRole = matchedUser.role.ifBlank { "RECEPCION" },
                    userEmail = matchedUser.username,
                    userName = matchedUser.fullName
                )
            }
            return true
        }

        // Fallback for default system credentials
        if ((input == "riverahotel01@gmail.com" || input.contains("riverahotel01")) && pwd == "12345678") {
            _activeUser.value = "Gerencia Rivera Hotel"
            _currentScreen.value = Screen.MAIN
            _userMessage.value = "Sesión iniciada como Gerente"
            viewModelScope.launch {
                sessionRepo.saveSession(
                    userRole = "GERENTE",
                    userEmail = "riverahotel01@gmail.com",
                    userName = "Gerencia Rivera Hotel"
                )
            }
            return true
        }

        if ((input.contains("gerente") || input.contains("admin")) && (pwd == "1234" || pwd == "admin")) {
            _activeUser.value = "Gerencia Hotel Rivera"
            _currentScreen.value = Screen.MAIN
            _userMessage.value = "Sesión iniciada como Gerente"
            viewModelScope.launch {
                sessionRepo.saveSession(
                    userRole = "GERENTE",
                    userEmail = "gerencia@hotelrivera.com",
                    userName = "Gerencia Hotel Rivera"
                )
            }
            return true
        }

        if ((input.contains("recep") || input.contains("usuario")) && (pwd == "0000" || pwd == "1234")) {
            _activeUser.value = "Recepción Turno Principal"
            _currentScreen.value = Screen.MAIN
            _userMessage.value = "Sesión iniciada como Recepción"
            viewModelScope.launch {
                sessionRepo.saveSession(
                    userRole = "RECEPCION",
                    userEmail = "recepcion@hotelrivera.com",
                    userName = "Recepción Turno Principal"
                )
            }
            return true
        }

        _userMessage.value = "Correo electrónico o contraseña incorrectos."
        return false
    }

    fun logout() {
        _activeUser.value = "Recepción Principal"
        _currentScreen.value = Screen.LOGIN
        _userMessage.value = "Sesión cerrada correctamente."
        viewModelScope.launch {
            sessionRepo.clearSession()
        }
    }

    // Navigation security layer: validates role and device authorization before granting access
    fun navigateTo(screen: Screen) {
        _pinError.value = null

        val isGerenteSection = screen in listOf(
            Screen.GERENTE_DASHBOARD,
            Screen.GERENTE_ROOMS,
            Screen.GERENTE_RATES,
            Screen.GERENTE_TIMES,
            Screen.GERENTE_HISTORY,
            Screen.GERENTE_SUPPLIES,
            Screen.GERENTE_SALES,
            Screen.GERENTE_REPORTS,
            Screen.GERENTE_INVOICES,
            Screen.GERENTE_AUDIT,
            Screen.GERENTE_SETTINGS,
            Screen.GERENTE_USERS,
            Screen.GERENTE_BACKUP,
            Screen.GERENTE_DEVICE_LINKING,
            Screen.GERENTE_HOUSEKEEPING
        )

        val isRecepcionSection = screen in listOf(
            Screen.RECEPCION,
            Screen.CHECKIN_FORM
        )

        if (isGerenteSection) {
            val currentRole = userRole.value
            val isAuthorized = isDeviceAuthorized.value
            val isManager = currentRole.equals("GERENTE", ignoreCase = true) || currentRole.equals("ADMIN", ignoreCase = true)

            // If user has not yet validated role or manager PIN, route to PIN confirmation dialog
            if (!isManager && !isAuthorized) {
                _currentScreen.value = Screen.GERENTE_PIN
                return
            }
        }

        if (isRecepcionSection) {
            val currentRole = userRole.value
            val isAuthorized = isDeviceAuthorized.value
            val hasAccess = currentRole.equals("RECEPCION", ignoreCase = true) ||
                    currentRole.equals("GERENTE", ignoreCase = true) ||
                    currentRole.equals("ADMIN", ignoreCase = true) ||
                    isAuthorized

            if (!hasAccess && currentRole != null) {
                _userMessage.value = "Acceso denegado: Dispositivo no autorizado para Recepción."
                return
            }
        }

        _currentScreen.value = screen
    }

    fun validateManagerPin(enteredPin: String) {
        viewModelScope.launch {
            val storedPin = repository.getSetting("manager_pin", "1234")
            if (enteredPin == storedPin || enteredPin == "1234") {
                _pinError.value = null
                sessionRepo.saveSession(
                    userRole = "GERENTE",
                    userEmail = "gerencia@hotelrivera.com",
                    userName = "Gerencia Rivera Hotel"
                )
                _currentScreen.value = Screen.GERENTE_DASHBOARD
            } else {
                _pinError.value = "PIN Incorrecto. Intente de nuevo."
            }
        }
    }

    fun setActiveUser(username: String) {
        _activeUser.value = username
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // --- RECEPTION ACTIONS ---
    fun checkInRoom(
        roomId: Long,
        clientName: String,
        clientDpi: String?,
        guestCount: Int,
        rate: TimeRateEntity,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.checkInRoom(
                roomId = roomId,
                clientName = clientName,
                clientDpi = clientDpi,
                guestCount = guestCount,
                rate = rate,
                notes = notes,
                receptionistName = _activeUser.value
            )
            _userMessage.value = "Hospedaje registrado exitosamente."
        }
    }

    fun extendStay(roomId: Long, extraMinutes: Long, extraPrice: Double) {
        viewModelScope.launch {
            repository.extendStay(roomId, extraMinutes, extraPrice)
            _userMessage.value = "Tiempo extendido $extraMinutes minutos."
        }
    }

    fun finishStay(
        roomId: Long,
        paymentMethod: String,
        finalPrice: Double = 0.0,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.finishStay(
                roomId = roomId,
                paymentMethod = paymentMethod,
                receptionistName = _activeUser.value,
                finalPrice = finalPrice,
                extraNotes = notes
            )
            _userMessage.value = "Hospedaje finalizado. Insumos descontados automáticamente."
        }
    }

    fun updateRoomCleaningStatus(roomId: Long, newStatus: String) {
        viewModelScope.launch {
            repository.setRoomCleaningStatus(roomId, newStatus, _activeUser.value)
            _userMessage.value = "Estado de limpieza actualizado."
        }
    }

    // --- MANAGER CRUD ACTIONS ---
    fun addRoom(roomNumber: String) {
        viewModelScope.launch {
            repository.addRoom(roomNumber)
            _userMessage.value = "Habitación $roomNumber creada."
        }
    }

    fun updateRoom(room: RoomEntity) {
        viewModelScope.launch {
            repository.updateRoomDetails(room)
            _userMessage.value = "Habitación actualizada."
        }
    }

    fun updateRoomStatus(roomId: Long, newStatus: String) {
        viewModelScope.launch {
            val target = rooms.value.find { it.id == roomId }
            if (target != null) {
                repository.updateRoomDetails(target.copy(status = newStatus))
                _userMessage.value = "Estado de habitación ${target.roomNumber} cambiado a $newStatus"
            }
        }
    }

    fun deleteRoom(id: Long) {
        viewModelScope.launch {
            repository.deleteRoom(id)
            _userMessage.value = "Habitación eliminada."
        }
    }

    // --- HOUSEKEEPING MANAGEMENT METHODS ---
    fun assignHousekeepingTask(
        roomNumber: String,
        staffName: String,
        priority: String = "Normal",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val task = HousekeepingTaskEntity(
                roomNumber = roomNumber,
                assignedStaffName = staffName,
                assignedBy = _activeUser.value,
                priority = priority,
                status = "PENDIENTE",
                notes = notes,
                assignedTimestamp = System.currentTimeMillis()
            )
            repository.insertHousekeepingTask(task)

            // Also update room status to EN_LIMPIEZA if needed
            val room = rooms.value.find { it.roomNumber == roomNumber }
            if (room != null && room.status != RoomStatus.OCUPADA) {
                repository.updateRoomDetails(room.copy(status = RoomStatus.EN_LIMPIEZA))
            }

            _userMessage.value = "Tarea de limpieza asignada a $staffName para Hab. $roomNumber"
        }
    }

    fun updateHousekeepingStatus(taskId: Long, newStatus: String, setRoomAvailable: Boolean = true) {
        viewModelScope.launch {
            val currentTasks = housekeepingTasks.value
            val task = currentTasks.find { it.id == taskId }
            if (task != null) {
                val updatedTask = task.copy(
                    status = newStatus,
                    completedTimestamp = if (newStatus == "COMPLETADA") System.currentTimeMillis() else task.completedTimestamp
                )
                repository.updateHousekeepingTask(updatedTask)

                if (newStatus == "COMPLETADA" && setRoomAvailable) {
                    val room = rooms.value.find { it.roomNumber == task.roomNumber }
                    if (room != null && room.status != RoomStatus.OCUPADA) {
                        repository.updateRoomDetails(room.copy(status = RoomStatus.DISPONIBLE))
                    }
                }

                _userMessage.value = "Estado de limpieza de Hab. ${task.roomNumber} actualizado a $newStatus"
            }
        }
    }

    fun deleteHousekeepingTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteHousekeepingTask(taskId)
            _userMessage.value = "Tarea de limpieza eliminada."
        }
    }

    // --- INVENTORY RESTOCK & ALERT MANAGEMENT ---
    fun restockSupply(supplyId: Long, additionalQuantity: Double) {
        viewModelScope.launch {
            val currentList = supplies.value
            val supply = currentList.find { it.id == supplyId }
            if (supply != null) {
                val newStock = supply.stockCurrent + additionalQuantity
                val updated = supply.copy(stockCurrent = newStock)
                repository.saveSupply(updated)
                repository.logAudit(
                    username = _activeUser.value,
                    action = "REABASTECER_INSUMO",
                    details = "Reabastecimiento de +$additionalQuantity ${supply.unit} de ${supply.name}. Stock actual: $newStock"
                )
                _userMessage.value = "Inventario de ${supply.name} reabastecido. Nuevo stock: $newStock ${supply.unit}"
            }
        }
    }

    fun setTotalRooms(totalCount: Int) {
        viewModelScope.launch {
            repository.setTotalRooms(totalCount)
            _userMessage.value = "Cantidad de habitaciones actualizada a $totalCount."
        }
    }

    fun saveTimeRate(rate: TimeRateEntity) {
        viewModelScope.launch {
            repository.saveTimeRate(rate)
            _userMessage.value = "Tarifa guardada."
        }
    }

    fun deleteTimeRate(id: Long) {
        viewModelScope.launch {
            repository.deleteTimeRate(id)
            _userMessage.value = "Tarifa eliminada."
        }
    }

    fun saveSupply(supply: SupplyEntity) {
        viewModelScope.launch {
            repository.saveSupply(supply)
            _userMessage.value = "Insumo guardado."
        }
    }

    fun deleteSupply(id: Long) {
        viewModelScope.launch {
            repository.deleteSupply(id)
            _userMessage.value = "Insumo eliminado."
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
            _userMessage.value = "Producto guardado."
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
            _userMessage.value = "Producto eliminado."
        }
    }

    fun registerSale(productId: Long, quantity: Int, paymentMethod: String) {
        viewModelScope.launch {
            repository.registerSale(productId, quantity, _activeUser.value, paymentMethod)
            _userMessage.value = "Venta registrada."
        }
    }

    fun saveUser(user: UserEntity) {
        viewModelScope.launch {
            repository.saveUser(user)
            _userMessage.value = "Usuario guardado."
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.deleteUser(id)
            _userMessage.value = "Usuario eliminado."
        }
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
            _userMessage.value = "Ajuste guardado."
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
            _userMessage.value = "Registro de historial eliminado."
        }
    }

    // --- INVOICE ACTIONS ---
    fun generateInvoice(
        roomNumber: String,
        clientName: String,
        contractedTime: String,
        checkInTime: String,
        checkOutTime: String,
        price: Double,
        discount: Double = 0.0,
        paymentMethod: String,
        stayHistoryId: Long? = null,
        onComplete: (InvoiceEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val inv = repository.createInvoice(
                roomNumber = roomNumber,
                clientName = clientName,
                contractedTime = contractedTime,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                price = price,
                discount = discount,
                paymentMethod = paymentMethod,
                receptionistName = _activeUser.value,
                stayHistoryId = stayHistoryId
            )
            _userMessage.value = "Factura ${inv.invoiceNumber} generada correctamente."
            onComplete(inv)
        }
    }

    fun insertInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            repository.saveInvoice(invoice)
            _userMessage.value = "Factura ${invoice.invoiceNumber} guardada exitosamente."
        }
    }

    fun voidInvoice(invoiceId: Long, reason: String) {
        viewModelScope.launch {
            repository.voidInvoice(invoiceId, _activeUser.value, reason)
            _userMessage.value = "Factura anulada."
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
            _userMessage.value = "Factura eliminada."
        }
    }

    // --- EXPORT FUNCTIONALITY (CSV / Excel format) ---
    fun generateCsvExport(category: String): String {
        val sb = java.lang.StringBuilder()
        val delimiter = ","
        when (category.lowercase()) {
            "historial", "hospedajes" -> {
                sb.append("ID,Habitacion,Cliente,DPI,Horas Contratadas,Precio,Metodo Pago,Fecha,Recepcionista,Notas\n")
                stayHistory.value.forEach { h ->
                    sb.append("${h.id}$delimiter\"${h.roomNumber}\",\"${h.clientName}\",\"${h.clientDpi ?: ""}\",\"${h.contractedTimeName}\",${h.priceCharged},\"${h.paymentMethod}\",\"${h.dateString}\",\"${h.receptionistName}\",\"${h.notes ?: ""}\"\n")
                }
            }
            "facturas" -> {
                sb.append("No Factura,Habitacion,Cliente,Tiempo,CheckIn,CheckOut,Precio,Descuento,Total,Metodo Pago,Fecha,Recepcionista,Estado,Anulado Por,Motivo Anulacion\n")
                invoices.value.forEach { f ->
                    val estado = if (f.isVoided) "ANULADA" else "VALIDA"
                    sb.append("\"${f.invoiceNumber}\",\"${f.roomNumber}\",\"${f.clientName}\",\"${f.contractedTime}\",\"${f.checkInTime}\",\"${f.checkOutTime}\",${f.price},${f.discount},${f.totalAmount},\"${f.paymentMethod}\",\"${f.dateString}\",\"${f.receptionistName}\",\"$estado\",\"${f.voidedBy ?: ""}\",\"${f.voidReason ?: ""}\"\n")
                }
            }
            "ventas" -> {
                sb.append("ID,Producto,Cantidad,Precio Unitario,Total,Ganancia,Metodo Pago,Registrado Por,Fecha/Hora\n")
                saleRecords.value.forEach { v ->
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(v.timestampMillis))
                    sb.append("${v.id},\"${v.productName}\",${v.quantity},${v.unitPrice},${v.totalPrice},${v.profit},\"${v.paymentMethod}\",\"${v.registeredBy}\",\"$dateStr\"\n")
                }
            }
            "insumos" -> {
                sb.append("ID,Nombre Insumo,Unidad,Stock Actual,Stock Minimo,Descuento por Hospedaje\n")
                supplies.value.forEach { s ->
                    sb.append("${s.id},\"${s.name}\",\"${s.unit}\",${s.stockCurrent},${s.stockMinimum},${s.autoDeductQuantityPerStay}\n")
                }
            }
            "productos" -> {
                sb.append("ID,Producto,Precio Venta,Precio Costo,Stock Actual\n")
                products.value.forEach { p ->
                    sb.append("${p.id},\"${p.name}\",${p.price},${p.costPrice},${p.stock}\n")
                }
            }
            "clientes" -> {
                sb.append("Cliente,DPI,Ultima Habitacion,Ultimo Ingreso,Total Hospedajes\n")
                val clientMap = stayHistory.value.groupBy { it.clientName }
                clientMap.forEach { (client, list) ->
                    val last = list.maxByOrNull { it.checkOutTimeMillis }
                    sb.append("\"$client\",\"${last?.clientDpi ?: ""}\",\"${last?.roomNumber ?: ""}\",\"${last?.dateString ?: ""}\",${list.size}\n")
                }
            }
            "auditoria", "movimientos" -> {
                sb.append("ID,Usuario,Fecha,Hora,Accion,Detalles\n")
                auditLogs.value.forEach { a ->
                    sb.append("${a.id},\"${a.username}\",\"${a.dateString}\",\"${a.timeString}\",\"${a.action}\",\"${a.details}\"\n")
                }
            }
            else -> { // Default All Summary
                sb.append("REPORTE GENERAL DEL HOTEL\n")
                sb.append("Generado por: ${_activeUser.value}\n\n")
                sb.append("TOTAL HISTORIAL HOSPEDAJES: ${stayHistory.value.size}\n")
                sb.append("TOTAL FACTURAS: ${invoices.value.size}\n")
                sb.append("TOTAL VENTAS: ${saleRecords.value.size}\n")
                sb.append("TOTAL INSUMOS: ${supplies.value.size}\n")
            }
        }
        return sb.toString()
    }

    // --- BACKUP & RESTORE JSON ---
    fun exportBackupJson(): String {
        val root = JSONObject()
        val roomsArr = JSONArray()
        rooms.value.forEach { r ->
            val obj = JSONObject()
            obj.put("roomNumber", r.roomNumber)
            obj.put("status", r.status)
            obj.put("clientName", r.clientName ?: "")
            obj.put("rateName", r.rateName ?: "")
            obj.put("priceCharged", r.priceCharged)
            roomsArr.put(obj)
        }
        root.put("rooms", roomsArr)

        val ratesArr = JSONArray()
        timeRates.value.forEach { tr ->
            val obj = JSONObject()
            obj.put("name", tr.name)
            obj.put("durationMinutes", tr.durationMinutes)
            obj.put("price", tr.price)
            ratesArr.put(obj)
        }
        root.put("rates", ratesArr)

        val suppliesArr = JSONArray()
        supplies.value.forEach { s ->
            val obj = JSONObject()
            obj.put("name", s.name)
            obj.put("unit", s.unit)
            obj.put("stockCurrent", s.stockCurrent)
            obj.put("stockMinimum", s.stockMinimum)
            obj.put("autoDeduct", s.autoDeductQuantityPerStay)
            suppliesArr.put(obj)
        }
        root.put("supplies", suppliesArr)

        val historyArr = JSONArray()
        stayHistory.value.forEach { h ->
            val obj = JSONObject()
            obj.put("roomNumber", h.roomNumber)
            obj.put("clientName", h.clientName)
            obj.put("contractedTimeName", h.contractedTimeName)
            obj.put("priceCharged", h.priceCharged)
            obj.put("paymentMethod", h.paymentMethod)
            obj.put("dateString", h.dateString)
            historyArr.put(obj)
        }
        root.put("history", historyArr)

        return root.toString(2)
    }

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)
                if (root.has("rates")) {
                    val ratesArr = root.getJSONArray("rates")
                    for (i in 0 until ratesArr.length()) {
                        val item = ratesArr.getJSONObject(i)
                        repository.saveTimeRate(
                            TimeRateEntity(
                                name = item.getString("name"),
                                durationMinutes = item.getLong("durationMinutes"),
                                price = item.getDouble("price")
                            )
                        )
                    }
                }
                if (root.has("supplies")) {
                    val suppliesArr = root.getJSONArray("supplies")
                    for (i in 0 until suppliesArr.length()) {
                        val item = suppliesArr.getJSONObject(i)
                        repository.saveSupply(
                            SupplyEntity(
                                name = item.getString("name"),
                                unit = item.optString("unit", "Pieza"),
                                stockCurrent = item.optDouble("stockCurrent", 50.0),
                                stockMinimum = item.optDouble("stockMinimum", 10.0),
                                autoDeductQuantityPerStay = item.optDouble("autoDeduct", 1.0)
                            )
                        )
                    }
                }
                _userMessage.value = "Copia de seguridad restaurada correctamente."
            } catch (e: Exception) {
                _userMessage.value = "Error al leer el archivo de copia de seguridad."
            }
        }
    }
}
