package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printer.ThermalReceiptHelper
import com.example.ui.components.OwnerPinDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.CashierPosScreen
import com.example.ui.screens.MemberScreen
import com.example.ui.screens.MenuManageScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UserGuideScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PosTab
import com.example.ui.viewmodel.ShoePosViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ShoePosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainPosApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPosApp(viewModel: ShoePosViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isOwnerMode by viewModel.isOwnerMode.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val receiptOrder by viewModel.receiptOrder.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showOwnerPinDialog by remember { mutableStateOf(false) }

    val activeQueueCount = remember(allOrders) {
        allOrders.count { it.orderStatus != "COMPLETED" && it.orderStatus != "CANCELLED" }
    }

    // Handle back button: return to KASIR tab if in any other tab
    BackHandler(enabled = currentTab != PosTab.KASIR) {
        viewModel.setTab(PosTab.KASIR)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val autoWhatsAppTrigger by viewModel.autoWhatsAppTrigger.collectAsState()

    // Automatic WhatsApp dispatch when order is placed
    LaunchedEffect(autoWhatsAppTrigger) {
        autoWhatsAppTrigger?.let { (order, items) ->
            ThermalReceiptHelper.sendReceiptViaWhatsApp(context, order, items, storeConfig)
            viewModel.clearAutoWhatsAppTrigger()
        }
    }

    // Toast/Snackbar notifications
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = storeConfig.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    // Owner Mode Indicator Badge
                    Surface(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable {
                                if (isOwnerMode) {
                                    viewModel.setOwnerMode(false)
                                    viewModel.showToast("Beralih ke Mode Kasir")
                                } else {
                                    showOwnerPinDialog = true
                                }
                            }
                            .testTag("owner_badge_topbar"),
                        color = if (isOwnerMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOwnerMode) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isOwnerMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOwnerMode) "Owner" else "Kasir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOwnerMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setTab(PosTab.PANDUAN) },
                        modifier = Modifier.testTag("help_topbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Petunjuk Penggunaan",
                            tint = if (currentTab == PosTab.PANDUAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // 1. Kasir POS
                NavigationBarItem(
                    selected = currentTab == PosTab.KASIR,
                    onClick = { viewModel.setTab(PosTab.KASIR) },
                    icon = {
                        if (cartItems.isNotEmpty()) {
                            BadgedBox(badge = { Badge { Text(cartItems.size.toString()) } }) {
                                Icon(Icons.Default.PointOfSale, contentDescription = "Kasir")
                            }
                        } else {
                            Icon(Icons.Default.PointOfSale, contentDescription = "Kasir")
                        }
                    },
                    label = { Text("Kasir", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_kasir")
                )

                // 2. Pesanan & Antrian
                NavigationBarItem(
                    selected = currentTab == PosTab.PESANAN,
                    onClick = { viewModel.setTab(PosTab.PESANAN) },
                    icon = {
                        if (activeQueueCount > 0) {
                            BadgedBox(badge = { Badge { Text(activeQueueCount.toString()) } }) {
                                Icon(Icons.Default.LocalLaundryService, contentDescription = "Antrian")
                            }
                        } else {
                            Icon(Icons.Default.LocalLaundryService, contentDescription = "Antrian")
                        }
                    },
                    label = { Text("Antrian", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_pesanan")
                )

                // 3. Laporan
                NavigationBarItem(
                    selected = currentTab == PosTab.LAPORAN,
                    onClick = { viewModel.setTab(PosTab.LAPORAN) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Laporan") },
                    label = { Text("Laporan", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_laporan")
                )

                // 4. Member
                NavigationBarItem(
                    selected = currentTab == PosTab.MEMBER,
                    onClick = { viewModel.setTab(PosTab.MEMBER) },
                    icon = { Icon(Icons.Default.CardMembership, contentDescription = "Member") },
                    label = { Text("Member", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_member")
                )

                // 5. Menu Layanan
                NavigationBarItem(
                    selected = currentTab == PosTab.MENU,
                    onClick = { viewModel.setTab(PosTab.MENU) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Menu") },
                    label = { Text("Menu", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_menu")
                )

                // 6. Pengaturan
                NavigationBarItem(
                    selected = currentTab == PosTab.PENGATURAN,
                    onClick = { viewModel.setTab(PosTab.PENGATURAN) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Setelan", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_pengaturan")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                PosTab.KASIR -> CashierPosScreen(viewModel = viewModel)
                PosTab.PESANAN -> OrdersScreen(viewModel = viewModel)
                PosTab.LAPORAN -> ReportScreen(viewModel = viewModel)
                PosTab.MEMBER -> MemberScreen(viewModel = viewModel)
                PosTab.MENU -> MenuManageScreen(viewModel = viewModel)
                PosTab.PENGATURAN -> SettingsScreen(viewModel = viewModel)
                PosTab.PANDUAN -> UserGuideScreen()
            }
        }
    }

    // Modal: PIN Dialog for topbar
    if (showOwnerPinDialog) {
        OwnerPinDialog(
            title = "Akses Mode Owner",
            subtitle = "Masukkan PIN Owner untuk membuka semua menu dan pengaturan:",
            onDismiss = { showOwnerPinDialog = false },
            onPinVerified = {
                showOwnerPinDialog = false
                viewModel.setOwnerMode(true)
                viewModel.showToast("Mode Owner berhasil diaktifkan")
            },
            verifyPin = { viewModel.verifyOwnerPin(it) }
        )
    }

    // Modal: Receipt Dialog
    if (receiptOrder != null) {
        val (order, items) = receiptOrder!!
        ReceiptDialog(
            order = order,
            items = items,
            config = storeConfig,
            onDismiss = { viewModel.closeReceipt() },
            onShowToast = { viewModel.showToast(it) }
        )
    }
}
