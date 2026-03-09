package com.example.graymatter.data.local

import com.example.graymatter.database.GrayMatterDatabase

/**
 * Factory for creating SQLDelight database driver.
 * Platform-specific implementations in androidMain and iosMain.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): GrayMatterDatabase
}
