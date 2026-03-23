package com.example.graymatter.android

import android.app.Application
import androidx.work.Configuration
import com.example.graymatter.android.workers.CleanupWorkerFactory
import com.example.graymatter.android.workers.setupCleanupWorker
import com.example.graymatter.data.local.DatabaseDriverFactory
import com.example.graymatter.di.AppModule

/**
 * Application class for Gray Matter.
 */
class GrayMatterApplication : Application(), Configuration.Provider {
    
    val appModule: AppModule by lazy {
        AppModule(
            databaseDriverFactory = DatabaseDriverFactory(this)
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager early if needed, 
        // but here we just ensure setupCleanupWorker is called
        setupCleanupWorker(this)
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(CleanupWorkerFactory(appModule = appModule))
        .build()
}
