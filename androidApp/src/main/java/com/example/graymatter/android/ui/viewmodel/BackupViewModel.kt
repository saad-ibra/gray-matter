package com.example.graymatter.android.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.graymatter.android.backup.BackupFrequency
import com.example.graymatter.android.backup.BackupInfo
import com.example.graymatter.android.backup.BackupManager
import com.example.graymatter.android.backup.BackupPreferences
import com.example.graymatter.android.workers.BackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class BackupUiState(
    val isPasswordSet: Boolean = false,
    val frequency: BackupFrequency = BackupFrequency.DAILY,
    val maxBackups: Int = 3,
    val lastBackupTimestamp: Long = 0L,
    val lastBackupSizeBytes: Long = 0L,
    val lastBackupSuccess: Boolean = true,
    val localBackups: List<BackupInfo> = emptyList(),
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val statusMessage: String? = null,
    val backupTimeHour: Int = 2,
    val backupTimeMinute: Int = 0,
    val is24HourFormat: Boolean = true,
    val isBackupEnabled: Boolean = false
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = BackupPreferences(application)
    private val manager = BackupManager(application, preferences)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        _uiState.value = _uiState.value.copy(
            isPasswordSet = preferences.hasPassword(),
            frequency = preferences.backupFrequency,
            maxBackups = preferences.maxBackupCount,
            lastBackupTimestamp = preferences.lastBackupTimestamp,
            lastBackupSizeBytes = preferences.lastBackupSizeBytes,
            lastBackupSuccess = preferences.lastBackupSuccess,
            localBackups = manager.getLocalBackups(),
            backupTimeHour = preferences.backupTimeHour,
            backupTimeMinute = preferences.backupTimeMinute,
            is24HourFormat = preferences.is24HourFormat,
            isBackupEnabled = preferences.isBackupEnabled
        )
    }

    fun setPassword(password: String) {
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(statusMessage = "Password must be at least 6 characters")
            return
        }
        preferences.masterPassword = password
        _uiState.value = _uiState.value.copy(
            isPasswordSet = true,
            statusMessage = "Master password set successfully"
        )
    }

    fun verifyPassword(password: String): Boolean {
        return preferences.masterPassword == password
    }

    fun setFrequency(frequency: BackupFrequency) {
        preferences.backupFrequency = frequency
        scheduleBackup(frequency)
        _uiState.value = _uiState.value.copy(frequency = frequency)
    }

    fun setMaxBackups(count: Int) {
        preferences.maxBackupCount = count
        _uiState.value = _uiState.value.copy(maxBackups = count)
    }

    fun setBackupTime(hour: Int, minute: Int) {
        preferences.backupTimeHour = hour
        preferences.backupTimeMinute = minute
        _uiState.value = _uiState.value.copy(backupTimeHour = hour, backupTimeMinute = minute)
        scheduleBackup(preferences.backupFrequency)
    }

    fun set24HourFormat(is24Hour: Boolean) {
        preferences.is24HourFormat = is24Hour
        _uiState.value = _uiState.value.copy(is24HourFormat = is24Hour)
    }

    fun setBackupEnabled(enabled: Boolean) {
        preferences.isBackupEnabled = enabled
        _uiState.value = _uiState.value.copy(isBackupEnabled = enabled)
        if (enabled && preferences.hasPassword()) {
            scheduleBackup(preferences.backupFrequency)
        } else if (!enabled) {
            WorkManager.getInstance(getApplication()).cancelUniqueWork(BACKUP_WORK_NAME)
        }
    }

    fun triggerManualBackup() {
        if (!preferences.hasPassword()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Set a master password first")
            return
        }
        _uiState.value = _uiState.value.copy(isBackingUp = true, statusMessage = null)
        viewModelScope.launch(Dispatchers.IO) {
            val result = manager.runBackup()
            _uiState.value = _uiState.value.copy(
                isBackingUp = false,
                statusMessage = if (result != null) "Backup created successfully" else "Backup failed",
                localBackups = manager.getLocalBackups(),
                lastBackupTimestamp = preferences.lastBackupTimestamp,
                lastBackupSizeBytes = preferences.lastBackupSizeBytes,
                lastBackupSuccess = preferences.lastBackupSuccess
            )
        }
    }

    fun exportBackup(backupFile: File, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = manager.exportBackup(backupFile, targetUri)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (success) "Backup exported successfully" else "Export failed"
            )
        }
    }

    /**
     * Restores from a backup file URI.
     * The password string is converted to CharArray and zeroed after use.
     */
    fun restoreFromBackup(backupUri: Uri, password: String) {
        _uiState.value = _uiState.value.copy(isRestoring = true, statusMessage = null)
        viewModelScope.launch(Dispatchers.IO) {
            val passwordChars = password.toCharArray()
            val success = manager.restoreFromBackup(backupUri, passwordChars)
            // Note: BackupManager.restoreFromBackup already zeros the CharArray
            _uiState.value = _uiState.value.copy(
                isRestoring = false,
                statusMessage = if (success) "Restore complete — please restart the app" else "Restore failed — wrong password or corrupt file"
            )
        }
    }

    fun deleteBackup(file: File) {
        manager.deleteBackup(file)
        _uiState.value = _uiState.value.copy(localBackups = manager.getLocalBackups())
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    private fun scheduleBackup(frequency: BackupFrequency) {
        val workManager = WorkManager.getInstance(getApplication())

        // Calculate initial delay to run at the specified time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, preferences.backupTimeHour)
            set(Calendar.MINUTE, preferences.backupTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val currentTime = System.currentTimeMillis()
        if (calendar.timeInMillis <= currentTime) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val initialDelay = calendar.timeInMillis - currentTime

        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            repeatInterval = frequency.intervalHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun initScheduleFromPrefs() {
        scheduleBackup(preferences.backupFrequency)
    }

    companion object {
        private const val BACKUP_WORK_NAME = "gray_matter_backup_work"
    }
}
