package com.example.data.repository

import com.example.data.dao.DeviceDao
import com.example.data.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val deviceDao: DeviceDao) {

    /**
     * Flow emitting the list of all linked devices ordered by timestamp.
     */
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()

    /**
     * Flow emitting devices filtered by connection status.
     */
    fun getDevicesByStatus(status: String): Flow<List<DeviceEntity>> =
        deviceDao.getDevicesByStatus(status)

    /**
     * Retrieves a linked device by its internal database ID.
     */
    suspend fun getDeviceById(id: Long): DeviceEntity? =
        deviceDao.getDeviceById(id)

    /**
     * Retrieves a linked device by its unique device identifier string.
     */
    suspend fun getDeviceByDeviceId(deviceId: String): DeviceEntity? =
        deviceDao.getDeviceByDeviceId(deviceId)

    /**
     * Inserts a new linked device or replaces an existing one.
     */
    suspend fun insertDevice(device: DeviceEntity): Long =
        deviceDao.insertDevice(device)

    /**
     * Inserts a list of linked devices.
     */
    suspend fun insertDevices(devices: List<DeviceEntity>) =
        deviceDao.insertDevices(devices)

    /**
     * Updates an existing linked device entity.
     */
    suspend fun updateDevice(device: DeviceEntity) =
        deviceDao.updateDevice(device)

    /**
     * Deletes a linked device entity.
     */
    suspend fun deleteDevice(device: DeviceEntity) =
        deviceDao.deleteDevice(device)

    /**
     * Deletes a linked device by its database ID.
     */
    suspend fun deleteDeviceById(id: Long) =
        deviceDao.deleteDeviceById(id)

    /**
     * Deletes a linked device by its device identifier.
     */
    suspend fun deleteDeviceByDeviceId(deviceId: String) =
        deviceDao.deleteDeviceByDeviceId(deviceId)

    /**
     * Deletes all linked devices from the database.
     */
    suspend fun deleteAllDevices() =
        deviceDao.deleteAllDevices()
}
