package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class DagangKuRepositoryImpl(
    private val db: AppDatabase,
    private val context: Context? = null
) : DagangKuRepository {

    companion object {
        private const val PREFS_NAME = "dagangku_prefs"
        private const val KEY_SALDO_AWAL_KAS = "saldo_awal_kas"
        private const val KEY_SAMPLE_DATA_ENABLED = "sample_data_enabled"
        private const val KEY_HAS_CHOSEN_SETUP = "has_chosen_setup"
    }

    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _saldoAwalKasFlow = MutableStateFlow(
        prefs?.getLong(KEY_SALDO_AWAL_KAS, 0L) ?: 0L
    )
    private val _sampleDataEnabledFlow = MutableStateFlow(
        prefs?.getBoolean(KEY_SAMPLE_DATA_ENABLED, false) ?: false
    )
    private val _hasChosenSetupFlow = MutableStateFlow(
        prefs?.getBoolean(KEY_HAS_CHOSEN_SETUP, false) ?: false
    )

    private val produkDao = db.produkDao()
    private val distributorDao = db.distributorDao()
    private val customerDao = db.customerDao()
    private val hargaCustomerDao = db.hargaCustomerDao()
    private val poDao = db.poDao()
    private val itemPoDao = db.itemPoDao()
    private val soDao = db.soDao()
    private val itemSoDao = db.itemSoDao()
    private val pembayaranDao = db.pembayaranDao()
    private val pengeluaranDao = db.pengeluaranDao()

    // ----------------------------------------------------
    // Produk & Stock
    // ----------------------------------------------------
    override fun getProdukList(): Flow<List<ProdukWithStock>> {
        return combine(
            produkDao.getAll(),
            itemPoDao.getAllItems(),
            itemSoDao.getAllItems()
        ) { produks, poItems, soItems ->
            val poQtyMap = poItems.groupBy { it.produkId }
                .mapValues { entry -> entry.value.sumOf { it.qty } }
            val soQtyMap = soItems.groupBy { it.produkId }
                .mapValues { entry -> entry.value.sumOf { it.qty } }

            produks.map { p ->
                val totalPo = poQtyMap[p.id] ?: 0
                val totalSo = soQtyMap[p.id] ?: 0
                val stock = totalPo - totalSo
                ProdukWithStock(produk = p, stok = stock)
            }
        }
    }

    override fun getProdukByIdFlow(id: Long): Flow<Produk?> = produkDao.getByIdFlow(id)

    override suspend fun getProdukById(id: Long): Produk? = withContext(Dispatchers.IO) {
        produkDao.getById(id)
    }

    override suspend fun saveProduk(produk: Produk): Long = withContext(Dispatchers.IO) {
        produkDao.insert(produk)
    }

    override suspend fun updateProduk(produk: Produk) = withContext(Dispatchers.IO) {
        produkDao.update(produk)
    }

    override suspend fun deleteProduk(produk: Produk) = withContext(Dispatchers.IO) {
        val poCount = itemPoDao.countByProdukId(produk.id)
        val soCount = itemSoDao.countByProdukId(produk.id)
        if (poCount > 0 || soCount > 0) {
            val transactions = mutableListOf<String>()
            if (poCount > 0) transactions.add("$poCount transaksi Pembelian (PO)")
            if (soCount > 0) transactions.add("$soCount transaksi Penjualan (SO)")
            throw IllegalStateException("Produk '${produk.nama}' tidak dapat dihapus karena sudah digunakan dalam ${transactions.joinToString(" dan ")}.")
        }
        hargaCustomerDao.deleteByProdukId(produk.id)
        produkDao.delete(produk)
    }

    override suspend fun canDeleteProduk(produkId: Long): Boolean = withContext(Dispatchers.IO) {
        itemPoDao.countByProdukId(produkId) == 0 && itemSoDao.countByProdukId(produkId) == 0
    }

    override suspend fun getProdukStock(produkId: Long): Int = withContext(Dispatchers.IO) {
        val totalPo = itemPoDao.getTotalQtyForProduct(produkId)
        val totalSo = itemSoDao.getTotalQtyForProduct(produkId)
        totalPo - totalSo
    }

    // ----------------------------------------------------
    // Distributor
    // ----------------------------------------------------
    override fun getDistributorList(): Flow<List<Distributor>> = distributorDao.getAll()

    override suspend fun getDistributorById(id: Long): Distributor? = withContext(Dispatchers.IO) {
        distributorDao.getById(id)
    }

    override suspend fun saveDistributor(distributor: Distributor): Long = withContext(Dispatchers.IO) {
        distributorDao.insert(distributor)
    }

    override suspend fun updateDistributor(distributor: Distributor) = withContext(Dispatchers.IO) {
        distributorDao.update(distributor)
    }

    override suspend fun deleteDistributor(distributor: Distributor) = withContext(Dispatchers.IO) {
        val poCount = poDao.countByDistributorId(distributor.id)
        if (poCount > 0) {
            throw IllegalStateException("Distributor '${distributor.nama}' tidak dapat dihapus karena memiliki riwayat $poCount transaksi Pembelian (PO).")
        }
        distributorDao.delete(distributor)
    }

    override suspend fun canDeleteDistributor(distributorId: Long): Boolean = withContext(Dispatchers.IO) {
        poDao.countByDistributorId(distributorId) == 0
    }

    // ----------------------------------------------------
    // Customer
    // ----------------------------------------------------
    override fun getCustomerList(): Flow<List<Customer>> = customerDao.getAll()

    override suspend fun getCustomerById(id: Long): Customer? = withContext(Dispatchers.IO) {
        customerDao.getById(id)
    }

    override fun getCustomerByIdFlow(id: Long): Flow<Customer?> = customerDao.getByIdFlow(id)

    override suspend fun saveCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        customerDao.insert(customer)
    }

    override suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.update(customer)
    }

    override suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        val soCount = soDao.countByCustomerId(customer.id)
        if (soCount > 0) {
            throw IllegalStateException("Pelanggan '${customer.nama}' tidak dapat dihapus karena memiliki riwayat $soCount transaksi Penjualan (SO).")
        }
        hargaCustomerDao.deleteByCustomerId(customer.id)
        customerDao.delete(customer)
    }

    override suspend fun canDeleteCustomer(customerId: Long): Boolean = withContext(Dispatchers.IO) {
        soDao.countByCustomerId(customerId) == 0
    }

    // ----------------------------------------------------
    // Harga Customer (Deal Prices)
    // ----------------------------------------------------
    override fun getHargaCustomerList(customerId: Long): Flow<List<CustomerDealItem>> {
        return combine(
            hargaCustomerDao.getByCustomer(customerId),
            produkDao.getAll()
        ) { deals, produks ->
            val productMap = produks.associateBy { it.id }
            deals.mapNotNull { deal ->
                productMap[deal.produkId]?.let { prod ->
                    CustomerDealItem(hargaCustomer = deal, produk = prod)
                }
            }
        }
    }

    override suspend fun getEffectivePrice(customerId: Long, produkId: Long): Long = withContext(Dispatchers.IO) {
        val deal = hargaCustomerDao.getDeal(customerId, produkId)
        if (deal != null) {
            deal.hargaDeal
        } else {
            val prod = produkDao.getById(produkId)
            prod?.hargaJual ?: 0L
        }
    }

    override suspend fun setCustomerDealPrice(customerId: Long, produkId: Long, hargaDeal: Long) = withContext(Dispatchers.IO) {
        hargaCustomerDao.insertOrUpdate(
            HargaCustomer(
                customerId = customerId,
                produkId = produkId,
                hargaDeal = hargaDeal
            )
        )
        Unit
    }

    override suspend fun removeCustomerDealPrice(customerId: Long, produkId: Long) = withContext(Dispatchers.IO) {
        hargaCustomerDao.deleteDeal(customerId, produkId)
    }

    // ----------------------------------------------------
    // PO (Purchase Order)
    // ----------------------------------------------------
    override fun getPoList(): Flow<List<PoWithDetails>> {
        return combine(
            poDao.getAll(),
            distributorDao.getAll(),
            itemPoDao.getAllItems(),
            produkDao.getAll(),
            pembayaranDao.getByType("DISTRIBUTOR")
        ) { pos, distributors, items, produks, payments ->
            val distMap = distributors.associateBy { it.id }
            val prodMap = produks.associateBy { it.id }
            val itemMap = items.groupBy { it.poId }
            val paymentMap = payments.groupBy { it.refId }

            pos.map { po ->
                val poItems = (itemMap[po.id] ?: emptyList()).map { item ->
                    ItemPoWithProduct(item = item, produk = prodMap[item.produkId])
                }
                val poPayments = paymentMap[po.id] ?: emptyList()
                PoWithDetails(
                    po = po,
                    distributor = distMap[po.distributorId],
                    items = poItems,
                    payments = poPayments
                )
            }
        }
    }

    override fun getPoDetailsFlow(poId: Long): Flow<PoWithDetails?> {
        return combine(
            poDao.getByIdFlow(poId),
            distributorDao.getAll(),
            itemPoDao.getItemsForPo(poId),
            produkDao.getAll(),
            pembayaranDao.getByTypeAndRef("DISTRIBUTOR", poId)
        ) { po, distributors, items, produks, payments ->
            if (po == null) return@combine null
            val distMap = distributors.associateBy { it.id }
            val prodMap = produks.associateBy { it.id }
            val poItems = items.map { item ->
                ItemPoWithProduct(item = item, produk = prodMap[item.produkId])
            }
            PoWithDetails(
                po = po,
                distributor = distMap[po.distributorId],
                items = poItems,
                payments = payments
            )
        }
    }

    override suspend fun getPoDetails(poId: Long): PoWithDetails? = withContext(Dispatchers.IO) {
        val po = poDao.getById(poId) ?: return@withContext null
        val dist = distributorDao.getById(po.distributorId)
        val items = itemPoDao.getItemsForPoSync(poId).map { item ->
            ItemPoWithProduct(item = item, produk = produkDao.getById(item.produkId))
        }
        val payments = pembayaranDao.getByTypeAndRefSync("DISTRIBUTOR", poId)
        PoWithDetails(po = po, distributor = dist, items = items, payments = payments)
    }

    override suspend fun createPO(po: PO, items: List<ItemPO>): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("PO harus memiliki minimal 1 item"))
            }

            // Merge duplicate products if any: merge qty and use latest purchase price
            val mergedItems = items.groupBy { it.produkId }
                .map { (prodId, group) ->
                    val totalQty = group.sumOf { it.qty }
                    val latestPrice = group.last().hargaBeli
                    group.first().copy(qty = totalQty, hargaBeli = latestPrice)
                }

            // 1. Update hargaDasar of each product using Weighted Average Cost
            // Formula: ((stokLama * hargaDasarLama) + (qtyBeli * hargaBeli)) / (stokLama + qtyBeli)
            // Round the weighted-average cost to the nearest rupiah
            for (item in mergedItems) {
                val product = produkDao.getById(item.produkId)
                if (product != null) {
                    val currentStock = getProdukStock(item.produkId)
                    val newHargaDasar = if (currentStock <= 0) {
                        item.hargaBeli
                    } else {
                        val totalCost = (currentStock.toLong() * product.hargaDasar) + (item.qty.toLong() * item.hargaBeli)
                        val totalQty = currentStock + item.qty
                        Math.round(totalCost.toDouble() / totalQty)
                    }
                    produkDao.updateHargaDasar(item.produkId, newHargaDasar)
                }
            }

            // 2. Insert PO with recalculated total
            val recalculatedTotal = mergedItems.sumOf { it.qty.toLong() * it.hargaBeli }
            val finalPo = po.copy(total = recalculatedTotal)
            val poId = poDao.insert(finalPo)

            // 3. Insert PO Items with assigned poId
            val itemsWithPoId = mergedItems.map { it.copy(poId = poId) }
            itemPoDao.insertAll(itemsWithPoId)

            Result.success(poId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePO(poId: Long) = withContext(Dispatchers.IO) {
        val po = poDao.getById(poId)
        if (po != null) {
            itemPoDao.deleteByPoId(poId)
            pembayaranDao.deleteByRef("DISTRIBUTOR", poId)
            poDao.delete(po)
        }
    }

    // ----------------------------------------------------
    // SO (Sales Order)
    // ----------------------------------------------------
    override fun getSoList(): Flow<List<SoWithDetails>> {
        return combine(
            soDao.getAll(),
            customerDao.getAll(),
            itemSoDao.getAllItems(),
            produkDao.getAll(),
            pembayaranDao.getByType("CUSTOMER")
        ) { sos, customers, items, produks, payments ->
            val custMap = customers.associateBy { it.id }
            val prodMap = produks.associateBy { it.id }
            val itemMap = items.groupBy { it.soId }
            val paymentMap = payments.groupBy { it.refId }

            sos.map { so ->
                val soItems = (itemMap[so.id] ?: emptyList()).map { item ->
                    ItemSoWithProduct(item = item, produk = prodMap[item.produkId])
                }
                val soPayments = paymentMap[so.id] ?: emptyList()
                SoWithDetails(
                    so = so,
                    customer = custMap[so.customerId],
                    items = soItems,
                    payments = soPayments
                )
            }
        }
    }

    override fun getSoDetailsFlow(soId: Long): Flow<SoWithDetails?> {
        return combine(
            soDao.getByIdFlow(soId),
            customerDao.getAll(),
            itemSoDao.getItemsForSo(soId),
            produkDao.getAll(),
            pembayaranDao.getByTypeAndRef("CUSTOMER", soId)
        ) { so, customers, items, produks, payments ->
            if (so == null) return@combine null
            val custMap = customers.associateBy { it.id }
            val prodMap = produks.associateBy { it.id }
            val soItems = items.map { item ->
                ItemSoWithProduct(item = item, produk = prodMap[item.produkId])
            }
            SoWithDetails(
                so = so,
                customer = custMap[so.customerId],
                items = soItems,
                payments = payments
            )
        }
    }

    override suspend fun getSoDetails(soId: Long): SoWithDetails? = withContext(Dispatchers.IO) {
        val so = soDao.getById(soId) ?: return@withContext null
        val cust = customerDao.getById(so.customerId)
        val items = itemSoDao.getItemsForSoSync(soId).map { item ->
            ItemSoWithProduct(item = item, produk = produkDao.getById(item.produkId))
        }
        val payments = pembayaranDao.getByTypeAndRefSync("CUSTOMER", soId)
        SoWithDetails(so = so, customer = cust, items = items, payments = payments)
    }

    override suspend fun createSO(so: SO, items: List<ItemSO>): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("SO harus memiliki minimal 1 item"))
            }

            // Merge duplicate products if any: merge qty and use latest selling price and snapshot HPP
            val mergedItems = items.groupBy { it.produkId }
                .map { (prodId, group) ->
                    val totalQty = group.sumOf { it.qty }
                    val latestPrice = group.last().harga
                    val latestHpp = group.last().hppSaatJual
                    group.first().copy(qty = totalQty, harga = latestPrice, hppSaatJual = latestHpp)
                }

            // CRITICAL BUSINESS RULE: Recheck stock on the merged total
            for (item in mergedItems) {
                val availableStock = getProdukStock(item.produkId)
                if (item.qty > availableStock) {
                    val prod = produkDao.getById(item.produkId)
                    val prodName = prod?.nama ?: "Produk #${item.produkId}"
                    val unit = prod?.satuan ?: "unit"
                    return@withContext Result.failure(
                        IllegalStateException("Stok tidak mencukupi untuk '$prodName'! Tersedia: $availableStock $unit, diminta: ${item.qty} $unit.")
                    )
                }
            }

            // Fill hppSaatJual with the product's current weighted-average hargaDasar
            val itemsWithHpp = mergedItems.map { item ->
                val prod = produkDao.getById(item.produkId)
                val currentHargaDasar = prod?.hargaDasar ?: item.hppSaatJual
                item.copy(hppSaatJual = currentHargaDasar)
            }

            val recalculatedTotal = itemsWithHpp.sumOf { it.qty.toLong() * it.harga }
            val finalSo = so.copy(total = recalculatedTotal)

            // Insert SO
            val soId = soDao.insert(finalSo)

            // Insert SO Items (this decreases available stock dynamically)
            val itemsWithSoId = itemsWithHpp.map { it.copy(soId = soId) }
            itemSoDao.insertAll(itemsWithSoId)

            Result.success(soId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSO(soId: Long) = withContext(Dispatchers.IO) {
        val so = soDao.getById(soId)
        if (so != null) {
            itemSoDao.deleteBySoId(soId)
            pembayaranDao.deleteByRef("CUSTOMER", soId)
            soDao.delete(so)
        }
    }

    // ----------------------------------------------------
    // Pembayaran
    // ----------------------------------------------------
    override fun getPaymentsForRef(tipe: String, refId: Long): Flow<List<Pembayaran>> =
        pembayaranDao.getByTypeAndRef(tipe, refId)

    override fun getAllPayments(): Flow<List<Pembayaran>> = pembayaranDao.getAll()

    override suspend fun addPembayaran(pembayaran: Pembayaran): Long = withContext(Dispatchers.IO) {
        if (pembayaran.nominal <= 0) {
            throw IllegalArgumentException("Nominal pembayaran harus lebih besar dari 0")
        }
        val totalTransaction = if (pembayaran.tipe == "CUSTOMER") {
            soDao.getById(pembayaran.refId)?.total ?: 0L
        } else {
            poDao.getById(pembayaran.refId)?.total ?: 0L
        }
        val alreadyPaid = pembayaranDao.getTotalPaidForRef(pembayaran.tipe, pembayaran.refId)
        val remaining = (totalTransaction - alreadyPaid).coerceAtLeast(0L)
        if (pembayaran.nominal > remaining) {
            throw IllegalArgumentException("Nominal melebihi sisa tagihan")
        }
        pembayaranDao.insert(pembayaran)
    }

    override suspend fun deletePembayaran(pembayaran: Pembayaran) = withContext(Dispatchers.IO) {
        pembayaranDao.delete(pembayaran)
    }

    // ----------------------------------------------------
    // Pengeluaran Operasional
    // ----------------------------------------------------
    override fun getPengeluaranList(): Flow<List<Pengeluaran>> = pengeluaranDao.getAll()

    override suspend fun addPengeluaran(pengeluaran: Pengeluaran): Long = withContext(Dispatchers.IO) {
        pengeluaranDao.insert(pengeluaran)
    }

    override suspend fun deletePengeluaran(pengeluaran: Pengeluaran) = withContext(Dispatchers.IO) {
        pengeluaranDao.delete(pengeluaran)
    }

    // ----------------------------------------------------
    // Buku Kas & Arus Kas
    // ----------------------------------------------------
    override fun getBukuKas(): Flow<List<KasTransaction>> {
        return combine(
            pembayaranDao.getAll(),
            pengeluaranDao.getAll(),
            getSoList(),
            getPoList()
        ) { payments, expenses, soList, poList ->
            val soMap = soList.associateBy { it.so.id }
            val poMap = poList.associateBy { it.po.id }

            val paymentKasList = payments.map { payment ->
                if (payment.tipe == "CUSTOMER") {
                    val soDetail = soMap[payment.refId]
                    KasTransaction(
                        id = "PAY-${payment.id}",
                        tanggal = payment.tanggal,
                        tipe = KasType.MASUK,
                        judul = "Pelunasan Customer",
                        pihak = soDetail?.customer?.nama ?: "Customer Umum",
                        refDocNumber = soDetail?.so?.nomor,
                        refType = "SO",
                        refId = payment.refId,
                        nominal = payment.nominal,
                        metode = payment.metode,
                        catatan = payment.catatan
                    )
                } else {
                    val poDetail = poMap[payment.refId]
                    KasTransaction(
                        id = "PAY-${payment.id}",
                        tanggal = payment.tanggal,
                        tipe = KasType.KELUAR,
                        judul = "Bayar Distributor",
                        pihak = poDetail?.distributor?.nama ?: "Distributor",
                        refDocNumber = poDetail?.po?.nomor,
                        refType = "PO",
                        refId = payment.refId,
                        nominal = payment.nominal,
                        metode = payment.metode,
                        catatan = payment.catatan
                    )
                }
            }

            val expenseKasList = expenses.map { exp ->
                KasTransaction(
                    id = "EXP-${exp.id}",
                    tanggal = exp.tanggal,
                    tipe = KasType.KELUAR,
                    judul = "Biaya Operasional",
                    pihak = exp.kategori,
                    refDocNumber = null,
                    refType = "EXPENSE",
                    refId = exp.id,
                    nominal = exp.nominal,
                    metode = "Kas / Tunai",
                    catatan = exp.keterangan
                )
            }

            (paymentKasList + expenseKasList).sortedByDescending { it.tanggal }
        }
    }

    override fun getKasSummary(): Flow<KasSummary> {
        return combine(
            pembayaranDao.getByType("CUSTOMER"),
            pembayaranDao.getByType("DISTRIBUTOR"),
            pengeluaranDao.getAll(),
            _saldoAwalKasFlow
        ) { custPayments, distPayments, expenses, saldoAwal ->
            val totalIn = custPayments.sumOf { it.nominal }
            val totalOutDist = distPayments.sumOf { it.nominal }
            val totalOutExp = expenses.sumOf { it.nominal }
            KasSummary(
                totalMasuk = totalIn,
                totalKeluarDistributor = totalOutDist,
                totalKeluarOperasional = totalOutExp,
                saldoAwal = saldoAwal
            )
        }
    }

    // ----------------------------------------------------
    // Dashboard Stats
    // ----------------------------------------------------
    override fun getDashboardSummary(startDate: Long, endDate: Long): Flow<DashboardSummary> {
        return combine(
            getSoList(),
            getPoList(),
            getProdukList(),
            pengeluaranDao.getAll(),
            customerDao.getAll()
        ) { soList, poList, produkList, expenses, customers ->
            val soInPeriod = if (startDate == 0L && endDate == Long.MAX_VALUE) {
                soList
            } else {
                soList.filter { it.so.tanggal in startDate..endDate }
            }
            val poInPeriod = if (startDate == 0L && endDate == Long.MAX_VALUE) {
                poList
            } else {
                poList.filter { it.po.tanggal in startDate..endDate }
            }
            val expensesInPeriod = if (startDate == 0L && endDate == Long.MAX_VALUE) {
                expenses
            } else {
                expenses.filter { it.tanggal in startDate..endDate }
            }

            val totalSales = soInPeriod.sumOf { it.so.total }
            val totalPurchases = poInPeriod.sumOf { it.po.total }
            val totalPiutang = soList.sumOf { it.sisaPiutang }
            val totalHutang = poList.sumOf { it.sisaHutang }
            val lowStockCount = produkList.count { it.isLowStock }

            // All HPP calculations must use sum(qty x hppSaatJual), never Produk.hargaDasar
            val totalHpp = soInPeriod.sumOf { soDetail ->
                soDetail.items.sumOf { it.item.qty.toLong() * it.item.hppSaatJual }
            }
            val totalLabaKotor = totalSales - totalHpp
            val totalExp = expensesInPeriod.sumOf { it.nominal }

            // Commission = persenKomisi x total payments received for that SO (sum over all payments received)
            val totalKomisi = soInPeriod.sumOf { it.komisiNominal }
            val totalLabaBersih = totalLabaKotor - totalExp - totalKomisi

            DashboardSummary(
                totalPenjualan = totalSales,
                totalHpp = totalHpp,
                totalLabaKotor = totalLabaKotor,
                totalLabaBersih = totalLabaBersih,
                totalPembelian = totalPurchases,
                totalPiutang = totalPiutang,
                totalHutang = totalHutang,
                totalPengeluaran = totalExp,
                totalProduk = produkList.size,
                totalProdukMenipis = lowStockCount,
                totalPelanggan = customers.size,
                totalDistributor = poList.mapNotNull { it.distributor }.distinctBy { it.id }.size
            )
        }
    }

    // ----------------------------------------------------
    // Laporan Keuangan (Step 3)
    // ----------------------------------------------------
    override fun getLaporanKeuangan(
        startDate: Long,
        endDate: Long,
        periodType: PeriodType
    ): Flow<LaporanKeuanganData> {
        return combine(
            getSoList(),
            pembayaranDao.getByType("CUSTOMER"),
            pengeluaranDao.getAll(),
            customerDao.getAll()
        ) { soDetailsList, custPayments, expenses, customers ->
            // 1. Omset in period
            val soInPeriod = soDetailsList.filter { it.so.tanggal in startDate..endDate }
            val omset = soInPeriod.sumOf { it.so.total }

            // 2. HPP in period = sum(qty x hppSaatJual), never Produk.hargaDasar
            val hpp = soInPeriod.sumOf { soDetail ->
                soDetail.items.sumOf { itemWithProd ->
                    itemWithProd.item.qty.toLong() * itemWithProd.item.hppSaatJual
                }
            }

            // 3. Laba Kotor = Omset - HPP
            val labaKotor = omset - hpp

            // 4. Komisi per customer = persenKomisi x total payments received for that SO
            // Exactly matching Beranda dashboard: sum of it.komisiNominal
            val commissionList = mutableListOf<CustomerCommissionItem>()
            var totalKomisi = 0L
            for (cust in customers) {
                val custSos = soInPeriod.filter { it.so.customerId == cust.id }
                val paidAmount = custSos.sumOf { it.totalPaid }
                val komisi = custSos.sumOf { it.komisiNominal }
                if (cust.persenKomisi > 0 || paidAmount > 0L || komisi > 0L) {
                    totalKomisi += komisi
                    if (paidAmount > 0L || cust.persenKomisi > 0) {
                        commissionList.add(
                            CustomerCommissionItem(
                                customer = cust,
                                totalSoTerbayar = paidAmount,
                                persenKomisi = cust.persenKomisi,
                                totalKomisi = komisi
                            )
                        )
                    }
                }
            }
            commissionList.sortByDescending { it.totalKomisi }

            // 5. Pengeluaran Operasional in period
            val expensesInPeriod = expenses.filter { it.tanggal in startDate..endDate }
            val totalPengeluaran = expensesInPeriod.sumOf { it.nominal }

            // 6. Laba Bersih = Laba Kotor - Pengeluaran operasional - Komisi
            val labaBersih = labaKotor - totalPengeluaran - totalKomisi

            // 7. Chart Data (Omset distribution)
            val chartData = generateOmsetChartData(soInPeriod, startDate, endDate, periodType)

            val periodLabel = when (periodType) {
                PeriodType.HARIAN -> "Hari Ini (${com.example.util.Formatters.formatTanggal(startDate)})"
                PeriodType.MINGGUAN -> "Minggu Ini (${com.example.util.Formatters.formatSingkat(startDate)} - ${com.example.util.Formatters.formatSingkat(endDate)})"
                PeriodType.BULANAN -> "Bulan Ini (${com.example.util.Formatters.formatTanggal(startDate).substringAfter(" ")})"
                PeriodType.CUSTOM -> "${com.example.util.Formatters.formatSingkat(startDate)} - ${com.example.util.Formatters.formatSingkat(endDate)}"
            }

            LaporanKeuanganData(
                periodType = periodType,
                periodLabel = periodLabel,
                startDate = startDate,
                endDate = endDate,
                omset = omset,
                hpp = hpp,
                labaKotor = labaKotor,
                pengeluaranOperasional = totalPengeluaran,
                totalKomisi = totalKomisi,
                labaBersih = labaBersih,
                soCount = soInPeriod.size,
                commissionList = commissionList,
                chartData = chartData
            )
        }
    }

    private fun generateOmsetChartData(
        soInPeriod: List<SoWithDetails>,
        startDate: Long,
        endDate: Long,
        periodType: PeriodType
    ): List<ChartBarData> {
        val cal = java.util.Calendar.getInstance()
        val bars = mutableListOf<ChartBarData>()

        when (periodType) {
            PeriodType.HARIAN -> {
                // For daily: show last 7 days ending today so the user sees a meaningful trend
                val sevenDaysAgo = com.example.util.Formatters.getStartOfDay(startDate - 6L * 24 * 3600 * 1000)
                cal.timeInMillis = sevenDaysAgo
                val soList = soDao.getAll()
                // Group SOs from that 7-day range
                for (i in 0..6) {
                    val dayStart = com.example.util.Formatters.getStartOfDay(cal.timeInMillis)
                    val dayEnd = com.example.util.Formatters.getEndOfDay(cal.timeInMillis)
                    val dayOmset = soInPeriod.filter { it.so.tanggal in dayStart..dayEnd }.sumOf { it.so.total }
                    val label = com.example.util.Formatters.formatHariTanggal(dayStart).substringBefore(",")
                    bars.add(ChartBarData(label = label, amount = dayOmset, timestamp = dayStart))
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            PeriodType.MINGGUAN -> {
                // 7 daily bars for the week
                cal.timeInMillis = startDate
                for (i in 0..6) {
                    val dayStart = com.example.util.Formatters.getStartOfDay(cal.timeInMillis)
                    val dayEnd = com.example.util.Formatters.getEndOfDay(cal.timeInMillis)
                    val dayOmset = soInPeriod.filter { it.so.tanggal in dayStart..dayEnd }.sumOf { it.so.total }
                    val dayName = com.example.util.Formatters.formatHariTanggal(dayStart).substringBefore(",")
                    val shortDate = com.example.util.Formatters.formatSingkat(dayStart)
                    bars.add(ChartBarData(label = "$dayName\n$shortDate", amount = dayOmset, timestamp = dayStart))
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            PeriodType.BULANAN -> {
                // 4-5 weekly buckets for the month
                cal.timeInMillis = startDate
                var weekNum = 1
                while (cal.timeInMillis <= endDate) {
                    val wStart = cal.timeInMillis
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 6)
                    val wEnd = minOf(com.example.util.Formatters.getEndOfDay(cal.timeInMillis), endDate)
                    val weekOmset = soInPeriod.filter { it.so.tanggal in wStart..wEnd }.sumOf { it.so.total }
                    bars.add(
                        ChartBarData(
                            label = "Mgg $weekNum\n${com.example.util.Formatters.formatSingkat(wStart)}",
                            amount = weekOmset,
                            timestamp = wStart
                        )
                    )
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    weekNum++
                }
            }
            PeriodType.CUSTOM -> {
                val diffDays = ((endDate - startDate) / (24 * 3600 * 1000)).toInt().coerceAtLeast(1)
                if (diffDays <= 14) {
                    // Daily bars
                    cal.timeInMillis = startDate
                    for (i in 0 until diffDays) {
                        val dayStart = com.example.util.Formatters.getStartOfDay(cal.timeInMillis)
                        val dayEnd = com.example.util.Formatters.getEndOfDay(cal.timeInMillis)
                        val dayOmset = soInPeriod.filter { it.so.tanggal in dayStart..dayEnd }.sumOf { it.so.total }
                        val label = com.example.util.Formatters.formatSingkat(dayStart)
                        bars.add(ChartBarData(label = label, amount = dayOmset, timestamp = dayStart))
                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                } else {
                    // Segment into 5-6 buckets
                    val stepDays = (diffDays / 5).coerceAtLeast(1)
                    cal.timeInMillis = startDate
                    var seg = 1
                    while (cal.timeInMillis <= endDate) {
                        val sStart = cal.timeInMillis
                        cal.add(java.util.Calendar.DAY_OF_YEAR, stepDays - 1)
                        val sEnd = minOf(com.example.util.Formatters.getEndOfDay(cal.timeInMillis), endDate)
                        val segOmset = soInPeriod.filter { it.so.tanggal in sStart..sEnd }.sumOf { it.so.total }
                        bars.add(
                            ChartBarData(
                                label = com.example.util.Formatters.formatSingkat(sStart),
                                amount = segOmset,
                                timestamp = sStart
                            )
                        )
                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                        seg++
                    }
                }
            }
        }
        return bars
    }

    // ----------------------------------------------------
    // Initial Seed Data
    // ----------------------------------------------------
    // ----------------------------------------------------
    // Pengaturan & Data Management
    // ----------------------------------------------------
    override fun getSaldoAwalKas(): Flow<Long> = _saldoAwalKasFlow.asStateFlow()

    override suspend fun setSaldoAwalKas(saldoAwal: Long) = withContext(Dispatchers.IO) {
        val nonNegative = saldoAwal.coerceAtLeast(0L)
        prefs?.edit()?.putLong(KEY_SALDO_AWAL_KAS, nonNegative)?.apply()
        _saldoAwalKasFlow.value = nonNegative
    }

    override fun isSampleDataEnabled(): Flow<Boolean> = _sampleDataEnabledFlow.asStateFlow()

    override fun hasChosenInitialSetup(): Flow<Boolean> = _hasChosenSetupFlow.asStateFlow()

    override suspend fun setInitialSetupChoice(useSampleData: Boolean) = withContext(Dispatchers.IO) {
        prefs?.edit()?.putBoolean(KEY_HAS_CHOSEN_SETUP, true)?.apply()
        _hasChosenSetupFlow.value = true
        if (useSampleData) {
            seedSampleData()
        } else {
            prefs?.edit()?.putBoolean(KEY_SAMPLE_DATA_ENABLED, false)?.apply()
            _sampleDataEnabledFlow.value = false
        }
    }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        pembayaranDao.deleteAll()
        pengeluaranDao.deleteAll()
        itemSoDao.deleteAll()
        soDao.deleteAll()
        itemPoDao.deleteAll()
        poDao.deleteAll()
        hargaCustomerDao.deleteAll()
        customerDao.deleteAll()
        distributorDao.deleteAll()
        produkDao.deleteAll()

        prefs?.edit()?.putBoolean(KEY_SAMPLE_DATA_ENABLED, false)?.apply()
        _sampleDataEnabledFlow.value = false
    }

    override suspend fun removeSampleData() = withContext(Dispatchers.IO) {
        clearAllData()
    }

    override suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        // Clear existing data first
        pembayaranDao.deleteAll()
        pengeluaranDao.deleteAll()
        itemSoDao.deleteAll()
        soDao.deleteAll()
        itemPoDao.deleteAll()
        poDao.deleteAll()
        hargaCustomerDao.deleteAll()
        customerDao.deleteAll()
        distributorDao.deleteAll()
        produkDao.deleteAll()

        seedDataInternal()

        prefs?.edit()?.putBoolean(KEY_SAMPLE_DATA_ENABLED, true)?.apply()
        _sampleDataEnabledFlow.value = true
    }

    override suspend fun exportDatabaseBackup(): com.example.util.BackupData = withContext(Dispatchers.IO) {
        val saldoAwal = _saldoAwalKasFlow.value
        com.example.util.BackupData(
            metadata = com.example.util.BackupMetadata(
                appName = "DagangKu",
                version = 1,
                exportedAt = System.currentTimeMillis(),
                saldoAwalKas = saldoAwal
            ),
            produkList = produkDao.getAllList(),
            distributorList = distributorDao.getAllList(),
            customerList = customerDao.getAllList(),
            hargaCustomerList = hargaCustomerDao.getAllList(),
            poList = poDao.getAllList(),
            itemPoList = itemPoDao.getAllList(),
            soList = soDao.getAllList(),
            itemSoList = itemSoDao.getAllList(),
            pembayaranList = pembayaranDao.getAllList(),
            pengeluaranList = pengeluaranDao.getAllList()
        )
    }

    override suspend fun restoreDatabaseBackup(backupData: com.example.util.BackupData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete existing data in reverse dependency order
            pembayaranDao.deleteAll()
            pengeluaranDao.deleteAll()
            itemSoDao.deleteAll()
            soDao.deleteAll()
            itemPoDao.deleteAll()
            poDao.deleteAll()
            hargaCustomerDao.deleteAll()
            customerDao.deleteAll()
            distributorDao.deleteAll()
            produkDao.deleteAll()

            // Insert restored entities in forward dependency order
            if (backupData.produkList.isNotEmpty()) produkDao.insertAll(backupData.produkList)
            if (backupData.distributorList.isNotEmpty()) distributorDao.insertAll(backupData.distributorList)
            if (backupData.customerList.isNotEmpty()) customerDao.insertAll(backupData.customerList)
            if (backupData.hargaCustomerList.isNotEmpty()) hargaCustomerDao.insertAll(backupData.hargaCustomerList)
            if (backupData.poList.isNotEmpty()) poDao.insertAll(backupData.poList)
            if (backupData.itemPoList.isNotEmpty()) itemPoDao.insertAll(backupData.itemPoList)
            if (backupData.soList.isNotEmpty()) soDao.insertAll(backupData.soList)
            if (backupData.itemSoList.isNotEmpty()) itemSoDao.insertAll(backupData.itemSoList)
            if (backupData.pembayaranList.isNotEmpty()) pembayaranDao.insertAll(backupData.pembayaranList)
            if (backupData.pengeluaranList.isNotEmpty()) pengeluaranDao.insertAll(backupData.pengeluaranList)

            // Restore Saldo Awal
            val restoredSaldo = backupData.metadata.saldoAwalKas.coerceAtLeast(0L)
            prefs?.edit()?.putLong(KEY_SALDO_AWAL_KAS, restoredSaldo)?.apply()
            _saldoAwalKasFlow.value = restoredSaldo

            prefs?.edit()?.putBoolean(KEY_SAMPLE_DATA_ENABLED, false)?.apply()
            _sampleDataEnabledFlow.value = false

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        // Requirement 4: Do NOT auto-seed on first launch.
        // Seeding is only performed upon explicit user choice ("Pakai data contoh" or settings toggle).
    }

    private suspend fun seedDataInternal() {

        // Seed Distributors
        val dist1Id = distributorDao.insert(
            Distributor(
                nama = "PT Pangan Makmur Abadi",
                noHp = "0812-3456-7890",
                alamat = "Jl. Industri Pergudangan No. 12, Surabaya"
            )
        )
        val dist2Id = distributorDao.insert(
            Distributor(
                nama = "CV Sumber Berkah Sembako",
                noHp = "0813-8899-1122",
                alamat = "Komp. Pasar Induk Blok C-4, Sidoarjo"
            )
        )
        val dist3Id = distributorDao.insert(
            Distributor(
                nama = "UD Jaya Sentosa Mandiri",
                noHp = "0857-1234-5678",
                alamat = "Jl. Raya Gedangan No. 88, Gresik"
            )
        )

        // Seed Customers
        val cust1Id = customerDao.insert(
            Customer(
                nama = "Toko Berkah Barokah",
                noHp = "0819-2233-4455",
                alamat = "Jl. Karang Menjangan No. 15, Surabaya",
                persenKomisi = 2.5
            )
        )
        val cust2Id = customerDao.insert(
            Customer(
                nama = "Warung Bu Siti",
                noHp = "0878-5544-3322",
                alamat = "Pasar Pucang Anom Kios B-10, Surabaya",
                persenKomisi = 0.0
            )
        )
        val cust3Id = customerDao.insert(
            Customer(
                nama = "RM Padang Minang Saiyo",
                noHp = "0812-9988-7766",
                alamat = "Jl. Manyar Kertoarjo No. 42, Surabaya",
                persenKomisi = 3.0
            )
        )

        // Seed Products
        val prodBerasId = produkDao.insert(
            Produk(
                nama = "Beras Pandan Wangi 5 Kg",
                satuan = "Sak",
                hargaDasar = 68000L,
                hargaJual = 78000L,
                stokMinimum = 10
            )
        )
        val prodMinyakId = produkDao.insert(
            Produk(
                nama = "Minyak Goreng SunCo 2L",
                satuan = "Pcs",
                hargaDasar = 32000L,
                hargaJual = 38000L,
                stokMinimum = 15
            )
        )
        val prodGulaId = produkDao.insert(
            Produk(
                nama = "Gula Pasir Gulaku 1 Kg",
                satuan = "Kg",
                hargaDasar = 14500L,
                hargaJual = 17500L,
                stokMinimum = 20
            )
        )
        val prodTelurId = produkDao.insert(
            Produk(
                nama = "Telur Ayam Negeri 1 Kg",
                satuan = "Kg",
                hargaDasar = 26000L,
                hargaJual = 30000L,
                stokMinimum = 15
            )
        )
        val prodIndomieId = produkDao.insert(
            Produk(
                nama = "Indomie Goreng (Dus/40 Pcs)",
                satuan = "Dus",
                hargaDasar = 112000L,
                hargaJual = 125000L,
                stokMinimum = 5
            )
        )
        val prodTepungId = produkDao.insert(
            Produk(
                nama = "Tepung Segitiga Biru 1 Kg",
                satuan = "Pcs",
                hargaDasar = 10500L,
                hargaJual = 13000L,
                stokMinimum = 10
            )
        )

        // Seed Customer Deal Prices (Harga Khusus)
        // Toko Berkah gets special discount on Beras & Minyak
        hargaCustomerDao.insertOrUpdate(
            HargaCustomer(
                customerId = cust1Id,
                produkId = prodBerasId,
                hargaDeal = 75000L // Default 78.000 -> Deal 75.000
            )
        )
        hargaCustomerDao.insertOrUpdate(
            HargaCustomer(
                customerId = cust1Id,
                produkId = prodMinyakId,
                hargaDeal = 36000L // Default 38.000 -> Deal 36.000
            )
        )
        // RM Padang gets special deal on Telur & Minyak
        hargaCustomerDao.insertOrUpdate(
            HargaCustomer(
                customerId = cust3Id,
                produkId = prodTelurId,
                hargaDeal = 28500L // Default 30.000 -> Deal 28.500
            )
        )

        // Seed Initial PO (Pembelian Stok Awal)
        val now = System.currentTimeMillis()
        val po1Items = listOf(
            ItemPO(poId = 0, produkId = prodBerasId, qty = 50, hargaBeli = 68000L),
            ItemPO(poId = 0, produkId = prodMinyakId, qty = 60, hargaBeli = 32000L),
            ItemPO(poId = 0, produkId = prodGulaId, qty = 80, hargaBeli = 14500L),
            ItemPO(poId = 0, produkId = prodTelurId, qty = 40, hargaBeli = 26000L),
            ItemPO(poId = 0, produkId = prodIndomieId, qty = 25, hargaBeli = 112000L),
            ItemPO(poId = 0, produkId = prodTepungId, qty = 30, hargaBeli = 10500L)
        )
        val po1Total = po1Items.sumOf { it.qty.toLong() * it.hargaBeli }
        val po1Id = poDao.insert(
            PO(
                nomor = "PO-20260920-001",
                distributorId = dist1Id,
                tanggal = now - (10L * 24 * 3600 * 1000),
                total = po1Total
            )
        )
        itemPoDao.insertAll(po1Items.map { it.copy(poId = po1Id) })

        // Partial payment on PO 1
        pembayaranDao.insert(
            Pembayaran(
                tipe = "DISTRIBUTOR",
                refId = po1Id,
                nominal = Math.round(po1Total.toDouble() * 0.6),
                tanggal = now - (9L * 24 * 3600 * 1000),
                metode = "Transfer Bank",
                catatan = "Pembayaran uang muka 60%"
            )
        )

        // Seed Initial SO (Penjualan)
        val so1Items = listOf(
            ItemSO(soId = 0, produkId = prodBerasId, qty = 10, harga = 75000L, hppSaatJual = 68000L),
            ItemSO(soId = 0, produkId = prodMinyakId, qty = 12, harga = 36000L, hppSaatJual = 32000L)
        )
        val so1Total = so1Items.sumOf { it.qty.toLong() * it.harga }
        val so1Id = soDao.insert(
            SO(
                nomor = "SO-20260925-001",
                customerId = cust1Id,
                tanggal = now - (5L * 24 * 3600 * 1000),
                total = so1Total
            )
        )
        itemSoDao.insertAll(so1Items.map { it.copy(soId = so1Id) })

        // Full payment for SO 1
        pembayaranDao.insert(
            Pembayaran(
                tipe = "CUSTOMER",
                refId = so1Id,
                nominal = so1Total,
                tanggal = now - (4L * 24 * 3600 * 1000),
                metode = "Transfer Bank",
                catatan = "Lunas transfer BCA"
            )
        )

        // Seed SO 2 (Unpaid / Piutang)
        val so2Items = listOf(
            ItemSO(soId = 0, produkId = prodTelurId, qty = 15, harga = 28500L, hppSaatJual = 26000L),
            ItemSO(soId = 0, produkId = prodIndomieId, qty = 5, harga = 125000L, hppSaatJual = 112000L)
        )
        val so2Total = so2Items.sumOf { it.qty.toLong() * it.harga }
        val so2Id = soDao.insert(
            SO(
                nomor = "SO-20260928-002",
                customerId = cust3Id,
                tanggal = now - (2L * 24 * 3600 * 1000),
                total = so2Total
            )
        )
        itemSoDao.insertAll(so2Items.map { it.copy(soId = so2Id) })

        // Partial payment for SO 2
        pembayaranDao.insert(
            Pembayaran(
                tipe = "CUSTOMER",
                refId = so2Id,
                nominal = 500000L,
                tanggal = now - (1L * 24 * 3600 * 1000),
                metode = "Tunai",
                catatan = "DP Tunai"
            )
        )

        // Seed initial operational expenses
        pengeluaranDao.insert(
            Pengeluaran(
                kategori = "Listrik & Air",
                nominal = 350000L,
                tanggal = now - (3L * 24 * 3600 * 1000),
                keterangan = "Listrik gudang & toko bulan berjalan"
            )
        )
        pengeluaranDao.insert(
            Pengeluaran(
                kategori = "Transportasi",
                nominal = 120000L,
                tanggal = now - (1L * 24 * 3600 * 1000),
                keterangan = "BBM armada pickup antar pesanan"
            )
        )
    }
}
