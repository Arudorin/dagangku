package com.example.util

import android.content.Context
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val appName: String = "DagangKu",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val saldoAwalKas: Long = 0L
)

data class BackupSummary(
    val produkCount: Int = 0,
    val distributorCount: Int = 0,
    val customerCount: Int = 0,
    val hargaCustomerCount: Int = 0,
    val poCount: Int = 0,
    val itemPoCount: Int = 0,
    val soCount: Int = 0,
    val itemSoCount: Int = 0,
    val pembayaranCount: Int = 0,
    val pengeluaranCount: Int = 0,
    val saldoAwalKas: Long = 0L,
    val exportedAt: Long = 0L,
    val appName: String = "DagangKu"
)

data class BackupData(
    val metadata: BackupMetadata,
    val produkList: List<Produk> = emptyList(),
    val distributorList: List<Distributor> = emptyList(),
    val customerList: List<Customer> = emptyList(),
    val hargaCustomerList: List<HargaCustomer> = emptyList(),
    val poList: List<PO> = emptyList(),
    val itemPoList: List<ItemPO> = emptyList(),
    val soList: List<SO> = emptyList(),
    val itemSoList: List<ItemSO> = emptyList(),
    val pembayaranList: List<Pembayaran> = emptyList(),
    val pengeluaranList: List<Pengeluaran> = emptyList()
) {
    val summary: BackupSummary
        get() = BackupSummary(
            produkCount = produkList.size,
            distributorCount = distributorList.size,
            customerCount = customerList.size,
            hargaCustomerCount = hargaCustomerList.size,
            poCount = poList.size,
            itemPoCount = itemPoList.size,
            soCount = soList.size,
            itemSoCount = itemSoList.size,
            pembayaranCount = pembayaranList.size,
            pengeluaranCount = pengeluaranList.size,
            saldoAwalKas = metadata.saldoAwalKas,
            exportedAt = metadata.exportedAt,
            appName = metadata.appName
        )
}

data class ExportBackupResult(
    val file: File,
    val jsonString: String,
    val summary: BackupSummary
)

object BackupRestoreManager {

    /**
     * Converts BackupData to a structured JSON string.
     */
    fun backupToJson(backupData: BackupData): String {
        val root = JSONObject()

        // Metadata
        root.put("appName", backupData.metadata.appName)
        root.put("version", backupData.metadata.version)
        root.put("exportedAt", backupData.metadata.exportedAt)
        root.put("exportedAtFormatted", Formatters.formatTanggalWaktu(backupData.metadata.exportedAt))
        root.put("saldoAwalKas", backupData.metadata.saldoAwalKas)

        // Summary for quick preview
        val summaryObj = JSONObject().apply {
            put("produkCount", backupData.produkList.size)
            put("distributorCount", backupData.distributorList.size)
            put("customerCount", backupData.customerList.size)
            put("hargaCustomerCount", backupData.hargaCustomerList.size)
            put("poCount", backupData.poList.size)
            put("itemPoCount", backupData.itemPoList.size)
            put("soCount", backupData.soList.size)
            put("itemSoCount", backupData.itemSoList.size)
            put("pembayaranCount", backupData.pembayaranList.size)
            put("pengeluaranCount", backupData.pengeluaranList.size)
        }
        root.put("summary", summaryObj)

        // 1. Produk
        val produkArray = JSONArray()
        for (item in backupData.produkList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("nama", item.nama)
                put("satuan", item.satuan)
                put("hargaDasar", item.hargaDasar)
                put("hargaJual", item.hargaJual)
                put("stokMinimum", item.stokMinimum)
            }
            produkArray.put(obj)
        }
        root.put("produk", produkArray)

        // 2. Distributor
        val distributorArray = JSONArray()
        for (item in backupData.distributorList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("nama", item.nama)
                put("noHp", item.noHp)
                put("alamat", item.alamat)
            }
            distributorArray.put(obj)
        }
        root.put("distributor", distributorArray)

        // 3. Customer
        val customerArray = JSONArray()
        for (item in backupData.customerList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("nama", item.nama)
                put("noHp", item.noHp)
                put("alamat", item.alamat)
                put("persenKomisi", item.persenKomisi)
            }
            customerArray.put(obj)
        }
        root.put("customer", customerArray)

        // 4. HargaCustomer
        val hargaCustomerArray = JSONArray()
        for (item in backupData.hargaCustomerList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("customerId", item.customerId)
                put("produkId", item.produkId)
                put("hargaDeal", item.hargaDeal)
            }
            hargaCustomerArray.put(obj)
        }
        root.put("hargaCustomer", hargaCustomerArray)

        // 5. PO
        val poArray = JSONArray()
        for (item in backupData.poList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("nomor", item.nomor)
                put("distributorId", item.distributorId)
                put("tanggal", item.tanggal)
                put("total", item.total)
            }
            poArray.put(obj)
        }
        root.put("po", poArray)

        // 6. ItemPO
        val itemPoArray = JSONArray()
        for (item in backupData.itemPoList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("poId", item.poId)
                put("produkId", item.produkId)
                put("qty", item.qty)
                put("hargaBeli", item.hargaBeli)
            }
            itemPoArray.put(obj)
        }
        root.put("itemPo", itemPoArray)

        // 7. SO
        val soArray = JSONArray()
        for (item in backupData.soList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("nomor", item.nomor)
                put("customerId", item.customerId)
                put("tanggal", item.tanggal)
                put("total", item.total)
            }
            soArray.put(obj)
        }
        root.put("so", soArray)

        // 8. ItemSO
        val itemSoArray = JSONArray()
        for (item in backupData.itemSoList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("soId", item.soId)
                put("produkId", item.produkId)
                put("qty", item.qty)
                put("harga", item.harga)
                put("hppSaatJual", item.hppSaatJual)
            }
            itemSoArray.put(obj)
        }
        root.put("itemSo", itemSoArray)

        // 9. Pembayaran
        val pembayaranArray = JSONArray()
        for (item in backupData.pembayaranList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("tipe", item.tipe)
                put("refId", item.refId)
                put("nominal", item.nominal)
                put("tanggal", item.tanggal)
                put("metode", item.metode)
                put("catatan", item.catatan)
            }
            pembayaranArray.put(obj)
        }
        root.put("pembayaran", pembayaranArray)

        // 10. Pengeluaran
        val pengeluaranArray = JSONArray()
        for (item in backupData.pengeluaranList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("kategori", item.kategori)
                put("nominal", item.nominal)
                put("tanggal", item.tanggal)
                put("keterangan", item.keterangan)
            }
            pengeluaranArray.put(obj)
        }
        root.put("pengeluaran", pengeluaranArray)

        return root.toString(2)
    }

    /**
     * Saves backup data to a cache file ready for sharing or saving.
     */
    fun writeBackupFile(context: Context, backupData: BackupData): ExportBackupResult {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Backup_DagangKu_$timeStamp.json"
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(backupDir, fileName)

        val jsonString = backupToJson(backupData)
        FileWriter(file).use { writer ->
            writer.write(jsonString)
        }

        return ExportBackupResult(
            file = file,
            jsonString = jsonString,
            summary = backupData.summary
        )
    }

    /**
     * Parses and validates a JSON backup string.
     */
    fun parseJsonToBackup(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString)

            // Validate that this is a DagangKu backup or has essential database tables
            val isDagangKu = root.optString("appName") == "DagangKu" ||
                    root.has("produk") ||
                    root.has("so") ||
                    root.has("po") ||
                    root.has("customer")

            if (!isDagangKu) {
                return Result.failure(IllegalArgumentException("Format file tidak sesuai: bukan file cadangan database DagangKu."))
            }

            val metadata = BackupMetadata(
                appName = root.optString("appName", "DagangKu"),
                version = root.optInt("version", 1),
                exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
                saldoAwalKas = root.optLong("saldoAwalKas", 0L)
            )

            // 1. Produk
            val produkList = mutableListOf<Produk>()
            val produkArray = root.optJSONArray("produk")
            if (produkArray != null) {
                for (i in 0 until produkArray.length()) {
                    val obj = produkArray.getJSONObject(i)
                    produkList.add(
                        Produk(
                            id = obj.optLong("id", 0L),
                            nama = obj.optString("nama", ""),
                            satuan = obj.optString("satuan", "Pcs"),
                            hargaDasar = obj.optLong("hargaDasar", 0L),
                            hargaJual = obj.optLong("hargaJual", 0L),
                            stokMinimum = obj.optInt("stokMinimum", 5)
                        )
                    )
                }
            }

            // 2. Distributor
            val distributorList = mutableListOf<Distributor>()
            val distributorArray = root.optJSONArray("distributor")
            if (distributorArray != null) {
                for (i in 0 until distributorArray.length()) {
                    val obj = distributorArray.getJSONObject(i)
                    distributorList.add(
                        Distributor(
                            id = obj.optLong("id", 0L),
                            nama = obj.optString("nama", ""),
                            noHp = obj.optString("noHp", ""),
                            alamat = obj.optString("alamat", "")
                        )
                    )
                }
            }

            // 3. Customer
            val customerList = mutableListOf<Customer>()
            val customerArray = root.optJSONArray("customer")
            if (customerArray != null) {
                for (i in 0 until customerArray.length()) {
                    val obj = customerArray.getJSONObject(i)
                    customerList.add(
                        Customer(
                            id = obj.optLong("id", 0L),
                            nama = obj.optString("nama", ""),
                            noHp = obj.optString("noHp", ""),
                            alamat = obj.optString("alamat", ""),
                            persenKomisi = obj.optDouble("persenKomisi", 0.0)
                        )
                    )
                }
            }

            // 4. HargaCustomer
            val hargaCustomerList = mutableListOf<HargaCustomer>()
            val hargaCustomerArray = root.optJSONArray("hargaCustomer")
            if (hargaCustomerArray != null) {
                for (i in 0 until hargaCustomerArray.length()) {
                    val obj = hargaCustomerArray.getJSONObject(i)
                    hargaCustomerList.add(
                        HargaCustomer(
                            id = obj.optLong("id", 0L),
                            customerId = obj.optLong("customerId", 0L),
                            produkId = obj.optLong("produkId", 0L),
                            hargaDeal = obj.optLong("hargaDeal", 0L)
                        )
                    )
                }
            }

            // 5. PO
            val poList = mutableListOf<PO>()
            val poArray = root.optJSONArray("po")
            if (poArray != null) {
                for (i in 0 until poArray.length()) {
                    val obj = poArray.getJSONObject(i)
                    poList.add(
                        PO(
                            id = obj.optLong("id", 0L),
                            nomor = obj.optString("nomor", ""),
                            distributorId = obj.optLong("distributorId", 0L),
                            tanggal = obj.optLong("tanggal", System.currentTimeMillis()),
                            total = obj.optLong("total", 0L)
                        )
                    )
                }
            }

            // 6. ItemPO
            val itemPoList = mutableListOf<ItemPO>()
            val itemPoArray = root.optJSONArray("itemPo")
            if (itemPoArray != null) {
                for (i in 0 until itemPoArray.length()) {
                    val obj = itemPoArray.getJSONObject(i)
                    itemPoList.add(
                        ItemPO(
                            id = obj.optLong("id", 0L),
                            poId = obj.optLong("poId", 0L),
                            produkId = obj.optLong("produkId", 0L),
                            qty = obj.optInt("qty", 1),
                            hargaBeli = obj.optLong("hargaBeli", 0L)
                        )
                    )
                }
            }

            // 7. SO
            val soList = mutableListOf<SO>()
            val soArray = root.optJSONArray("so")
            if (soArray != null) {
                for (i in 0 until soArray.length()) {
                    val obj = soArray.getJSONObject(i)
                    soList.add(
                        SO(
                            id = obj.optLong("id", 0L),
                            nomor = obj.optString("nomor", ""),
                            customerId = obj.optLong("customerId", 0L),
                            tanggal = obj.optLong("tanggal", System.currentTimeMillis()),
                            total = obj.optLong("total", 0L)
                        )
                    )
                }
            }

            // 8. ItemSO
            val itemSoList = mutableListOf<ItemSO>()
            val itemSoArray = root.optJSONArray("itemSo")
            if (itemSoArray != null) {
                for (i in 0 until itemSoArray.length()) {
                    val obj = itemSoArray.getJSONObject(i)
                    itemSoList.add(
                        ItemSO(
                            id = obj.optLong("id", 0L),
                            soId = obj.optLong("soId", 0L),
                            produkId = obj.optLong("produkId", 0L),
                            qty = obj.optInt("qty", 1),
                            harga = obj.optLong("harga", 0L),
                            hppSaatJual = obj.optLong("hppSaatJual", 0L)
                        )
                    )
                }
            }

            // 9. Pembayaran
            val pembayaranList = mutableListOf<Pembayaran>()
            val pembayaranArray = root.optJSONArray("pembayaran")
            if (pembayaranArray != null) {
                for (i in 0 until pembayaranArray.length()) {
                    val obj = pembayaranArray.getJSONObject(i)
                    pembayaranList.add(
                        Pembayaran(
                            id = obj.optLong("id", 0L),
                            tipe = obj.optString("tipe", "CUSTOMER"),
                            refId = obj.optLong("refId", 0L),
                            nominal = obj.optLong("nominal", 0L),
                            tanggal = obj.optLong("tanggal", System.currentTimeMillis()),
                            metode = obj.optString("metode", "Tunai"),
                            catatan = obj.optString("catatan", "")
                        )
                    )
                }
            }

            // 10. Pengeluaran
            val pengeluaranList = mutableListOf<Pengeluaran>()
            val pengeluaranArray = root.optJSONArray("pengeluaran")
            if (pengeluaranArray != null) {
                for (i in 0 until pengeluaranArray.length()) {
                    val obj = pengeluaranArray.getJSONObject(i)
                    pengeluaranList.add(
                        Pengeluaran(
                            id = obj.optLong("id", 0L),
                            kategori = obj.optString("kategori", "Operasional"),
                            nominal = obj.optLong("nominal", 0L),
                            tanggal = obj.optLong("tanggal", System.currentTimeMillis()),
                            keterangan = obj.optString("keterangan", "")
                        )
                    )
                }
            }

            Result.success(
                BackupData(
                    metadata = metadata,
                    produkList = produkList,
                    distributorList = distributorList,
                    customerList = customerList,
                    hargaCustomerList = hargaCustomerList,
                    poList = poList,
                    itemPoList = itemPoList,
                    soList = soList,
                    itemSoList = itemSoList,
                    pembayaranList = pembayaranList,
                    pengeluaranList = pengeluaranList
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Gagal membaca berkas JSON: ${e.localizedMessage ?: "Format tidak valid"}"))
        }
    }
}
