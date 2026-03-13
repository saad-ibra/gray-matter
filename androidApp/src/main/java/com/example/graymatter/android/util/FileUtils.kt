package com.example.graymatter.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtils {
    /**
     * Copies the content from the given URI to the app's internal storage
     * and returns the absolute path of the copied file.
     */
    fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            
            if (inputStream == null) return null

            val outputDir = File(context.filesDir, "graymatter_docs")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Sanitize filename and prepend UUID to avoid collisions
            val safeFileName = fileName.replace("[^a-zA-Z0-9.\\-]".toRegex(), "_")
            val uniqueName = "${UUID.randomUUID()}_$safeFileName"
            val outputFile = File(outputDir, uniqueName)

            val outputStream = FileOutputStream(outputFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Triggers the system "Open With" menu for the given file.
     */
    fun openFileWithIntent(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            val uri = if (filePath.startsWith("content://")) {
                Uri.parse(filePath)
            } else if (filePath.startsWith("http")) {
                Uri.parse(filePath)
            } else {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(filePath))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(filePath: String): String {
        val extension = File(filePath).extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
    }
}
