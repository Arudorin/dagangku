package com.example

import com.example.data.model.KasSummary
import com.example.data.model.LaporanKeuanganData
import com.example.util.Formatters
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testFinancialFormulas() {
        val omset = 10_000_000.0 // 10 million
        val hpp = 6_500_000.0   // 6.5 million
        val labaKotor = omset - hpp
        assertEquals(3_500_000.0, labaKotor, 0.001)

        val totalSoTerbayar = 8_000_000.0
        val persenKomisi = 5.0 // 5%
        val komisi = (totalSoTerbayar * persenKomisi) / 100.0
        assertEquals(400_000.0, komisi, 0.001)

        val pengeluaranOperasional = 1_100_000.0
        val labaBersih = labaKotor - pengeluaranOperasional - komisi
        assertEquals(2_000_000.0, labaBersih, 0.001)

        val laporan = LaporanKeuanganData(
            omset = omset,
            hpp = hpp,
            labaKotor = labaKotor,
            pengeluaranOperasional = pengeluaranOperasional,
            totalKomisi = komisi,
            labaBersih = labaBersih
        )

        assertEquals(35.0, laporan.marginLabaKotorPersen, 0.001)
        assertEquals(20.0, laporan.marginLabaBersihPersen, 0.001)
    }

    @Test
    fun testKasSummaryCalculations() {
        val kas = KasSummary(
            totalMasuk = 5_000_000.0,
            totalKeluarDistributor = 3_000_000.0,
            totalKeluarOperasional = 500_000.0
        )

        assertEquals(3_500_000.0, kas.totalKeluar, 0.001)
        assertEquals(1_500_000.0, kas.saldoKas, 0.001)
    }

    @Test
    fun testFormatters() {
        val rupiah = Formatters.formatRupiah(1_250_000.0)
        assertTrue(rupiah.contains("1.250.000"))
        assertTrue(rupiah.startsWith("Rp"))

        val persen = Formatters.formatPersen(12.5)
        assertEquals("12,5%", persen)
    }
}

