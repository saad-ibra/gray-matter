package com.example.graymatter.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.viewmodel.LookupsViewModel
import com.example.graymatter.domain.Opinion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupsScreen(
    viewModel: LookupsViewModel,
    onBackClick: () -> Unit,
    onNavigateToOrigin: (itemId: String, opinionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLookups by viewModel.activeLookups.collectAsState()
    val learntLookups by viewModel.learntLookups.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Learning, 1 = Learnt

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GrayMatterColors.BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Lookup Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GrayMatterColors.BackgroundDark,
                    titleContentColor = GrayMatterColors.TextPrimary,
                    navigationIconContentColor = GrayMatterColors.TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = GrayMatterColors.BackgroundDark,
                contentColor = GrayMatterColors.Primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GrayMatterColors.Primary
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Learning", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Learnt", fontWeight = FontWeight.SemiBold) }
                )
            }

            val currentList = if (selectedTab == 0) activeLookups else learntLookups

            if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTab == 0) "No lookups currently learning." else "No learnt lookups yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GrayMatterColors.Neutral500
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                ) {
                    items(currentList, key = { it.id }) { opinion ->
                        LookupItem(
                            opinion = opinion,
                            isLearnt = selectedTab == 1,
                            onToggleLearnt = { viewModel.toggleLearntStatus(opinion) },
                            onJumpToOrigin = { onNavigateToOrigin(opinion.itemId, opinion.id) }
                        )
                        HorizontalDivider(color = GrayMatterColors.Neutral800, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupItem(
    opinion: Opinion,
    isLearnt: Boolean,
    onToggleLearnt: () -> Unit,
    onJumpToOrigin: () -> Unit
) {
    // Strip [DICT:42], [DICT], and #learnt tags
    val regex = Regex("\\[DICT(:\\d+)?\\]\\s*")
    val cleanWord = opinion.text.replace(regex, "").replace(" #learnt", "").trim()
    
    // Opacity based logic using a standard white text color
    val alpha = if (isLearnt) 0.5f else 1.0f

    ListItem(
        headlineContent = { 
            Text(
                text = cleanWord, 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isLearnt) FontWeight.Normal else FontWeight.Bold),
                color = GrayMatterColors.TextPrimary.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            ) 
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleLearnt) {
                    Icon(
                        imageVector = if (isLearnt) Icons.Default.Restore else Icons.Default.CheckCircleOutline,
                        contentDescription = if (isLearnt) "Restore to Learning" else "Mark as Learnt",
                        tint = GrayMatterColors.TextPrimary.copy(alpha = alpha)
                    )
                }
                IconButton(onClick = onJumpToOrigin) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "View Original Context",
                        tint = GrayMatterColors.TextPrimary.copy(alpha = alpha)
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onJumpToOrigin)
    )
}
