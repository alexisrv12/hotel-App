package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_records")
data class SaleRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val profit: Double,
    val timestampMillis: Long = System.currentTimeMillis(),
    val registeredBy: String = "Recepción",
    val paymentMethod: String = "Efectivo"
)
