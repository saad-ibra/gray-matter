package com.example.graymatter.android.util

import android.util.Log
import java.io.*
import java.nio.charset.Charset

/**
 * Robust text file reader that handles large files and different encodings.
 */
object SafeTextReader {
    private const val TAG = "SafeTextReader"
    private const val MAX_READ_SIZE = 2 * 1024 * 1024 // 2MB limit for initial load for stability

    /**
     * Reads text from a file path with encoding detection and size limiting.
     */
    fun readText(path: String): Result<String> {
        val file = File(path)
        if (!file.exists()) {
            return Result.failure(FileNotFoundException("File not found: $path"))
        }

        return try {
            val fileSize = file.length()
            val bytes = if (fileSize > MAX_READ_SIZE) {
                val bis = BufferedInputStream(FileInputStream(file))
                val b = ByteArray(MAX_READ_SIZE)
                val read = bis.read(b)
                bis.close()
                b.sliceArray(0 until read)
            } else {
                file.readBytes()
            }

            val charset = detectCharset(bytes)
            var text = String(bytes, charset)

            if (fileSize > MAX_READ_SIZE) {
                text += "\n\n--- [FILE TRUNCATED] ---\n" +
                    "This file is too large for the current text viewer. " +
                    "Only the first ${MAX_READ_SIZE / 1024 / 1024}MB has been loaded."
            }
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read text file: $path", e)
            Result.failure(e)
        }
    }

    /**
     * Detects charset from bytes, checking for BOM and validating UTF-8.
     */
    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return Charsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE
        }

        // Check if it's valid UTF-8
        return if (isValidUtf8(bytes)) {
            Charsets.UTF_8
        } else {
            // Fallback to ISO-8859-1 (Windows-1252 is a superset, but ISO-8859-1 is standard in Java)
            Charset.forName("ISO-8859-1")
        }
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b <= 0x7F) {
                i++
            } else if (b >= 0xC2 && b <= 0xDF) {
                if (i + 1 >= bytes.size) return false
                if (bytes[i + 1].toInt() and 0xFF !in 0x80..0xBF) return false
                i += 2
            } else if (b >= 0xE0 && b <= 0xEF) {
                if (i + 2 >= bytes.size) return false
                if (bytes[i + 1].toInt() and 0xFF !in 0x80..0xBF) return false
                if (bytes[i + 2].toInt() and 0xFF !in 0x80..0xBF) return false
                i += 3
            } else if (b >= 0xF0 && b <= 0xF4) {
                if (i + 3 >= bytes.size) return false
                if (bytes[i + 1].toInt() and 0xFF !in 0x80..0xBF) return false
                if (bytes[i + 2].toInt() and 0xFF !in 0x80..0xBF) return false
                if (bytes[i + 3].toInt() and 0xFF !in 0x80..0xBF) return false
                i += 4
            } else {
                return false
            }
        }
        return true
    }
}
