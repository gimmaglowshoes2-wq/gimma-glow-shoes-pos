package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long = 0,
    val serviceId: Long? = null,
    val serviceName: String,
    val shoeBrandModel: String = "",
    val shoeColor: String = "",
    val shoeNotes: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
) {
    val totalPrice: Double
        get() = price * quantity
}
