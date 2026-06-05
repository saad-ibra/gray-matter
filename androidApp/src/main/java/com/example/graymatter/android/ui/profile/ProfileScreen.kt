package com.example.graymatter.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.components.TutorialOverlay

@Composable
fun ProfileScreen(
    onNavigateToTemplates: () -> Unit,
    onNavigateToRecentlyDeleted: () -> Unit,
    onNavigateToLookups: () -> Unit,
    onNavigateToBackupSettings: () -> Unit = {},
    onNavigateToSecuritySettings: () -> Unit = {},
    onNavigateToAppearanceSettings: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTutorial by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                ProfileHeader(onBackClick)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Management",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
                )

                SettingsButton(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    title = "Template Management",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = onNavigateToTemplates
                )
                SettingsButton(
                    icon = Icons.Default.Search,
                    title = "Lookup Management",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = onNavigateToLookups
                )
                
                SettingsButton(
                    icon = Icons.Default.School,
                    title = "Replay Tutorial",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = { showTutorial = true }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
                )

                SettingsButton(
                    icon = Icons.Default.Restore,
                    title = "Recently Deleted",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = onNavigateToRecentlyDeleted
                )
                SettingsButton(
                    icon = Icons.Default.Backup,
                    title = "Backup",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = onNavigateToBackupSettings
                )
                SettingsButton(
                    icon = Icons.Outlined.Security,
                    title = "Security",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = onNavigateToSecuritySettings
                )
                SettingsButton(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = onNavigateToAppearanceSettings
                )
            }
        }
    }

    if (showTutorial) {
        TutorialOverlay(onDismiss = { showTutorial = false })
    }
}

@Composable
private fun SettingsButton(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tint: androidx.compose.ui.graphics.Color = GrayMatterTheme.colors.primary, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GrayMatterTheme.colors.surface)
            .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = GrayMatterTheme.colors.textPrimary
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = GrayMatterTheme.colors.neutral700)
        }
    }
}

@Composable
private fun ProfileHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(GrayMatterTheme.colors.surfaceCard)
                .border(1.dp, GrayMatterTheme.colors.neutral800, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = GrayMatterTheme.colors.textPrimary
            )
        }
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary
        )
    }
}
