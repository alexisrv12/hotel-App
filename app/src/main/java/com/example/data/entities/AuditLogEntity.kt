package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val dateString: String,
    val timeString: String,
    val action: String,
    val details: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
