package com.example.data.model

import com.google.firebase.firestore.DocumentId

data class VinculacionToken(
    @DocumentId val id: String = "",
    val pin: String = "",
    val qrToken: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaExpiracion: Long = System.currentTimeMillis() + 15 * 60 * 1000L,
    val activo: Boolean = true,
    val estado: String = "PENDIENTE",
    val hotelName: String = "Hotel Rivera",
    val rol: String = "RECEPCION",
    val dispositivoVinculadoId: String = "",
    val dispositivoVinculadoNombre: String = ""
)
