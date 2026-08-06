package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hotel_settings")
data class HotelSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
