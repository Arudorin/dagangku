package com.example.ui.screens.distributor

import androidx.compose.foundation.border
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Distributor
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val distributorList by viewModel.distributorList.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    var showFormDialog by remember { mutableStateOf(false) }
    var editingDistributor by remember { mutableStateOf<Distributor?>(null) }
    var distributorToDelete by remember { mutableStateOf<Distributor?>(null) }

    val filteredList = remember(distributorList, searchQuery) {
        if (searchQuery.isBlank()) distributorList else {
            distributorList.filter {
                it.nama.contains(searchQuery, ignoreCase = true) ||
                        it.noHp.contains(searchQuery, ignoreCase = true) ||
                        it.alamat.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = "Data Distributor (Pemasok)",
                subtitle = "${distributorList.size} Distributor Terdaftar"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingDistributor = null
                    showFormDialog = true
                },
                containerColor = DagangBluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_distributor")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Distributor")
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
                    .testTag("search_distributor_input"),
                placeholder = { Text("Cari nama distributor, kontak, atau alamat...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NeutralTextSecondary)
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
                    icon = Icons.Default.LocalShipping,
                    title = if (searchQuery.isNotEmpty()) "Distributor tidak ditemukan" else "Belum ada distributor",
                    subtitle = if (searchQuery.isNotEmpty()) "Coba kata kunci pencarian lain." else "Tambah distributor untuk mulai mencatat pesanan pembelian stok (PO).",
                    actionButtonText = if (searchQuery.isEmpty()) "Tambah Distributor" else null,
                    onActionClick = {
                        editingDistributor = null
                        showFormDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { distributor ->
                        DistributorCard(
                            distributor = distributor,
                            onEdit = {
                                editingDistributor = distributor
                                showFormDialog = true
                            },
                            onDelete = {
                                distributorToDelete = distributor
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        }
    }

    if (showFormDialog) {
        DistributorFormDialog(
            initialDistributor = editingDistributor,
            onDismiss = { showFormDialog = false },
            onSave = { nama, noHp, alamat ->
                viewModel.saveDistributor(
                    id = editingDistributor?.id ?: 0L,
                    nama = nama,
                    noHp = noHp,
                    alamat = alamat,
                    onSuccess = { showFormDialog = false }
                )
            }
        )
    }

    distributorToDelete?.let { dist ->
        ConfirmDeleteDialog(
            title = "Hapus Distributor?",
            message = "Apakah Anda yakin ingin menghapus distributor '${dist.nama}'?",
            onConfirm = {
                viewModel.deleteDistributor(dist)
                distributorToDelete = null
            },
            onDismiss = { distributorToDelete = null }
        )
    }
}

@Composable
fun DistributorCard(
    distributor: Distributor,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeutralOutline, RoundedCornerShape(16.dp))
            .testTag("distributor_card_${distributor.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = distributor.nama,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeutralTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_distributor_${distributor.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Distributor",
                            tint = DagangBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_distributor_${distributor.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Distributor",
                            tint = DagangDebtRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (distributor.noHp.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = NeutralTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = distributor.noHp,
                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                    )
                }
            }

            if (distributor.alamat.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = NeutralTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = distributor.alamat,
                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DistributorFormDialog(
    initialDistributor: Distributor?,
    onDismiss: () -> Unit,
    onSave: (nama: String, noHp: String, alamat: String) -> Unit
) {
    var nama by remember { mutableStateOf(initialDistributor?.nama ?: "") }
    var noHp by remember { mutableStateOf(initialDistributor?.noHp ?: "") }
    var alamat by remember { mutableStateOf(initialDistributor?.alamat ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialDistributor == null) "Tambah Distributor Baru" else "Edit Distributor",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                        color = DagangDebtRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                OutlinedTextField(
                    value = nama,
                    onValueChange = {
                        nama = it
                        errorMessage = null
                    },
                    label = { Text("Nama Distributor / Perusahaan *") },
                    placeholder = { Text("Contoh: PT Pangan Nusantara") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_distributor_nama")
                )

                OutlinedTextField(
                    value = noHp,
                    onValueChange = { noHp = it },
                    label = { Text("Nomor Telepon / Kontak") },
                    placeholder = { Text("Contoh: 081234567890") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_distributor_phone")
                )

                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat Gudang / Kantor") },
                    placeholder = { Text("Alamat lengkap distributor") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_distributor_address")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isBlank()) {
                        errorMessage = "Nama distributor wajib diisi"
                        return@Button
                    }
                    onSave(nama.trim(), noHp.trim(), alamat.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = DagangBluePrimary),
                modifier = Modifier.testTag("btn_save_distributor")
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
