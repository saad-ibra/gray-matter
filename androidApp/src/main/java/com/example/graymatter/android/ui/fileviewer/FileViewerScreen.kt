package com.example.graymatter.android.ui.fileviewer

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.components.TemplateSelector
import com.example.graymatter.android.ui.components.TemplateEditorDialog
import com.example.graymatter.android.ui.components.MarkdownEditor
import com.example.graymatter.domain.ChapterOutline
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.domain.CustomTemplate
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.example.graymatter.android.util.FileUtils
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.QuestionAnswer

/**
 * Main file viewer screen.
 * Implements gesture-based navigation, auto-cropping, and premium themes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    viewModel: FileViewerViewModel,
    resourceId: String,
    initialPage: Int? = null,
    onBackClick: () -> Unit,
    onViewInGraph: (String) -> Unit,
    onNavigateToDictionaryOrigin: (opinionId: String, itemId: String) -> Unit = { _, _ -> },
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null
) {
    val resource by viewModel.resource.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val opinions by viewModel.opinions.collectAsState()
    val globalDictionaryWords by viewModel.globalDictionaryWords.collectAsState()
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var lastHapticTime by remember { mutableLongStateOf(0L) }
    var lastPageChangeTime by remember { mutableLongStateOf(0L) }
    
    val performThrottledHaptic: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastHapticTime > 40) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            lastHapticTime = now
        }
    }
    
    var showAddEntrySheet by remember { mutableStateOf(false) }
    var showTemplateEditor by remember { mutableStateOf(false) }
    
    var editingOpinion by remember { mutableStateOf<com.example.graymatter.domain.Opinion?>(null) }

    LaunchedEffect(resourceId, initialPage) {
        viewModel.loadResource(resourceId, initialPage)
    }

    var showEditReferenceSelector by remember { mutableStateOf(false) }
    var editReferenceToInsert by remember { mutableStateOf<String?>(null) }
    var editSelectedReferences by remember { mutableStateOf(emptyList<com.example.graymatter.domain.ReferenceSelectorItem>()) }
    var currentEditorText by remember { mutableStateOf(resource?.extractedText ?: "") }

    // Load existing references on startup if not already loaded
    LaunchedEffect(resourceId) {
        if (referenceSelectorViewModel != null) {
            viewModel.getLinksForResource(resourceId).collect { links ->
                editSelectedReferences = links
            }
        }
    }

    // Robust reference synchronizer (matches NewEntryScreen)
    LaunchedEffect(currentEditorText) {
        val regex = Regex("\\[\\[(.*?)\\]\\]")
        val foundTexts = regex.findAll(currentEditorText).map { it.groupValues[1] }.toSet()
        
        editSelectedReferences = editSelectedReferences.filter { ref ->
            val refText = when (ref) {
                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
            }
            foundTexts.contains(refText) || foundTexts.any { it.endsWith(refText) }
        }.distinctBy { it.id }
    }

    // Auto-trigger external viewer for unsupported internal types
    LaunchedEffect(resource) {
        resource?.let { res ->
            if (res.type != ResourceType.PDF && res.type != ResourceType.MARKDOWN) {
                res.filePath?.let { path ->
                    FileUtils.openFileWithIntent(context, path)
                }
                onBackClick()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgressOnExit()
        }
    }

    val themeColors = getThemeColors(settings.theme)
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.VolumeUp -> {
                            val now = System.currentTimeMillis()
                            if (now - lastPageChangeTime > 200L) {
                                viewModel.previousPage()
                                performThrottledHaptic()
                                lastPageChangeTime = now
                            }
                            true
                        }
                        Key.VolumeDown -> {
                            val now = System.currentTimeMillis()
                            if (now - lastPageChangeTime > 200L) {
                                viewModel.nextPage()
                                performThrottledHaptic()
                                lastPageChangeTime = now
                            }
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    // Consume KeyUp for Volume keys to prevent system volume UI from appearing
                    when (keyEvent.key) {
                        Key.VolumeUp, Key.VolumeDown -> true
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        // ── Content Area ──
        resource?.let { res ->
            val isPaged = res.type == ResourceType.PDF

            Box(modifier = Modifier.fillMaxSize()) {
                // ── The Viewer Content ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isPaged) {
                            detectTapGestures(onTap = { offset ->
                                val width = size.width
                                if (isPaged && offset.x < width * 0.15f) {
                                    viewModel.previousPage()
                                    performThrottledHaptic()
                                } else if (isPaged && offset.x > width * 0.85f) {
                                    viewModel.nextPage()
                                    performThrottledHaptic()
                                } else {
                                    viewModel.toggleBars()
                                }
                            })
                        }
                ) {
                    when (res.type) {
                        ResourceType.PDF -> {
                            PdfViewerContent(
                                filePath = res.filePath ?: "",
                                currentPage = viewModel.currentPage,
                                autoCrop = settings.autoCrop,
                                theme = settings.theme,
                                onPageChanged = { page, total -> viewModel.onPageChanged(page, total) },
                                onTotalPages = { viewModel.updatePageCount(it) },
                                onChaptersFound = { viewModel.setChapters(it) },
                                opinions = opinions,
                                globalDictionaryWords = globalDictionaryWords,
                                onEmptyTap = { offset, width ->
                                    if (offset.x < width * 0.15f) {
                                        viewModel.previousPage()
                                        performThrottledHaptic()
                                    } else if (offset.x > width * 0.85f) {
                                        viewModel.nextPage()
                                        performThrottledHaptic()
                                    } else {
                                        viewModel.toggleBars()
                                    }
                                },
                                onTextSelectionAction = { action, text, id, startIndex -> 
                                    when(action) {
                                        "annotate", "create" -> viewModel.onTextSelected("annotate", text)
                                        "dictionary" -> {
                                            viewModel.saveDictionaryEntry(text, id, startIndex)
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(text)}"))
                                            context.startActivity(intent)
                                        }
                                        "edit" -> {
                                            val op = opinions.find { it.id == id }
                                            if(op != null) {
                                                editingOpinion = op
                                            }
                                        }
                                        "delete" -> {
                                            if(id != null) viewModel.deleteAnnotation(id)
                                        }
                                        "copy" -> {
                                            // Handled in overlay via clipboardManager
                                        }
                                    }
                                },
                                onNavigateToDictionaryOrigin = onNavigateToDictionaryOrigin,
                                onRequestPreviousPage = {
                                    viewModel.previousPage()
                                    performThrottledHaptic()
                                },
                                onRequestNextPage = {
                                    viewModel.nextPage()
                                    performThrottledHaptic()
                                }
                            )
                        }
                        ResourceType.MARKDOWN -> {
                            // Key on extractedText so MarkdownEditor reinitializes after save
                            val noteText = res.extractedText ?: ""
                            var showSavedFeedback by remember { mutableStateOf(false) }

                            key(noteText) {
                                MarkdownEditor(
                                    title = res.title ?: "Markdown Note",
                                    initialText = noteText,
                                    onBackClick = onBackClick,
                                    onTextChange = { currentEditorText = it },
                                    onTitleChange = { /* Rename logic if needed */ },
                                    onSave = { content ->
                                        viewModel.updateResourceText(content, editSelectedReferences)
                                        showSavedFeedback = true
                                    },
                                    initialPreviewMode = true,
                                    onShowReferenceSelector = { 
                                        referenceSelectorViewModel?.clearSelection()
                                        showEditReferenceSelector = true 
                                    },
                                    referenceToInsert = editReferenceToInsert,
                                    onReferenceInserted = { editReferenceToInsert = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Save feedback snackbar
                            if (showSavedFeedback) {
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(1500)
                                    showSavedFeedback = false
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 32.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = GrayMatterColors.Success.copy(alpha = 0.9f),
                                        tonalElevation = 8.dp
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Note saved",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            // Handled by LaunchedEffect - show loading while intent triggers
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = GrayMatterColors.Primary)
                            }
                        }
                    }
                }
            }
        }

        if (showEditReferenceSelector && referenceSelectorViewModel != null) {
            com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
                viewModel = referenceSelectorViewModel,
                onDismissRequest = { showEditReferenceSelector = false },
                onConfirm = { items ->
                    showEditReferenceSelector = false
                    editSelectedReferences = (editSelectedReferences + items).distinctBy { it.id }
                    
                    if (items.isNotEmpty()) {
                        val item = items.first()
                        val refText = when (item) {
                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> item.name
                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> item.title
                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> item.snippet
                        }
                        editReferenceToInsert = "[[$refText]]"
                    }
                }
            )
        }
        
        // ── Top Bar (Animated) ──
        AnimatedVisibility(
            visible = viewModel.showTopBar && (resource?.type != ResourceType.MARKDOWN),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                FileViewerTopBar(
                    title = resource?.title ?: "Document",
                    chapterName = viewModel.currentChapterName,
                    onBackClick = onBackClick,
                    onMenuAction = { action ->
                        when(action) {
                            "filter" -> { /* TODO: History Filter */ }
                            "export" -> { /* TODO: Export History */ }
                            "relatrix" -> onViewInGraph(resourceId)
                            "edit" -> { /* TODO: Item Edit */ }
                        }
                    }
                )
                
                val chapters = viewModel.chapters.collectAsState().value
                if (chapters.isNotEmpty()) {
                    val currentFlat = remember(chapters) {
                        fun flatten(list: List<ChapterOutline>): List<ChapterOutline> = 
                            list.flatMap { listOf(it) + flatten(it.children) }
                        flatten(chapters).sortedBy { it.targetPage }.distinctBy { it.targetPage }
                    }
                    val currentPg = viewModel.currentPage
                    val prevChapter = currentFlat.lastOrNull { it.targetPage < currentPg }
                    val nextChapter = currentFlat.firstOrNull { it.targetPage > currentPg }
                    
                    if (prevChapter != null || nextChapter != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (prevChapter != null) {
                                TextButton(onClick = { viewModel.jumpToPage(prevChapter.targetPage); performThrottledHaptic() }) {
                                    Text("Prev Ch. P. ${prevChapter.targetPage + 1}", color = Color.White, fontSize = 12.sp)
                                }
                            } else Spacer(modifier = Modifier.width(8.dp))
                            
                            if (nextChapter != null) {
                                TextButton(onClick = { viewModel.jumpToPage(nextChapter.targetPage); performThrottledHaptic() }) {
                                    Text("Next Ch. P. ${nextChapter.targetPage + 1}", color = Color.White, fontSize = 12.sp)
                                }
                            } else Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        }

        // ── Bottom Bar (Animated) — Expanded HUD ──
        AnimatedVisibility(
            visible = viewModel.showBottomBar && (resource?.type != ResourceType.MARKDOWN),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column {
                val isCurrentPageBookmarked = bookmarks.any { it.page == viewModel.currentPage }
                
                HudNavigationSlider(
                    currentPage = viewModel.currentPage,
                    totalPages = viewModel.totalPages,
                    isExpanded = true,
                    isBookmarked = isCurrentPageBookmarked,
                    chapters = viewModel.chapters.collectAsState().value,
                    onPageSlide = { viewModel.jumpToPage(it) },
                    onPreviousPage = {
                        viewModel.previousPage()
                        performThrottledHaptic()
                    },
                    onNextPage = {
                        viewModel.nextPage()
                        performThrottledHaptic()
                    },
                    onBookmarkToggle = {
                        if (!isCurrentPageBookmarked) {
                            viewModel.openBookmarkDialog()
                        }
                    },
                    isLeftHanded = settings.isLeftHanded
                )

                ReaderActionBar(
                    onSearchClick = { viewModel.toggleSearchPanel() },
                    onCustomiseClick = { viewModel.toggleSettingsSheet() },
                    onAddClick = { viewModel.toggleAddEntrySheet() },
                    onBookmarksClick = { viewModel.toggleBookmarksSheet() },
                    onChaptersClick = { viewModel.toggleChaptersSheet() }
                )
            }
        }

        // ── Persistent Idle Progress Strip — always visible when bars hidden ──
        if (!viewModel.showBottomBar && resource?.type == ResourceType.PDF) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                HudNavigationSlider(
                    currentPage = viewModel.currentPage,
                    totalPages = viewModel.totalPages,
                    isExpanded = false,
                    isBookmarked = false,
                    chapters = viewModel.chapters.collectAsState().value,
                    onPageSlide = {},
                    onPreviousPage = {},
                    onNextPage = {},
                    onBookmarkToggle = {},
                    isLeftHanded = settings.isLeftHanded
                )
            }
        }

        // ── Undo Snackbar ──
        if (viewModel.recentlyDeletedOpinionId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (viewModel.showBottomBar && resource?.type != ResourceType.MARKDOWN) 120.dp else 48.dp)
            ) {
                com.example.graymatter.android.ui.components.UndoSnackbar(
                    message = "Annotation deleted",
                    onUndo = { viewModel.undoDeleteOpinion() },
                    onDismissRequest = { viewModel.clearRecentlyDeletedOpinion() }
                )
            }
        }

        // ── Overlays ──
        if (viewModel.showSearchPanel) {
            SearchPanel(
                query = viewModel.searchQuery,
                results = searchResults,
                currentIndex = viewModel.currentSearchIndex,
                onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onNextResult = { viewModel.nextSearchResult() },
                onPreviousResult = { viewModel.previousSearchResult() },
                onResultClick = { viewModel.jumpToSearchResult(it) },
                onClose = { viewModel.toggleSearchPanel() }
            )
        }

        if (viewModel.showSettingsSheet) {
            DisplaySettingsSheet(
                settings = settings,
                onSettingsChanged = { viewModel.updateSettings { it } },
                onDismiss = { viewModel.toggleSettingsSheet() }
            )
        }

        if (viewModel.showBookmarksSheet) {
            BookmarkPanel(
                bookmarks = bookmarks,
                onBookmarkClick = { 
                    viewModel.jumpToPage(it.page)
                    viewModel.toggleBookmarksSheet()
                },
                onBookmarkDelete = { viewModel.removeBookmark(it.id) },
                onAddBookmark = { viewModel.openBookmarkDialog() },
                onDismiss = { viewModel.toggleBookmarksSheet() }
            )
        }

        if (viewModel.showBookmarkDialog) {
            BookmarkOpinionDialog(
                viewModel = referenceSelectorViewModel,
                onDismiss = { viewModel.closeBookmarkDialog() },
                onConfirm = { opinion, confidence, referenceLinks ->
                    viewModel.saveBookmarkAndOpinion(opinion, confidence, referenceLinks)
                }
            )
        }

        if (viewModel.showChaptersSheet) {
            ChaptersSheet(
                chapters = viewModel.chapters.collectAsState().value,
                onChapterClick = { viewModel.jumpToChapter(it) },
                onDismiss = { viewModel.toggleChaptersSheet() }
            )
        }

        if (viewModel.showAddEntrySheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleAddEntrySheet() },
                containerColor = GrayMatterColors.SurfaceDark
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Add Opinion", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    
                    ListItem(
                        headlineContent = { Text("General Opinion", color = Color.White) },
                        leadingContent = { Icon(Icons.Default.EditNote, null, tint = GrayMatterColors.Success) },
                        modifier = Modifier.clickable { 
                            viewModel.toggleAddEntrySheet()
                            viewModel.toggleCustomOpinionDialog()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    ListItem(
                        headlineContent = { Text("Template Entry", color = Color.White) },
                        leadingContent = { Icon(Icons.Default.DashboardCustomize, null, tint = GrayMatterColors.CustomizedAccent) },
                        modifier = Modifier.clickable { 
                            viewModel.toggleAddEntrySheet()
                            viewModel.toggleTemplateSelectionDialog()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (viewModel.showTemplateSelectionDialog) {
            val templates = viewModel.templates.collectAsState().value
            AlertDialog(
                onDismissRequest = { viewModel.toggleTemplateSelectionDialog() },
                containerColor = GrayMatterColors.SurfaceDark,
                title = { Text("Select Template", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (templates.isEmpty()) {
                            Text("No templates found.", color = GrayMatterColors.Neutral500)
                        } else {
                            templates.forEach { template ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GrayMatterColors.Neutral900)
                                        .clickable { 
                                            viewModel.selectTemplateForNewEntry(template)
                                            viewModel.toggleTemplateSelectionDialog()
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(template.name, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Divider(color = GrayMatterColors.Neutral800, modifier = Modifier.padding(vertical = 4.dp))
                        
                        TextButton(
                            onClick = { 
                                viewModel.toggleTemplateSelectionDialog()
                                showTemplateEditor = true 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = GrayMatterColors.CustomizedAccent)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Template", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.toggleTemplateSelectionDialog() }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        if (viewModel.selectedTemplateForNewEntry != null) {
            val template = viewModel.selectedTemplateForNewEntry!!
            var fieldValues by remember { mutableStateOf(template.headings.associateWith { "" }) }
            var confidence by remember { mutableFloatStateOf(0.0f) }
            var selectedReferences by remember { mutableStateOf(emptyList<com.example.graymatter.domain.ReferenceSelectorItem>()) }
            var showReferenceSelector by remember { mutableStateOf(false) }
            
            Dialog(onDismissRequest = { viewModel.selectTemplateForNewEntry(null) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GrayMatterColors.SurfaceDark)
                        .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New ${template.name}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = GrayMatterColors.TextPrimary
                            )
                            
                            TemplateSelector(
                                templates = viewModel.templates.collectAsState().value,
                                selectedTemplate = template,
                                onTemplateSelect = { viewModel.selectTemplateForNewEntry(it) },
                                onCreateTemplate = {
                                    viewModel.selectTemplateForNewEntry(null)
                                    showTemplateEditor = true
                                }
                            )
                        }
                        
                        Column(
                            modifier = Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
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
                                            referenceSelectorViewModel?.clearSelection()
                                            showReferenceSelector = true 
                                        }, 
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = GrayMatterColors.KnowledgeBlue, modifier = Modifier.size(20.dp))
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
                                            onValueChange = { newVal ->
                                                fieldValues = fieldValues.toMutableMap().apply { put(heading, newVal) }
                                            },
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary),
                                            modifier = Modifier.fillMaxWidth(),
                                            cursorBrush = SolidColor(GrayMatterColors.CustomizedAccent)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Column {
                            Text("Confidence: ${(confidence * 10).toInt()}/10", style = MaterialTheme.typography.labelMedium, color = GrayMatterColors.Neutral500)
                            Slider(
                                value = confidence, 
                                onValueChange = { confidence = it }, 
                                colors = SliderDefaults.colors(thumbColor = GrayMatterColors.CustomizedAccent, activeTrackColor = GrayMatterColors.CustomizedAccent)
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.selectTemplateForNewEntry(null) }) { Text("Cancel", color = GrayMatterColors.Neutral500) }
                            Button(
                                onClick = { 
                                    // Assemble template format as expected in ResourceDetailScreen / History
                                    val formatted = buildString {
                                        append("[TEMPLATE:${template.name}]\n")
                                        template.headings.forEach { heading ->
                                            append("### $heading\n")
                                            append(fieldValues[heading] ?: "")
                                            append("\n\n")
                                        }
                                    }.trim()
                                    viewModel.saveTemplateOpinion(formatted, (confidence * 100).toInt(), selectedReferences) 
                                    viewModel.selectTemplateForNewEntry(null)
                                },
                                enabled = fieldValues.values.any { it.isNotBlank() },
                                colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.CustomizedAccent, contentColor = Color.White)
                            ) { 
                                Text("Save") 
                            }
                        }
                    }
                }
            }

            if (showReferenceSelector && referenceSelectorViewModel != null) {
                com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
                    viewModel = referenceSelectorViewModel,
                    onDismissRequest = { showReferenceSelector = false },
                    onConfirm = { items ->
                        showReferenceSelector = false
                        selectedReferences = (selectedReferences + items).distinctBy { it.id }
                    }
                )
            }
        }

        if (viewModel.showCustomOpinionDialog) {
            var text by remember { mutableStateOf("") }
            var confidence by remember { mutableFloatStateOf(0.0f) }
            var selectedReferences by remember { mutableStateOf(emptyList<com.example.graymatter.domain.ReferenceSelectorItem>()) }
            var showReferenceSelector by remember { mutableStateOf(false) }
            
            Dialog(onDismissRequest = { viewModel.toggleCustomOpinionDialog() }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GrayMatterColors.SurfaceDark)
                        .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "New General Opinion", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                            color = GrayMatterColors.TextPrimary
                        )

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
                                        referenceSelectorViewModel?.clearSelection()
                                        showReferenceSelector = true 
                                    }, 
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = GrayMatterColors.KnowledgeBlue, modifier = Modifier.size(20.dp))
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
                        
                        BasicTextField(
                            value = text, 
                            onValueChange = { text = it }, 
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary), 
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp))
                                .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            cursorBrush = SolidColor(GrayMatterColors.Success),
                            decorationBox = { inner ->
                                if (text.isEmpty()) {
                                    Text("Type your reflection here...", color = GrayMatterColors.Neutral600)
                                }
                                inner()
                            }
                        )
        
                        Column {
                            Text(
                                "Confidence: ${(confidence * 10).toInt()}/10",
                                style = MaterialTheme.typography.labelMedium,
                                color = GrayMatterColors.Neutral500
                            )
                            Slider(
                                value = confidence,
                                onValueChange = { confidence = it },
                                colors = SliderDefaults.colors(
                                    thumbColor = GrayMatterColors.Success,
                                    activeTrackColor = GrayMatterColors.Success
                                )
                            )
                        }
        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.toggleCustomOpinionDialog() }) {
                                Text("Cancel", color = GrayMatterColors.Neutral500)
                            }
                            Button(
                                onClick = { 
                                    viewModel.saveGeneralOpinion(content = text, confidence = (confidence * 100).toInt(), referenceLinks = selectedReferences)
                                    viewModel.toggleCustomOpinionDialog() 
                                },
                                enabled = text.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GrayMatterColors.Success,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Save Opinion")
                            }
                        }
                    }
                }
            }

            if (showReferenceSelector && referenceSelectorViewModel != null) {
                com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
                    viewModel = referenceSelectorViewModel,
                    onDismissRequest = { showReferenceSelector = false },
                    onConfirm = { items ->
                        showReferenceSelector = false
                        selectedReferences = (selectedReferences + items).distinctBy { it.id }
                    }
                )
            }
        }

        if (viewModel.showSelectionAnnotationDialog && viewModel.selectedText != null) {
            SelectionAnnotationDialog(
                selectedText = viewModel.selectedText!!,
                viewModel = referenceSelectorViewModel,
                onDismiss = { viewModel.closeSelectionAnnotationDialog() },
                onConfirm = { opinion, score, selectedRefs ->
                    viewModel.saveAnnotation(opinion, score, selectedRefs)
                }
            )
        }

        if (editingOpinion != null) {
            val op = editingOpinion!!
            var quote = "Selected Text"
            var userText = op.text
            if (op.text.startsWith("> ")) {
                val parts = op.text.split("\n\n")
                if (parts.size >= 2) {
                    quote = parts[0].substring(2).trim()
                    userText = parts.drop(1).joinToString("\n\n")
                }
            }

            SelectionAnnotationDialog(
                selectedText = quote,
                initialOpinion = userText,
                initialConfidence = op.confidenceScore / 10f,
                viewModel = referenceSelectorViewModel,
                onDismiss = { editingOpinion = null },
                onConfirm = { updatedOpinion, score, selectedRefs ->
                    val fullOpinion = "> $quote\n\n$updatedOpinion"
                    viewModel.updateAnnotation(op.id, fullOpinion, score, selectedRefs)
                    editingOpinion = null
                }
            )
        }

        if (showTemplateEditor) {
            TemplateEditorDialog(
                template = CustomTemplate("", "", emptyList()),
                onDismiss = { showTemplateEditor = false },
                onSave = { template ->
                    viewModel.saveTemplate(template)
                    viewModel.selectTemplateForNewEntry(template)
                    showTemplateEditor = false
                }
            )
        }
    }
}

@Composable
fun FileViewerTopBar(
    title: String,
    chapterName: String?,
    onBackClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onMenuAction: (String) -> Unit = {}
) {
    Surface(
        color = HudDeepDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (chapterName != null) {
                    Text(
                        text = chapterName,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ReaderBottomBar and RepeatingIconButton removed — replaced by HudNavigationSlider

@Composable
fun ReaderActionBar(
    onSearchClick: () -> Unit,
    onCustomiseClick: () -> Unit,
    onAddClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onChaptersClick: () -> Unit
) {
    Surface(
        color = HudDeepDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .height(56.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionBarIcon(Icons.Default.Search, "Search", onSearchClick)
            ActionBarIcon(Icons.Default.Palette, "Customise", onCustomiseClick)
            ActionBarIcon(Icons.Default.Add, "Add Note", onAddClick)
            ActionBarIcon(Icons.Default.Menu, "TOC", onChaptersClick)
            ActionBarIcon(Icons.Default.Bookmarks, "Bookmarks", onBookmarksClick)
        }
    }
}

@Composable
fun ActionBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = Color.White)
        }
    }
}

@Composable
fun BookmarkOpinionDialog(
    viewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var confidence by remember { mutableFloatStateOf(0.0f) }
    var selectedReferences by remember { mutableStateOf(emptyList<com.example.graymatter.domain.ReferenceSelectorItem>()) }
    var showReferenceSelector by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GrayMatterColors.SurfaceDark)
                .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Add Bookmark Note", 
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                    color = GrayMatterColors.TextPrimary
                )

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
                
                BasicTextField(
                    value = text, 
                    onValueChange = { text = it }, 
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary), 
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp))
                        .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    cursorBrush = SolidColor(GrayMatterColors.Primary),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text("Type your reflection here...", color = GrayMatterColors.Neutral600)
                        }
                        inner()
                    }
                )

                Column {
                    Text(
                        "Confidence: ${(confidence * 10).toInt()}/10",
                        style = MaterialTheme.typography.labelMedium,
                        color = GrayMatterColors.Neutral500
                    )
                    Slider(
                        value = confidence,
                        onValueChange = { confidence = it },
                        colors = SliderDefaults.colors(
                            thumbColor = GrayMatterColors.Primary,
                            activeTrackColor = GrayMatterColors.Primary
                        )
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = GrayMatterColors.Neutral500)
                    }
                    Button(
                        onClick = { onConfirm(text, (confidence * 100).toInt(), selectedReferences) },
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrayMatterColors.Primary,
                            contentColor = GrayMatterColors.OnPrimary
                        )
                    ) {
                        Text("Save Bookmark")
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

@Composable
fun SelectionAnnotationDialog(
    selectedText: String,
    initialOpinion: String = "",
    initialConfidence: Float = 0.0f,
    viewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit
) {
    var opinion by remember { mutableStateOf(initialOpinion) }
    var confidence by remember { mutableFloatStateOf(initialConfidence) }
    var selectedReferences by remember { mutableStateOf(emptyList<com.example.graymatter.domain.ReferenceSelectorItem>()) }
    var showReferenceSelector by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GrayMatterColors.SurfaceDark)
                .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Annotate Selection",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary
                )
                
                // Show quote snippet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "\"$selectedText\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = GrayMatterColors.Neutral400,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
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

                BasicTextField(
                    value = opinion,
                    onValueChange = { opinion = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp))
                        .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    cursorBrush = SolidColor(GrayMatterColors.Primary),
                    decorationBox = { inner ->
                        if (opinion.isEmpty()) Text("Type your reflection...", color = GrayMatterColors.Neutral600)
                        inner()
                    }
                )

                Column {
                    Text(
                        "Confidence Score: ${(confidence * 10).toInt()}/10",
                        style = MaterialTheme.typography.labelMedium,
                        color = GrayMatterColors.Neutral500
                    )
                    Slider(
                        value = confidence,
                        onValueChange = { confidence = it },
                        colors = SliderDefaults.colors(
                            thumbColor = GrayMatterColors.Primary,
                            activeTrackColor = GrayMatterColors.Primary
                        )
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = GrayMatterColors.Neutral500)
                    }
                    Button(
                        onClick = { onConfirm(opinion, (confidence * 100).toInt(), selectedReferences) },
                        enabled = opinion.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrayMatterColors.Primary,
                            contentColor = GrayMatterColors.OnPrimary
                        )
                    ) {
                        Text("Save Annotation")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersSheet(
    chapters: List<com.example.graymatter.domain.ChapterOutline>,
    onChapterClick: (com.example.graymatter.domain.ChapterOutline) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GrayMatterColors.SurfaceDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GrayMatterColors.Neutral700) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Table of Contents",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (chapters.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No chapters found in this document", color = GrayMatterColors.Neutral500)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                    items(chapters) { chapter ->
                        ChapterItem(chapter = chapter, onChapterClick = onChapterClick, level = 0)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: com.example.graymatter.domain.ChapterOutline,
    onChapterClick: (com.example.graymatter.domain.ChapterOutline) -> Unit,
    level: Int
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChapterClick(chapter) }
                .padding(vertical = 12.dp, horizontal = (level * 16).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Normal
                ),
                color = GrayMatterColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "P. ${chapter.targetPage + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = GrayMatterColors.Primary
            )
        }
        
        chapter.children.forEach { child ->
            ChapterItem(chapter = child, onChapterClick = onChapterClick, level = level + 1)
        }
    }
}

// Mock function to satisfy the code, this should be in a theme file ideally
@Composable
fun getThemeColors(theme: String): ReadingThemeColors {
    return when (theme) {
        "daylight" -> ReadingThemeColors(background = Color(0xFFFFFFFF), text = Color(0xFF000000))
        "paper_classic" -> ReadingThemeColors(background = Color(0xFFF4ECD8), text = Color(0xFF333333))
        "book_linen" -> ReadingThemeColors(background = Color(0xFFFAF9F6), text = Color(0xFF3E3E3E))
        "night" -> ReadingThemeColors(background = Color(0xFF121212), text = Color(0xFFB0B0B0))
        "amoled_black" -> ReadingThemeColors(background = Color(0xFF000000), text = Color(0xFFFFFFFF))
        "twilight" -> ReadingThemeColors(background = Color(0xFF1A1B26), text = Color(0xFFC0CAF5))
        "console" -> ReadingThemeColors(background = Color(0xFF0D0D0D), text = Color(0xFF4AF626))
        "sepia" -> ReadingThemeColors(background = Color(0xFF704214), text = Color(0xFFEBD5B3))
        "vintage_parchment" -> ReadingThemeColors(background = Color(0xFFF1E5AC), text = Color(0xFF4E342E))
        else -> ReadingThemeColors(background = Color.White, text = Color.Black)
    }
}

data class ReadingThemeColors(val background: Color, val text: Color)
