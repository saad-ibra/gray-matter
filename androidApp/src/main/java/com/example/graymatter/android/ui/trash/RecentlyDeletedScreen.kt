package com.example.graymatter.android.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel
import com.example.graymatter.android.ui.viewmodel.TrashViewModel
import com.example.graymatter.android.ui.components.TopicPickerSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

sealed class DeletedItemUiModel(val id: String, val title: String, val deletedAt: Long) {
    class TopicItem(id: String, title: String, deletedAt: Long) : DeletedItemUiModel(id, title, deletedAt)
    class ResourceItem(id: String, title: String, deletedAt: Long, val resourceType: String) : DeletedItemUiModel(id, title, deletedAt)
    class OpinionItem(id: String, title: String, deletedAt: Long, val text: String, val hasPage: Boolean) : DeletedItemUiModel(id, title, deletedAt)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentlyDeletedScreen(
    viewModel: GrayMatterViewModel,
    trashViewModel: TrashViewModel,
    onBackClick: () -> Unit
) {
    val deletedTopics by trashViewModel.deletedTopics.collectAsState()
    val deletedResources by trashViewModel.deletedResourceEntries.collectAsState()
    val deletedOpinions by trashViewModel.deletedOpinions.collectAsState()
    val deletedBookmarks by trashViewModel.deletedBookmarks.collectAsState()
    val topics by viewModel.topicsStream.collectAsState()
    val restoreNeedsTopicId by trashViewModel.restoreNeedsTopicId.collectAsState()
    
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val combinedList = remember(deletedTopics, deletedResources, deletedOpinions, deletedBookmarks) {
        val list = mutableListOf<DeletedItemUiModel>()
        list.addAll(deletedTopics.map { 
            DeletedItemUiModel.TopicItem(it.id, it.name, it.deletedAt ?: 0L) 
        })
        list.addAll(deletedResources.map { 
            DeletedItemUiModel.ResourceItem(it.resourceEntry.id, it.resource.title ?: "Untitled Resource", it.resourceEntry.deletedAt ?: 0L, it.resource.type.name) 
        })
        list.addAll(deletedOpinions.map { 
            DeletedItemUiModel.OpinionItem(it.id, "Opinion", it.deletedAt ?: 0L, it.text, it.pageNumber != null) 
        })
        list.addAll(deletedBookmarks.map {
            DeletedItemUiModel.OpinionItem(it.id, it.title ?: "Bookmark", it.deletedAt ?: 0L, it.opinion ?: "", true)
        })
        list.sortedByDescending { it.deletedAt }
    }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    
    var previewItem by remember { mutableStateOf<DeletedItemUiModel?>(null) }

    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore Items", color = Color.White) },
            text = { Text("Are you sure you want to restore ${selectedIds.size} selected item(s)?", color = GrayMatterColors.TextSecondary) },
            containerColor = Color(0xFF1A1A1E),
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedIds
                    ids.forEach { id ->
                        val item = combinedList.find { it.id == id }
                        item?.let {
                            when (it) {
                                is DeletedItemUiModel.TopicItem -> trashViewModel.undoDeleteTopic(id)
                                is DeletedItemUiModel.ResourceItem -> trashViewModel.undoDeleteResourceEntry(id)
                                is DeletedItemUiModel.OpinionItem -> {
                                    if (deletedBookmarks.any { it.id == id }) trashViewModel.undoDeleteBookmark(id)
                                    else trashViewModel.undoDeleteOpinion(id)
                                }
                            }
                        }
                    }
                    selectedIds = emptySet()
                    showRestoreConfirm = false
                }) {
                    Text("Restore", color = GrayMatterColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Permanently Delete", color = Color.White) },
            text = { Text("Are you sure you want to permanently delete ${selectedIds.size} selected item(s)? This action cannot be undone.", color = GrayMatterColors.TextSecondary) },
            containerColor = Color(0xFF1A1A1E),
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedIds
                    ids.forEach { id ->
                        val item = combinedList.find { it.id == id }
                        item?.let {
                            when (it) {
                                is DeletedItemUiModel.TopicItem -> trashViewModel.permanentlyDeleteTopic(id)
                                is DeletedItemUiModel.ResourceItem -> trashViewModel.permanentlyDeleteResourceEntry(id)
                                is DeletedItemUiModel.OpinionItem -> {
                                    if (deletedBookmarks.any { it.id == id }) trashViewModel.permanentlyDeleteBookmark(id)
                                    else trashViewModel.permanentlyDeleteOpinion(id)
                                }
                            }
                        }
                    }
                    selectedIds = emptySet()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = GrayMatterColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
    
    if (restoreNeedsTopicId != null) {
        val entryWithDetails = remember(restoreNeedsTopicId, deletedResources) {
            deletedResources.find { it.resourceEntry.id == restoreNeedsTopicId }
        }

        TopicPickerSheet(
            title = entryWithDetails?.resource?.title ?: "Restore Item",
            topics = topics,
            onDismiss = {
                trashViewModel.cancelRestore(restoreNeedsTopicId!!)
            },
            onSelectTopic = { topic ->
                trashViewModel.assignTopicToResourceEntry(restoreNeedsTopicId!!, topic.id)
                trashViewModel.clearRestoreNeedsTopic()
            },
            onCreateNewTopic = { name ->
                scope.launch {
                    val newId = viewModel.createTopic(name)
                    trashViewModel.assignTopicToResourceEntry(restoreNeedsTopicId!!, newId)
                    trashViewModel.clearRestoreNeedsTopic()
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GrayMatterColors.TextPrimary)
                    }
                    Text(
                        text = if (isSelectionMode) "${selectedIds.size} Selected" else "Recently Deleted",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = GrayMatterColors.TextPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val allSelected = selectedIds.size == combinedList.size
                        TextButton(
                            onClick = {
                                selectedIds = if (allSelected) emptySet() else combinedList.map { it.id }.toSet()
                            }
                        ) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                color = GrayMatterColors.Primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        IconButton(onClick = { showRestoreConfirm = true }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = GrayMatterColors.Primary)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Permanently Delete", tint = GrayMatterColors.Error)
                        }
                    }
                }
            }

            if (combinedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DeleteOutline, null, tint = GrayMatterColors.Neutral600, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No recently deleted items", color = GrayMatterColors.Neutral600)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(combinedList) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        
                        val typeName = when (item) {
                            is DeletedItemUiModel.TopicItem -> "Topic"
                            is DeletedItemUiModel.ResourceItem -> "Resource"
                            is DeletedItemUiModel.OpinionItem -> {
                                val text = item.text
                                when {
                                    text.startsWith("[DICT") -> "Dictionary"
                                    text.startsWith("> ") -> "Annotation"
                                    text.startsWith("[TEMPLATE:") || text.startsWith("[CUSTOM:") -> "Template/Custom"
                                    item.hasPage -> "Bookmark"
                                    else -> "Opinion"
                                }
                            }
                        }

                        val icon = when (typeName) {
                            "Topic" -> Icons.Default.Folder
                            "Resource" -> Icons.AutoMirrored.Filled.Article
                            "Dictionary" -> Icons.AutoMirrored.Filled.MenuBook
                            "Annotation" -> Icons.Default.FormatQuote
                            "Template/Custom" -> Icons.Default.DashboardCustomize
                            "Bookmark" -> Icons.Default.Bookmark
                            else -> Icons.Default.QuestionAnswer
                        }
                        
                        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        val thirtyDays = 30L * 24 * 60 * 60 * 1000
                        val elapsed = now - item.deletedAt
                        val remaining = thirtyDays - elapsed
                        val daysRemaining = maxOf(0, (remaining / (24 * 60 * 60 * 1000)).toInt())
                        
                        val snippet = if (item is DeletedItemUiModel.OpinionItem) {
                            item.text.replace(Regex("\\[(TEMPLATE|CUSTOM|DICT).*?\\]|>"), "").trim().take(50)
                        } else ""

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GrayMatterColors.Primary.copy(alpha=0.1f) else GrayMatterColors.SurfaceDark)
                                .border(1.dp, if (isSelected) GrayMatterColors.Primary else GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                        } else {
                                            previewItem = item
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            selectedIds = setOf(item.id)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { 
                                            selectedIds = if (it) selectedIds + item.id else selectedIds - item.id 
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = GrayMatterColors.Primary,
                                            uncheckedColor = GrayMatterColors.Neutral500,
                                            checkmarkColor = Color.Black
                                        ),
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                                
                                Icon(icon, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = typeName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GrayMatterColors.Primary,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = "• $daysRemaining days left",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (daysRemaining <= 3) GrayMatterColors.Error else GrayMatterColors.Neutral500
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = GrayMatterColors.TextPrimary,
                                        maxLines = 1
                                    )
                                    if (snippet.isNotBlank()) {
                                        Text(
                                            text = snippet,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GrayMatterColors.TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (previewItem != null) {
            DeletedItemPreviewDialog(
                item = previewItem!!,
                onDismiss = { previewItem = null },
                onRestore = {
                    val currentPreviewItem = previewItem
                    currentPreviewItem?.let { item ->
                        val id = item.id
                        when (item) {
                            is DeletedItemUiModel.TopicItem -> trashViewModel.undoDeleteTopic(id)
                            is DeletedItemUiModel.ResourceItem -> trashViewModel.undoDeleteResourceEntry(id)
                            is DeletedItemUiModel.OpinionItem -> {
                                if (deletedBookmarks.any { it.id == id }) trashViewModel.undoDeleteBookmark(id)
                                else trashViewModel.undoDeleteOpinion(id)
                            }
                        }
                    }
                    previewItem = null
                }
            )
        }
    }
}

@Composable
private fun DeletedItemPreviewDialog(
    item: DeletedItemUiModel,
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = when(item) {
                    is DeletedItemUiModel.TopicItem -> "Topic Details"
                    is DeletedItemUiModel.ResourceItem -> "Resource Details"
                    else -> "Opinion/Bookmark Details"
                },
                color = Color.White 
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = GrayMatterColors.TextPrimary
                )
                if (item is DeletedItemUiModel.OpinionItem) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayMatterColors.TextSecondary
                    )
                } else if (item is DeletedItemUiModel.ResourceItem) {
                    Text(
                        text = "Type: ${item.resourceType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayMatterColors.TextSecondary
                    )
                }
                
                val dateStr = remember(item.deletedAt) {
                    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(item.deletedAt)
                    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
                    val dt = instant.toLocalDateTime(tz)
                    "${dt.dayOfMonth}/${dt.monthNumber}/${dt.year}"
                }
                Text(
                    text = "Deleted on $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayMatterColors.Neutral500
                )
            }
        },
        containerColor = Color(0xFF1A1A1E),
        confirmButton = {
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Primary)
            ) {
                Text("Restore", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}
