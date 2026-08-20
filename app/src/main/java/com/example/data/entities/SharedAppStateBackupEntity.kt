package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity to store a local snapshot backup of the SharedAppState.
 * Ensures the application boots with full state and UI even completely offline.
 */
@Entity(tableName = "shared_app_state_backup")
data class SharedAppStateBackupEntity(
    @PrimaryKey
    val sessionId: String,
    val hotelName: String,
    val syncStatus: String,
    val jsonRooms: String,
    val jsonTasks: String,
    val jsonCashRegister: String,
    val jsonSyncLogs: String,
    val lastUpdatedByDevice: String,
    val lastActivityTimestamp: Long,
    val sessionToken: String,
    val lastBackupTimestamp: Long = System.currentTimeMillis()
)
