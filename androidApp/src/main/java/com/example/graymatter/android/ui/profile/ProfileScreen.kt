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
import androidx.compose.ui.platform.LocalContext
import com.example.graymatter.android.preferences.AppPreferences
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
    onNavigateToTags: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTutorial by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences.getInstance(context) }
    var currentUrl by remember { mutableStateOf(appPreferences.lookupUrl) }

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
                    icon = Icons.Default.Style,
                    title = "Tag Management",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = onNavigateToTags
                )
                
                SettingsButton(
                    icon = Icons.Default.Public,
                    title = "Lookup Search Engine",
                    tint = GrayMatterTheme.colors.primary,
                    onClick = { showSearchEngineDialog = true }
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

    if (showSearchEngineDialog) {
        var selectedEngine by remember { 
            mutableStateOf(
                when (currentUrl) {
                    "https://duckduckgo.com/?q=" -> "DuckDuckGo"
                    "https://www.google.com/search?q=" -> "Google"
                    "https://www.bing.com/search?q=" -> "Bing"
                    "https://search.yahoo.com/search?p=" -> "Yahoo"
                    else -> "Custom"
                }
            )
        }
        
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { Text("Search Engine URL") },
            text = {
                Column {
                    Text("Select your preferred search engine for lookups.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val engines = listOf("DuckDuckGo", "Google", "Bing", "Yahoo", "Custom")
                    engines.forEach { engine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedEngine = engine
                                    when (engine) {
                                        "DuckDuckGo" -> currentUrl = "https://duckduckgo.com/?q="
                                        "Google" -> currentUrl = "https://www.google.com/search?q="
                                        "Bing" -> currentUrl = "https://www.bing.com/search?q="
                                        "Yahoo" -> currentUrl = "https://search.yahoo.com/search?p="
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedEngine == engine,
                                onClick = { 
                                    selectedEngine = engine
                                    when (engine) {
                                        "DuckDuckGo" -> currentUrl = "https://duckduckgo.com/?q="
                                        "Google" -> currentUrl = "https://www.google.com/search?q="
                                        "Bing" -> currentUrl = "https://www.bing.com/search?q="
                                        "Yahoo" -> currentUrl = "https://search.yahoo.com/search?p="
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(engine)
                        }
                    }
                    
                    if (selectedEngine == "Custom") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Enter custom URL prefix (selected text will be appended):", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = currentUrl,
                            onValueChange = { currentUrl = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    appPreferences.lookupUrl = currentUrl
                    showSearchEngineDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    currentUrl = appPreferences.lookupUrl
                    showSearchEngineDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
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
