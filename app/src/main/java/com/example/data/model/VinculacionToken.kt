package com.example.data.model

import com.google.firebase.firestore.DocumentId

data class VinculacionToken(
    @DocumentId val id: String = "",
    val pin: String = "",
    val qrToken: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val activo: Boolean = true
)
