package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplies")
data class SupplyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // e.g. "Jabón", "Papel Higiénico"
    val unit: String = "Unidad",         // e.g. "Pieza", "Paquete", "Botella"
    val stockCurrent: Double = 50.0,
    val stockMinimum: Double = 10.0,
    val autoDeductQuantityPerStay: Double = 1.0
)
