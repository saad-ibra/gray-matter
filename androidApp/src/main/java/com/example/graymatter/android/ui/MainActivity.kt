package com.example.graymatter.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.graymatter.android.GrayMatterApplication
import com.example.graymatter.android.ui.theme.GrayMatterTheme

/**
 * Main Activity for Gray Matter app.
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        val appModule = (application as GrayMatterApplication).appModule
        
        setContent {
            GrayMatterTheme(darkTheme = true) {
                GrayMatterApp(appModule = appModule)
            }
        }
    }
}
