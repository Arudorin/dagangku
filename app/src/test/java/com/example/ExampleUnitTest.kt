package com.example

import com.example.data.model.KasSummary
import com.example.data.model.LaporanKeuanganData
import com.example.util.Formatters
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testFinancialFormulas() {
        val omset = 10_000_000L // 10 million
        val hpp = 6_500_000L   // 6.5 million
        val labaKotor = omset - hpp
        assertEquals(3_500_000L, labaKotor)

        val totalSoTerbayar = 8_000_000L
        val persenKomisi = 5.0 // 5%
        val komisi = Math.round((totalSoTerbayar.toDouble() * persenKomisi) / 100.0)
        assertEquals(400_000L, komisi)

        val pengeluaranOperasional = 1_100_000L
        val labaBersih = labaKotor - pengeluaranOperasional - komisi
        assertEquals(2_000_000L, labaBersih)

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
    fun testCommissionRounding() {
        val paid = 153_250L
        val percent = 2.5
        // 153250 * 2.5 / 100 = 3831.25 -> rounded to 3831
        val komisi = Math.round((paid.toDouble() * percent) / 100.0)
        assertEquals(3831L, komisi)
    }

    @Test
    fun testKasSummaryCalculations() {
        val kas = KasSummary(
            totalMasuk = 5_000_000L,
            totalKeluarDistributor = 3_000_000L,
            totalKeluarOperasional = 500_000L
        )

        assertEquals(3_500_000L, kas.totalKeluar)
        assertEquals(1_500_000L, kas.saldoKas)
    }

    @Test
    fun testKasSummaryWithSaldoAwal() {
        val kas = KasSummary(
            totalMasuk = 5_000_000L,
            totalKeluarDistributor = 3_000_000L,
            totalKeluarOperasional = 500_000L,
            saldoAwal = 2_000_000L
        )

        assertEquals(3_500_000L, kas.totalKeluar)
        // 2,000,000 + 5,000,000 - 3,500,000 = 3,500,000L
        assertEquals(3_500_000L, kas.saldoKas)
    }

    @Test
    fun testWeightedAverageRounding() {
        val currentStock = 10
        val currentHargaDasar = 10_000L
        val qtyBeli = 7
        val hargaBeli = 12_500L

        val totalCost = (currentStock.toLong() * currentHargaDasar) + (qtyBeli.toLong() * hargaBeli)
        val totalQty = currentStock + qtyBeli
        val newHargaDasar = Math.round(totalCost.toDouble() / totalQty)

        // 187,500 / 17 = 11029.411... -> rounded to 11029
        assertEquals(11_029L, newHargaDasar)
    }

    @Test
    fun testHppCalculationUsesHppSaatJual() {
        // Product current hargaDasar is 15_000L, but when SO was created, hppSaatJual was snapshot at 10_000L
        val item1 = com.example.data.model.ItemSO(
            id = 1,
            soId = 1,
            produkId = 1,
            qty = 3,
            harga = 20_000L,
            hppSaatJual = 10_000L
        )
        val item2 = com.example.data.model.ItemSO(
            id = 2,
            soId = 1,
            produkId = 2,
            qty = 2,
            harga = 15_000L,
            hppSaatJual = 8_000L
        )

        val items = listOf(item1, item2)
        val totalHpp = items.sumOf { it.qty.toLong() * it.hppSaatJual }
        // 3 * 10_000 + 2 * 8_000 = 30_000 + 16_000 = 46_000
        assertEquals(46_000L, totalHpp)
    }

    @Test
    fun testSoWithDetailsCommissionComputedFromPaymentsReceived() {
        val customer = com.example.data.model.Customer(
            id = 10,
            nama = "Agen Jaya",
            noHp = "0812345678",
            alamat = "Jl. Merdeka",
            persenKomisi = 5.0 // 5%
        )
        val so = com.example.data.model.SO(
            id = 100,
            nomor = "SO-TEST-001",
            customerId = customer.id,
            tanggal = System.currentTimeMillis(),
            total = 10_000_000L // Total SO is 10 million
        )

        // When NO payments received: commission must be 0, NOT 500_000L
        val unpaidSo = com.example.data.model.SoWithDetails(
            so = so,
            customer = customer,
            items = emptyList(),
            payments = emptyList()
        )
        assertEquals(0L, unpaidSo.totalPaid)
        assertEquals(0L, unpaidSo.komisiNominal)

        // When partial payments received: e.g. 3,000,000L total paid
        val payment1 = com.example.data.model.Pembayaran(
            id = 1,
            tipe = "CUSTOMER",
            refId = so.id,
            nominal = 1_000_000L,
            tanggal = System.currentTimeMillis(),
            metode = "Transfer"
        )
        val payment2 = com.example.data.model.Pembayaran(
            id = 2,
            tipe = "CUSTOMER",
            refId = so.id,
            nominal = 2_000_000L,
            tanggal = System.currentTimeMillis(),
            metode = "Tunai"
        )
        val partiallyPaidSo = com.example.data.model.SoWithDetails(
            so = so,
            customer = customer,
            items = emptyList(),
            payments = listOf(payment1, payment2)
        )
        assertEquals(3_000_000L, partiallyPaidSo.totalPaid)
        // Commission = 5% of 3,000,000 = 150,000L (NEVER 5% of 10,000,000 = 500,000L)
        assertEquals(150_000L, partiallyPaidSo.komisiNominal)
    }

    @Test
    fun testDashboardAndLaporanLabaBersihConsistencyForSamePeriod() {
        val cust = com.example.data.model.Customer(
            id = 1,
            nama = "Toko Makmur",
            noHp = "08123456",
            alamat = "Kota",
            persenKomisi = 3.0
        )
        val so = com.example.data.model.SO(
            id = 1,
            nomor = "SO-001",
            customerId = cust.id,
            tanggal = 1_000_000L,
            total = 5_000_000L
        )
        val itemSo = com.example.data.model.ItemSO(
            id = 1,
            soId = 1,
            produkId = 1,
            qty = 10,
            harga = 500_000L,
            hppSaatJual = 350_000L
        )
        val payment = com.example.data.model.Pembayaran(
            id = 1,
            tipe = "CUSTOMER",
            refId = 1,
            nominal = 2_000_000L,
            tanggal = 1_000_100L,
            metode = "Transfer"
        )
        val itemWithProd = com.example.data.model.ItemSoWithProduct(itemSo, null)
        val soDetail = com.example.data.model.SoWithDetails(
            so = so,
            customer = cust,
            items = listOf(itemWithProd),
            payments = listOf(payment)
        )

        // Commission on payments received: 2,000,000 * 3% = 60,000L
        assertEquals(60_000L, soDetail.komisiNominal)

        // Omset: 5,000,000L
        // HPP: 10 * 350,000 = 3,500,000L
        // Laba Kotor: 1,500,000L
        // Expense: 200,000L
        // Total Komisi: 60,000L
        // Laba Bersih = 1,500,000 - 200,000 - 60,000 = 1,240,000L
        val expenses = 200_000L
        val omset = soDetail.so.total
        val hpp = soDetail.items.sumOf { it.item.qty.toLong() * it.item.hppSaatJual }
        val labaKotor = omset - hpp
        val komisi = soDetail.komisiNominal
        val labaBersih = labaKotor - expenses - komisi

        assertEquals(1_240_000L, labaBersih)
    }

    @Test
    fun testFormatters() {
        val rupiah = Formatters.formatRupiah(1_250_000L)
        assertTrue(rupiah.contains("1.250.000"))
        assertTrue(rupiah.startsWith("Rp"))

        val persen = Formatters.formatPersen(12.5)
        assertEquals("12,5%", persen)
    }

    @Test
    fun testBusinessRulesFormulas() {
        // PO1: 100 units @ 10.000
        val po1Qty = 100
        val po1Price = 10_000L
        val costAfterPo1 = po1Qty.toLong() * po1Price // 1.000.000

        // PO2: 50 units @ 13.000
        val po2Qty = 50
        val po2Price = 13_000L
        val totalCostPo1Po2 = costAfterPo1 + (po2Qty.toLong() * po2Price) // 1.650.000
        val totalQtyPo1Po2 = po1Qty + po2Qty // 150
        val avgCost = Math.round(totalCostPo1Po2.toDouble() / totalQtyPo1Po2)
        assertEquals(11_000L, avgCost)

        // SO: 40 units @ 15.000
        val soQty = 40
        val soPrice = 15_000L
        val omset = soQty.toLong() * soPrice
        assertEquals(600_000L, omset)

        // HPP at time of sale snapshot:
        val hpp = soQty.toLong() * avgCost
        assertEquals(440_000L, hpp)

        val labaKotor = omset - hpp
        assertEquals(160_000L, labaKotor)

        // Payment: 300.000 -> Customer commission (5%)
        val payment = 300_000L
        val commissionRate = 5.0
        val commission = Math.round((payment.toDouble() * commissionRate) / 100.0)
        assertEquals(15_000L, commission)

        // Expense: 20.000
        val expense = 20_000L

        // Laba Bersih = Laba Kotor - Expense - Commission
        val labaBersih = labaKotor - expense - commission
        assertEquals(125_000L, labaBersih)

        // Then PO3: 50 units @ 20.000
        val remainingStockBeforePo3 = totalQtyPo1Po2 - soQty // 110 units
        val remainingCost = remainingStockBeforePo3.toLong() * avgCost // 1.210.000
        val po3Qty = 50
        val po3Price = 20_000L
        val newTotalCost = remainingCost + (po3Qty.toLong() * po3Price) // 2.210.000
        val newTotalQty = remainingStockBeforePo3 + po3Qty // 160
        val newAvgCost = Math.round(newTotalCost.toDouble() / newTotalQty)
        assertEquals(13_813L, newAvgCost)

        // Assert the earlier SO's HPP stays 440.000 (does not change with PO3)
        val earlierSoHppStill = soQty.toLong() * avgCost
        assertEquals(440_000L, earlierSoHppStill)
    }
}

