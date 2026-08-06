package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.DeviceDao
import com.example.data.dao.HotelDao
import com.example.data.dao.RoomDao
import com.example.data.entities.AuditLogEntity
import com.example.data.entities.DeviceEntity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        RoomEntity::class,
        TimeRateEntity::class,
        SupplyEntity::class,
        ProductEntity::class,
        SaleRecordEntity::class,
        StayHistoryEntity::class,
        UserEntity::class,
        HotelSettingEntity::class,
        InvoiceEntity::class,
        AuditLogEntity::class,
        DeviceEntity::class,
        HousekeepingTaskEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class HotelDatabase : RoomDatabase() {

    abstract fun hotelDao(): HotelDao
    abstract fun deviceDao(): DeviceDao
    abstract fun roomDao(): RoomDao

    companion object {
        @Volatile
        private var INSTANCE: HotelDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): HotelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HotelDatabase::class.java,
                    "hotel_rivera_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(HotelDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class HotelDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.hotelDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: HotelDao) {
                // Populate Rooms (1 to 10)
                val defaultRooms = (1..10).mapIndexed { index, num ->
                    RoomEntity(
                        roomNumber = num.toString(),
                        status = RoomStatus.DISPONIBLE,
                        sortOrder = index + 1
                    )
                }
                dao.insertRooms(defaultRooms)

                // Populate Time Rates in Hours
                val defaultRates = listOf(
                    TimeRateEntity(name = "1 Hora", durationMinutes = 60, price = 50.0),
                    TimeRateEntity(name = "2 Horas", durationMinutes = 120, price = 80.0),
                    TimeRateEntity(name = "3 Horas", durationMinutes = 180, price = 100.0),
                    TimeRateEntity(name = "12 Horas (Noche)", durationMinutes = 720, price = 150.0),
                    TimeRateEntity(name = "24 Horas (Día completo)", durationMinutes = 1440, price = 200.0)
                )
                dao.insertTimeRates(defaultRates)

                // Populate Supplies
                val defaultSupplies = listOf(
                    SupplyEntity(name = "Jabón de Tocador", unit = "Pieza", stockCurrent = 50.0, stockMinimum = 10.0, autoDeductQuantityPerStay = 1.0),
                    SupplyEntity(name = "Papel Higiénico", unit = "Rollo", stockCurrent = 50.0, stockMinimum = 10.0, autoDeductQuantityPerStay = 1.0),
                    SupplyEntity(name = "Shampoo Personal", unit = "Sachet", stockCurrent = 50.0, stockMinimum = 10.0, autoDeductQuantityPerStay = 1.0),
                    SupplyEntity(name = "Agua Embotellada", unit = "Botella", stockCurrent = 30.0, stockMinimum = 8.0, autoDeductQuantityPerStay = 1.0),
                    SupplyEntity(name = "Bolsa de Basura", unit = "Unidad", stockCurrent = 60.0, stockMinimum = 15.0, autoDeductQuantityPerStay = 1.0)
                )
                dao.insertSupplies(defaultSupplies)

                // Populate Sample Extra Products
                val defaultProducts = listOf(
                    ProductEntity(name = "Refresco Lobert 500ml", price = 12.0, costPrice = 7.0, stock = 24),
                    ProductEntity(name = "Snack Papas Fritas", price = 10.0, costPrice = 5.0, stock = 30),
                    ProductEntity(name = "Agua Mineral 600ml", price = 8.0, costPrice = 4.0, stock = 20)
                )
                defaultProducts.forEach { dao.insertProduct(it) }

                // Populate Settings
                val defaultSettings = listOf(
                    HotelSettingEntity("hotel_name", "Hotel Rivera"),
                    HotelSettingEntity("hotel_address", "Calle Principal 4-12, Zona 1"),
                    HotelSettingEntity("hotel_phone", "+502 7765-4321"),
                    HotelSettingEntity("hotel_nit", "1234567-8"),
                    HotelSettingEntity("invoice_next_num", "1"),
                    HotelSettingEntity("manager_pin", "1234"),
                    HotelSettingEntity("currency_symbol", "Q"),
                    HotelSettingEntity("alarm_15min", "true"),
                    HotelSettingEntity("sound_enabled", "true"),
                    HotelSettingEntity("total_rooms", "10")
                )
                dao.insertSettings(defaultSettings)

                // Populate Users
                val defaultUsers = listOf(
                    UserEntity(
                        username = "recepcion1",
                        fullName = "Recepción Turno Principal",
                        pinCode = "0000",
                        passwordHash = com.example.utils.SecurityUtils.hashPassword("0000"),
                        role = "RECEPCION",
                        isActive = true
                    ),
                    UserEntity(
                        username = "gerente",
                        fullName = "Gerencia Hotel Rivera",
                        pinCode = "1234",
                        passwordHash = com.example.utils.SecurityUtils.hashPassword("1234"),
                        role = "GERENTE",
                        isActive = true
                    ),
                    UserEntity(
                        username = "riverahotel01@gmail.com",
                        fullName = "Gerencia Rivera Hotel",
                        pinCode = "12345678",
                        passwordHash = com.example.utils.SecurityUtils.hashPassword("12345678"),
                        role = "GERENTE",
                        isActive = true
                    )
                )
                defaultUsers.forEach { dao.insertUser(it) }
            }
        }
    }
}
