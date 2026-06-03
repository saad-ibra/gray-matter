package com.example.graymatter.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.preferences.AppPreferences
import com.example.graymatter.android.preferences.AppTheme
import com.example.graymatter.android.ui.theme.GrayMatterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    // Ideally use a ViewModel here, but for simplicity we directly use AppPreferences
    val appPreferences = AppPreferences.getInstance(context)
    val currentTheme by appPreferences.themeState.collectAsState()
    val keepScreenAwake by appPreferences.keepScreenAwakeState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Appearance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = GrayMatterTheme.colors.textPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GrayMatterTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GrayMatterTheme.colors.background
                )
            )
        },
        containerColor = GrayMatterTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = GrayMatterTheme.colors.textSecondary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "Light",
                        icon = Icons.Default.LightMode,
                        isSelected = currentTheme == AppTheme.LIGHT,
                        onClick = { appPreferences.appTheme = AppTheme.LIGHT }
                    )
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "Dark",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentTheme == AppTheme.DARK,
                        onClick = { appPreferences.appTheme = AppTheme.DARK }
                    )
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "System",
                        icon = Icons.Default.SettingsSuggest,
                        isSelected = currentTheme == AppTheme.SYSTEM,
                        onClick = { appPreferences.appTheme = AppTheme.SYSTEM }
                    )
                }
            }

            Divider(color = GrayMatterTheme.colors.neutral800)

            // Display Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Display",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = GrayMatterTheme.colors.textSecondary
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GrayMatterTheme.colors.surfaceCard)
                        .clickable { appPreferences.isKeepScreenAwakeEnabled = !keepScreenAwake }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Keep Screen Awake",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = GrayMatterTheme.colors.textPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Prevent the device from sleeping while using the app",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = GrayMatterTheme.colors.textSecondary
                            )
                        )
                    }
                    Switch(
                        checked = keepScreenAwake,
                        onCheckedChange = { appPreferences.isKeepScreenAwakeEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GrayMatterTheme.colors.onPrimary,
                            checkedTrackColor = GrayMatterTheme.colors.primary,
                            uncheckedThumbColor = GrayMatterTheme.colors.neutral500,
                            uncheckedTrackColor = GrayMatterTheme.colors.surfaceInput
                        )
                    )
                }
            }
            

        }
    }
}

@Composable
private fun ThemeOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) GrayMatterTheme.colors.primary else GrayMatterTheme.colors.neutral800
    val backgroundColor = if (isSelected) GrayMatterTheme.colors.primary.copy(alpha = 0.05f) else GrayMatterTheme.colors.surfaceCard
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) GrayMatterTheme.colors.primary else GrayMatterTheme.colors.neutral500,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) GrayMatterTheme.colors.primary else GrayMatterTheme.colors.textSecondary
            )
        )
    }
}
