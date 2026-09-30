package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.LaporanKeuanganData
import com.example.data.model.SoWithDetails
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

object ExportManager {

    /**
     * Exports financial report to CSV (Excel compatible with UTF-8 BOM).
     */
    fun exportToCsv(
        context: Context,
        laporan: LaporanKeuanganData,
        soList: List<SoWithDetails>
    ): File {
        val fileName = "Laporan_DagangKu_${System.currentTimeMillis()}.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileWriter(file).use { writer ->
            // Write UTF-8 BOM so Microsoft Excel recognizes Indonesian Rupiah characters properly
            writer.write("\uFEFF")

            // Title
            writer.write("LAPORAN KEUANGAN & LABA RUGI - DAGANGKU\n")
            writer.write("Periode Laporan;${laporan.periodLabel}\n")
            writer.write("Tanggal Cetak;${Formatters.formatTanggalWaktu(System.currentTimeMillis())}\n")
            writer.write("\n")

            // Summary P&L
            writer.write("RINGKASAN LABA / RUGI (P&L)\n")
            writer.write("Komponen;Nominal (Rp);Keterangan\n")
            writer.write("Omset Penjualan (Total SO);${laporan.omset.toLong()};${laporan.soCount} Pesanan SO\n")
            writer.write("HPP (Harga Pokok Penjualan);${laporan.hpp.toLong()};Total biaya pokok barang terjual\n")
            writer.write("Laba Kotor (Gross Profit);${laporan.labaKotor.toLong()};Margin: ${Formatters.formatPersen(laporan.marginLabaKotorPersen)}\n")
            writer.write("Biaya Operasional Usaha;${laporan.pengeluaranOperasional.toLong()};Beban listrik, sewa, gaji, transport\n")
            writer.write("Total Komisi Agen / Pelanggan;${laporan.totalKomisi.toLong()};Dihitung dari SO yang sudah terbayar\n")
            writer.write("LABA BERSIH (NET PROFIT);${laporan.labaBersih.toLong()};Margin Bersih: ${Formatters.formatPersen(laporan.marginLabaBersihPersen)}\n")
            writer.write("\n")

            // Customer Commissions
            writer.write("RINCIAN KOMISI PELANGGAN / AGEN\n")
            writer.write("Nama Pelanggan;Tarif Komisi (%);Total Pelunasan SO Masuk (Rp);Hak Komisi Diterima (Rp)\n")
            if (laporan.commissionList.isEmpty()) {
                writer.write("Tidak ada komisi terealisasi pada periode ini;;;\n")
            } else {
                for (comm in laporan.commissionList) {
                    writer.write("${comm.customer.nama};${comm.persenKomisi}%;${comm.totalSoTerbayar.toLong()};${comm.totalKomisi.toLong()}\n")
                }
            }
            writer.write("\n")

            // Transactions breakdown in period
            val soInPeriod = soList.filter { it.so.tanggal in laporan.startDate..laporan.endDate }
            writer.write("DAFTAR PESANAN PENJUALAN (SO) DALAM PERIODE\n")
            writer.write("Nomor SO;Tanggal;Pelanggan;Total Penjualan (Rp);Total Terbayar (Rp);Sisa Piutang (Rp);Status\n")
            for (item in soInPeriod) {
                writer.write("${item.so.nomor};${Formatters.formatTanggal(item.so.tanggal)};${item.customer?.nama ?: "Umum"};${item.so.total.toLong()};${item.totalPaid.toLong()};${item.sisaPiutang.toLong()};${item.status.label}\n")
            }
        }

        return file
    }

    /**
     * Exports financial report to PDF using native Android PdfDocument.
     */
    fun exportToPdf(
        context: Context,
        laporan: LaporanKeuanganData,
        soList: List<SoWithDetails>
    ): File {
        val fileName = "Laporan_DagangKu_${System.currentTimeMillis()}.pdf"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        val pageWidth = 595 // Standard A4 points (72 dpi)
        val pageHeight = 842

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Header Background Banner
        paint.color = AndroidColor.parseColor("#1E4FA3")
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // Header Text
        paint.color = AndroidColor.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText("DAGANGKU", 28f, 42f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Laporan Keuangan & Laba Rugi (P&L)", 28f, 62f, paint)

        paint.textSize = 9f
        canvas.drawText("Periode: ${laporan.periodLabel} • Dicetak: ${Formatters.formatTanggalWaktu(System.currentTimeMillis())}", 28f, 80f, paint)

        var y = 125f

        // Section 1: Ringkasan Finansial P&L
        paint.color = AndroidColor.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("1. RINGKASAN LABA / RUGI (P&L)", 28f, y, paint)
        y += 18f

        // Table header box
        paint.color = AndroidColor.parseColor("#F1F5F9")
        canvas.drawRoundRect(28f, y, pageWidth - 28f, y + 24f, 6f, 6f, paint)

        paint.color = AndroidColor.parseColor("#475569")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Komponen Keuangan", 38f, y + 16f, paint)
        canvas.drawText("Nominal (Rupiah)", pageWidth - 160f, y + 16f, paint)
        y += 32f

        // Table rows
        val pnlRows = listOf(
            Triple("Omset Penjualan (Total SO)", Formatters.formatRupiah(laporan.omset), AndroidColor.parseColor("#1E4FA3")),
            Triple("Harga Pokok Penjualan (HPP)", "-${Formatters.formatRupiah(laporan.hpp)}", AndroidColor.parseColor("#475569")),
            Triple("Laba Kotor (Gross Profit)", Formatters.formatRupiah(laporan.labaKotor), if (laporan.labaKotor >= 0) AndroidColor.parseColor("#16A34A") else AndroidColor.parseColor("#DC2626")),
            Triple("Beban Operasional Usaha", "-${Formatters.formatRupiah(laporan.pengeluaranOperasional)}", AndroidColor.parseColor("#DC2626")),
            Triple("Total Komisi Agen / Pelanggan", "-${Formatters.formatRupiah(laporan.totalKomisi)}", AndroidColor.parseColor("#7E22CE"))
        )

        paint.textSize = 10f
        for ((label, value, color) in pnlRows) {
            paint.color = AndroidColor.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(label, 38f, y, paint)

            paint.color = color
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(value, pageWidth - 160f, y, paint)

            paint.color = AndroidColor.parseColor("#E2E8F0")
            canvas.drawLine(28f, y + 6f, pageWidth - 28f, y + 6f, paint)
            y += 22f
        }

        // Net Profit Box
        y += 6f
        val isNetProfitPos = laporan.labaBersih >= 0
        paint.color = if (isNetProfitPos) AndroidColor.parseColor("#DCFCE7") else AndroidColor.parseColor("#FEE2E2")
        canvas.drawRoundRect(28f, y, pageWidth - 28f, y + 36f, 8f, 8f, paint)

        paint.color = if (isNetProfitPos) AndroidColor.parseColor("#14532D") else AndroidColor.parseColor("#7F1D1D")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("LABA BERSIH (NET PROFIT):", 38f, y + 23f, paint)

        paint.textSize = 14f
        canvas.drawText(Formatters.formatRupiah(laporan.labaBersih), pageWidth - 160f, y + 24f, paint)

        y += 58f

        // Section 2: Rincian Komisi Pelanggan
        paint.color = AndroidColor.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("2. RINCIAN KOMISI PELANGGAN / AGEN", 28f, y, paint)
        y += 18f

        paint.color = AndroidColor.parseColor("#F1F5F9")
        canvas.drawRoundRect(28f, y, pageWidth - 28f, y + 24f, 6f, 6f, paint)

        paint.color = AndroidColor.parseColor("#475569")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Nama Pelanggan", 38f, y + 16f, paint)
        canvas.drawText("Tarif", 240f, y + 16f, paint)
        canvas.drawText("SO Terbayar", 320f, y + 16f, paint)
        canvas.drawText("Hak Komisi", pageWidth - 110f, y + 16f, paint)
        y += 30f

        paint.textSize = 9.5f
        if (laporan.commissionList.isEmpty()) {
            paint.color = AndroidColor.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Tidak ada komisi yang terealisasi pada periode ini.", 38f, y, paint)
            y += 20f
        } else {
            for (comm in laporan.commissionList.take(6)) {
                paint.color = AndroidColor.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(comm.customer.nama, 38f, y, paint)
                canvas.drawText(Formatters.formatPersen(comm.persenKomisi), 240f, y, paint)
                canvas.drawText(Formatters.formatRupiah(comm.totalSoTerbayar), 320f, y, paint)

                paint.color = AndroidColor.parseColor("#7E22CE")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(Formatters.formatRupiah(comm.totalKomisi), pageWidth - 110f, y, paint)

                paint.color = AndroidColor.parseColor("#E2E8F0")
                canvas.drawLine(28f, y + 5f, pageWidth - 28f, y + 5f, paint)
                y += 20f
            }
        }

        y += 25f

        // Section 3: Ringkasan Penjualan SO dalam Periode
        val soInPeriod = soList.filter { it.so.tanggal in laporan.startDate..laporan.endDate }
        paint.color = AndroidColor.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("3. TRANSAKSI PENJUALAN DALAM PERIODE (${soInPeriod.size} SO)", 28f, y, paint)
        y += 18f

        paint.color = AndroidColor.parseColor("#F1F5F9")
        canvas.drawRoundRect(28f, y, pageWidth - 28f, y + 24f, 6f, 6f, paint)

        paint.color = AndroidColor.parseColor("#475569")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Nomor SO", 38f, y + 16f, paint)
        canvas.drawText("Pelanggan", 160f, y + 16f, paint)
        canvas.drawText("Nilai SO", 320f, y + 16f, paint)
        canvas.drawText("Status", pageWidth - 90f, y + 16f, paint)
        y += 30f

        paint.textSize = 9.5f
        for (item in soInPeriod.take(8)) {
            paint.color = AndroidColor.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(item.so.nomor, 38f, y, paint)
            canvas.drawText(item.customer?.nama ?: "Umum", 160f, y, paint)
            canvas.drawText(Formatters.formatRupiah(item.so.total), 320f, y, paint)

            paint.color = when (item.status) {
                com.example.data.model.StatusPembayaran.LUNAS -> AndroidColor.parseColor("#16A34A")
                com.example.data.model.StatusPembayaran.SEBAGIAN -> AndroidColor.parseColor("#D97706")
                com.example.data.model.StatusPembayaran.BELUM_LUNAS -> AndroidColor.parseColor("#DC2626")
            }
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.status.label, pageWidth - 90f, y, paint)

            paint.color = AndroidColor.parseColor("#E2E8F0")
            canvas.drawLine(28f, y + 5f, pageWidth - 28f, y + 5f, paint)
            y += 20f
        }

        // Footer
        paint.color = AndroidColor.parseColor("#94A3B8")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Dokumen ini dihasilkan secara otomatis oleh DagangKu Business Management App", 28f, pageHeight - 25f, paint)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    /**
     * Shares a generated export file via Android Intent.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            android.util.Log.e("ExportManager", "Failed to share file: ${e.message}", e)
            false
        }
    }
}
