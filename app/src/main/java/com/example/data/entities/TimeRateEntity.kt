package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_rates")
data class TimeRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,               // e.g., "1 hora", "2 horas", "3 horas", "Día", "Noche"
    val durationMinutes: Long,      // e.g., 60, 120, 180, 1440, 720
    val price: Double,              // e.g., 50.0
    val isActive: Boolean = true,
    val isPromotional: Boolean = false
)
