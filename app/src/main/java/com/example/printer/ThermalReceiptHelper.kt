package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.OrderItem
import com.example.data.model.StoreConfig
import com.example.data.model.TransactionOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ThermalReceiptHelper {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    /**
     * Generates a plain-text receipt formatted specifically for 58mm (32 chars) or 80mm (48 chars).
     */
    fun buildTextReceipt(
        order: TransactionOrder,
        items: List<OrderItem>,
        config: StoreConfig
    ): String {
        val width = if (config.paperSizeWidth == 80) 48 else 32
        val separator = "=".repeat(width)
        val dash = "-".repeat(width)

        val sb = StringBuilder()

        // Center aligned header
        sb.append(centerText(config.storeName, width)).append("\n")
        if (config.storeAddress.isNotBlank()) {
            sb.append(centerText(config.storeAddress, width)).append("\n")
        }
        if (config.storePhone.isNotBlank()) {
            sb.append(centerText("WA: ${config.storePhone}", width)).append("\n")
        }
        sb.append(separator).append("\n")

        // Order Info
        sb.append("No. Nota : ${order.invoiceNumber}\n")
        sb.append("Tgl Masuk: ${formatDate(order.createdAt)}\n")
        sb.append("Estimasi : ${formatDateOnly(order.estimatedCompletion)}\n")
        sb.append("Kasir    : ${order.cashierName}\n")
        sb.append("Pelanggan: ${order.customerName}\n")
        if (order.customerPhone.isNotBlank()) {
            sb.append("No. Telp : ${order.customerPhone}\n")
        }
        sb.append(dash).append("\n")

        // Items
        items.forEachIndexed { index, item ->
            val num = "${index + 1}. "
            val name = "${item.serviceName} (${item.quantity} psg)"
            sb.append(num).append(name).append("\n")
            if (item.shoeBrandModel.isNotBlank() || item.shoeColor.isNotBlank()) {
                val detail = "   [${item.shoeBrandModel} - ${item.shoeColor}]"
                sb.append(detail).append("\n")
            }
            if (item.shoeNotes.isNotBlank()) {
                sb.append("   Ket: ${item.shoeNotes}\n")
            }
            val priceStr = formatRupiah(item.totalPrice)
            sb.append(padBetween("   Biaya:", priceStr, width)).append("\n")
        }
        sb.append(dash).append("\n")

        // Totals
        sb.append(padBetween("Subtotal:", formatRupiah(order.subtotal), width)).append("\n")
        if (order.discountAmount > 0) {
            sb.append(padBetween("Diskon:", "-${formatRupiah(order.discountAmount)}", width)).append("\n")
        }
        if (order.pointsUsed > 0) {
            sb.append(padBetween("Poin Dipakai:", "-${order.pointsUsed} Pts", width)).append("\n")
        }
        sb.append(separator).append("\n")
        sb.append(padBetween("TOTAL AKHIR:", formatRupiah(order.finalTotal), width)).append("\n")
        sb.append(padBetween("Metode Bayar:", order.paymentMethod, width)).append("\n")
        sb.append(padBetween("Status Bayar:", order.paymentStatus, width)).append("\n")

        if (order.paymentStatus == "Lunas" && order.cashGiven > 0) {
            sb.append(padBetween("Tunai Diterima:", formatRupiah(order.cashGiven), width)).append("\n")
            sb.append(padBetween("Kembalian:", formatRupiah(order.changeGiven), width)).append("\n")
        }

        if (order.pointsEarned > 0) {
            sb.append(padBetween("Bonus Poin Baru:", "+${order.pointsEarned} Pts", width)).append("\n")
        }
        sb.append(separator).append("\n")

        // Footer & Notes
        if (config.receiptFooter.isNotBlank()) {
            config.receiptFooter.lines().forEach { line ->
                sb.append(centerText(line.trim(), width)).append("\n")
            }
        }
        sb.append("\n\n")
        return sb.toString()
    }

    /**
     * Generates ESC/POS byte payload for Bluetooth thermal printer.
     */
    fun buildEscPosBytes(
        order: TransactionOrder,
        items: List<OrderItem>,
        config: StoreConfig
    ): ByteArray {
        val width = if (config.paperSizeWidth == 80) 48 else 32
        val separator = "=".repeat(width)
        val dash = "-".repeat(width)

        val out = mutableListOf<Byte>()

        fun addBytes(bytes: ByteArray) {
            for (b in bytes) out.add(b)
        }

        fun addString(str: String) {
            addBytes(str.toByteArray(charset("GBK")))
        }

        // ESC @ -> Init printer
        addBytes(byteArrayOf(0x1B, 0x40))

        // Center Align (ESC a 1)
        addBytes(byteArrayOf(0x1B, 0x61, 0x01))
        // Double height & bold for Store Name
        addBytes(byteArrayOf(0x1B, 0x21, 0x10))
        addString("${config.storeName}\n")
        // Normal text
        addBytes(byteArrayOf(0x1B, 0x21, 0x00))

        if (config.storeAddress.isNotBlank()) {
            addString("${config.storeAddress}\n")
        }
        if (config.storePhone.isNotBlank()) {
            addString("WA: ${config.storePhone}\n")
        }
        addString("$separator\n")

        // Left Align (ESC a 0)
        addBytes(byteArrayOf(0x1B, 0x61, 0x00))
        addString("No. Nota : ${order.invoiceNumber}\n")
        addString("Tgl Masuk: ${formatDate(order.createdAt)}\n")
        addString("Estimasi : ${formatDateOnly(order.estimatedCompletion)}\n")
        addString("Kasir    : ${order.cashierName}\n")
        addString("Pelanggan: ${order.customerName}\n")
        if (order.customerPhone.isNotBlank()) {
            addString("No. Telp : ${order.customerPhone}\n")
        }
        addString("$dash\n")

        // Items
        items.forEachIndexed { index, item ->
            val num = "${index + 1}. "
            addString("$num${item.serviceName} (${item.quantity} psg)\n")
            if (item.shoeBrandModel.isNotBlank() || item.shoeColor.isNotBlank()) {
                addString("   [${item.shoeBrandModel} - ${item.shoeColor}]\n")
            }
            if (item.shoeNotes.isNotBlank()) {
                addString("   Ket: ${item.shoeNotes}\n")
            }
            addString(padBetween("   Biaya:", formatRupiah(item.totalPrice), width) + "\n")
        }
        addString("$dash\n")

        // Totals
        addString(padBetween("Subtotal:", formatRupiah(order.subtotal), width) + "\n")
        if (order.discountAmount > 0) {
            addString(padBetween("Diskon:", "-${formatRupiah(order.discountAmount)}", width) + "\n")
        }
        if (order.pointsUsed > 0) {
            addString(padBetween("Poin Dipakai:", "-${order.pointsUsed} Pts", width) + "\n")
        }
        addString("$separator\n")

        // Bold total
        addBytes(byteArrayOf(0x1B, 0x45, 0x01)) // Bold on
        addString(padBetween("TOTAL AKHIR:", formatRupiah(order.finalTotal), width) + "\n")
        addBytes(byteArrayOf(0x1B, 0x45, 0x00)) // Bold off

        addString(padBetween("Metode Bayar:", order.paymentMethod, width) + "\n")
        addString(padBetween("Status Bayar:", order.paymentStatus, width) + "\n")

        if (order.paymentStatus == "Lunas" && order.cashGiven > 0) {
            addString(padBetween("Tunai Diterima:", formatRupiah(order.cashGiven), width) + "\n")
            addString(padBetween("Kembalian:", formatRupiah(order.changeGiven), width) + "\n")
        }
        if (order.pointsEarned > 0) {
            addString(padBetween("Bonus Poin:", "+${order.pointsEarned} Pts", width) + "\n")
        }
        addString("$separator\n")

        // Footer Center
        addBytes(byteArrayOf(0x1B, 0x61, 0x01))
        if (config.receiptFooter.isNotBlank()) {
            config.receiptFooter.lines().forEach { line ->
                addString("${line.trim()}\n")
            }
        }

        // Feed 4 lines & partial paper cut (GS V 66 0)
        addString("\n\n\n\n")
        addBytes(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

        return out.toByteArray()
    }

    /**
     * Send ESC/POS bytes directly to paired Bluetooth thermal printer.
     */
    @SuppressLint("MissingPermission")
    suspend fun printToBluetoothDevice(
        deviceAddress: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia di perangkat ini."))

            if (!adapter.isEnabled) {
                return@withContext Result.failure(Exception("Bluetooth belum aktif. Silakan nyalakan Bluetooth."))
            }

            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                ?: return@withContext Result.failure(Exception("Printer Bluetooth tidak ditemukan: $deviceAddress"))

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()
            socket.connect()
            outputStream = socket.outputStream
            outputStream.write(bytes)
            outputStream.flush()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak via Bluetooth: ${e.localizedMessage ?: "Koneksi terputus"}"))
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Get paired Bluetooth devices list.
     */
    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<Pair<String, String>> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices.map { (it.name ?: "Unknown Device") to it.address }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Android standard PrintManager integration.
     * Generates a clean HTML receipt and invokes Android's native print framework (supports PDF, WiFi, USB, BT printers).
     */
    fun printViaAndroidPrintManager(
        context: Context,
        order: TransactionOrder,
        items: List<OrderItem>,
        config: StoreConfig
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Struk_${order.invoiceNumber}"

        val webView = WebView(context)
        val htmlContent = buildHtmlReceipt(order, items, config)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A7)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    /**
     * Share formatted receipt to WhatsApp directly.
     */
    fun sendReceiptViaWhatsApp(
        context: Context,
        order: TransactionOrder,
        items: List<OrderItem>,
        config: StoreConfig
    ) {
        val receiptText = buildTextReceipt(order, items, config)
        val encodedMessage = Uri.encode(
            "Halo Kak *${order.customerName}*,\nBerikut adalah Nota Transaksi Cuci Sepatu Anda di *${config.storeName}*:\n\n```\n$receiptText\n```\nStatus: *${order.orderStatus}*\nTerima kasih! 🙏"
        )
        val phone = sanitizePhone(order.customerPhone)
        val uri = if (phone.isNotBlank()) {
            Uri.parse("https://wa.me/$phone?text=$encodedMessage")
        } else {
            Uri.parse("https://wa.me/?text=$encodedMessage")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback generic share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Nota"))
        }
    }

    fun sendPickupNotificationViaWhatsApp(
        context: Context,
        order: TransactionOrder,
        config: StoreConfig
    ) {
        val message = "Halo Kak *${order.customerName}*! 👟✨\n\nKabar gembira! Sepatu Anda dengan No. Nota *${order.invoiceNumber}* di *${config.storeName}* sudah *SELESAI DICUCI DAN SIAP DIAMBIL*!\n\nAlamat: ${config.storeAddress}\nSilakan bawa nota fisik atau tunjukkan pesan ini saat pengambilan. Terima kasih! 🙏"
        val phone = sanitizePhone(order.customerPhone)
        val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Notifikasi Siap Ambil"))
        }
    }

    private fun sanitizePhone(phone: String): String {
        var clean = phone.replace(Regex("[^0-9]"), "")
        if (clean.startsWith("0")) {
            clean = "62" + clean.substring(1)
        } else if (clean.startsWith("8")) {
            clean = "62$clean"
        }
        return clean
    }

    private fun padBetween(left: String, right: String, totalWidth: Int): String {
        val spacesCount = (totalWidth - left.length - right.length).coerceAtLeast(1)
        return left + " ".repeat(spacesCount) + right
    }

    private fun centerText(text: String, totalWidth: Int): String {
        if (text.length >= totalWidth) return text
        val leftPad = (totalWidth - text.length) / 2
        val rightPad = totalWidth - text.length - leftPad
        return " ".repeat(leftPad) + text + " ".repeat(rightPad)
    }

    private fun buildHtmlReceipt(order: TransactionOrder, items: List<OrderItem>, config: StoreConfig): String {
        val itemsHtml = items.joinToString("") { item ->
            """
            <tr>
                <td style="padding: 4px 0;">
                    <b>${item.serviceName}</b> (${item.quantity} psg)<br>
                    <small style="color: #555;">${item.shoeBrandModel} ${item.shoeColor} ${if (item.shoeNotes.isNotBlank()) " - " + item.shoeNotes else ""}</small>
                </td>
                <td style="text-align: right; vertical-align: top; padding: 4px 0;"><b>${formatRupiah(item.totalPrice)}</b></td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 13px;
                    line-height: 1.3;
                    width: 280px;
                    margin: 0 auto;
                    padding: 8px;
                    color: #000;
                }
                .center { text-align: center; }
                .right { text-align: right; }
                .bold { font-weight: bold; }
                .divider { border-top: 1px dashed #000; margin: 6px 0; }
                .double-divider { border-top: 2px solid #000; margin: 6px 0; }
                table { width: 100%; border-collapse: collapse; }
                .title { font-size: 16px; font-weight: bold; margin-bottom: 2px; }
            </style>
        </head>
        <body>
            <div class="center">
                <div class="title">${config.storeName}</div>
                <div>${config.storeAddress}</div>
                <div>WA: ${config.storePhone}</div>
            </div>
            <div class="double-divider"></div>
            <div><b>No. Nota :</b> ${order.invoiceNumber}</div>
            <div><b>Tgl Masuk:</b> ${formatDate(order.createdAt)}</div>
            <div><b>Estimasi :</b> ${formatDateOnly(order.estimatedCompletion)}</div>
            <div><b>Kasir    :</b> ${order.cashierName}</div>
            <div><b>Pelanggan:</b> ${order.customerName} (${order.customerPhone})</div>
            <div class="divider"></div>
            <table>
                $itemsHtml
            </table>
            <div class="divider"></div>
            <table>
                <tr><td>Subtotal</td><td class="right">${formatRupiah(order.subtotal)}</td></tr>
                ${if (order.discountAmount > 0) "<tr><td>Diskon</td><td class=\"right\">-${formatRupiah(order.discountAmount)}</td></tr>" else ""}
                ${if (order.pointsUsed > 0) "<tr><td>Poin Dipakai</td><td class=\"right\">-${order.pointsUsed} Pts</td></tr>" else ""}
                <tr style="font-size: 15px; font-weight: bold;">
                    <td>TOTAL</td>
                    <td class="right">${formatRupiah(order.finalTotal)}</td>
                </tr>
                <tr><td>Metode Bayar</td><td class="right">${order.paymentMethod} (${order.paymentStatus})</td></tr>
                ${if (order.cashGiven > 0) "<tr><td>Tunai</td><td class=\"right\">${formatRupiah(order.cashGiven)}</td></tr><tr><td>Kembalian</td><td class=\"right\">${formatRupiah(order.changeGiven)}</td></tr>" else ""}
                ${if (order.pointsEarned > 0) "<tr><td>Bonus Poin</td><td class=\"right\">+${order.pointsEarned} Pts</td></tr>" else ""}
            </table>
            <div class="double-divider"></div>
            <div class="center" style="font-size: 11px;">
                ${config.receiptFooter.replace("\n", "<br>")}
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
