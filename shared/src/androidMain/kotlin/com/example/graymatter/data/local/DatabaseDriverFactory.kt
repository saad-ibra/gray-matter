package com.example.graymatter.data.local

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.graymatter.database.GrayMatterDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): GrayMatterDatabase {
        val driver = AndroidSqliteDriver(
            schema = GrayMatterDatabase.Schema,
            context = context,
            name = "graymatter_v11_fresh.db", // Incremented version to v11 to sync schema changes
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
