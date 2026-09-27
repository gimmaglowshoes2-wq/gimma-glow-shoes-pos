package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Customer
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.StoreConfig

@Database(
    entities = [
        MenuItem::class,
        Customer::class,
        Order::class,
        OrderItem::class,
        StoreConfig::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuItemDao(): MenuItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun storeConfigDao(): StoreConfigDao

    // Backward compatibility methods
    fun shoeServiceDao(): MenuItemDao = menuItemDao()
    fun customerMemberDao(): CustomerDao = customerDao()
    fun transactionOrderDao(): OrderDao = orderDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glowshoes_pos.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
