package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.StoreConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items ORDER BY category ASC, name ASC")
    fun getAllMenuItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE isActive = 1 ORDER BY category ASC, name ASC")
    fun getActiveMenuItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE id = :id LIMIT 1")
    suspend fun getMenuItemById(id: Long): MenuItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MenuItem>)

    @Update
    suspend fun updateMenuItem(item: MenuItem)

    @Query("DELETE FROM menu_items WHERE id = :id")
    suspend fun deleteMenuItemById(id: Long)

    // Aliases for compatibility
    fun getAllServices(): Flow<List<MenuItem>> = getAllMenuItems()
    fun getActiveServices(): Flow<List<MenuItem>> = getActiveMenuItems()
    suspend fun getServiceById(id: Long): MenuItem? = getMenuItemById(id)
    suspend fun insertService(service: MenuItem): Long = insertMenuItem(service)
    suspend fun updateService(service: MenuItem) = updateMenuItem(service)
    suspend fun deleteServiceById(id: Long) = deleteMenuItemById(id)
}

typealias ShoeServiceDao = MenuItemDao

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY totalSpent DESC, name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)

    // Aliases for compatibility
    fun getAllMembers(): Flow<List<Customer>> = getAllCustomers()
    suspend fun getMemberById(id: Long): Customer? = getCustomerById(id)
    fun searchMembers(query: String): Flow<List<Customer>> = searchCustomers(query)
    suspend fun insertMember(member: Customer): Long = insertCustomer(member)
    suspend fun updateMember(member: Customer) = updateCustomer(member)
    suspend fun deleteMemberById(id: Long) = deleteCustomerById(id)
}

typealias CustomerMemberDao = CustomerDao

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderStatus = :status ORDER BY createdAt DESC")
    fun getOrdersByStatus(status: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): Order?

    @Query("SELECT * FROM orders WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    fun getOrdersBetweenDates(start: Long, end: Long): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    suspend fun getOrdersBetweenDatesSync(start: Long, end: Long): List<Order>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersSync(): List<Order>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("UPDATE orders SET orderStatus = :status, completedAt = :completedAt, takenAt = :takenAt WHERE id = :id")
    suspend fun updateOrderStatus(id: Long, status: String, completedAt: Long?, takenAt: Long?)

    @Query("UPDATE orders SET paymentStatus = :paymentStatus, cashGiven = :cashGiven, changeGiven = :changeGiven WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, paymentStatus: String, cashGiven: Double, changeGiven: Double)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrderById(id: Long)

    @Query("DELETE FROM orders WHERE createdAt BETWEEN :start AND :end")
    suspend fun deleteOrdersBetweenDates(start: Long, end: Long): Int

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders(): Int

    @Query("SELECT * FROM orders WHERE isSyncedToCloud = 0")
    suspend fun getUnsyncedOrders(): List<Order>

    @Query("UPDATE orders SET isSyncedToCloud = 1 WHERE id IN (:ids)")
    suspend fun markOrdersAsSynced(ids: List<Long>)
}

typealias TransactionOrderDao = OrderDao

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getItemsForOrder(orderId: Long): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrderSync(orderId: Long): List<OrderItem>

    @Query("SELECT * FROM order_items")
    suspend fun getAllItemsSync(): List<OrderItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OrderItem>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsByOrderId(orderId: Long)

    @Query("DELETE FROM order_items")
    suspend fun deleteAllItems(): Int
}

@Dao
interface StoreConfigDao {
    @Query("SELECT * FROM store_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<StoreConfig?>

    @Query("SELECT * FROM store_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): StoreConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: StoreConfig)
}
