package com.example.ui.screens.customer

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.Customer
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DagangKuTopAppBar
import kotlinx.coroutines.launch
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val customerList by viewModel.customerList.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    var showFormDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var cannotDeleteMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val filteredList = remember(customerList, searchQuery) {
        if (searchQuery.isBlank()) customerList else {
            customerList.filter {
                it.nama.contains(searchQuery, ignoreCase = true) ||
                        it.noHp.contains(searchQuery, ignoreCase = true) ||
                        it.alamat.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = "Data Pelanggan (Customer)",
                subtitle = "${customerList.size} Pelanggan Terdaftar"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    showFormDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pelanggan")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_customer_input"),
                placeholder = { Text("Cari nama, nomor HP, atau alamat...") },
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

            if (filteredList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = if (searchQuery.isNotEmpty()) "Pelanggan tidak ditemukan" else "Belum ada pelanggan",
                    subtitle = if (searchQuery.isNotEmpty()) "Coba kata kunci pencarian lain." else "Tambah pelanggan untuk mengelola transaksi penjualan dan harga khusus (deal price).",
                    actionButtonText = if (searchQuery.isEmpty()) "Tambah Pelanggan" else null,
                    onActionClick = {
                        editingCustomer = null
                        showFormDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onClick = { onNavigateToDetail(customer.id) },
                            onEdit = {
                                editingCustomer = customer
                                showFormDialog = true
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    if (!viewModel.canDeleteCustomer(customer.id)) {
                                        cannotDeleteMessage = "Pelanggan '${customer.nama}' tidak dapat dihapus karena sudah memiliki riwayat transaksi Penjualan (SO)."
                                    } else {
                                        customerToDelete = customer
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

    if (showFormDialog) {
        CustomerFormDialog(
            initialCustomer = editingCustomer,
            onDismiss = { showFormDialog = false },
            onSave = { nama, noHp, alamat, komisi ->
                viewModel.saveCustomer(
                    id = editingCustomer?.id ?: 0L,
                    nama = nama,
                    noHp = noHp,
                    alamat = alamat,
                    persenKomisi = komisi,
                    onSuccess = { showFormDialog = false }
                )
            }
        )
    }

    cannotDeleteMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { cannotDeleteMessage = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusAmberText) },
            title = { Text("Tidak Dapat Menghapus Pelanggan", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { cannotDeleteMessage = null }) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    customerToDelete?.let { cust ->
        ConfirmDeleteDialog(
            title = "Hapus Pelanggan?",
            message = "Apakah Anda yakin ingin menghapus pelanggan '${cust.nama}'? Seluruh data kesepakatan harga deal khusus pelanggan ini akan dihapus.",
            onConfirm = {
                viewModel.deleteCustomer(cust)
                customerToDelete = null
            },
            onDismiss = { customerToDelete = null }
        )
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("customer_card_${customer.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.nama,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.persenKomisi > 0) {
                        Surface(
                            color = StatusPurpleContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Komisi: ${Formatters.formatPersen(customer.persenKomisi)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = StatusPurpleText,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_customer_${customer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Pelanggan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_customer_${customer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Pelanggan",
                            tint = StatusRedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (customer.noHp.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customer.noHp,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            if (customer.alamat.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customer.alamat,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lihat Profil & Harga Khusus",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CustomerFormDialog(
    initialCustomer: Customer?,
    onDismiss: () -> Unit,
    onSave: (nama: String, noHp: String, alamat: String, komisi: Double) -> Unit
) {
    var nama by remember { mutableStateOf(initialCustomer?.nama ?: "") }
    var noHp by remember { mutableStateOf(initialCustomer?.noHp ?: "") }
    var alamat by remember { mutableStateOf(initialCustomer?.alamat ?: "") }
    var komisiText by remember { mutableStateOf(initialCustomer?.persenKomisi?.toString() ?: "0") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (initialCustomer == null) "Tambah Pelanggan Baru" else "Edit Pelanggan",
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
                    label = { Text("Nama Pelanggan / Toko *") },
                    placeholder = { Text("Contoh: Toko Barokah Jaya") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_nama")
                )

                OutlinedTextField(
                    value = noHp,
                    onValueChange = { noHp = it },
                    label = { Text("Nomor HP / WhatsApp") },
                    placeholder = { Text("Contoh: 081234567890") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_phone")
                )

                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat") },
                    placeholder = { Text("Alamat toko atau pengiriman") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_address")
                )

                OutlinedTextField(
                    value = komisiText,
                    onValueChange = { komisiText = it },
                    label = { Text("Persentase Komisi (%)") },
                    placeholder = { Text("0") },
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_customer_komisi")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isBlank()) {
                        errorMessage = "Nama pelanggan wajib diisi"
                        return@Button
                    }
                    val komisi = komisiText.toDoubleOrNull() ?: 0.0
                    onSave(nama.trim(), noHp.trim(), alamat.trim(), komisi)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("btn_save_customer")
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
