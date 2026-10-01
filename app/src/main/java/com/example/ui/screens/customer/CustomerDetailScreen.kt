package com.example.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerDealItem
import com.example.data.model.Produk
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSoDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val customerList by viewModel.customerList.collectAsStateWithLifecycle()
    val customer = customerList.find { it.id == customerId }

    val dealPricesFlow = remember(customerId) { viewModel.getCustomerDealPrices(customerId) }
    val dealPrices by dealPricesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val produkList by viewModel.produkList.collectAsStateWithLifecycle()
    val soList by viewModel.soList.collectAsStateWithLifecycle()
    val customerSos = soList.filter { it.so.customerId == customerId }

    var showSetDealDialog by remember { mutableStateOf(false) }
    var selectedProductForDeal by remember { mutableStateOf<Produk?>(null) }
    var existingDealPriceToEdit by remember { mutableStateOf<Long?>(null) }

    var dealToDelete by remember { mutableStateOf<CustomerDealItem?>(null) }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = customer?.nama ?: "Detail Pelanggan",
                subtitle = "Kelola Profil & Harga Khusus",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedProductForDeal = null
                    existingDealPriceToEdit = null
                    showSetDealDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Sell, contentDescription = null) },
                text = { Text("Atur Harga Deal") },
                modifier = Modifier.testTag("btn_add_deal_price")
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (customer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Pelanggan tidak ditemukan")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer Profile Header Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = customer.nama,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (customer.persenKomisi > 0) {
                                        Text(
                                            text = "Komisi Penjualan: ${Formatters.formatPersen(customer.persenKomisi)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = StatusPurpleText,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)

                            if (customer.noHp.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = customer.noHp, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                                }
                            }

                            if (customer.alamat.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = customer.alamat, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                                }
                            }
                        }
                    }
                }

                // Deal Prices Section Header
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Harga Deal Khusus Pelanggan (${dealPrices.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Saat membuat SO untuk ${customer.nama}, harga produk otomatis memakai Harga Deal ini (tetap dapat diubah saat transaksi).",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Deal Prices List
                if (dealPrices.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Belum Ada Harga Khusus",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Pelanggan ini saat ini menggunakan harga jual standar.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                } else {
                    items(dealPrices, key = { it.hargaCustomer.id }) { dealItem ->
                        DealPriceCard(
                            dealItem = dealItem,
                            onEdit = {
                                selectedProductForDeal = dealItem.produk
                                existingDealPriceToEdit = dealItem.hargaCustomer.hargaDeal
                                showSetDealDialog = true
                            },
                            onDelete = {
                                dealToDelete = dealItem
                            }
                        )
                    }
                }

                // SO History for this Customer
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Riwayat Pesanan Pelanggan (${customerSos.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (customerSos.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada pesanan penjualan untuk pelanggan ini.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(customerSos, key = { it.so.id }) { soDetail ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSoDetail(soDetail.so.id) }
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = soDetail.so.nomor,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = Formatters.formatTanggal(soDetail.so.tanggal),
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = Formatters.formatRupiah(soDetail.so.total),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                StatusBadge(status = soDetail.status)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    // Set / Edit Deal Price Dialog
    if (showSetDealDialog) {
        SetDealPriceDialog(
            customerName = customer?.nama ?: "",
            allProduk = produkList.map { it.produk },
            selectedProduk = selectedProductForDeal,
            initialDealPrice = existingDealPriceToEdit,
            onDismiss = { showSetDealDialog = false },
            onSave = { prodId, price ->
                viewModel.setCustomerDealPrice(customerId, prodId, price)
                showSetDealDialog = false
            }
        )
    }

    // Delete Deal Price Confirmation
    dealToDelete?.let { dealItem ->
        ConfirmDeleteDialog(
            title = "Hapus Harga Deal?",
            message = "Apakah Anda yakin ingin menghapus harga deal khusus untuk '${dealItem.produk.nama}'? Pelanggan akan kembali menggunakan harga jual standar.",
            onConfirm = {
                viewModel.removeCustomerDealPrice(customerId, dealItem.produk.id)
                dealToDelete = null
            },
            onDismiss = { dealToDelete = null }
        )
    }
}

@Composable
fun DealPriceCard(
    dealItem: CustomerDealItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val selisih = dealItem.produk.hargaJual - dealItem.hargaCustomer.hargaDeal

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatusAmberContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sell,
                    contentDescription = null,
                    tint = StatusAmberText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dealItem.produk.nama,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.formatRupiah(dealItem.produk.hargaJual),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Formatters.formatRupiah(dealItem.hargaCustomer.hargaDeal),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = " / ${dealItem.produk.satuan}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (selisih > 0) {
                    Text(
                        text = "Hemat ${Formatters.formatRupiah(selisih)} dari harga normal",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StatusGreenText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Harga Deal",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus Harga Deal",
                        tint = StatusRedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDealPriceDialog(
    customerName: String,
    allProduk: List<Produk>,
    selectedProduk: Produk?,
    initialDealPrice: Long?,
    onDismiss: () -> Unit,
    onSave: (produkId: Long, hargaDeal: Long) -> Unit
) {
    var chosenProduct by remember { mutableStateOf(selectedProduk ?: allProduk.firstOrNull()) }
    var dealPriceText by remember {
        mutableStateOf(
            initialDealPrice?.toString()
                ?: chosenProduct?.hargaJual?.toString()
                ?: ""
        )
    }
    var expandedDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Atur Harga Khusus Deal",
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
                Text(
                    text = "Pelanggan: $customerName",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = StatusRedText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                // Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = chosenProduct?.let { "${it.nama} (Normal: ${Formatters.formatRupiah(it.hargaJual)})" } ?: "Pilih Produk",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Produk *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        allProduk.forEach { prod ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(prod.nama, fontWeight = FontWeight.SemiBold)
                                        Text("Harga Normal: ${Formatters.formatRupiah(prod.hargaJual)} / ${prod.satuan}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    chosenProduct = prod
                                    if (initialDealPrice == null) {
                                        dealPriceText = prod.hargaJual.toString()
                                    }
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                chosenProduct?.let { prod ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Harga Standar:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${Formatters.formatRupiah(prod.hargaJual)} / ${prod.satuan}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dealPriceText,
                    onValueChange = { dealPriceText = it.filter { char -> char.isDigit() } },
                    label = { Text("Harga Deal Khusus *") },
                    prefix = { Text("Rp ") },
                    placeholder = { Text("Masukkan harga kesepakatan") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_harga_deal")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prod = chosenProduct
                    if (prod == null) {
                        errorMessage = "Pilih produk terlebih dahulu"
                        return@Button
                    }
                    val price = dealPriceText.toLongOrNull() ?: 0L
                    if (price <= 0) {
                        errorMessage = "Harga deal harus lebih dari 0"
                        return@Button
                    }
                    onSave(prod.id, price)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("btn_save_deal_price")
            ) {
                Text("Simpan Harga Deal")
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
