package com.example.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val id: Int = 1, // Only one row will exist
    val stallName: String,
    val address: String,
    val phone: String,
    val pin: String,
    val printerMac: String?,
    val paddingTop: Int,
    val paddingBottom: Int,
    val printerWidth: Int,
    val linesBeforeCut: Int
)