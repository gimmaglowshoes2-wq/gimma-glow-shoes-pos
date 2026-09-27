package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShoeService
import com.example.printer.ThermalReceiptHelper
import com.example.ui.components.OwnerPinDialog
import com.example.ui.viewmodel.ShoePosViewModel

@Composable
fun MenuManageScreen(
    viewModel: ShoePosViewModel,
    modifier: Modifier = Modifier
) {
    val isOwnerMode by viewModel.isOwnerMode.collectAsState()
    val allServices by viewModel.allServices.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ShoeService?>(null) }
    var isNewServiceDialog by remember { mutableStateOf(false) }

    if (!isOwnerMode) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_menu_locked_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Kelola Menu & Harga (Owner)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Penambahan, perubahan harga, dan penghapusan menu layanan cuci sepatu hanya dapat diatur oleh Owner.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("unlock_menu_button")
                    ) {
                        Text("Buka Menu dengan PIN Owner")
                    }
                }
            }
        }

        if (showPinDialog) {
            OwnerPinDialog(
                title = "Akses Kelola Menu & Harga",
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ubah harga & durasi pengerjaan sesuai kebutuhan bisnis Anda. Perubahan akan langsung aktif di menu kasir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allServices, key = { it.id }) { service ->
                    ServiceItemCard(
                        service = service,
                        onEdit = { editingService = service },
                        onDelete = { viewModel.deleteService(service.id) },
                        onToggleActive = { active ->
                            viewModel.saveService(service.copy(isActive = active))
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { isNewServiceDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_service_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Layanan Baru")
        }
    }

    // Modal: Add / Edit Service Dialog
    if (isNewServiceDialog || editingService != null) {
        val target = editingService ?: ShoeService(name = "", price = 50000.0, estimatedDays = 2, category = "Deep Clean")
        var nameInput by remember { mutableStateOf(target.name) }
        var priceInput by remember { mutableStateOf(target.price.toLong().toString()) }
        var daysInput by remember { mutableStateOf(target.estimatedDays.toString()) }
        var categoryInput by remember { mutableStateOf(target.category) }
        var descInput by remember { mutableStateOf(target.description) }

        AlertDialog(
            onDismissRequest = {
                isNewServiceDialog = false
                editingService = null
            },
            title = {
                Text(
                    text = if (editingService != null) "Edit Layanan Cuci" else "Tambah Menu Layanan Baru",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Layanan / Treatment") },
                        placeholder = { Text("Misal: Deep Clean Premium") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("service_name_input")
                    )
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Harga Layanan (Rp)") },
                        placeholder = { Text("50000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("service_price_input")
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = daysInput,
                            onValueChange = { daysInput = it },
                            label = { Text("Estimasi (Hari)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("service_days_input")
                        )
                        OutlinedTextField(
                            value = categoryInput,
                            onValueChange = { categoryInput = it },
                            label = { Text("Kategori") },
                            placeholder = { Text("Deep Clean / Repair") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f).testTag("service_category_input")
                        )
                    }
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Deskripsi Pengerjaan") },
                        placeholder = { Text("Keterangan bagian yang dibersihkan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            val price = priceInput.toDoubleOrNull() ?: target.price
                            val days = daysInput.toIntOrNull() ?: target.estimatedDays
                            val updated = target.copy(
                                name = nameInput.trim(),
                                price = price,
                                estimatedDays = days,
                                category = categoryInput.trim().ifBlank { "Treatment" },
                                description = descInput.trim()
                            )
                            viewModel.saveService(updated)
                            isNewServiceDialog = false
                            editingService = null
                        }
                    },
                    modifier = Modifier.testTag("save_service_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isNewServiceDialog = false
                        editingService = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ServiceItemCard(
    service: ShoeService,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_item_card_${service.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${ThermalReceiptHelper.formatRupiah(service.price)} • ${service.estimatedDays} Hari Kerja",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = service.isActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.testTag("toggle_active_${service.id}")
                )
            }

            if (service.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = service.category,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp).testTag("edit_service_${service.id}")) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Layanan",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).testTag("delete_service_${service.id}")) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Layanan",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
