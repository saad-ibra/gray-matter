package com.example.graymatter.android.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material.icons.filled.Share
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEntriesScreen(
    tagId: String,
    tagViewModel: TagViewModel,
    homeViewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onItemClick: (String, String) -> Unit,
    onExportPdf: () -> Unit = {}
) {
    val tags by tagViewModel.allTags.collectAsState()
    val currentTag = tags.find { it.id == tagId }
    val tagName = currentTag?.name ?: "Unknown Tag"

    val opinionsByTag by tagViewModel.getOpinionsByTagId(tagId).collectAsState(initial = emptyList())
    
    var selectedOpinionIds by remember { mutableStateOf(setOf<String>()) }
    val inSelectionMode = selectedOpinionIds.isNotEmpty()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            if (inSelectionMode) {
                TopAppBar(
                    title = { 
                        Text(
                            "${selectedOpinionIds.size} Selected", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedOpinionIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            val toUntag = selectedOpinionIds.toList()
                            selectedOpinionIds = emptySet()
                            toUntag.forEach { tagViewModel.removeTagFromOpinion(it, tagId) }
                            
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Removed tag from ${toUntag.size} entries",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    toUntag.forEach { tagViewModel.addTagToOpinion(it, tagId) }
                                }
                            }
                        }) {
                            Icon(Icons.Default.LabelOff, contentDescription = "Remove tag")
                        }
                        IconButton(onClick = { 
                            val toDelete = selectedOpinionIds.toList()
                            selectedOpinionIds = emptySet()
                            toDelete.forEach { tagViewModel.softDeleteOpinion(it) }
                            
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Deleted ${toDelete.size} entries",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    toDelete.forEach { tagViewModel.undoDeleteOpinion(it) }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete entries")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GrayMatterTheme.colors.surface,
                        titleContentColor = GrayMatterTheme.colors.textPrimary,
                        navigationIconContentColor = GrayMatterTheme.colors.textPrimary,
                        actionIconContentColor = GrayMatterTheme.colors.textPrimary
                    )
                )
            } else {
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
                    actions = {
                        IconButton(onClick = onExportPdf) {
                            Icon(Icons.Default.Share, contentDescription = "Export as PDF")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GrayMatterTheme.colors.background,
                        titleContentColor = GrayMatterTheme.colors.textPrimary,
                        navigationIconContentColor = GrayMatterTheme.colors.textPrimary,
                        actionIconContentColor = GrayMatterTheme.colors.textPrimary
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    val isSelected = selectedOpinionIds.contains(opinion.id)
                    RecentItemCard(
                        title = opinion.text.take(50) + if (opinion.text.length > 50) "..." else "",
                        time = formatTimeAgo(opinion.createdAt),
                        type = com.example.graymatter.domain.ResourceType.MARKDOWN,
                        onClick = { 
                            if (inSelectionMode) {
                                selectedOpinionIds = if (isSelected) {
                                    selectedOpinionIds - opinion.id
                                } else {
                                    selectedOpinionIds + opinion.id
                                }
                            } else {
                                onItemClick(opinion.itemId, opinion.id) 
                            }
                        },
                        onLongClick = {
                            if (!inSelectionMode) {
                                selectedOpinionIds = setOf(opinion.id)
                            }
                        },
                        selected = isSelected,
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
