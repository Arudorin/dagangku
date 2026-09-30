package com.example.ui.screens.po

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentTargetInfo
import com.example.data.model.Pembayaran
import com.example.ui.MainViewModel
import com.example.ui.components.CatatPembayaranBottomSheet
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun PoDetailScreen(
    poId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val poDetailFlow = remember(poId) { viewModel.getPoDetailsFlow(poId) }
    val poDetail by poDetailFlow.collectAsStateWithLifecycle(initialValue = null)

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = poDetail?.po?.nomor ?: "Detail PO",
                subtitle = "Pembelian ke ${poDetail?.distributor?.nama ?: "Distributor"}",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("btn_delete_po")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus PO", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            poDetail?.let { detail ->
                if (detail.sisaHutang > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier.border(1.dp, NeutralOutline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sisa Hutang Belum Dibayar:", style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary))
                                Text(
                                    Formatters.formatRupiah(detail.sisaHutang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DagangDebtRed)
                                )
                            }

                            Button(
                                onClick = { showPaymentDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DagangBluePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_bayar_po")
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bayar Hutang", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (poDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val detail = poDetail!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Status Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeutralOutline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = detail.po.nomor,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                StatusBadge(status = detail.status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tanggal: ${Formatters.formatTanggal(detail.po.tanggal)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NeutralOutline)

                            Text(
                                text = "Distributor: ${detail.distributor?.nama ?: "-"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (detail.distributor?.noHp?.isNotBlank() == true) {
                                Text(
                                    text = "Kontak: ${detail.distributor.noHp}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                                )
                            }
                            if (detail.distributor?.alamat?.isNotBlank() == true) {
                                Text(
                                    text = "Alamat: ${detail.distributor.alamat}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                                )
                            }
                        }
                    }
                }

                // Financial Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeutralOutline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ringkasan Keuangan PO", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Nilai PO:", style = MaterialTheme.typography.bodyMedium)
                                Text(Formatters.formatRupiah(detail.po.total), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Terbayar:", style = MaterialTheme.typography.bodyMedium)
                                Text(Formatters.formatRupiah(detail.totalPaid), style = MaterialTheme.typography.bodyMedium.copy(color = DagangIncomeGreen, fontWeight = FontWeight.Bold))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sisa Hutang:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Formatters.formatRupiah(detail.sisaHutang),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (detail.sisaHutang > 0) DagangDebtRed else DagangIncomeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // Purchased Items Section
                item {
                    Text(
                        text = "Item Barang Masuk (${detail.items.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(detail.items) { itemPo ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeutralOutline, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemPo.produk?.nama ?: "Produk #${itemPo.item.produkId}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${itemPo.item.qty} ${itemPo.produk?.satuan ?: "unit"} × ${Formatters.formatRupiah(itemPo.item.hargaBeli)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                                )
                            }
                            Text(
                                text = Formatters.formatRupiah(itemPo.subtotal),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DagangBluePrimary
                                )
                            )
                        }
                    }
                }

                // Payment History Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Pembayaran Hutang (${detail.payments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (detail.sisaHutang > 0) {
                            TextButton(onClick = { showPaymentDialog = true }) {
                                Text("+ Tambah Bayar")
                            }
                        }
                    }
                }

                if (detail.payments.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NeutralOutline, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = "Belum ada pembayaran yang dicatat untuk PO ini.",
                                style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(detail.payments) { payment ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NeutralOutline, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Formatters.formatRupiah(payment.nominal),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DagangIncomeGreen
                                        )
                                    )
                                    Text(
                                        text = "${Formatters.formatTanggalWaktu(payment.tanggal)} • ${payment.metode}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                                    )
                                    if (payment.catatan.isNotBlank()) {
                                        Text(
                                            text = "Catatan: ${payment.catatan}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deletePayment(payment) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Hapus Pembayaran",
                                        tint = NeutralTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }

            // Catat Pembayaran Bottom Sheet
            if (showPaymentDialog) {
                CatatPembayaranBottomSheet(
                    targetInfo = PaymentTargetInfo(
                        tipe = "DISTRIBUTOR",
                        refId = poId,
                        refNumber = detail.po.nomor,
                        partyName = detail.distributor?.nama ?: "Distributor",
                        totalTransaction = detail.po.total,
                        alreadyPaid = detail.totalPaid,
                        remainingBalance = detail.sisaHutang
                    ),
                    onDismiss = { showPaymentDialog = false },
                    onSavePayment = { nominal, metode, catatan ->
                        viewModel.recordPayment(
                            tipe = "DISTRIBUTOR",
                            refId = poId,
                            nominal = nominal,
                            metode = metode,
                            catatan = catatan,
                            onSuccess = { showPaymentDialog = false }
                        )
                    }
                )
            }

            // Delete PO Confirmation Dialog
            if (showDeleteConfirmDialog) {
                ConfirmDeleteDialog(
                    title = "Hapus Dokumen PO?",
                    message = "Apakah Anda yakin ingin menghapus PO '${detail.po.nomor}'? Riwayat pembayaran terkait dokumen ini juga akan dihapus.",
                    onConfirm = {
                        viewModel.deletePO(poId, onSuccess = onNavigateBack)
                        showDeleteConfirmDialog = false
                    },
                    onDismiss = { showDeleteConfirmDialog = false }
                )
            }
        }
    }
}

