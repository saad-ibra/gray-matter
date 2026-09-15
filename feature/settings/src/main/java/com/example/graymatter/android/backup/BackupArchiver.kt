package com.example.graymatter.android.backup

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

// Handles creating and extracting compressed backup archives.
object BackupArchiver {

    private const val TAG = "BackupArchiver"
    private const val MANIFEST_FILENAME = "manifest.json"

    // Creates a compressed archive of all app data.
    fun createArchive(context: Context, outputFile: File): Boolean {
        return try {
            GZIPOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { gzip ->
                val dataOut = DataOutputStream(gzip)

                val entries = mutableListOf<ArchiveEntry>()

                // Databases (try encrypted v14 first, fall back to v13 for legacy)
                val encDbFile = context.getDatabasePath("graymatter_v14_enc.db")
                val legacyDbFile = context.getDatabasePath("graymatter_v13.db")
                val dbFile = if (encDbFile.exists()) encDbFile else legacyDbFile
                if (dbFile.exists()) {
                    entries.add(ArchiveEntry("databases/${dbFile.name}", dbFile))
                }
                val notesDbFile = context.getDatabasePath("notes.db")
                if (notesDbFile.exists()) {
                    entries.add(ArchiveEntry("databases/notes.db", notesDbFile))
                }
                val walFile = File(dbFile.path + "-wal")
                if (walFile.exists()) entries.add(ArchiveEntry("databases/${dbFile.name}-wal", walFile))
                val shmFile = File(dbFile.path + "-shm")
                if (shmFile.exists()) entries.add(ArchiveEntry("databases/${dbFile.name}-shm", shmFile))

                // Resource files (PDFs, etc.)
                val resourcesDir = File(context.filesDir, "resources")
                if (resourcesDir.exists()) {
                    resourcesDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            entries.add(ArchiveEntry("files/resources/${file.name}", file))
                        }
                    }
                }

                // Opinion images (visual entries)
                val opinionsDir = File(context.filesDir, "opinions")
                if (opinionsDir.exists()) {
                    opinionsDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            entries.add(ArchiveEntry("files/opinions/${file.name}", file))
                        }
                    }
                }

                // Build manifest
                val manifest = JSONObject().apply {
                    put("appVersion", getAppVersion(context))
                    put("backupTimestamp", System.currentTimeMillis())
                    put("fileCount", entries.size)
                    put("schemaVersion", 13)
                }
                val manifestBytes = manifest.toString(2).toByteArray(Charsets.UTF_8)

                writeEntry(dataOut, MANIFEST_FILENAME, manifestBytes)

                for (entry in entries) {
                    val bytes = entry.file.readBytes()
                    writeEntry(dataOut, entry.archivePath, bytes)
                }

                dataOut.writeInt(-1)
                dataOut.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup archive", e)
            false
        }
    }

    // Extracts a compressed archive to a staging directory.
    fun extractArchive(archiveFile: File, targetDir: File): JSONObject? {
        return try {
            if (!targetDir.exists()) targetDir.mkdirs()

            var manifest: JSONObject? = null

            GZIPInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { gzip ->
                val dataIn = DataInputStream(gzip)

                while (true) {
                    val pathLength = try {
                        dataIn.readInt()
                    } catch (_: EOFException) {
                        break
                    }
                    if (pathLength == -1) break

                    val pathBytes = ByteArray(pathLength)
                    dataIn.readFully(pathBytes)
                    val path = String(pathBytes, Charsets.UTF_8)

                    val dataLength = dataIn.readInt()
                    val data = ByteArray(dataLength)
                    dataIn.readFully(data)

                    if (path == MANIFEST_FILENAME) {
                        manifest = JSONObject(String(data, Charsets.UTF_8))
                    } else {
                        val outFile = File(targetDir, path)
                        outFile.parentFile?.mkdirs()
                        outFile.writeBytes(data)
                    }
                }
            }
            manifest
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract backup archive", e)
            null
        }
    }

    private fun writeEntry(dataOut: DataOutputStream, path: String, data: ByteArray) {
        val pathBytes = path.toByteArray(Charsets.UTF_8)
        dataOut.writeInt(pathBytes.size)
        dataOut.write(pathBytes)
        dataOut.writeInt(data.size)
        dataOut.write(data)
    }

    private fun getAppVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private data class ArchiveEntry(val archivePath: String, val file: File)
}
