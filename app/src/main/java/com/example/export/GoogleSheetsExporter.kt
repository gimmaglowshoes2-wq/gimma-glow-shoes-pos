package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.CustomerMember
import com.example.data.model.OrderItem
import com.example.data.model.ShoeService
import com.example.data.model.StoreConfig
import com.example.data.model.TransactionOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object GoogleSheetsExporter {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Generates CSV format compatible directly with Google Sheets and Microsoft Excel.
     * Starts with UTF-8 BOM so Indonesian characters open cleanly.
     */
    fun generateGoogleSheetsCsv(
        orders: List<TransactionOrder>,
        itemsMap: Map<Long, List<OrderItem>>
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM
        sb.append('\uFEFF')

        // CSV Header
        sb.append("No Nota,Tanggal,Jam,Pelanggan,No WhatsApp,Detail Sepatu,Layanan,Jumlah Psg,Subtotal,Diskon,Total Akhir,Metode Bayar,Status Bayar,Status Cuci,Estimasi Selesai,Kasir\n")

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        orders.forEach { order ->
            val items = itemsMap[order.id] ?: emptyList()
            val shoeDetails = items.joinToString(" | ") { item ->
                "${item.shoeBrandModel} (${item.shoeColor})"
            }.ifEmpty { "-" }
            val services = items.joinToString(" | ") { it.serviceName }.ifEmpty { "-" }
            val totalPairs = items.sumOf { it.quantity }.coerceAtLeast(1)

            val dateStr = dateFmt.format(Date(order.createdAt))
            val timeStr = timeFmt.format(Date(order.createdAt))
            val estStr = dateFmt.format(Date(order.estimatedCompletion))

            sb.append(escapeCsv(order.invoiceNumber)).append(",")
            sb.append(dateStr).append(",")
            sb.append(timeStr).append(",")
            sb.append(escapeCsv(order.customerName)).append(",")
            sb.append(escapeCsv(order.customerPhone)).append(",")
            sb.append(escapeCsv(shoeDetails)).append(",")
            sb.append(escapeCsv(services)).append(",")
            sb.append(totalPairs).append(",")
            sb.append(order.subtotal.toLong()).append(",")
            sb.append(order.discountAmount.toLong()).append(",")
            sb.append(order.finalTotal.toLong()).append(",")
            sb.append(escapeCsv(order.paymentMethod)).append(",")
            sb.append(escapeCsv(order.paymentStatus)).append(",")
            sb.append(escapeCsv(order.orderStatus)).append(",")
            sb.append(estStr).append(",")
            sb.append(escapeCsv(order.cashierName)).append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\"", "\"\"")
        if (str.contains(",") || str.contains("\n") || str.contains("\"")) {
            str = "\"$str\""
        }
        return str
    }

    /**
     * Share CSV file to Google Drive, Google Sheets, WhatsApp, Gmail, etc.
     */
    fun shareCsvToGoogleSheets(
        context: Context,
        csvData: String,
        filenamePrefix: String = "Laporan_Penjualan_GlowShoes"
    ) {
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val filename = "${filenamePrefix}_$dateStr.csv"
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { it.write(csvData.toByteArray(Charsets.UTF_8)) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan GlowShoes Shoe Care")
                putExtra(Intent.EXTRA_TEXT, "File laporan penjualan cuci sepatu (format Google Sheets / CSV).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Buka di Google Sheets / Bagikan Laporan"))
        } catch (e: Exception) {
            // Fallback plain share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, csvData)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Data CSV"))
        }
    }

    /**
     * Generate full JSON backup of the entire database.
     */
    fun exportBackupJson(
        orders: List<TransactionOrder>,
        items: List<OrderItem>,
        members: List<CustomerMember>,
        services: List<ShoeService>,
        config: StoreConfig?
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("backupDate", System.currentTimeMillis())
        root.put("backupDateStr", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Orders
        val ordersArray = JSONArray()
        orders.forEach { o ->
            val obj = JSONObject().apply {
                put("id", o.id)
                put("invoiceNumber", o.invoiceNumber)
                put("customerId", o.customerId ?: 0)
                put("customerName", o.customerName)
                put("customerPhone", o.customerPhone)
                put("createdAt", o.createdAt)
                put("estimatedCompletion", o.estimatedCompletion)
                put("orderStatus", o.orderStatus)
                put("paymentMethod", o.paymentMethod)
                put("paymentStatus", o.paymentStatus)
                put("subtotal", o.subtotal)
                put("discountAmount", o.discountAmount)
                put("finalTotal", o.finalTotal)
                put("pointsUsed", o.pointsUsed)
                put("pointsEarned", o.pointsEarned)
                put("cashierName", o.cashierName)
                put("notes", o.notes)
            }
            ordersArray.put(obj)
        }
        root.put("orders", ordersArray)

        // Items
        val itemsArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("orderId", item.orderId)
                put("serviceId", item.serviceId ?: 0)
                put("serviceName", item.serviceName)
                put("shoeBrandModel", item.shoeBrandModel)
                put("shoeColor", item.shoeColor)
                put("shoeNotes", item.shoeNotes)
                put("price", item.price)
                put("quantity", item.quantity)
            }
            itemsArray.put(obj)
        }
        root.put("orderItems", itemsArray)

        // Members
        val membersArray = JSONArray()
        members.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("phone", m.phone)
                put("totalPoints", m.totalPoints)
                put("totalSpent", m.totalSpent)
                put("totalWashCount", m.totalWashCount)
                put("tier", m.tier)
                put("joinDate", m.joinDate)
                put("notes", m.notes)
            }
            membersArray.put(obj)
        }
        root.put("members", membersArray)

        // Services
        val servicesArray = JSONArray()
        services.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("price", s.price)
                put("estimatedDays", s.estimatedDays)
                put("category", s.category)
                put("description", s.description)
                put("isActive", s.isActive)
            }
            servicesArray.put(obj)
        }
        root.put("services", servicesArray)

        return root.toString(2)
    }

    /**
     * Share full backup JSON file.
     */
    fun shareBackupFile(context: Context, jsonString: String) {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val filename = "GlowShoes_Backup_$dateStr.json"
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup Database GlowShoes POS")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Simpan / Kirim File Backup"))
    }

    /**
     * Sync data to cloud Webhook / Google Sheets AppScript URL.
     */
    suspend fun syncToCloud(webhookUrl: String, payloadJson: String): Result<String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) {
            return@withContext Result.failure(Exception("URL Cloud Sync / Webhook Google Sheets belum diatur di Pengaturan."))
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payloadJson.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Sinkronisasi Cloud Berhasil! (${response.code})")
            } else {
                Result.failure(Exception("Cloud merespons kode: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Koneksi gagal: ${e.localizedMessage ?: "Cek koneksi internet"}"))
        }
    }
}
