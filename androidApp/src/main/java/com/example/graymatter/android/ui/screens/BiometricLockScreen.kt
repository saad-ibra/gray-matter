package com.example.graymatter.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Minimal lock screen that requires biometric/device credential
 * authentication before showing app content.
 *
 * This composable automatically triggers the biometric prompt on first display.
 * It is kept intentionally blank to abstract security features from the user.
 */
@Composable
fun BiometricLockScreen(
    onAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-trigger biometric prompt on first composition
    LaunchedEffect(Unit) {
        onAuthenticate()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Minimal fallback button in case the system prompt is dismissed
        Button(
            onClick = onAuthenticate,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = "Unlock")
        }
    }
}
