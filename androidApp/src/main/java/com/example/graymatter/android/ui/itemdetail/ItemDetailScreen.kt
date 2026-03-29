package com.example.graymatter.android.ui.itemdetail

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.ItemWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.CustomTemplate
import java.text.SimpleDateFormat
import java.util.*

/**
 * Item Details Screen.
 * Features a beautiful animated timeline where opinions and bookmark reflections are unified.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemDetailScreen(
    itemDetails: ItemWithDetails?,
    readingProgress: com.example.graymatter.domain.ReadingProgress?,
    focusOpinionId: String? = null,
    templates: List<CustomTemplate> = emptyList(),
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    onBackClick: () -> Unit,
    onOpenResource: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onAddOpinion: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit,
    onUpdateOpinion: (String, String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit,
    onDeleteOpinion: (String) -> Unit,
    onRenameResource: (String) -> Unit,
    onDeleteItem: () -> Unit,
    onEditNote: () -> Unit,
    onExport: (List<Opinion>) -> Unit,
    onLoadLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    onLoadResourceLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    onLoadBacklinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.android.ui.components.BacklinkUiModel>>,
    onViewInGraphClick: (String) -> Unit,
    onSaveTemplate: (CustomTemplate) -> Unit = {},
    generateUuid: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTemplateEditor by remember { mutableStateOf(false) }
    var showEditDialogId by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var description by remember(itemDetails?.item?.description) { mutableStateOf(itemDetails?.item?.description ?: "") }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark),
        containerColor = GrayMatterColors.BackgroundDark,
        floatingActionButton = {
            if (!isEditing) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GrayMatterColors.TextPrimary,
                    contentColor = GrayMatterColors.BackgroundDark,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Knowledge",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            if (itemDetails == null) {
                ItemDetailHeader(
                    onBackClick = onBackClick,
                    isEditing = false,
                    onToggleEdit = {},
                    onDeleteClick = {}
                )
            }

            if (itemDetails != null) {
                var selectedFilters by remember { mutableStateOf(setOf("Dictionary", "Annotation", "Custom", "Bookmark", "Opinion")) }
                var showFilterMenu by remember { mutableStateOf(false) }
                
                // Unified sorted list of Opinions
                val sortedOpinions = remember(itemDetails.opinions, selectedFilters) {
                    itemDetails.opinions.sortedByDescending { it.createdAt }
                        .filter { opinion ->
                            val isAnnotation = opinion.text.startsWith("> ")
                            val isDictionary = opinion.text.startsWith("[DICT] ")
                            val isTemplate = opinion.text.startsWith("[TEMPLATE:")
                            val isCustomTitle = opinion.text.startsWith("[CUSTOM: ")
                            val hasPageNumber = opinion.pageNumber != null
                            
                            val type = when {
                                isDictionary -> "Dictionary"
                                isAnnotation -> "Annotation"
                                isTemplate || isCustomTitle -> "Custom"
                                hasPageNumber -> "Bookmark"
                                else -> "Opinion"
                            }
                            
                            selectedFilters.contains(type)
                        }
                }

                // Header with unified dropdown menu
                ItemDetailHeader(
                    onBackClick = onBackClick,
                    isEditing = isEditing,
                    onToggleEdit = {
                        if (isEditing) {
                            onUpdateDescription(description)
                        }
                        isEditing = !isEditing
                    },
                    onDeleteClick = { showDeleteConfirm = true },
                    onFilterClick = { showFilterMenu = true },
                    onExportClick = { onExport(sortedOpinions) },
                    onViewInRelatrixClick = { onViewInGraphClick(itemDetails.resource.id) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Resource Card
                    ResourceCard(
                        title = itemDetails.resource.title ?: "Untitled",
                        url = itemDetails.resource.url ?: itemDetails.resource.filePath ?: "",
                        savedTimeAgo = "Saved ${formatTimeAgo(itemDetails.item.firstOpinionAt)}",
                        readingProgress = readingProgress,
                        onOpenClick = onOpenResource,
                        onRenameClick = { if (isEditing) showRenameDialog = true },
                        showEditButton = isEditing && itemDetails.resource.type == com.example.graymatter.domain.ResourceType.MARKDOWN,
                        onEditClick = onEditNote
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Optional Description Section
                    SectionHeader(text = "DESCRIPTION")
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isEditing) {
                        DescriptionEditor(
                            value = description,
                            onValueChange = { description = it }
                        )
                    } else if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = GrayMatterColors.TextSecondary
                        )
                    } else {
                        Text(
                            text = "No description provided.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = GrayMatterColors.Neutral700
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // RESOURCE LINKS section — for notes (MARKDOWN) only
                    if (itemDetails.resource.type == com.example.graymatter.domain.ResourceType.MARKDOWN) {
                        val resourceLinks by onLoadResourceLinks(itemDetails.resource.id).collectAsState(initial = emptyList())
                        if (resourceLinks.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GrayMatterColors.KnowledgeBlue.copy(alpha = 0.06f))
                                    .border(1.dp, GrayMatterColors.KnowledgeBlue.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        null,
                                        tint = GrayMatterColors.KnowledgeBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "RESOURCE LINKS",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            letterSpacing = 1.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = GrayMatterColors.TextSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    resourceLinks.forEach { link ->
                                        val linkText = when (link) {
                                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> link.name
                                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> link.title
                                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> link.snippet.take(30)
                                        }
                                        val linkIcon = when (link) {
                                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> Icons.Default.Folder
                                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> Icons.Default.Article
                                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> Icons.Default.FormatQuote
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = GrayMatterColors.KnowledgeBlue.copy(alpha = 0.10f),
                                            modifier = Modifier.border(
                                                0.5.dp,
                                                GrayMatterColors.KnowledgeBlue.copy(alpha = 0.25f),
                                                RoundedCornerShape(10.dp)
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    linkIcon,
                                                    null,
                                                    tint = GrayMatterColors.KnowledgeBlue.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    linkText,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                    color = GrayMatterColors.KnowledgeBlue,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    Text(
                        text = "Knowledge History",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = GrayMatterColors.TextPrimary
                    )

                    // Filter bottom sheet / dropdown
                    if (showFilterMenu) {
                        val availableFilters = listOf(
                            "Dictionary" to Icons.Default.MenuBook,
                            "Annotation" to Icons.Default.FormatQuote,
                            "Custom" to Icons.Default.DashboardCustomize,
                            "Bookmark" to Icons.Default.Bookmark,
                            "Opinion" to Icons.Default.QuestionAnswer
                        )
                        val filterNames = availableFilters.map { it.first }.toSet()
                        
                        AlertDialog(
                            onDismissRequest = { showFilterMenu = false },
                            containerColor = GrayMatterColors.SurfaceDark,
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Filter Knowledge", color = Color.White, fontWeight = FontWeight.Bold)
                                    Row {
                                        TextButton(
                                            onClick = { selectedFilters = filterNames },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("All", color = GrayMatterColors.Primary, fontSize = 13.sp)
                                        }
                                        TextButton(
                                            onClick = { selectedFilters = emptySet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("None", color = GrayMatterColors.Primary, fontSize = 13.sp)
                                        }
                                    }
                                }
                            },
                            text = {
                                Column {
                                    availableFilters.forEach { (filter, icon) ->
                                        val isSelected = selectedFilters.contains(filter)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedFilters = if (isSelected) {
                                                        selectedFilters - filter
                                                    } else {
                                                        selectedFilters + filter
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    selectedFilters = if (isSelected) {
                                                        selectedFilters - filter
                                                    } else {
                                                        selectedFilters + filter
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = GrayMatterColors.Primary,
                                                    uncheckedColor = GrayMatterColors.Neutral500,
                                                    checkmarkColor = Color.Black
                                                )
                                            )
                                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (isSelected) GrayMatterColors.Primary else GrayMatterColors.Neutral500)
                                            Text(filter, color = Color.White)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showFilterMenu = false }) {
                                    Text("Done", color = GrayMatterColors.Primary)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Unified Timeline
                    OpinionTimeline(
                        opinions = sortedOpinions,
                        resourceId = itemDetails.resource.id,
                        isEditing = isEditing,
                        focusOpinionId = focusOpinionId,
                        templates = templates,
                        referenceSelectorViewModel = referenceSelectorViewModel,
                        onUpdateOpinion = onUpdateOpinion,
                        onDeleteOpinion = onDeleteOpinion,
                        onJumpToPage = { resourceId, page ->
                            onOpenBookmark(Bookmark(id="", resourceId=resourceId, page=page, createdAt=0L))
                        },
                        onLoadLinks = onLoadLinks,
                        onViewInGraph = onViewInGraphClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val backlinks by onLoadBacklinks(itemDetails.resource.id).collectAsState(initial = emptyList())
                    if (backlinks.isNotEmpty() || isEditing) {
                        com.example.graymatter.android.ui.components.BacklinkPanel(
                            backlinks = backlinks,
                            onBacklinkClick = { backlink ->
                                // Implementation to navigate to the source based on backlink.sourceType and ID
                            },
                            onViewInGraphClick = { onViewInGraphClick(itemDetails.resource.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        if (showAddDialog) {
            OpinionEditDialog(
                viewModel = referenceSelectorViewModel,
                templates = templates,
                onDismiss = { showAddDialog = false },
                onCreateTemplate = { showTemplateEditor = true },
                onConfirm = { text, confidence, referenceLinks ->
                    onAddOpinion(text, confidence, referenceLinks)
                    showAddDialog = false
                }
            )
        }

        if (showRenameDialog && itemDetails != null) {
            RenameDialog(
                currentName = itemDetails.resource.title ?: "",
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    onRenameResource(newName)
                    showRenameDialog = false
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Item", color = Color.White) },
                text = { Text("Are you sure you want to delete this resource and all its opinions? This action cannot be undone.", color = GrayMatterColors.TextSecondary) },
                containerColor = Color(0xFF1A1A1E),
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDeleteItem()
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

        if (showTemplateEditor) {
            com.example.graymatter.android.ui.components.TemplateEditorDialog(
                template = com.example.graymatter.domain.CustomTemplate(generateUuid(), "", listOf("")),
                onDismiss = { showTemplateEditor = false },
                onSave = { updated ->
                    onSaveTemplate(updated)
                    showTemplateEditor = false
                }
            )
        }
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1E),
        title = { Text("Rename Resource", color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("Rename", color = GrayMatterColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        color = GrayMatterColors.Neutral500
    )
}

@Composable
private fun DescriptionEditor(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayMatterColors.SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary),
            modifier = Modifier.fillMaxWidth(),
            cursorBrush = SolidColor(GrayMatterColors.Primary),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Add a description...", color = GrayMatterColors.Neutral700)
                inner()
            }
        )
    }
}

@Composable
private fun ItemDetailHeader(
    onBackClick: () -> Unit,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    onFilterClick: (() -> Unit)? = null,
    onExportClick: (() -> Unit)? = null,
    onViewInRelatrixClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.KeyboardArrowLeft, "Back", tint = GrayMatterColors.TextPrimary, modifier = Modifier.size(32.dp))
        }
        Text("Item Details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.TextPrimary)
        Row {
            if (isEditing) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = GrayMatterColors.Error,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = onToggleEdit) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done Editing",
                        tint = GrayMatterColors.Success,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = GrayMatterColors.TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(GrayMatterColors.SurfaceDark)
                    ) {
                        if (onFilterClick != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.FilterList, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(20.dp))
                                        Text("Filter History", color = Color.White)
                                    }
                                },
                                onClick = { menuExpanded = false; onFilterClick() }
                            )
                        }
                        if (onExportClick != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Share, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(20.dp))
                                        Text("Export History", color = Color.White)
                                    }
                                },
                                onClick = { menuExpanded = false; onExportClick() }
                            )
                        }
                        if (onViewInRelatrixClick != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Hub, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Text("View in Relatrix", color = Color.White)
                                    }
                                },
                                onClick = { menuExpanded = false; onViewInRelatrixClick() }
                            )
                        }
                        Divider(color = GrayMatterColors.Neutral800, modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Edit, null, tint = GrayMatterColors.TextPrimary, modifier = Modifier.size(20.dp))
                                    Text("Edit Item", color = Color.White)
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleEdit()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceCard(
    title: String,
    url: String,
    savedTimeAgo: String,
    readingProgress: com.example.graymatter.domain.ReadingProgress?,
    onOpenClick: () -> Unit,
    onRenameClick: () -> Unit,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.2.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenClick)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary,
                    modifier = Modifier.weight(1f).clickable(onClick = onRenameClick)
                )
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open",
                    tint = GrayMatterColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(url, style = MaterialTheme.typography.bodySmall, color = GrayMatterColors.Neutral500, maxLines = 2)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Schedule, null, tint = GrayMatterColors.Neutral600, modifier = Modifier.size(16.dp))
                Text(savedTimeAgo, style = MaterialTheme.typography.bodySmall, color = GrayMatterColors.Neutral600)
            }

            // Reading Progress Indicator
            if (readingProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${(readingProgress.percentComplete * 100).toInt()}% Read",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterColors.Primary
                        )
                        Text(
                            "Page ${readingProgress.currentPage + 1} of ${readingProgress.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterColors.Neutral500
                        )
                    }
                    LinearProgressIndicator(
                        progress = readingProgress.percentComplete.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = GrayMatterColors.Primary,
                        trackColor = GrayMatterColors.Neutral800
                    )
                }
            } else if (showEditButton) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayMatterColors.Primary,
                        contentColor = GrayMatterColors.OnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Note")
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayMatterColors.Neutral900,
                        contentColor = GrayMatterColors.Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Read / View")
                }
            }
        }
    }
}

@Composable
private fun OpinionTimeline(
    opinions: List<Opinion>,
    resourceId: String,
    isEditing: Boolean,
    focusOpinionId: String? = null,
    templates: List<CustomTemplate>,
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel?,
    onUpdateOpinion: (String, String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit,
    onDeleteOpinion: (String) -> Unit,
    onJumpToPage: (String, Int) -> Unit,
    onLoadLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    onViewInGraph: (String) -> Unit
) {
    Column {
        opinions.forEachIndexed { index, opinion ->
            OpinionTimelineItem(
                opinion = opinion,
                serialNumber = opinions.size - index,
                isFirst = index == 0,
                isLast = index == opinions.lastIndex,
                isEditing = isEditing,
                isFocused = opinion.id == focusOpinionId,
                templates = templates,
                referenceSelectorViewModel = referenceSelectorViewModel,
                onUpdate = { text, confidence, date, links -> onUpdateOpinion(opinion.id, text, confidence, date, links) },
                onDelete = { onDeleteOpinion(opinion.id) },
                onJump = {
                    opinion.pageNumber?.let { page ->
                        onJumpToPage(resourceId, page)
                    }
                },
                onLoadLinks = onLoadLinks,
                onViewInGraph = onViewInGraph
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun OpinionTimelineItem(
    opinion: Opinion,
    serialNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isEditing: Boolean,
    isFocused: Boolean = false,
    templates: List<CustomTemplate>,
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel?,
    onUpdate: (String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit,
    onDelete: () -> Unit,
    onJump: () -> Unit,
    onLoadLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    onViewInGraph: (String) -> Unit
) {
    var text by remember(opinion.text) { mutableStateOf(opinion.text) }
    var confidence by remember(opinion.confidenceScore) { mutableStateOf(opinion.confidenceScore.toFloat() / 100f) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    
    // Load initial reference links to pre-populate selection
    val flowLinks by onLoadLinks(opinion.id).collectAsState(initial = emptyList())
    var selectedReferences by remember(flowLinks) { mutableStateOf(flowLinks) }
    var showReferenceSelector by remember { mutableStateOf(false) }
    
    val hasPageNumber = opinion.pageNumber != null

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isFirst) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "scale"
    )

    val targetBackgroundColor = if (isFocused) GrayMatterColors.Primary.copy(alpha = 0.2f) else Color.Transparent
    val backgroundColor by animateColorAsState(targetBackgroundColor, tween(1000))

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(500)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(backgroundColor)
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        // Physical thin line indicator
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            val dotSize = if (isFirst) 12.dp else 8.dp
            val dotTopPadding = 12.dp
            val lineColor = GrayMatterColors.Neutral800
            
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(dotTopPadding + dotSize / 2)
                        .background(lineColor)
                )
            }
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = dotTopPadding + dotSize / 2)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(lineColor)
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = dotTopPadding)
                    .size(dotSize)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(if (isFirst) GrayMatterColors.Primary else GrayMatterColors.Neutral600)
                    .border(2.dp, GrayMatterColors.BackgroundDark, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 16.dp else 48.dp)
                .then(if (hasPageNumber && !isEditing) Modifier.clickable { onJump() } else Modifier)
        ) {
            // Header: Serial Number, Title, Date & Confidence Badge
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Serial Number
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GrayMatterColors.SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = serialNumber.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = GrayMatterColors.Neutral500
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        val isAnnotation = opinion.text.startsWith("> ")
                        val isDictionary = opinion.text.startsWith("[DICT] ")
                        val isTemplate = opinion.text.startsWith("[TEMPLATE:")
                        val isCustomTitle = opinion.text.startsWith("[CUSTOM: ")
                        val hasPageNumber = opinion.pageNumber != null
                        
                        val dynamicTitle = when {
                            isTemplate -> opinion.text.substringAfter("[TEMPLATE:").substringBefore("]")
                            isCustomTitle -> opinion.text.substringAfter("[CUSTOM: ").substringBefore("]")
                            else -> "CUSTOM ENTRY"
                        }
                        
                        val (title, icon, color) = when {
                            isDictionary -> Triple("DICTIONARY", Icons.Default.Book, Color(0xFFC6280B))
                            isAnnotation -> Triple("ANNOTATION", Icons.Default.FormatQuote, GrayMatterColors.Gamboge)
                            isTemplate -> Triple("TEMPLATE", Icons.Default.DashboardCustomize, GrayMatterColors.CustomizedAccent)
                            isCustomTitle -> Triple(dynamicTitle.uppercase(), Icons.Default.EditNote, GrayMatterColors.Success)
                            hasPageNumber -> Triple("BOOKMARK", Icons.Default.Bookmark, GrayMatterColors.Jonquil)
                            else -> Triple("OPINION", Icons.Default.QuestionAnswer, GrayMatterColors.Success)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = color
                            )
                        }

                        if (isEditing) {
                            DateChip(timestamp = opinion.createdAt, onClick = { showDateTimePicker = true })
                        } else {
                            Text(
                                text = formatFullDate(opinion.createdAt).uppercase(), 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), 
                                color = GrayMatterColors.Neutral600,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfidenceBadge(score = opinion.confidenceScore)
                    if (isEditing) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = GrayMatterColors.Error, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        IconButton(onClick = { onViewInGraph(opinion.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Hub, "View in Relatrix", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            
            val isAnnotation = text.startsWith("> ")
            val isDictionary = text.startsWith("[DICT] ")
            val isTemplate = text.startsWith("[TEMPLATE:")
            val isCustomTitle = text.startsWith("[CUSTOM: ")
            val hasPageNumber = opinion.pageNumber != null

            if (hasPageNumber && !isEditing && !isTemplate && !isCustomTitle) {
                Spacer(modifier = Modifier.height(4.dp))
                val tagColor = when {
                    isDictionary -> Color(0xFFC6280B)
                    isAnnotation -> GrayMatterColors.Gamboge
                    else -> GrayMatterColors.Jonquil
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tagColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(if (isDictionary) Icons.Default.Book else if (isAnnotation) Icons.Default.FormatQuote else Icons.Default.Bookmark, null, tint = tagColor, modifier = Modifier.size(10.dp))
                        Text("PAGE ${opinion.pageNumber!! + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tagColor, maxLines = 1, softWrap = false)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Opinion Content
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Knowledge Connections in Edit Mode
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("KNOWLEDGE LINKS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = GrayMatterColors.Neutral500)
                        TextButton(
                            onClick = {
                                referenceSelectorViewModel?.clearSelection()
                                showReferenceSelector = true
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.AddLink, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                    if (selectedReferences.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedReferences.forEach { ref ->
                                val refText = when (ref) {
                                    is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                                    is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                                    is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                                }
                                InputChip(
                                    selected = true,
                                    onClick = { 
                                        selectedReferences = selectedReferences.filter { it.id != ref.id }
                                        onUpdate(text, (confidence * 100).toInt(), opinion.createdAt, selectedReferences)
                                    },
                                    label = { Text(refText, maxLines = 1) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }

                    if (isTemplate) {
                        // Try to parse and show dynamic form for custom entry editing
                        val templateName = text.substringAfter("[TEMPLATE:").substringBefore("]")
                        val template = templates.find { it.name == templateName }
                        
                        if (template != null) {
                            val fieldValues = remember(text) { parseTemplateContent(text, template.headings) }
                            DynamicEntryEditor(
                                template = template,
                                fieldValues = fieldValues,
                                onFieldChange = { heading, newVal ->
                                    val newValues = fieldValues.toMutableMap().apply { put(heading, newVal) }
                                    val newText = formatTemplateContent(template, newValues)
                                    text = newText
                                    onUpdate(newText, (confidence * 100).toInt(), opinion.createdAt, selectedReferences)
                                },
                                confidence = confidence,
                                onConfidenceChange = {
                                    confidence = it
                                    onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences)
                                }
                            )
                        } else {
                            // Fallback to plain text editor if template not found
                            OpinionEditor(
                                text = text,
                                confidence = confidence,
                                onTextChange = { 
                                    text = it 
                                    onUpdate(it, (confidence * 100).toInt(), opinion.createdAt, selectedReferences)
                                },
                                onConfidenceChange = { 
                                    confidence = it 
                                    onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences)
                                }
                            )
                        }
                    } else {
                        OpinionEditor(
                            text = if (isCustomTitle) text.substringAfter("]\n").trim() else text,
                            confidence = confidence,
                            onTextChange = { 
                                val newFullText = if (isCustomTitle) {
                                    val prefix = text.substringBefore("]\n") + "]\n"
                                    prefix + it
                                } else {
                                    it
                                }
                                text = newFullText 
                                onUpdate(newFullText, (confidence * 100).toInt(), opinion.createdAt, selectedReferences)
                            },
                            onConfidenceChange = { 
                                confidence = it 
                                onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences)
                            }
                        )
                    }
                }
            } else {
                if (isDictionary) {
                    val phrase = text.removePrefix("[DICT] ").trim()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFC6280B).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFC6280B).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = phrase,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = GrayMatterColors.TextPrimary
                        )
                    }
                } else if (isAnnotation) {
                    // Split into quote and reflection
                    val parts = text.split("\n\n", limit = 2)
                    val quote = parts[0].removePrefix("> ").trim()
                    val reflection = if (parts.size > 1) parts[1].trim() else ""
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.Gamboge.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.Gamboge.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quote block (Gray)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.width(4.dp).height(IntrinsicSize.Min).background(GrayMatterColors.Gamboge)) // Orange accent
                                Text(
                                    text = "\"$quote\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 24.sp),
                                    color = GrayMatterColors.Neutral400,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        // Reflection
                        if (reflection.isNotEmpty()) {
                            Text(
                                text = reflection,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                color = GrayMatterColors.TextPrimary
                            )
                        }
                    }
                } else if (isTemplate) {
                    val templateName = text.substringAfter("[TEMPLATE:").substringBefore("]")
                    val content = text.substringAfter("]\n").trim()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.CustomizedAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.CustomizedAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = templateName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = GrayMatterColors.CustomizedAccent
                        )
                        
                        // Parse markdown-ish structure
                        val headings = content.split("### ").filter { it.isNotBlank() }
                        headings.forEach { headingBlock ->
                            val lines = headingBlock.split("\n", limit = 2)
                            val heading = lines[0].trim()
                            val response = lines.getOrNull(1)?.trim() ?: ""
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = heading,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GrayMatterColors.Neutral500
                                )
                                Text(
                                    text = response,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                    color = GrayMatterColors.TextPrimary
                                )
                            }
                        }
                    }
                } else if (isCustomTitle) {
                    val displayContent = text.substringAfter("]\n").trim()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.Success.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.Success.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = displayContent,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = GrayMatterColors.TextPrimary
                        )
                    }
                } else if (hasPageNumber) {
                    // Bookmark or old style page opinion
                    val cleanText = if (text.startsWith("[Page ")) text.replace(Regex("^\\[Page \\d+\\]\\s*"), "") else text
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.Jonquil.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.Jonquil.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = cleanText,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = GrayMatterColors.TextPrimary
                        )
                    }
                } else {
                    // General Opinion (Success)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.Success.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.Success.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = GrayMatterColors.TextPrimary
                        )
                    }
                }
                
                // Reference Links Chips are rendered below via onLoadLinks flow


                // Reference Links Chips
                val links by onLoadLinks(opinion.id).collectAsState(initial = emptyList())
                if (links.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        links.forEach { link ->
                            val linkText = when (link) {
                                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> link.name
                                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> link.title
                                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> {
                                    val t = link.snippet
                                    if (t.length > 10) t.take(10) + "..." else t
                                }
                            }
                            val icon = when (link) {
                                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> Icons.Default.Tag
                                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> Icons.Default.Article
                                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> Icons.Default.QuestionAnswer
                            }
                            AssistChip(
                                onClick = { /* Future: navigate to link */ },
                                label = { Text(linkText, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = GrayMatterColors.SurfaceDark,
                                    labelColor = GrayMatterColors.Primary,
                                    leadingIconContentColor = GrayMatterColors.Primary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GrayMatterColors.Neutral700)
                            )
                        }
                    }
                }
                
                if (hasPageNumber && !isEditing && !isTemplate && !isCustomTitle) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Launch, null, tint = GrayMatterColors.Primary.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tap to jump back to page", fontSize = 11.sp, color = GrayMatterColors.Primary.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

    if (showDateTimePicker) {
        DateTimePicker(
            initialTimestamp = opinion.createdAt,
            onDismiss = { showDateTimePicker = false },
            onConfirm = { onUpdate(text, (confidence * 100).toInt(), it, selectedReferences) }
        )
    }

    if (showReferenceSelector && referenceSelectorViewModel != null) {
        com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
            viewModel = referenceSelectorViewModel,
            onDismissRequest = { showReferenceSelector = false },
            onConfirm = { items ->
                showReferenceSelector = false
                selectedReferences = (selectedReferences + items).distinctBy { it.id }
                onUpdate(text, (confidence * 100).toInt(), opinion.createdAt, selectedReferences)
            }
        )
    }
}

@Composable
private fun DynamicEntryEditor(
    template: CustomTemplate,
    fieldValues: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
    confidence: Float,
    onConfidenceChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        template.headings.forEach { heading ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = heading.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = GrayMatterColors.Neutral500
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp))
                        .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = fieldValues[heading] ?: "",
                        onValueChange = { onFieldChange(heading, it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(GrayMatterColors.CustomizedAccent)
                    )
                }
            }
        }
        
        val legacyHeadings = fieldValues.keys - template.headings.toSet()
        legacyHeadings.forEach { heading ->
            val value = fieldValues[heading] ?: ""
            if (value.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "$heading (Legacy)".uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = GrayMatterColors.Neutral500
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = { onFieldChange(heading, it) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(GrayMatterColors.CustomizedAccent)
                        )
                    }
                }
            }
        }
        
        Slider(value = confidence, onValueChange = onConfidenceChange, colors = SliderDefaults.colors(thumbColor = GrayMatterColors.CustomizedAccent, activeTrackColor = GrayMatterColors.CustomizedAccent))
    }
}

private fun parseTemplateContent(content: String, headings: List<String>): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val body = content.substringAfter("]\n").trim()
    val parts = body.split("### ").filter { it.isNotBlank() }
    parts.forEach { part ->
        val lines = part.split("\n", limit = 2)
        val heading = lines[0].trim()
        val response = lines.getOrNull(1)?.trim() ?: ""
        result[heading] = response
    }
    // Ensure all headings are present
    headings.forEach { if (!result.containsKey(it)) result[it] = "" }
    return result
}

private fun formatTemplateContent(template: CustomTemplate, values: Map<String, String>): String {
    val sb = StringBuilder()
    sb.appendLine("[TEMPLATE:${template.name}]")
    
    // Write current headings
    template.headings.forEach { heading ->
        val value = values[heading] ?: ""
        if (value.isNotBlank()) {
            sb.appendLine("### $heading")
            sb.appendLine(value)
            sb.appendLine()
        }
    }
    
    // Write legacy headings to preserve them
    val legacyHeadings = values.keys - template.headings.toSet()
    legacyHeadings.forEach { heading ->
        val value = values[heading] ?: ""
        if (value.isNotBlank()) {
            sb.appendLine("### $heading")
            sb.appendLine(value)
            sb.appendLine()
        }
    }
    
    return sb.toString().trim()
}

@Composable
private fun DateChip(timestamp: Long, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GrayMatterColors.Neutral900)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatFullDate(timestamp).uppercase(), style = MaterialTheme.typography.labelSmall.copy(color = GrayMatterColors.Primary, fontWeight = FontWeight.Bold))
            Icon(Icons.Default.CalendarToday, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ConfidenceBadge(score: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = if (score > 50) Icons.Default.TrendingUp else Icons.Default.ArrowForward,
                contentDescription = null,
                tint = GrayMatterColors.Primary,
                modifier = Modifier.size(12.dp)
            )
            Text("${score/10}/10", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.TextPrimary, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun OpinionEditor(text: String, confidence: Float, onTextChange: (String) -> Unit, onConfidenceChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp)).border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp)).padding(12.dp)) {
            BasicTextField(value = text, onValueChange = onTextChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary), modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(GrayMatterColors.Primary))
        }
        Slider(value = confidence, onValueChange = onConfidenceChange, colors = SliderDefaults.colors(thumbColor = GrayMatterColors.Primary, activeTrackColor = GrayMatterColors.Primary))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePicker(initialTimestamp: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var isTimeStep by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTimestamp)
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTimestamp }
    val timePickerState = rememberTimePickerState(initialHour = calendar.get(Calendar.HOUR_OF_DAY), initialMinute = calendar.get(Calendar.MINUTE))

    if (!isTimeStep) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = { isTimeStep = true }) { Text("Next", color = GrayMatterColors.Primary) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GrayMatterColors.Neutral500) } },
            colors = DatePickerDefaults.colors(containerColor = GrayMatterColors.SurfaceDark)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(selectedDayContainerColor = GrayMatterColors.Primary, selectedDayContentColor = GrayMatterColors.OnPrimary, todayContentColor = GrayMatterColors.Primary, todayDateBorderColor = GrayMatterColors.Primary))
        }
    } else {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
            Box(modifier = Modifier.clip(RoundedCornerShape(28.dp)).background(GrayMatterColors.SurfaceDark).padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Time", style = MaterialTheme.typography.titleMedium, color = GrayMatterColors.TextPrimary)
                    Spacer(modifier = Modifier.height(24.dp))
                    TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(selectorColor = GrayMatterColors.Primary, clockDialSelectedContentColor = GrayMatterColors.OnPrimary))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isTimeStep = false }) { Text("Back", color = GrayMatterColors.Neutral500) }
                        TextButton(onClick = {
                            val resultCal = Calendar.getInstance()
                            datePickerState.selectedDateMillis?.let { resultCal.timeInMillis = it }
                            resultCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            resultCal.set(Calendar.MINUTE, timePickerState.minute)
                            onConfirm(resultCal.timeInMillis)
                            onDismiss()
                        }) { Text("OK", color = GrayMatterColors.Primary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpinionEditDialog(
    viewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    templates: List<com.example.graymatter.domain.CustomTemplate> = emptyList(),
    onDismiss: () -> Unit, 
    onCreateTemplate: () -> Unit,
    onConfirm: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<com.example.graymatter.domain.CustomTemplate?>(null) }
    var templateFieldValues by remember { mutableStateOf(emptyMap<String, String>()) }
    var confidence by remember { mutableFloatStateOf(0.0f) }
    var selectedReferences by remember { mutableStateOf(emptyList<com.example.graymatter.domain.ReferenceSelectorItem>()) }
    var showReferenceSelector by remember { mutableStateOf(false) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GrayMatterColors.SurfaceDark).border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp)).padding(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Knowledge Entry", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.TextPrimary)
                    
                    com.example.graymatter.android.ui.components.TemplateSelector(
                        templates = templates,
                        selectedTemplate = selectedTemplate,
                        onTemplateSelect = { 
                            selectedTemplate = it
                            it?.let { template ->
                                templateFieldValues = template.headings.associateWith { "" }
                            }
                        },
                        onCreateTemplate = onCreateTemplate
                    )
                }
                
                // Knowledge Connections
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Knowledge Connections", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.Neutral500)
                        IconButton(
                            onClick = {
                                viewModel?.clearSelection()
                                showReferenceSelector = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    if (selectedReferences.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedReferences.forEach { ref ->
                                val refText = when (ref) {
                                    is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                                    is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                                    is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                                }
                                InputChip(
                                    selected = true,
                                    onClick = { selectedReferences = selectedReferences.filter { it.id != ref.id } },
                                    label = { Text(refText, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = GrayMatterColors.SurfaceInput,
                                        labelColor = Color.White,
                                        trailingIconColor = GrayMatterColors.Neutral500
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                if (selectedTemplate != null) {
                    com.example.graymatter.android.ui.components.DynamicEntryForm(
                        template = selectedTemplate!!,
                        fieldValues = templateFieldValues,
                        onFieldValueChange = { heading, value ->
                            templateFieldValues = templateFieldValues.toMutableMap().apply { put(heading, value) }
                        }
                    )
                } else {
                    BasicTextField(value = text, onValueChange = { text = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary), modifier = Modifier.fillMaxWidth().height(120.dp).background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp)).border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp)).padding(16.dp), cursorBrush = SolidColor(GrayMatterColors.Primary), decorationBox = { inner -> if (text.isEmpty()) Text("Type your opinion here...", color = GrayMatterColors.Neutral600); inner() })
                }
                
                Column {
                    Text("Confidence: ${(confidence * 10).toInt()}/10", style = MaterialTheme.typography.labelMedium, color = GrayMatterColors.Neutral500)
                    Slider(value = confidence, onValueChange = { confidence = it }, colors = SliderDefaults.colors(thumbColor = GrayMatterColors.Primary, activeTrackColor = GrayMatterColors.Primary))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = GrayMatterColors.Neutral500) }
                    Button(
                        onClick = { 
                            val finalText = if (selectedTemplate != null) {
                                val sb = StringBuilder()
                                sb.appendLine("[TEMPLATE:${selectedTemplate!!.name}]")
                                selectedTemplate!!.headings.forEach { heading ->
                                    val value = templateFieldValues[heading] ?: ""
                                    if (value.isNotBlank()) {
                                        sb.appendLine("### $heading")
                                        sb.appendLine(value)
                                        sb.appendLine()
                                    }
                                }
                                sb.toString().trim()
                            } else {
                                text
                            }
                            onConfirm(finalText, (confidence * 100).toInt(), selectedReferences) 
                        }, 
                        enabled = (selectedTemplate != null && templateFieldValues.values.any { it.isNotBlank() }) || (selectedTemplate == null && text.isNotBlank()), 
                        colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Primary, contentColor = GrayMatterColors.OnPrimary)
                    ) { Text("Save") }
                }
            }
        }
    }

    if (showReferenceSelector && viewModel != null) {
        com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
            viewModel = viewModel,
            onDismissRequest = { showReferenceSelector = false },
            onConfirm = { items ->
                showReferenceSelector = false
                selectedReferences = (selectedReferences + items).distinctBy { it.id }
            }
        )
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        days < 1 -> "today"
        days < 30 -> "$days days ago"
        else -> "${days / 30} months ago"
    }
}

private fun formatFullDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
