package com.example.graymatter.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * Copies the content from the given URI to the app's internal storage
     * specifically in the 'resources' folder.
     */
    fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val contentResolver = context.contentResolver
            // Using openInputStream handles Scoped Storage automatically if we have URI permissions
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null for URI: $uri")
                return null
            }

            val outputDir = File(context.filesDir, "resources")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Extract extension
            val extension = fileName.substringAfterLast('.', "bin")
            // Use UUID for internal storage to avoid collisions and simplify backup
            val internalName = "${UUID.randomUUID()}.$extension"
            val outputFile = File(outputDir, internalName)

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to internal storage", e)
            null
        }
    }

    /**
     * Checks if a file exists at the given path.
     */
    fun verifyFileExists(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val file = File(path)
        return file.exists() && file.isFile
    }

    /**
     * Triggers the system "Open With" menu for the given file.
     */
    fun openFileWithIntent(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File no longer exists locally.", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(filePath))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open file", e)
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(filePath: String): String {
        val extension = File(filePath).extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
    }
}
