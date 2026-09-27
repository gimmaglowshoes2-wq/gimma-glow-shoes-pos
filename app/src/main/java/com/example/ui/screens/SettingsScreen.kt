package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreConfig
import com.example.printer.ThermalReceiptHelper
import com.example.ui.components.OwnerPinDialog
import com.example.ui.viewmodel.ShoePosViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun SettingsScreen(
    viewModel: ShoePosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isOwnerMode by viewModel.isOwnerMode.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val cloudSyncing by viewModel.cloudSyncing.collectAsState()

    var showPinDialogForAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Dialog states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showBluetoothPicker by remember { mutableStateOf(false) }
    var showResetMonthlyDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Owner Mode Banner
            Card(
                modifier = Modifier.fillMaxWidth().testTag("owner_mode_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOwnerMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOwnerMode) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isOwnerMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isOwnerMode) "Mode Owner Aktif" else "Mode Kasir Aktif",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isOwnerMode) "Akses penuh pengaturan, laporan, dan menu." else "Fitur sensitif terkunci untuk keamanan.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isOwnerMode) {
                                viewModel.setOwnerMode(false)
                                viewModel.showToast("Mode Owner dinonaktifkan")
                            } else {
                                showPinDialogForAction = { viewModel.setOwnerMode(true) }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOwnerMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("toggle_owner_mode_button")
                    ) {
                        Text(if (isOwnerMode) "Kunci" else "Buka PIN")
                    }
                }
            }
        }

        // Section: Profil Toko & Footer Nota
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Informasi Toko Cuci Sepatu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = {
                                if (isOwnerMode) showEditProfileDialog = true
                                else showPinDialogForAction = { showEditProfileDialog = true }
                            },
                            modifier = Modifier.testTag("edit_store_profile_button")
                        ) {
                            Text("Ubah")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = storeConfig.storeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(text = storeConfig.storeAddress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "WhatsApp: ${storeConfig.storePhone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Catatan Kaki Struk (Garansi / Syarat):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = storeConfig.receiptFooter, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Section: Printer Thermal Bluetooth
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Printer Thermal & Cetak Otomatis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Printer Bluetooth Terpilih", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (storeConfig.bluetoothPrinterName.isNotBlank())
                                    "${storeConfig.bluetoothPrinterName} (${storeConfig.bluetoothPrinterAddress})"
                                else "Belum dipilih (Gunakan tombol di samping)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { showBluetoothPicker = true },
                            modifier = Modifier.testTag("choose_bluetooth_button")
                        ) {
                            Text("Pilih")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto Print on Checkout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cetak Struk Otomatis Saat Transaksi", fontWeight = FontWeight.SemiBold)
                            Text("Struk langsung tercetak begitu tombol proses ditekan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = storeConfig.autoPrintOnSave,
                            onCheckedChange = { checked ->
                                viewModel.updateStoreConfig(storeConfig.copy(autoPrintOnSave = checked))
                            },
                            modifier = Modifier.testTag("toggle_autoprint")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Paper Size (58mm vs 80mm)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ukuran Kertas Thermal:", fontWeight = FontWeight.SemiBold)
                        Row {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = storeConfig.paperSizeWidth == 58,
                                    onClick = { viewModel.updateStoreConfig(storeConfig.copy(paperSizeWidth = 58)) }
                                )
                                Text("58 mm")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = storeConfig.paperSizeWidth == 80,
                                    onClick = { viewModel.updateStoreConfig(storeConfig.copy(paperSizeWidth = 80)) }
                                )
                                Text("80 mm")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Test Print Button
                    OutlinedButton(
                        onClick = {
                            if (storeConfig.bluetoothPrinterAddress.isBlank()) {
                                viewModel.showToast("Pilih printer Bluetooth terlebih dahulu")
                            } else {
                                coroutineScope.launch {
                                    val testBytes = "\u001B\u0040\u001B\u0061\u0001*** TEST PRINTER GLOWSHOES ***\n\nPrinter Thermal Siap Digunakan!\nTanggal: ${ThermalReceiptHelper.formatDate(System.currentTimeMillis())}\n\n\n\n\u001D\u0056\u0042\u0000".toByteArray(charset("GBK"))
                                    val res = ThermalReceiptHelper.printToBluetoothDevice(storeConfig.bluetoothPrinterAddress, testBytes)
                                    res.onSuccess { viewModel.showToast("Test print berhasil terkirim!") }
                                        .onFailure { viewModel.showToast(it.message ?: "Test print gagal") }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("test_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Cetak Kertas Printer")
                    }
                }
            }
        }

        // Section: Akses POS untuk iPhone (iOS) & Tablet
        item {
            val iosServerUrl by viewModel.iosServerUrl.collectAsState()
            val isServerRunning by viewModel.isIosServerRunning.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth().testTag("ios_pos_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF0284C7)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Bisa Dipakai di iPhone (iOS) & Safari",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gunakan iPhone sebagai mesin kasir tambahan nirkabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Alamat Web POS untuk iPhone:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = iosServerUrl,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Status Server: ${if (isServerRunning) "Aktif 🟢" else "Mati 🔴"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isServerRunning) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Cara Pakai di iPhone:\n" +
                                "1. Sambungkan iPhone ke Wi-Fi atau Hotspot yang sama dengan HP ini.\n" +
                                "2. Buka browser Safari di iPhone, ketikkan alamat link di atas.\n" +
                                "3. Anda bisa input order, cek omset, dan kirim WhatsApp langsung dari iPhone!\n" +
                                "4. Tips: Tap tombol Share di Safari ➔ 'Add to Home Screen' untuk menjadikannya seperti aplikasi asli.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(iosServerUrl)).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f).testTag("open_ios_browser_button")
                        ) {
                            Text("Buka Browser", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.restartIosServer() },
                            modifier = Modifier.weight(1f).testTag("restart_ios_server_button")
                        ) {
                            Text("Restart Server", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: WhatsApp Otomatis
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("whatsapp_auto_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WhatsApp Otomatis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kirim Nota Otomatis ke WhatsApp", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Saat kasir menekan tombol selesai proses, WhatsApp langsung terbuka dengan nota lengkap siap dikirim",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = storeConfig.autoSendWhatsAppOnSave,
                            onCheckedChange = { checked ->
                                viewModel.updateStoreConfig(storeConfig.copy(autoSendWhatsAppOnSave = checked))
                                viewModel.showToast("WhatsApp Otomatis: ${if (checked) "Aktif" else "Nonaktif"}")
                            },
                            modifier = Modifier.testTag("toggle_auto_whatsapp")
                        )
                    }
                }
            }
        }

        // Section: Sinkronisasi Cloud & Backup Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud Sync & Backup Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aplikasi berjalan 100% Offline (Local First). Anda dapat mencadangkan seluruh data transaksi atau menyinkronkan ke Webhook / Google Sheets secara berkala.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Backup JSON File
                    Button(
                        onClick = { viewModel.exportFullBackup(context) },
                        modifier = Modifier.fillMaxWidth().testTag("backup_json_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download / Bagikan File Backup Database")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cloud Webhook URL Setting
                    var webhookInput by remember(storeConfig) { mutableStateOf(storeConfig.cloudSyncWebhookUrl) }
                    OutlinedTextField(
                        value = webhookInput,
                        onValueChange = {
                            webhookInput = it
                            viewModel.updateStoreConfig(storeConfig.copy(cloudSyncWebhookUrl = it))
                        },
                        label = { Text("URL Cloud Webhook / Google Apps Script") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { viewModel.triggerCloudSync() },
                        enabled = !cloudSyncing,
                        modifier = Modifier.fillMaxWidth().testTag("sync_cloud_now_button")
                    ) {
                        if (cloudSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menghubungkan ke Cloud...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sinkronisasi ke Cloud Sekarang")
                        }
                    }

                    if (storeConfig.cloudLastSyncTimestamp > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Terakhir sinkron: ${ThermalReceiptHelper.formatDate(storeConfig.cloudLastSyncTimestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Section: Keamanan Akun & Riset Data (Owner Controlled)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keamanan Akun & Riset Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Change PIN button
                    OutlinedButton(
                        onClick = {
                            if (isOwnerMode) showChangePinDialog = true
                            else showPinDialogForAction = { showChangePinDialog = true }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("change_pin_button")
                    ) {
                        Text("Ubah PIN Owner (Saat ini: ${if (isOwnerMode) storeConfig.ownerPin else "****"})")
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Reset Data Bulanan
                    OutlinedButton(
                        onClick = {
                            if (isOwnerMode) showResetMonthlyDialog = true
                            else showPinDialogForAction = { showResetMonthlyDialog = true }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        modifier = Modifier.fillMaxWidth().testTag("reset_monthly_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Riset Data Transaksi Bulanan")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reset All Data
                    Button(
                        onClick = {
                            if (isOwnerMode) showResetAllDialog = true
                            else showPinDialogForAction = { showResetAllDialog = true }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("reset_all_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Riset Seluruh Data Transaksi")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Modal: Pin Verification Trigger
    if (showPinDialogForAction != null) {
        OwnerPinDialog(
            onDismiss = { showPinDialogForAction = null },
            onPinVerified = {
                val action = showPinDialogForAction
                showPinDialogForAction = null
                viewModel.setOwnerMode(true)
                action?.invoke()
            },
            verifyPin = { viewModel.verifyOwnerPin(it) }
        )
    }

    // Modal: Edit Store Profile
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(storeConfig.storeName) }
        var addressInput by remember { mutableStateOf(storeConfig.storeAddress) }
        var phoneInput by remember { mutableStateOf(storeConfig.storePhone) }
        var footerInput by remember { mutableStateOf(storeConfig.receiptFooter) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Ubah Profil Toko", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Toko") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Alamat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("No. WhatsApp Toko") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = footerInput,
                        onValueChange = { footerInput = it },
                        label = { Text("Catatan Kaki Struk / Garansi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = storeConfig.copy(
                            storeName = nameInput.trim(),
                            storeAddress = addressInput.trim(),
                            storePhone = phoneInput.trim(),
                            receiptFooter = footerInput.trim()
                        )
                        viewModel.updateStoreConfig(updated)
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditProfileDialog = false }) { Text("Batal") }
            }
        )
    }

    // Modal: Change PIN Owner
    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Ubah PIN Keamanan Owner", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 8) newPin = it },
                        label = { Text("PIN Baru (4-8 Digit Angka)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 8) confirmPin = it },
                        label = { Text("Ulangi PIN Baru") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length < 4) {
                            errorMsg = "PIN minimal 4 digit angka!"
                        } else if (newPin != confirmPin) {
                            errorMsg = "Konfirmasi PIN tidak cocok!"
                        } else {
                            viewModel.updateStoreConfig(storeConfig.copy(ownerPin = newPin))
                            viewModel.showToast("PIN Owner berhasil diperbarui!")
                            showChangePinDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("Ganti PIN")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangePinDialog = false }) { Text("Batal") }
            }
        )
    }

    // Modal: Bluetooth Paired Devices Picker
    if (showBluetoothPicker) {
        val pairedDevices = remember { ThermalReceiptHelper.getPairedBluetoothDevices() }

        AlertDialog(
            onDismissRequest = { showBluetoothPicker = false },
            title = { Text("Pilih Printer Thermal Bluetooth", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Pastikan printer thermal Anda sudah dipasangkan (paired) di pengaturan Bluetooth HP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (pairedDevices.isEmpty()) {
                        Text(
                            text = "Tidak ada perangkat Bluetooth tersimpan.\nNyalakan Bluetooth dan pasangkan printer Anda di setelan HP terlebih dahulu.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pairedDevices.size) { idx ->
                                val (name, address) = pairedDevices[idx]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateStoreConfig(
                                                storeConfig.copy(
                                                    bluetoothPrinterName = name,
                                                    bluetoothPrinterAddress = address
                                                )
                                            )
                                            viewModel.showToast("Printer $name dipilih!")
                                            showBluetoothPicker = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (storeConfig.bluetoothPrinterAddress == address)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(name, fontWeight = FontWeight.Bold)
                                            Text(address, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBluetoothPicker = false }) { Text("Tutup") }
            }
        )
    }

    // Modal: Reset Data Bulanan
    if (showResetMonthlyDialog) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        var selectedMonthIndex by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
        val monthNames = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

        AlertDialog(
            onDismissRequest = { showResetMonthlyDialog = false },
            title = { Text("Riset Data Transaksi Bulanan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Fitur ini akan menghapus riwayat pesanan cuci pada bulan yang dipilih untuk evaluasi berkala. Data pelanggan dan menu layanan tetap tersimpan aman.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pilih Bulan ($currentYear):", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(monthNames.size) { idx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMonthIndex = idx }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMonthIndex == idx,
                                    onClick = { selectedMonthIndex = idx }
                                )
                                Text(monthNames[idx])
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetMonthlyData(currentYear, selectedMonthIndex)
                        showResetMonthlyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_month_button")
                ) {
                    Text("Hapus Transaksi Bulan ${monthNames[selectedMonthIndex]}")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetMonthlyDialog = false }) { Text("Batal") }
            }
        )
    }

    // Modal: Reset All Data
    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = { Text("PERINGATAN: Riset Seluruh Data!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus SELURUH riwayat transaksi penjualan?\nTindakan ini permanen. Pastikan Anda telah mengunduh Backup Database terlebih dahulu!",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_all_button")
                ) {
                    Text("Ya, Bersihkan Semua")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetAllDialog = false }) { Text("Batal") }
            }
        )
    }
}
