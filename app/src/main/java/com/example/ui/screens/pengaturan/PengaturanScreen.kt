package com.example.ui.screens.pengaturan

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DagangKuTopAppBar
import com.example.ui.theme.*
import com.example.util.BackupData
import com.example.util.BackupRestoreManager
import com.example.util.ExportBackupResult
import com.example.util.ExportManager
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saldoAwal by viewModel.saldoAwalKas.collectAsStateWithLifecycle()
    val isSampleDataEnabled by viewModel.sampleDataEnabled.collectAsStateWithLifecycle()
    val kasSummary by viewModel.kasSummary.collectAsStateWithLifecycle()

    var saldoAwalInput by remember(saldoAwal) {
        mutableStateOf(if (saldoAwal > 0) saldoAwal.toString() else "")
    }

    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var showLoadSampleConfirmDialog by remember { mutableStateOf(false) }
    var showToggleOffSampleConfirmDialog by remember { mutableStateOf(false) }

    // Backup & Restore states
    var isExporting by remember { mutableStateOf(false) }
    var exportResultDialogData by remember { mutableStateOf<ExportBackupResult?>(null) }
    var pendingRestoreData by remember { mutableStateOf<BackupData?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }

    // Storage Access Framework Launcher to save backup file to user chosen location
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && exportResultDialogData != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(exportResultDialogData!!.jsonString.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Berkas cadangan berhasil disimpan!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menyimpan berkas: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Storage Access Framework Launcher to select a JSON file to restore
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (jsonString.isNullOrBlank()) {
                    restoreErrorMessage = "Berkas cadangan kosong atau tidak dapat dibaca."
                } else {
                    val result = BackupRestoreManager.parseJsonToBackup(jsonString)
                    result.onSuccess { backup ->
                        pendingRestoreData = backup
                        showRestoreConfirmDialog = true
                    }.onFailure { err ->
                        restoreErrorMessage = err.localizedMessage ?: "Format file tidak valid."
                    }
                }
            } catch (e: Exception) {
                restoreErrorMessage = "Gagal membuka berkas: ${e.localizedMessage}"
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            DagangKuTopAppBar(
                title = "Pengaturan",
                onNavigateBack = onNavigateBack,
                windowInsets = WindowInsets(0)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. Saldo Awal Kas
            // -----------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Saldo Awal Kas",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Modal kas awal usaha sebelum ada pencatatan transaksi",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Text(
                        text = "Saldo Awal Saat Ini: ${Formatters.formatRupiah(saldoAwal)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    OutlinedTextField(
                        value = saldoAwalInput,
                        onValueChange = { saldoAwalInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nominal Saldo Awal (Rp)") },
                        prefix = { Text("Rp ") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_saldo_awal_kas")
                    )

                    Button(
                        onClick = {
                            val nominal = saldoAwalInput.toLongOrNull() ?: 0L
                            viewModel.setSaldoAwalKas(nominal)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_saldo_awal")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Saldo Awal Kas", fontWeight = FontWeight.Bold)
                    }

                    // Calculation breakdown preview
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Dampak ke Saldo Kas:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Saldo Awal:", style = MaterialTheme.typography.bodySmall)
                                Text(Formatters.formatRupiah(kasSummary.saldoAwal), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Uang Masuk:", style = MaterialTheme.typography.bodySmall)
                                Text("+${Formatters.formatRupiah(kasSummary.totalMasuk)}", style = MaterialTheme.typography.bodySmall.copy(color = StatusGreenText, fontWeight = FontWeight.SemiBold))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Uang Keluar:", style = MaterialTheme.typography.bodySmall)
                                Text("-${Formatters.formatRupiah(kasSummary.totalKeluar)}", style = MaterialTheme.typography.bodySmall.copy(color = StatusRedText, fontWeight = FontWeight.SemiBold))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Saldo Kas Akhir:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    Formatters.formatRupiah(kasSummary.saldoKas),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (kasSummary.saldoKas >= 0) StatusGreenText else StatusRedText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. Muat Data Contoh (Untuk demo/uji coba)
            // -----------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusPurpleContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = StatusPurpleText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Muat Data Contoh",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Surface(
                                    color = StatusPurpleContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Untuk demo/uji coba",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = StatusPurpleText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gunakan data simulasi usaha sembako untuk demonstrasi dan uji coba fitur aplikasi",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        Switch(
                            checked = isSampleDataEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showLoadSampleConfirmDialog = true
                                } else {
                                    showToggleOffSampleConfirmDialog = true
                                }
                            },
                            modifier = Modifier.testTag("switch_sample_data")
                        )
                    }

                    Text(
                        text = if (isSampleDataEnabled) {
                            "Status: Aktif — Database berisi data contoh sembako, pelanggan, dan transaksi (Untuk demo/uji coba)."
                        } else {
                            "Status: Tidak Aktif — Tidak menggunakan data contoh demonstrasi."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSampleDataEnabled) StatusPurpleText else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSampleDataEnabled) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }

            // -----------------------------------------------------------------
            // 3. Cadangkan & Pulihkan Database (Backup & Restore)
            // -----------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SettingsBackupRestore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cadangkan & Pulihkan",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Ekspor data ke file JSON atau pulihkan dari cadangan",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Text(
                        text = "Ekspor seluruh database usaha (inventaris produk, harga kesepakatan, pelanggan, distributor, transaksi PO & SO, mutasi kas, dan saldo awal) ke file format JSON untuk disimpan secara aman atau dibagikan.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    )

                    // Export Button
                    Button(
                        onClick = {
                            isExporting = true
                            viewModel.exportBackup(context) { result ->
                                isExporting = false
                                exportResultDialogData = result
                            }
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_backup")
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sedang Menyiapkan Cadangan...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cadangkan Data (Ekspor JSON)", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Import Button
                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(
                                arrayOf("application/json", "text/*", "*/*")
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_import_restore")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pulihkan Data (Impor JSON)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // -----------------------------------------------------------------
            // 4. Zona Bahaya: Hapus Semua Data
            // -----------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusRedContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusRedText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Zona Bahaya",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                            Text(
                                text = "Tindakan pembersihan database permanen",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Text(
                        text = "Menghapus seluruh produk inventaris, kontak pelanggan, distributor, transaksi pembelian (PO), transaksi penjualan (SO), catatan kas, dan pengeluaran.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    )

                    Button(
                        onClick = { showClearAllConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_hapus_semua_data")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus Semua Data", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // -------------------------------------------------------------------------
    // Dialog 1: Export Backup Success Dialog
    // -------------------------------------------------------------------------
    exportResultDialogData?.let { result ->
        AlertDialog(
            onDismissRequest = { exportResultDialogData = null },
            icon = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusGreenContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreenText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Cadangan Database Siap",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Seluruh database berhasil diekspor ke format JSON:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = result.file.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "• ${result.summary.produkCount} Produk Inventaris",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• ${result.summary.customerCount} Pelanggan & ${result.summary.distributorCount} Distributor",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• ${result.summary.poCount} Pembelian PO & ${result.summary.soCount} Penjualan SO",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• ${result.summary.pembayaranCount} Pembayaran & ${result.summary.pengeluaranCount} Pengeluaran",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Saldo Awal Kas: ${Formatters.formatRupiah(result.summary.saldoAwalKas)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Text(
                        text = "Pilih untuk membagikan berkas (via WhatsApp, Google Drive, Email) atau menyimpannya langsung ke penyimpanan perangkat.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ExportManager.shareFile(
                            context = context,
                            file = result.file,
                            mimeType = "application/json",
                            title = "Cadangan Database DagangKu"
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_share_backup")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            createDocumentLauncher.launch(result.file.name)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_save_backup_device")
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan File")
                    }
                    TextButton(onClick = { exportResultDialogData = null }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // Dialog 2: Restore Confirmation Dialog (Crucial Requirement)
    // -------------------------------------------------------------------------
    if (showRestoreConfirmDialog && pendingRestoreData != null) {
        val backup = pendingRestoreData!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreData = null
            },
            icon = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Konfirmasi Pulihkan Data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "File cadangan valid ditemukan. Rincian data yang akan dimuat:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Waktu Cadangan: ${Formatters.formatTanggalWaktu(backup.metadata.exportedAt)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "• ${backup.produkList.size} Produk Inventaris",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• ${backup.customerList.size} Pelanggan & ${backup.distributorList.size} Distributor",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• ${backup.poList.size} Pembelian PO & ${backup.soList.size} Penjualan SO",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• ${backup.pembayaranList.size} Pembayaran & ${backup.pengeluaranList.size} Pengeluaran",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Saldo Awal: ${Formatters.formatRupiah(backup.metadata.saldoAwalKas)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PERINGATAN: Memulihkan cadangan akan MENGGANTIKAN seluruh data usaha saat ini di aplikasi. Data yang tidak ada di file cadangan akan terhapus.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = "Apakah Anda yakin ingin memulihkan dan mengganti seluruh data sekarang?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(
                            backupData = backup,
                            onSuccess = {
                                showRestoreConfirmDialog = false
                                pendingRestoreData = null
                            },
                            onError = { err ->
                                showRestoreConfirmDialog = false
                                pendingRestoreData = null
                                restoreErrorMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_restore")
                ) {
                    Text("Pulihkan Sekarang", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreData = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_cancel_restore")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // Dialog 3: Restore Error Dialog
    // -------------------------------------------------------------------------
    restoreErrorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { restoreErrorMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Gagal Memulihkan Data",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { restoreErrorMessage = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // Confirm Clear All Dialog
    if (showClearAllConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Hapus Semua Data Usaha?",
            message = "PERINGATAN: Tindakan ini akan menghapus seluruh data produk, transaksi pembelian (PO), penjualan (SO), pelanggan, pemasok, mutasi kas, dan pengeluaran secara permanen. Data yang telah dihapus tidak dapat dipulihkan.",
            onConfirm = {
                viewModel.clearAllData()
                showClearAllConfirmDialog = false
            },
            onDismiss = { showClearAllConfirmDialog = false }
        )
    }

    // Confirm Turn Off Sample Data Dialog
    if (showToggleOffSampleConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Bersihkan Data Contoh?",
            message = "Apakah Anda ingin menghapus data contoh demonstrasi dari aplikasi?",
            onConfirm = {
                viewModel.toggleSampleData(false)
                showToggleOffSampleConfirmDialog = false
            },
            onDismiss = { showToggleOffSampleConfirmDialog = false }
        )
    }

    // Confirm Load Sample Data Dialog (Untuk demo/uji coba)
    if (showLoadSampleConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLoadSampleConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = StatusPurpleText
                )
            },
            title = {
                Text(
                    text = "Muat Data Contoh?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Data contoh ditujukan untuk demo/uji coba fitur aplikasi (produk sembako, pelanggan, supplier, dan transaksi simulasi). Apakah Anda yakin ingin memuat data contoh ini?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleSampleData(true)
                        showLoadSampleConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Muat Data Contoh")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLoadSampleConfirmDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
