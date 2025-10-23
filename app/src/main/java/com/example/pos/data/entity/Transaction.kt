package com.example.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val txnId: String,  // Format: TXNYYYYMMDD-001
    val buyerName: String?,
    val buyerPhone: String?,
    val dateTime: Date,
    val subtotal: Double,
    val discount: Double,
    val total: Double
)