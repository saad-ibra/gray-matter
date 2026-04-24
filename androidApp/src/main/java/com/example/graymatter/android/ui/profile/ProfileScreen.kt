package com.example.graymatter.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterColors

@Composable
fun ProfileScreen(
    onNavigateToGraph: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToRecentlyDeleted: () -> Unit,
    onNavigateToLookups: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                ProfileHeader()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsButton(
                    icon = Icons.Default.Hub,
                    title = "Relatrix",
                    tint = GrayMatterColors.Primary,
                    onClick = onNavigateToGraph
                )
                SettingsButton(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    title = "Template Management",
                    tint = GrayMatterColors.Primary,
                    onClick = onNavigateToTemplates
                )
                SettingsButton(
                    icon = Icons.Default.Search,
                    title = "Lookup Management",
                    tint = GrayMatterColors.Primary,
                    onClick = onNavigateToLookups
                )
                SettingsButton(
                    icon = Icons.Default.Restore,
                    title = "Recently Deleted",
                    tint = GrayMatterColors.Primary,
                    onClick = onNavigateToRecentlyDeleted
                )
            }
        }
    }
}

@Composable
private fun SettingsButton(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tint: androidx.compose.ui.graphics.Color = GrayMatterColors.Primary, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp))
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
                    color = GrayMatterColors.TextPrimary
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = GrayMatterColors.Neutral700)
        }
    }
}

@Composable
private fun ProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterColors.TextPrimary
        )
    }
}
