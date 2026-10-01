package com.example.ui.screens.po

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
import com.example.data.model.CartItemPo
import com.example.data.model.Distributor
import com.example.data.model.Produk
import com.example.ui.MainViewModel
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.theme.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePoScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSuccessCreate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val distributorList by viewModel.distributorList.collectAsStateWithLifecycle()
    val produkList by viewModel.produkList.collectAsStateWithLifecycle()

    var nomorPo by remember { mutableStateOf(Formatters.generatePoNumber()) }
    var selectedDistributor by remember { mutableStateOf<Distributor?>(distributorList.firstOrNull()) }
    val tanggal by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val cartItems = remember { mutableStateListOf<CartItemPo>() }
    var showAddItemDialog by remember { mutableStateOf(false) }

    var expandedDistributorDropdown by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // If initial selected distributor is null but list loads
    LaunchedEffect(distributorList) {
        if (selectedDistributor == null && distributorList.isNotEmpty()) {
            selectedDistributor = distributorList.first()
        }
    }

    val totalPembelian = cartItems.sumOf { it.subtotal }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = "Buat PO Baru",
                subtitle = "Pesanan Pembelian Stok",
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Pembelian",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = Formatters.formatRupiah(totalPembelian),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp
                            )
                        )
                    }

                    Button(
                        onClick = {
                            val dist = selectedDistributor
                            if (dist == null) {
                                validationError = "Pilih distributor terlebih dahulu"
                                return@Button
                            }
                            if (cartItems.isEmpty()) {
                                validationError = "Tambahkan minimal 1 item produk"
                                return@Button
                            }
                            viewModel.createPO(
                                nomor = nomorPo,
                                distributorId = dist.id,
                                tanggal = tanggal,
                                items = cartItems.toList(),
                                onSuccess = onSuccessCreate
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_submit_po")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan PO", fontWeight = FontWeight.Bold)
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

            // Info Notice: Weighted Average Cost & Stock
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
                            text = "Menyimpan PO akan otomatis menambah stok produk dan memperbarui Harga Dasar (HPP) dengan metode Weighted Average Cost.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer, lineHeight = 16.sp)
                        )
                    }
                }
            }

            // PO Header Section
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
                            text = "Informasi Dokumen PO",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        OutlinedTextField(
                            value = nomorPo,
                            onValueChange = { nomorPo = it },
                            label = { Text("Nomor PO *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_nomor_po")
                        )

                        // Distributor Selector Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedDistributorDropdown,
                            onExpandedChange = { expandedDistributorDropdown = !expandedDistributorDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedDistributor?.nama ?: "Pilih Distributor",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Distributor / Pemasok *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistributorDropdown) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("dropdown_distributor")
                            )

                            ExposedDropdownMenu(
                                expanded = expandedDistributorDropdown,
                                onDismissRequest = { expandedDistributorDropdown = false }
                            ) {
                                distributorList.forEach { dist ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(dist.nama, fontWeight = FontWeight.SemiBold)
                                                if (dist.alamat.isNotBlank()) {
                                                    Text(dist.alamat, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedDistributor = dist
                                            expandedDistributorDropdown = false
                                            validationError = null
                                        }
                                    )
                                }
                            }
                        }

                        // Tanggal display
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
                        text = "Daftar Item Barang (${cartItems.size})",
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
                        modifier = Modifier.testTag("btn_add_item_po")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Item")
                    }
                }
            }

            // Added Items List
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
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada produk yang dimasukkan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                            Text("Ketuk tombol 'Tambah Item' untuk memilih produk yang dibeli.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            } else {
                itemsIndexed(cartItems) { index, item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.produk.nama,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.qty} ${item.produk.satuan} × ${Formatters.formatRupiah(item.hargaBeli)}",
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
                                modifier = Modifier.testTag("remove_item_$index")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Item", tint = StatusRedText)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddPoItemDialog(
            allProduk = produkList.map { it.produk },
            existingCartItems = cartItems.toList(),
            onDismiss = { showAddItemDialog = false },
            onAddItem = { prod, qty, hargaBeli ->
                val existingIndex = cartItems.indexOfFirst { it.produk.id == prod.id }
                if (existingIndex >= 0) {
                    val existing = cartItems[existingIndex]
                    val mergedQty = existing.qty + qty
                    cartItems[existingIndex] = existing.copy(
                        qty = mergedQty,
                        hargaBeli = hargaBeli
                    )
                } else {
                    cartItems.add(CartItemPo(produk = prod, qty = qty, hargaBeli = hargaBeli))
                }
                validationError = null
                showAddItemDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPoItemDialog(
    allProduk: List<Produk>,
    existingCartItems: List<CartItemPo> = emptyList(),
    onDismiss: () -> Unit,
    onAddItem: (produk: Produk, qty: Int, hargaBeli: Long) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(allProduk.firstOrNull()) }
    var qtyText by remember { mutableStateOf("1") }
    var hargaBeliText by remember {
        mutableStateOf(selectedProduct?.hargaDasar?.toString() ?: "")
    }

    var expandedDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val qty = qtyText.toIntOrNull() ?: 0
    val hargaBeli = hargaBeliText.toLongOrNull() ?: 0L
    val subtotal = qty.toLong() * hargaBeli

    val existingInCart = remember(selectedProduct, existingCartItems) {
        selectedProduct?.let { p -> existingCartItems.firstOrNull { it.produk.id == p.id } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Tambah Produk ke PO",
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
                        value = selectedProduct?.nama ?: "Pilih Produk",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Produk *") },
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
                                        Text("HPP Sekarang: ${Formatters.formatRupiah(prod.hargaDasar)} / ${prod.satuan}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedProduct = prod
                                    hargaBeliText = prod.hargaDasar.toString()
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                if (existingInCart != null) {
                    Surface(
                        color = StatusAmberContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Produk ini sudah ada di daftar (${existingInCart.qty} ${selectedProduct?.satuan}). Jumlah akan digabungkan menjadi ${existingInCart.qty + qty} ${selectedProduct?.satuan}.",
                            color = StatusAmberText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { char -> char.isDigit() } },
                        label = { Text("Jumlah (Qty) *") },
                        suffix = { Text(selectedProduct?.satuan ?: "unit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_po_qty")
                    )

                    OutlinedTextField(
                        value = hargaBeliText,
                        onValueChange = { hargaBeliText = it.filter { char -> char.isDigit() } },
                        label = { Text("Harga Beli / Unit *") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_po_harga_beli")
                    )
                }

                // Subtotal preview
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
                    val prod = selectedProduct
                    if (prod == null) {
                        errorMessage = "Pilih produk terlebih dahulu"
                        return@Button
                    }
                    if (qty <= 0) {
                        errorMessage = "Jumlah qty harus lebih dari 0"
                        return@Button
                    }
                    if (hargaBeli <= 0) {
                        errorMessage = "Harga beli harus lebih dari 0"
                        return@Button
                    }
                    onAddItem(prod, qty, hargaBeli)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("btn_confirm_add_po_item")
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
