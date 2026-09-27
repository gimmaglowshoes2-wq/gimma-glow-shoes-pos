package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printer.ThermalReceiptHelper
import com.example.ui.components.OwnerPinDialog
import com.example.ui.viewmodel.ReportFilter
import com.example.ui.viewmodel.ShoePosViewModel

@Composable
fun ReportScreen(
    viewModel: ShoePosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOwnerMode by viewModel.isOwnerMode.collectAsState()
    val reportFilter by viewModel.reportFilter.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }

    if (!isOwnerMode) {
        // Locked state: Must unlock as Owner to see detailed financial reports
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_report_locked_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Laporan Finansial Owner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Data omset, keuntungan, dan laporan performa bisnis diatur dan dilindungi PIN Owner.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("unlock_report_button")
                    ) {
                        Text("Buka Laporan dengan PIN")
                    }
                }
            }
        }

        if (showPinDialog) {
            OwnerPinDialog(
                title = "Akses Laporan Penjualan",
                onDismiss = { showPinDialog = false },
                onPinVerified = {
                    showPinDialog = false
                    viewModel.setOwnerMode(true)
                },
                verifyPin = { viewModel.verifyOwnerPin(it) }
            )
        }
        return
    }

    val filteredOrders = viewModel.getFilteredOrdersForReport()
    val totalRevenue = filteredOrders.sumOf { it.finalTotal }
    val totalOrdersCount = filteredOrders.size
    val paidOrders = filteredOrders.filter { it.paymentStatus == "Lunas" }
    val unpaidOrders = filteredOrders.filter { it.paymentStatus != "Lunas" }
    val totalPaidRevenue = paidOrders.sumOf { it.finalTotal }
    val totalUnpaidRevenue = unpaidOrders.sumOf { it.finalTotal }
    val avgTicket = if (totalOrdersCount > 0) totalRevenue / totalOrdersCount else 0.0

    // Payment methods breakdown
    val cashRevenue = filteredOrders.filter { it.paymentMethod.contains("Tunai", ignoreCase = true) }.sumOf { it.finalTotal }
    val qrisRevenue = filteredOrders.filter { it.paymentMethod.contains("QRIS", ignoreCase = true) }.sumOf { it.finalTotal }
    val transferRevenue = filteredOrders.filter { it.paymentMethod.contains("Transfer", ignoreCase = true) }.sumOf { it.finalTotal }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = reportFilter == filter,
                        onClick = { viewModel.setReportFilter(filter) },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("report_filter_${filter.name}")
                    )
                }
            }
        }

        // Action Banner: Export to Google Sheets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ekspor ke Google Sheets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Unduh laporan CSV rapi untuk diolah di Google Sheets / Excel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = { viewModel.exportToGoogleSheets(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("export_sheets_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor")
                    }
                }
            }
        }

        // Big Omset Highlight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Omset (${reportFilter.label})",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Aktif",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ThermalReceiptHelper.formatRupiah(totalRevenue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Pesanan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text("$totalOrdersCount Transaksi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        Column {
                            Text("Rata-rata Nota", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(ThermalReceiptHelper.formatRupiah(avgTicket), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        // Payment Methods Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rincian Metode Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Tunai
                    PaymentBreakdownRow(
                        title = "Tunai (Cash)",
                        amount = cashRevenue,
                        total = totalRevenue,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // QRIS
                    PaymentBreakdownRow(
                        title = "QRIS",
                        amount = qrisRevenue,
                        total = totalRevenue,
                        color = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Transfer
                    PaymentBreakdownRow(
                        title = "Transfer Bank",
                        amount = transferRevenue,
                        total = totalRevenue,
                        color = Color(0xFF8B5CF6)
                    )

                    if (totalUnpaidRevenue > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Belum Lunas / Piutang:", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            Text(ThermalReceiptHelper.formatRupiah(totalUnpaidRevenue), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Evaluasi Performa Bisnis Bulanan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Evaluasi Performa Bisnis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Tren Penjualan: ${if (totalOrdersCount > 0) "Berjalan aktif dengan rata-rata tiket ${ThermalReceiptHelper.formatRupiah(avgTicket)} per transaksi." else "Belum ada transaksi di periode ini."}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Kolektibilitas Kas: ${if (totalRevenue > 0) "${((totalPaidRevenue / totalRevenue) * 100).toInt()}% pembayaran telah lunas." else "-"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Tips Bisnis: Gunakan sistem poin loyalitas untuk mengajak pelanggan kembali cuci sepatu secara berkala!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Section: Riwayat Transaksi Terbaru di Periode Ini
        item {
            Text(
                text = "Daftar Transaksi (${filteredOrders.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(filteredOrders.take(20), key = { it.id }) { order ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "#${order.invoiceNumber} • ${order.customerName}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${ThermalReceiptHelper.formatDate(order.createdAt)} • ${order.paymentMethod} (${order.paymentStatus})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = ThermalReceiptHelper.formatRupiah(order.finalTotal),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun PaymentBreakdownRow(
    title: String,
    amount: Double,
    total: Double,
    color: Color
) {
    val percentage = if (total > 0) (amount / total).toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${ThermalReceiptHelper.formatRupiah(amount)} (${(percentage * 100).toInt()}%)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}
