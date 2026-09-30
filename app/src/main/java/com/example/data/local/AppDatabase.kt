package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Produk::class,
        Distributor::class,
        Customer::class,
        HargaCustomer::class,
        PO::class,
        ItemPO::class,
        SO::class,
        ItemSO::class,
        Pembayaran::class,
        Pengeluaran::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun produkDao(): ProdukDao
    abstract fun distributorDao(): DistributorDao
    abstract fun customerDao(): CustomerDao
    abstract fun hargaCustomerDao(): HargaCustomerDao
    abstract fun poDao(): PoDao
    abstract fun itemPoDao(): ItemPoDao
    abstract fun soDao(): SoDao
    abstract fun itemSoDao(): ItemSoDao
    abstract fun pembayaranDao(): PembayaranDao
    abstract fun pengeluaranDao(): PengeluaranDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dagangku.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
