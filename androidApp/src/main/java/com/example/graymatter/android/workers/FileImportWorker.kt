package com.example.graymatter.android.workers

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.graymatter.android.R
import com.example.graymatter.android.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class FileImportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriString = inputData.getString(KEY_URI) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "unknown_file"
        val uri = Uri.parse(uriString)

        try {
            setForeground(createForegroundInfo())

            val contentResolver = applicationContext.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri) 
                ?: return@withContext Result.failure()

            val outputDir = File(applicationContext.filesDir, "resources")
            if (!outputDir.exists()) outputDir.mkdirs()

            val extension = fileName.substringAfterLast('.', "bin")
            val internalFileName = "${UUID.randomUUID()}.$extension"
            val outputFile = File(outputDir, internalFileName)

            val totalBytes = inputStream.available().toLong() // Note: available() is not always accurate for large files
            var bytesCopied = 0L

            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytes = inputStream.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    
                    // Update progress (optional, but good for large files)
                    if (totalBytes > 0) {
                        val progress = (bytesCopied * 100 / totalBytes).toInt()
                        setProgress(workDataOf(PROGRESS to progress))
                    }
                    
                    bytes = inputStream.read(buffer)
                }
            }

            Result.success(workDataOf(KEY_RESULT_PATH to outputFile.absolutePath))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, "import_channel")
            .setContentTitle("Importing File")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_URI = "key_uri"
        const val KEY_FILE_NAME = "key_file_name"
        const val KEY_RESULT_PATH = "key_result_path"
        const val PROGRESS = "progress"
        private const val NOTIFICATION_ID = 101
    }
}
