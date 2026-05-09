package com.example.graymatter.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.graymatter.android.backup.BackupManager
import com.example.graymatter.android.backup.BackupPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A WorkManager [CoroutineWorker] that performs encrypted backups in the background.
 * Runs on the configured schedule (daily/weekly/monthly) or as a one-time manual trigger.
 */
class BackupWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext = appContext, params = workerParameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val preferences = BackupPreferences(applicationContext)
            val manager = BackupManager(applicationContext, preferences)

            val backupFile = manager.runBackup()
            if (backupFile != null) {
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure()
        }
    }
}
