package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderStatus
import com.example.data.model.TransactionOrder
import com.example.printer.ThermalReceiptHelper
import com.example.ui.viewmodel.ShoePosViewModel

@Composable
fun OrdersScreen(
    viewModel: ShoePosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allOrders by viewModel.allOrders.collectAsState()
    val statusFilter by viewModel.orderStatusFilter.collectAsState()
    val searchQuery by viewModel.orderSearchQuery.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var showPaymentDialogForOrder by remember { mutableStateOf<TransactionOrder?>(null) }
    var payAmountText by remember { mutableStateOf("") }

    val filteredOrders = remember(allOrders, statusFilter, searchQuery) {
        allOrders.filter { order ->
            val matchStatus = statusFilter == null || order.orderStatus == statusFilter?.name
            val matchQuery = searchQuery.isBlank() ||
                    order.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    order.customerName.contains(searchQuery, ignoreCase = true) ||
                    order.customerPhone.contains(searchQuery)
            matchStatus && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setOrderSearchQuery(it) },
            label = { Text("Cari Nota, Pelanggan, atau No WA") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setOrderSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("orders_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips (Horizontal Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { viewModel.setOrderStatusFilter(null) },
                label = { Text("Semua (${allOrders.size})") },
                modifier = Modifier.testTag("filter_all_orders")
            )
            OrderStatus.entries.forEach { status ->
                val count = allOrders.count { it.orderStatus == status.name }
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { viewModel.setOrderStatusFilter(status) },
                    label = { Text("${status.label} ($count)") },
                    modifier = Modifier.testTag("filter_${status.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List of Orders
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocalLaundryService,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak ada pesanan cuci yang cocok",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    OrderCardItem(
                        order = order,
                        onUpdateStatus = { newStatus -> viewModel.updateOrderStatus(order, newStatus) },
                        onOpenReceipt = { viewModel.openReceiptForOrder(order) },
                        onPayOrder = {
                            showPaymentDialogForOrder = order
                            payAmountText = order.finalTotal.toLong().toString()
                        },
                        onSendPickupWa = {
                            ThermalReceiptHelper.sendPickupNotificationViaWhatsApp(context, order, storeConfig)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Modal: Pelunasan Sisa Pembayaran
    if (showPaymentDialogForOrder != null) {
        val o = showPaymentDialogForOrder!!
        AlertDialog(
            onDismissRequest = { showPaymentDialogForOrder = null },
            title = { Text("Pelunasan Nota #${o.invoiceNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Total Tagihan: ${ThermalReceiptHelper.formatRupiah(o.finalTotal)}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text("Nominal Tunai Diterima") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("settle_payment_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmountText.toDoubleOrNull() ?: o.finalTotal
                        viewModel.markOrderPaid(o, amount)
                        showPaymentDialogForOrder = null
                    },
                    modifier = Modifier.testTag("confirm_settle_button")
                ) {
                    Text("Tandai Lunas")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPaymentDialogForOrder = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun OrderCardItem(
    order: TransactionOrder,
    onUpdateStatus: (OrderStatus) -> Unit,
    onOpenReceipt: () -> Unit,
    onPayOrder: () -> Unit,
    onSendPickupWa: () -> Unit
) {
    val currentStatus = OrderStatus.fromLabel(order.orderStatus)

    val statusColor = when (currentStatus) {
        OrderStatus.QUEUED -> Color(0xFFF59E0B) // Amber
        OrderStatus.WASHING -> Color(0xFF0284C7) // Blue
        OrderStatus.DRYING -> Color(0xFF8B5CF6) // Purple
        OrderStatus.READY -> Color(0xFF10B981) // Green
        OrderStatus.COMPLETED -> Color(0xFF64748B) // Slate
        OrderStatus.CANCELLED -> Color(0xFFEF4444) // Red
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Invoice & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "#${order.invoiceNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Masuk: ${ThermalReceiptHelper.formatDate(order.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentStatus.label,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Customer Info & Estimation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pelanggan: ${order.customerName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (order.customerPhone.isNotBlank()) {
                        Text(
                            text = "WA: ${order.customerPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Estimasi Selesai:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = ThermalReceiptHelper.formatDateOnly(order.estimatedCompletion),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Payment Status & Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.paymentMethod,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (order.paymentStatus == "Lunas") Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = order.paymentStatus,
                            color = if (order.paymentStatus == "Lunas") Color(0xFF10B981) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = ThermalReceiptHelper.formatRupiah(order.finalTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (order.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${order.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Receipt Button
                OutlinedButton(
                    onClick = onOpenReceipt,
                    modifier = Modifier.weight(1f).height(42.dp).testTag("receipt_btn_${order.id}")
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Struk", fontSize = 12.sp)
                }

                // If not paid, show settle payment button
                if (order.paymentStatus != "Lunas") {
                    FilledTonalButton(
                        onClick = onPayOrder,
                        modifier = Modifier.weight(1.2f).height(42.dp).testTag("pay_btn_${order.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pelunasan", fontSize = 12.sp)
                    }
                }

                // WhatsApp notify button if READY
                if (currentStatus == OrderStatus.READY && order.customerPhone.isNotBlank()) {
                    Button(
                        onClick = onSendPickupWa,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1.2f).height(42.dp).testTag("notify_wa_btn_${order.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WA Siap", fontSize = 12.sp)
                    }
                }

                // Step Progression Button
                val (nextStatus, nextLabel) = when (currentStatus) {
                    OrderStatus.QUEUED -> Pair(OrderStatus.WASHING, "Mulai Cuci")
                    OrderStatus.WASHING -> Pair(OrderStatus.DRYING, "Keringkan")
                    OrderStatus.DRYING -> Pair(OrderStatus.READY, "Siap Ambil")
                    OrderStatus.READY -> Pair(OrderStatus.COMPLETED, "Diserahkan")
                    OrderStatus.COMPLETED -> Pair(null, "Selesai")
                    OrderStatus.CANCELLED -> Pair(null, "-")
                }

                if (nextStatus != null) {
                    Button(
                        onClick = { onUpdateStatus(nextStatus) },
                        modifier = Modifier.weight(1.3f).height(42.dp).testTag("next_status_btn_${order.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(nextLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
