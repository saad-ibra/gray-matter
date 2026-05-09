package com.example.graymatter.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.R

/**
 * Minimal lock screen with a black background and a small logo.
 * Designed to be as unobtrusive as possible while security is verified.
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
            .background(Color.Black), // Black background as requested
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Small logo
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp) // Small logo size
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Minimal fallback button
            TextButton(
                onClick = onAuthenticate,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text(text = "Unlock")
            }
        }
    }
}
