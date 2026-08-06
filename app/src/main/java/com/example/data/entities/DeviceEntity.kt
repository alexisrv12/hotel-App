package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

object DeviceConnectionStatus {
    const val CONNECTED = "CONNECTED"
    const val DISCONNECTED = "DISCONNECTED"
    const val PENDING = "PENDING"
}

object RealTimeConnectivityStatus {
    const val ACTIVE = "Active"
    const val DISCONNECTED = "Disconnected"
}

@Entity(tableName = "linked_devices")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String = "",
    val name: String,
    val userAssigned: String,
    val timestamp: Long = System.currentTimeMillis(),
    val connectionStatus: String = DeviceConnectionStatus.CONNECTED,
    val realTimeConnectivityStatus: String = RealTimeConnectivityStatus.ACTIVE,
    val lastHeartbeat: Long = System.currentTimeMillis(),
    val ipAddress: String? = null
) {
    /**
     * Checks if device is currently active based on a heartbeat timeout (default 30 seconds).
     */
    fun isCurrentlyActive(timeoutMs: Long = 30000L): Boolean {
        return (System.currentTimeMillis() - lastHeartbeat) <= timeoutMs &&
                connectionStatus != DeviceConnectionStatus.DISCONNECTED
    }

    /**
     * Computes the real-time connectivity status string ('Active' or 'Disconnected').
     */
    fun computedRealTimeStatus(timeoutMs: Long = 30000L): String {
        return if (isCurrentlyActive(timeoutMs)) RealTimeConnectivityStatus.ACTIVE else RealTimeConnectivityStatus.DISCONNECTED
    }
}

