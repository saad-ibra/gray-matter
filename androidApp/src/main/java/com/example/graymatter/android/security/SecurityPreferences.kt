package com.example.graymatter.android.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecurityPreferences(context: Context) {

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
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK, value).apply()

    var isScreenSecurityEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_SECURITY, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_SECURITY, value).apply()

    companion object {
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_SCREEN_SECURITY = "screen_security_enabled"
    }
}
