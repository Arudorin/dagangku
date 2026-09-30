package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.DagangKuRepository
import com.example.data.repository.DagangKuRepositoryImpl
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DagangKuRepository = DagangKuRepositoryImpl(
        AppDatabase.getInstance(application)
    )

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    // Dashboard
    val dashboardSummary: StateFlow<DashboardSummary> = repository.getDashboardSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardSummary()
        )

    // Produk & Stock
    val produkList: StateFlow<List<ProdukWithStock>> = repository.getProdukList()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Distributor
    val distributorList: StateFlow<List<Distributor>> = repository.getDistributorList()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Customer
    val customerList: StateFlow<List<Customer>> = repository.getCustomerList()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // PO
    val poList: StateFlow<List<PoWithDetails>> = repository.getPoList()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // SO
    val soList: StateFlow<List<SoWithDetails>> = repository.getSoList()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Pengeluaran
    val pengeluaranList: StateFlow<List<Pengeluaran>> = repository.getPengeluaranList()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Buku Kas & Summary
    val bukuKas: StateFlow<List<KasTransaction>> = repository.getBukuKas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val kasSummary: StateFlow<KasSummary> = repository.getKasSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = KasSummary()
        )

    // Laporan Keuangan (Step 3) Period Filter & Data
    private val _selectedPeriodType = MutableStateFlow(PeriodType.BULANAN)
    val selectedPeriodType: StateFlow<PeriodType> = _selectedPeriodType.asStateFlow()

    private val _customDateRange = MutableStateFlow(
        Pair(
            com.example.util.Formatters.getStartOfMonth(),
            com.example.util.Formatters.getEndOfMonth()
        )
    )
    val customDateRange: StateFlow<Pair<Long, Long>> = _customDateRange.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val laporanKeuanganData: StateFlow<LaporanKeuanganData> = combine(
        _selectedPeriodType,
        _customDateRange
    ) { periodType, customRange ->
        val (start, end) = when (periodType) {
            PeriodType.HARIAN -> Pair(
                com.example.util.Formatters.getStartOfDay(),
                com.example.util.Formatters.getEndOfDay()
            )
            PeriodType.MINGGUAN -> Pair(
                com.example.util.Formatters.getStartOfWeek(),
                com.example.util.Formatters.getEndOfDay()
            )
            PeriodType.BULANAN -> Pair(
                com.example.util.Formatters.getStartOfMonth(),
                com.example.util.Formatters.getEndOfMonth()
            )
            PeriodType.CUSTOM -> customRange
        }
        Triple(start, end, periodType)
    }.flatMapLatest { (start, end, periodType) ->
        repository.getLaporanKeuangan(start, end, periodType)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LaporanKeuanganData()
    )

    fun setPeriodType(type: PeriodType) {
        _selectedPeriodType.value = type
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = Pair(start, end)
        _selectedPeriodType.value = PeriodType.CUSTOM
    }

    // ----------------------------------------------------
    // Produk Actions
    // ----------------------------------------------------
    fun saveProduk(
        id: Long = 0,
        nama: String,
        satuan: String,
        hargaDasar: Double,
        hargaJual: Double,
        stokMinimum: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (nama.isBlank()) {
                _eventFlow.emit(UiEvent.ShowMessage("Nama produk tidak boleh kosong", isError = true))
                return@launch
            }
            if (hargaJual < 0 || hargaDasar < 0) {
                _eventFlow.emit(UiEvent.ShowMessage("Harga tidak boleh negatif", isError = true))
                return@launch
            }
            val produk = Produk(
                id = id,
                nama = nama.trim(),
                satuan = satuan.ifBlank { "Pcs" }.trim(),
                hargaDasar = hargaDasar,
                hargaJual = hargaJual,
                stokMinimum = stokMinimum.coerceAtLeast(0)
            )
            if (id == 0L) {
                repository.saveProduk(produk)
                _eventFlow.emit(UiEvent.ShowMessage("Produk '${produk.nama}' berhasil ditambahkan"))
            } else {
                repository.updateProduk(produk)
                _eventFlow.emit(UiEvent.ShowMessage("Produk '${produk.nama}' berhasil diperbarui"))
            }
            onSuccess()
        }
    }

    fun deleteProduk(produk: Produk, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProduk(produk)
            _eventFlow.emit(UiEvent.ShowMessage("Produk '${produk.nama}' telah dihapus"))
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Distributor Actions
    // ----------------------------------------------------
    fun saveDistributor(
        id: Long = 0,
        nama: String,
        noHp: String,
        alamat: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (nama.isBlank()) {
                _eventFlow.emit(UiEvent.ShowMessage("Nama distributor tidak boleh kosong", isError = true))
                return@launch
            }
            val distributor = Distributor(
                id = id,
                nama = nama.trim(),
                noHp = noHp.trim(),
                alamat = alamat.trim()
            )
            if (id == 0L) {
                repository.saveDistributor(distributor)
                _eventFlow.emit(UiEvent.ShowMessage("Distributor '${distributor.nama}' berhasil ditambahkan"))
            } else {
                repository.updateDistributor(distributor)
                _eventFlow.emit(UiEvent.ShowMessage("Distributor '${distributor.nama}' berhasil diperbarui"))
            }
            onSuccess()
        }
    }

    fun deleteDistributor(distributor: Distributor, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteDistributor(distributor)
            _eventFlow.emit(UiEvent.ShowMessage("Distributor '${distributor.nama}' telah dihapus"))
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Customer Actions
    // ----------------------------------------------------
    fun saveCustomer(
        id: Long = 0,
        nama: String,
        noHp: String,
        alamat: String,
        persenKomisi: Double,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (nama.isBlank()) {
                _eventFlow.emit(UiEvent.ShowMessage("Nama pelanggan tidak boleh kosong", isError = true))
                return@launch
            }
            val customer = Customer(
                id = id,
                nama = nama.trim(),
                noHp = noHp.trim(),
                alamat = alamat.trim(),
                persenKomisi = persenKomisi.coerceAtLeast(0.0)
            )
            if (id == 0L) {
                repository.saveCustomer(customer)
                _eventFlow.emit(UiEvent.ShowMessage("Pelanggan '${customer.nama}' berhasil ditambahkan"))
            } else {
                repository.updateCustomer(customer)
                _eventFlow.emit(UiEvent.ShowMessage("Pelanggan '${customer.nama}' berhasil diperbarui"))
            }
            onSuccess()
        }
    }

    fun deleteCustomer(customer: Customer, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _eventFlow.emit(UiEvent.ShowMessage("Pelanggan '${customer.nama}' telah dihapus"))
            onSuccess()
        }
    }

    // Customer Deal Prices
    fun getCustomerDealPrices(customerId: Long): Flow<List<CustomerDealItem>> =
        repository.getHargaCustomerList(customerId)

    fun setCustomerDealPrice(customerId: Long, produkId: Long, hargaDeal: Double) {
        viewModelScope.launch {
            if (hargaDeal <= 0) {
                _eventFlow.emit(UiEvent.ShowMessage("Harga deal harus lebih dari 0", isError = true))
                return@launch
            }
            repository.setCustomerDealPrice(customerId, produkId, hargaDeal)
            _eventFlow.emit(UiEvent.ShowMessage("Harga deal berhasil disimpan"))
        }
    }

    fun removeCustomerDealPrice(customerId: Long, produkId: Long) {
        viewModelScope.launch {
            repository.removeCustomerDealPrice(customerId, produkId)
            _eventFlow.emit(UiEvent.ShowMessage("Harga deal khusus telah dihapus"))
        }
    }

    suspend fun getEffectivePriceForCustomer(customerId: Long, produkId: Long): Double {
        return repository.getEffectivePrice(customerId, produkId)
    }

    // ----------------------------------------------------
    // PO Actions
    // ----------------------------------------------------
    fun getPoDetailsFlow(poId: Long): Flow<PoWithDetails?> = repository.getPoDetailsFlow(poId)

    fun createPO(
        nomor: String,
        distributorId: Long,
        tanggal: Long,
        items: List<CartItemPo>,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (distributorId <= 0) {
                _eventFlow.emit(UiEvent.ShowMessage("Pilih distributor terlebih dahulu", isError = true))
                return@launch
            }
            if (items.isEmpty()) {
                _eventFlow.emit(UiEvent.ShowMessage("Tambahkan minimal 1 produk", isError = true))
                return@launch
            }
            val total = items.sumOf { it.subtotal }
            val po = PO(
                nomor = nomor.trim(),
                distributorId = distributorId,
                tanggal = tanggal,
                total = total
            )
            val poItems = items.map {
                ItemPO(
                    poId = 0,
                    produkId = it.produk.id,
                    qty = it.qty,
                    hargaBeli = it.hargaBeli
                )
            }
            val result = repository.createPO(po, poItems)
            if (result.isSuccess) {
                val newId = result.getOrThrow()
                _eventFlow.emit(UiEvent.ShowMessage("PO '$nomor' berhasil dibuat! Stok & HPP telah diperbarui."))
                onSuccess(newId)
            } else {
                _eventFlow.emit(UiEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Gagal membuat PO", isError = true))
            }
        }
    }

    fun deletePO(poId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePO(poId)
            _eventFlow.emit(UiEvent.ShowMessage("Pesanan Pembelian (PO) telah dihapus"))
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // SO Actions
    // ----------------------------------------------------
    fun getSoDetailsFlow(soId: Long): Flow<SoWithDetails?> = repository.getSoDetailsFlow(soId)

    fun createSO(
        nomor: String,
        customerId: Long,
        tanggal: Long,
        items: List<CartItemSo>,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (customerId <= 0) {
                _eventFlow.emit(UiEvent.ShowMessage("Pilih pelanggan terlebih dahulu", isError = true))
                return@launch
            }
            if (items.isEmpty()) {
                _eventFlow.emit(UiEvent.ShowMessage("Tambahkan minimal 1 produk", isError = true))
                return@launch
            }
            // Check for insufficient stock items
            val insufficient = items.firstOrNull { it.isInsufficientStock }
            if (insufficient != null) {
                _eventFlow.emit(
                    UiEvent.ShowMessage(
                        "Stok '${insufficient.produk.nama}' tidak cukup! Tersedia: ${insufficient.availableStock} ${insufficient.produk.satuan}, diminta: ${insufficient.qty}",
                        isError = true
                    )
                )
                return@launch
            }

            val total = items.sumOf { it.subtotal }
            val so = SO(
                nomor = nomor.trim(),
                customerId = customerId,
                tanggal = tanggal,
                total = total
            )
            val soItems = items.map {
                ItemSO(
                    soId = 0,
                    produkId = it.produk.id,
                    qty = it.qty,
                    harga = it.hargaJual
                )
            }
            val result = repository.createSO(so, soItems)
            if (result.isSuccess) {
                val newId = result.getOrThrow()
                _eventFlow.emit(UiEvent.ShowMessage("SO '$nomor' berhasil dibuat!"))
                onSuccess(newId)
            } else {
                _eventFlow.emit(UiEvent.ShowMessage(result.exceptionOrNull()?.message ?: "Gagal membuat SO", isError = true))
            }
        }
    }

    fun deleteSO(soId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSO(soId)
            _eventFlow.emit(UiEvent.ShowMessage("Pesanan Penjualan (SO) telah dihapus"))
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Pembayaran Actions
    // ----------------------------------------------------
    fun recordPayment(
        tipe: String, // "CUSTOMER" or "DISTRIBUTOR"
        refId: Long,
        nominal: Double,
        metode: String,
        catatan: String,
        tanggal: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (nominal <= 0) {
                _eventFlow.emit(UiEvent.ShowMessage("Nominal pembayaran harus lebih dari 0", isError = true))
                return@launch
            }
            val payment = Pembayaran(
                tipe = tipe,
                refId = refId,
                nominal = nominal,
                tanggal = tanggal,
                metode = metode.ifBlank { "Tunai" },
                catatan = catatan.trim()
            )
            repository.addPembayaran(payment)
            val typeDesc = if (tipe == "CUSTOMER") "Pembayaran pelanggan" else "Pembayaran ke distributor"
            _eventFlow.emit(UiEvent.ShowMessage("$typeDesc berhasil dicatat"))
            onSuccess()
        }
    }

    fun deletePayment(pembayaran: Pembayaran) {
        viewModelScope.launch {
            repository.deletePembayaran(pembayaran)
            _eventFlow.emit(UiEvent.ShowMessage("Catatan pembayaran telah dihapus"))
        }
    }

    // ----------------------------------------------------
    // Pengeluaran Operasional Actions
    // ----------------------------------------------------
    fun addPengeluaran(
        kategori: String,
        nominal: Double,
        keterangan: String,
        tanggal: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (nominal <= 0) {
                _eventFlow.emit(UiEvent.ShowMessage("Nominal pengeluaran harus lebih dari 0", isError = true))
                return@launch
            }
            val pengeluaran = Pengeluaran(
                kategori = kategori.ifBlank { "Operasional" },
                nominal = nominal,
                tanggal = tanggal,
                keterangan = keterangan.trim()
            )
            repository.addPengeluaran(pengeluaran)
            _eventFlow.emit(UiEvent.ShowMessage("Pengeluaran operasional berhasil dicatat"))
            onSuccess()
        }
    }

    fun deletePengeluaran(pengeluaran: Pengeluaran) {
        viewModelScope.launch {
            repository.deletePengeluaran(pengeluaran)
            _eventFlow.emit(UiEvent.ShowMessage("Catatan pengeluaran telah dihapus"))
        }
    }
}
