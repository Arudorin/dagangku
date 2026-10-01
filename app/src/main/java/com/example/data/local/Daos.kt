package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdukDao {
    @Query("SELECT * FROM produk ORDER BY nama ASC")
    fun getAll(): Flow<List<Produk>>

    @Query("SELECT * FROM produk WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Produk?

    @Query("SELECT * FROM produk WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<Produk?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produk: Produk): Long

    @Update
    suspend fun update(produk: Produk)

    @Delete
    suspend fun delete(produk: Produk)

    @Query("UPDATE produk SET hargaDasar = :newHargaDasar WHERE id = :id")
    suspend fun updateHargaDasar(id: Long, newHargaDasar: Long)
}

@Dao
interface DistributorDao {
    @Query("SELECT * FROM distributor ORDER BY nama ASC")
    fun getAll(): Flow<List<Distributor>>

    @Query("SELECT * FROM distributor WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Distributor?

    @Query("SELECT * FROM distributor WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<Distributor?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(distributor: Distributor): Long

    @Update
    suspend fun update(distributor: Distributor)

    @Delete
    suspend fun delete(distributor: Distributor)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customer ORDER BY nama ASC")
    fun getAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customer WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Customer?

    @Query("SELECT * FROM customer WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<Customer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)
}

@Dao
interface HargaCustomerDao {
    @Query("SELECT * FROM harga_customer WHERE customerId = :customerId")
    fun getByCustomer(customerId: Long): Flow<List<HargaCustomer>>

    @Query("SELECT * FROM harga_customer WHERE customerId = :customerId")
    suspend fun getByCustomerSync(customerId: Long): List<HargaCustomer>

    @Query("SELECT * FROM harga_customer WHERE customerId = :customerId AND produkId = :produkId LIMIT 1")
    suspend fun getDeal(customerId: Long, produkId: Long): HargaCustomer?

    @Query("SELECT * FROM harga_customer WHERE customerId = :customerId AND produkId = :produkId LIMIT 1")
    fun getDealFlow(customerId: Long, produkId: Long): Flow<HargaCustomer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(hargaCustomer: HargaCustomer): Long

    @Delete
    suspend fun delete(hargaCustomer: HargaCustomer)

    @Query("DELETE FROM harga_customer WHERE customerId = :customerId AND produkId = :produkId")
    suspend fun deleteDeal(customerId: Long, produkId: Long)
}

@Dao
interface PoDao {
    @Query("SELECT * FROM po ORDER BY tanggal DESC, id DESC")
    fun getAll(): Flow<List<PO>>

    @Query("SELECT * FROM po WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PO?

    @Query("SELECT * FROM po WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<PO?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(po: PO): Long

    @Delete
    suspend fun delete(po: PO)

    @Query("SELECT COUNT(*) FROM po")
    fun getPoCount(): Flow<Int>
}

@Dao
interface ItemPoDao {
    @Query("SELECT * FROM item_po WHERE poId = :poId")
    fun getItemsForPo(poId: Long): Flow<List<ItemPO>>

    @Query("SELECT * FROM item_po WHERE poId = :poId")
    suspend fun getItemsForPoSync(poId: Long): List<ItemPO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemPO>)

    @Query("DELETE FROM item_po WHERE poId = :poId")
    suspend fun deleteByPoId(poId: Long)

    @Query("SELECT COALESCE(SUM(qty), 0) FROM item_po WHERE produkId = :produkId")
    suspend fun getTotalQtyForProduct(produkId: Long): Int

    @Query("SELECT COALESCE(SUM(qty), 0) FROM item_po WHERE produkId = :produkId")
    fun getTotalQtyForProductFlow(produkId: Long): Flow<Int>

    @Query("SELECT * FROM item_po")
    fun getAllItems(): Flow<List<ItemPO>>
}

@Dao
interface SoDao {
    @Query("SELECT * FROM so ORDER BY tanggal DESC, id DESC")
    fun getAll(): Flow<List<SO>>

    @Query("SELECT * FROM so WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SO?

    @Query("SELECT * FROM so WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<SO?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(so: SO): Long

    @Delete
    suspend fun delete(so: SO)

    @Query("SELECT COUNT(*) FROM so")
    fun getSoCount(): Flow<Int>
}

@Dao
interface ItemSoDao {
    @Query("SELECT * FROM item_so WHERE soId = :soId")
    fun getItemsForSo(soId: Long): Flow<List<ItemSO>>

    @Query("SELECT * FROM item_so WHERE soId = :soId")
    suspend fun getItemsForSoSync(soId: Long): List<ItemSO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemSO>)

    @Query("DELETE FROM item_so WHERE soId = :soId")
    suspend fun deleteBySoId(soId: Long)

    @Query("SELECT COALESCE(SUM(qty), 0) FROM item_so WHERE produkId = :produkId")
    suspend fun getTotalQtyForProduct(produkId: Long): Int

    @Query("SELECT COALESCE(SUM(qty), 0) FROM item_so WHERE produkId = :produkId")
    fun getTotalQtyForProductFlow(produkId: Long): Flow<Int>

    @Query("SELECT * FROM item_so")
    fun getAllItems(): Flow<List<ItemSO>>
}

@Dao
interface PembayaranDao {
    @Query("SELECT * FROM pembayaran ORDER BY tanggal DESC, id DESC")
    fun getAll(): Flow<List<Pembayaran>>

    @Query("SELECT * FROM pembayaran WHERE tipe = :tipe AND refId = :refId ORDER BY tanggal DESC")
    fun getByTypeAndRef(tipe: String, refId: Long): Flow<List<Pembayaran>>

    @Query("SELECT * FROM pembayaran WHERE tipe = :tipe AND refId = :refId ORDER BY tanggal DESC")
    suspend fun getByTypeAndRefSync(tipe: String, refId: Long): List<Pembayaran>

    @Query("SELECT * FROM pembayaran WHERE tipe = :tipe ORDER BY tanggal DESC")
    fun getByType(tipe: String): Flow<List<Pembayaran>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pembayaran: Pembayaran): Long

    @Delete
    suspend fun delete(pembayaran: Pembayaran)

    @Query("DELETE FROM pembayaran WHERE tipe = :tipe AND refId = :refId")
    suspend fun deleteByRef(tipe: String, refId: Long)

    @Query("SELECT COALESCE(SUM(nominal), 0) FROM pembayaran WHERE tipe = :tipe AND refId = :refId")
    suspend fun getTotalPaidForRef(tipe: String, refId: Long): Long

    @Query("SELECT COALESCE(SUM(nominal), 0) FROM pembayaran WHERE tipe = :tipe")
    fun getTotalPaidForType(tipe: String): Flow<Long>
}

@Dao
interface PengeluaranDao {
    @Query("SELECT * FROM pengeluaran ORDER BY tanggal DESC, id DESC")
    fun getAll(): Flow<List<Pengeluaran>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pengeluaran: Pengeluaran): Long

    @Delete
    suspend fun delete(pengeluaran: Pengeluaran)

    @Query("SELECT COALESCE(SUM(nominal), 0) FROM pengeluaran")
    fun getTotalPengeluaran(): Flow<Long>
}
