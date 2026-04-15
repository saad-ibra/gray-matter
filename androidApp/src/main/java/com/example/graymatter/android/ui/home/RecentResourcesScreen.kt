package com.example.graymatter.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.components.RecentItemCard
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentResourcesScreen(
    homeViewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val recentItems by homeViewModel.allRecentResourceEntryDetails.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Recent Activity", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GrayMatterColors.BackgroundDark,
                    titleContentColor = GrayMatterColors.TextPrimary,
                    navigationIconContentColor = GrayMatterColors.TextPrimary
                )
            )
        },
        containerColor = GrayMatterColors.BackgroundDark
    ) { paddingValues ->
        if (recentItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No recent activity",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrayMatterColors.Neutral600
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(recentItems, key = { it.resourceEntry.id }) { details ->
                    RecentItemCard(
                        title = details.resource.title ?: "Untitled Resource",
                        time = formatTimeAgo(maxOf(details.resourceEntry.firstOpinionAt, 0L)), // fallback
                        type = details.resource.type,
                        onClick = { onItemClick(details.resourceEntry.id) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (1000 * 60)
    val hours = mins / 60
    val days = hours / 24

    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "on ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))}"
    }
}
