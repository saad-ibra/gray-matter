package com.example.graymatter.data.local

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.graymatter.database.GrayMatterDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Android-specific database driver factory using SQLCipher for
 * transparent AES-256 encryption of the local database.
 *
 * @param context Application context
 * @param passphrase The encryption passphrase from Android Keystore.
 *                   The caller is responsible for zeroing this after the driver is created.
 */
actual class DatabaseDriverFactory(
    private val context: Context,
    private val passphrase: ByteArray
) {
    actual fun createDriver(): GrayMatterDatabase {
        val factory = SupportFactory(passphrase)
        val driver = AndroidSqliteDriver(
            schema = GrayMatterDatabase.Schema,
            context = context,
            name = "graymatter_v14_enc.db",
            factory = factory,
            callback = object : AndroidSqliteDriver.Callback(GrayMatterDatabase.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
        return GrayMatterDatabase(driver)
    }
}
