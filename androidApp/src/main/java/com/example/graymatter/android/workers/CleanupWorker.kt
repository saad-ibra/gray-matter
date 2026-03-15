package com.example.graymatter.android.workers

import android.content.Context
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.graymatter.di.AppModule
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * A [CoroutineWorker] that performs background cleanup tasks for Gray Matter.
 * Currently a placeholder as we transition from the legacy Notes architecture.
 */
class CleanupWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val appModule: AppModule
) : CoroutineWorker(appContext = appContext, params = workerParameters) {

    override suspend fun doWork(): Result = try {
        // TODO: Implement Gray Matter specific cleanup logic here
        // e.g., cleaning up orphaned resources or temporary files
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