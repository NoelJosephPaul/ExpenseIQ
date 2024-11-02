package com.example.expenseiq

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.w3c.dom.Text

@Entity(tableName = "pending_payments")
data class PendingPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Float,
    val date: String,
    val description: String? = null
)