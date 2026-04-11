package com.example.graymatter.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel
import com.example.graymatter.domain.ResourceEntry
import com.example.graymatter.domain.ResourceEntryWithDetails
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.android.ui.components.TopicPickerSheet
import kotlinx.coroutines.launch

/**
 * Home Screen.
 * Minimalist entry point with primary action and 4 most recent entries.
 */
@Composable
fun HomeScreen(
    viewModel: GrayMatterViewModel,
    homeViewModel: com.example.graymatter.android.ui.viewmodel.HomeViewModel,
    continueReadingItem: ResourceEntryWithDetails?,
    continueReadingProgress: com.example.graymatter.domain.ReadingProgress?,
    onCreateNewEntryClick: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect the reactive stream of the 4 most recent items with details
    val recentItems by homeViewModel.recentResourceEntryDetails.collectAsState()
    val orphanEntries by homeViewModel.orphanResourceEntries.collectAsState()
    val topics by viewModel.topicsStream.collectAsState()
    
    var selectedOrphan by remember { mutableStateOf<ResourceEntry?>(null) }
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Minimal top margin
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Main action - Centered "+" button (Moved up)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AddNewEntryCard(onClick = onCreateNewEntryClick)
            }
        }
        // Orphan Resource Entries Banner
        if (orphanEntries.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GrayMatterColors.Error.copy(alpha = 0.1f))
                        .border(1.dp, GrayMatterColors.Error.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = GrayMatterColors.Error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Rogue Resources Detected",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GrayMatterColors.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Some resources are not assigned to a topic. Please organize them to ensure data integrity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayMatterColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { selectedOrphan = orphanEntries.first() },
                            colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Error),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Fix Now", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Recently Added Section (Limited to 4)
        if (recentItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Resources",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = GrayMatterColors.TextPrimary
                    )
                }
            }

            items(recentItems, key = { it.resourceEntry.id }) { details ->
                RecentItemCard(
                    title = details.resource.title ?: "Untitled Resource",
                    time = formatTime(details.resourceEntry.firstOpinionAt),
                    type = details.resource.type,
                    onClick = { onItemClick(details.resourceEntry.id) }
                )
            }
        }
    }

    if (selectedOrphan != null) {
        TopicPickerSheet(
            title = "Unorganized Entry",
            topics = topics,
            onDismiss = { selectedOrphan = null },
            onSelectTopic = { topic ->
                homeViewModel.assignTopicToResourceEntry(selectedOrphan!!.id, topic.id)
                selectedOrphan = null
            },
            onCreateNewTopic = { name ->
                scope.launch {
                    val newId = viewModel.createTopic(name)
                    homeViewModel.assignTopicToResourceEntry(selectedOrphan!!.id, newId)
                    selectedOrphan = null
                }
            }
        )
    }
}

@Composable
private fun RecentItemCard(
    title: String, 
    time: String, 
    type: ResourceType,
    onClick: () -> Unit
) {
    val icon = when (type) {
        ResourceType.WEB_LINK -> Icons.Default.Language
        ResourceType.MARKDOWN -> Icons.Default.EditNote
        ResourceType.PDF -> Icons.Default.PictureAsPdf
        ResourceType.IMAGE -> Icons.Default.Image
        else -> Icons.Default.Description
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GrayMatterColors.BackgroundDark)
                        .border(1.dp, GrayMatterColors.Neutral700, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = if (type == ResourceType.MARKDOWN) GrayMatterColors.Primary else GrayMatterColors.Neutral500, 
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), 
                        color = GrayMatterColors.TextPrimary, 
                        maxLines = 1
                    )
                    Text(
                        text = time, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = GrayMatterColors.Neutral600
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = GrayMatterColors.Neutral700, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AddNewEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1.8f),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GrayMatterColors.Primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Glassmorphism card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(GrayMatterColors.SurfaceDark)
                .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(28.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(GrayMatterColors.Neutral900, CircleShape)
                    .border(1.dp, GrayMatterColors.Neutral700, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Resource",
                    tint = GrayMatterColors.Primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (1000 * 60)
    val hours = mins / 60
    
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "more than a day ago"
    }
}

@Composable
private fun ContinueReadingCard(
    title: String,
    type: ResourceType,
    progress: com.example.graymatter.domain.ReadingProgress?,
    onClick: () -> Unit
) {
    val icon = when (type) {
        ResourceType.WEB_LINK -> Icons.Default.Language
        ResourceType.MARKDOWN -> Icons.Default.EditNote
        ResourceType.PDF -> Icons.Default.PictureAsPdf
        ResourceType.IMAGE -> Icons.Default.Image
        else -> Icons.Default.Description
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.5.dp, GrayMatterColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GrayMatterColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GrayMatterColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Icon(Icons.Default.OpenInNew, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(20.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterColors.TextPrimary,
                maxLines = 2
            )

            if (progress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${(progress.percentComplete * 100).toInt()}% Read",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterColors.Primary
                        )
                        Text(
                            "Page ${progress.currentPage + 1} of ${progress.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterColors.Neutral500
                        )
                    }
                    LinearProgressIndicator(
                        progress = progress.percentComplete.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = GrayMatterColors.Primary,
                        trackColor = GrayMatterColors.Neutral800
                    )
                }
            } else {
                Text(
                    text = "Pick up where you left off",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayMatterColors.Neutral500
                )
            }
        }
    }
}
