package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentTargetInfo
import com.example.ui.theme.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatPembayaranBottomSheet(
    targetInfo: PaymentTargetInfo,
    onDismiss: () -> Unit,
    onSavePayment: (nominal: Long, metode: String, catatan: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isCustomerPayment = targetInfo.tipe == "CUSTOMER"
    val accentColor = if (isCustomerPayment) StatusGreenText else MaterialTheme.colorScheme.primary
    val accentContainer = if (isCustomerPayment) StatusGreenContainer else MaterialTheme.colorScheme.primaryContainer

    var nominalText by remember {
        mutableStateOf(targetInfo.remainingBalance.toString())
    }
    var metode by remember {
        mutableStateOf("Transfer Bank")
    }
    var catatan by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val methods = listOf("Tunai", "Transfer Bank", "QRIS", "Giro", "Lainnya")

    val inputNominal = nominalText.toLongOrNull() ?: 0L
    val isExceedingBalance = inputNominal > targetInfo.remainingBalance
    val sisaSetelahBayar = (targetInfo.remainingBalance - inputNominal).coerceAtLeast(0L)
    val isSaveEnabled = inputNominal > 0L && !isExceedingBalance

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = accentContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCustomerPayment) Icons.Default.Payments else Icons.Default.Payment,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isCustomerPayment) "Catat Pembayaran Masuk" else "Catat Pembayaran Keluar",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isCustomerPayment) "Penerimaan cicilan / pelunasan piutang customer" else "Pembayaran cicilan / pelunasan hutang PO distributor",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            // Info Card (SO/PO Ref & Balance)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isCustomerPayment) "Nomor SO / Pelanggan:" else "Nomor PO / Distributor:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "${targetInfo.refNumber} • ${targetInfo.partyName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Nilai Transaksi:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = Formatters.formatRupiah(targetInfo.totalTransaction),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sudah Dibayar Sebelumnya:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = Formatters.formatRupiah(targetInfo.alreadyPaid),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = StatusGreenText
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCustomerPayment) "Sisa Piutang Saat Ini:" else "Sisa Hutang Saat Ini:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = Formatters.formatRupiah(targetInfo.remainingBalance),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusRedText
                            )
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = StatusRedText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Quick Fill Buttons (Cicilan & Pelunasan)
            Text(
                text = "Pilih Cepat Nominal Pembayaran:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        nominalText = targetInfo.remainingBalance.toString()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text("Lunas (100%)", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        val half = Math.round(targetInfo.remainingBalance * 0.5)
                        nominalText = half.toString()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text("Cicil 50%", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        val quarter = Math.round(targetInfo.remainingBalance * 0.25)
                        nominalText = quarter.toString()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text("Cicil 25%", fontSize = 11.sp, maxLines = 1)
                }
            }

            // Nominal Input
            OutlinedTextField(
                value = nominalText,
                onValueChange = {
                    nominalText = it.filter { char -> char.isDigit() }
                    errorMessage = null
                },
                label = { Text("Nominal Pembayaran (Rp) *") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = isExceedingBalance,
                supportingText = {
                    if (isExceedingBalance) {
                        Text(
                            text = "Nominal melebihi sisa tagihan",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_sheet_nominal")
            )

            if (isExceedingBalance) {
                Surface(
                    color = StatusRedContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusRedText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nominal melebihi sisa tagihan",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusRedText
                            )
                        )
                    }
                }
            }

            // Balance after payment preview
            if (inputNominal > 0L && !isExceedingBalance) {
                Surface(
                    color = if (sisaSetelahBayar == 0L) StatusGreenContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (sisaSetelahBayar == 0L) "Status setelah bayar: LUNAS" else "Sisa tagihan tersisa:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (sisaSetelahBayar == 0L) StatusGreenText else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = Formatters.formatRupiah(sisaSetelahBayar),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (sisaSetelahBayar == 0L) StatusGreenText else StatusRedText
                            )
                        )
                    }
                }
            }

            // Payment Method Selector
            Text(
                text = "Metode Pembayaran:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(methods) { m ->
                    FilterChip(
                        selected = metode == m,
                        onClick = { metode = m },
                        label = { Text(m, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = catatan,
                onValueChange = { catatan = it },
                label = { Text("Catatan / Keterangan (Opsional)") },
                placeholder = { Text("Contoh: Pembayaran cicilan tahap 1 via BCA") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_sheet_catatan")
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        val nominal = nominalText.toLongOrNull() ?: 0L
                        if (nominal <= 0) {
                            errorMessage = "Nominal pembayaran harus lebih besar dari 0"
                            return@Button
                        }
                        if (nominal > targetInfo.remainingBalance) {
                            errorMessage = "Nominal melebihi sisa tagihan"
                            return@Button
                        }
                        onSavePayment(nominal, metode, catatan)
                    },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("btn_sheet_save_payment"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Pembayaran", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
