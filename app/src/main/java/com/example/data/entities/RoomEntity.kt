package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

object RoomStatus {
    const val DISPONIBLE = "DISPONIBLE"
    const val OCUPADA = "OCUPADA"
    const val PENDIENTE_LIMPIEZA = "PENDIENTE_LIMPIEZA"
    const val EN_LIMPIEZA = "EN_LIMPIEZA"
    const val RESERVADA = "RESERVADA"

    // Standard English status constants
    const val AVAILABLE = "DISPONIBLE"
    const val OCCUPIED = "OCUPADA"
    const val CLEANING = "PENDIENTE_LIMPIEZA"

    fun isAvailable(status: String): Boolean =
        status.equals(DISPONIBLE, ignoreCase = true) || status.equals("Available", ignoreCase = true)

    fun isOccupied(status: String): Boolean =
        status.equals(OCUPADA, ignoreCase = true) || status.equals("Occupied", ignoreCase = true)

    fun isCleaning(status: String): Boolean =
        status.equals(PENDIENTE_LIMPIEZA, ignoreCase = true) || status.equals(EN_LIMPIEZA, ignoreCase = true) || status.equals("Cleaning", ignoreCase = true)
}

typealias Room = RoomEntity

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roomNumber: String,
    val roomType: String = "Estándar",
    val status: String = RoomStatus.DISPONIBLE,
    val capacity: Int = 2,
    val nightlyRate: Double = 150.0,
    val clientName: String? = null,
    val clientDpi: String? = null,
    val guestCount: Int = 1,
    val rateName: String? = null,
    val priceCharged: Double = 0.0,
    val checkInTimeMillis: Long = 0L,
    val checkOutTimeMillis: Long = 0L,
    val contractedDurationMinutes: Long = 0L,
    val receptionistName: String? = null,
    val notes: String? = null,
    val cleaningStartTimeMillis: Long = 0L,
    val cleaningFinishedBy: String? = null,
    val sortOrder: Int = 0
) {
    val isAvailable: Boolean get() = RoomStatus.isAvailable(status)
    val isOccupied: Boolean get() = RoomStatus.isOccupied(status)
    val isCleaning: Boolean get() = RoomStatus.isCleaning(status)
}
