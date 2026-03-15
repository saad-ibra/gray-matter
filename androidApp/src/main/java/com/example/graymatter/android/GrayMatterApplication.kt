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
    
    lateinit var appModule: AppModule
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        appModule = AppModule(
            databaseDriverFactory = DatabaseDriverFactory(this)
        )
        
        setupCleanupWorker(this)
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(CleanupWorkerFactory(appModule = appModule))
        .build()
}
