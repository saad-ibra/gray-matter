// DEPRECATED
package com.example.graymatter.notes.android.ui.workers
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkerFactory
import androidx.work.ListenableWorker

class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}

class CleanupWorkerFactory : WorkerFactory() {
    override fun createWorker(context: Context, className: String, params: WorkerParameters): ListenableWorker? = null
}
