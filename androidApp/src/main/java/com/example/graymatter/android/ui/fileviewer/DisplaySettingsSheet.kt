package com.example.graymatter.android.ui.fileviewer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.domain.ReadingSettings

/**
 * Display settings bottom sheet for configuring themes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsSheet(
    settings: ReadingSettings,
    onSettingsChanged: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1E),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Display Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ── Themes List ──
            ThemeGroup("Light Modes", listOf(
                ThemeOption("Daylight", "daylight", Color.White, Color.Black),
                ThemeOption("Paper Classic", "paper_classic", Color(0xFFF4ECD8), Color(0xFF333333)),
                ThemeOption("Book Linen", "book_linen", Color(0xFFFAF9F6), Color(0xFF3E3E3E))
            ), settings.theme, onSettingsChanged, settings)

            ThemeGroup("Dark Modes", listOf(
                ThemeOption("Night", "night", Color(0xFF121212), Color(0xFFB0B0B0)),
                ThemeOption("AMOLED Black", "amoled_black", Color.Black, Color.White),
                ThemeOption("Twilight", "twilight", Color(0xFF1A1B26), Color(0xFFC0CAF5)),
                ThemeOption("Console", "console", Color(0xFF0D0D0D), Color(0xFF4AF626))
            ), settings.theme, onSettingsChanged, settings)

            ThemeGroup("Warm Modes", listOf(
                ThemeOption("Sepia", "sepia", Color(0xFF704214), Color(0xFFEBD5B3)),
                ThemeOption("Vintage Parchment", "vintage_parchment", Color(0xFFF1E5AC), Color(0xFF4E342E))
            ), settings.theme, onSettingsChanged, settings)

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(
                label = "Left-handed Optimization",
                checked = settings.isLeftHanded,
                onCheckedChange = { onSettingsChanged(settings.copy(isLeftHanded = it)) }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

data class ThemeOption(val label: String, val id: String, val bg: Color, val text: Color)

@Composable
private fun ThemeGroup(
    title: String,
    options: List<ThemeOption>,
    currentTheme: String,
    onSettingsChanged: (ReadingSettings) -> Unit,
    settings: ReadingSettings
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(options) { option ->
                ThemeCircle(option.label, option.bg, option.text, currentTheme == option.id) {
                    onSettingsChanged(settings.copy(theme = option.id))
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ThemeCircle(
    label: String,
    bgColor: Color,
    textColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Color(0xFF4A90D9) else Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label, 
            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f), 
            fontSize = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF22C55E)
            )
        )
    }
}
