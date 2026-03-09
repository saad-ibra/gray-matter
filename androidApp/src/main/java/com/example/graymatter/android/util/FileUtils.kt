package com.example.graymatter.android.util

import android.content.Context
import android.net.Uri
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
}
