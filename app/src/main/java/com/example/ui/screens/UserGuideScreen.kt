package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GuideItem(
    val title: String,
    val icon: ImageVector,
    val summary: String,
    val steps: List<String>
)

@Composable
fun UserGuideScreen(
    modifier: Modifier = Modifier
) {
    val guides = remember {
        listOf(
            GuideItem(
                title = "1. Cara Input Kasir Cepat (Transaksi Sepatu)",
                icon = Icons.Default.PointOfSale,
                summary = "Langkah input penerimaan sepatu di meja kasir dalam waktu kurang dari 1 menit.",
                steps = listOf(
                    "Pilih atau masukkan nama pelanggan dan nomor WhatsApp (atau klik '+ Pilih Member' jika pelanggan sudah terdaftar).",
                    "Pilih layanan cuci sepatu (Fast Clean, Deep Clean, Unyellowing, Repaint, dll).",
                    "Masukkan merk sepatu (misal: Nike Air Jordan 1), warna, dan catatan kondisi/noda khusus.",
                    "Pilih metode pembayaran (Tunai, QRIS, Transfer, atau Bayar Saat Ambil).",
                    "Tekan tombol 'PROSES & CETAK STRUK'. Nomor nota unik (contoh: GS-260927-...) akan langsung dibuat dan struk siap dicetak!"
                )
            ),
            GuideItem(
                title = "2. Menghubungkan Printer Thermal Bluetooth & Cetak Otomatis",
                icon = Icons.Default.Print,
                summary = "Mencetak struk kasir profesional langsung ke printer thermal 58mm atau 80mm.",
                steps = listOf(
                    "Nyalakan printer thermal Bluetooth Anda.",
                    "Buka menu Bluetooth di setelan Android HP Anda, cari perangkat printer dan lakukan 'Pairing'.",
                    "Buka tab 'Pengaturan' di aplikasi GlowShoes POS, lalu klik tombol 'Pilih' pada bagian Printer Bluetooth.",
                    "Pilih nama printer Anda dari daftar.",
                    "Aktifkan tombol 'Cetak Struk Otomatis' agar struk langsung keluar saat kasir menekan tombol selesai transaksi.",
                    "Anda juga bisa menekan tombol 'Test Cetak Kertas Printer' untuk memastikan hasil cetak rapi."
                )
            ),
            GuideItem(
                title = "3. Mengatur Status Cucian & Kirim Notifikasi WhatsApp",
                icon = Icons.Default.LocalLaundryService,
                summary = "Pantau antrian pengerjaan dari pencucian hingga siap diambil pelanggan.",
                steps = listOf(
                    "Buka tab 'Antrian & Cuci' untuk melihat seluruh sepatu yang sedang dikerjakan.",
                    "Gunakan tab filter status: Antrian -> Sedang Dicuci -> Pengeringan -> Siap Diambil -> Selesai.",
                    "Tekan tombol alur (misal: 'Mulai Cuci', 'Keringkan', 'Siap Ambil') untuk memperbarui status secara real-time.",
                    "Saat status menjadi 'Siap Diambil', tekan tombol hijau 'WA Siap' untuk mengirim notifikasi pesan WhatsApp otomatis ke pelanggan bahwa sepatunya sudah wangi dan bersih!"
                )
            ),
            GuideItem(
                title = "4. Mengubah Menu & Harga Layanan oleh Owner",
                icon = Icons.Default.Lock,
                summary = "Owner memiliki kendali penuh untuk menambah, mengedit, atau menghapus layanan.",
                steps = listOf(
                    "Buka tab 'Menu Layanan'.",
                    "Masukkan PIN Owner (Default: 1234).",
                    "Tekan tombol (+) di pojok kanan bawah untuk menambah jenis layanan cuci baru.",
                    "Isi nama layanan, harga (Rp), estimasi hari pengerjaan, dan kategori.",
                    "Untuk mengubah harga menu yang sudah ada, tekan ikon pensil di kartu menu terkait.",
                    "Menu yang diperbarui akan langsung muncul di halaman kasir tanpa perlu restart aplikasi."
                )
            ),
            GuideItem(
                title = "5. Ekspor Data ke Google Sheets / CSV",
                icon = Icons.Default.Share,
                summary = "Ekspor laporan transaksi terstruktur untuk diolah tim operasional & akuntansi.",
                steps = listOf(
                    "Buka tab 'Laporan Omset' dan masukkan PIN Owner.",
                    "Pilih rentang filter (Hari Ini, Minggu Ini, Bulan Ini, atau Semua).",
                    "Tekan tombol 'Ekspor ke Google Sheets'.",
                    "Pilih Google Drive, Google Sheets, Gmail, atau WhatsApp untuk membagikan file CSV.",
                    "File CSV sudah berformat standar UTF-8 dan langsung rapi saat dibuka di Google Sheets atau Excel."
                )
            ),
            GuideItem(
                title = "6. Sistem Member & Poin Loyalitas",
                icon = Icons.Default.Star,
                summary = "Tingkatkan retensi dan repeat order pelanggan dengan sistem reward.",
                steps = listOf(
                    "Daftarkan pelanggan baru di tab 'Pelanggan & Poin' dengan nama dan nomor WhatsApp.",
                    "Setiap transaksi cuci sepatu, member otomatis mendapatkan bonus poin 5% dari total belanja.",
                    "Poin yang terkumpul dapat ditukarkan langsung sebagai potongan diskon di nota berikutnya (1 Poin = Rp 1).",
                    "Tingkat Tier (Silver, Gold, Platinum) otomatis naik seiring bertambahnya frekuensi cuci sepatu."
                )
            ),
            GuideItem(
                title = "7. Mode Offline, Backup & Sinkronisasi Cloud",
                icon = Icons.Default.CloudDone,
                summary = "Aplikasi aman digunakan saat internet mati dan mendukung cloud sync.",
                steps = listOf(
                    "Aplikasi menggunakan Database Lokal Room (Offline First). Transaksi kasir tetap berjalan lancar 100% tanpa internet.",
                    "Untuk mencadangkan seluruh data transaksi, buka tab Pengaturan lalu pilih 'Download / Bagikan File Backup Database'.",
                    "Jika Anda memiliki webhook Google Sheets atau server Cloud, masukkan URL pada kolom 'URL Cloud Webhook' di Pengaturan dan klik 'Sinkronisasi ke Cloud'."
                )
            ),
            GuideItem(
                title = "8. Riset Data Bulanan & Keamanan PIN",
                icon = Icons.Default.Lock,
                summary = "Fitur riset data untuk evaluasi performa berkala dan menjaga kerahasiaan toko.",
                steps = listOf(
                    "PIN Owner melindungi menu sensitif: Laporan Penjualan, Ubah Menu Layanan, Ganti Profil Toko, dan Riset Data.",
                    "PIN bawaan adalah '1234'. Sangat disarankan untuk segera mengubahnya di tab Pengaturan -> 'Ubah PIN Owner'.",
                    "Fitur 'Riset Data Transaksi Bulanan' memungkinkan Owner membersihkan transaksi bulan tertentu setelah laporan dibukukan di Google Sheets.",
                    "Data master pelanggan dan menu layanan tidak akan terhapus saat melakukan riset data transaksi."
                )
            ),
            GuideItem(
                title = "9. Cara Memakai POS di iPhone (iOS & Safari)",
                icon = Icons.Default.CloudDone,
                summary = "Buka dan jalankan mesin kasir langsung dari browser Safari di iPhone Anda.",
                steps = listOf(
                    "Hubungkan iPhone Anda ke jaringan Wi-Fi atau Hotspot yang sama dengan HP Android kasir.",
                    "Buka tab 'Setelan' di aplikasi Android, lihat 'Alamat Web POS untuk iPhone' (misal: http://192.168.1.15:8080).",
                    "Buka aplikasi Safari di iPhone, ketikkan alamat tersebut di bilah alamat browser.",
                    "Halaman Web POS khusus iPhone akan terbuka: Anda dapat input order baru, cek antrian, dan memantau omset secara real-time.",
                    "Agar mudah diakses setiap hari: Di Safari iPhone, tekan ikon 'Share' (kotak panah ke atas) lalu pilih 'Add to Home Screen' (Tambah ke Layar Utama)."
                )
            ),
            GuideItem(
                title = "10. Pengaturan WhatsApp Otomatis (Nota & Notifikasi)",
                icon = Icons.Default.PointOfSale,
                summary = "Kirim nota dan pemberitahuan siap ambil ke WhatsApp pelanggan secara otomatis.",
                steps = listOf(
                    "Aktifkan tombol 'Kirim Nota Otomatis ke WhatsApp' di menu Setelan.",
                    "Saat kasir menekan tombol 'PROSES & CETAK STRUK', aplikasi secara otomatis membuka WhatsApp dan menyiapkan pesan nota lengkap berformat monospace rapi.",
                    "Kasir hanya tinggal menekan tombol Kirim di WhatsApp.",
                    "Saat sepatu selesai dicuci di tab Antrian, tekan tombol hijau 'WA Siap' untuk mengirim notifikasi siap ambil kepada pelanggan dalam 1-klik."
                )
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Buku Panduan & Petunjuk Pemakaian",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Panduan operasional lengkap untuk Kasir dan Owner GlowShoes POS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        items(guides.size) { index ->
            val guide = guides[index]
            ExpandableGuideCard(guide = guide, testTag = "guide_card_$index")
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ExpandableGuideCard(guide: GuideItem, testTag: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag(testTag),
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
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = guide.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = guide.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = guide.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(10.dp))
                    guide.steps.forEachIndexed { i, step ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${i + 1}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
