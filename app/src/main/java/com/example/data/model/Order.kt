package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String,
    val createdAt: Long = System.currentTimeMillis(),
    val estimatedCompletion: Long = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000L),
    val orderStatus: String = OrderStatus.QUEUED.name,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val paymentStatus: String = "Lunas", // "Lunas" or "Belum Lunas"
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val pointsUsed: Int = 0,
    val pointsEarned: Int = 0,
    val finalTotal: Double = 0.0,
    val cashGiven: Double = 0.0,
    val changeGiven: Double = 0.0,
    val cashierName: String = "Kasir",
    val notes: String = "",
    val isSyncedToCloud: Boolean = false,
    val completedAt: Long? = null,
    val takenAt: Long? = null
)

// Backward compatibility alias
typealias TransactionOrder = Order
