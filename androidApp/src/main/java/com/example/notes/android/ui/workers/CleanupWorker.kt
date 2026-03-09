package com.example.notes.android.ui.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.notes.data.NotesRepository
import kotlinx.coroutines.CancellationException

/**
 * A [CoroutineWorker] that deletes redundant data from the database.
 */
class CleanupWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val notesRepository: NotesRepository
) : CoroutineWorker(appContext = appContext, params = workerParameters) {

    override suspend fun doWork(): Result = try {
        notesRepository.deleteAllNotesMarkedAsDeleted()
        Result.success()
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        Result.failure()
    }
}

class CleanupWorkerFactory(private val notesRepository: NotesRepository) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker = CleanupWorker(
        appContext = appContext,
        workerParameters = workerParameters,
        notesRepository = notesRepository
    )
}
