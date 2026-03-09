package com.example.graymatter.data.local

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.graymatter.database.GrayMatterDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): GrayMatterDatabase {
        val driver = AndroidSqliteDriver(
            schema = GrayMatterDatabase.Schema,
            context = context,
            name = "graymatter_v10_fresh.db" // Fresh name to avoid any migration issues
        )
        return GrayMatterDatabase(driver)
    }
}
