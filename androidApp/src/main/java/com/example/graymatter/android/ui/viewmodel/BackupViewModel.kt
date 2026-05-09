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
    val statusMessage: String? = null
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
            localBackups = manager.getLocalBackups()
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

    fun restoreFromBackup(backupUri: Uri, password: String) {
        _uiState.value = _uiState.value.copy(isRestoring = true, statusMessage = null)
        viewModelScope.launch(Dispatchers.IO) {
            val success = manager.restoreFromBackup(backupUri, password)
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

        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            repeatInterval = frequency.intervalHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).setConstraints(
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
