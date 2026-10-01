package com.example.ui.screens.transaksi

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.screens.customer.CustomerScreen
import com.example.ui.screens.distributor.DistributorScreen
import com.example.ui.screens.po.PoListScreen
import com.example.ui.screens.so.SoListScreen

private data class TransaksiTabItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

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
    var selectedSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

    val tabs = remember {
        listOf(
            TransaksiTabItem("SO", Icons.Default.PointOfSale, "tab_so"),
            TransaksiTabItem("PO", Icons.Default.AddShoppingCart, "tab_po"),
            TransaksiTabItem("Pelanggan", Icons.Default.People, "tab_pelanggan"),
            TransaksiTabItem("Distributor", Icons.Default.LocalShipping, "tab_distributor")
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ONE Header for Transaksi
        DagangKuTopAppBar(
            title = "Transaksi",
            windowInsets = WindowInsets(0)
        )

        // Directly below header: SingleChoiceSegmentedButtonRow with 4 equal-width segments
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                SegmentedButton(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(tab.testTag),
                    icon = {},
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedSubTab) {
                0 -> SoListScreen(
                    viewModel = viewModel,
                    onNavigateToCreateSo = onNavigateToCreateSo,
                    onNavigateToDetail = onNavigateToSoDetail,
                    showHeader = false
                )
                1 -> PoListScreen(
                    viewModel = viewModel,
                    onNavigateToCreatePo = onNavigateToCreatePo,
                    onNavigateToDetail = onNavigateToPoDetail,
                    showHeader = false
                )
                2 -> CustomerScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = onNavigateToCustomerDetail,
                    showHeader = false
                )
                3 -> DistributorScreen(
                    viewModel = viewModel,
                    showHeader = false
                )
            }
        }
    }
}
