package com.example.graymatter.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.graymatter.di.AppModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A [CoroutineWorker] that performs background cleanup tasks for Gray Matter.
 * Deletes orphaned files in the internal resources directory that are no longer referenced in the database.
 */
class CleanupWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val appModule: AppModule
) : CoroutineWorker(appContext = appContext, params = workerParameters) {

    override suspend fun doWork(): Result = try {
        val resourceDir = File(applicationContext.filesDir, "resources")
        if (resourceDir.exists()) {
            val files = resourceDir.listFiles()
            if (files != null) {
                // Fetch current resource entries from the database
                // resourceEntriesStream is a Flow<List<ResourceEntry>>, so we use .first() to get the current snapshot
                val resourceEntriesList = appModule.resourceEntryRepository.resourceEntriesStream.first()
                
                // Collect all valid file paths currently tracked in the database
                val validPaths = mutableSetOf<String>()
                for (resourceEntry in resourceEntriesList) {
                    // getResourceEntryWithDetails is a suspend function, so we must call it within the suspend doWork()
                    // or another coroutine context. A simple for-loop is the safest way.
                    val details = appModule.resourceEntryRepository.getResourceEntryWithDetails(resourceEntry.id)
                    details?.resource?.filePath?.let { path ->
                        // Store the absolute path for reliable comparison
                        validPaths.add(File(path).absolutePath)
                    }
                }
                
                // Remove files from storage that are no longer in the database
                for (file in files) {
                    if (!validPaths.contains(file.absolutePath)) {
                        file.delete()
                    }
                }
            }
        }
        Result.success()
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        Result.failure()
    }
}

class CleanupWorkerFactory(private val appModule: AppModule) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            CleanupWorker::class.java.name -> CleanupWorker(
                appContext = appContext,
                workerParameters = workerParameters,
                appModule = appModule
            )
            else -> null
        }
    }
}

fun setupCleanupWorker(context: Context) {
    val periodicWorkRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
        repeatInterval = 1L,
        repeatIntervalTimeUnit = TimeUnit.DAYS
    ).build()

    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            "gray_matter_cleanup_work",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
}
