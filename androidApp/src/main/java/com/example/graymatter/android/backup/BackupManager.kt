package com.example.graymatter.android.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Information about a local backup file.
 */
data class BackupInfo(
    val file: File,
    val name: String,
    val timestamp: Long,
    val sizeBytes: Long
)

/**
 * Orchestrates backup creation, rotation, and export.
 *
 * All password parameters are [CharArray] to enable explicit memory zeroing.
 * Callers MUST zero their password arrays after use.
 */
class BackupManager(
    private val context: Context,
    private val preferences: BackupPreferences
) {
    private val backupDir: File
        get() = File(context.filesDir, "backups").also { if (!it.exists()) it.mkdirs() }

    /**
     * Runs a full backup: archive → encrypt → rotate.
     * Returns the backup file on success, null on failure.
     *
     * The master password is retrieved as a [CharArray] and zeroed after use.
     */
    fun runBackup(): File? {
        val password = preferences.getMasterPasswordChars()
        if (password == null || password.isEmpty()) {
            Log.e(TAG, "Cannot run backup: no master password set")
            return null
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupDir, "graymatter_$timestamp.gm.bak")
        val tempArchive = File(context.cacheDir, "backup_temp.tar.gz")

        return try {
            // 1. Create compressed archive
            val archiveSuccess = BackupArchiver.createArchive(context, tempArchive)
            if (!archiveSuccess) {
                Log.e(TAG, "Failed to create archive")
                preferences.lastBackupSuccess = false
                return null
            }

            // 2. Encrypt the archive
            tempArchive.inputStream().buffered().use { input ->
                backupFile.outputStream().buffered().use { output ->
                    BackupCrypto.encrypt(input, output, password)
                }
            }

            // 3. Rotate old backups
            rotateBackups()

            // 4. Update preferences
            preferences.lastBackupTimestamp = System.currentTimeMillis()
            preferences.lastBackupSizeBytes = backupFile.length()
            preferences.lastBackupSuccess = true

            Log.i(TAG, "Backup created: ${backupFile.name} (${backupFile.length()} bytes)")
            backupFile
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            preferences.lastBackupSuccess = false
            backupFile.delete()
            null
        } finally {
            password.fill('\u0000') // Zero password from memory
            tempArchive.delete()
        }
    }

    /**
     * Restores from a backup file URI.
     * Returns true on success.
     *
     * @param password Password as CharArray. Will be zeroed after use.
     */
    fun restoreFromBackup(backupUri: Uri, password: CharArray): Boolean {
        val stagingDir = File(context.cacheDir, "restore_staging")
        val tempArchive = File(context.cacheDir, "restore_temp.tar.gz")

        return try {
            // 1. Decrypt
            context.contentResolver.openInputStream(backupUri)?.buffered()?.use { input ->
                tempArchive.outputStream().buffered().use { output ->
                    BackupCrypto.decrypt(input, output, password)
                }
            } ?: throw IllegalStateException("Could not open backup file")

            // 2. Extract
            val manifest = BackupArchiver.extractArchive(tempArchive, stagingDir)
                ?: throw IllegalStateException("Failed to extract archive")

            Log.i(TAG, "Restore manifest: $manifest")

            // 3. Replace databases
            val stagingDbDir = File(stagingDir, "databases")
            if (stagingDbDir.exists()) {
                stagingDbDir.listFiles()?.forEach { dbFile ->
                    val targetDb = context.getDatabasePath(dbFile.name)
                    // Close any open connections by deleting journal files
                    File(targetDb.path + "-wal").delete()
                    File(targetDb.path + "-shm").delete()
                    File(targetDb.path + "-journal").delete()
                    dbFile.copyTo(targetDb, overwrite = true)
                }
            }

            // 4. Replace resource files
            val stagingResources = File(stagingDir, "files/resources")
            if (stagingResources.exists()) {
                val targetDir = File(context.filesDir, "resources")
                targetDir.deleteRecursively()
                targetDir.mkdirs()
                stagingResources.listFiles()?.forEach { file ->
                    file.copyTo(File(targetDir, file.name), overwrite = true)
                }
            }

            // 5. Replace opinion images
            val stagingOpinions = File(stagingDir, "files/opinions")
            if (stagingOpinions.exists()) {
                val targetDir = File(context.filesDir, "opinions")
                targetDir.deleteRecursively()
                targetDir.mkdirs()
                stagingOpinions.listFiles()?.forEach { file ->
                    file.copyTo(File(targetDir, file.name), overwrite = true)
                }
            }

            Log.i(TAG, "Restore completed successfully")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Restore failed: wrong password or corrupt file", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            false
        } finally {
            password.fill('\u0000') // Zero password from memory
            tempArchive.delete()
            stagingDir.deleteRecursively()
        }
    }

    /**
     * Exports a backup file to a user-chosen SAF URI.
     */
    fun exportBackup(backupFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.buffered()?.use { output ->
                backupFile.inputStream().buffered().use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            false
        }
    }

    /**
     * Returns a list of all local backup files, sorted newest first.
     */
    fun getLocalBackups(): List<BackupInfo> {
        return backupDir.listFiles { file -> file.extension == "bak" && file.name.startsWith("graymatter_") }
            ?.map { file ->
                BackupInfo(
                    file = file,
                    name = file.name,
                    timestamp = file.lastModified(),
                    sizeBytes = file.length()
                )
            }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    /**
     * Deletes a specific backup file.
     */
    fun deleteBackup(file: File): Boolean = file.delete()

    /**
     * Rotates backups, keeping only the newest `maxBackupCount`.
     */
    private fun rotateBackups() {
        val backups = getLocalBackups()
        val maxCount = preferences.maxBackupCount
        if (backups.size > maxCount) {
            backups.drop(maxCount).forEach { backup ->
                backup.file.delete()
                Log.i(TAG, "Rotated old backup: ${backup.name}")
            }
        }
    }

    companion object {
        private const val TAG = "BackupManager"
    }
}
