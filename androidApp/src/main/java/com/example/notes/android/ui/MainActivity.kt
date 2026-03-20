package com.example.notes.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import android.content.res.Configuration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.notes.android.NotesAppApplication
import com.example.notes.android.ui.navigation.NotesAppNavigation
import com.example.notes.android.ui.theme.NotesAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // make the app edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // set appropriate status bar colors so that the items in the status bar are legible
        setStatusBarColor()
        val application = this.application as NotesAppApplication
        val appModule = application.appModule
        setContent {
            NotesAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    content = { NotesAppNavigation(appModule = appModule) }
                )
            }
        }
    }

    private fun setStatusBarColor() {
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        val isDarkThemeActive = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        controller?.isAppearanceLightStatusBars = !isDarkThemeActive
    }
}

