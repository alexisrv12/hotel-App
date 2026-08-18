package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

object ComandaStatus {
    const val PENDIENTE = "PENDIENTE"
    const val EN_COCINA = "EN_COCINA"
    const val LISTO = "LISTO"
    const val ENTREGADO = "ENTREGADO"
    const val COBRADO = "COBRADO"
    const val CANCELADO = "CANCELADO"
}

@Entity(tableName = "comandas")
data class ComandaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val comandaNumber: String,
    val tableNumber: String,
    val waiterName: String,
    val status: String = ComandaStatus.PENDIENTE,
    val itemsJson: String = "[]", // JSON array of items: [{"productId": 1, "name": "Refresco", "quantity": 2, "price": 12.0, "notes": "Sin hielo"}]
    val notes: String? = null,
    val totalAmount: Double = 0.0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val roomId: Long? = null, // Optional link to hotel room if charged to room
    val isSynced: Boolean = true
)
