package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.CustomerMember
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.ShoeService
import com.example.data.model.StoreConfig
import com.example.data.model.TransactionOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ShoePosRepository(private val db: AppDatabase) {
    private val serviceDao = db.shoeServiceDao()
    private val memberDao = db.customerMemberDao()
    private val orderDao = db.transactionOrderDao()
    private val orderItemDao = db.orderItemDao()
    private val configDao = db.storeConfigDao()

    val activeServices: Flow<List<ShoeService>> = serviceDao.getActiveServices()
    val allServices: Flow<List<ShoeService>> = serviceDao.getAllServices()
    val allMembers: Flow<List<CustomerMember>> = memberDao.getAllMembers()
    val allOrders: Flow<List<TransactionOrder>> = orderDao.getAllOrders()
    val storeConfig: Flow<StoreConfig?> = configDao.getConfig()

    fun getOrdersByStatus(status: String): Flow<List<TransactionOrder>> = orderDao.getOrdersByStatus(status)
    fun getOrdersBetweenDates(start: Long, end: Long): Flow<List<TransactionOrder>> = orderDao.getOrdersBetweenDates(start, end)
    fun getItemsForOrder(orderId: Long): Flow<List<OrderItem>> = orderItemDao.getItemsForOrder(orderId)

    suspend fun initDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        // Init default store config
        if (configDao.getConfigSync() == null) {
            configDao.insertOrUpdateConfig(
                StoreConfig(
                    id = 1,
                    storeName = "GlowShoes Shoe Care",
                    storeAddress = "Jl. Merdeka No. 45, Kota",
                    storePhone = "0812-3456-7890",
                    ownerPin = "1234",
                    receiptFooter = "Terima kasih telah mempercayakan sepatu Anda di GlowShoes!\nGaransi klaim 2x24 jam setelah diambil.\nHarap bawa nota ini saat pengambilan.",
                    bluetoothPrinterName = "",
                    bluetoothPrinterAddress = "",
                    paperSizeWidth = 58,
                    autoPrintOnSave = true
                )
            )
        }

        // Init default services if empty
        val currentServices = serviceDao.getActiveServices()
        // We check if empty by querying once
        val sampleCheck = serviceDao.getServiceById(1)
        if (sampleCheck == null) {
            val defaults = listOf(
                ShoeService(name = "Fast Clean (Upper & Midsole)", price = 30000.0, estimatedDays = 1, category = "Fast Clean", description = "Cuci cepat bagian luar & midsole untuk sepatu kotor ringan."),
                ShoeService(name = "Deep Clean (Luar Dalam Sol)", price = 50000.0, estimatedDays = 2, category = "Deep Clean", description = "Cuci mendalam menyeluruh: upper, insole, lining, midsole, outsole & tali."),
                ShoeService(name = "Unyellowing Midsole Karet", price = 65000.0, estimatedDays = 3, category = "Treatment", description = "Menghilangkan noda kuning oksidasi pada midsole & sol karet agar putih kembali."),
                ShoeService(name = "Repaint / Cat Ulang Sneaker", price = 120000.0, estimatedDays = 4, category = "Repaint", description = "Pewarnaan ulang sepatu pudar atau custom warna dengan cat sneaker tahan lama."),
                ShoeService(name = "Reglue / Rekat Lem Sol", price = 45000.0, estimatedDays = 2, category = "Repair", description = "Rekat ulang sol yang lepas / mangap menggunakan adhesive sneaker grade."),
                ShoeService(name = "Leather & Suede Care", price = 75000.0, estimatedDays = 3, category = "Treatment", description = "Perawatan khusus bahan kulit/suede/nubuck + conditioner + water repellent."),
                ShoeService(name = "Sepatu Anak (Kids Clean)", price = 35000.0, estimatedDays = 2, category = "Fast Clean", description = "Pembersihan aman, higienis & wangi untuk sepatu anak-anak."),
                ShoeService(name = "White Shoes Booster", price = 55000.0, estimatedDays = 2, category = "Deep Clean", description = "Formula khusus mengangkat kusam & mencerahkan sepatu kanvas/mesh putih.")
            )
            serviceDao.insertAll(defaults)
        }

        // Init default member if empty
        val memberCheck = memberDao.getMemberById(1)
        if (memberCheck == null) {
            val defaultMembers = listOf(
                CustomerMember(
                    name = "Budi Pratama",
                    phone = "081234567891",
                    totalPoints = 1500,
                    totalSpent = 180000.0,
                    totalWashCount = 3,
                    tier = "Silver",
                    notes = "Pelanggan langganan Nike Dunk"
                ),
                CustomerMember(
                    name = "Citra Kirana",
                    phone = "082198765432",
                    totalPoints = 6500,
                    totalSpent = 550000.0,
                    totalWashCount = 9,
                    tier = "Gold",
                    notes = "Kolektor Jordan & New Balance"
                )
            )
            memberDao.insertAll(defaultMembers)
        }
    }

    suspend fun insertService(service: ShoeService): Long = withContext(Dispatchers.IO) {
        serviceDao.insertService(service)
    }

    suspend fun updateService(service: ShoeService) = withContext(Dispatchers.IO) {
        serviceDao.updateService(service)
    }

    suspend fun deleteService(id: Long) = withContext(Dispatchers.IO) {
        serviceDao.deleteServiceById(id)
    }

    suspend fun insertMember(member: CustomerMember): Long = withContext(Dispatchers.IO) {
        memberDao.insertMember(member)
    }

    suspend fun updateMember(member: CustomerMember) = withContext(Dispatchers.IO) {
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(id: Long) = withContext(Dispatchers.IO) {
        memberDao.deleteMemberById(id)
    }

    suspend fun searchMembers(query: String): Flow<List<CustomerMember>> = withContext(Dispatchers.IO) {
        memberDao.searchMembers(query)
    }

    suspend fun createOrder(order: TransactionOrder, items: List<OrderItem>): Long = withContext(Dispatchers.IO) {
        val orderId = orderDao.insertOrder(order)
        val itemsWithId = items.map { it.copy(orderId = orderId) }
        orderItemDao.insertItems(itemsWithId)

        // Update member points and stats if member is linked
        if (order.customerId != null && order.customerId > 0) {
            val member = memberDao.getMemberById(order.customerId)
            if (member != null) {
                val newPoints = (member.totalPoints - order.pointsUsed + order.pointsEarned).coerceAtLeast(0)
                val newSpent = member.totalSpent + order.finalTotal
                val newWashCount = member.totalWashCount + items.sumOf { it.quantity }
                val updatedMember = member.copy(
                    totalPoints = newPoints,
                    totalSpent = newSpent,
                    totalWashCount = newWashCount
                )
                val finalMember = updatedMember.copy(tier = updatedMember.calculateTier())
                memberDao.updateMember(finalMember)
            }
        }
        orderId
    }

    suspend fun updateOrderStatus(id: Long, newStatus: OrderStatus) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val completedAt = if (newStatus == OrderStatus.READY || newStatus == OrderStatus.COMPLETED) now else null
        val takenAt = if (newStatus == OrderStatus.COMPLETED) now else null
        orderDao.updateOrderStatus(id, newStatus.name, completedAt, takenAt)
    }

    suspend fun updateOrderPayment(id: Long, paymentStatus: String, cashGiven: Double, changeGiven: Double) = withContext(Dispatchers.IO) {
        orderDao.updatePaymentStatus(id, paymentStatus, cashGiven, changeGiven)
    }

    suspend fun getOrderById(id: Long): TransactionOrder? = withContext(Dispatchers.IO) {
        orderDao.getOrderById(id)
    }

    suspend fun getOrderItems(orderId: Long): List<OrderItem> = withContext(Dispatchers.IO) {
        orderItemDao.getItemsForOrderSync(orderId)
    }

    suspend fun deleteOrder(orderId: Long) = withContext(Dispatchers.IO) {
        orderItemDao.deleteItemsByOrderId(orderId)
        orderDao.deleteOrderById(orderId)
    }

    suspend fun resetMonthlyOrders(year: Int, month: Int): Int = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis - 1
        orderDao.deleteOrdersBetweenDates(start, end)
    }

    suspend fun resetAllOrders(): Int = withContext(Dispatchers.IO) {
        orderItemDao.deleteAllItems()
        orderDao.deleteAllOrders()
    }

    suspend fun updateStoreConfig(config: StoreConfig) = withContext(Dispatchers.IO) {
        configDao.insertOrUpdateConfig(config)
    }

    suspend fun verifyOwnerPin(inputPin: String): Boolean = withContext(Dispatchers.IO) {
        val cfg = configDao.getConfigSync()
        val correctPin = cfg?.ownerPin ?: "1234"
        inputPin == correctPin
    }

    suspend fun getAllOrdersSync(): List<TransactionOrder> = withContext(Dispatchers.IO) {
        orderDao.getAllOrdersSync()
    }

    suspend fun getAllItemsSync(): List<OrderItem> = withContext(Dispatchers.IO) {
        orderItemDao.getAllItemsSync()
    }

    fun generateInvoiceNumber(): String {
        val sdf = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
        return "GS-${sdf.format(Date())}"
    }
}
