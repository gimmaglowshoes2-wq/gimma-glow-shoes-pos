package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val estimatedDays: Int = 2,
    val category: String = "Deep Clean",
    val description: String = "",
    val isActive: Boolean = true
)

// Backward compatibility alias
typealias ShoeService = MenuItem
