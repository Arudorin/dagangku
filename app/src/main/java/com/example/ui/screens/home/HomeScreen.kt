package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PoWithDetails
import com.example.data.model.ProdukWithStock
import com.example.data.model.SoWithDetails
import com.example.ui.MainViewModel
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToCreateSo: () -> Unit,
    onNavigateToCreatePo: () -> Unit,
    onNavigateToProduk: () -> Unit,
    onNavigateToCustomer: () -> Unit,
    onNavigateToDistributor: () -> Unit,
    onNavigateToSoDetail: (Long) -> Unit,
    onNavigateToPoDetail: (Long) -> Unit,
    onNavigateToKeuangan: () -> Unit,
    onNavigateToLaporan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val soList by viewModel.soList.collectAsStateWithLifecycle()
    val poList by viewModel.poList.collectAsStateWithLifecycle()
    val produkList by viewModel.produkList.collectAsStateWithLifecycle()
    val laporanData by viewModel.laporanKeuanganData.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriodType.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var selectedTransactionTab by remember { mutableIntStateOf(0) } // 0: Semua, 1: SO, 2: PO

    val lowStockProducts = remember(produkList) { produkList.filter { it.isLowStock } }

    val combinedTransactions = remember(soList, poList) {
        val soItems = soList.map { Pair("SO", it) }
        val poItems = poList.map { Pair("PO", it) }
        (soItems + poItems)
            .sortedByDescending {
                if (it.first == "SO") (it.second as SoWithDetails).so.tanggal
                else (it.second as PoWithDetails).po.tanggal
            }
            .take(5)
    }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = "DagangKu",
                subtitle = "Dasbor Usaha & Finansial",
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("btn_export_beranda")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Ekspor Laporan PDF & Excel",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Welcome & Greeting Card with Quick Export Callout
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Halo, Juragan! \uD83D\uDC4B",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Kelola omset, pantau laba bersih, piutang & stok toko secara presisi.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                        lineHeight = 16.sp
                                    )
                                )
                            }

                            FilledTonalButton(
                                onClick = { showExportDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("btn_header_export")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ekspor", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // 2. CORE FINANCIAL METRICS: OMSET, LABA, PIUTANG, HUTANG
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ringkasan Finansial",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = laporanData.periodLabel.ifBlank { "Bulan Ini" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { onNavigateToLaporan() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Quick Period Filter Chips for Beranda (matching Laporan exactly)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(com.example.data.model.PeriodType.entries) { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                if (period == com.example.data.model.PeriodType.CUSTOM) {
                                    onNavigateToLaporan()
                                } else {
                                    viewModel.setPeriodType(period)
                                }
                            },
                            label = { Text(period.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("home_filter_period_${period.name.lowercase()}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: OMSET & LABA (Gross & Net Profit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Omset Penjualan",
                            value = Formatters.formatRupiah(summary.totalPenjualan),
                            subtitle = "${laporanData.soCount} Pesanan SO",
                            icon = Icons.Default.TrendingUp,
                            iconBgColor = StatusGreenText,
                            accentColor = StatusGreenText,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_stat_omset"),
                            onClick = onNavigateToLaporan
                        )

                        // Laba Card with Net & Gross breakdown
                        val isNetProfitPositive = summary.totalLabaBersih >= 0
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToLaporan() }
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .testTag("card_stat_laba")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Laba Bersih",
                                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isNetProfitPositive) StatusGreenContainer
                                                else StatusRedContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            tint = if (isNetProfitPositive) StatusGreenText else StatusRedText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = Formatters.formatRupiah(summary.totalLabaBersih),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNetProfitPositive) StatusGreenText else StatusRedText
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "Kotor: ${Formatters.formatRupiah(summary.totalLabaKotor)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Row 2: PIUTANG & HUTANG
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Piutang Pelanggan",
                            value = Formatters.formatRupiah(summary.totalPiutang),
                            subtitle = "Tagihan SO tertunda",
                            icon = Icons.Default.ReceiptLong,
                            iconBgColor = StatusAmberText,
                            accentColor = if (summary.totalPiutang > 0) StatusAmberText else StatusGreenText,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_stat_piutang"),
                            onClick = onNavigateToKeuangan
                        )

                        StatCard(
                            title = "Hutang Distributor",
                            value = Formatters.formatRupiah(summary.totalHutang),
                            subtitle = "Kewajiban PO tertunda",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconBgColor = StatusRedText,
                            accentColor = if (summary.totalHutang > 0) StatusRedText else StatusGreenText,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_stat_hutang"),
                            onClick = onNavigateToKeuangan
                        )
                    }
                }
            }

            // 3. AKSI CEPAT (QUICK ACTIONS)
            item {
                Text(
                    text = "Aksi Cepat",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Buat SO",
                        subtitle = "Penjualan",
                        icon = Icons.Default.PointOfSale,
                        color = StatusGreenText,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCreateSo,
                        testTag = "quick_action_create_so"
                    )
                    QuickActionButton(
                        title = "Buat PO",
                        subtitle = "Pembelian",
                        icon = Icons.Default.AddShoppingCart,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCreatePo,
                        testTag = "quick_action_create_po"
                    )
                    QuickActionButton(
                        title = "Buku Kas",
                        subtitle = "Arus Uang",
                        icon = Icons.Default.AccountBalance,
                        color = StatusAmberText,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToKeuangan,
                        testTag = "quick_action_keuangan"
                    )
                    QuickActionButton(
                        title = "Laporan",
                        subtitle = "P&L & Ekspor",
                        icon = Icons.Default.Assessment,
                        color = StatusPurpleText,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLaporan,
                        testTag = "quick_action_laporan"
                    )
                }
            }

            // 4. STOK MENIPIS (LOW STOCK ALERT & RESTOCK SECTION)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Status Stok Menipis",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (lowStockProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = StatusRedContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${lowStockProducts.size} Menipis",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StatusRedText,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    TextButton(onClick = onNavigateToProduk) {
                        Text("Lihat Semua Stok", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (lowStockProducts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusGreenContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToProduk() }
                            .testTag("card_stok_aman")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreenText.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StatusGreenText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Semua Stok Barang Aman \u2705",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreenText
                                    )
                                )
                                Text(
                                    text = "Semua ${produkList.size} produk saat ini berada di atas batas minimum stok.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StatusGreenText,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // List of low stock products with quick restock PO trigger
                items(lowStockProducts.take(4), key = { it.produk.id }) { item ->
                    LowStockProductCard(
                        productWithStock = item,
                        onRestockPo = onNavigateToCreatePo,
                        onProductDetail = onNavigateToProduk
                    )
                }
            }

            // 5. BANNER EKSPOR LAPORAN (PDF & EXCEL/CSV)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExportDialog = true }
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .testTag("banner_export_reports")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ekspor Laporan PDF & Excel",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Unduh laporan laba/rugi, komisi agen, dan rekap SO/PO untuk arsip atau pembukuan.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 6. TRANSAKSI TERAKHIR (RECENT TRANSACTIONS)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transaksi Terakhir",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Tab selector for recent transactions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTransactionTab == 0,
                            onClick = { selectedTransactionTab = 0 },
                            label = { Text("Semua") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = selectedTransactionTab == 1,
                            onClick = { selectedTransactionTab = 1 },
                            label = { Text("Penjualan (SO)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusGreenContainer,
                                selectedLabelColor = StatusGreenText
                            )
                        )
                        FilterChip(
                            selected = selectedTransactionTab == 2,
                            onClick = { selectedTransactionTab = 2 },
                            label = { Text("Pembelian (PO)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            when (selectedTransactionTab) {
                0 -> {
                    // Combined SO and PO
                    if (combinedTransactions.isEmpty()) {
                        item {
                            Text(
                                text = "Belum ada riwayat transaksi penjualan ataupun pembelian.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(combinedTransactions) { entry ->
                            if (entry.first == "SO") {
                                val soDetail = entry.second as SoWithDetails
                                RecentSoCard(
                                    soDetail = soDetail,
                                    onClick = { onNavigateToSoDetail(soDetail.so.id) }
                                )
                            } else {
                                val poDetail = entry.second as PoWithDetails
                                RecentPoCard(
                                    poDetail = poDetail,
                                    onClick = { onNavigateToPoDetail(poDetail.po.id) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Only SO
                    val recentSos = soList.take(5)
                    if (recentSos.isEmpty()) {
                        item {
                            Text(
                                text = "Belum ada transaksi penjualan (SO).",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(recentSos, key = { it.so.id }) { soDetail ->
                            RecentSoCard(
                                soDetail = soDetail,
                                onClick = { onNavigateToSoDetail(soDetail.so.id) }
                            )
                        }
                    }
                }

                2 -> {
                    // Only PO
                    val recentPos = poList.take(5)
                    if (recentPos.isEmpty()) {
                        item {
                            Text(
                                text = "Belum ada transaksi pembelian (PO).",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(recentPos, key = { it.po.id }) { poDetail ->
                            RecentPoCard(
                                poDetail = poDetail,
                                onClick = { onNavigateToPoDetail(poDetail.po.id) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        ExportReportDialog(
            laporan = laporanData,
            soList = soList,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun LowStockProductCard(
    productWithStock: ProdukWithStock,
    onRestockPo: () -> Unit,
    onProductDetail: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StatusRedContainer, RoundedCornerShape(14.dp))
            .clickable { onProductDetail() }
            .testTag("low_stock_item_${productWithStock.produk.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatusRedContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = StatusRedText,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productWithStock.produk.nama,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tersedia: ${productWithStock.stok} ${productWithStock.produk.satuan}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StatusRedText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = " • Min: ${productWithStock.produk.stokMinimum}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Text(
                    text = "Harga Beli: ${Formatters.formatRupiah(productWithStock.produk.hargaDasar)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )
            }

            OutlinedButton(
                onClick = onRestockPo,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_restock_po_${productWithStock.produk.id}")
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PO Beli", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun RecentSoCard(
    soDetail: SoWithDetails,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .testTag("recent_so_${soDetail.so.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatusGreenContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = StatusGreenText,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StatusGreenContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusGreenText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = soDetail.so.nomor,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${soDetail.customer?.nama ?: "Umum"} • ${Formatters.formatTanggal(soDetail.so.tanggal)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.formatRupiah(soDetail.so.total),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = StatusGreenText
                    )
                )
            }
            StatusBadge(status = soDetail.status)
        }
    }
}

@Composable
fun RecentPoCard(
    poDetail: PoWithDetails,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .testTag("recent_po_${poDetail.po.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "PO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = poDetail.po.nomor,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${poDetail.distributor?.nama ?: "Distributor"} • ${Formatters.formatTanggal(poDetail.po.tanggal)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.formatRupiah(poDetail.po.total),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            StatusBadge(status = poDetail.status)
        }
    }
}
