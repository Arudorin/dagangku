package com.example.data.model

data class ProdukWithStock(
    val produk: Produk,
    val stok: Int
) {
    val isLowStock: Boolean
        get() = stok <= produk.stokMinimum

    val totalNilaiAset: Long
        get() = stok.coerceAtLeast(0).toLong() * produk.hargaDasar
}

data class ItemPoWithProduct(
    val item: ItemPO,
    val produk: Produk?
) {
    val subtotal: Long
        get() = item.qty.toLong() * item.hargaBeli
}

data class PoWithDetails(
    val po: PO,
    val distributor: Distributor?,
    val items: List<ItemPoWithProduct>,
    val payments: List<Pembayaran>
) {
    val totalPaid: Long
        get() = payments.sumOf { it.nominal }

    val sisaHutang: Long
        get() = (po.total - totalPaid).coerceAtLeast(0L)

    val status: StatusPembayaran
        get() = when {
            totalPaid >= po.total && po.total > 0 -> StatusPembayaran.LUNAS
            totalPaid > 0 -> StatusPembayaran.SEBAGIAN
            else -> StatusPembayaran.BELUM_LUNAS
        }
}

data class ItemSoWithProduct(
    val item: ItemSO,
    val produk: Produk?
) {
    val subtotal: Long
        get() = item.qty.toLong() * item.harga

    val subtotalHpp: Long
        get() = item.qty.toLong() * item.hppSaatJual
}

data class SoWithDetails(
    val so: SO,
    val customer: Customer?,
    val items: List<ItemSoWithProduct>,
    val payments: List<Pembayaran>
) {
    val totalPaid: Long
        get() = payments.sumOf { it.nominal }

    val sisaPiutang: Long
        get() = (so.total - totalPaid).coerceAtLeast(0L)

    val komisiNominal: Long
        get() = if (customer != null && customer.persenKomisi > 0) {
            Math.round((totalPaid.toDouble() * customer.persenKomisi) / 100.0)
        } else 0L

    val status: StatusPembayaran
        get() = when {
            totalPaid >= so.total && so.total > 0 -> StatusPembayaran.LUNAS
            totalPaid > 0 -> StatusPembayaran.SEBAGIAN
            else -> StatusPembayaran.BELUM_LUNAS
        }
}

data class CustomerDealItem(
    val hargaCustomer: HargaCustomer,
    val produk: Produk
)

data class DashboardSummary(
    val totalPenjualan: Long = 0L, // Omset
    val totalHpp: Long = 0L,
    val totalLabaKotor: Long = 0L,
    val totalLabaBersih: Long = 0L,
    val totalPembelian: Long = 0L,
    val totalPiutang: Long = 0L,
    val totalHutang: Long = 0L,
    val totalPengeluaran: Long = 0L,
    val totalProduk: Int = 0,
    val totalProdukMenipis: Int = 0,
    val totalPelanggan: Int = 0,
    val totalDistributor: Int = 0
)

data class CartItemPo(
    val produk: Produk,
    val qty: Int,
    val hargaBeli: Long
) {
    val subtotal: Long get() = qty.toLong() * hargaBeli
}

data class CartItemSo(
    val produk: Produk,
    val qty: Int,
    val hargaJual: Long,
    val isDealPrice: Boolean = false,
    val availableStock: Int
) {
    val subtotal: Long get() = qty.toLong() * hargaJual
    val isInsufficientStock: Boolean get() = qty > availableStock
}

enum class KasType {
    MASUK,
    KELUAR
}

data class KasTransaction(
    val id: String, // unique ID e.g. "PAY-123" or "EXP-456"
    val tanggal: Long,
    val tipe: KasType, // MASUK (green) or KELUAR (red)
    val judul: String,
    val pihak: String, // Customer name or Distributor name or Kategori
    val refDocNumber: String? = null, // e.g. SO-20260925-001 or PO-20260920-001
    val refType: String? = null, // "SO", "PO", "EXPENSE"
    val refId: Long? = null,
    val nominal: Long,
    val metode: String, // Tunai, Transfer Bank, QRIS, dll.
    val catatan: String = ""
)

data class KasSummary(
    val totalMasuk: Long = 0L,
    val totalKeluarDistributor: Long = 0L,
    val totalKeluarOperasional: Long = 0L,
    val saldoAwal: Long = 0L
) {
    val totalKeluar: Long get() = totalKeluarDistributor + totalKeluarOperasional
    val saldoKas: Long get() = saldoAwal + totalMasuk - totalKeluar
}

data class PaymentTargetInfo(
    val tipe: String, // "CUSTOMER" or "DISTRIBUTOR"
    val refId: Long,
    val refNumber: String,
    val partyName: String,
    val totalTransaction: Long,
    val alreadyPaid: Long,
    val remainingBalance: Long
)

enum class PeriodType(val label: String) {
    HARIAN("Harian"),
    MINGGUAN("Mingguan"),
    BULANAN("Bulanan"),
    CUSTOM("Kustom")
}

data class CustomerCommissionItem(
    val customer: Customer,
    val totalSoTerbayar: Long,
    val persenKomisi: Double,
    val totalKomisi: Long
)

data class ChartBarData(
    val label: String,
    val amount: Long,
    val timestamp: Long
)

data class LaporanKeuanganData(
    val periodType: PeriodType = PeriodType.BULANAN,
    val periodLabel: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val omset: Long = 0L,
    val hpp: Long = 0L,
    val labaKotor: Long = 0L,
    val pengeluaranOperasional: Long = 0L,
    val totalKomisi: Long = 0L,
    val labaBersih: Long = 0L,
    val soCount: Int = 0,
    val commissionList: List<CustomerCommissionItem> = emptyList(),
    val chartData: List<ChartBarData> = emptyList()
) {
    val marginLabaKotorPersen: Double
        get() = if (omset > 0L) (labaKotor.toDouble() / omset.toDouble()) * 100.0 else 0.0

    val marginLabaBersihPersen: Double
        get() = if (omset > 0L) (labaBersih.toDouble() / omset.toDouble()) * 100.0 else 0.0
}

