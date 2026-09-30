package com.example.ui.screens.transaksi

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.screens.customer.CustomerScreen
import com.example.ui.screens.distributor.DistributorScreen
import com.example.ui.screens.po.PoListScreen
import com.example.ui.screens.so.SoListScreen
import com.example.ui.theme.DagangBluePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaksiScreen(
    viewModel: MainViewModel,
    initialSubTab: Int = 0, // 0 = SO, 1 = PO, 2 = Customer, 3 = Distributor
    onNavigateToCreateSo: () -> Unit,
    onNavigateToCreatePo: () -> Unit,
    onNavigateToSoDetail: (Long) -> Unit,
    onNavigateToPoDetail: (Long) -> Unit,
    onNavigateToCustomerDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(initialSubTab) }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = DagangBluePrimary,
            edgePadding = 0.dp
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Penjualan (SO)") }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Pembelian (PO)") }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Pelanggan") }
            )
            Tab(
                selected = selectedSubTab == 3,
                onClick = { selectedSubTab = 3 },
                text = { Text("Distributor") }
            )
        }

        when (selectedSubTab) {
            0 -> SoListScreen(
                viewModel = viewModel,
                onNavigateToCreateSo = onNavigateToCreateSo,
                onNavigateToDetail = onNavigateToSoDetail
            )
            1 -> PoListScreen(
                viewModel = viewModel,
                onNavigateToCreatePo = onNavigateToCreatePo,
                onNavigateToDetail = onNavigateToPoDetail
            )
            2 -> CustomerScreen(
                viewModel = viewModel,
                onNavigateToDetail = onNavigateToCustomerDetail
            )
            3 -> DistributorScreen(
                viewModel = viewModel
            )
        }
    }
}
