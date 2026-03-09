package com.example.graymatter.data.local

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.graymatter.database.GrayMatterDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): GrayMatterDatabase {
        val driver = NativeSqliteDriver(
            schema = GrayMatterDatabase.Schema,
            name = "graymatter.db"
        )
        return GrayMatterDatabase(driver)
    }
}
