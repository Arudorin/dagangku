package com.example.ui.screens.so

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CartItemSo
import com.example.data.model.Customer
import com.example.data.model.ProdukWithStock
import com.example.ui.MainViewModel
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.theme.*
import com.example.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSoScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSuccessCreate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val customerList by viewModel.customerList.collectAsStateWithLifecycle()
    val produkList by viewModel.produkList.collectAsStateWithLifecycle()

    var nomorSo by remember { mutableStateOf(Formatters.generateSoNumber()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(customerList.firstOrNull()) }
    val tanggal by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val cartItems = remember { mutableStateListOf<CartItemSo>() }
    var showAddItemDialog by remember { mutableStateOf(false) }

    var expandedCustomerDropdown by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(customerList) {
        if (selectedCustomer == null && customerList.isNotEmpty()) {
            selectedCustomer = customerList.first()
        }
    }

    // When customer changes, optionally re-evaluate prices for items in cart
    fun updateCartPricesForCustomer(customer: Customer) {
        coroutineScope.launch {
            val updated = cartItems.map { item ->
                val effectivePrice = viewModel.getEffectivePriceForCustomer(customer.id, item.produk.id)
                val isDeal = effectivePrice != item.produk.hargaJual
                item.copy(hargaJual = effectivePrice, isDealPrice = isDeal)
            }
            cartItems.clear()
            cartItems.addAll(updated)
        }
    }

    val totalPenjualan = cartItems.sumOf { it.subtotal }
    val komisiEstimasi = if (selectedCustomer != null && selectedCustomer!!.persenKomisi > 0) {
        (totalPenjualan * selectedCustomer!!.persenKomisi) / 100.0
    } else 0.0

    val hasInsufficientStock = cartItems.any { it.isInsufficientStock }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = "Buat SO Baru",
                subtitle = "Pesanan Penjualan Barang",
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (hasInsufficientStock) {
                        Surface(
                            color = StatusRedContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "⚠️ Transaksi ditolak: Ada item dengan jumlah melebihi stok tersedia!",
                                color = StatusRedText,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Penjualan",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = Formatters.formatRupiah(totalPenjualan),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                )
                            )
                            if (komisiEstimasi > 0) {
                                Text(
                                    text = "Estimasi Komisi: ${Formatters.formatRupiah(komisiEstimasi)} (${Formatters.formatPersen(selectedCustomer?.persenKomisi ?: 0.0)})",
                                    style = MaterialTheme.typography.labelSmall.copy(color = StatusPurpleText, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val cust = selectedCustomer
                                if (cust == null) {
                                    validationError = "Pilih pelanggan terlebih dahulu"
                                    return@Button
                                }
                                if (cartItems.isEmpty()) {
                                    validationError = "Tambahkan minimal 1 item produk"
                                    return@Button
                                }
                                if (hasInsufficientStock) {
                                    validationError = "Stok tidak mencukupi untuk beberapa produk!"
                                    return@Button
                                }
                                viewModel.createSO(
                                    nomor = nomorSo,
                                    customerId = cust.id,
                                    tanggal = tanggal,
                                    items = cartItems.toList(),
                                    onSuccess = onSuccessCreate
                                )
                            },
                            enabled = !hasInsufficientStock && cartItems.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_submit_so")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan SO", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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
            // Validation Error Alert
            if (validationError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusRedContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusRedText)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.bodyMedium.copy(color = StatusRedText)
                            )
                        }
                    }
                }
            }

            // Notice about auto deal prices & stock validation
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Harga otomatis menggunakan Harga Deal Pelanggan jika ada (tetap dapat diubah). SO tidak dapat disimpan bila stok kurang.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer, lineHeight = 16.sp)
                        )
                    }
                }
            }

            // SO Header Form
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Informasi Dokumen SO",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        OutlinedTextField(
                            value = nomorSo,
                            onValueChange = { nomorSo = it },
                            label = { Text("Nomor SO *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_nomor_so")
                        )

                        // Customer Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedCustomerDropdown,
                            onExpandedChange = { expandedCustomerDropdown = !expandedCustomerDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.let {
                                    if (it.persenKomisi > 0) "${it.nama} (Komisi ${Formatters.formatPersen(it.persenKomisi)})" else it.nama
                                } ?: "Pilih Pelanggan",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pelanggan (Customer) *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomerDropdown) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("dropdown_customer")
                            )

                            ExposedDropdownMenu(
                                expanded = expandedCustomerDropdown,
                                onDismissRequest = { expandedCustomerDropdown = false }
                            ) {
                                customerList.forEach { cust ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(cust.nama, fontWeight = FontWeight.SemiBold)
                                                if (cust.persenKomisi > 0) {
                                                    Text("Komisi: ${Formatters.formatPersen(cust.persenKomisi)}", fontSize = 11.sp, color = StatusPurpleText)
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCustomer = cust
                                            updateCartPricesForCustomer(cust)
                                            expandedCustomerDropdown = false
                                            validationError = null
                                        }
                                    )
                                }
                            }
                        }

                        // Tanggal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tanggal Dokumen:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatTanggal(tanggal),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }

            // Items Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Item Barang Penjualan (${cartItems.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Button(
                        onClick = { showAddItemDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_item_so")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Item")
                    }
                }
            }

            // Cart Items List
            if (cartItems.isEmpty()) {
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
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada produk yang dijual", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                            Text("Ketuk tombol 'Tambah Item' untuk memilih produk yang dijual.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            } else {
                itemsIndexed(cartItems) { index, item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isInsufficientStock) StatusRedContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (item.isInsufficientStock) StatusRedText else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.produk.nama,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.isDealPrice) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = StatusAmberContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "Harga Deal",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = StatusAmberText,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.qty} ${item.produk.satuan} × ${Formatters.formatRupiah(item.hargaJual)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = "Subtotal: ${Formatters.formatRupiah(item.subtotal)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = { cartItems.removeAt(index) },
                                    modifier = Modifier.testTag("remove_so_item_$index")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Item", tint = StatusRedText)
                                }
                            }

                            if (item.isInsufficientStock) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "❌ Stok tidak mencukupi! Tersedia: ${item.availableStock} ${item.produk.satuan}, Diminta: ${item.qty}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StatusRedText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddSoItemDialog(
            allProdukWithStock = produkList,
            customerId = selectedCustomer?.id ?: 0L,
            viewModel = viewModel,
            onDismiss = { showAddItemDialog = false },
            onAddItem = { prodWithStock, qty, hargaJual, isDeal ->
                cartItems.add(
                    CartItemSo(
                        produk = prodWithStock.produk,
                        qty = qty,
                        hargaJual = hargaJual,
                        isDealPrice = isDeal,
                        availableStock = prodWithStock.stok
                    )
                )
                validationError = null
                showAddItemDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSoItemDialog(
    allProdukWithStock: List<ProdukWithStock>,
    customerId: Long,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onAddItem: (produkWithStock: ProdukWithStock, qty: Int, hargaJual: Long, isDeal: Boolean) -> Unit
) {
    var selectedItem by remember { mutableStateOf(allProdukWithStock.firstOrNull()) }
    var qtyText by remember { mutableStateOf("1") }
    var hargaJualText by remember { mutableStateOf("") }
    var isDealPrice by remember { mutableStateOf(false) }

    var expandedDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch effective price whenever product changes or dialog opens
    LaunchedEffect(selectedItem, customerId) {
        selectedItem?.let { item ->
            val effective = viewModel.getEffectivePriceForCustomer(customerId, item.produk.id)
            hargaJualText = effective.toString()
            isDealPrice = (effective != item.produk.hargaJual)
        }
    }

    val qty = qtyText.toIntOrNull() ?: 0
    val hargaJual = hargaJualText.toLongOrNull() ?: 0L
    val subtotal = qty.toLong() * hargaJual
    val availableStock = selectedItem?.stok ?: 0
    val isStockInsufficient = qty > availableStock

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Tambah Produk ke Penjualan",
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

                // Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.let { "${it.produk.nama} (Stok: ${it.stok} ${it.produk.satuan})" } ?: "Pilih Produk",
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
                        allProdukWithStock.forEach { prodItem ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(prodItem.produk.nama, fontWeight = FontWeight.SemiBold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                "Stok: ${prodItem.stok} ${prodItem.produk.satuan}",
                                                fontSize = 11.sp,
                                                color = if (prodItem.stok <= 0) StatusRedText else StatusGreenText,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Normal: ${Formatters.formatRupiah(prodItem.produk.hargaJual)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedItem = prodItem
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Available Stock & Deal Price Banner
                selectedItem?.let { item ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.stok <= 0) StatusRedContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stok Tersedia Saat Ini:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${item.stok} ${item.produk.satuan}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.stok <= 0) StatusRedText else StatusGreenText
                                )
                            )
                        }
                    }
                }

                if (isDealPrice) {
                    Surface(
                        color = StatusAmberContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sell, contentDescription = null, tint = StatusAmberText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Harga deal khusus diterapkan untuk pelanggan ini (tetap dapat diedit)",
                                style = MaterialTheme.typography.labelSmall.copy(color = StatusAmberText, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { char -> char.isDigit() } },
                        label = { Text("Jumlah (Qty) *") },
                        suffix = { Text(selectedItem?.produk?.satuan ?: "unit") },
                        isError = isStockInsufficient,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_so_qty")
                    )

                    OutlinedTextField(
                        value = hargaJualText,
                        onValueChange = { hargaJualText = it.filter { char -> char.isDigit() } },
                        label = { Text("Harga Satuan *") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_so_harga_jual")
                    )
                }

                if (isStockInsufficient) {
                    Text(
                        text = "⚠️ Stok tidak mencukupi! Maksimal: $availableStock",
                        color = StatusRedText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Subtotal calculation preview
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
                        Text("Subtotal Item:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = Formatters.formatRupiah(subtotal),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prodItem = selectedItem
                    if (prodItem == null) {
                        errorMessage = "Pilih produk terlebih dahulu"
                        return@Button
                    }
                    if (qty <= 0) {
                        errorMessage = "Jumlah qty harus lebih dari 0"
                        return@Button
                    }
                    // CRITICAL: Block if stock is insufficient
                    if (qty > prodItem.stok) {
                        errorMessage = "Stok tidak mencukupi! Hanya tersedia ${prodItem.stok} ${prodItem.produk.satuan}"
                        return@Button
                    }
                    if (hargaJual <= 0) {
                        errorMessage = "Harga jual harus lebih dari 0"
                        return@Button
                    }
                    onAddItem(prodItem, qty, hargaJual, isDealPrice)
                },
                enabled = !isStockInsufficient && qty > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("btn_confirm_add_so_item")
            ) {
                Text("Tambahkan")
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
