package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

interface DagangKuRepository {
    // Produk
    fun getProdukList(): Flow<List<ProdukWithStock>>
    fun getProdukByIdFlow(id: Long): Flow<Produk?>
    suspend fun getProdukById(id: Long): Produk?
    suspend fun saveProduk(produk: Produk): Long
    suspend fun updateProduk(produk: Produk)
    suspend fun deleteProduk(produk: Produk)
    suspend fun getProdukStock(produkId: Long): Int

    // Distributor
    fun getDistributorList(): Flow<List<Distributor>>
    suspend fun getDistributorById(id: Long): Distributor?
    suspend fun saveDistributor(distributor: Distributor): Long
    suspend fun updateDistributor(distributor: Distributor)
    suspend fun deleteDistributor(distributor: Distributor)

    // Customer
    fun getCustomerList(): Flow<List<Customer>>
    suspend fun getCustomerById(id: Long): Customer?
    fun getCustomerByIdFlow(id: Long): Flow<Customer?>
    suspend fun saveCustomer(customer: Customer): Long
    suspend fun updateCustomer(customer: Customer)
    suspend fun deleteCustomer(customer: Customer)

    // Harga Customer (Deal)
    fun getHargaCustomerList(customerId: Long): Flow<List<CustomerDealItem>>
    suspend fun getEffectivePrice(customerId: Long, produkId: Long): Long
    suspend fun setCustomerDealPrice(customerId: Long, produkId: Long, hargaDeal: Long)
    suspend fun removeCustomerDealPrice(customerId: Long, produkId: Long)

    // PO (Pembelian)
    fun getPoList(): Flow<List<PoWithDetails>>
    fun getPoDetailsFlow(poId: Long): Flow<PoWithDetails?>
    suspend fun getPoDetails(poId: Long): PoWithDetails?
    suspend fun createPO(po: PO, items: List<ItemPO>): Result<Long>
    suspend fun deletePO(poId: Long)

    // SO (Penjualan)
    fun getSoList(): Flow<List<SoWithDetails>>
    fun getSoDetailsFlow(soId: Long): Flow<SoWithDetails?>
    suspend fun getSoDetails(soId: Long): SoWithDetails?
    suspend fun createSO(so: SO, items: List<ItemSO>): Result<Long>
    suspend fun deleteSO(soId: Long)

    // Pembayaran
    fun getPaymentsForRef(tipe: String, refId: Long): Flow<List<Pembayaran>>
    fun getAllPayments(): Flow<List<Pembayaran>>
    suspend fun addPembayaran(pembayaran: Pembayaran): Long
    suspend fun deletePembayaran(pembayaran: Pembayaran)

    // Pengeluaran Operasional
    fun getPengeluaranList(): Flow<List<Pengeluaran>>
    suspend fun addPengeluaran(pengeluaran: Pengeluaran): Long
    suspend fun deletePengeluaran(pengeluaran: Pengeluaran)

    // Kas & Arus Kas
    fun getBukuKas(): Flow<List<KasTransaction>>
    fun getKasSummary(): Flow<KasSummary>

    // Dashboard
    fun getDashboardSummary(startDate: Long = 0L, endDate: Long = Long.MAX_VALUE): Flow<DashboardSummary>

    // Laporan Keuangan (Step 3)
    fun getLaporanKeuangan(startDate: Long, endDate: Long, periodType: PeriodType): Flow<LaporanKeuanganData>

    // Validation checks for deletion
    suspend fun canDeleteProduk(produkId: Long): Boolean
    suspend fun canDeleteDistributor(distributorId: Long): Boolean
    suspend fun canDeleteCustomer(customerId: Long): Boolean

    // Pengaturan & Data Management
    fun getSaldoAwalKas(): Flow<Long>
    suspend fun setSaldoAwalKas(saldoAwal: Long)
    fun isSampleDataEnabled(): Flow<Boolean>
    fun hasChosenInitialSetup(): Flow<Boolean>
    suspend fun setInitialSetupChoice(useSampleData: Boolean)
    suspend fun clearAllData()
    suspend fun seedSampleData()
    suspend fun removeSampleData()

    // Backup & Restore
    suspend fun exportDatabaseBackup(): com.example.util.BackupData
    suspend fun restoreDatabaseBackup(backupData: com.example.util.BackupData): Result<Unit>

    // Seed
    suspend fun seedInitialDataIfNeeded()
}
