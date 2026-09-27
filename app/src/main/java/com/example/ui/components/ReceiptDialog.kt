package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OrderItem
import com.example.data.model.StoreConfig
import com.example.data.model.TransactionOrder
import com.example.printer.ThermalReceiptHelper
import kotlinx.coroutines.launch

@Composable
fun ReceiptDialog(
    order: TransactionOrder,
    items: List<OrderItem>,
    config: StoreConfig,
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPrintingBt by remember { mutableStateOf(false) }

    val receiptText = remember(order, items, config) {
        ThermalReceiptHelper.buildTextReceipt(order, items, config)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.95f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Struk Nota Pembayaran",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No: ${order.invoiceNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_receipt_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Paper Receipt Visual Mockup
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = receiptText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Text(
                    text = "Pilih Aksi Cetak / Kirim:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Direct Bluetooth Thermal Print
                Button(
                    onClick = {
                        if (config.bluetoothPrinterAddress.isBlank()) {
                            onShowToast("Alamat printer Bluetooth belum diatur. Atur di menu Pengaturan!")
                        } else {
                            coroutineScope.launch {
                                isPrintingBt = true
                                val bytes = ThermalReceiptHelper.buildEscPosBytes(order, items, config)
                                val result = ThermalReceiptHelper.printToBluetoothDevice(config.bluetoothPrinterAddress, bytes)
                                isPrintingBt = false
                                result.onSuccess {
                                    onShowToast("Struk berhasil dikirim ke printer thermal!")
                                }.onFailure {
                                    onShowToast(it.message ?: "Gagal cetak Bluetooth")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("print_bluetooth_button"),
                    enabled = !isPrintingBt
                ) {
                    if (isPrintingBt) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.width(20.dp).height(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mengirim ke Printer...")
                    } else {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cetak Printer Thermal (Bluetooth)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Standard Android Print / Save to PDF
                FilledTonalButton(
                    onClick = {
                        ThermalReceiptHelper.printViaAndroidPrintManager(context, order, items, config)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("print_android_button")
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak via Android / Simpan PDF")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // WhatsApp Send
                OutlinedButton(
                    onClick = {
                        ThermalReceiptHelper.sendReceiptViaWhatsApp(context, order, items, config)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kirim Nota ke WhatsApp Pelanggan")
                }
            }
        }
    }
}
