package com.example.ui.screens.stok

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Produk
import com.example.data.model.ProdukWithStock
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DagangKuTopAppBar
import kotlinx.coroutines.launch
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StockBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val produkList by viewModel.produkList.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") } // "Semua", "Menipis", "Tersedia", "Habis"

    var showFormDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Produk?>(null) }
    var productToDelete by remember { mutableStateOf<Produk?>(null) }
    var cannotDeleteMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val filteredList = remember(produkList, searchQuery, selectedFilter) {
        produkList.filter { item ->
            val matchesSearch = item.produk.nama.contains(searchQuery, ignoreCase = true) ||
                    item.produk.satuan.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Menipis" -> item.isLowStock && item.stok > 0
                "Habis" -> item.stok <= 0
                "Tersedia" -> item.stok > item.produk.stokMinimum
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            DagangKuTopAppBar(
                title = "Inventaris (${produkList.size})",
                windowInsets = WindowInsets(0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showFormDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_produk")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
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
                    .testTag("search_produk_input"),
                placeholder = { Text("Cari produk atau satuan...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Semua", "Menipis", "Tersedia", "Habis")
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Product List
            if (filteredList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = if (searchQuery.isNotEmpty()) "Produk tidak ditemukan" else "Belum ada produk",
                    subtitle = if (searchQuery.isNotEmpty()) "Coba kata kunci pencarian yang lain." else "Mulai dengan menambahkan master produk dan stok awal.",
                    actionButtonText = if (searchQuery.isEmpty()) "Tambah Produk" else null,
                    onActionClick = {
                        editingProduct = null
                        showFormDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.produk.id }) { itemWithStock ->
                        ProdukCard(
                            item = itemWithStock,
                            onEdit = {
                                editingProduct = itemWithStock.produk
                                showFormDialog = true
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    if (!viewModel.canDeleteProduk(itemWithStock.produk.id)) {
                                        cannotDeleteMessage = "Produk '${itemWithStock.produk.nama}' tidak dapat dihapus karena sudah digunakan dalam riwayat transaksi pembelian (PO) atau penjualan (SO)."
                                    } else {
                                        productToDelete = itemWithStock.produk
                                    }
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        }
    }

    // Add/Edit Product Dialog
    if (showFormDialog) {
        ProdukFormDialog(
            initialProduk = editingProduct,
            onDismiss = { showFormDialog = false },
            onSave = { nama, satuan, hargaDasar, hargaJual, stokMin ->
                viewModel.saveProduk(
                    id = editingProduct?.id ?: 0L,
                    nama = nama,
                    satuan = satuan,
                    hargaDasar = hargaDasar,
                    hargaJual = hargaJual,
                    stokMinimum = stokMin,
                    onSuccess = { showFormDialog = false }
                )
            }
        )
    }

    // Cannot Delete Alert Dialog
    cannotDeleteMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { cannotDeleteMessage = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusAmberText) },
            title = { Text("Tidak Dapat Menghapus Produk", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { cannotDeleteMessage = null }) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { prod ->
        ConfirmDeleteDialog(
            title = "Hapus Produk?",
            message = "Apakah Anda yakin ingin menghapus '${prod.nama}'? Seluruh data stok dan riwayat harga deal terkait produk ini akan dihapus.",
            onConfirm = {
                viewModel.deleteProduk(prod)
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }
}

@Composable
fun ProdukCard(
    item: ProdukWithStock,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val margin = item.produk.hargaJual - item.produk.hargaDasar

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (item.isLowStock) 1.5.dp else 1.dp,
                color = if (item.isLowStock) StatusAmberText else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("produk_card_${item.produk.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.produk.nama,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StockBadge(
                        stok = item.stok,
                        stokMinimum = item.produk.stokMinimum,
                        satuan = item.produk.satuan
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_produk_${item.produk.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Produk",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_produk_${item.produk.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Produk",
                            tint = StatusRedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Price & Margin Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Harga Dasar (HPP)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    )
                    Text(
                        text = Formatters.formatRupiah(item.produk.hargaDasar),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Harga Jual",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    )
                    Text(
                        text = Formatters.formatRupiah(item.produk.hargaJual),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Margin Keuntungan",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    )
                    Text(
                        text = "+${Formatters.formatRupiah(margin)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = StatusGreenText
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ProdukFormDialog(
    initialProduk: Produk?,
    onDismiss: () -> Unit,
    onSave: (nama: String, satuan: String, hargaDasar: Long, hargaJual: Long, stokMin: Int) -> Unit
) {
    var nama by remember { mutableStateOf(initialProduk?.nama ?: "") }
    var satuan by remember { mutableStateOf(initialProduk?.satuan ?: "Pcs") }
    var hargaDasarText by remember { mutableStateOf(initialProduk?.hargaDasar?.toString() ?: "") }
    var hargaJualText by remember { mutableStateOf(initialProduk?.hargaJual?.toString() ?: "") }
    var stokMinText by remember { mutableStateOf(initialProduk?.stokMinimum?.toString() ?: "5") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val commonUnits = listOf("Pcs", "Kg", "Dus", "Karton", "Btl", "Pak", "Sak", "Lusin")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (initialProduk == null) "Tambah Produk Baru" else "Edit Produk",
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

                OutlinedTextField(
                    value = nama,
                    onValueChange = {
                        nama = it
                        errorMessage = null
                    },
                    label = { Text("Nama Produk *") },
                    placeholder = { Text("Contoh: Beras Pandan Wangi 5kg") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_produk_nama")
                )

                // Satuan selector & field
                Column {
                    OutlinedTextField(
                        value = satuan,
                        onValueChange = {
                            satuan = it
                            errorMessage = null
                        },
                        label = { Text("Satuan Unit *") },
                        placeholder = { Text("Contoh: Pcs, Kg, Dus") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_produk_satuan")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(commonUnits) { unit ->
                            SuggestionChip(
                                onClick = { satuan = unit },
                                label = { Text(unit, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hargaDasarText,
                        onValueChange = { hargaDasarText = it.filter { char -> char.isDigit() } },
                        label = { Text("HPP / Harga Dasar") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_produk_harga_dasar")
                    )
                    OutlinedTextField(
                        value = hargaJualText,
                        onValueChange = { hargaJualText = it.filter { char -> char.isDigit() } },
                        label = { Text("Harga Jual *") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_produk_harga_jual")
                    )
                }

                OutlinedTextField(
                    value = stokMinText,
                    onValueChange = { stokMinText = it.filter { char -> char.isDigit() } },
                    label = { Text("Stok Minimum Peringatan") },
                    placeholder = { Text("Default: 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_produk_stok_min")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isBlank()) {
                        errorMessage = "Nama produk wajib diisi"
                        return@Button
                    }
                    if (satuan.isBlank()) {
                        errorMessage = "Satuan produk wajib diisi"
                        return@Button
                    }
                    val hargaJual = hargaJualText.toLongOrNull() ?: 0L
                    val hargaDasar = hargaDasarText.toLongOrNull() ?: 0L
                    val stokMin = stokMinText.toIntOrNull() ?: 5

                    onSave(nama.trim(), satuan.trim(), hargaDasar, hargaJual, stokMin)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("btn_save_produk")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
