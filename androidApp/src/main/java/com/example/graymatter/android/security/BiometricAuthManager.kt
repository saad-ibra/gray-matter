package com.example.graymatter.android.security

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages biometric authentication.
 *
 * Uses standard BiometricPrompt without CryptoObject to ensure maximum device
 * compatibility and prevent fatal hardware Keystore crashes on app startup that
 * affect certain OEM devices (causing Rescue Party / recovery reboots).
 */
class BiometricAuthManager {

    companion object {
        private const val TAG = "BiometricAuth"
    }

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _biometricAvailable = MutableStateFlow(false)
    val biometricAvailable: StateFlow<Boolean> = _biometricAvailable.asStateFlow()

    /**
     * Checks if biometric authentication is available on the device.
     */
    fun checkAvailability(activity: FragmentActivity) {
        val biometricManager = BiometricManager.from(activity)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        _biometricAvailable.value = (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS)

        if (!_biometricAvailable.value) {
            Log.w(TAG, "Biometric auth not available (code=$canAuthenticate), auto-unlocking")
            _isUnlocked.value = true
        }
    }

    /**
     * Triggers the biometric prompt.
     */
    fun authenticate(activity: FragmentActivity) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor, authCallback)

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Gray Matter")
                .setSubtitle("Verify your identity to access your data")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start biometric authentication, auto-unlocking", e)
            _isUnlocked.value = true
        }
    }

    fun lock() {
        _isUnlocked.value = false
    }

    private val authCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            Log.i(TAG, "Biometric authentication succeeded")
            _isUnlocked.value = true
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Log.w(TAG, "Biometric authentication error: $errorCode - $errString")
            // Error code 13 (ERROR_NEGATIVE_BUTTON) or 10 (ERROR_USER_CANCELED) 
            // should not auto-unlock. Let the user tap "Unlock" to retry.
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Log.w(TAG, "Biometric authentication failed")
        }
    }
}

