package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.entities.HousekeepingTaskEntity
import com.example.data.entities.HotelSettingEntity
import com.example.data.entities.InvoiceEntity
import com.example.data.entities.MaintenanceRequestEntity
import com.example.data.entities.AuditLogEntity
import com.example.data.entities.ProductEntity
import com.example.data.entities.ReservationEntity
import com.example.data.entities.RoomEntity
import com.example.data.entities.SaleRecordEntity
import com.example.data.entities.StayHistoryEntity
import com.example.data.entities.SupplyEntity
import com.example.data.entities.TimeRateEntity
import com.example.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HotelDao {

    // --- ROOMS ---
    @Query("SELECT * FROM rooms ORDER BY sortOrder ASC, id ASC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getRoomById(id: Long): RoomEntity?

    @Query("SELECT * FROM rooms WHERE roomNumber = :roomNumber LIMIT 1")
    suspend fun getRoomByNumber(roomNumber: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteRoomById(id: Long)

    @Query("DELETE FROM rooms")
    suspend fun deleteAllRooms()

    @Query("SELECT COUNT(*) FROM rooms")
    suspend fun getRoomsCount(): Int

    // --- TIME RATES ---
    @Query("SELECT * FROM time_rates ORDER BY durationMinutes ASC")
    fun getAllTimeRates(): Flow<List<TimeRateEntity>>

    @Query("SELECT * FROM time_rates WHERE isActive = 1 ORDER BY durationMinutes ASC")
    fun getActiveTimeRates(): Flow<List<TimeRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeRate(rate: TimeRateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeRates(rates: List<TimeRateEntity>)

    @Update
    suspend fun updateTimeRate(rate: TimeRateEntity)

    @Query("DELETE FROM time_rates WHERE id = :id")
    suspend fun deleteTimeRateById(id: Long)

    // --- SUPPLIES (INSUMOS) ---
    @Query("SELECT * FROM supplies ORDER BY name ASC")
    fun getAllSupplies(): Flow<List<SupplyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupply(supply: SupplyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplies(supplies: List<SupplyEntity>)

    @Update
    suspend fun updateSupply(supply: SupplyEntity)

    @Query("DELETE FROM supplies WHERE id = :id")
    suspend fun deleteSupplyById(id: Long)

    // --- PRODUCTS & EXTRA SALES ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("SELECT * FROM sale_records ORDER BY timestampMillis DESC")
    fun getAllSaleRecords(): Flow<List<SaleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleRecord(record: SaleRecordEntity): Long

    @Query("DELETE FROM sale_records")
    suspend fun deleteAllSaleRecords()

    // --- STAY HISTORY ---
    @Query("SELECT * FROM stay_history ORDER BY checkOutTimeMillis DESC")
    fun getAllStayHistory(): Flow<List<StayHistoryEntity>>

    @Query("SELECT * FROM stay_history WHERE dateString = :dateString ORDER BY checkOutTimeMillis DESC")
    fun getStayHistoryByDate(dateString: String): Flow<List<StayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStayHistory(history: StayHistoryEntity): Long

    @Query("DELETE FROM stay_history WHERE id = :id")
    suspend fun deleteStayHistoryById(id: Long)

    @Query("DELETE FROM stay_history")
    suspend fun deleteAllStayHistory()

    // --- USERS ---
    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    // --- SETTINGS ---
    @Query("SELECT * FROM hotel_settings")
    fun getAllSettingsFlow(): Flow<List<HotelSettingEntity>>

    @Query("SELECT value FROM hotel_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: HotelSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<HotelSettingEntity>)

    // --- INVOICES ---
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :number LIMIT 1")
    suspend fun getInvoiceByNumber(number: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()

    // --- AUDIT LOGS ---
    @Query("SELECT * FROM audit_logs ORDER BY timestampMillis DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAllAuditLogs()

    // --- HOUSEKEEPING SCHEDULE ---
    @Query("SELECT * FROM housekeeping_tasks ORDER BY assignedTimestamp DESC")
    fun getAllHousekeepingTasks(): Flow<List<HousekeepingTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousekeepingTask(task: HousekeepingTaskEntity): Long

    @Update
    suspend fun updateHousekeepingTask(task: HousekeepingTaskEntity)

    @Query("DELETE FROM housekeeping_tasks WHERE id = :id")
    suspend fun deleteHousekeepingTaskById(id: Long)

    // --- MAINTENANCE REQUESTS & BROKEN ITEMS ---
    @Query("SELECT * FROM maintenance_requests ORDER BY reportedTimestamp DESC")
    fun getAllMaintenanceRequests(): Flow<List<MaintenanceRequestEntity>>

    @Query("SELECT * FROM maintenance_requests WHERE status != 'RESUELTO' ORDER BY reportedTimestamp DESC")
    fun getActiveMaintenanceRequests(): Flow<List<MaintenanceRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceRequest(request: MaintenanceRequestEntity): Long

    @Update
    suspend fun updateMaintenanceRequest(request: MaintenanceRequestEntity)

    @Query("DELETE FROM maintenance_requests WHERE id = :id")
    suspend fun deleteMaintenanceRequestById(id: Long)

    // --- RESERVATIONS ---
    @Query("SELECT * FROM reservations ORDER BY reservationDateMillis ASC, id ASC")
    fun getAllReservations(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE status != 'CANCELADA' ORDER BY reservationDateMillis ASC")
    fun getActiveReservations(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE checkInDateString = :dateString AND status != 'CANCELADA' ORDER BY checkInTime ASC")
    fun getReservationsByDate(dateString: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun getReservationById(id: Long): ReservationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: ReservationEntity): Long

    @Update
    suspend fun updateReservation(reservation: ReservationEntity)

    @Query("DELETE FROM reservations WHERE id = :id")
    suspend fun deleteReservationById(id: Long)

    @Query("DELETE FROM reservations")
    suspend fun deleteAllReservations()
}
