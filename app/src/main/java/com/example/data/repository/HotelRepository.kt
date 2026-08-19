package com.example.data.repository

import com.example.data.dao.HotelDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HotelRepository(private val dao: HotelDao) {

    val allRooms: Flow<List<RoomEntity>> = dao.getAllRooms()
    val allTimeRates: Flow<List<TimeRateEntity>> = dao.getAllTimeRates()
    val activeTimeRates: Flow<List<TimeRateEntity>> = dao.getActiveTimeRates()
    val allSupplies: Flow<List<SupplyEntity>> = dao.getAllSupplies()
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val allSaleRecords: Flow<List<SaleRecordEntity>> = dao.getAllSaleRecords()
    val allStayHistory: Flow<List<StayHistoryEntity>> = dao.getAllStayHistory()
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    val allSettings: Flow<List<HotelSettingEntity>> = dao.getAllSettingsFlow()
    val allInvoices: Flow<List<InvoiceEntity>> = dao.getAllInvoices()
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()
    val allHousekeepingTasks: Flow<List<HousekeepingTaskEntity>> = dao.getAllHousekeepingTasks()

    // --- AUDIT LOGGING ---
    suspend fun logAudit(username: String, action: String, details: String) {
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))
        val log = AuditLogEntity(
            username = username,
            dateString = dateStr,
            timeString = timeStr,
            action = action,
            details = details,
            timestampMillis = now
        )
        dao.insertAuditLog(log)
    }

    // --- ROOM OPERATIONS ---
    suspend fun getRoomById(id: Long) = dao.getRoomById(id)

    suspend fun checkInRoom(
        roomId: Long,
        clientName: String,
        clientDpi: String?,
        guestCount: Int,
        rate: TimeRateEntity,
        notes: String?,
        receptionistName: String
    ) {
        val room = dao.getRoomById(roomId) ?: return
        val now = System.currentTimeMillis()
        val durationMillis = rate.durationMinutes * 60 * 1000L
        val estimatedCheckOut = now + durationMillis

        val updatedRoom = room.copy(
            status = RoomStatus.OCUPADA,
            clientName = clientName,
            clientDpi = clientDpi,
            guestCount = guestCount,
            rateName = rate.name,
            priceCharged = rate.price,
            checkInTimeMillis = now,
            checkOutTimeMillis = estimatedCheckOut,
            contractedDurationMinutes = rate.durationMinutes,
            receptionistName = receptionistName,
            notes = notes
        )
        dao.updateRoom(updatedRoom)
        logAudit(
            username = receptionistName,
            action = "ENTRADA_HABITACION",
            details = "Habitación ${room.roomNumber} ocupada por $clientName (${rate.name})"
        )
    }

    suspend fun extendStay(roomId: Long, extraMinutes: Long, extraPrice: Double) {
        val room = dao.getRoomById(roomId) ?: return
        val newCheckOut = room.checkOutTimeMillis + (extraMinutes * 60 * 1000L)
        val newTotalMinutes = room.contractedDurationMinutes + extraMinutes
        val newTotalPrice = room.priceCharged + extraPrice

        val updatedRoom = room.copy(
            checkOutTimeMillis = newCheckOut,
            contractedDurationMinutes = newTotalMinutes,
            priceCharged = newTotalPrice
        )
        dao.updateRoom(updatedRoom)
    }

    suspend fun finishStay(
        roomId: Long,
        paymentMethod: String,
        receptionistName: String,
        finalPrice: Double = 0.0,
        extraNotes: String? = null
    ): Long? {
        val room = dao.getRoomById(roomId) ?: return null
        val now = System.currentTimeMillis()
        val actualDurationMinutes = maxOf(1L, (now - room.checkInTimeMillis) / (60 * 1000L))
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        val charged = if (finalPrice > 0) finalPrice else room.priceCharged

        // 1. Record Stay in History
        val historyEntry = StayHistoryEntity(
            roomNumber = room.roomNumber,
            clientName = room.clientName ?: "Cliente General",
            clientDpi = room.clientDpi,
            guestCount = room.guestCount,
            checkInTimeMillis = room.checkInTimeMillis,
            checkOutTimeMillis = now,
            contractedTimeName = room.rateName ?: "Tarifa General",
            contractedDurationMinutes = room.contractedDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            priceCharged = charged,
            paymentMethod = paymentMethod,
            receptionistName = receptionistName,
            notes = listOfNotNull(room.notes, extraNotes).joinToString(" | ").ifBlank { null },
            dateString = dateString
        )
        val historyId = dao.insertStayHistory(historyEntry)

        // 2. Automatically deduct supplies per stay configuration
        val currentSupplies = dao.getAllSupplies().first()
        currentSupplies.forEach { supply ->
            if (supply.autoDeductQuantityPerStay > 0) {
                val newStock = maxOf(0.0, supply.stockCurrent - supply.autoDeductQuantityPerStay)
                dao.updateSupply(supply.copy(stockCurrent = newStock))
            }
        }

        // 3. Reset Room to PENDIENTE_LIMPIEZA
        val updatedRoom = room.copy(
            status = RoomStatus.PENDIENTE_LIMPIEZA,
            clientName = null,
            clientDpi = null,
            guestCount = 1,
            rateName = null,
            priceCharged = 0.0,
            checkInTimeMillis = 0L,
            checkOutTimeMillis = 0L,
            contractedDurationMinutes = 0L,
            receptionistName = null,
            notes = null,
            cleaningStartTimeMillis = 0L
        )
        dao.updateRoom(updatedRoom)
        logAudit(
            username = receptionistName,
            action = "SALIDA_HABITACION",
            details = "Salida de Habitación ${room.roomNumber} (${historyEntry.clientName}). Total cobrado: Q${String.format(Locale.US, "%.2f", charged)}"
        )
        return historyId
    }

    suspend fun setRoomCleaningStatus(roomId: Long, newStatus: String, receptionistName: String) {
        val room = dao.getRoomById(roomId) ?: return
        val now = System.currentTimeMillis()
        val updatedRoom = room.copy(
            status = newStatus,
            cleaningStartTimeMillis = if (newStatus == RoomStatus.EN_LIMPIEZA) now else room.cleaningStartTimeMillis,
            cleaningFinishedBy = if (newStatus == RoomStatus.DISPONIBLE) receptionistName else room.cleaningFinishedBy
        )
        dao.updateRoom(updatedRoom)
    }

    suspend fun updateRoomDetails(room: RoomEntity) = dao.updateRoom(room)

    suspend fun addRoom(roomNumber: String) {
        val count = dao.getRoomsCount()
        val newRoom = RoomEntity(
            roomNumber = roomNumber,
            status = RoomStatus.DISPONIBLE,
            sortOrder = count + 1
        )
        dao.insertRoom(newRoom)
    }

    suspend fun deleteRoom(id: Long) = dao.deleteRoomById(id)

    suspend fun setTotalRooms(totalCount: Int) {
        val currentRooms = dao.getAllRooms().first()
        if (currentRooms.size < totalCount) {
            val toAdd = totalCount - currentRooms.size
            var maxNum = currentRooms.mapNotNull { it.roomNumber.toIntOrNull() }.maxOrNull() ?: 0
            for (i in 1..toAdd) {
                maxNum++
                dao.insertRoom(
                    RoomEntity(
                        roomNumber = maxNum.toString(),
                        status = RoomStatus.DISPONIBLE,
                        sortOrder = currentRooms.size + i
                    )
                )
            }
        } else if (currentRooms.size > totalCount) {
            // Delete extra available rooms from the bottom
            val excessCount = currentRooms.size - totalCount
            val removable = currentRooms.filter { it.status == RoomStatus.DISPONIBLE }.takeLast(excessCount)
            removable.forEach { dao.deleteRoomById(it.id) }
        }
        saveSetting("total_rooms", totalCount.toString())
    }

    // --- RATES OPERATIONS ---
    suspend fun saveTimeRate(rate: TimeRateEntity) {
        if (rate.id == 0L) {
            dao.insertTimeRate(rate)
        } else {
            dao.updateTimeRate(rate)
        }
    }

    suspend fun deleteTimeRate(id: Long) = dao.deleteTimeRateById(id)

    // --- SUPPLIES OPERATIONS ---
    suspend fun saveSupply(supply: SupplyEntity) {
        if (supply.id == 0L) {
            dao.insertSupply(supply)
        } else {
            dao.updateSupply(supply)
        }
    }

    suspend fun deleteSupply(id: Long) = dao.deleteSupplyById(id)

    // --- PRODUCTS & EXTRA SALES OPERATIONS ---
    suspend fun saveProduct(product: ProductEntity) {
        if (product.id == 0L) {
            dao.insertProduct(product)
        } else {
            dao.updateProduct(product)
        }
    }

    suspend fun deleteProduct(id: Long) = dao.deleteProductById(id)

    suspend fun registerSale(productId: Long, quantity: Int, receptionistName: String, paymentMethod: String) {
        val products = dao.getAllProducts().first()
        val product = products.find { it.id == productId } ?: return
        val total = product.price * quantity
        val totalCost = product.costPrice * quantity
        val profit = total - totalCost

        val record = SaleRecordEntity(
            productName = product.name,
            quantity = quantity,
            unitPrice = product.price,
            totalPrice = total,
            profit = profit,
            timestampMillis = System.currentTimeMillis(),
            registeredBy = receptionistName,
            paymentMethod = paymentMethod
        )
        dao.insertSaleRecord(record)

        // Update product stock
        val newStock = maxOf(0, product.stock - quantity)
        dao.updateProduct(product.copy(stock = newStock))
    }

    // --- USERS OPERATIONS ---
    suspend fun saveUser(user: UserEntity) {
        if (user.id == 0L) {
            dao.insertUser(user)
        } else {
            dao.updateUser(user)
        }
    }

    suspend fun getUserByUsername(username: String): UserEntity? = dao.getUserByUsername(username)

    suspend fun deleteUser(id: Long) = dao.deleteUserById(id)

    // --- SETTINGS OPERATIONS ---
    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(HotelSettingEntity(key, value))
    }

    suspend fun getSetting(key: String, default: String): String {
        return dao.getSettingValue(key) ?: default
    }

    // --- HISTORY OPERATIONS ---
    suspend fun deleteHistoryItem(id: Long) = dao.deleteStayHistoryById(id)

    // --- INVOICE OPERATIONS ---
    suspend fun createInvoice(
        roomNumber: String,
        clientName: String,
        contractedTime: String,
        checkInTime: String,
        checkOutTime: String,
        price: Double,
        discount: Double,
        paymentMethod: String,
        receptionistName: String,
        stayHistoryId: Long? = null
    ): InvoiceEntity {
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))

        val hName = getSetting("hotel_name", "Hotel Rivera")
        val hAddress = getSetting("hotel_address", "Calle Principal 4-12, Zona 1")
        val hPhone = getSetting("hotel_phone", "+502 7765-4321")
        val hNit = getSetting("hotel_nit", "1234567-8")

        val currentCounterStr = getSetting("invoice_next_num", "1")
        val currentCounter = currentCounterStr.toLongOrNull() ?: 1L
        val invoiceNum = "FAC-%05d".format(currentCounter)

        // Save next counter
        saveSetting("invoice_next_num", (currentCounter + 1).toString())

        val totalAmount = maxOf(0.0, price - discount)

        val invoice = InvoiceEntity(
            invoiceNumber = invoiceNum,
            stayHistoryId = stayHistoryId,
            hotelName = hName,
            hotelAddress = hAddress,
            hotelPhone = hPhone,
            hotelNit = hNit,
            dateString = dateStr,
            timeString = timeStr,
            roomNumber = roomNumber,
            clientName = if (clientName.isBlank()) "Cliente General" else clientName,
            contractedTime = contractedTime,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            price = price,
            discount = discount,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            receptionistName = receptionistName,
            timestampMillis = now
        )

        val id = dao.insertInvoice(invoice)
        logAudit(
            username = receptionistName,
            action = "GENERAR_FACTURA",
            details = "Factura $invoiceNum creada para ${invoice.clientName} (Hab. $roomNumber). Total: Q${String.format(Locale.US, "%.2f", totalAmount)}"
        )
        return invoice.copy(id = id)
    }

    suspend fun saveInvoice(invoice: InvoiceEntity): Long {
        val id = dao.insertInvoice(invoice)
        logAudit(
            username = invoice.receptionistName ?: "Recepción",
            action = "INSERTAR_FACTURA",
            details = "Factura ${invoice.invoiceNumber} registrada manualmente. Total: Q${String.format(Locale.US, "%.2f", invoice.totalAmount)}"
        )
        return id
    }

    suspend fun voidInvoice(invoiceId: Long, managerUsername: String, reason: String) {
        val invoice = dao.getInvoiceById(invoiceId) ?: return
        val updated = invoice.copy(
            isVoided = true,
            voidedBy = managerUsername,
            voidReason = reason
        )
        dao.updateInvoice(updated)
        logAudit(
            username = managerUsername,
            action = "ANULAR_FACTURA",
            details = "Factura ${invoice.invoiceNumber} anulada por $managerUsername. Motivo: $reason"
        )
    }

    suspend fun deleteInvoice(id: Long) = dao.deleteInvoiceById(id)

    // --- HOUSEKEEPING OPERATIONS ---
    suspend fun insertHousekeepingTask(task: HousekeepingTaskEntity): Long {
        val id = dao.insertHousekeepingTask(task)
        logAudit(
            username = task.assignedBy,
            action = "ASIGNAR_LIMPIEZA",
            details = "Tarea de limpieza asignada a ${task.assignedStaffName} para Hab. ${task.roomNumber} (Prioridad: ${task.priority})"
        )
        return id
    }

    suspend fun updateHousekeepingTask(task: HousekeepingTaskEntity) {
        dao.updateHousekeepingTask(task)
        logAudit(
            username = task.assignedStaffName,
            action = "ACTUALIZAR_LIMPIEZA",
            details = "Estado de limpieza Hab. ${task.roomNumber} actualizado a: ${task.status}"
        )
    }

    suspend fun deleteHousekeepingTask(id: Long) = dao.deleteHousekeepingTaskById(id)
}
