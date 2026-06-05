package com.example.graymatter.android.ui.resourcedetail

import com.example.graymatter.android.ui.theme.GrayMatterTheme
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.ResourceEntryWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.Bookmark
import com.example.graymatter.domain.CustomTemplate
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.File
import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.painterResource

/**
 * Resource Details Screen.
 * Features a beautiful animated timeline where opinions and bookmark reflections are unified.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ResourceDetailScreen(
    resourceEntryDetails: ResourceEntryWithDetails?,
    readingProgress: com.example.graymatter.domain.ReadingProgress?,
    focusOpinionId: String? = null,
    initialSearchQuery: String? = null,
    templates: List<CustomTemplate> = emptyList(),
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    onBackClick: () -> Unit,
    onOpenResource: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onUpdateDescription: (String, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit,
    onAddOpinion: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit,
    onUpdateOpinion: (String, String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit,
    onDeleteOpinion: (String) -> Unit,
    onUndoDeleteOpinion: (String) -> Unit = {},
    onRenameResource: (String) -> Unit,
    onDeleteResourceEntry: () -> Unit,
    onUndoDeleteResourceEntry: () -> Unit = {},
    onEditNote: () -> Unit,
    onExport: (List<Opinion>) -> Unit,
    onExportPdf: (List<Opinion>) -> Unit = {},
    onShareOpinion: (Opinion) -> Unit = {},
    onShareOpinionMarkdown: (Opinion) -> Unit = {},
    onLoadLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    onLoadResourceLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    onViewInGraphClick: (String) -> Unit,
    onNavigateToKnowledgeLink: (com.example.graymatter.domain.ReferenceSelectorItem) -> Unit = {},
    onSaveTemplate: (CustomTemplate) -> Unit = {},
    onNavigateToImageEditor: (android.net.Uri, String, Int) -> Unit = { _, _, _ -> },
    imageResultPath: String? = null,
    visualText: String? = null,
    visualConfidence: Int? = null,
    onClearVisualResult: () -> Unit = {},
    generateUuid: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTemplateEditor by remember { mutableStateOf(false) }
    var showEditDialogId by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var description by remember(resourceEntryDetails?.resourceEntry?.description) { mutableStateOf(resourceEntryDetails?.resourceEntry?.description ?: "") }

    // Description Markdown Editor state
    var showDescriptionEditor by remember { mutableStateOf(false) }
    var showDescRefSelector by remember { mutableStateOf(false) }
    var descReferenceToInsert by remember { mutableStateOf<String?>(null) }
    var descSelectedReferences by remember { mutableStateOf<List<com.example.graymatter.domain.ReferenceSelectorItem>>(emptyList()) }
    var currentDescEditorText by remember { mutableStateOf(description) }
    
    var fullScreenImagePath by remember { mutableStateOf<String?>(null) }

    // Load existing references for the description (use resource.id to match Relatrix graph)
    LaunchedEffect(resourceEntryDetails?.resource?.id) {
        resourceEntryDetails?.resource?.id?.let { resId ->
            onLoadResourceLinks(resId).collect { links ->
                descSelectedReferences = links
            }
        }
    }

    val sortedOpinions = resourceEntryDetails?.opinions?.sortedByDescending { it.createdAt } ?: emptyList()

    // Auto-save visual entries returning from ImageEditorScreen
    LaunchedEffect(imageResultPath, visualText, visualConfidence) {
        if (imageResultPath != null && visualText != null && visualConfidence != null) {
            if (showEditDialogId != null) {
                // For simplicity, we just update the text, confidence, and image path.
                onUpdateOpinion(showEditDialogId!!, visualText, visualConfidence, kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), emptyList(), imageResultPath)
                showEditDialogId = null
            } else {
                onAddOpinion(visualText, visualConfidence, emptyList(), imageResultPath)
            }
            showAddDialog = false
            onClearVisualResult()
        }
    }

    // Robust reference synchronizer for description editor
    LaunchedEffect(currentDescEditorText) {
        val regex = Regex("\\[\\[(.*?)\\]\\]")
        val foundTexts = regex.findAll(currentDescEditorText).map { it.groupValues[1] }.toSet()

        descSelectedReferences = descSelectedReferences.filter { ref ->
            val refText = when (ref) {
                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
            }
            foundTexts.contains(refText) || foundTexts.any { it.endsWith(refText) }
        }.distinctBy { it.id }
    }

    var deletedResourceInfo by remember { mutableStateOf<Pair<String, String>?>(null) } // ID and Title
    var deletedOpinionInfo by remember { mutableStateOf<String?>(null) } // Opinion ID

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterTheme.colors.background),
        containerColor = GrayMatterTheme.colors.background,
        floatingActionButton = {
            if (!isEditing && !showDescriptionEditor) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GrayMatterTheme.colors.textPrimary,
                    contentColor = GrayMatterTheme.colors.background,
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
            if (resourceEntryDetails == null) {
                ResourceDetailHeader(
                    onBackClick = onBackClick,
                    isEditing = false,
                    onToggleEdit = {},
                    onDeleteClick = {}
                )
            }

            if (resourceEntryDetails != null) {
                var selectedFilters by remember { mutableStateOf(setOf("Lookups", "Annotation", "Custom", "Bookmark", "Visual", "Opinion")) }
                var showFilterMenu by remember { mutableStateOf(false) }
                var localFocusOpinionId by remember(focusOpinionId) { mutableStateOf(focusOpinionId) }
                var pulseTrigger by remember { mutableLongStateOf(0L) }
                
                // Unified sorted list of Opinions
                val sortedOpinions = remember(resourceEntryDetails.opinions, selectedFilters) {
                    resourceEntryDetails.opinions.sortedByDescending { it.createdAt }
                        .filter { opinion ->
                            if (opinion.text.contains(" #learnt")) {
                                return@filter false
                            }
                            
                            val isAnnotation = opinion.text.startsWith("> ") || opinion.text.startsWith("[INDEX:")
                            val isDictionary = opinion.text.startsWith("[DICT")
                            val isTemplate = opinion.text.startsWith("[TEMPLATE:")
                            val isCustomTitle = opinion.text.startsWith("[CUSTOM: ")
                            val isVisual = opinion.imagePath != null
                            val hasPageNumber = opinion.pageNumber != null
                            val isPureBookmark = hasPageNumber && opinion.text.isBlank() && opinion.imagePath == null
                            
                            val type = when {
                                isVisual -> "Visual"
                                isDictionary -> "Lookups"
                                isAnnotation -> "Annotation"
                                isTemplate || isCustomTitle -> "Custom"
                                isPureBookmark -> "Bookmark"
                                else -> "Opinion"
                            }
                            
                            selectedFilters.contains(type)
                        }
                }

                // Header with unified dropdown menu
                ResourceDetailHeader(
                    onBackClick = onBackClick,
                    isEditing = isEditing,
                    onToggleEdit = {
                        if (isEditing) {
                            onUpdateDescription(description, descSelectedReferences)
                        }
                        isEditing = !isEditing
                    },
                    onDeleteClick = { showDeleteConfirm = true },
                    onFilterClick = { showFilterMenu = true },
                    onExportClick = { onExport(sortedOpinions) },
                    onExportPdfClick = { onExportPdf(sortedOpinions) },
                    onViewInRelatrixClick = { onViewInGraphClick(resourceEntryDetails.resource.id) }
                )

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Resource Card
                    ResourceCard(
                        title = resourceEntryDetails.resource.title ?: "Untitled",
                        url = resourceEntryDetails.resource.url ?: resourceEntryDetails.resource.filePath ?: "",
                        savedTimeAgo = "Saved ${formatTimeAgo(resourceEntryDetails.resourceEntry.firstOpinionAt)}",
                        readingProgress = readingProgress,
                        onOpenClick = onOpenResource,
                        onRenameClick = { if (isEditing) showRenameDialog = true },
                        showEditButton = isEditing && resourceEntryDetails.resource.type == com.example.graymatter.domain.ResourceType.MARKDOWN,
                        onEditClick = onEditNote
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Description Section — hidden for notes (MARKDOWN), enhanced for files & URLs
                    if (resourceEntryDetails.resource.type != com.example.graymatter.domain.ResourceType.MARKDOWN) {
                        SectionHeader(text = "DESCRIPTION")
                        Spacer(modifier = Modifier.height(12.dp))

                        // Clickable card to open the Markdown description editor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(GrayMatterColors.TypeOpinion.copy(alpha = 0.12f))
                                .border(
                                    1.5.dp,
                                    GrayMatterColors.TypeOpinion.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { showDescriptionEditor = true }
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (description.isNotEmpty()) Icons.Default.EditNote else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = GrayMatterColors.TypeOpinion
                                )
                                Text(
                                    text = if (description.isNotEmpty()) "Edit Resource Description" else "Add Resource Description",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GrayMatterColors.TypeOpinion
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // RESOURCE LINKS section — visible for ALL resource types
                    run {
                        val resourceLinks by onLoadResourceLinks(resourceEntryDetails.resource.id).collectAsState(initial = emptyList())
                        if (resourceLinks.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(GrayMatterColors.TypeLink.copy(alpha = 0.05f))
                                    .border(1.dp, GrayMatterColors.TypeLink.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GrayMatterColors.TypeLink.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Link,
                                            null,
                                            tint = GrayMatterColors.TypeLink,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        "RESOURCE LINKS",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = GrayMatterTheme.colors.textPrimary.copy(alpha = 0.9f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    resourceLinks.forEach { link ->
                                        val linkText = when (link) {
                                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> link.name
                                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> link.title
                                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> link.snippet.take(35)
                                        }
                                        val linkIcon = when (link) {
                                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> Icons.Default.Folder
                                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> Icons.Default.Article
                                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> Icons.Default.FormatQuote
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(GrayMatterColors.TypeLink.copy(alpha = 0.12f))
                                                .border(1.dp, GrayMatterColors.TypeLink.copy(alpha = 0.25f), CircleShape)
                                                .clickable { onNavigateToKnowledgeLink(link) }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Icon(
                                                    linkIcon,
                                                    null,
                                                    tint = GrayMatterColors.TypeLink.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    linkText,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = GrayMatterColors.TypeLink,
                                                    maxLines = 1
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.OpenInNew,
                                                    null,
                                                    tint = GrayMatterColors.TypeLink.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
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
                        text = "Timeline",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = GrayMatterTheme.colors.textPrimary
                    )

                    // Filter bottom sheet / dropdown
                    if (showFilterMenu) {
                        val availableFilters = listOf(
                            "Lookups" to Icons.Default.MenuBook,
                            "Annotation" to Icons.Default.FormatQuote,
                            "Custom" to Icons.Default.DashboardCustomize,
                            "Bookmark" to Icons.Default.Bookmark,
                            "Visual" to Icons.Default.Image,
                            "Opinion" to Icons.Default.QuestionAnswer
                        )
                        val filterNames = availableFilters.map { it.first }.toSet()
                        
                        AlertDialog(
                            onDismissRequest = { showFilterMenu = false },
                            containerColor = GrayMatterTheme.colors.surface,
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Filter Timeline", color = GrayMatterTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                    Row {
                                        TextButton(
                                            onClick = { selectedFilters = filterNames },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("All", color = GrayMatterTheme.colors.primary, fontSize = 13.sp)
                                        }
                                        TextButton(
                                            onClick = { selectedFilters = emptySet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("None", color = GrayMatterTheme.colors.primary, fontSize = 13.sp)
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
                                                    checkedColor = GrayMatterTheme.colors.primary,
                                                    uncheckedColor = GrayMatterTheme.colors.neutral500,
                                                    checkmarkColor = Color.Black
                                                )
                                            )
                                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (isSelected) GrayMatterTheme.colors.primary else GrayMatterTheme.colors.neutral500)
                                            Text(filter, color = GrayMatterTheme.colors.textPrimary)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showFilterMenu = false }) {
                                    Text("Done", color = GrayMatterTheme.colors.primary)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Unified Timeline
                    OpinionTimeline(
                        opinions = sortedOpinions,
                        scrollState = scrollState,
                        resourceId = resourceEntryDetails.resource.id,
                        isEditing = isEditing,
                        focusOpinionId = localFocusOpinionId,
                        templates = templates,
                        referenceSelectorViewModel = referenceSelectorViewModel,
                        onUpdateOpinion = { opinionId, newText, newConfidence, newCreatedAt, newLinks, newImagePath ->
                            onUpdateOpinion(opinionId, newText, newConfidence, newCreatedAt, newLinks, newImagePath)
                        },
                        onShareOpinion = onShareOpinion,
                        onShareOpinionMarkdown = onShareOpinionMarkdown,
                        onDeleteOpinion = { opinionId -> 
                            deletedOpinionInfo = opinionId
                            onDeleteOpinion(opinionId)
                        },
                        onImageClick = { fullScreenImagePath = it },
                        onJumpToPage = { resourceId, page ->
                            onOpenBookmark(Bookmark(id="", resourceId=resourceId, page=page, createdAt=0L))
                        },
                        onLoadLinks = onLoadLinks,
                        onViewInGraph = onViewInGraphClick,
                        onNavigateToKnowledgeLink = { link ->
                            if (link is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem && link.resourceId == resourceEntryDetails.resource.id) {
                                localFocusOpinionId = link.id 
                                pulseTrigger = Clock.System.now().toEpochMilliseconds()
                            } else {
                                onNavigateToKnowledgeLink(link)
                            }
                        },
                        onStartEditingOpinion = { opinionId -> 
                            val opinion = sortedOpinions.find { it.id == opinionId }
                            if (opinion?.imagePath != null) {
                                val uri = android.net.Uri.fromFile(java.io.File(opinion.imagePath!!))
                                onNavigateToImageEditor(uri, opinion.text, opinion.confidenceScore)
                                showEditDialogId = opinionId
                            } else {
                                showEditDialogId = opinionId
                                showAddDialog = true // Assuming showAddDialog is used for both Add and Edit
                            }
                        },
                        pulseTrigger = pulseTrigger,
                        initialSearchQuery = initialSearchQuery
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        if (showAddDialog) {
            // If showEditDialogId is set, find that opinion to edit
            val opinionToEdit = showEditDialogId?.let { id -> sortedOpinions.find { it.id == id } }
            OpinionEditDialog(
                viewModel = referenceSelectorViewModel,
                templates = templates,
                initialText = opinionToEdit?.text ?: "",
                initialConfidence = opinionToEdit?.confidenceScore ?: 0,
                onDismiss = { showAddDialog = false; showEditDialogId = null },
                onCreateTemplate = { showTemplateEditor = true },
                onNavigateToImageEditor = onNavigateToImageEditor,
                onConfirm = { text, confidence, referenceLinks, imagePath ->
                    if (opinionToEdit != null) {
                        onUpdateOpinion(opinionToEdit.id, text, confidence, kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), referenceLinks, imagePath)
                    } else {
                        onAddOpinion(text, confidence, referenceLinks, imagePath)
                    }
                    showAddDialog = false
                    showEditDialogId = null
                }
            )
        }

        if (showRenameDialog && resourceEntryDetails != null) {
            RenameDialog(
                currentName = resourceEntryDetails.resource.title ?: "",
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
                title = { Text("Delete Resource", color = GrayMatterTheme.colors.textPrimary) },
                text = { Text("Are you sure you want to delete this resource and all its opinions? This action cannot be undone.", color = GrayMatterTheme.colors.textSecondary) },
                containerColor = GrayMatterTheme.colors.neutral800,
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        resourceEntryDetails?.let {
                            deletedResourceInfo = Pair(it.resourceEntry.id, it.resource.title ?: "Resource")                            
                        }
                        onDeleteResourceEntry()
                    }) {
                        Text("Delete", color = GrayMatterTheme.colors.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = GrayMatterTheme.colors.textPrimary)
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
        // Description Markdown Editor Overlay
        if (showDescriptionEditor && resourceEntryDetails != null) {
            com.example.graymatter.android.ui.components.MarkdownEditor(
                title = resourceEntryDetails.resource.title ?: "Description",
                initialText = description,
                onBackClick = { showDescriptionEditor = false },
                onSave = { content ->
                    description = content
                    currentDescEditorText = content
                    onUpdateDescription(content, descSelectedReferences)
                    showDescriptionEditor = false
                },
                onTextChange = { currentDescEditorText = it },
                initialPreviewMode = description.isNotBlank(),
                onShowReferenceSelector = {
                    referenceSelectorViewModel?.clearSelection()
                    showDescRefSelector = true
                },
                referenceToInsert = descReferenceToInsert,
                onReferenceInserted = { descReferenceToInsert = null }
            )

            if (showDescRefSelector && referenceSelectorViewModel != null) {
                com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
                    viewModel = referenceSelectorViewModel,
                    onDismissRequest = { showDescRefSelector = false },
                    onConfirm = { items ->
                        showDescRefSelector = false
                        if (items.isNotEmpty()) {
                            descSelectedReferences = (descSelectedReferences + items).distinctBy { it.id }
                            val item = items.first()
                            val text = when (item) {
                                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> "Topic: ${item.name}"
                                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> "Resource: ${item.title}"
                                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> "Opinion: ${item.snippet.take(15)}..."
                            }
                            descReferenceToInsert = "[[$text]]"
                        }
                    }
                )
            }
        }

        // Overlay Undo Snackbars
        if (deletedResourceInfo != null) {
            com.example.graymatter.android.ui.components.UndoSnackbar(
                message = "${deletedResourceInfo!!.second} deleted",
                onUndo = {
                    onUndoDeleteResourceEntry()
                    deletedResourceInfo = null
                },
                onDismissRequest = {
                    deletedResourceInfo = null
                    onBackClick() // Exit screen when finished
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .imePadding()
            )
        } else if (deletedOpinionInfo != null) {
            com.example.graymatter.android.ui.components.UndoSnackbar(
                message = "Opinion deleted",
                onUndo = {
                    onUndoDeleteOpinion(deletedOpinionInfo!!)
                    deletedOpinionInfo = null
                },
                onDismissRequest = {
                    deletedOpinionInfo = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .imePadding()
            )
        }
        
        // Full Screen Image Preview
        if (fullScreenImagePath != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { fullScreenImagePath = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black).clickable { fullScreenImagePath = null },
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = java.io.File(fullScreenImagePath!!),
                        contentDescription = "Full Image",
                        modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    IconButton(
                        onClick = { fullScreenImagePath = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }
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
        containerColor = GrayMatterTheme.colors.neutral800,
        title = { Text("Rename Resource", color = GrayMatterTheme.colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GrayMatterTheme.colors.textPrimary,
                    unfocusedTextColor = GrayMatterTheme.colors.textPrimary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("Rename", color = GrayMatterTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GrayMatterTheme.colors.neutral500)
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
        color = GrayMatterTheme.colors.neutral500
    )
}

@Composable
private fun DescriptionEditor(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayMatterTheme.colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterTheme.colors.textPrimary),
            modifier = Modifier.fillMaxWidth(),
            cursorBrush = SolidColor(GrayMatterTheme.colors.primary),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Add a description...", color = GrayMatterTheme.colors.neutral700)
                inner()
            }
        )
    }
}

@Composable
private fun ResourceDetailHeader(
    onBackClick: () -> Unit,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    onFilterClick: (() -> Unit)? = null,
    onExportClick: (() -> Unit)? = null,
    onExportPdfClick: (() -> Unit)? = null,
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
            Icon(Icons.Default.KeyboardArrowLeft, "Back", tint = GrayMatterTheme.colors.textPrimary, modifier = Modifier.size(32.dp))
        }
        Text("Resource Details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.textPrimary)
        Row {
            if (isEditing) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = GrayMatterTheme.colors.error,
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
                            tint = GrayMatterTheme.colors.textPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(GrayMatterTheme.colors.surface)
                    ) {
                        if (onFilterClick != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.FilterList, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(20.dp))
                                        Text("Filter Timeline", color = GrayMatterTheme.colors.textPrimary)
                                    }
                                },
                                onClick = { menuExpanded = false; onFilterClick() }
                            )
                        }
                        if (onExportClick != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Description, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(20.dp))
                                        Text("Export as Markdown", color = GrayMatterTheme.colors.textPrimary)
                                    }
                                },
                                onClick = { menuExpanded = false; onExportClick() }
                            )
                        }
                        if (onExportPdfClick != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.PictureAsPdf, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(20.dp))
                                        Text("Export as PDF", color = GrayMatterTheme.colors.textPrimary)
                                    }
                                },
                                onClick = { menuExpanded = false; onExportPdfClick() }
                            )
                        }
                        if (onViewInRelatrixClick != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Hub, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(20.dp))
                                        Text("View in Relatrix", color = GrayMatterTheme.colors.textPrimary)
                                    }
                                },
                                onClick = { menuExpanded = false; onViewInRelatrixClick() }
                            )
                        }
                        Divider(color = GrayMatterTheme.colors.neutral800, modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Edit, null, tint = GrayMatterTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                                    Text("Edit Resource", color = GrayMatterTheme.colors.textPrimary)
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
            .background(GrayMatterTheme.colors.surface)
            .border(1.2.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(20.dp))
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
                    color = GrayMatterTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f).clickable(onClick = onRenameClick)
                )
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open",
                    tint = GrayMatterTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(url, style = MaterialTheme.typography.bodySmall, color = GrayMatterTheme.colors.neutral500, maxLines = 2)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Schedule, null, tint = GrayMatterTheme.colors.neutral600, modifier = Modifier.size(16.dp))
                Text(savedTimeAgo, style = MaterialTheme.typography.bodySmall, color = GrayMatterTheme.colors.neutral600)
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
                            color = GrayMatterTheme.colors.primary
                        )
                        Text(
                            "Page ${readingProgress.currentPage + 1} of ${readingProgress.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterTheme.colors.neutral500
                        )
                    }
                    LinearProgressIndicator(
                        progress = readingProgress.percentComplete.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = GrayMatterTheme.colors.primary,
                        trackColor = GrayMatterTheme.colors.neutral800
                    )
                }
            } else if (showEditButton) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayMatterTheme.colors.primary,
                        contentColor = GrayMatterTheme.colors.onPrimary
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
                        containerColor = GrayMatterTheme.colors.neutral900,
                        contentColor = GrayMatterTheme.colors.primary
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
    scrollState: ScrollState,
    resourceId: String,
    isEditing: Boolean,
    focusOpinionId: String? = null,
    templates: List<CustomTemplate>,
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel?,
    onUpdateOpinion: (String, String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit,
    onDeleteOpinion: (String) -> Unit,
    onJumpToPage: (String, Int) -> Unit,
    onLoadLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    onViewInGraph: (String) -> Unit,
    onNavigateToKnowledgeLink: (com.example.graymatter.domain.ReferenceSelectorItem) -> Unit,
    onImageClick: (String) -> Unit,
    onShareOpinion: (Opinion) -> Unit = {},
    onShareOpinionMarkdown: (Opinion) -> Unit = {},
    onStartEditingOpinion: (String) -> Unit = {},
    pulseTrigger: Long = 0L,
    initialSearchQuery: String? = null
) {
    Column {
        opinions.forEachIndexed { index, opinion ->
            OpinionTimelineItem(
                opinion = opinion,
                scrollState = scrollState,
                serialNumber = opinions.size - index,
                isFirst = index == 0,
                isLast = index == opinions.lastIndex,
                isEditing = isEditing,
                isFocused = opinion.id == focusOpinionId,
                templates = templates,
                referenceSelectorViewModel = referenceSelectorViewModel,
                onUpdate = { text, confidence, date, links, imagePath -> onUpdateOpinion(opinion.id, text, confidence, date, links, imagePath) },
                onDelete = { onDeleteOpinion(opinion.id) },
                onJump = {
                    opinion.pageNumber?.let { page ->
                        onJumpToPage(resourceId, page)
                    }
                },
                onLoadLinks = onLoadLinks,
                onViewInGraph = onViewInGraph,
                onNavigateToKnowledgeLink = onNavigateToKnowledgeLink,
                onImageClick = onImageClick,
                onShareOpinion = onShareOpinion,
                onShareOpinionMarkdown = onShareOpinionMarkdown,
                onStartEditing = { onStartEditingOpinion(opinion.id) },
                pulseTrigger = pulseTrigger,
                initialSearchQuery = initialSearchQuery
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun OpinionTimelineItem(
    opinion: Opinion,
    scrollState: ScrollState,
    serialNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isEditing: Boolean,
    isFocused: Boolean = false,
    templates: List<CustomTemplate>,
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel?,
    onUpdate: (String, Int, Long, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit,
    onDelete: () -> Unit,
    onJump: () -> Unit,
    onLoadLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    onViewInGraph: (String) -> Unit,
    onNavigateToKnowledgeLink: (com.example.graymatter.domain.ReferenceSelectorItem) -> Unit,
    onImageClick: (String) -> Unit,
    onShareOpinion: (Opinion) -> Unit = {},
    onShareOpinionMarkdown: (Opinion) -> Unit = {},
    onStartEditing: () -> Unit = {},
    pulseTrigger: Long = 0L,
    initialSearchQuery: String? = null
) {
    var text by remember(opinion.text) { mutableStateOf(opinion.text) }
    var confidence by remember(opinion.confidenceScore) { mutableStateOf(opinion.confidenceScore.toFloat() / 100f) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showItemMenu by remember { mutableStateOf(false) }

    @Composable
    fun highlightText(
        fullText: String,
        query: String?,
        baseColor: Color = GrayMatterTheme.colors.textPrimary,
        baseAlpha: Float = 1f
    ): androidx.compose.ui.text.AnnotatedString {
        return remember(fullText, query) {
            buildAnnotatedString {
                if (query.isNullOrBlank() || query.length < 2) {
                    withStyle(SpanStyle(color = baseColor.copy(alpha = baseAlpha))) {
                        append(fullText)
                    }
                } else {
                    val lowerText = fullText.lowercase()
                    val lowerQuery = query.lowercase()
                    var currentIndex = 0

                    while (currentIndex < fullText.length) {
                        val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
                        if (matchIndex == -1) {
                            withStyle(SpanStyle(color = baseColor.copy(alpha = baseAlpha))) {
                                append(fullText.substring(currentIndex))
                            }
                            break
                        }

                        if (matchIndex > currentIndex) {
                            withStyle(SpanStyle(color = baseColor.copy(alpha = baseAlpha))) {
                                append(fullText.substring(currentIndex, matchIndex))
                            }
                        }

                        // High-performance premium highlight (amber/gold)
                        withStyle(SpanStyle(
                            color = Color(0xFFFFCC00),
                            fontWeight = FontWeight.Bold,
                            background = Color(0xFFFFCC00).copy(alpha = 0.15f)
                        )) {
                            append(fullText.substring(matchIndex, matchIndex + query.length))
                        }

                        currentIndex = matchIndex + query.length
                    }
                }
            }
        }
    }
    
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

    val backgroundColor = remember { androidx.compose.animation.Animatable(Color.Transparent) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var itemHeight by remember { mutableIntStateOf(0) }

    val containsQuery = remember(opinion.text, initialSearchQuery) {
        !initialSearchQuery.isNullOrBlank() && opinion.text.contains(initialSearchQuery, ignoreCase = true)
    }

    val primaryColor = GrayMatterTheme.colors.primary
    LaunchedEffect(isFocused, isEditing, pulseTrigger, containsQuery) {
        if (isFocused || isEditing || containsQuery) {
            val viewportHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
            val itemH = itemHeight.toFloat()
            
            if (itemH > 0) {
                // To center an item of height H in viewport V, we bring into view a rect 
                // that is the size of the viewport and centered on the item.
                bringIntoViewRequester.bringIntoView(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = 0f,
                        top = itemH / 2 - viewportHeight / 2,
                        right = 0f,
                        bottom = itemH / 2 + viewportHeight / 2
                    )
                )
            } else {
                bringIntoViewRequester.bringIntoView()
            }
            
            // Pulse effect for focus
            if (isFocused) {
                val pulseColor = primaryColor.copy(alpha = 0.25f)
                backgroundColor.animateTo(pulseColor, tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                backgroundColor.animateTo(Color.Transparent, tween(800, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(backgroundColor.value)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { itemHeight = it.size.height }
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
            val lineColor = GrayMatterTheme.colors.neutral800
            
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp + dotSize / 2)
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
                    .padding(top = 12.dp)
                    .size(dotSize)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(if (isFirst) GrayMatterTheme.colors.primary else GrayMatterTheme.colors.neutral600)
                    .border(2.dp, GrayMatterTheme.colors.background, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Card container for entire entry content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 16.dp else 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GrayMatterTheme.colors.surface)
                .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
            // Header: Serial Number, Title, Date & Confidence Badge
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalAlignment = Alignment.Top) {
                    // Plain serial number aligned with the first line of text
                    Text(
                        text = serialNumber.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = com.example.graymatter.android.ui.theme.InterFontFamily
                        ),
                        color = GrayMatterTheme.colors.neutral500,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        val cleanText = opinion.text.replace(Regex("\\[COLOR:[^\\]]+\\]\\s*", RegexOption.IGNORE_CASE), "")
                        val isAnnotation = cleanText.startsWith("> ") || cleanText.startsWith("[INDEX:")
                        val isDictionary = cleanText.startsWith("[DICT")
                        val isTemplate = cleanText.startsWith("[TEMPLATE:")
                        val isCustomTitle = cleanText.startsWith("[CUSTOM: ")
                        val hasPageNumber = opinion.pageNumber != null
                        
                        val dynamicTitle = when {
                            isTemplate -> cleanText.substringAfter("[TEMPLATE:").substringBefore("]")
                            isCustomTitle -> cleanText.substringAfter("[CUSTOM: ").substringBefore("]")
                            else -> "CUSTOM ENTRY"
                        }
                        
                        val isVisual = opinion.imagePath != null
                        
                        val (title, icon, color) = when {
                            isVisual -> Triple("VISUAL", Icons.Default.Image, GrayMatterColors.TypeVisual)
                            isDictionary -> Triple("LOOKUP", Icons.Default.MenuBook, GrayMatterColors.TypeLookupMain)
                            isAnnotation -> Triple("ANNOTATION", Icons.Default.FormatQuote, GrayMatterColors.TypeAnnotation)
                            isTemplate -> Triple("TEMPLATE", Icons.Default.DashboardCustomize, GrayMatterColors.TypeTemplate)
                            isCustomTitle -> Triple(dynamicTitle.uppercase(), Icons.Default.EditNote, GrayMatterColors.TypeOpinion)
                            else -> Triple("OPINION", Icons.Default.QuestionAnswer, GrayMatterColors.TypeOpinion)
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
                                text = formatDate(opinion.createdAt).uppercase(), 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), 
                                color = GrayMatterTheme.colors.neutral600
                            )
                            Text(
                                text = formatTime(opinion.createdAt).uppercase(), 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp), 
                                color = GrayMatterTheme.colors.neutral600.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfidenceBadge(score = opinion.confidenceScore)
                    // 3-dot overflow menu
                    val isDictionary = opinion.text.startsWith("[DICT")
                    Box {
                        IconButton(onClick = { showItemMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, "More options", tint = GrayMatterTheme.colors.neutral500, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showItemMenu,
                            onDismissRequest = { showItemMenu = false },
                            modifier = Modifier.background(GrayMatterTheme.colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Hub, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(18.dp))
                                        Text("View in Relatrix", color = GrayMatterTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                onClick = { showItemMenu = false; onViewInGraph(opinion.id) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Image, null, tint = GrayMatterTheme.colors.textPrimary, modifier = Modifier.size(18.dp))
                                        Text("Export as Image", color = GrayMatterTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                onClick = { showItemMenu = false; onShareOpinion(opinion) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Description, null, tint = GrayMatterTheme.colors.textPrimary, modifier = Modifier.size(18.dp))
                                        Text("Export as Markdown", color = GrayMatterTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                onClick = { showItemMenu = false; onShareOpinionMarkdown(opinion) }
                            )
                            if (isDictionary) {
                                val isCurrentlyLearnt = text.contains(" #learnt")
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(
                                                imageVector = if (isCurrentlyLearnt) Icons.Default.Restore else Icons.Default.LibraryAddCheck, 
                                                contentDescription = null, 
                                                tint = GrayMatterColors.TypeLookupMain, 
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(if (isCurrentlyLearnt) "Mark as Learning" else "Mark as Learnt", color = GrayMatterTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    },
                                    onClick = { 
                                        showItemMenu = false; 
                                        val newText = if (isCurrentlyLearnt) {
                                            text.replace(" #learnt", "")
                                        } else {
                                            if (text.contains(" #learnt")) text else "$text #learnt"
                                        }
                                        onUpdate(newText, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                                    }
                                )
                            }
                            if (!isDictionary) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Default.Edit, null, tint = GrayMatterTheme.colors.textPrimary, modifier = Modifier.size(18.dp))
                                            Text("Edit Entry", color = GrayMatterTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    },
                                    onClick = { showItemMenu = false; onStartEditing() }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Delete, null, tint = GrayMatterTheme.colors.error, modifier = Modifier.size(18.dp))
                                        Text("Delete", color = GrayMatterTheme.colors.error, style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                onClick = { showItemMenu = false; onDelete() }
                            )
                            if (opinion.pageNumber != null) {
                                Divider(color = GrayMatterTheme.colors.neutral800, modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Default.Launch, null, tint = GrayMatterColors.TypeBookmark, modifier = Modifier.size(18.dp))
                                            Text("Jump to Page ${opinion.pageNumber!! + 1}", color = GrayMatterTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    },
                                    onClick = { showItemMenu = false; onJump() }
                                )
                            }
                        }
                    }
                }
            }
            
            val cleanTextForContent = text.replace(Regex("\\[COLOR:[^\\]]+\\]\\s*", RegexOption.IGNORE_CASE), "")
            val isAnnotation = cleanTextForContent.startsWith("> ") || cleanTextForContent.startsWith("[INDEX:")
            val isDictionary = cleanTextForContent.startsWith("[DICT")
            val isTemplate = cleanTextForContent.startsWith("[TEMPLATE:")
            val isCustomTitle = cleanTextForContent.startsWith("[CUSTOM: ")
            val isVisual = opinion.imagePath != null
            val hasPageNumber = opinion.pageNumber != null
            val isPureBookmark = hasPageNumber && cleanTextForContent.isBlank() && opinion.imagePath == null

            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Opinion Content
            if (isEditing && !isDictionary) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Knowledge Connections in Edit Mode
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TIMELINE LINKS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = GrayMatterTheme.colors.neutral500)
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
                                        onUpdate(text, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                                    },
                                    label = { Text(refText, maxLines = 1) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }

                    if (isTemplate) {
                        // Try to parse and show dynamic form for custom entry editing
                        val templateName = cleanTextForContent.substringAfter("[TEMPLATE:").substringBefore("]")
                        val template = templates.find { it.name == templateName }
                        
                        if (template != null) {
                            val fieldValues = remember(text) { parseTemplateContent(cleanTextForContent, template.headings) }
                            DynamicEntryEditor(
                                template = template,
                                fieldValues = fieldValues,
                                onFieldChange = { heading, newVal ->
                                    val newValues = fieldValues.toMutableMap().apply { put(heading, newVal) }
                                    val newText = formatTemplateContent(template, newValues)
                                    text = newText
                                    onUpdate(newText, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                                },
                                confidence = confidence,
                                onConfidenceChange = {
                                    confidence = it
                                    onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                                }
                            )
                        } else {
                            // Fallback to plain text editor if template not found
                            OpinionEditor(
                                text = text,
                                confidence = confidence,
                                onTextChange = { 
                                    text = it 
                                    onUpdate(it, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                                },
                                onConfidenceChange = { 
                                    confidence = it 
                                    onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                                }
                            )
                        }
                    } else if (isAnnotation) {
                        // Split into quote and reflection
                        val cleanText = text.replace(Regex("\\[COLOR:[^\\]]+\\]\\s*"), "")
                        val parts = cleanText.split("\n\n", limit = 2)
                        val quote = parts[0].replace(Regex("\\[INDEX:\\d+\\]\\s*"), "").removePrefix("> ").trim()
                        val initialReflection = if (parts.size > 1) parts[1].trim() else ""
                        
                        // Show read-only quote block above the editor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.width(4.dp).height(IntrinsicSize.Min).background(GrayMatterColors.TypeAnnotation)) // Orange accent
                                Text(
                                    text = "\"$quote\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 24.sp),
                                    color = GrayMatterTheme.colors.neutral400,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        
                        OpinionEditor(
                            text = initialReflection,
                            confidence = confidence,
                            onTextChange = { newVal ->
                                val currentParts = text.split("\n\n", limit = 2)
                                val quotePart = currentParts[0]
                                val newFullText = "$quotePart\n\n$newVal"
                                text = newFullText
                                onUpdate(newFullText, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                            },
                            onConfidenceChange = { 
                                confidence = it 
                                onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                            }
                        )
                    } else {
                        OpinionEditor(
                            text = if (isCustomTitle) cleanTextForContent.substringAfter("]\n").trim() else cleanTextForContent,
                            confidence = confidence,
                            onTextChange = { 
                                val newFullText = if (isCustomTitle) {
                                    val prefix = cleanTextForContent.substringBefore("]\n") + "]\n"
                                    prefix + it
                                } else {
                                    it
                                }
                                text = newFullText 
                                onUpdate(newFullText, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                            },
                            onConfidenceChange = { 
                                confidence = it 
                                onUpdate(text, (it * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
                            }
                        )
                    }
                }
            } else {
                if (isVisual) {
                    // Visual Entry — image is the primary content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GrayMatterColors.TypeVisual.copy(alpha = 0.1f))
                            .border(1.dp, GrayMatterColors.TypeVisual.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    .background(GrayMatterTheme.colors.neutral900)
                                    .clickable { onImageClick(opinion.imagePath!!) }
                            ) {
                                AsyncImage(
                                    model = if (opinion.imagePath!!.startsWith("content://")) opinion.imagePath!! else java.io.File(opinion.imagePath!!),
                                    contentDescription = "Visual Entry",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    error = painterResource(id = android.R.drawable.ic_menu_report_image)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.OpenInFull, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                                }
                            }
                            // Caption (if any)
                            if (cleanTextForContent.isNotBlank()) {
                                Text(
                                    text = highlightText(cleanTextForContent, initialSearchQuery, GrayMatterTheme.colors.textSecondary),
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                } else if (isDictionary) {
                    val phrase = cleanTextForContent.substringAfter("]").trim()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.TypeLookupMain.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.TypeLookupMain.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = highlightText(phrase, initialSearchQuery, Color.White),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                } else if (isAnnotation) {
                    // Split into quote and reflection
                    val parts = cleanTextForContent.split("\n\n", limit = 2)
                    val quote = parts[0].replace(Regex("\\[INDEX:\\d+\\]\\s*"), "").removePrefix("> ").trim()
                    val reflection = if (parts.size > 1) parts[1].trim() else ""
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.TypeAnnotation.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.TypeAnnotation.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                Box(modifier = Modifier.width(4.dp).height(IntrinsicSize.Min).background(GrayMatterColors.TypeAnnotation)) // Orange accent
                                Text(
                                    text = highlightText("\"$quote\"", initialSearchQuery, GrayMatterTheme.colors.neutral400),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 24.sp),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        // Reflection
                        if (reflection.isNotEmpty()) {
                            Text(
                                text = highlightText(reflection, initialSearchQuery, GrayMatterTheme.colors.textPrimary),
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
                            )
                        }
                    }
                } else if (isTemplate) {
                    val templateName = cleanTextForContent.substringAfter("[TEMPLATE:").substringBefore("]")
                    val content = cleanTextForContent.substringAfter("]\n").trim()
                    
                    if (content.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GrayMatterColors.TypeTemplate.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, GrayMatterColors.TypeTemplate.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = templateName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = GrayMatterColors.TypeTemplate
                            )
                            
                            // Parse markdown-ish structure
                            val headings = content.split("### ").filter { it.isNotBlank() }
                            headings.forEach { headingBlock ->
                                val lines = headingBlock.split("\n", limit = 2)
                                val heading = lines[0].trim()
                                val response = lines.getOrNull(1)?.trim() ?: ""
                                
                                if (response.isNotBlank()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = heading,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GrayMatterColors.TypeTemplate
                                        )
                                        Text(
                                            text = highlightText(response, initialSearchQuery, GrayMatterTheme.colors.textPrimary),
                                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (isCustomTitle) {
                    val displayContent = cleanTextForContent.substringAfter("]\n").trim()
                    if (displayContent.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GrayMatterColors.TypeOpinion.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, GrayMatterColors.TypeOpinion.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = highlightText(displayContent, initialSearchQuery, GrayMatterTheme.colors.textPrimary),
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
                            )
                        }
                    }
                } else if (isPureBookmark) {
                    // Bookmark or old style page opinion
                    val cleanText = if (cleanTextForContent.startsWith("[Page ")) cleanTextForContent.replace(Regex("^\\[Page \\d+\\]\\s*"), "") else cleanTextForContent
                    val displayText = if (cleanText.isNotBlank()) cleanText else "Bookmark Page ${opinion.pageNumber!! + 1}"
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterColors.TypeBookmark.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.TypeBookmark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = highlightText(displayText, initialSearchQuery, GrayMatterTheme.colors.textPrimary),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
                        )
                    }
                } else {
                    // General Opinion (including page-numbered opinions)
                    val cleanText = if (cleanTextForContent.startsWith("[Page ")) cleanTextForContent.replace(Regex("^\\[Page \\d+\\]\\s*"), "") else cleanTextForContent
                    if (cleanText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GrayMatterColors.TypeOpinion.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, GrayMatterColors.TypeOpinion.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = highlightText(cleanText, initialSearchQuery, GrayMatterTheme.colors.textPrimary),
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
                            )
                        }
                    }
                }
                
                // Show Image for non-visual entries that happen to have images (legacy)
                if (opinion.imagePath != null && !isVisual) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GrayMatterTheme.colors.neutral900)
                            .clickable { onImageClick(opinion.imagePath!!) }
                    ) {
                        AsyncImage(
                            model = if (opinion.imagePath!!.startsWith("content://")) opinion.imagePath!! else java.io.File(opinion.imagePath!!),
                            contentDescription = "Opinion Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.OpenInFull, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                        }
                    }
                }

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
                                onClick = { 
                                    onNavigateToKnowledgeLink(link)
                                },
                                label = { Text(linkText, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = GrayMatterTheme.colors.surface,
                                    labelColor = GrayMatterTheme.colors.primary,
                                    leadingIconContentColor = GrayMatterTheme.colors.primary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GrayMatterTheme.colors.neutral700)
                            )
                        }
                    }
                }
                if (hasPageNumber && !isEditing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val (tagColor, tagIcon) = when {
                        opinion.imagePath != null -> GrayMatterColors.TypeVisual to Icons.Default.Image
                        cleanTextForContent.startsWith("[DICT") -> GrayMatterColors.TypeLookupMain to Icons.Default.MenuBook
                        cleanTextForContent.startsWith("[TEMPLATE:") -> GrayMatterColors.TypeTemplate to Icons.Default.Assignment
                        cleanTextForContent.startsWith("> ") || cleanTextForContent.startsWith("[INDEX:") -> GrayMatterColors.TypeAnnotation to Icons.Default.FormatQuote
                        isPureBookmark -> GrayMatterColors.TypeBookmark to Icons.Default.Bookmark
                        else -> GrayMatterColors.TypeOpinion to Icons.Default.QuestionAnswer
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tagColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = tagIcon, 
                                contentDescription = null, 
                                tint = tagColor, 
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                "Page ${opinion.pageNumber!! + 1}", 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.SemiBold, 
                                color = tagColor, 
                                maxLines = 1, 
                                softWrap = false
                            )
                        }
                    }
                }
            }
            } // end Card Box
        }
    }

    if (showDateTimePicker) {
        DateTimePicker(
            initialTimestamp = opinion.createdAt,
            onDismiss = { showDateTimePicker = false },
            onConfirm = { onUpdate(text, (confidence * 100).toInt(), it, selectedReferences, opinion.imagePath) }
        )
    }

    if (showReferenceSelector && referenceSelectorViewModel != null) {
        com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
            viewModel = referenceSelectorViewModel,
            onDismissRequest = { showReferenceSelector = false },
            onConfirm = { items ->
                showReferenceSelector = false
                selectedReferences = (selectedReferences + items).distinctBy { it.id }
                onUpdate(text, (confidence * 100).toInt(), opinion.createdAt, selectedReferences, opinion.imagePath)
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
                    color = GrayMatterTheme.colors.neutral500
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GrayMatterTheme.colors.surfaceInput, RoundedCornerShape(12.dp))
                        .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = fieldValues[heading] ?: "",
                        onValueChange = { onFieldChange(heading, it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterTheme.colors.textPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(GrayMatterColors.TypeTemplate)
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
                        color = GrayMatterTheme.colors.neutral500
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterTheme.colors.surfaceInput, RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = { onFieldChange(heading, it) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterTheme.colors.textPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(GrayMatterColors.TypeTemplate)
                        )
                    }
                }
            }
        }
        
        Slider(
            value = confidence, 
            onValueChange = onConfidenceChange, 
            colors = SliderDefaults.colors(
                thumbColor = Color.White, 
                activeTrackColor = Color.White,
                inactiveTrackColor = GrayMatterTheme.colors.neutral800
            )
        )
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
            .background(GrayMatterTheme.colors.neutral900)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text(
                    text = formatDate(timestamp).uppercase(), 
                    style = MaterialTheme.typography.labelSmall.copy(color = GrayMatterTheme.colors.primary, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = formatTime(timestamp).uppercase(), 
                    style = MaterialTheme.typography.labelSmall.copy(color = GrayMatterTheme.colors.primary.copy(alpha = 0.7f), fontWeight = FontWeight.Medium, fontSize = 10.sp)
                )
            }
            Icon(Icons.Default.CalendarToday, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ConfidenceBadge(score: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(GrayMatterTheme.colors.surface)
            .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = if (score > 50) Icons.Default.TrendingUp else Icons.Default.ArrowForward,
                contentDescription = null,
                tint = GrayMatterTheme.colors.primary,
                modifier = Modifier.size(12.dp)
            )
            Text("${score/10}/10", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.textPrimary, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun OpinionEditor(text: String, confidence: Float, onTextChange: (String) -> Unit, onConfidenceChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(GrayMatterTheme.colors.surfaceInput, RoundedCornerShape(12.dp)).border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(12.dp)).padding(12.dp)) {
            BasicTextField(value = text, onValueChange = onTextChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterTheme.colors.textPrimary), modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(GrayMatterTheme.colors.primary))
        }
        Slider(
            value = confidence, 
            onValueChange = onConfidenceChange, 
            colors = SliderDefaults.colors(
                thumbColor = Color.White, 
                activeTrackColor = Color.White,
                inactiveTrackColor = GrayMatterTheme.colors.neutral800
            )
        )
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
            confirmButton = { TextButton(onClick = { isTimeStep = true }) { Text("Next", color = GrayMatterTheme.colors.primary) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GrayMatterTheme.colors.neutral500) } },
            colors = DatePickerDefaults.colors(containerColor = GrayMatterTheme.colors.surface)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(selectedDayContainerColor = GrayMatterTheme.colors.primary, selectedDayContentColor = GrayMatterTheme.colors.onPrimary, todayContentColor = GrayMatterTheme.colors.primary, todayDateBorderColor = GrayMatterTheme.colors.primary))
        }
    } else {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
            Box(modifier = Modifier.clip(RoundedCornerShape(28.dp)).background(GrayMatterTheme.colors.surface).padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Time", style = MaterialTheme.typography.titleMedium, color = GrayMatterTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(24.dp))
                    TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(selectorColor = GrayMatterTheme.colors.primary, clockDialSelectedContentColor = GrayMatterTheme.colors.onPrimary))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isTimeStep = false }) { Text("Back", color = GrayMatterTheme.colors.neutral500) }
                        TextButton(onClick = {
                            val resultCal = Calendar.getInstance()
                            datePickerState.selectedDateMillis?.let { resultCal.timeInMillis = it }
                            resultCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            resultCal.set(Calendar.MINUTE, timePickerState.minute)
                            onConfirm(resultCal.timeInMillis)
                            onDismiss()
                        }) { Text("OK", color = GrayMatterTheme.colors.primary) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpinionEditDialog(
    viewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    templates: List<com.example.graymatter.domain.CustomTemplate> = emptyList(),
    initialText: String = "",
    initialConfidence: Int = 0,
    onDismiss: () -> Unit, 
    onCreateTemplate: () -> Unit,
    onNavigateToImageEditor: (Uri, String, Int) -> Unit,
    onConfirm: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedTemplate by remember { mutableStateOf<com.example.graymatter.domain.CustomTemplate?>(null) }
    var templateFieldValues by remember { mutableStateOf(emptyMap<String, String>()) }
    var confidence by remember { mutableFloatStateOf(if (initialConfidence > 0) initialConfidence / 100f else 0.0f) }
    var selectedReferences by remember { mutableStateOf(emptyList<com.example.graymatter.domain.ReferenceSelectorItem>()) }
    var showReferenceSelector by remember { mutableStateOf(false) }
    var currentImagePath by remember { mutableStateOf<String?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onNavigateToImageEditor(it, text, (confidence * 100).toInt()) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            onNavigateToImageEditor(tempCameraUri!!, text, (confidence * 100).toInt())
        }
    }

    val isVisualMode = currentImagePath != null
    val accentColor = if (isVisualMode) GrayMatterColors.TypeVisual else GrayMatterTheme.colors.primary
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GrayMatterTheme.colors.surface)
                .border(1.dp, if (isVisualMode) GrayMatterColors.TypeVisual.copy(alpha = 0.3f) else GrayMatterTheme.colors.neutral800, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isVisualMode) {
                            Icon(Icons.Default.Image, null, tint = GrayMatterColors.TypeVisual, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            if (isVisualMode) "Add Visual" else "Add Knowledge Entry",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GrayMatterTheme.colors.textPrimary
                        )
                    }
                    
                    if (!isVisualMode) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { showImagePicker = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto, 
                                    "Add Image", 
                                    tint = if (GrayMatterTheme.colors.isLight) Color.Black else Color.White, 
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                    }
                }

                // Visual Mode: Prominent image preview
                if (isVisualMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GrayMatterTheme.colors.neutral900)
                    ) {
                        AsyncImage(
                            model = java.io.File(currentImagePath!!),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Remove image button
                        IconButton(
                            onClick = { currentImagePath = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Caption field (optional for visual entries)
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = GrayMatterTheme.colors.textPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayMatterTheme.colors.surfaceInput, RoundedCornerShape(12.dp))
                            .border(1.dp, GrayMatterColors.TypeVisual.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        cursorBrush = SolidColor(GrayMatterColors.TypeVisual),
                        decorationBox = { inner ->
                            if (text.isEmpty()) Text("Add a caption (optional)...", color = GrayMatterTheme.colors.neutral600, style = MaterialTheme.typography.bodyMedium)
                            inner()
                        }
                    )
                } else {
                    // Action row: Knowledge Connections + Add Image button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Knowledge Connections", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.neutral500)
                                IconButton(
                                    onClick = {
                                        viewModel?.clearSelection()
                                        showReferenceSelector = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(20.dp))
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
                                                containerColor = GrayMatterTheme.colors.surfaceInput,
                                                labelColor = Color.White,
                                                trailingIconColor = GrayMatterTheme.colors.neutral500
                                            ),
                                            border = null
                                        )
                                    }
                                }
                            }
                        }
                        
                    }

                    // Text input or template
                    if (selectedTemplate != null) {
                        com.example.graymatter.android.ui.components.DynamicEntryForm(
                            template = selectedTemplate!!,
                            fieldValues = templateFieldValues,
                            onFieldValueChange = { heading, value ->
                                templateFieldValues = templateFieldValues.toMutableMap().apply { put(heading, value) }
                            }
                        )
                    } else {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterTheme.colors.textPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(GrayMatterTheme.colors.surfaceInput, RoundedCornerShape(12.dp))
                                .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            cursorBrush = SolidColor(GrayMatterTheme.colors.primary),
                            decorationBox = { inner ->
                                if (text.isEmpty()) Text("Type your opinion here...", color = GrayMatterTheme.colors.neutral600)
                                inner()
                            }
                        )
                    }
                }
                
                // Confidence slider
                Column {
                    Text("Confidence: ${(confidence * 10).toInt()}/10", style = MaterialTheme.typography.labelMedium, color = GrayMatterTheme.colors.neutral500)
                    Slider(
                        value = confidence,
                        onValueChange = { confidence = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = GrayMatterTheme.colors.neutral800
                        )
                    )
                }

                // Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = GrayMatterTheme.colors.neutral500) }
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
                            onConfirm(finalText, (confidence * 100).toInt(), selectedReferences, currentImagePath) 
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = if (isVisualMode) Color.White else GrayMatterTheme.colors.onPrimary)
                    ) { Text(if (isVisualMode) "Save Visual" else "Save") }
                }
            }

            // Image Picker Internal Overlay
            AnimatedVisibility(
                visible = showImagePicker,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GrayMatterTheme.colors.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Add Image", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.textPrimary)
                            IconButton(onClick = { showImagePicker = false }) {
                                Icon(Icons.Default.Close, null, tint = GrayMatterTheme.colors.neutral600)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Take Photo option
                        Surface(
                            onClick = {
                                showImagePicker = false
                                tempCameraUri = com.example.graymatter.android.util.FileUtils.createTempImageUri(context)
                                tempCameraUri?.let { cameraLauncher.launch(it) }
                            },
                            color = GrayMatterTheme.colors.surfaceInput,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).background(GrayMatterColors.TypeVisual.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoCamera, null, tint = GrayMatterColors.TypeVisual, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text("Take Photo", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = GrayMatterTheme.colors.textPrimary)
                                    Text("Capture with camera", style = MaterialTheme.typography.bodySmall, color = GrayMatterTheme.colors.neutral500)
                                }
                            }
                        }
                        
                        // Gallery option
                        Surface(
                            onClick = {
                                showImagePicker = false
                                galleryLauncher.launch("image/*")
                            },
                            color = GrayMatterTheme.colors.surfaceInput,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).background(GrayMatterColors.TypeVisual.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, null, tint = GrayMatterColors.TypeVisual, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text("Choose from Gallery", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = GrayMatterTheme.colors.textPrimary)
                                    Text("Pick an existing image", style = MaterialTheme.typography.bodySmall, color = GrayMatterTheme.colors.neutral500)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

private fun formatFullDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
