package com.example.graymatter.android.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecurityPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "graymatter_security_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_APP_LOCK, value).apply()
            _appLockState.value = value
        }

    private val _appLockState = MutableStateFlow(isAppLockEnabled)
    val appLockState: StateFlow<Boolean> = _appLockState.asStateFlow()

    var isScreenSecurityEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_SECURITY, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SCREEN_SECURITY, value).apply()
            _screenSecurityState.value = value
        }

    private val _screenSecurityState = MutableStateFlow(isScreenSecurityEnabled)
    val screenSecurityState: StateFlow<Boolean> = _screenSecurityState.asStateFlow()

    var lockTimeoutSeconds: Long
        get() = prefs.getLong(KEY_LOCK_TIMEOUT, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCK_TIMEOUT, value).apply()

    var lastActiveTime: Long
        get() = prefs.getLong(KEY_LAST_ACTIVE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ACTIVE, value).apply()

    companion object {
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_SCREEN_SECURITY = "screen_security_enabled"
        private const val KEY_LOCK_TIMEOUT = "lock_timeout_seconds"
        private const val KEY_LAST_ACTIVE = "last_active_timestamp"
        
        @Volatile
        private var INSTANCE: SecurityPreferences? = null

        fun getInstance(context: Context): SecurityPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
