package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data model representing a hotel room in Cloud Firestore.
 * Supports room number, status (Available/Occupied/Cleaning), price/nightly rate,
 * guest details, and check-in timestamp.
 */
data class Room(
    @DocumentId
    val id: String = "",
    val roomNumber: String = "",
    val status: String = "Available", // "Available", "Occupied", "Cleaning", "Disponible", "Ocupada", "Limpieza"
    val price: Double = 150.0,
    val roomType: String = "Estándar",
    val clientName: String? = null,
    val clientDpi: String? = null,
    val checkInTimestamp: Long = 0L,
    val checkOutTimestamp: Long = 0L,
    val notes: String? = null
) {
    val isAvailable: Boolean
        get() = status.equals("Available", ignoreCase = true) || status.equals("Disponible", ignoreCase = true)

    val isOccupied: Boolean
        get() = status.equals("Occupied", ignoreCase = true) || status.equals("Ocupada", ignoreCase = true)

    val isCleaning: Boolean
        get() = status.equals("Cleaning", ignoreCase = true) || 
                status.equals("Limpieza", ignoreCase = true) ||
                status.equals("En Limpieza", ignoreCase = true) ||
                status.equals("Pend. Limpieza", ignoreCase = true)
}
