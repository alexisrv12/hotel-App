package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a maintenance request or broken item report in the hotel.
 * Supports room/location tagging, broken item categorization, urgency levels,
 * and photo attachments captured via the integrated camera module.
 */
@Entity(tableName = "maintenance_requests")
data class MaintenanceRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomNumber: String, // e.g., "101", "102", "Recepción", "Pasillo 2do Nivel", "Lavandería"
    val reportedBy: String, // Staff name or username
    val itemName: String, // e.g., "Aire Acondicionado", "Ducha / Grifo", "Cerradura", "Televisor"
    val category: String = "Plomería", // "Plomería", "Electricidad", "Climatización", "Cerrajería", "Electrónica", "Mobiliario", "Estructura", "Otro"
    val priority: String = "Normal", // "Baja", "Media", "Alta", "Urgente"
    val description: String = "",
    val photoPath: String? = null, // Local absolute path to the captured photo image
    val status: String = "PENDIENTE", // "PENDIENTE", "EN_REPARACION", "RESUELTO", "CANCELADO"
    val reportedTimestamp: Long = System.currentTimeMillis(),
    val resolvedTimestamp: Long? = null,
    val assignedTechnician: String? = null,
    val resolutionNotes: String? = null,
    val repairCost: Double? = null
)
