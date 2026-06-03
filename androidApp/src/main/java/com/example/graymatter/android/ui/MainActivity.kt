package com.example.graymatter.android.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.example.graymatter.android.GrayMatterApplication
import com.example.graymatter.android.preferences.AppPreferences
import com.example.graymatter.android.preferences.AppTheme
import com.example.graymatter.android.security.BiometricAuthManager
import com.example.graymatter.android.ui.screens.BiometricLockScreen
import com.example.graymatter.android.ui.theme.GrayMatterTheme

/**
 * Main Activity for Gray Matter app.
 *
 * Security measures applied here:
 * - FLAG_SECURE: prevents screenshots, screen recordings, and recent-apps previews
 * - Biometric gate: requires hardware-verified authentication before showing content
 */
class MainActivity : FragmentActivity() {

    private val biometricAuthManager = BiometricAuthManager()
    private lateinit var securityPreferences: com.example.graymatter.android.security.SecurityPreferences
    private lateinit var appPreferences: AppPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)
        
        securityPreferences = com.example.graymatter.android.security.SecurityPreferences(this)
        appPreferences = AppPreferences.getInstance(this)

        // Prevent screenshots, screen recordings, and recent-apps preview if enabled.
        if (securityPreferences.isScreenSecurityEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        
        // Check biometric availability (auto-unlocks if not available)
        biometricAuthManager.checkAvailability(this)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            // Observe theme preference reactively
            val themeChoice by appPreferences.themeState.collectAsState()
            val darkTheme = when (themeChoice) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            
            // Observe keep-screen-awake preference reactively
            val keepAwake by appPreferences.keepScreenAwakeState.collectAsState()
            if (keepAwake) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            GrayMatterTheme(darkTheme = darkTheme) {
                val isUnlocked by biometricAuthManager.isUnlocked.collectAsState()
                val isAppLockEnabled = securityPreferences.isAppLockEnabled

                if (isUnlocked || !isAppLockEnabled) {
                    GrayMatterApp()
                } else {
                    BiometricLockScreen(
                        onAuthenticate = { biometricAuthManager.authenticate(this@MainActivity) }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Optional: re-lock when app goes to background for extended time
        // Uncomment the line below for stricter security (re-lock on every pause)
        // biometricAuthManager.lock()
    }
}
