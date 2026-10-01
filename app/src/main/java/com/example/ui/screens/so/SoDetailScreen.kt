package com.example.ui.screens.so

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentTargetInfo
import com.example.ui.MainViewModel
import com.example.ui.components.CatatPembayaranBottomSheet
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun SoDetailScreen(
    soId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val soDetailFlow = remember(soId) { viewModel.getSoDetailsFlow(soId) }
    val soDetail by soDetailFlow.collectAsStateWithLifecycle(initialValue = null)

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DagangKuTopAppBar(
                title = soDetail?.so?.nomor ?: "Detail SO",
                subtitle = "Penjualan ke ${soDetail?.customer?.nama ?: "Pelanggan"}",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("btn_delete_so")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus SO", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        bottomBar = {
            soDetail?.let { detail ->
                if (detail.sisaPiutang > 0) {
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
                            Column {
                                Text("Sisa Piutang Pelanggan:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(
                                    Formatters.formatRupiah(detail.sisaPiutang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = StatusRedText)
                                )
                            }

                            Button(
                                onClick = { showPaymentDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_terima_pembayaran_so")
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Terima Bayar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (soDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val detail = soDetail!!

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
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = detail.so.nomor,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                StatusBadge(status = detail.status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tanggal: ${Formatters.formatTanggal(detail.so.tanggal)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)

                            Text(
                                text = "Pelanggan: ${detail.customer?.nama ?: "-"}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (detail.customer?.noHp?.isNotBlank() == true) {
                                Text(
                                    text = "Kontak: ${detail.customer.noHp}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            if (detail.customer?.alamat?.isNotBlank() == true) {
                                Text(
                                    text = "Alamat: ${detail.customer.alamat}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Ringkasan Keuangan SO",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Nilai SO:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Formatters.formatRupiah(detail.so.total),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Terbayar:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Formatters.formatRupiah(detail.totalPaid),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = StatusGreenText, fontWeight = FontWeight.Bold)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sisa Piutang:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Formatters.formatRupiah(detail.sisaPiutang),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (detail.sisaPiutang > 0) StatusRedText else StatusGreenText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            if (detail.komisiNominal > 0 || (detail.customer?.persenKomisi ?: 0.0) > 0.0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "Komisi (dari pembayaran masuk):",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = StatusPurpleText)
                                    )
                                    Text(
                                        Formatters.formatRupiah(detail.komisiNominal),
                                        style = MaterialTheme.typography.bodyMedium.copy(color = StatusPurpleText, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                // Purchased Items Section
                item {
                    Text(
                        text = "Item Barang Keluar (${detail.items.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                items(detail.items) { itemSo ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemSo.produk?.nama ?: "Produk #${itemSo.item.produkId}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "${itemSo.item.qty} ${itemSo.produk?.satuan ?: "unit"} × ${Formatters.formatRupiah(itemSo.item.harga)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Text(
                                text = Formatters.formatRupiah(itemSo.subtotal),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
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
                            text = "Riwayat Pembayaran Customer (${detail.payments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (detail.sisaPiutang > 0) {
                            TextButton(onClick = { showPaymentDialog = true }) {
                                Text("+ Terima Bayar", color = MaterialTheme.colorScheme.primary)
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
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = "Belum ada pembayaran yang dicatat untuk SO ini.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
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
                                            color = StatusGreenText
                                        )
                                    )
                                    Text(
                                        text = "${Formatters.formatTanggalWaktu(payment.tanggal)} • ${payment.metode}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    if (payment.catatan.isNotBlank()) {
                                        Text(
                                            text = "Catatan: ${payment.catatan}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        tipe = "CUSTOMER",
                        refId = soId,
                        refNumber = detail.so.nomor,
                        partyName = detail.customer?.nama ?: "Customer Umum",
                        totalTransaction = detail.so.total,
                        alreadyPaid = detail.totalPaid,
                        remainingBalance = detail.sisaPiutang
                    ),
                    onDismiss = { showPaymentDialog = false },
                    onSavePayment = { nominal, metode, catatan ->
                        viewModel.recordPayment(
                            tipe = "CUSTOMER",
                            refId = soId,
                            nominal = nominal,
                            metode = metode,
                            catatan = catatan,
                            onSuccess = { showPaymentDialog = false }
                        )
                    }
                )
            }

            // Delete SO Confirmation Dialog
            if (showDeleteConfirmDialog) {
                ConfirmDeleteDialog(
                    title = "Hapus Dokumen SO?",
                    message = "Apakah Anda yakin ingin menghapus SO '${detail.so.nomor}'? Riwayat pembayaran terkait dokumen ini juga akan dihapus.",
                    onConfirm = {
                        viewModel.deleteSO(soId, onSuccess = onNavigateBack)
                        showDeleteConfirmDialog = false
                    },
                    onDismiss = { showDeleteConfirmDialog = false }
                )
            }
        }
    }
}
