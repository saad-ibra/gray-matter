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
     * Copies an image and automatically corrects any EXIF rotation issues so it displays upright.
     */
    fun copyImageAndFixRotation(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val path = copyUriToInternalStorage(context, uri, fileName) ?: return null
            
            val exif = android.media.ExifInterface(path)
            val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
            
            var degrees = 0f
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> degrees = 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> degrees = 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> degrees = 270f
            }
            
            if (degrees != 0f) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
                val rotatedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                
                FileOutputStream(path).use { out ->
                    rotatedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                }
                
                val newExif = android.media.ExifInterface(path)
                newExif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL.toString())
                newExif.saveAttributes()
            }
            
            path
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing image rotation", e)
            // If rotation fails, at least return the original copied path
            copyUriToInternalStorage(context, uri, fileName)
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
     * Extracts original filename from a content URI
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying file name", e)
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result ?: "shared_file"
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
            val chooser = Intent.createChooser(intent, "Open with")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val ownActivity = android.content.ComponentName(
                    context,
                    com.example.graymatter.android.ui.MainActivity::class.java
                )
                chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ownActivity))
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open file", e)
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(filePath: String): String {
        val extension = File(filePath).extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
    }

    /**
     * Saves a bitmap to the app's internal storage and returns the absolute path.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: android.graphics.Bitmap, folderName: String = "opinions"): String? {
        return try {
            val outputDir = File(context.filesDir, folderName)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val outputFile = File(outputDir, fileName)

            FileOutputStream(outputFile).use { output ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to internal storage", e)
            null
        }
    }

    /**
     * Creates a temporary image file and returns its URI for camera capture.
     */
    fun createTempImageUri(context: Context): Uri? {
        return try {
            val outputDir = File(context.cacheDir, "camera_temp")
            if (!outputDir.exists()) outputDir.mkdirs()
            val file = File(outputDir, "temp_capture.jpg")
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp URI", e)
            null
        }
    }
}
