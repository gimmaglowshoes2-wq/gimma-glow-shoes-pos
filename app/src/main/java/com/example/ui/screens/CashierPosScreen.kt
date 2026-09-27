package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerMember
import com.example.data.model.PaymentMethod
import com.example.data.model.ShoeService
import com.example.printer.ThermalReceiptHelper
import com.example.ui.viewmodel.CartItemDraft
import com.example.ui.viewmodel.ShoePosViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CashierPosScreen(
    viewModel: ShoePosViewModel,
    modifier: Modifier = Modifier
) {
    val activeServices by viewModel.activeServices.collectAsState()
    val allMembers by viewModel.allMembers.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val selectedMember by viewModel.selectedMember.collectAsState()
    val guestName by viewModel.guestName.collectAsState()
    val guestPhone by viewModel.guestPhone.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val isPaidDirectly by viewModel.isPaidDirectly.collectAsState()
    val cashReceivedText by viewModel.cashReceivedText.collectAsState()
    val usePoints by viewModel.usePoints.collectAsState()
    val orderNotes by viewModel.orderNotes.collectAsState()
    val estimatedDays by viewModel.estimatedDays.collectAsState()

    var showShoeDetailDialogForService by remember { mutableStateOf<ShoeService?>(null) }
    var shoeBrandInput by remember { mutableStateOf("") }
    var shoeColorInput by remember { mutableStateOf("") }
    var shoeConditionNotes by remember { mutableStateOf("") }
    var shoeQtyInput by remember { mutableStateOf(1) }

    var showMemberPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Customer Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_info_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selectedMember != null) Icons.Default.CardMembership else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (selectedMember != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedMember != null) "Member: ${selectedMember?.name}" else "Pelanggan Umum",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = { showMemberPicker = true },
                            modifier = Modifier.testTag("select_member_button")
                        ) {
                            Text(if (selectedMember != null) "Ganti Member" else "+ Pilih Member")
                        }
                    }

                    if (selectedMember != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Tier: ${selectedMember?.tier} ⭐",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Poin: ${selectedMember?.totalPoints} pts",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.selectMember(null) }) {
                                Text("Lepas Member", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = guestName,
                                onValueChange = { viewModel.setGuestInfo(it, guestPhone) },
                                label = { Text("Nama Pelanggan") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("guest_name_input")
                            )
                            OutlinedTextField(
                                value = guestPhone,
                                onValueChange = { viewModel.setGuestInfo(guestName, it) },
                                label = { Text("No. WhatsApp") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("guest_phone_input")
                            )
                        }
                    }
                }
            }
        }

        // Section: Menu Layanan Cuci Sepatu (Quick Select)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Layanan Cuci Sepatu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeServices.size} menu aktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeServices.forEach { service ->
                        Card(
                            modifier = Modifier
                                .clickable {
                                    showShoeDetailDialogForService = service
                                    shoeBrandInput = ""
                                    shoeColorInput = ""
                                    shoeConditionNotes = ""
                                    shoeQtyInput = 1
                                }
                                .testTag("service_card_${service.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = service.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${ThermalReceiptHelper.formatRupiah(service.price)} • ${service.estimatedDays} hr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Keranjang Cucian (Order Items)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Sepatu Masuk (${cartItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Kosongkan", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (cartItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada sepatu di keranjang",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Pilih salah satu layanan cuci di atas untuk menambahkan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            itemsIndexed(cartItems) { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cart_item_$index"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${item.service.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.shoeBrandModel.isNotBlank() || item.shoeColor.isNotBlank()) {
                                Text(
                                    text = "Sepatu: ${item.shoeBrandModel} (${item.shoeColor})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.shoeNotes.isNotBlank()) {
                                Text(
                                    text = "Catatan: ${item.shoeNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = "${ThermalReceiptHelper.formatRupiah(item.price)} x ${item.quantity} = ${ThermalReceiptHelper.formatRupiah(item.subtotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.removeCartItem(index) },
                            modifier = Modifier.testTag("remove_cart_item_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Section: Estimasi & Poin Loyalitas & Catatan Order
        if (cartItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimasi Pengerjaan (Hari):",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { viewModel.setEstimatedDays(estimatedDays - 1) },
                                    enabled = estimatedDays > 1
                                ) { Text("-") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$estimatedDays Hari",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(onClick = { viewModel.setEstimatedDays(estimatedDays + 1) }) { Text("+") }
                            }
                        }

                        // Member Points Redemption
                        if (selectedMember != null && (selectedMember?.totalPoints ?: 0) > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Tukar Poin Member",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Tersedia: ${selectedMember?.totalPoints} pts (1 pt = Rp 1)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (usePoints > 0) {
                                        TextButton(onClick = { viewModel.setUsePoints(0) }) {
                                            Text("Batal")
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            val maxCanUse = (selectedMember?.totalPoints ?: 0).coerceAtMost(viewModel.calculateSubtotal().toInt())
                                            viewModel.setUsePoints(maxCanUse)
                                        }
                                    ) {
                                        Text(if (usePoints > 0) "Pakai $usePoints pts" else "Pakai Poin")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = orderNotes,
                            onValueChange = { viewModel.setOrderNotes(it) },
                            label = { Text("Catatan Tambahan untuk Kasir / Tim Cuci") },
                            placeholder = { Text("Misal: Tali lepas saat dicuci, jangan kena air sol dalam") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section: Pembayaran & Nominal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Metode Pembayaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { viewModel.setPaymentMethod(method) },
                                    label = { Text(method.label, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f).testTag("pay_chip_${method.name}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (paymentMethod == PaymentMethod.CASH) {
                            Column {
                                OutlinedTextField(
                                    value = cashReceivedText,
                                    onValueChange = { viewModel.setCashReceived(it) },
                                    label = { Text("Uang Tunai Diterima (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("cash_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                // Quick Nominal Buttons
                                val finalTotal = viewModel.calculateFinalTotal()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { viewModel.setCashReceived(finalTotal.toLong().toString()) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Pas", fontSize = 11.sp) }

                                    FilledTonalButton(
                                        onClick = { viewModel.setCashReceived("50000") },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("50k", fontSize = 11.sp) }

                                    FilledTonalButton(
                                        onClick = { viewModel.setCashReceived("100000") },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("100k", fontSize = 11.sp) }

                                    FilledTonalButton(
                                        onClick = { viewModel.setCashReceived("200000") },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("200k", fontSize = 11.sp) }
                                }

                                val change = viewModel.calculateChange()
                                if (cashReceivedText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Kembalian:", fontWeight = FontWeight.Bold)
                                        Text(
                                            ThermalReceiptHelper.formatRupiah(change),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Totals Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:")
                            Text(ThermalReceiptHelper.formatRupiah(viewModel.calculateSubtotal()))
                        }

                        if (usePoints > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Diskon Poin:")
                                Text("-${ThermalReceiptHelper.formatRupiah(viewModel.calculatePointsDiscount())}", color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL BAYAR:",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ThermalReceiptHelper.formatRupiah(viewModel.calculateFinalTotal()),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Main Action Button: Checkout & Print
                        Button(
                            onClick = {
                                viewModel.submitOrder {
                                    // Order submitted successfully, receipt dialog opens automatically
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("submit_order_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PROSES & CETAK STRUK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Modal: Detail Sepatu Input Dialog when tapping a Service
    if (showShoeDetailDialogForService != null) {
        val s = showShoeDetailDialogForService!!
        AlertDialog(
            onDismissRequest = { showShoeDetailDialogForService = null },
            title = {
                Text(text = "Detail Sepatu: ${s.name}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Biaya: ${ThermalReceiptHelper.formatRupiah(s.price)} (${s.estimatedDays} hari kerja)",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = shoeBrandInput,
                        onValueChange = { shoeBrandInput = it },
                        label = { Text("Merk / Model Sepatu") },
                        placeholder = { Text("Contoh: Nike Air Jordan 1 / Converse 70s") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("shoe_brand_input")
                    )
                    OutlinedTextField(
                        value = shoeColorInput,
                        onValueChange = { shoeColorInput = it },
                        label = { Text("Warna Sepatu") },
                        placeholder = { Text("Contoh: Putih / Hitam Merah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("shoe_color_input")
                    )
                    OutlinedTextField(
                        value = shoeConditionNotes,
                        onValueChange = { shoeConditionNotes = it },
                        label = { Text("Kondisi / Catatan Noda Khusus") },
                        placeholder = { Text("Contoh: Sol menguning, noda kopi di toebox") },
                        modifier = Modifier.fillMaxWidth().testTag("shoe_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addServiceToCart(
                            service = s,
                            shoeBrandModel = shoeBrandInput.ifBlank { "Sneakers" },
                            shoeColor = shoeColorInput.ifBlank { "Standar" },
                            shoeNotes = shoeConditionNotes
                        )
                        showShoeDetailDialogForService = null
                    },
                    modifier = Modifier.testTag("confirm_add_shoe_button")
                ) {
                    Text("Tambahkan ke Nota")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showShoeDetailDialogForService = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal: Member Picker
    if (showMemberPicker) {
        var query by remember { mutableStateOf("") }
        val filtered = allMembers.filter {
            it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
        }

        AlertDialog(
            onDismissRequest = { showMemberPicker = false },
            title = {
                Text(text = "Pilih Member Pelanggan", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.height(350.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Cari nama atau No WA") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered.size) { idx ->
                            val m = filtered[idx]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectMember(m)
                                        showMemberPicker = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(m.name, fontWeight = FontWeight.Bold)
                                        Text("WA: ${m.phone} • Tier: ${m.tier}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("${m.totalPoints} Pts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemberPicker = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}
