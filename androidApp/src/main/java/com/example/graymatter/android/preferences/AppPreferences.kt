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


    var recentTopicColorsList: List<String>
        get() {
            val str = prefs.getString("recent_topic_colors", "") ?: ""
            return if (str.isEmpty()) emptyList() else str.split(",")
        }
        set(value) {
            prefs.edit().putString("recent_topic_colors", value.joinToString(",")).apply()
            _recentTopicColorsState.value = value
        }
        
    private val _recentTopicColorsState = MutableStateFlow(recentTopicColorsList)
    val recentTopicColors: StateFlow<List<String>> = _recentTopicColorsState.asStateFlow()
    
    fun addRecentTopicColor(hexColor: String) {
        val current = recentTopicColorsList.toMutableList()
        current.remove(hexColor)
        current.add(0, hexColor)
        while(current.size > 18) {
            current.removeLast()
        }
        recentTopicColorsList = current
    }

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

    var lookupUrl: String
        get() = prefs.getString("lookup_url", "https://duckduckgo.com/?q=") ?: "https://duckduckgo.com/?q="
        set(value) {
            prefs.edit().putString("lookup_url", value).apply()
        }

    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean("has_seen_tutorial", false)
        set(value) {
            prefs.edit().putBoolean("has_seen_tutorial", value).apply()
        }

    var librarySortOption: String
        get() = prefs.getString("library_sort_option", "CUSTOM") ?: "CUSTOM"
        set(value) = prefs.edit().putString("library_sort_option", value).apply()

    var libraryGroupOption: String
        get() = prefs.getString("library_group_option", "NONE") ?: "NONE"
        set(value) = prefs.edit().putString("library_group_option", value).apply()

    fun getTopicSortOption(topicId: String): String {
        return prefs.getString("topic_sort_$topicId", "DATE_MODIFIED") ?: "DATE_MODIFIED"
    }

    fun setTopicSortOption(topicId: String, value: String) {
        prefs.edit().putString("topic_sort_$topicId", value).apply()
    }

    fun getTopicGroupOption(topicId: String): String {
        return prefs.getString("topic_group_$topicId", "NONE") ?: "NONE"
    }

    fun setTopicGroupOption(topicId: String, value: String) {
        prefs.edit().putString("topic_group_$topicId", value).apply()
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
