package com.example.graymatter.android.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.components.RecentItemCard
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import com.example.graymatter.android.ui.viewmodel.TagViewModel
import com.example.graymatter.android.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEntriesScreen(
    tagId: String,
    tagViewModel: TagViewModel,
    homeViewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val tags by tagViewModel.allTags.collectAsState()
    val currentTag = tags.find { it.id == tagId }
    val tagName = currentTag?.name ?: "Unknown Tag"

    val opinionsByTag by tagViewModel.getOpinionsByTagId(tagId).collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        tagName, 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GrayMatterTheme.colors.background,
                    titleContentColor = GrayMatterTheme.colors.textPrimary,
                    navigationIconContentColor = GrayMatterTheme.colors.textPrimary
                )
            )
        },
        containerColor = GrayMatterTheme.colors.background
    ) { paddingValues ->
        if (opinionsByTag.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No entries found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrayMatterTheme.colors.neutral600
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(opinionsByTag, key = { it.id }) { opinion ->
                    RecentItemCard(
                        title = opinion.text.take(50) + if (opinion.text.length > 50) "..." else "",
                        time = formatTimeAgo(opinion.createdAt),
                        type = com.example.graymatter.domain.ResourceType.MARKDOWN,
                        onClick = { onItemClick(opinion.itemId) },
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
