package com.example.graymatter.android.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

class AppPreferences private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("graymatter_app_prefs", Context.MODE_PRIVATE)

    var appTheme: AppTheme
        get() {
            val themeString = prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
            return try {
                AppTheme.valueOf(themeString)
            } catch (e: IllegalArgumentException) {
                AppTheme.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString("app_theme", value.name).apply()
            _themeState.value = value
        }

    private val _themeState = MutableStateFlow(appTheme)
    val themeState: StateFlow<AppTheme> = _themeState.asStateFlow()

    var isKeepScreenAwakeEnabled: Boolean
        get() = prefs.getBoolean("keep_screen_awake", true) // Default is ON per user request
        set(value) {
            prefs.edit().putBoolean("keep_screen_awake", value).apply()
            _keepScreenAwakeState.value = value
        }

    private val _keepScreenAwakeState = MutableStateFlow(isKeepScreenAwakeEnabled)
    val keepScreenAwakeState: StateFlow<Boolean> = _keepScreenAwakeState.asStateFlow()

    var isConfirmBeforeDeleteEnabled: Boolean
        get() = prefs.getBoolean("confirm_before_delete", true)
        set(value) {
            prefs.edit().putBoolean("confirm_before_delete", value).apply()
        }

    var defaultPdfTheme: String
        get() = prefs.getString("default_pdf_theme", "daylight") ?: "daylight"
        set(value) {
            prefs.edit().putString("default_pdf_theme", value).apply()
        }

    companion object {
        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
