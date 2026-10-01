package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DagangKu", appName)
  }

  @Test
  fun testBackupToJsonAndParseBack() {
    val produk = com.example.data.model.Produk(
      id = 1,
      nama = "Beras Rojolele 5kg",
      satuan = "Karung",
      hargaDasar = 65_000L,
      hargaJual = 75_000L,
      stokMinimum = 10
    )
    val customer = com.example.data.model.Customer(
      id = 2,
      nama = "Ibu Siti",
      noHp = "08123456789",
      alamat = "Pasar Induk Blok B",
      persenKomisi = 2.5
    )
    val distributor = com.example.data.model.Distributor(
      id = 3,
      nama = "PT Pangan Utama",
      noHp = "08555666777",
      alamat = "Kawasan Industri"
    )
    val so = com.example.data.model.SO(
      id = 10,
      nomor = "SO-20261001-001",
      customerId = 2,
      tanggal = 1_700_000_000L,
      total = 150_000L
    )
    val itemSo = com.example.data.model.ItemSO(
      id = 100,
      soId = 10,
      produkId = 1,
      qty = 2,
      harga = 75_000L,
      hppSaatJual = 65_000L
    )
    val pembayaran = com.example.data.model.Pembayaran(
      id = 50,
      tipe = "CUSTOMER",
      refId = 10,
      nominal = 150_000L,
      tanggal = 1_700_000_500L,
      metode = "Tunai",
      catatan = "Lunas"
    )

    val originalBackup = com.example.util.BackupData(
      metadata = com.example.util.BackupMetadata(
        appName = "DagangKu",
        version = 1,
        exportedAt = 1_700_000_000L,
        saldoAwalKas = 1_500_000L
      ),
      produkList = listOf(produk),
      distributorList = listOf(distributor),
      customerList = listOf(customer),
      soList = listOf(so),
      itemSoList = listOf(itemSo),
      pembayaranList = listOf(pembayaran)
    )

    // 1. Serialize to JSON
    val json = com.example.util.BackupRestoreManager.backupToJson(originalBackup)
    org.junit.Assert.assertTrue(json.contains("DagangKu"))
    org.junit.Assert.assertTrue(json.contains("Beras Rojolele 5kg"))
    org.junit.Assert.assertTrue(json.contains("Ibu Siti"))
    org.junit.Assert.assertTrue(json.contains("PT Pangan Utama"))
    org.junit.Assert.assertTrue(json.contains("SO-20261001-001"))

    // 2. Parse from JSON
    val parseResult = com.example.util.BackupRestoreManager.parseJsonToBackup(json)
    org.junit.Assert.assertTrue(parseResult.isSuccess)

    val parsed = parseResult.getOrThrow()
    assertEquals(1, parsed.produkList.size)
    assertEquals("Beras Rojolele 5kg", parsed.produkList[0].nama)
    assertEquals(75_000L, parsed.produkList[0].hargaJual)

    assertEquals(1, parsed.customerList.size)
    assertEquals("Ibu Siti", parsed.customerList[0].nama)
    assertEquals(2.5, parsed.customerList[0].persenKomisi, 0.001)

    assertEquals(1, parsed.distributorList.size)
    assertEquals("PT Pangan Utama", parsed.distributorList[0].nama)

    assertEquals(1, parsed.soList.size)
    assertEquals("SO-20261001-001", parsed.soList[0].nomor)

    assertEquals(1, parsed.itemSoList.size)
    assertEquals(65_000L, parsed.itemSoList[0].hppSaatJual)

    assertEquals(1, parsed.pembayaranList.size)
    assertEquals(150_000L, parsed.pembayaranList[0].nominal)

    assertEquals(1_500_000L, parsed.metadata.saldoAwalKas)
    assertEquals(1, parsed.summary.produkCount)
    assertEquals(1, parsed.summary.soCount)
  }

  @Test
  fun testInvalidBackupJsonThrowsError() {
    val invalidJson = "{\"randomKey\": \"someValue\"}"
    val result = com.example.util.BackupRestoreManager.parseJsonToBackup(invalidJson)
    org.junit.Assert.assertTrue(result.isFailure)
    org.junit.Assert.assertTrue(result.exceptionOrNull()?.message?.contains("Format file tidak sesuai") == true)
  }

  @Test
  fun testBusinessRulesScenarioEndToEnd() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val repository = com.example.data.repository.DagangKuRepositoryImpl(db, context)

    // 1. Setup Master Data
    val produkId = repository.saveProduk(
      com.example.data.model.Produk(
        nama = "Kopi Robusta 1kg",
        satuan = "Bks",
        hargaDasar = 0L,
        hargaJual = 15_000L,
        stokMinimum = 5
      )
    )
    val distributorId = repository.saveDistributor(
      com.example.data.model.Distributor(
        nama = "Supplier Kopi",
        noHp = "08111222333",
        alamat = "Lampung"
      )
    )
    val customerId = repository.saveCustomer(
      com.example.data.model.Customer(
        nama = "Customer VIP",
        noHp = "08999888777",
        alamat = "Jakarta",
        persenKomisi = 5.0 // 5% commission
      )
    )

    // 2. PO1: 100 units @ 10.000
    val po1Result = repository.createPO(
      po = com.example.data.model.PO(
        nomor = "PO-001",
        distributorId = distributorId,
        tanggal = 1_000_000L,
        total = 0L
      ),
      items = listOf(
        com.example.data.model.ItemPO(
          poId = 0L,
          produkId = produkId,
          qty = 100,
          hargaBeli = 10_000L
        )
      )
    )
    org.junit.Assert.assertTrue(po1Result.isSuccess)
    var prod = repository.getProdukById(produkId)
    assertEquals(10_000L, prod?.hargaDasar)
    assertEquals(100, repository.getProdukStock(produkId))

    // 3. PO2: 50 units @ 13.000
    // Weighted average: ((100 * 10.000) + (50 * 13.000)) / (100 + 50) = 1.650.000 / 150 = 11.000
    val po2Result = repository.createPO(
      po = com.example.data.model.PO(
        nomor = "PO-002",
        distributorId = distributorId,
        tanggal = 1_100_000L,
        total = 0L
      ),
      items = listOf(
        com.example.data.model.ItemPO(
          poId = 0L,
          produkId = produkId,
          qty = 50,
          hargaBeli = 13_000L
        )
      )
    )
    org.junit.Assert.assertTrue(po2Result.isSuccess)
    prod = repository.getProdukById(produkId)
    // Assert average cost must be 11.000
    assertEquals(11_000L, prod?.hargaDasar)
    assertEquals(150, repository.getProdukStock(produkId))

    // 4. SO: 40 units @ 15.000 for customer with 5% commission
    val soResult = repository.createSO(
      so = com.example.data.model.SO(
        nomor = "SO-001",
        customerId = customerId,
        tanggal = 1_200_000L,
        total = 0L
      ),
      items = listOf(
        com.example.data.model.ItemSO(
          soId = 0L,
          produkId = produkId,
          qty = 40,
          harga = 15_000L
        )
      )
    )
    org.junit.Assert.assertTrue(soResult.isSuccess)
    val soId = soResult.getOrThrow()
    assertEquals(110, repository.getProdukStock(produkId))

    // 5. Payment 300.000 for the SO (out of total 600.000)
    repository.addPembayaran(
      com.example.data.model.Pembayaran(
        tipe = "CUSTOMER",
        refId = soId,
        nominal = 300_000L,
        tanggal = 1_250_000L,
        metode = "Transfer Bank",
        catatan = "DP Pembayaran"
      )
    )

    // 6. Expense 20.000
    repository.addPengeluaran(
      com.example.data.model.Pengeluaran(
        kategori = "Operasional",
        nominal = 20_000L,
        tanggal = 1_260_000L,
        keterangan = "Listrik dan kebersihan"
      )
    )

    // 7. Verify Financial Report:
    // Expected:
    // Omset = 600.000
    // HPP = 40 * 11.000 = 440.000
    // Laba Kotor = 600.000 - 440.000 = 160.000
    // Commission = 5% of 300.000 payment = 15.000
    // Expense = 20.000
    // Laba Bersih = 160.000 - 20.000 - 15.000 = 125.000
    val laporan = repository.getLaporanKeuangan(0L, 2_000_000L, com.example.data.model.PeriodType.CUSTOM).first()
    assertEquals(600_000L, laporan.omset)
    assertEquals(440_000L, laporan.hpp)
    assertEquals(160_000L, laporan.labaKotor)
    assertEquals(15_000L, laporan.totalKomisi)
    assertEquals(20_000L, laporan.pengeluaranOperasional)
    assertEquals(125_000L, laporan.labaBersih)

    // 8. Then add PO3: 50 units @ 20.000
    val po3Result = repository.createPO(
      po = com.example.data.model.PO(
        nomor = "PO-003",
        distributorId = distributorId,
        tanggal = 1_300_000L,
        total = 0L
      ),
      items = listOf(
        com.example.data.model.ItemPO(
          poId = 0L,
          produkId = produkId,
          qty = 50,
          hargaBeli = 20_000L
        )
      )
    )
    org.junit.Assert.assertTrue(po3Result.isSuccess)
    // Product's new current average cost changes:
    // Remaining before PO3: 110 units @ 11.000 = 1.210.000
    // PO3: 50 units @ 20.000 = 1.000.000
    // New average: 2.210.000 / 160 = 13.813L
    prod = repository.getProdukById(produkId)
    assertEquals(13_813L, prod?.hargaDasar)

    // Assert the earlier SO's HPP stays 440.000
    val soDetailsAfterPo3 = repository.getSoDetails(soId)
    val earlierSoHpp = soDetailsAfterPo3?.items?.sumOf { it.item.qty.toLong() * it.item.hppSaatJual } ?: 0L
    assertEquals(440_000L, earlierSoHpp)

    val laporanAfterPo3 = repository.getLaporanKeuangan(0L, 2_000_000L, com.example.data.model.PeriodType.CUSTOM).first()
    assertEquals(440_000L, laporanAfterPo3.hpp)
    assertEquals(125_000L, laporanAfterPo3.labaBersih)

    // 9. Assert that an SO exceeding stock is rejected
    // Current stock is 110 + 50 = 160
    assertEquals(160, repository.getProdukStock(produkId))
    val excessiveSoResult = repository.createSO(
      so = com.example.data.model.SO(
        nomor = "SO-EXCEED-STOCK",
        customerId = customerId,
        tanggal = 1_400_000L,
        total = 0L
      ),
      items = listOf(
        com.example.data.model.ItemSO(
          soId = 0L,
          produkId = produkId,
          qty = 161, // Exceeds available stock of 160!
          harga = 15_000L
        )
      )
    )
    org.junit.Assert.assertTrue(excessiveSoResult.isFailure)
    org.junit.Assert.assertTrue(
      excessiveSoResult.exceptionOrNull()?.message?.contains("Stok tidak mencukupi") == true
    )

    // 10. Assert that a payment exceeding the remaining balance is rejected
    // Earlier SO total = 600.000, already paid = 300.000, remaining balance = 300.000
    var paymentRejected = false
    try {
      repository.addPembayaran(
        com.example.data.model.Pembayaran(
          tipe = "CUSTOMER",
          refId = soId,
          nominal = 300_001L, // Exceeds remaining balance of 300.000!
          tanggal = 1_450_000L,
          metode = "Tunai"
        )
      )
    } catch (e: IllegalArgumentException) {
      paymentRejected = true
      org.junit.Assert.assertTrue(e.message?.contains("Nominal melebihi sisa tagihan") == true)
    }
    org.junit.Assert.assertTrue("Payment exceeding balance must throw IllegalArgumentException", paymentRejected)

    db.close()
  }
}
