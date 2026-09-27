package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_config")
data class StoreConfig(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "GlowShoes Shoe Care & Treatment",
    val storeAddress: String = "Jl. Merdeka No. 45, Kota",
    val storePhone: String = "0812-3456-7890",
    val ownerPin: String = "1234",
    val receiptFooter: String = "Terima kasih atas kepercayaan Anda!\nGaransi cuci 2x24 jam setelah sepatu diambil.\nHarap bawa nota ini saat pengambilan.",
    val bluetoothPrinterName: String = "",
    val bluetoothPrinterAddress: String = "",
    val paperSizeWidth: Int = 58, // 58 or 80 mm
    val autoPrintOnSave: Boolean = true,
    val autoSendWhatsAppOnSave: Boolean = true, // WhatsApp otomatis saat simpan nota
    val enableIosWebServer: Boolean = true, // Web POS server untuk iPhone / iOS
    val webServerPort: Int = 8080,
    val cloudSyncWebhookUrl: String = "",
    val cloudLastSyncTimestamp: Long = 0L,
    val pointPerTransactionPercent: Double = 0.05 // 5% of spend converted to points
)
