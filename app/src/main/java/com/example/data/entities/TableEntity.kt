package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

object TableStatus {
    const val LIBRE = "LIBRE"
    const val OCUPADA = "OCUPADA"
    const val RESERVADA = "RESERVADA"
    const val CUENTA_PEDIDA = "CUENTA_PEDIDA"
}

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tableNumber: String,
    val capacity: Int = 4,
    val status: String = TableStatus.LIBRE,
    val activeComandaId: Long? = null,
    val currentWaiter: String? = null,
    val occupiedSinceMillis: Long? = null
)
