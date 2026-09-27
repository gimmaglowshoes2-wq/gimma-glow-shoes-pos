package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CustomerMember
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ShoeService
import com.example.data.model.StoreConfig
import com.example.data.model.TransactionOrder
import com.example.data.repository.ShoePosRepository
import com.example.export.GoogleSheetsExporter
import com.example.printer.ThermalReceiptHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class PosTab(val label: String) {
    KASIR("Kasir"),
    PESANAN("Antrian & Cuci"),
    LAPORAN("Laporan Omset"),
    MEMBER("Pelanggan & Poin"),
    MENU("Menu Layanan"),
    PENGATURAN("Pengaturan"),
    PANDUAN("Petunjuk")
}

enum class ReportFilter(val label: String) {
    TODAY("Hari Ini"),
    THIS_WEEK("Minggu Ini"),
    THIS_MONTH("Bulan Ini"),
    LAST_MONTH("Bulan Lalu"),
    ALL("Semua")
}

data class CartItemDraft(
    val tempId: String = UUID.randomUUID().toString(),
    val service: ShoeService,
    val shoeBrandModel: String = "",
    val shoeColor: String = "",
    val shoeNotes: String = "",
    val quantity: Int = 1,
    val price: Double = service.price
) {
    val subtotal: Double get() = price * quantity
}

class ShoePosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ShoePosRepository(db)

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(PosTab.KASIR)
    val currentTab: StateFlow<PosTab> = _currentTab.asStateFlow()

    // Owner Security
    private val _isOwnerMode = MutableStateFlow(false)
    val isOwnerMode: StateFlow<Boolean> = _isOwnerMode.asStateFlow()

    // Store Config
    val storeConfig: StateFlow<StoreConfig> = repository.storeConfig
        .combine(MutableStateFlow(Unit)) { cfg, _ ->
            cfg ?: StoreConfig()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StoreConfig()
        )

    // Services & Members
    val activeServices: StateFlow<List<ShoeService>> = repository.activeServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allServices: StateFlow<List<ShoeService>> = repository.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMembers: StateFlow<List<CustomerMember>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<TransactionOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // POS Cart State
    private val _cartItems = MutableStateFlow<List<CartItemDraft>>(emptyList())
    val cartItems: StateFlow<List<CartItemDraft>> = _cartItems.asStateFlow()

    private val _selectedMember = MutableStateFlow<CustomerMember?>(null)
    val selectedMember: StateFlow<CustomerMember?> = _selectedMember.asStateFlow()

    private val _guestName = MutableStateFlow("")
    val guestName: StateFlow<String> = _guestName.asStateFlow()

    private val _guestPhone = MutableStateFlow("")
    val guestPhone: StateFlow<String> = _guestPhone.asStateFlow()

    private val _paymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val paymentMethod: StateFlow<PaymentMethod> = _paymentMethod.asStateFlow()

    private val _isPaidDirectly = MutableStateFlow(true)
    val isPaidDirectly: StateFlow<Boolean> = _isPaidDirectly.asStateFlow()

    private val _cashReceivedText = MutableStateFlow("")
    val cashReceivedText: StateFlow<String> = _cashReceivedText.asStateFlow()

    private val _usePoints = MutableStateFlow(0)
    val usePoints: StateFlow<Int> = _usePoints.asStateFlow()

    private val _orderNotes = MutableStateFlow("")
    val orderNotes: StateFlow<String> = _orderNotes.asStateFlow()

    private val _estimatedDays = MutableStateFlow(2)
    val estimatedDays: StateFlow<Int> = _estimatedDays.asStateFlow()

    // Orders Screen Filters
    private val _orderStatusFilter = MutableStateFlow<OrderStatus?>(null)
    val orderStatusFilter: StateFlow<OrderStatus?> = _orderStatusFilter.asStateFlow()

    private val _orderSearchQuery = MutableStateFlow("")
    val orderSearchQuery: StateFlow<String> = _orderSearchQuery.asStateFlow()

    // Reports Filter
    private val _reportFilter = MutableStateFlow(ReportFilter.TODAY)
    val reportFilter: StateFlow<ReportFilter> = _reportFilter.asStateFlow()

    // UI Feedback & Dialogs
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _receiptOrder = MutableStateFlow<Pair<TransactionOrder, List<OrderItem>>?>(null)
    val receiptOrder: StateFlow<Pair<TransactionOrder, List<OrderItem>>?> = _receiptOrder.asStateFlow()

    private val _autoWhatsAppTrigger = MutableStateFlow<Pair<TransactionOrder, List<OrderItem>>?>(null)
    val autoWhatsAppTrigger: StateFlow<Pair<TransactionOrder, List<OrderItem>>?> = _autoWhatsAppTrigger.asStateFlow()

    private val _cloudSyncing = MutableStateFlow(false)
    val cloudSyncing: StateFlow<Boolean> = _cloudSyncing.asStateFlow()

    // iOS Web POS Server
    val iosServer = com.example.server.IosPosServer(application, repository, viewModelScope)
    private val _iosServerUrl = MutableStateFlow("http://${com.example.server.IosPosServer.getLocalIpAddress(application)}:8080")
    val iosServerUrl: StateFlow<String> = _iosServerUrl.asStateFlow()
    private val _isIosServerRunning = MutableStateFlow(true)
    val isIosServerRunning: StateFlow<Boolean> = _isIosServerRunning.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initDefaultDataIfNeeded()
            iosServer.startServer(8080)
            _isIosServerRunning.value = true
        }
    }

    fun restartIosServer() {
        val ip = com.example.server.IosPosServer.getLocalIpAddress(getApplication())
        _iosServerUrl.value = "http://$ip:8080"
        iosServer.stopServer()
        iosServer.startServer(8080)
        _isIosServerRunning.value = true
        showToast("Server Web POS untuk iPhone aktif di: http://$ip:8080")
    }

    fun clearAutoWhatsAppTrigger() {
        _autoWhatsAppTrigger.value = null
    }

    fun setTab(tab: PosTab) {
        _currentTab.value = tab
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setOwnerMode(enabled: Boolean) {
        _isOwnerMode.value = enabled
    }

    fun verifyOwnerPin(pin: String): Boolean {
        val currentPin = storeConfig.value.ownerPin
        val success = pin == currentPin
        if (success) {
            _isOwnerMode.value = true
        }
        return success
    }

    // --- POS Cart Operations ---
    fun addServiceToCart(
        service: ShoeService,
        shoeBrandModel: String = "",
        shoeColor: String = "",
        shoeNotes: String = ""
    ) {
        val current = _cartItems.value.toMutableList()
        current.add(
            CartItemDraft(
                service = service,
                shoeBrandModel = shoeBrandModel,
                shoeColor = shoeColor,
                shoeNotes = shoeNotes,
                quantity = 1,
                price = service.price
            )
        )
        _cartItems.value = current

        // Auto update estimated days to max of items
        val maxDays = current.maxOfOrNull { it.service.estimatedDays } ?: 2
        if (maxDays > _estimatedDays.value) {
            _estimatedDays.value = maxDays
        }
    }

    fun updateCartItem(index: Int, updated: CartItemDraft) {
        val current = _cartItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _cartItems.value = current
        }
    }

    fun removeCartItem(index: Int) {
        val current = _cartItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _cartItems.value = current
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedMember.value = null
        _guestName.value = ""
        _guestPhone.value = ""
        _paymentMethod.value = PaymentMethod.CASH
        _isPaidDirectly.value = true
        _cashReceivedText.value = ""
        _usePoints.value = 0
        _orderNotes.value = ""
        _estimatedDays.value = 2
    }

    fun selectMember(member: CustomerMember?) {
        _selectedMember.value = member
        if (member != null) {
            _guestName.value = member.name
            _guestPhone.value = member.phone
            _usePoints.value = 0
        }
    }

    fun setGuestInfo(name: String, phone: String) {
        _guestName.value = name
        _guestPhone.value = phone
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
        if (method == PaymentMethod.LATER) {
            _isPaidDirectly.value = false
        }
    }

    fun setIsPaidDirectly(paid: Boolean) {
        _isPaidDirectly.value = paid
    }

    fun setCashReceived(amountStr: String) {
        _cashReceivedText.value = amountStr
    }

    fun setUsePoints(points: Int) {
        val maxAvailable = _selectedMember.value?.totalPoints ?: 0
        _usePoints.value = points.coerceIn(0, maxAvailable)
    }

    fun setOrderNotes(notes: String) {
        _orderNotes.value = notes
    }

    fun setEstimatedDays(days: Int) {
        _estimatedDays.value = days.coerceAtLeast(1)
    }

    fun calculateSubtotal(): Double {
        return _cartItems.value.sumOf { it.subtotal }
    }

    fun calculatePointsDiscount(): Double {
        // 1 Point = Rp 1
        return _usePoints.value.toDouble()
    }

    fun calculateFinalTotal(): Double {
        val sub = calculateSubtotal()
        val discount = calculatePointsDiscount()
        return (sub - discount).coerceAtLeast(0.0)
    }

    fun calculateChange(): Double {
        val cash = _cashReceivedText.value.toDoubleOrNull() ?: 0.0
        val total = calculateFinalTotal()
        return (cash - total).coerceAtLeast(0.0)
    }

    fun submitOrder(onSuccess: (Long) -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            showToast("Keranjang cucian masih kosong!")
            return
        }

        val name = _selectedMember.value?.name ?: _guestName.value.ifBlank { "Pelanggan Umum" }
        val phone = _selectedMember.value?.phone ?: _guestPhone.value.trim()

        val subtotal = calculateSubtotal()
        val pointsDiscount = calculatePointsDiscount()
        val finalTotal = calculateFinalTotal()

        val cashVal = _cashReceivedText.value.toDoubleOrNull() ?: 0.0
        val isPaid = _isPaidDirectly.value && _paymentMethod.value != PaymentMethod.LATER
        val paymentStatus = if (isPaid) "Lunas" else "Belum Lunas"
        val changeVal = if (isPaid && _paymentMethod.value == PaymentMethod.CASH) {
            (cashVal - finalTotal).coerceAtLeast(0.0)
        } else 0.0

        val config = storeConfig.value
        // Calculate points earned: default 5% of finalTotal
        val pointsEarned = (finalTotal * config.pointPerTransactionPercent).toInt()

        val order = TransactionOrder(
            invoiceNumber = repository.generateInvoiceNumber(),
            customerId = _selectedMember.value?.id,
            customerName = name,
            customerPhone = phone,
            createdAt = System.currentTimeMillis(),
            estimatedCompletion = System.currentTimeMillis() + (_estimatedDays.value * 24 * 60 * 60 * 1000L),
            orderStatus = OrderStatus.QUEUED.name,
            paymentMethod = _paymentMethod.value.label,
            paymentStatus = paymentStatus,
            subtotal = subtotal,
            discountAmount = pointsDiscount,
            pointsUsed = _usePoints.value,
            pointsEarned = if (_selectedMember.value != null) pointsEarned else 0,
            finalTotal = finalTotal,
            cashGiven = if (isPaid && _paymentMethod.value == PaymentMethod.CASH) cashVal else 0.0,
            changeGiven = changeVal,
            cashierName = "Kasir",
            notes = _orderNotes.value,
            isSyncedToCloud = false
        )

        val orderItems = items.map {
            OrderItem(
                orderId = 0,
                serviceId = it.service.id,
                serviceName = it.service.name,
                shoeBrandModel = it.shoeBrandModel,
                shoeColor = it.shoeColor,
                shoeNotes = it.shoeNotes,
                price = it.price,
                quantity = it.quantity
            )
        }

        viewModelScope.launch {
            val orderId = repository.createOrder(order, orderItems)
            val createdOrder = order.copy(id = orderId)
            val itemsWithOrderId = orderItems.map { it.copy(orderId = orderId) }

            _receiptOrder.value = Pair(createdOrder, itemsWithOrderId)
            clearCart()
            showToast("Pesanan #${createdOrder.invoiceNumber} berhasil disimpan!")
            onSuccess(orderId)

            // Auto print Bluetooth if configured
            if (config.autoPrintOnSave && config.bluetoothPrinterAddress.isNotBlank()) {
                val bytes = ThermalReceiptHelper.buildEscPosBytes(createdOrder, itemsWithOrderId, config)
                ThermalReceiptHelper.printToBluetoothDevice(config.bluetoothPrinterAddress, bytes)
            }

            // Auto send WhatsApp if enabled
            if (config.autoSendWhatsAppOnSave && createdOrder.customerPhone.isNotBlank()) {
                _autoWhatsAppTrigger.value = Pair(createdOrder, itemsWithOrderId)
            }
        }
    }

    // --- Order Queue Operations ---
    fun setOrderStatusFilter(status: OrderStatus?) {
        _orderStatusFilter.value = status
    }

    fun setOrderSearchQuery(query: String) {
        _orderSearchQuery.value = query
    }

    fun updateOrderStatus(order: TransactionOrder, newStatus: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, newStatus)
            showToast("Status #${order.invoiceNumber} diubah ke ${newStatus.label}")
        }
    }

    fun markOrderPaid(order: TransactionOrder, cashGiven: Double) {
        viewModelScope.launch {
            val change = (cashGiven - order.finalTotal).coerceAtLeast(0.0)
            repository.updateOrderPayment(order.id, "Lunas", cashGiven, change)
            showToast("Pesanan #${order.invoiceNumber} ditandai Lunas!")
        }
    }

    fun openReceiptForOrder(order: TransactionOrder) {
        viewModelScope.launch {
            val items = repository.getOrderItems(order.id)
            _receiptOrder.value = Pair(order, items)
        }
    }

    fun closeReceipt() {
        _receiptOrder.value = null
    }

    // --- Owner Management ---
    fun saveService(service: ShoeService) {
        viewModelScope.launch {
            if (service.id == 0L) {
                repository.insertService(service)
                showToast("Layanan '${service.name}' berhasil ditambahkan")
            } else {
                repository.updateService(service)
                showToast("Layanan '${service.name}' berhasil diperbarui")
            }
        }
    }

    fun deleteService(id: Long) {
        viewModelScope.launch {
            repository.deleteService(id)
            showToast("Layanan telah dihapus")
        }
    }

    fun saveMember(member: CustomerMember) {
        viewModelScope.launch {
            if (member.id == 0L) {
                repository.insertMember(member)
                showToast("Member '${member.name}' berhasil didaftarkan")
            } else {
                repository.updateMember(member)
                showToast("Data member '${member.name}' diperbarui")
            }
        }
    }

    fun deleteMember(id: Long) {
        viewModelScope.launch {
            repository.deleteMember(id)
            showToast("Member dihapus")
        }
    }

    fun updateStoreConfig(config: StoreConfig) {
        viewModelScope.launch {
            repository.updateStoreConfig(config)
            showToast("Pengaturan toko berhasil disimpan")
        }
    }

    fun resetMonthlyData(year: Int, month: Int) {
        viewModelScope.launch {
            val deletedCount = repository.resetMonthlyOrders(year, month)
            showToast("Berhasil meriset $deletedCount transaksi bulan tersebut.")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            val deletedCount = repository.resetAllOrders()
            showToast("Data transaksi telah dibersihkan ($deletedCount pesanan).")
        }
    }

    // --- Reports Filtering & Calculations ---
    fun setReportFilter(filter: ReportFilter) {
        _reportFilter.value = filter
    }

    fun getFilteredOrdersForReport(): List<TransactionOrder> {
        val orders = allOrders.value
        val cal = Calendar.getInstance()

        return when (_reportFilter.value) {
            ReportFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                orders.filter { it.createdAt >= start }
            }
            ReportFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                orders.filter { it.createdAt >= start }
            }
            ReportFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                orders.filter { it.createdAt >= start }
            }
            ReportFilter.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis - 1
                orders.filter { it.createdAt in start..end }
            }
            ReportFilter.ALL -> orders
        }
    }

    // --- Google Sheets & Cloud Sync ---
    fun exportToGoogleSheets(context: android.content.Context) {
        viewModelScope.launch {
            val orders = getFilteredOrdersForReport()
            val allItems = repository.getAllItemsSync()
            val itemsMap = allItems.groupBy { it.orderId }
            val csv = GoogleSheetsExporter.generateGoogleSheetsCsv(orders, itemsMap)
            GoogleSheetsExporter.shareCsvToGoogleSheets(context, csv)
        }
    }

    fun exportFullBackup(context: android.content.Context) {
        viewModelScope.launch {
            val orders = repository.getAllOrdersSync()
            val items = repository.getAllItemsSync()
            val members = allMembers.value
            val services = allServices.value
            val config = storeConfig.value
            val json = GoogleSheetsExporter.exportBackupJson(orders, items, members, services, config)
            GoogleSheetsExporter.shareBackupFile(context, json)
        }
    }

    fun triggerCloudSync() {
        val config = storeConfig.value
        if (config.cloudSyncWebhookUrl.isBlank()) {
            showToast("URL Cloud Sync belum diisi di Pengaturan.")
            return
        }

        viewModelScope.launch {
            _cloudSyncing.value = true
            val orders = repository.getAllOrdersSync()
            val items = repository.getAllItemsSync()
            val members = allMembers.value
            val services = allServices.value
            val json = GoogleSheetsExporter.exportBackupJson(orders, items, members, services, config)

            val res = GoogleSheetsExporter.syncToCloud(config.cloudSyncWebhookUrl, json)
            _cloudSyncing.value = false
            res.onSuccess {
                showToast(it)
                repository.updateStoreConfig(config.copy(cloudLastSyncTimestamp = System.currentTimeMillis()))
            }.onFailure {
                showToast(it.message ?: "Sync gagal")
            }
        }
    }
}
