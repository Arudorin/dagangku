package com.example.data.model

data class ProdukWithStock(
    val produk: Produk,
    val stok: Int
) {
    val isLowStock: Boolean
        get() = stok <= produk.stokMinimum

    val totalNilaiAset: Double
        get() = stok.coerceAtLeast(0) * produk.hargaDasar
}

data class ItemPoWithProduct(
    val item: ItemPO,
    val produk: Produk?
) {
    val subtotal: Double
        get() = item.qty * item.hargaBeli
}

data class PoWithDetails(
    val po: PO,
    val distributor: Distributor?,
    val items: List<ItemPoWithProduct>,
    val payments: List<Pembayaran>
) {
    val totalPaid: Double
        get() = payments.sumOf { it.nominal }

    val sisaHutang: Double
        get() = (po.total - totalPaid).coerceAtLeast(0.0)

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
    val subtotal: Double
        get() = item.qty * item.harga
}

data class SoWithDetails(
    val so: SO,
    val customer: Customer?,
    val items: List<ItemSoWithProduct>,
    val payments: List<Pembayaran>
) {
    val totalPaid: Double
        get() = payments.sumOf { it.nominal }

    val sisaPiutang: Double
        get() = (so.total - totalPaid).coerceAtLeast(0.0)

    val komisiNominal: Double
        get() = if (customer != null && customer.persenKomisi > 0) {
            (so.total * customer.persenKomisi) / 100.0
        } else 0.0

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
    val totalPenjualan: Double = 0.0, // Omset
    val totalHpp: Double = 0.0,
    val totalLabaKotor: Double = 0.0,
    val totalLabaBersih: Double = 0.0,
    val totalPembelian: Double = 0.0,
    val totalPiutang: Double = 0.0,
    val totalHutang: Double = 0.0,
    val totalPengeluaran: Double = 0.0,
    val totalProduk: Int = 0,
    val totalProdukMenipis: Int = 0,
    val totalPelanggan: Int = 0,
    val totalDistributor: Int = 0
)

data class CartItemPo(
    val produk: Produk,
    val qty: Int,
    val hargaBeli: Double
) {
    val subtotal: Double get() = qty * hargaBeli
}

data class CartItemSo(
    val produk: Produk,
    val qty: Int,
    val hargaJual: Double,
    val isDealPrice: Boolean = false,
    val availableStock: Int
) {
    val subtotal: Double get() = qty * hargaJual
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
    val nominal: Double,
    val metode: String, // Tunai, Transfer Bank, QRIS, dll.
    val catatan: String = ""
)

data class KasSummary(
    val totalMasuk: Double = 0.0,
    val totalKeluarDistributor: Double = 0.0,
    val totalKeluarOperasional: Double = 0.0
) {
    val totalKeluar: Double get() = totalKeluarDistributor + totalKeluarOperasional
    val saldoKas: Double get() = totalMasuk - totalKeluar
}

data class PaymentTargetInfo(
    val tipe: String, // "CUSTOMER" or "DISTRIBUTOR"
    val refId: Long,
    val refNumber: String,
    val partyName: String,
    val totalTransaction: Double,
    val alreadyPaid: Double,
    val remainingBalance: Double
)

enum class PeriodType(val label: String) {
    HARIAN("Harian"),
    MINGGUAN("Mingguan"),
    BULANAN("Bulanan"),
    CUSTOM("Kustom")
}

data class CustomerCommissionItem(
    val customer: Customer,
    val totalSoTerbayar: Double,
    val persenKomisi: Double,
    val totalKomisi: Double
)

data class ChartBarData(
    val label: String,
    val amount: Double,
    val timestamp: Long
)

data class LaporanKeuanganData(
    val periodType: PeriodType = PeriodType.BULANAN,
    val periodLabel: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val omset: Double = 0.0,
    val hpp: Double = 0.0,
    val labaKotor: Double = 0.0,
    val pengeluaranOperasional: Double = 0.0,
    val totalKomisi: Double = 0.0,
    val labaBersih: Double = 0.0,
    val soCount: Int = 0,
    val commissionList: List<CustomerCommissionItem> = emptyList(),
    val chartData: List<ChartBarData> = emptyList()
) {
    val marginLabaKotorPersen: Double
        get() = if (omset > 0) (labaKotor / omset) * 100.0 else 0.0

    val marginLabaBersihPersen: Double
        get() = if (omset > 0) (labaBersih / omset) * 100.0 else 0.0
}

