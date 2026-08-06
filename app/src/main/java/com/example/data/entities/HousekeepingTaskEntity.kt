package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "housekeeping_tasks")
data class HousekeepingTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomNumber: String,
    val assignedStaffName: String,
    val assignedBy: String = "Gerencia",
    val priority: String = "Normal", // "Normal", "Alta", "Urgente"
    val status: String = "PENDIENTE", // "PENDIENTE", "EN_PROCESO", "COMPLETADA"
    val notes: String = "",
    val assignedTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long? = null
)
