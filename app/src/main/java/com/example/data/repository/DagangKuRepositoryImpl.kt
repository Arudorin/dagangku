package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class DagangKuRepositoryImpl(
    private val db: AppDatabase
) : DagangKuRepository {

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
        produkDao.delete(produk)
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
        distributorDao.delete(distributor)
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
        customerDao.delete(customer)
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

    override suspend fun getEffectivePrice(customerId: Long, produkId: Long): Double = withContext(Dispatchers.IO) {
        val deal = hargaCustomerDao.getDeal(customerId, produkId)
        if (deal != null) {
            deal.hargaDeal
        } else {
            val prod = produkDao.getById(produkId)
            prod?.hargaJual ?: 0.0
        }
    }

    override suspend fun setCustomerDealPrice(customerId: Long, produkId: Long, hargaDeal: Double) = withContext(Dispatchers.IO) {
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

            // 1. Update hargaDasar of each product using Weighted Average Cost
            // Formula: ((stokLama * hargaDasarLama) + (qtyBeli * hargaBeli)) / (stokLama + qtyBeli)
            for (item in items) {
                val product = produkDao.getById(item.produkId)
                if (product != null) {
                    val currentStock = getProdukStock(item.produkId)
                    val newHargaDasar = if (currentStock <= 0) {
                        item.hargaBeli
                    } else {
                        ((currentStock * product.hargaDasar) + (item.qty * item.hargaBeli)) / (currentStock + item.qty)
                    }
                    produkDao.updateHargaDasar(item.produkId, newHargaDasar)
                }
            }

            // 2. Insert PO
            val poId = poDao.insert(po)

            // 3. Insert PO Items with assigned poId
            val itemsWithPoId = items.map { it.copy(poId = poId) }
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

            // CRITICAL BUSINESS RULE: Block the SO if stock is insufficient
            for (item in items) {
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

            // Insert SO
            val soId = soDao.insert(so)

            // Insert SO Items (this decreases available stock dynamically)
            val itemsWithSoId = items.map { it.copy(soId = soId) }
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
            pengeluaranDao.getAll()
        ) { custPayments, distPayments, expenses ->
            val totalIn = custPayments.sumOf { it.nominal }
            val totalOutDist = distPayments.sumOf { it.nominal }
            val totalOutExp = expenses.sumOf { it.nominal }
            KasSummary(
                totalMasuk = totalIn,
                totalKeluarDistributor = totalOutDist,
                totalKeluarOperasional = totalOutExp
            )
        }
    }

    // ----------------------------------------------------
    // Dashboard Stats
    // ----------------------------------------------------
    override fun getDashboardSummary(): Flow<DashboardSummary> {
        return combine(
            getSoList(),
            getPoList(),
            getProdukList(),
            pengeluaranDao.getAll(),
            customerDao.getAll()
        ) { soList, poList, produkList, expenses, customers ->
            val totalSales = soList.sumOf { it.so.total }
            val totalPurchases = poList.sumOf { it.po.total }
            val totalPiutang = soList.sumOf { it.sisaPiutang }
            val totalHutang = poList.sumOf { it.sisaHutang }
            val lowStockCount = produkList.count { it.isLowStock }

            val totalHpp = soList.sumOf { soDetail ->
                soDetail.items.sumOf { it.item.qty * (it.produk?.hargaDasar ?: 0.0) }
            }
            val totalLabaKotor = totalSales - totalHpp
            val totalExp = expenses.sumOf { it.nominal }
            val totalKomisi = soList.sumOf { it.komisiNominal }
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
            val soMap = soDetailsList.associateBy { it.so.id }

            // 1. Omset in period
            val soInPeriod = soDetailsList.filter { it.so.tanggal in startDate..endDate }
            val omset = soInPeriod.sumOf { it.so.total }

            // 2. HPP in period = sum(qty x hargaDasar)
            val hpp = soInPeriod.sumOf { soDetail ->
                soDetail.items.sumOf { itemWithProd ->
                    itemWithProd.item.qty * (itemWithProd.produk?.hargaDasar ?: 0.0)
                }
            }

            // 3. Laba Kotor = Omset - HPP
            val labaKotor = omset - hpp

            // 4. Komisi per customer = persenKomisi x total SO yang sudah terbayar (from payments received in period)
            val paymentsInPeriod = custPayments.filter { it.tanggal in startDate..endDate }
            val custPaidMap = mutableMapOf<Long, Double>()
            for (pay in paymentsInPeriod) {
                val soDetail = soMap[pay.refId]
                if (soDetail != null) {
                    val custId = soDetail.so.customerId
                    custPaidMap[custId] = (custPaidMap[custId] ?: 0.0) + pay.nominal
                }
            }

            val commissionList = mutableListOf<CustomerCommissionItem>()
            var totalKomisi = 0.0
            for (cust in customers) {
                val paidAmount = custPaidMap[cust.id] ?: 0.0
                if (cust.persenKomisi > 0 || paidAmount > 0) {
                    val komisi = if (cust.persenKomisi > 0) (paidAmount * cust.persenKomisi) / 100.0 else 0.0
                    totalKomisi += komisi
                    if (paidAmount > 0 || cust.persenKomisi > 0) {
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
    override suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingProducts = produkDao.getAll().first()
        if (existingProducts.isNotEmpty()) return@withContext

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
                hargaDasar = 68000.0,
                hargaJual = 78000.0,
                stokMinimum = 10
            )
        )
        val prodMinyakId = produkDao.insert(
            Produk(
                nama = "Minyak Goreng SunCo 2L",
                satuan = "Pcs",
                hargaDasar = 32000.0,
                hargaJual = 38000.0,
                stokMinimum = 15
            )
        )
        val prodGulaId = produkDao.insert(
            Produk(
                nama = "Gula Pasir Gulaku 1 Kg",
                satuan = "Kg",
                hargaDasar = 14500.0,
                hargaJual = 17500.0,
                stokMinimum = 20
            )
        )
        val prodTelurId = produkDao.insert(
            Produk(
                nama = "Telur Ayam Negeri 1 Kg",
                satuan = "Kg",
                hargaDasar = 26000.0,
                hargaJual = 30000.0,
                stokMinimum = 15
            )
        )
        val prodIndomieId = produkDao.insert(
            Produk(
                nama = "Indomie Goreng (Dus/40 Pcs)",
                satuan = "Dus",
                hargaDasar = 112000.0,
                hargaJual = 125000.0,
                stokMinimum = 5
            )
        )
        val prodTepungId = produkDao.insert(
            Produk(
                nama = "Tepung Segitiga Biru 1 Kg",
                satuan = "Pcs",
                hargaDasar = 10500.0,
                hargaJual = 13000.0,
                stokMinimum = 10
            )
        )

        // Seed Customer Deal Prices (Harga Khusus)
        // Toko Berkah gets special discount on Beras & Minyak
        hargaCustomerDao.insertOrUpdate(
            HargaCustomer(
                customerId = cust1Id,
                produkId = prodBerasId,
                hargaDeal = 75000.0 // Default 78.000 -> Deal 75.000
            )
        )
        hargaCustomerDao.insertOrUpdate(
            HargaCustomer(
                customerId = cust1Id,
                produkId = prodMinyakId,
                hargaDeal = 36000.0 // Default 38.000 -> Deal 36.000
            )
        )
        // RM Padang gets special deal on Telur & Minyak
        hargaCustomerDao.insertOrUpdate(
            HargaCustomer(
                customerId = cust3Id,
                produkId = prodTelurId,
                hargaDeal = 28500.0 // Default 30.000 -> Deal 28.500
            )
        )

        // Seed Initial PO (Pembelian Stok Awal)
        val now = System.currentTimeMillis()
        val po1Items = listOf(
            ItemPO(poId = 0, produkId = prodBerasId, qty = 50, hargaBeli = 68000.0),
            ItemPO(poId = 0, produkId = prodMinyakId, qty = 60, hargaBeli = 32000.0),
            ItemPO(poId = 0, produkId = prodGulaId, qty = 80, hargaBeli = 14500.0),
            ItemPO(poId = 0, produkId = prodTelurId, qty = 40, hargaBeli = 26000.0),
            ItemPO(poId = 0, produkId = prodIndomieId, qty = 25, hargaBeli = 112000.0),
            ItemPO(poId = 0, produkId = prodTepungId, qty = 30, hargaBeli = 10500.0)
        )
        val po1Total = po1Items.sumOf { it.qty * it.hargaBeli }
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
                nominal = po1Total * 0.6,
                tanggal = now - (9L * 24 * 3600 * 1000),
                metode = "Transfer Bank",
                catatan = "Pembayaran uang muka 60%"
            )
        )

        // Seed Initial SO (Penjualan)
        val so1Items = listOf(
            ItemSO(soId = 0, produkId = prodBerasId, qty = 10, harga = 75000.0),
            ItemSO(soId = 0, produkId = prodMinyakId, qty = 12, harga = 36000.0)
        )
        val so1Total = so1Items.sumOf { it.qty * it.harga }
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
            ItemSO(soId = 0, produkId = prodTelurId, qty = 15, harga = 28500.0),
            ItemSO(soId = 0, produkId = prodIndomieId, qty = 5, harga = 125000.0)
        )
        val so2Total = so2Items.sumOf { it.qty * it.harga }
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
                nominal = 500000.0,
                tanggal = now - (1L * 24 * 3600 * 1000),
                metode = "Tunai",
                catatan = "DP Tunai"
            )
        )

        // Seed initial operational expenses
        pengeluaranDao.insert(
            Pengeluaran(
                kategori = "Listrik & Air",
                nominal = 350000.0,
                tanggal = now - (3L * 24 * 3600 * 1000),
                keterangan = "Listrik gudang & toko bulan berjalan"
            )
        )
        pengeluaranDao.insert(
            Pengeluaran(
                kategori = "Transportasi",
                nominal = 120000.0,
                tanggal = now - (1L * 24 * 3600 * 1000),
                keterangan = "BBM armada pickup antar pesanan"
            )
        )
    }
}
