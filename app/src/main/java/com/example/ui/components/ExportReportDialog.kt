package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LaporanKeuanganData
import com.example.data.model.SoWithDetails
import com.example.ui.theme.*
import com.example.util.ExportManager

@Composable
fun ExportReportDialog(
    laporan: LaporanKeuanganData,
    soList: List<SoWithDetails>,
    onDismiss: () -> Unit,
    onSuccessMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DagangBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = DagangBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Ekspor Laporan Finansial",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = laporan.periodLabel.ifBlank { "Laporan Usaha DagangKu" },
                        style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary, fontSize = 11.sp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih format dokumen laporan untuk diunduh atau dibagikan ke WhatsApp, Email, atau Google Drive:",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                )

                // Option 1: PDF Format
                ExportFormatOption(
                    title = "Dokumen PDF (Format A4 Cetak)",
                    subtitle = "Rangkuman rapi Laba/Rugi, margin, rincian komisi, dan daftar transaksi.",
                    badge = "Rekomendasi",
                    icon = Icons.Default.PictureAsPdf,
                    accentColor = Color(0xFFDC2626),
                    bgColor = Color(0xFFFEE2E2),
                    enabled = !isExporting,
                    testTag = "export_option_pdf",
                    onClick = {
                        isExporting = true
                        try {
                            val pdfFile = ExportManager.exportToPdf(context, laporan, soList)
                            val shared = ExportManager.shareFile(
                                context = context,
                                file = pdfFile,
                                mimeType = "application/pdf",
                                title = "Bagikan Laporan DagangKu (PDF)"
                            )
                            val msg = "Laporan PDF berhasil dibuat (${pdfFile.name})"
                            onSuccessMessage(msg)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isExporting = false
                        }
                    }
                )

                // Option 2: Excel / CSV Format
                ExportFormatOption(
                    title = "Spreadsheet Excel / CSV",
                    subtitle = "Format data tabular (UTF-8 BOM) siap dibuka & dianalisis di Microsoft Excel.",
                    badge = "Excel Ready",
                    icon = Icons.Default.TableChart,
                    accentColor = Color(0xFF16A34A),
                    bgColor = Color(0xFFDCFCE7),
                    enabled = !isExporting,
                    testTag = "export_option_csv",
                    onClick = {
                        isExporting = true
                        try {
                            val csvFile = ExportManager.exportToCsv(context, laporan, soList)
                            val shared = ExportManager.shareFile(
                                context = context,
                                file = csvFile,
                                mimeType = "text/csv",
                                title = "Bagikan Laporan DagangKu (Excel/CSV)"
                            )
                            val msg = "Laporan CSV berhasil dibuat (${csvFile.name})"
                            onSuccessMessage(msg)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuat CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isExporting = false
                        }
                    }
                )

                if (isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Membuat berkas dokumen...",
                            style = MaterialTheme.typography.bodySmall.copy(color = NeutralTextSecondary)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("Tutup")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ExportFormatOption(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(enabled = enabled) { onClick() }
            .border(1.dp, NeutralOutline, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeutralTextPrimary
                    )
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NeutralTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeutralTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
