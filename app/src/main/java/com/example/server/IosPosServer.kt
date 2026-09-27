package com.example.server

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.StoreConfig
import com.example.data.repository.ShoePosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IosPosServer(
    private val context: Context,
    private val repository: ShoePosRepository,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    var isRunning = false
        private set

    fun startServer(port: Int = 8080) {
        if (isRunning) return
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                while (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
                    val clientSocket = serverSocket!!.accept()
                    scope.launch(Dispatchers.IO) {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                isRunning = false
            }
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1]

            // Read headers
            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read body if POST
            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                reader.read(chars, 0, contentLength)
                String(chars)
            } else ""

            when {
                path == "/" || path.startsWith("/index") -> {
                    serveIosHtml(out)
                }
                path == "/api/menu" -> {
                    serveMenuJson(out)
                }
                path == "/api/orders" -> {
                    serveOrdersJson(out)
                }
                path == "/api/create_order" && method == "POST" -> {
                    handleCreateOrder(body, out)
                }
                path == "/api/update_status" && method == "POST" -> {
                    handleUpdateStatus(body, out)
                }
                else -> {
                    sendResponse(out, 404, "text/plain", "Not Found")
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun serveMenuJson(out: OutputStream) {
        val allMenu = repository.getAllOrdersSync() // helper or query menu
        val dbConfig = repository.storeConfig
        val activeServices = repository.activeServices
        // Collect current snapshot
        val jsonArray = JSONArray()
        // Query items sync
        val items = repository.getAllItemsSync()
        sendResponse(out, 200, "application/json", jsonArray.toString())
    }

    private suspend fun serveOrdersJson(out: OutputStream) {
        val orders = repository.getAllOrdersSync()
        val jsonArray = JSONArray()
        orders.forEach { o ->
            val obj = JSONObject().apply {
                put("id", o.id)
                put("invoiceNumber", o.invoiceNumber)
                put("customerName", o.customerName)
                put("customerPhone", o.customerPhone)
                put("orderStatus", o.orderStatus)
                put("finalTotal", o.finalTotal)
                put("paymentStatus", o.paymentStatus)
                put("createdAt", o.createdAt)
            }
            jsonArray.put(obj)
        }
        sendResponse(out, 200, "application/json", jsonArray.toString())
    }

    private suspend fun handleCreateOrder(body: String, out: OutputStream) {
        try {
            val json = JSONObject(body)
            val customerName = json.optString("customerName", "Pelanggan iPhone")
            val customerPhone = json.optString("customerPhone", "")
            val serviceName = json.optString("serviceName", "Deep Clean")
            val shoeModel = json.optString("shoeModel", "Sneakers")
            val price = json.optDouble("price", 50000.0)
            val notes = json.optString("notes", "Input via iPhone")

            val invoice = repository.generateInvoiceNumber()
            val order = Order(
                invoiceNumber = invoice,
                customerName = customerName,
                customerPhone = customerPhone,
                subtotal = price,
                finalTotal = price,
                paymentMethod = "Tunai",
                paymentStatus = "Lunas",
                cashierName = "Kasir (iPhone)",
                notes = notes
            )
            val item = OrderItem(
                serviceName = serviceName,
                shoeBrandModel = shoeModel,
                price = price,
                quantity = 1,
                shoeNotes = notes
            )
            repository.createOrder(order, listOf(item))

            val res = JSONObject().apply {
                put("success", true)
                put("invoiceNumber", invoice)
            }
            sendResponse(out, 200, "application/json", res.toString())
        } catch (e: Exception) {
            sendResponse(out, 400, "application/json", """{"success":false,"error":"${e.message}"}""")
        }
    }

    private suspend fun handleUpdateStatus(body: String, out: OutputStream) {
        try {
            val json = JSONObject(body)
            val orderId = json.getLong("orderId")
            val newStatus = json.getString("status")
            repository.updateOrderStatus(orderId, OrderStatus.valueOf(newStatus))
            sendResponse(out, 200, "application/json", """{"success":true}""")
        } catch (e: Exception) {
            sendResponse(out, 400, "application/json", """{"success":false,"error":"${e.message}"}""")
        }
    }

    private suspend fun serveIosHtml(out: OutputStream) {
        val orders = repository.getAllOrdersSync()
        val totalOmset = orders.sumOf { it.finalTotal }
        val activeCount = orders.count { it.orderStatus != "COMPLETED" && it.orderStatus != "CANCELLED" }

        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatRp.maximumFractionDigits = 0

        val rowsHtml = orders.take(25).joinToString("") { o ->
            val statusColor = when (o.orderStatus) {
                "READY" -> "#10B981"
                "WASHING" -> "#0284C7"
                "DRYING" -> "#8B5CF6"
                "COMPLETED" -> "#6B7280"
                else -> "#F59E0B"
            }
            var cleanPhone = o.customerPhone.replace(Regex("[^0-9]"), "")
            if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)

            val waBtn = if (cleanPhone.isNotBlank()) {
                val msg = "Halo Kak ${o.customerName}, berikut info nota cuci sepatu Anda di GlowShoes: No #${o.invoiceNumber}, Status: ${o.orderStatus}. Terima kasih! 🙏"
                """<a href="https://wa.me/$cleanPhone?text=${java.net.URLEncoder.encode(msg, "UTF-8")}" class="wa-btn">💬 WA</a>"""
            } else ""

            """
            <div class="order-card">
                <div class="order-header">
                    <div>
                        <div class="order-id">#${o.invoiceNumber}</div>
                        <div class="order-cust"><b>${o.customerName}</b> (${o.customerPhone.ifBlank { "Umum" }})</div>
                    </div>
                    <span class="badge" style="background:${statusColor}20; color:${statusColor};">${o.orderStatus}</span>
                </div>
                <div class="order-footer">
                    <span class="order-price">${formatRp.format(o.finalTotal)}</span>
                    <div>
                        $waBtn
                    </div>
                </div>
            </div>
            """.trimIndent()
        }

        val html = """
        <!DOCTYPE html>
        <html lang="id">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
            <meta name="apple-mobile-web-app-capable" content="yes">
            <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
            <title>GlowShoes POS - iPhone Edition</title>
            <style>
                :root {
                    --primary: #0D9488;
                    --primary-dark: #0F172A;
                    --bg: #F8FAFC;
                    --card: #FFFFFF;
                    --text: #1E293B;
                    --text-sub: #64748B;
                }
                * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
                body {
                    margin: 0;
                    padding: env(safe-area-inset-top, 16px) 16px env(safe-area-inset-bottom, 24px) 16px;
                    font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
                    background-color: var(--bg);
                    color: var(--text);
                }
                .nav {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    padding: 12px 0;
                }
                .title { font-size: 22px; font-weight: 800; color: var(--primary-dark); }
                .subtitle { font-size: 13px; color: var(--text-sub); }
                .stat-grid {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 12px;
                    margin: 16px 0;
                }
                .stat-box {
                    background: var(--card);
                    padding: 16px;
                    border-radius: 16px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
                    border: 1px solid #E2E8F0;
                }
                .stat-label { font-size: 12px; color: var(--text-sub); font-weight: 600; text-transform: uppercase; }
                .stat-val { font-size: 20px; font-weight: 700; color: var(--primary); margin-top: 4px; }
                .btn {
                    display: block;
                    width: 100%;
                    background: var(--primary);
                    color: white;
                    text-align: center;
                    padding: 14px;
                    border-radius: 14px;
                    font-size: 16px;
                    font-weight: 700;
                    border: none;
                    cursor: pointer;
                    margin-bottom: 20px;
                    box-shadow: 0 4px 12px rgba(13, 148, 136, 0.3);
                }
                .section-title { font-size: 18px; font-weight: 700; margin: 20px 0 12px 0; }
                .order-card {
                    background: var(--card);
                    border-radius: 16px;
                    padding: 14px;
                    margin-bottom: 12px;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.03);
                    border: 1px solid #E2E8F0;
                }
                .order-header { display: flex; justify-content: space-between; align-items: flex-start; }
                .order-id { font-size: 12px; color: var(--text-sub); font-weight: 600; }
                .order-cust { font-size: 15px; margin-top: 2px; }
                .badge { padding: 4px 8px; border-radius: 8px; font-size: 11px; font-weight: 700; }
                .order-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding-top: 10px; border-top: 1px dashed #E2E8F0; }
                .order-price { font-size: 15px; font-weight: 700; color: var(--primary-dark); }
                .wa-btn {
                    background: #25D366;
                    color: white;
                    text-decoration: none;
                    padding: 6px 12px;
                    border-radius: 8px;
                    font-size: 12px;
                    font-weight: 700;
                    display: inline-block;
                }
                /* Modal */
                #modal {
                    display: none;
                    position: fixed;
                    top: 0; left: 0; right: 0; bottom: 0;
                    background: rgba(0,0,0,0.5);
                    padding: 20px;
                    align-items: center;
                    justify-content: center;
                    z-index: 99;
                }
                .modal-content {
                    background: white;
                    border-radius: 20px;
                    padding: 20px;
                    width: 100%;
                    max-width: 400px;
                }
                .input-group { margin-bottom: 12px; }
                .input-group label { display: block; font-size: 12px; font-weight: 600; color: var(--text-sub); margin-bottom: 4px; }
                .input-group input, .input-group select {
                    width: 100%;
                    padding: 10px 12px;
                    border: 1px solid #CBD5E1;
                    border-radius: 10px;
                    font-size: 15px;
                }
            </style>
        </head>
        <body>
            <div class="nav">
                <div>
                    <div class="title">GlowShoes POS 👟</div>
                    <div class="subtitle">Akses Kasir via iPhone / Safari</div>
                </div>
                <div style="font-size: 12px; background: #E0F2FE; color: #0369A1; padding: 4px 8px; border-radius: 8px; font-weight: 700;">iOS Online</div>
            </div>

            <div class="stat-grid">
                <div class="stat-box">
                    <div class="stat-label">Total Omset</div>
                    <div class="stat-val">${formatRp.format(totalOmset)}</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">Antrian Cuci</div>
                    <div class="stat-val">$activeCount Sepatu</div>
                </div>
            </div>

            <button class="btn" onclick="openOrderModal()">+ Input Pesanan dari iPhone</button>

            <div class="section-title">Daftar Antrian & Pesanan</div>
            <div id="orders-list">
                $rowsHtml
            </div>

            <!-- Modal Input Order -->
            <div id="modal">
                <div class="modal-content">
                    <h3 style="margin-top:0;">Input Pesanan Baru (iPhone)</h3>
                    <div class="input-group">
                        <label>Nama Pelanggan</label>
                        <input type="text" id="custName" placeholder="Contoh: Andi Wijaya">
                    </div>
                    <div class="input-group">
                        <label>Nomor WhatsApp</label>
                        <input type="tel" id="custPhone" placeholder="081234567890">
                    </div>
                    <div class="input-group">
                        <label>Layanan Cuci</label>
                        <select id="serviceSelect">
                            <option value="Fast Clean|30000">Fast Clean (Rp 30.000)</option>
                            <option value="Deep Clean|50000" selected>Deep Clean (Rp 50.000)</option>
                            <option value="Unyellowing|65000">Unyellowing Sol (Rp 65.000)</option>
                            <option value="Repaint Sneaker|120000">Repaint Sneaker (Rp 120.000)</option>
                            <option value="Reglue Lem Sol|45000">Reglue Lem Sol (Rp 45.000)</option>
                            <option value="Leather Care|75000">Leather Care (Rp 75.000)</option>
                        </select>
                    </div>
                    <div class="input-group">
                        <label>Merk & Model Sepatu</label>
                        <input type="text" id="shoeModel" placeholder="Contoh: Nike Jordan / Converse 70s">
                    </div>
                    <div class="input-group">
                        <label>Catatan Khusus</label>
                        <input type="text" id="shoeNotes" placeholder="Noda oli di midsole, sol lepas">
                    </div>
                    <div style="display:flex; gap:10px; margin-top:16px;">
                        <button style="flex:1; padding:12px; border:1px solid #CBD5E1; background:#F1F5F9; border-radius:10px; font-weight:600;" onclick="closeModal()">Batal</button>
                        <button style="flex:2; padding:12px; background:var(--primary); color:white; border:none; border-radius:10px; font-weight:700;" onclick="submitOrder()">Simpan Nota</button>
                    </div>
                </div>
            </div>

            <script>
                function openOrderModal() { document.getElementById('modal').style.display = 'flex'; }
                function closeModal() { document.getElementById('modal').style.display = 'none'; }
                async function submitOrder() {
                    const custName = document.getElementById('custName').value.trim() || 'Pelanggan iPhone';
                    const custPhone = document.getElementById('custPhone').value.trim();
                    const sVal = document.getElementById('serviceSelect').value.split('|');
                    const serviceName = sVal[0];
                    const price = parseFloat(sVal[1]);
                    const shoeModel = document.getElementById('shoeModel').value.trim() || 'Sneakers';
                    const shoeNotes = document.getElementById('shoeNotes').value.trim();

                    const res = await fetch('/api/create_order', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ customerName: custName, customerPhone: custPhone, serviceName, price, shoeModel, notes: shoeNotes })
                    });
                    const data = await res.json();
                    if (data.success) {
                        alert('Pesanan #' + data.invoiceNumber + ' berhasil disimpan ke POS!');
                        location.reload();
                    } else {
                        alert('Gagal simpan: ' + (data.error || 'Terjadi kesalahan'));
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()

        sendResponse(out, 200, "text/html; charset=UTF-8", html)
    }

    private fun sendResponse(out: OutputStream, statusCode: Int, contentType: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val statusText = when (statusCode) {
            200 -> "OK"
            404 -> "Not Found"
            else -> "Bad Request"
        }
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    companion object {
        fun getLocalIpAddress(context: Context): String {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    return Formatter.formatIpAddress(ipInt)
                }
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
                for (intf in interfaces) {
                    val addrs = intf.inetAddresses
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            } catch (_: Exception) {}
            return "127.0.0.1"
        }
    }
}
