package com.example.graymatter.android.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * Handles one-time migration from unencrypted SQLite to encrypted SQLCipher database.
 *
 * Migration strategy:
 * 1. Check if unencrypted DB exists at the old path
 * 2. Copy the unencrypted DB to the new encrypted path
 * 3. SQLCipher's SupportFactory (in DatabaseDriverFactory) will handle
 *    encrypting it on first open via PRAGMA key
 *
 * Since we cannot directly use sqlcipher_export from the androidApp module
 * (SQLCipher dependency lives in the shared module), we use a simpler approach:
 * copy the plain DB, then let SQLCipher encrypt it on first access.
 *
 * Actually, the proper approach is: we detect the old unencrypted DB,
 * read all data from it using the standard Android SQLite API, and the
 * new encrypted DB will be created fresh by SQLDelight schema migration.
 * The data will be re-populated from a backup if needed.
 *
 * For seamless migration, we use the Android SQLite API to export data
 * and SQLCipher (via the shared module) to import it.
 */
object DatabaseMigrationHelper {

    private const val TAG = "DBMigrationHelper"
    private const val OLD_DB_NAME = "graymatter_v13.db"
    private const val NEW_DB_NAME = "graymatter_v14_enc.db"

    /**
     * Checks if migration is needed and performs a simple file-based migration.
     *
     * Since the SQLCipher driver in the shared module will create the encrypted
     * DB from the schema on first access, we just need to ensure:
     * 1. The old unencrypted DB data is preserved (via standard Android backup/export)
     * 2. The old DB file is cleaned up after the new encrypted one is created
     *
     * For a seamless experience, we export the old DB to a temporary file,
     * then re-import after the encrypted DB is initialized.
     *
     * Returns true if migration succeeded or was not needed.
     */
    @Suppress("UNUSED_PARAMETER")
    fun migrateIfNeeded(context: Context, passphrase: ByteArray): Boolean {
        val oldDbFile = context.getDatabasePath(OLD_DB_NAME)
        val newDbFile = context.getDatabasePath(NEW_DB_NAME)

        // If encrypted DB already exists, no migration needed
        if (newDbFile.exists()) {
            Log.i(TAG, "Encrypted database already exists, skipping migration")
            return true
        }

        // If old DB doesn't exist either, fresh install — no migration needed
        if (!oldDbFile.exists()) {
            Log.i(TAG, "No existing database found, fresh install")
            return true
        }

        Log.i(TAG, "Unencrypted database detected at ${oldDbFile.absolutePath}")
        Log.i(TAG, "Starting migration: extracting data from $OLD_DB_NAME")

        return try {
            // Read all data from the unencrypted database
            val exportedData = exportUnencryptedData(oldDbFile)

            if (exportedData.isEmpty()) {
                Log.w(TAG, "No user data found in old database, proceeding with clean start")
                // Just delete old DB, SQLCipher will create fresh encrypted one
                cleanupOldDb(oldDbFile)
                return true
            }

            // Save exported data to a temporary migration file
            val migrationFile = File(context.cacheDir, "migration_data.sql")
            migrationFile.writeText(exportedData.joinToString("\n"))

            // Store path for post-initialization import
            context.getSharedPreferences("gm_migration", Context.MODE_PRIVATE)
                .edit()
                .putString("pending_migration_file", migrationFile.absolutePath)
                .putBoolean("migration_pending", true)
                .apply()

            // Clean up old unencrypted database
            cleanupOldDb(oldDbFile)

            Log.i(TAG, "Data exported, old DB cleaned up. Migration will complete on first DB access.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            // Don't delete old DB on failure — user data is still accessible
            false
        }
    }

    /**
     * Checks if there's a pending data import from migration.
     */
    fun hasPendingMigration(context: Context): Boolean {
        return context.getSharedPreferences("gm_migration", Context.MODE_PRIVATE)
            .getBoolean("migration_pending", false)
    }

    /**
     * Marks migration as complete.
     */
    fun completeMigration(context: Context) {
        val prefs = context.getSharedPreferences("gm_migration", Context.MODE_PRIVATE)
        val migrationFile = prefs.getString("pending_migration_file", null)
        if (migrationFile != null) {
            File(migrationFile).delete()
        }
        prefs.edit()
            .putBoolean("migration_pending", false)
            .remove("pending_migration_file")
            .apply()
        Log.i(TAG, "Migration marked as complete")
    }

    /**
     * Exports INSERT statements from the old unencrypted database.
     * Uses the standard Android SQLiteDatabase API (no SQLCipher needed).
     */
    private fun exportUnencryptedData(dbFile: File): List<String> {
        val statements = mutableListOf<String>()

        val db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
        )

        try {
            // Get all user tables (exclude SQLite internal tables)
            val tablesCursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
                null
            )

            val tables = mutableListOf<String>()
            while (tablesCursor.moveToNext()) {
                tables.add(tablesCursor.getString(0))
            }
            tablesCursor.close()

            Log.i(TAG, "Found ${tables.size} tables to migrate: $tables")

            for (table in tables) {
                val cursor = db.rawQuery("SELECT * FROM $table", null)
                val columnCount = cursor.columnCount

                while (cursor.moveToNext()) {
                    val values = mutableListOf<String>()
                    for (i in 0 until columnCount) {
                        val value = when (cursor.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> "NULL"
                            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                            android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i).toString()
                            android.database.Cursor.FIELD_TYPE_STRING -> {
                                "'" + cursor.getString(i).replace("'", "''") + "'"
                            }
                            android.database.Cursor.FIELD_TYPE_BLOB -> {
                                val blob = cursor.getBlob(i)
                                "X'" + blob.joinToString("") { "%02x".format(it) } + "'"
                            }
                            else -> "NULL"
                        }
                        values.add(value)
                    }
                    statements.add("INSERT OR REPLACE INTO $table VALUES(${values.joinToString(",")})")
                }
                cursor.close()
                Log.i(TAG, "Exported ${statements.size} rows from table: $table")
            }
        } finally {
            db.close()
        }

        return statements
    }

    /**
     * Removes the old unencrypted database and its journal files.
     */
    private fun cleanupOldDb(dbFile: File) {
        dbFile.delete()
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-journal").delete()
        Log.i(TAG, "Old unencrypted database cleaned up")
    }
}
