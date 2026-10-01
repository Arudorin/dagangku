package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Migrate 'produk' (Double -> Long)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `produk_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nama` TEXT NOT NULL,
                `satuan` TEXT NOT NULL,
                `hargaDasar` INTEGER NOT NULL,
                `hargaJual` INTEGER NOT NULL,
                `stokMinimum` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `produk_new` (`id`, `nama`, `satuan`, `hargaDasar`, `hargaJual`, `stokMinimum`)
            SELECT `id`, `nama`, `satuan`, CAST(ROUND(`hargaDasar`) AS INTEGER), CAST(ROUND(`hargaJual`) AS INTEGER), `stokMinimum`
            FROM `produk`
        """.trimIndent())
        db.execSQL("DROP TABLE `produk`")
        db.execSQL("ALTER TABLE `produk_new` RENAME TO `produk`")

        // 2. Migrate 'harga_customer' (Double -> Long)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `harga_customer_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `produkId` INTEGER NOT NULL,
                `hargaDeal` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `harga_customer_new` (`id`, `customerId`, `produkId`, `hargaDeal`)
            SELECT `id`, `customerId`, `produkId`, CAST(ROUND(`hargaDeal`) AS INTEGER)
            FROM `harga_customer`
        """.trimIndent())
        db.execSQL("DROP TABLE `harga_customer`")
        db.execSQL("ALTER TABLE `harga_customer_new` RENAME TO `harga_customer`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_harga_customer_customerId_produkId` ON `harga_customer` (`customerId`, `produkId`)")

        // 3. Migrate 'po' (Double -> Long)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `po_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nomor` TEXT NOT NULL,
                `distributorId` INTEGER NOT NULL,
                `tanggal` INTEGER NOT NULL,
                `total` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `po_new` (`id`, `nomor`, `distributorId`, `tanggal`, `total`)
            SELECT `id`, `nomor`, `distributorId`, `tanggal`, CAST(ROUND(`total`) AS INTEGER)
            FROM `po`
        """.trimIndent())
        db.execSQL("DROP TABLE `po`")
        db.execSQL("ALTER TABLE `po_new` RENAME TO `po`")

        // 4. Migrate 'item_po' (Double -> Long)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `item_po_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `poId` INTEGER NOT NULL,
                `produkId` INTEGER NOT NULL,
                `qty` INTEGER NOT NULL,
                `hargaBeli` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `item_po_new` (`id`, `poId`, `produkId`, `qty`, `hargaBeli`)
            SELECT `id`, `poId`, `produkId`, `qty`, CAST(ROUND(`hargaBeli`) AS INTEGER)
            FROM `item_po`
        """.trimIndent())
        db.execSQL("DROP TABLE `item_po`")
        db.execSQL("ALTER TABLE `item_po_new` RENAME TO `item_po`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_po_poId` ON `item_po` (`poId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_po_produkId` ON `item_po` (`produkId`)")

        // 5. Migrate 'so' (Double -> Long)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `so_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nomor` TEXT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `tanggal` INTEGER NOT NULL,
                `total` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `so_new` (`id`, `nomor`, `customerId`, `tanggal`, `total`)
            SELECT `id`, `nomor`, `customerId`, `tanggal`, CAST(ROUND(`total`) AS INTEGER)
            FROM `so`
        """.trimIndent())
        db.execSQL("DROP TABLE `so`")
        db.execSQL("ALTER TABLE `so_new` RENAME TO `so`")

        // 6. Migrate 'item_so' (Double -> Long, add hppSaatJual)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `item_so_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `soId` INTEGER NOT NULL,
                `produkId` INTEGER NOT NULL,
                `qty` INTEGER NOT NULL,
                `harga` INTEGER NOT NULL,
                `hppSaatJual` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `item_so_new` (`id`, `soId`, `produkId`, `qty`, `harga`, `hppSaatJual`)
            SELECT `id`, `soId`, `produkId`, `qty`, CAST(ROUND(`harga`) AS INTEGER),
                   COALESCE((SELECT CAST(ROUND(p.`hargaDasar`) AS INTEGER) FROM `produk` p WHERE p.`id` = `item_so`.`produkId`), 0)
            FROM `item_so`
        """.trimIndent())
        db.execSQL("DROP TABLE `item_so`")
        db.execSQL("ALTER TABLE `item_so_new` RENAME TO `item_so`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_so_soId` ON `item_so` (`soId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_so_produkId` ON `item_so` (`produkId`)")

        // 7. Migrate 'pembayaran' (Double -> Long)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `pembayaran_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tipe` TEXT NOT NULL,
                `refId` INTEGER NOT NULL,
                `nominal` INTEGER NOT NULL,
                `tanggal` INTEGER NOT NULL,
                `metode` TEXT NOT NULL,
                `catatan` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `pembayaran_new` (`id`, `tipe`, `refId`, `nominal`, `tanggal`, `metode`, `catatan`)
            SELECT `id`, `tipe`, `refId`, CAST(ROUND(`nominal`) AS INTEGER), `tanggal`, `metode`, `catatan`
            FROM `pembayaran`
        """.trimIndent())
        db.execSQL("DROP TABLE `pembayaran`")
        db.execSQL("ALTER TABLE `pembayaran_new` RENAME TO `pembayaran`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pembayaran_refId` ON `pembayaran` (`refId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pembayaran_tipe` ON `pembayaran` (`tipe`)")

        // 8. Migrate 'pengeluaran' (Double -> Long)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `pengeluaran_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `kategori` TEXT NOT NULL,
                `nominal` INTEGER NOT NULL,
                `tanggal` INTEGER NOT NULL,
                `keterangan` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `pengeluaran_new` (`id`, `kategori`, `nominal`, `tanggal`, `keterangan`)
            SELECT `id`, `kategori`, CAST(ROUND(`nominal`) AS INTEGER), `tanggal`, `keterangan`
            FROM `pengeluaran`
        """.trimIndent())
        db.execSQL("DROP TABLE `pengeluaran`")
        db.execSQL("ALTER TABLE `pengeluaran_new` RENAME TO `pengeluaran`")
    }
}

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
    version = 2,
    exportSchema = true
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
                ).addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
