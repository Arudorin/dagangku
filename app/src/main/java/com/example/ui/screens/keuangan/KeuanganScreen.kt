package com.example.ui.screens.keuangan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeuanganScreen(
    viewModel: MainViewModel,
    onNavigateToCustomer: () -> Unit = {},
    onNavigateToDistributor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val kasSummary by viewModel.kasSummary.collectAsStateWithLifecycle()
    val bukuKas by viewModel.bukuKas.collectAsStateWithLifecycle()
    val pengeluaranList by viewModel.pengeluaranList.collectAsStateWithLifecycle()
    val soList by viewModel.soList.collectAsStateWithLifecycle()
    val poList by viewModel.poList.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Buku Kas, 1 = Piutang, 2 = Hutang, 3 = Pengeluaran
    var kasFilter by remember { mutableStateOf("Semua") } // "Semua", "Masuk", "Keluar"

    var showAddPengeluaranDialog by remember { mutableStateOf(false) }
    var pengeluaranToDelete by remember { mutableStateOf<Pengeluaran?>(null) }

    // Bottom sheet state for recording payment
    var paymentTargetInfo by remember { mutableStateOf<PaymentTargetInfo?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            DagangKuTopAppBar(
                title = "Keuangan & Kas",
                windowInsets = WindowInsets(0)
            )
        },
        floatingActionButton = {
            if (selectedTab == 3) {
                ExtendedFloatingActionButton(
                    onClick = { showAddPengeluaranDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.AddCard, contentDescription = null) },
                    text = { Text("Catat Pengeluaran") },
                    modifier = Modifier.testTag("fab_catat_pengeluaran")
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Saldo Kas & Cash Flow Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
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
                                text = "Saldo Kas Saat Ini",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = Formatters.formatRupiah(kasSummary.saldoKas),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (kasSummary.saldoKas >= 0) StatusGreenText else StatusRedText
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    if (kasSummary.saldoAwal > 0L) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Termasuk Saldo Awal Kas:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = Formatters.formatRupiah(kasSummary.saldoAwal),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }

                    // In & Out Summary Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(StatusGreenText)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Uang Masuk (Pelunasan)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Text(
                                text = "+${Formatters.formatRupiah(kasSummary.totalMasuk)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreenText
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(StatusRedText)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Uang Keluar (PO + Beban)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Text(
                                text = "-${Formatters.formatRupiah(kasSummary.totalKeluar)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusRedText
                                )
                            )
                        }
                    }
                }
            }

            // Scrollable Tab Row for 4 Tabs
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Buku Kas (${bukuKas.size})",
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val uncompletedSos = soList.count { it.sisaPiutang > 0 }
                        Text(
                            if (uncompletedSos > 0) "Piutang ($uncompletedSos)" else "Piutang",
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        val uncompletedPos = poList.count { it.sisaHutang > 0 }
                        Text(
                            if (uncompletedPos > 0) "Hutang ($uncompletedPos)" else "Hutang",
                            color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            "Biaya Operasional",
                            color = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: BUKU KAS (CASH FLOW TIMELINE)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = kasFilter == "Semua",
                                onClick = { kasFilter = "Semua" },
                                label = { Text("Semua Mutasi") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = kasFilter == "Masuk",
                                onClick = { kasFilter = "Masuk" },
                                label = { Text("Uang Masuk (+)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusGreenContainer,
                                    selectedLabelColor = StatusGreenText
                                )
                            )
                            FilterChip(
                                selected = kasFilter == "Keluar",
                                onClick = { kasFilter = "Keluar" },
                                label = { Text("Uang Keluar (-)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusRedContainer,
                                    selectedLabelColor = StatusRedText
                                )
                            )
                        }

                        val filteredKas = remember(bukuKas, kasFilter) {
                            when (kasFilter) {
                                "Masuk" -> bukuKas.filter { it.tipe == KasType.MASUK }
                                "Keluar" -> bukuKas.filter { it.tipe == KasType.KELUAR }
                                else -> bukuKas
                            }
                        }

                        if (filteredKas.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.AccountBalanceWallet,
                                title = "Belum Ada Mutasi Kas",
                                subtitle = "Transaksi kas masuk dari pelunasan customer dan kas keluar bayar distributor / operasional akan tercatat di sini."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredKas, key = { it.id }) { tx ->
                                    KasTransactionCard(
                                        transaction = tx,
                                        onNavigateToCustomer = onNavigateToCustomer,
                                        onNavigateToDistributor = onNavigateToDistributor
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(60.dp)) }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: DAFTAR PIUTANG CUSTOMER (WITH "TERIMA BAYAR" DIRECTLY)
                    val uncompletedSos = remember(soList) { soList.filter { it.sisaPiutang > 0 } }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusAmberContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = StatusAmberText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Total Piutang Tertunda: ${Formatters.formatRupiah(summary.totalPiutang)} dari ${uncompletedSos.size} pesanan. Ketuk 'Terima Bayar' untuk mencatat cicilan atau pelunasan.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = StatusAmberText, lineHeight = 16.sp)
                                    )
                                }
                            }
                        }

                        if (uncompletedSos.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.CheckCircle,
                                    title = "Tidak Ada Piutang Tertunda",
                                    subtitle = "Semua pesanan penjualan (SO) pelanggan saat ini sudah lunas!"
                                )
                            }
                        } else {
                            items(uncompletedSos, key = { it.so.id }) { soDetail ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                        .testTag("piutang_item_${soDetail.so.id}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = soDetail.customer?.nama ?: "Customer Umum",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                                Text(
                                                    text = "${soDetail.so.nomor} • ${Formatters.formatTanggal(soDetail.so.tanggal)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }
                                            StatusBadge(status = soDetail.status)
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Total Nilai SO", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                                Text(
                                                    Formatters.formatRupiah(soDetail.so.total),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                            Column {
                                                Text("Sudah Dicicil", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                                Text(
                                                    Formatters.formatRupiah(soDetail.totalPaid),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = StatusGreenText,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Sisa Piutang", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                                Text(
                                                    Formatters.formatRupiah(soDetail.sisaPiutang),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        color = StatusRedText,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                paymentTargetInfo = PaymentTargetInfo(
                                                    tipe = "CUSTOMER",
                                                    refId = soDetail.so.id,
                                                    refNumber = soDetail.so.nomor,
                                                    partyName = soDetail.customer?.nama ?: "Customer Umum",
                                                    totalTransaction = soDetail.so.total,
                                                    alreadyPaid = soDetail.totalPaid,
                                                    remainingBalance = soDetail.sisaPiutang
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = StatusGreenContainer,
                                                contentColor = StatusGreenText
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("btn_terima_bayar_piutang_${soDetail.so.id}")
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Terima Bayar / Cicilan", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(60.dp)) }
                    }
                }

                2 -> {
                    // TAB 2: DAFTAR HUTANG DISTRIBUTOR (WITH "BAYAR HUTANG" DIRECTLY)
                    val uncompletedPos = remember(poList) { poList.filter { it.sisaHutang > 0 } }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusRedContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = StatusRedText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Total Hutang PO ke Distributor: ${Formatters.formatRupiah(summary.totalHutang)} dari ${uncompletedPos.size} pesanan. Ketuk 'Bayar Hutang' untuk mencatat cicilan atau pelunasan.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = StatusRedText, lineHeight = 16.sp)
                                    )
                                }
                            }
                        }

                        if (uncompletedPos.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.CheckCircle,
                                    title = "Tidak Ada Hutang Tertunda",
                                    subtitle = "Semua pesanan pembelian (PO) ke distributor saat ini sudah lunas!"
                                )
                            }
                        } else {
                            items(uncompletedPos, key = { it.po.id }) { poDetail ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                        .testTag("hutang_item_${poDetail.po.id}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = poDetail.distributor?.nama ?: "Distributor",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                                Text(
                                                    text = "${poDetail.po.nomor} • ${Formatters.formatTanggal(poDetail.po.tanggal)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }
                                            StatusBadge(status = poDetail.status)
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Total Nilai PO", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                                Text(
                                                    Formatters.formatRupiah(poDetail.po.total),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                            Column {
                                                Text("Sudah Dibayar", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                                Text(
                                                    Formatters.formatRupiah(poDetail.totalPaid),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = StatusGreenText,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Sisa Hutang", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                                Text(
                                                    Formatters.formatRupiah(poDetail.sisaHutang),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        color = StatusRedText,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                paymentTargetInfo = PaymentTargetInfo(
                                                    tipe = "DISTRIBUTOR",
                                                    refId = poDetail.po.id,
                                                    refNumber = poDetail.po.nomor,
                                                    partyName = poDetail.distributor?.nama ?: "Distributor",
                                                    totalTransaction = poDetail.po.total,
                                                    alreadyPaid = poDetail.totalPaid,
                                                    remainingBalance = poDetail.sisaHutang
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("btn_bayar_hutang_${poDetail.po.id}")
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Bayar Hutang / Cicilan", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(60.dp)) }
                    }
                }

                3 -> {
                    // TAB 3: BIAYA OPERASIONAL
                    if (pengeluaranList.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Receipt,
                            title = "Belum Ada Pengeluaran",
                            subtitle = "Catat biaya operasional seperti listrik, air, gaji, transportasi, atau sewa.",
                            actionButtonText = "Catat Pengeluaran",
                            onActionClick = { showAddPengeluaranDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pengeluaranList, key = { it.id }) { pengeluaran ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = pengeluaran.kategori,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = Formatters.formatTanggal(pengeluaran.tanggal),
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = pengeluaran.keterangan.ifBlank { "Pengeluaran ${pengeluaran.kategori}" },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }

                                        Text(
                                            text = Formatters.formatRupiah(pengeluaran.nominal),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = StatusRedText
                                            )
                                        )

                                        IconButton(
                                            onClick = { pengeluaranToDelete = pengeluaran },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet "Catat Pembayaran"
    if (paymentTargetInfo != null) {
        CatatPembayaranBottomSheet(
            targetInfo = paymentTargetInfo!!,
            onDismiss = { paymentTargetInfo = null },
            onSavePayment = { nominal, metode, catatan ->
                val target = paymentTargetInfo!!
                viewModel.recordPayment(
                    tipe = target.tipe,
                    refId = target.refId,
                    nominal = nominal,
                    metode = metode,
                    catatan = catatan,
                    onSuccess = { paymentTargetInfo = null }
                )
            }
        )
    }

    // Add Pengeluaran Dialog
    if (showAddPengeluaranDialog) {
        AddPengeluaranDialog(
            onDismiss = { showAddPengeluaranDialog = false },
            onSave = { kategori, nominal, keterangan ->
                viewModel.addPengeluaran(
                    kategori = kategori,
                    nominal = nominal,
                    keterangan = keterangan,
                    onSuccess = { showAddPengeluaranDialog = false }
                )
            }
        )
    }

    // Delete Confirmation Dialog
    if (pengeluaranToDelete != null) {
        ConfirmDeleteDialog(
            title = "Hapus Catatan Pengeluaran",
            message = "Apakah Anda yakin ingin menghapus pengeluaran '${pengeluaranToDelete!!.keterangan}' sebesar ${Formatters.formatRupiah(pengeluaranToDelete!!.nominal)}?",
            onConfirm = {
                viewModel.deletePengeluaran(pengeluaranToDelete!!)
                pengeluaranToDelete = null
            },
            onDismiss = { pengeluaranToDelete = null }
        )
    }
}

@Composable
fun KasTransactionCard(
    transaction: KasTransaction,
    onNavigateToCustomer: () -> Unit = {},
    onNavigateToDistributor: () -> Unit = {}
) {
    val isMasuk = transaction.tipe == KasType.MASUK

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .testTag("kas_transaction_${transaction.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isMasuk) StatusGreenContainer else StatusRedContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isMasuk -> Icons.Default.ArrowDownward
                        transaction.refType == "EXPENSE" -> Icons.Default.ReceiptLong
                        else -> Icons.Default.ArrowUpward
                    },
                    contentDescription = null,
                    tint = if (isMasuk) StatusGreenText else StatusRedText,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.judul,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = (if (isMasuk) "+" else "-") + Formatters.formatRupiah(transaction.nominal),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isMasuk) StatusGreenText else StatusRedText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.pihak,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Formatters.formatTanggal(transaction.tanggal)} • ${transaction.metode}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                if (transaction.refDocNumber != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Ref: ${transaction.refDocNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (transaction.catatan.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Catatan: ${transaction.catatan}",
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPengeluaranDialog(
    onDismiss: () -> Unit,
    onSave: (kategori: String, nominal: Long, keterangan: String) -> Unit
) {
    var kategori by remember { mutableStateOf("Operasional") }
    var nominalText by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Operasional", "Listrik & Air", "Gaji", "Sewa", "Transportasi", "Lain-lain")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Catat Pengeluaran Baru",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = StatusRedText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                Text(
                    "Kategori Pengeluaran *",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.take(3).forEach { cat ->
                            FilterChip(
                                selected = kategori == cat,
                                onClick = { kategori = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.drop(3).forEach { cat ->
                            FilterChip(
                                selected = kategori == cat,
                                onClick = { kategori = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nominalText,
                    onValueChange = { nominalText = it.filter { char -> char.isDigit() } },
                    label = { Text("Nominal Pengeluaran *") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nominal_pengeluaran")
                )

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan *") },
                    placeholder = { Text("Contoh: Pembayaran listrik gudang") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_keterangan_pengeluaran")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nominal = nominalText.toLongOrNull() ?: 0L
                    if (nominal <= 0) {
                        errorMessage = "Nominal pengeluaran harus lebih dari 0"
                        return@Button
                    }
                    if (keterangan.isBlank()) {
                        errorMessage = "Keterangan pengeluaran wajib diisi"
                        return@Button
                    }
                    onSave(kategori, nominal, keterangan)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("btn_save_pengeluaran")
            ) {
                Text("Simpan")
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
