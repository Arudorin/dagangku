package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "produk")
data class Produk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String,
    val satuan: String, // Pcs, Kg, Dus, Karton, Btl, dll.
    val hargaDasar: Double, // HPP / Weighted average cost
    val hargaJual: Double,
    val stokMinimum: Int = 5
)

@Entity(tableName = "distributor")
data class Distributor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String,
    val noHp: String,
    val alamat: String
)

@Entity(tableName = "customer")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String,
    val noHp: String,
    val alamat: String,
    val persenKomisi: Double = 0.0 // Komisi % untuk customer/sales agen
)

@Entity(
    tableName = "harga_customer",
    indices = [
        Index(value = ["customerId", "produkId"], unique = true)
    ]
)
data class HargaCustomer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val produkId: Long,
    val hargaDeal: Double
)

@Entity(tableName = "po")
data class PO(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nomor: String,
    val distributorId: Long,
    val tanggal: Long, // Epoch millis
    val total: Double
)

@Entity(
    tableName = "item_po",
    indices = [Index(value = ["poId"]), Index(value = ["produkId"])]
)
data class ItemPO(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val poId: Long,
    val produkId: Long,
    val qty: Int,
    val hargaBeli: Double
)

@Entity(tableName = "so")
data class SO(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nomor: String,
    val customerId: Long,
    val tanggal: Long, // Epoch millis
    val total: Double
)

@Entity(
    tableName = "item_so",
    indices = [Index(value = ["soId"]), Index(value = ["produkId"])]
)
data class ItemSO(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val soId: Long,
    val produkId: Long,
    val qty: Int,
    val harga: Double
)

@Entity(
    tableName = "pembayaran",
    indices = [Index(value = ["refId"]), Index(value = ["tipe"])]
)
data class Pembayaran(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipe: String, // "CUSTOMER" (SO) atau "DISTRIBUTOR" (PO)
    val refId: Long,  // id dari SO atau PO
    val nominal: Double,
    val tanggal: Long,
    val metode: String, // Tunai, Transfer Bank, QRIS, Giro
    val catatan: String = ""
)

@Entity(tableName = "pengeluaran")
data class Pengeluaran(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kategori: String, // Operasional, Gaji, Sewa, Listrik & Air, Transport, Lain-lain
    val nominal: Double,
    val tanggal: Long,
    val keterangan: String
)

enum class StatusPembayaran(val label: String) {
    LUNAS("Lunas"),
    SEBAGIAN("Sebagian"),
    BELUM_LUNAS("Belum Lunas")
}
