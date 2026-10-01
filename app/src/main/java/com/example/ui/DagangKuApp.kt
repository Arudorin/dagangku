package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.customer.CustomerDetailScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.keuangan.KeuanganScreen
import com.example.ui.screens.laporan.LaporanScreen
import com.example.ui.screens.pengaturan.PengaturanScreen
import com.example.ui.screens.po.CreatePoScreen
import com.example.ui.screens.po.PoDetailScreen
import com.example.ui.screens.so.CreateSoScreen
import com.example.ui.screens.so.SoDetailScreen
import com.example.ui.screens.stok.ProdukScreen
import com.example.ui.screens.transaksi.TransaksiScreen
import com.example.ui.theme.DagangBluePrimary
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

enum class BottomNavTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    BERANDA("Beranda", Icons.Default.Home, "tab_beranda"),
    TRANSAKSI("Transaksi", Icons.Default.ReceiptLong, "tab_transaksi"),
    STOK("Stok", Icons.Default.Inventory2, "tab_stok"),
    KEUANGAN("Keuangan", Icons.Default.AccountBalanceWallet, "tab_keuangan"),
    LAPORAN("Laporan", Icons.Default.BarChart, "tab_laporan")
}

sealed interface Screen {
    data object Home : Screen
    data class Transaksi(val subTab: Int = 0) : Screen
    data object Stok : Screen
    data object Keuangan : Screen
    data object Laporan : Screen

    // Sub-screens
    data object CreatePo : Screen
    data class PoDetail(val poId: Long) : Screen
    data object CreateSo : Screen
    data class SoDetail(val soId: Long) : Screen
    data class CustomerDetail(val customerId: Long) : Screen
    data object Pengaturan : Screen
}

@Composable
fun DagangKuApp(
    viewModel: MainViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(BottomNavTab.BERANDA) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var transaksiSubTab by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Determine whether to show the bottom bar (hide inside create / detail sub-screens)
    val isSubScreen = currentScreen is Screen.CreatePo ||
            currentScreen is Screen.PoDetail ||
            currentScreen is Screen.CreateSo ||
            currentScreen is Screen.SoDetail ||
            currentScreen is Screen.CustomerDetail ||
            currentScreen is Screen.Pengaturan

    // Back handling
    BackHandler(enabled = isSubScreen || currentTab != BottomNavTab.BERANDA) {
        if (isSubScreen) {
            // Return to active tab
            currentScreen = when (currentTab) {
                BottomNavTab.BERANDA -> Screen.Home
                BottomNavTab.TRANSAKSI -> Screen.Transaksi(transaksiSubTab)
                BottomNavTab.STOK -> Screen.Stok
                BottomNavTab.KEUANGAN -> Screen.Keuangan
                BottomNavTab.LAPORAN -> Screen.Laporan
            }
        } else {
            // Return to Home tab
            currentTab = BottomNavTab.BERANDA
            currentScreen = Screen.Home
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    BottomNavTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                currentTab = tab
                                currentScreen = when (tab) {
                                    BottomNavTab.BERANDA -> Screen.Home
                                    BottomNavTab.TRANSAKSI -> Screen.Transaksi(transaksiSubTab)
                                    BottomNavTab.STOK -> Screen.Stok
                                    BottomNavTab.KEUANGAN -> Screen.Keuangan
                                    BottomNavTab.LAPORAN -> Screen.Laporan
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreateSo = { currentScreen = Screen.CreateSo },
                    onNavigateToCreatePo = { currentScreen = Screen.CreatePo },
                    onNavigateToProduk = {
                        currentTab = BottomNavTab.STOK
                        currentScreen = Screen.Stok
                    },
                    onNavigateToCustomer = {
                        currentTab = BottomNavTab.TRANSAKSI
                        transaksiSubTab = 2
                        currentScreen = Screen.Transaksi(2)
                    },
                    onNavigateToDistributor = {
                        currentTab = BottomNavTab.TRANSAKSI
                        transaksiSubTab = 3
                        currentScreen = Screen.Transaksi(3)
                    },
                    onNavigateToSoDetail = { soId -> currentScreen = Screen.SoDetail(soId) },
                    onNavigateToPoDetail = { poId -> currentScreen = Screen.PoDetail(poId) },
                    onNavigateToKeuangan = {
                        currentTab = BottomNavTab.KEUANGAN
                        currentScreen = Screen.Keuangan
                    },
                    onNavigateToLaporan = {
                        currentTab = BottomNavTab.LAPORAN
                        currentScreen = Screen.Laporan
                    },
                    onNavigateToPengaturan = {
                        currentScreen = Screen.Pengaturan
                    }
                )

                is Screen.Transaksi -> TransaksiScreen(
                    viewModel = viewModel,
                    initialSubTab = screen.subTab,
                    onNavigateToCreateSo = { currentScreen = Screen.CreateSo },
                    onNavigateToCreatePo = { currentScreen = Screen.CreatePo },
                    onNavigateToSoDetail = { soId -> currentScreen = Screen.SoDetail(soId) },
                    onNavigateToPoDetail = { poId -> currentScreen = Screen.PoDetail(poId) },
                    onNavigateToCustomerDetail = { custId -> currentScreen = Screen.CustomerDetail(custId) }
                )

                is Screen.Stok -> ProdukScreen(
                    viewModel = viewModel
                )

                is Screen.Keuangan -> KeuanganScreen(
                    viewModel = viewModel,
                    onNavigateToCustomer = {
                        currentTab = BottomNavTab.TRANSAKSI
                        transaksiSubTab = 2
                        currentScreen = Screen.Transaksi(2)
                    },
                    onNavigateToDistributor = {
                        currentTab = BottomNavTab.TRANSAKSI
                        transaksiSubTab = 3
                        currentScreen = Screen.Transaksi(3)
                    }
                )

                is Screen.Laporan -> LaporanScreen(
                    viewModel = viewModel
                )

                // Sub-screens
                is Screen.CreatePo -> CreatePoScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentScreen = Screen.Transaksi(1)
                    },
                    onSuccessCreate = { poId ->
                        currentScreen = Screen.PoDetail(poId)
                    }
                )

                is Screen.PoDetail -> PoDetailScreen(
                    poId = screen.poId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentScreen = Screen.Transaksi(1)
                    }
                )

                is Screen.CreateSo -> CreateSoScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentScreen = Screen.Transaksi(0)
                    },
                    onSuccessCreate = { soId ->
                        currentScreen = Screen.SoDetail(soId)
                    }
                )

                is Screen.SoDetail -> SoDetailScreen(
                    soId = screen.soId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentScreen = Screen.Transaksi(0)
                    }
                )

                is Screen.CustomerDetail -> CustomerDetailScreen(
                    customerId = screen.customerId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentScreen = Screen.Transaksi(2)
                    },
                    onNavigateToSoDetail = { soId ->
                        currentScreen = Screen.SoDetail(soId)
                    }
                )

                is Screen.Pengaturan -> PengaturanScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}
