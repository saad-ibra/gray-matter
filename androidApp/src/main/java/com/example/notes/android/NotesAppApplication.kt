package com.example.notes.android

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notes.android.ui.workers.CleanupWorker
import com.example.notes.android.ui.workers.CleanupWorkerFactory
import com.example.notes.di.AppModule
import java.util.concurrent.TimeUnit

class NotesAppApplication : Application(), Configuration.Provider {

    val appModule by lazy { AppModule(this) }

    override fun onCreate() {
        super.onCreate()
        setupCleanupWorker()
    }

    private fun setupCleanupWorker() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            repeatInterval = 1L,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                CLEANUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
    }

    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(CleanupWorkerFactory(notesRepository = appModule.provideNotesRepository()))
        .build()

    companion object {
        private const val CLEANUP_WORK_NAME = "notes_application_cleanup_work"
    }
}