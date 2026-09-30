package com.example.ui.screens.laporan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChartBarData
import com.example.data.model.CustomerCommissionItem
import com.example.data.model.LaporanKeuanganData
import com.example.data.model.PeriodType
import com.example.ui.MainViewModel
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ExportReportDialog
import com.example.ui.theme.*
import com.example.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val laporanData by viewModel.laporanKeuanganData.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriodType.collectAsStateWithLifecycle()
    val customRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    val soList by viewModel.soList.collectAsStateWithLifecycle()

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = "Laporan Laba & Keuangan",
                subtitle = laporanData.periodLabel.ifBlank { "Analisis Finansial Usaha" },
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("btn_export_laporan_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Ekspor Laporan PDF & Excel",
                            tint = Color.White
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
            // Period Filter Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih Periode Laporan:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeutralTextSecondary
                        )
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PeriodType.entries) { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = {
                                    if (period == PeriodType.CUSTOM) {
                                        showCustomDateDialog = true
                                    } else {
                                        viewModel.setPeriodType(period)
                                    }
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (period == PeriodType.CUSTOM) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(period.label)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DagangBluePrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_period_${period.name.lowercase()}")
                            )
                        }
                    }

                    // Display active date range details with quick change button
                    Surface(
                        color = NeutralSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = DagangBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = laporanData.periodLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            if (selectedPeriod == PeriodType.CUSTOM) {
                                TextButton(
                                    onClick = { showCustomDateDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Ubah Tanggal", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 1. P&L BREAKDOWN CARD (KARTU RINCIAN LABA KOTOR & LABA BERSIH)
            item {
                BreakdownCard(laporan = laporanData)
            }

            // 2. OMSET CHART (GRAFIK OMSET PENJUALAN)
            item {
                OmsetChartCard(
                    chartData = laporanData.chartData,
                    totalOmset = laporanData.omset,
                    periodLabel = laporanData.periodLabel
                )
            }

            // 3. DAFTAR KOMISI PER PELANGGAN (KOMISI LIST)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rincian Komisi Pelanggan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Dihitung dari total pembayaran SO yang diterima pada periode ini",
                            style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary, fontSize = 11.sp)
                        )
                    }
                }
            }

            if (laporanData.commissionList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeutralOutline, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "Belum ada komisi pelanggan yang terealisasi pada periode ini. Komisi otomatis dihitung saat ada pembayaran SO yang diterima.",
                            style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(laporanData.commissionList) { item ->
                    CustomerCommissionCard(item = item)
                }
            }

            // Export Action Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExportDialog = true }
                        .border(1.dp, DagangBluePrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .testTag("card_export_banner_laporan")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DagangBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = DagangBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unduh / Bagikan Laporan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Cetak dokumen resmi PDF (A4) atau ekspor data ke Excel (CSV).",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NeutralTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DagangBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Ekspor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
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

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            initialStart = customRange.first,
            initialEnd = customRange.second,
            onDismiss = { showCustomDateDialog = false },
            onApply = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showCustomDateDialog = false
            }
        )
    }
}

/**
 * Breakdown Card:
 * - Omset = total SO in period
 * - HPP = sum(qty terjual x hargaDasar)
 * - Laba Kotor = Omset - HPP
 * - Komisi = total komisi from customer payments
 * - Pengeluaran = pengeluaran operasional
 * - Laba Bersih = Laba Kotor - Pengeluaran - Komisi
 */
@Composable
fun BreakdownCard(
    laporan: LaporanKeuanganData,
    modifier: Modifier = Modifier
) {
    val isNetProfitPositive = laporan.labaBersih >= 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeutralOutline, RoundedCornerShape(16.dp))
            .testTag("laporan_breakdown_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Laporan Laba / Rugi (P&L)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    color = DagangBlueContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${laporan.soCount} Transaksi SO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DagangOnBlueContainer,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = NeutralOutline)

            // 1. Omset Penjualan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "1. Omset Penjualan (Total SO)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Total nilai pesanan penjualan periode ini",
                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary, fontSize = 11.sp)
                    )
                }
                Text(
                    text = Formatters.formatRupiah(laporan.omset),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = DagangBluePrimary
                    )
                )
            }

            // 2. HPP (Harga Pokok Penjualan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "2. HPP (Harga Pokok Penjualan)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Σ (Qty Terjual × HPP Rata-Rata)",
                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary, fontSize = 11.sp)
                    )
                }
                Text(
                    text = "-${Formatters.formatRupiah(laporan.hpp)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeutralTextPrimary
                    )
                )
            }

            // 3. Laba Kotor Result Bar
            Surface(
                color = NeutralSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "= Laba Kotor (Gross Profit)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Margin: ${Formatters.formatPersen(laporan.marginLabaKotorPersen)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = NeutralTextSecondary)
                        )
                    }
                    Text(
                        text = Formatters.formatRupiah(laporan.labaKotor),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (laporan.labaKotor >= 0) DagangIncomeGreen else DagangDebtRed
                        )
                    )
                }
            }

            // 4. Biaya Operasional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "3. Beban Operasional Usaha",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Listrik, sewa, gaji, transport, dll.",
                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary, fontSize = 11.sp)
                    )
                }
                Text(
                    text = "-${Formatters.formatRupiah(laporan.pengeluaranOperasional)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DagangDebtRed
                    )
                )
            }

            // 5. Total Komisi Pelanggan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "4. Total Komisi Agen/Pelanggan",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Berdasarkan SO yang sudah terbayar",
                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary, fontSize = 11.sp)
                    )
                }
                Text(
                    text = "-${Formatters.formatRupiah(laporan.totalKomisi)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE)
                    )
                )
            }

            HorizontalDivider(color = NeutralOutline)

            // 6. Laba Bersih (Net Profit) Prominent Highlight Box
            Surface(
                color = if (isNetProfitPositive) DagangIncomeGreenContainer else DagangDebtRedContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LABA BERSIH (NET PROFIT)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isNetProfitPositive) DagangOnIncomeGreen else DagangOnDebtRed,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "Margin Bersih: ${Formatters.formatPersen(laporan.marginLabaBersihPersen)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isNetProfitPositive) DagangOnIncomeGreen else DagangOnDebtRed,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = Formatters.formatRupiah(laporan.labaBersih),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isNetProfitPositive) DagangOnIncomeGreen else DagangOnDebtRed
                        )
                    )
                }
            }
        }
    }
}

/**
 * Native Jetpack Compose Canvas Bar Chart for Omset
 */
@Composable
fun OmsetChartCard(
    chartData: List<ChartBarData>,
    totalOmset: Double,
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeutralOutline, RoundedCornerShape(16.dp))
            .testTag("omset_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Grafik Tren Omset Penjualan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Total: ${Formatters.formatRupiah(totalOmset)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DagangBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = DagangBluePrimary
                )
            }

            if (chartData.isEmpty() || chartData.all { it.amount == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(NeutralSurfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi penjualan pada grafik periode ini.",
                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val maxAmount = remember(chartData) {
                    chartData.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
                }

                // Interactive selected tooltip preview
                selectedBarIndex?.let { idx ->
                    if (idx in chartData.indices) {
                        val selected = chartData[idx]
                        Surface(
                            color = DagangBlueContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selected.label.replace("\n", " "),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = Formatters.formatRupiah(selected.amount),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DagangBluePrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Canvas Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(chartData) {
                                detectTapGestures { offset ->
                                    val barSlotWidth = size.width / chartData.size
                                    val tappedIndex = (offset.x / barSlotWidth).toInt()
                                    if (tappedIndex in chartData.indices) {
                                        selectedBarIndex = tappedIndex
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height - 30.dp.toPx()
                        val barCount = chartData.size
                        val slotWidth = canvasWidth / barCount
                        val barWidth = (slotWidth * 0.55f).coerceAtLeast(8.dp.toPx()).coerceAtMost(36.dp.toPx())

                        // Draw horizontal background guide lines
                        val lines = 3
                        for (i in 1..lines) {
                            val y = (canvasHeight / lines) * i
                            drawLine(
                                color = NeutralOutline,
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw bars
                        chartData.forEachIndexed { index, barData ->
                            val fraction = (barData.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                            val barHeight = canvasHeight * fraction
                            val x = (index * slotWidth) + (slotWidth - barWidth) / 2f
                            val y = canvasHeight - barHeight

                            val isSelected = selectedBarIndex == index

                            val brush = if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(DagangIncomeGreen, Color(0xFF22C55E))
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(DagangBluePrimary, DagangBlueLight)
                                )
                            }

                            drawRoundRect(
                                brush = brush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }

                    // Bar Labels underneath
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(28.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        chartData.forEachIndexed { index, bar ->
                            Text(
                                text = bar.label.substringBefore("\n"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = if (selectedBarIndex == index) DagangBluePrimary else NeutralTextSecondary,
                                    fontWeight = if (selectedBarIndex == index) FontWeight.Bold else FontWeight.Normal
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedBarIndex = index }
                            )
                        }
                    }
                }

                Text(
                    text = "Ketuk salah satu batang grafik untuk melihat rincian omset per tanggal.",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary, fontSize = 11.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Customer Commission List Item:
 * Komisi per customer = persenKomisi x total SO yang sudah terbayar (from payments received in period)
 */
@Composable
fun CustomerCommissionCard(
    item: CustomerCommissionItem,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeutralOutline, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF7E22CE),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.customer.nama,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tarif Komisi: ${Formatters.formatPersen(item.persenKomisi)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF7E22CE),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Hak Komisi",
                        style = MaterialTheme.typography.labelSmall.copy(color = NeutralTextSecondary, fontSize = 10.sp)
                    )
                    Text(
                        text = Formatters.formatRupiah(item.totalKomisi),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E22CE)
                        )
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = NeutralOutline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Pelunasan/Cicilan Masuk:",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                )
                Text(
                    text = Formatters.formatRupiah(item.totalSoTerbayar),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = DagangIncomeGreen
                    )
                )
            }
        }
    }
}

/**
 * Custom Date Range Dialog
 */
@Composable
fun CustomDateRangeDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onApply: (start: Long, end: Long) -> Unit
) {
    var selectedPresetDays by remember { mutableIntStateOf(30) } // 7, 14, 30, 60, 90

    val now = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pilih Rentang Tanggal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih periode cepat atau sesuaikan:",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                )

                val presets = listOf(
                    Pair(7, "7 Hari Terakhir"),
                    Pair(14, "14 Hari Terakhir"),
                    Pair(30, "30 Hari Terakhir"),
                    Pair(60, "60 Hari Terakhir"),
                    Pair(90, "90 Hari (3 Bulan)")
                )

                presets.forEach { (days, label) ->
                    val isSelected = selectedPresetDays == days
                    Surface(
                        color = if (isSelected) DagangBlueContainer else NeutralSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPresetDays = days }
                            .border(
                                1.dp,
                                if (isSelected) DagangBluePrimary else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) DagangOnBlueContainer else NeutralTextPrimary
                                )
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = DagangBluePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = Formatters.getStartOfDay(now - (selectedPresetDays.toLong() * 24 * 3600 * 1000))
                    val end = Formatters.getEndOfDay(now)
                    onApply(start, end)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DagangBluePrimary)
            ) {
                Text("Terapkan Periode")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
