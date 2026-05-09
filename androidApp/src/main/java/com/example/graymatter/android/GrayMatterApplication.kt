package com.example.graymatter.android

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.graymatter.android.security.DatabaseMigrationHelper
import com.example.graymatter.android.security.SecureDatabaseKeyManager
import com.example.graymatter.android.workers.GrayMatterWorkerFactory
import com.example.graymatter.android.workers.setupCleanupWorker
import com.example.graymatter.di.sharedModule
import com.example.graymatter.android.di.androidAppModule
import com.example.graymatter.android.di.androidViewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class for Gray Matter.
 */
class GrayMatterApplication : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()

        // Run database migration from unencrypted v13 to encrypted v14 (if needed)
        runDatabaseMigration()
        
        startKoin {
            // Enable Koin logger (using ERROR level by default since DEBUG might be too verbose)
            androidLogger(Level.ERROR)
            androidContext(this@GrayMatterApplication)
            modules(androidAppModule, sharedModule, androidViewModelModule)
        }
        
        // Initialize WorkManager early if needed, 
        // but here we just ensure setupCleanupWorker is called
        setupCleanupWorker(this)
    }

    /**
     * Migrates the unencrypted SQLite database to an encrypted SQLCipher database.
     * This runs before Koin is initialized so the DatabaseDriverFactory
     * can open the encrypted DB directly.
     */
    private fun runDatabaseMigration() {
        val keyManager = SecureDatabaseKeyManager(this)
        val passphrase = keyManager.getDatabasePassphrase()
        try {
            val success = DatabaseMigrationHelper.migrateIfNeeded(this, passphrase)
            if (!success) {
                Log.e("GrayMatterApp", "Database migration failed — app may need a fresh install")
            }
        } finally {
            passphrase.fill(0) // Zero passphrase from memory
        }
    }

    // The WorkerManager uses our combined WorkerFactory
    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(GrayMatterWorkerFactory())
        .build()
}
