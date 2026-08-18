package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

object SyncOperationStatus {
    const val PENDING = "PENDING"
    const val IN_FLIGHT = "IN_FLIGHT"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
}

object SyncOperationType {
    const val ROOM_CHECKIN = "ROOM_CHECKIN"
    const val ROOM_CHECKOUT = "ROOM_CHECKOUT"
    const val ROOM_STATUS_UPDATE = "ROOM_STATUS_UPDATE"
    const val COMANDA_CREATE = "COMANDA_CREATE"
    const val COMANDA_STATUS_UPDATE = "COMANDA_STATUS_UPDATE"
    const val TABLE_STATUS_UPDATE = "TABLE_STATUS_UPDATE"
    const val PAYMENT_REGISTER = "PAYMENT_REGISTER"
    const val SUPPLY_DEDUCT = "SUPPLY_DEDUCT"
    const val INVOICE_CREATE = "INVOICE_CREATE"
}

@Entity(tableName = "offline_sync_queue")
data class OfflineSyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationId: String = UUID.randomUUID().toString(),
    val operationType: String,
    val entityType: String,
    val entityId: String,
    val payloadJson: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = SyncOperationStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null
)
