package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roomNumber: String,
    val roomId: Long,
    val clientName: String,
    val clientDpi: String? = null,
    val clientPhone: String? = null,
    val guestCount: Int = 1,
    val reservationDateMillis: Long, // Check-in target date millis at 00:00:00
    val checkInDateString: String,   // Format: "yyyy-MM-dd" or "dd/MM/yyyy"
    val checkInTime: String = "14:00",
    val durationText: String = "24 Horas (Día completo)",
    val durationMinutes: Long = 1440,
    val rateName: String = "Tarifa General",
    val totalPrice: Double = 0.0,
    val advancePayment: Double = 0.0,
    val paymentMethod: String = "Efectivo",
    val notes: String? = null,
    val status: String = "CONFIRMADA", // "CONFIRMADA", "CHECKED_IN", "CANCELADA"
    val createdAtMillis: Long = System.currentTimeMillis()
)
