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
import androidx.lifecycle.lifecycleScope
import com.example.graymatter.android.preferences.AppPreferences
import com.example.graymatter.android.preferences.AppTheme
import com.example.graymatter.android.security.BiometricAuthManager
import com.example.graymatter.android.security.SecurityPreferences
import com.example.graymatter.android.ui.screens.BiometricLockScreen
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import kotlinx.coroutines.launch

/**
 * Main Activity for Gray Matter app.
 *
 * Security measures applied here:
 * - FLAG_SECURE: prevents screenshots, screen recordings, and recent-apps previews
 * - Biometric gate: requires hardware-verified authentication before showing content
 */
class MainActivity : FragmentActivity() {

    private val biometricAuthManager = BiometricAuthManager()
    private lateinit var securityPreferences: SecurityPreferences
    private lateinit var appPreferences: AppPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)
        
        securityPreferences = SecurityPreferences.getInstance(this)
        appPreferences = AppPreferences.getInstance(this)

        // Check biometric availability on this device
        biometricAuthManager.checkAvailability(this)
        
        // On cold start: if app lock is enabled AND the device supports biometrics,
        // lock the app so the user must authenticate before seeing content.
        // If app lock is toggled ON *during* a session, the user stays unlocked
        // because isUnlocked defaults to true — the lock applies on next launch.
        if (securityPreferences.isAppLockEnabled && biometricAuthManager.biometricAvailable.value) {
            biometricAuthManager.lock()
        }

        // ── Screen Security (FLAG_SECURE) ──────────────────────────────
        // Apply immediately on cold start
        applyScreenSecurity(securityPreferences.isScreenSecurityEnabled)
        
        // Observe changes reactively via a lifecycle-scoped coroutine.
        // This runs on the main thread and is NOT affected by Compose
        // recomposition, navigation, or DisposableEffect quirks.
        lifecycleScope.launch {
            securityPreferences.screenSecurityState.collect { isSecure ->
                applyScreenSecurity(isSecure)
            }
        }
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // ── Handle incoming intents ──────────────────────────────
        var initialSharedUri: android.net.Uri? = null
        if (intent.action == android.content.Intent.ACTION_SEND) {
            initialSharedUri = intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
                ?: intent.clipData?.getItemAt(0)?.uri
        } else if (intent.action == android.content.Intent.ACTION_VIEW) {
            initialSharedUri = intent.data
        }

        setContent {
            // ── Theme ──────────────────────────────────────────────────
            val themeChoice by appPreferences.themeState.collectAsState()
            val darkTheme = when (themeChoice) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            
            // ── Keep Screen Awake ──────────────────────────────────────
            val keepAwake by appPreferences.keepScreenAwakeState.collectAsState()
            if (keepAwake) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            // ── App Content ────────────────────────────────────────────
            GrayMatterTheme(darkTheme = darkTheme) {
                val isAppLockEnabled by securityPreferences.appLockState.collectAsState()
                if (isAppLockEnabled) {
                    val isUnlocked by biometricAuthManager.isUnlocked.collectAsState()
                    if (isUnlocked) {
                        GrayMatterApp(initialSharedUri = initialSharedUri)
                    } else {
                        BiometricLockScreen(
                            onAuthenticate = { biometricAuthManager.authenticate(this@MainActivity) }
                        )
                    }
                } else {
                    GrayMatterApp(initialSharedUri = initialSharedUri)
                }
            }
        }
    }

    /**
     * Applies or removes FLAG_SECURE on the window.
     * Called from both onCreate and the reactive flow collector.
     */
    private fun applyScreenSecurity(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onResume() {
        super.onResume()
        // Belt-and-suspenders: re-apply screen security on every resume
        if (::securityPreferences.isInitialized) {
            applyScreenSecurity(securityPreferences.isScreenSecurityEnabled)
        }
    }

    override fun onPause() {
        super.onPause()
        // Optional: re-lock when app goes to background for extended time
        // Uncomment the line below for stricter security (re-lock on every pause)
        // biometricAuthManager.lock()
    }
}
