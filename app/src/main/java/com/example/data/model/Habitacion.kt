package com.example.data.model

import com.google.firebase.firestore.DocumentId

data class Habitacion(
    @DocumentId val id: String = "",
    val numero: String = "",
    val estado: String = "Disponible",
    val precio: Double = 0.0
)
