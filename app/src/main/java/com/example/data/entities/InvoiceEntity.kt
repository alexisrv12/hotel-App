package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val stayHistoryId: Long? = null,
    val hotelName: String,
    val hotelAddress: String,
    val hotelPhone: String,
    val hotelNit: String,
    val dateString: String,
    val timeString: String,
    val roomNumber: String,
    val clientName: String,
    val contractedTime: String,
    val checkInTime: String,
    val checkOutTime: String,
    val price: Double,
    val discount: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String,
    val receptionistName: String,
    val isVoided: Boolean = false,
    val voidedBy: String? = null,
    val voidReason: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)
