package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stay_history")
data class StayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roomNumber: String,
    val clientName: String,
    val clientDpi: String? = null,
    val guestCount: Int = 1,
    val checkInTimeMillis: Long,
    val checkOutTimeMillis: Long,
    val contractedTimeName: String,
    val contractedDurationMinutes: Long,
    val actualDurationMinutes: Long,
    val priceCharged: Double,
    val paymentMethod: String = "Efectivo",
    val receptionistName: String = "Recepción",
    val notes: String? = null,
    val dateString: String // YYYY-MM-DD
)
