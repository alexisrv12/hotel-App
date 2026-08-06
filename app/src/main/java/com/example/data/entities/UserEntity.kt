package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val pinCode: String = "",
    val passwordHash: String = "",
    val role: String = "RECEPCION", // "GERENTE" or "RECEPCION"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

