package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val totalPoints: Int = 0,
    val totalSpent: Double = 0.0,
    val totalWashCount: Int = 0,
    val tier: String = "Silver", // Silver, Gold, Platinum
    val joinDate: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    fun calculateTier(): String {
        return when {
            totalWashCount >= 15 || totalSpent >= 750000.0 -> "Platinum"
            totalWashCount >= 5 || totalSpent >= 250000.0 -> "Gold"
            else -> "Silver"
        }
    }
}

// Backward compatibility alias
typealias CustomerMember = Customer
