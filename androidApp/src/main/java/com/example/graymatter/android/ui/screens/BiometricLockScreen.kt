package com.example.graymatter.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.graymatter.core.designsystem.R
import com.example.graymatter.android.ui.theme.GrayMatterTheme

/**
 * Lock screen shown when biometric authentication is required.
 * Displays the app branding and a single unlock button.
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
            .background(GrayMatterTheme.colors.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .align(Alignment.Center)
                .offset(y = (-60).dp)
        ) {
            // Squircle logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_full),
                    contentDescription = "Relatrix",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App name
            Text(
                text = "Relatrix",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = GrayMatterTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Sleek Unlock button
            OutlinedButton(
                onClick = onAuthenticate,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GrayMatterTheme.colors.primary,
                    containerColor = GrayMatterTheme.colors.primary.copy(alpha = 0.05f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    GrayMatterTheme.colors.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tap to Unlock",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}
