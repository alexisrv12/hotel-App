package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM linked_devices ORDER BY timestamp DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM linked_devices WHERE id = :id")
    suspend fun getDeviceById(id: Long): DeviceEntity?

    @Query("SELECT * FROM linked_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceByDeviceId(deviceId: String): DeviceEntity?

    @Query("SELECT * FROM linked_devices WHERE connectionStatus = :status")
    fun getDevicesByStatus(status: String): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("DELETE FROM linked_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)

    @Query("DELETE FROM linked_devices WHERE deviceId = :deviceId")
    suspend fun deleteDeviceByDeviceId(deviceId: String)

    @Query("DELETE FROM linked_devices")
    suspend fun deleteAllDevices()
}
