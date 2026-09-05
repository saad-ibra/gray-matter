package com.example.graymatter.data.local

import android.content.Context
import android.util.Log
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
                    runSafeMigrations(db)
                }
            }
        )
        return GrayMatterDatabase(driver)
    }

    /**
     * Idempotent migration: every statement uses IF NOT EXISTS or
     * catches "duplicate column" so it is safe to run on every app start.
     * This ensures v1.8 users get the new schema on their existing data.
     */
    private fun runSafeMigrations(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        try {
            // v1.8 → v2.0: add 'color' column to topicEntity
            try {
                db.execSQL("ALTER TABLE topicEntity ADD COLUMN color TEXT")
            } catch (_: Exception) {
                // Column already exists — safe to ignore
            }

            // v1.8 → v2.0: create tag tables
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tagEntity (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS entryTagEntity (
                    id TEXT NOT NULL PRIMARY KEY,
                    entryId TEXT NOT NULL,
                    entryType TEXT NOT NULL,
                    tagId TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY (tagId) REFERENCES tagEntity(id) ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_entry_tag_unique ON entryTagEntity(entryId, tagId)")
        } catch (e: Exception) {
            Log.e("DatabaseDriverFactory", "Migration error (non-fatal)", e)
        }
    }
}
