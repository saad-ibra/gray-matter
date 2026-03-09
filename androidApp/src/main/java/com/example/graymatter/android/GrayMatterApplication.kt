package com.example.graymatter.android

import android.app.Application
import com.example.graymatter.data.local.DatabaseDriverFactory
import com.example.graymatter.di.AppModule

/**
 * Application class for Gray Matter.
 */
class GrayMatterApplication : Application() {
    
    lateinit var appModule: AppModule
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        appModule = AppModule(
            databaseDriverFactory = DatabaseDriverFactory(this)
        )
    }
}
