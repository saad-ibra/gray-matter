package com.example.graymatter.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.graymatter.android.backup.BackupPreferences
import com.example.graymatter.android.security.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SecurityUiState(
    val isAppLockEnabled: Boolean = true,
    val isScreenSecurityEnabled: Boolean = true,
    val isMasterPasswordSet: Boolean = false,
    val statusMessage: String? = null
)

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val securityPrefs = SecurityPreferences(application)
    private val backupPrefs = BackupPreferences(application)

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        _uiState.value = SecurityUiState(
            isAppLockEnabled = securityPrefs.isAppLockEnabled,
            isScreenSecurityEnabled = securityPrefs.isScreenSecurityEnabled,
            isMasterPasswordSet = backupPrefs.hasPassword()
        )
    }

    fun setAppLockEnabled(enabled: Boolean) {
        securityPrefs.isAppLockEnabled = enabled
        _uiState.value = _uiState.value.copy(isAppLockEnabled = enabled)
    }

    fun setScreenSecurityEnabled(enabled: Boolean) {
        securityPrefs.isScreenSecurityEnabled = enabled
        _uiState.value = _uiState.value.copy(isScreenSecurityEnabled = enabled)
    }

    fun setMasterPassword(password: String) {
        backupPrefs.masterPassword = password
        _uiState.value = _uiState.value.copy(
            isMasterPasswordSet = true,
            statusMessage = "Master password updated"
        )
    }

    fun verifyMasterPassword(password: String): Boolean {
        return backupPrefs.masterPassword == password
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
