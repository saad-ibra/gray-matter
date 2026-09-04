package com.example.graymatter.android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicColorPickerSheet(
    initialColor: String?,
    recentColors: List<String>,
    onColorSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultColors = listOf(
        "#E57373", "#F06292", "#BA68C8", "#9575CD",
        "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1",
        "#4DB6AC", "#81C784", "#AED581", "#DCE775",
        "#FFD54F", "#FFB74D", "#FF8A65", "#A1887F"
    )

    val allColors = remember(recentColors) {
        (recentColors + defaultColors).distinct().take(20)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GrayMatterTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Topic Color",
                style = MaterialTheme.typography.titleMedium,
                color = GrayMatterTheme.colors.textPrimary
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Clear color option
                item {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GrayMatterTheme.colors.background)
                            .border(1.dp, GrayMatterTheme.colors.neutral700, CircleShape)
                            .clickable {
                                onColorSelected(null)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = GrayMatterTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(allColors) { hexColor ->
                    val color = remember(hexColor) {
                        try { Color(android.graphics.Color.parseColor(hexColor)) }
                        catch (e: Exception) { Color.Gray }
                    }
                    val isSelected = initialColor.equals(hexColor, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) GrayMatterTheme.colors.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                onColorSelected(hexColor)
                                onDismiss()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
