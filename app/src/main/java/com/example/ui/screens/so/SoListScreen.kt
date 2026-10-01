package com.example.ui.screens.so

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SoWithDetails
import com.example.data.model.StatusPembayaran
import com.example.ui.MainViewModel
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoListScreen(
    viewModel: MainViewModel,
    onNavigateToCreateSo: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val soList by viewModel.soList.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("Semua") }

    val filteredList = remember(soList, searchQuery, selectedStatusFilter) {
        soList.filter { item ->
            val matchesSearch = item.so.nomor.contains(searchQuery, ignoreCase = true) ||
                    (item.customer?.nama?.contains(searchQuery, ignoreCase = true) == true)
            val matchesStatus = when (selectedStatusFilter) {
                "Belum Lunas" -> item.status == StatusPembayaran.BELUM_LUNAS
                "Sebagian" -> item.status == StatusPembayaran.SEBAGIAN
                "Lunas" -> item.status == StatusPembayaran.LUNAS
                else -> true
            }
            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (showHeader) {
                DagangKuTopAppBar(
                    title = "Penjualan (${soList.size})",
                    windowInsets = WindowInsets(0)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateSo,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_so")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat SO Baru")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                    .testTag("search_so_input"),
                placeholder = { Text("Cari SO / pelanggan") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Semua", "Belum Lunas", "Sebagian", "Lunas")
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedStatusFilter == filter,
                        onClick = { selectedStatusFilter = filter },
                        label = { Text(filter, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (filteredList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PointOfSale,
                    title = if (searchQuery.isNotEmpty()) "SO tidak ditemukan" else "Belum ada pesanan penjualan (SO)",
                    subtitle = if (searchQuery.isNotEmpty()) "Coba kata kunci pencarian yang lain." else "Buat SO untuk menjual barang ke pelanggan, mencatat piutang, dan mengurangi stok otomatis.",
                    actionButtonText = if (searchQuery.isEmpty()) "Buat SO Sekarang" else null,
                    onActionClick = onNavigateToCreateSo
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.so.id }) { soDetail ->
                        SoCard(
                            soDetail = soDetail,
                            onClick = { onNavigateToDetail(soDetail.so.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SoCard(
    soDetail: SoWithDetails,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("so_card_${soDetail.so.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = soDetail.so.nomor,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${soDetail.customer?.nama ?: "Umum"} • ${Formatters.formatTanggal(soDetail.so.tanggal)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                StatusBadge(status = soDetail.status)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Penjualan (${soDetail.items.size} item)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    )
                    Text(
                        text = Formatters.formatRupiah(soDetail.so.total),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (soDetail.sisaPiutang > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Sisa Piutang",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                        Text(
                            text = Formatters.formatRupiah(soDetail.sisaPiutang),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusRedText
                            )
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreenText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lunas",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = StatusGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
