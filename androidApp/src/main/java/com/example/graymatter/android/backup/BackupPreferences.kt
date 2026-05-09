package com.example.graymatter.android.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Manages backup settings using EncryptedSharedPreferences.
 * The master password and all settings are stored encrypted at rest
 * using Android Keystore-backed keys.
 */
class BackupPreferences(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "graymatter_backup_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var masterPassword: String?
        get() = prefs.getString(KEY_MASTER_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_MASTER_PASSWORD, value).apply()

    var backupFrequency: BackupFrequency
        get() = BackupFrequency.fromOrdinal(prefs.getInt(KEY_FREQUENCY, BackupFrequency.DAILY.ordinal))
        set(value) = prefs.edit().putInt(KEY_FREQUENCY, value.ordinal).apply()

    var maxBackupCount: Int
        get() = prefs.getInt(KEY_MAX_BACKUPS, 3)
        set(value) = prefs.edit().putInt(KEY_MAX_BACKUPS, value).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIME, value).apply()

    var lastBackupSizeBytes: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_SIZE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_SIZE, value).apply()

    var lastBackupSuccess: Boolean
        get() = prefs.getBoolean(KEY_LAST_BACKUP_SUCCESS, true)
        set(value) = prefs.edit().putBoolean(KEY_LAST_BACKUP_SUCCESS, value).apply()

    fun hasPassword(): Boolean = !masterPassword.isNullOrEmpty()

    companion object {
        private const val KEY_MASTER_PASSWORD = "master_password"
        private const val KEY_FREQUENCY = "backup_frequency"
        private const val KEY_MAX_BACKUPS = "max_backup_count"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_timestamp"
        private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
        private const val KEY_LAST_BACKUP_SUCCESS = "last_backup_success"
    }
}

enum class BackupFrequency(val label: String, val intervalHours: Long) {
    DAILY("Daily", 24),
    WEEKLY("Weekly", 24 * 7),
    MONTHLY("Monthly", 24 * 30);

    companion object {
        fun fromOrdinal(ordinal: Int): BackupFrequency =
            entries.getOrElse(ordinal) { DAILY }
    }
}
