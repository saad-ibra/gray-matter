package com.example.graymatter.android

import android.app.Application
import androidx.work.Configuration
import com.example.graymatter.android.workers.CleanupWorkerFactory
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

    // The WorkerManager uses our CleanupWorkerFactory which does not need appModule anymore
    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(CleanupWorkerFactory())
        .build()
}
