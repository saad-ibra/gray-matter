package com.example.graymatter.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import kotlinx.coroutines.delay

@Composable
fun UndoSnackbar(
    message: String,
    onUndo: () -> Unit,
    onDismissRequest: () -> Unit,
    durationMillis: Long = 6000L,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(1f) }
    
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < durationMillis) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = 1f - (elapsed.toFloat() / durationMillis.toFloat())
            delay(16) // ~60fps
        }
        progress = 0f
        onDismissRequest()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GrayMatterColors.SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                color = GrayMatterColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { 
                    onUndo() 
                    onDismissRequest()
                }) {
                    Text(
                        text = "UNDO",
                        color = GrayMatterColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(24.dp),
                        color = GrayMatterColors.Primary,
                        trackColor = GrayMatterColors.Neutral800,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
