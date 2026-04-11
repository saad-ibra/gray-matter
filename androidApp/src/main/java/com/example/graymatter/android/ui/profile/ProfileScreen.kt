package com.example.graymatter.android.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.example.graymatter.android.ui.components.TopicPickerSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel
import com.example.graymatter.android.ui.viewmodel.TrashViewModel
import com.example.graymatter.domain.CustomTemplate

import com.example.graymatter.android.ui.components.TemplateEditorDialog

@Composable
fun ProfileScreen(
    viewModel: GrayMatterViewModel,
    trashViewModel: TrashViewModel,
    templateViewModel: com.example.graymatter.android.ui.viewmodel.TemplateViewModel,
    onNavigateToGraph: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTemplatesScreen by remember { mutableStateOf(false) }
    var showRecentlyDeleted by remember { mutableStateOf(false) }

    if (showTemplatesScreen) {
        BackHandler { showTemplatesScreen = false }
        TemplatesManagementScreen(
            templateViewModel = templateViewModel,
            onBackClick = { showTemplatesScreen = false }
        )
    } else if (showRecentlyDeleted) {
        BackHandler { showRecentlyDeleted = false }
        RecentlyDeletedScreen(
            viewModel = viewModel,
            trashViewModel = trashViewModel,
            onBackClick = { showRecentlyDeleted = false }
        )
    } else {
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
                        onClick = onNavigateToGraph
                    )
                    SettingsButton(
                        icon = Icons.Default.ListAlt,
                        title = "Template Management",
                        onClick = { showTemplatesScreen = true }
                    )
                    SettingsButton(
                        icon = Icons.Default.Restore,
                        title = "Recently Deleted",
                        onClick = { showRecentlyDeleted = true }
                    )
                }
            }
        }
    }
}

// ... existing code ...
@Composable
private fun SettingsButton(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
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
                Icon(icon, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(24.dp))
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

sealed class DeletedItemUiModel(val id: String, val title: String, val deletedAt: Long) {
    class TopicItem(id: String, title: String, deletedAt: Long) : DeletedItemUiModel(id, title, deletedAt)
    class ResourceItem(id: String, title: String, deletedAt: Long, val resourceType: String) : DeletedItemUiModel(id, title, deletedAt)
    class OpinionItem(id: String, title: String, deletedAt: Long, val text: String, val hasPage: Boolean) : DeletedItemUiModel(id, title, deletedAt)
}

@Composable
fun TemplatesManagementScreen(
    templateViewModel: com.example.graymatter.android.ui.viewmodel.TemplateViewModel,
    onBackClick: () -> Unit
) {
    val templates by templateViewModel.templates.collectAsState()
    var editingTemplate by remember { mutableStateOf<CustomTemplate?>(null) }
    var showEditor by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GrayMatterColors.TextPrimary)
                }
                Text(
                    text = "Template Management",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    SectionHeader(title = "YOUR TEMPLATES") {
                        IconButton(onClick = {
                            editingTemplate = CustomTemplate(java.util.UUID.randomUUID().toString(), "", listOf(""))
                            showEditor = true
                        }) {
                            Icon(Icons.Default.Add, null, tint = GrayMatterColors.TextPrimary)
                        }
                    }
                }

                items(templates) { template ->
                    TemplateItem(
                        template = template,
                        onClick = {
                            editingTemplate = template
                            showEditor = true
                        }
                    )
                }
            }
        }

        if (showEditor && editingTemplate != null) {
            TemplateEditorDialog(
                template = editingTemplate!!,
                onDismiss = { showEditor = false },
                onSave = { updated ->
                    templateViewModel.saveTemplate(updated)
                    showEditor = false
                },
                onDelete = { id ->
                    templateViewModel.deleteTemplate(id)
                    showEditor = false
                }
            )
        }
    }
}

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

    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showRestoreConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore Items", color = Color.White) },
            text = { Text("Are you sure you want to restore ${selectedIds.size} selected item(s)?", color = GrayMatterColors.TextSecondary) },
            containerColor = Color(0xFF1A1A1E),
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedIds
                    ids.forEach { id ->
                        val item = combinedList.find { it.id == id }
                        when (item) {
                            is DeletedItemUiModel.TopicItem -> trashViewModel.undoDeleteTopic(id)
                            is DeletedItemUiModel.ResourceItem -> trashViewModel.undoDeleteResourceEntry(id)
                            is DeletedItemUiModel.OpinionItem -> {
                                // Check if it's a bookmark or opinion by looking at the deletedLists
                                if (deletedBookmarks.any { it.id == id }) trashViewModel.undoDeleteBookmark(id)
                                else trashViewModel.undoDeleteOpinion(id)
                            }
                            else -> {}
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
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Permanently Delete", color = Color.White) },
            text = { Text("Are you sure you want to permanently delete ${selectedIds.size} selected item(s)? This action cannot be undone.", color = GrayMatterColors.TextSecondary) },
            containerColor = Color(0xFF1A1A1E),
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedIds
                    ids.forEach { id ->
                        val item = combinedList.find { it.id == id }
                        when (item) {
                            is DeletedItemUiModel.TopicItem -> trashViewModel.permanentlyDeleteTopic(id)
                            is DeletedItemUiModel.ResourceItem -> trashViewModel.permanentlyDeleteResourceEntry(id)
                            is DeletedItemUiModel.OpinionItem -> {
                                if (deletedBookmarks.any { it.id == id }) trashViewModel.permanentlyDeleteBookmark(id)
                                else trashViewModel.permanentlyDeleteOpinion(id)
                            }
                            else -> {}
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
                viewModel.assignTopicToResourceEntry(restoreNeedsTopicId!!, topic.id)
                trashViewModel.clearRestoreNeedsTopic()
            },
            onCreateNewTopic = { name ->
                scope.launch {
                    val newId = viewModel.createTopic(name)
                    viewModel.assignTopicToResourceEntry(restoreNeedsTopicId!!, newId)
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GrayMatterColors.TextPrimary)
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
                            "Resource" -> Icons.Default.Article
                            "Dictionary" -> Icons.Default.MenuBook
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
                                .clickable {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                    } else {
                                        selectedIds = setOf(item.id)
                                    }
                                }
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

@Composable
private fun SectionHeader(title: String, action: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = GrayMatterColors.Neutral500
        )
        action()
    }
}

@Composable
private fun TemplateItem(template: CustomTemplate, onClick: () -> Unit) {
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
            Column {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary
                )
                Text(
                    text = "${template.headings.size} headings",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayMatterColors.TextSecondary
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = GrayMatterColors.Neutral700)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditorDialog(
    template: CustomTemplate,
    onDismiss: () -> Unit,
    onSave: (CustomTemplate) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(template.name) }
    var headings by remember { mutableStateOf(template.headings) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GrayMatterColors.SurfaceDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GrayMatterColors.Neutral700) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Template",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary
                )
                IconButton(onClick = { onDelete(template.id) }) {
                    Icon(Icons.Default.Delete, null, tint = GrayMatterColors.Error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Template Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = GrayMatterColors.SurfaceInput,
                    focusedContainerColor = GrayMatterColors.SurfaceInput,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(24.dp))


            Text(
                text = "HEADINGS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterColors.Neutral500
            )

            Spacer(modifier = Modifier.height(8.dp))

            headings.forEachIndexed { index, heading ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = heading,
                        onValueChange = { newVal ->
                            val newList = headings.toMutableList()
                            newList[index] = newVal
                            headings = newList
                        },
                        placeholder = { Text("Heading ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = GrayMatterColors.SurfaceInput,
                            focusedContainerColor = GrayMatterColors.SurfaceInput,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    IconButton(onClick = {
                        headings = headings.toMutableList().apply { removeAt(index) }
                    }) {
                        Icon(Icons.Default.RemoveCircleOutline, null, tint = GrayMatterColors.Error)
                    }
                }
            }

            TextButton(
                onClick = {
                    headings = headings + ""
                    scope.launch {
                        delay(100)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Heading")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSave(template.copy(name = name, headings = headings.filter { it.isNotBlank() })) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3E6A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Template", fontWeight = FontWeight.Bold)
            }
        }
    }
}
