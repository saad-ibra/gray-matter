package com.example.graymatter.android.ui.fileviewer

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.graymatter.domain.ChapterOutline
import com.example.graymatter.domain.ResourceType
import androidx.core.net.toUri
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type

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
    onBackClick: () -> Unit
) {
    val resource by viewModel.resource.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val opinions by viewModel.opinions.collectAsState()
    val context = LocalContext.current
    
    var editingOpinion by remember { mutableStateOf<com.example.graymatter.domain.Opinion?>(null) }

    LaunchedEffect(resourceId, initialPage) {
        viewModel.loadResource(resourceId, initialPage)
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
                            viewModel.previousPage()
                            true
                        }
                        Key.VolumeDown -> {
                            viewModel.nextPage()
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
            val isPaged = res.type == ResourceType.PDF || res.type == ResourceType.IMAGE

            Box(modifier = Modifier.fillMaxSize()) {
                // ── The Viewer Content ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Listen for taps in the center to toggle bars
                            detectTapGestures(onTap = { viewModel.toggleBars() })
                        }
                ) {
                    when (res.type) {
                        ResourceType.PDF -> {
                            AnimatedContent(
                                targetState = viewModel.currentPage,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        // Slide left for Next Page
                                        slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                                    } else {
                                        // Slide right for Previous Page
                                        slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                                    }
                                },
                                label = "PageFlipAnimation"
                            ) { targetPage ->
                                PdfViewerContent(
                                    filePath = res.filePath ?: "",
                                    currentPage = targetPage,
                                    autoCrop = settings.autoCrop,
                                    theme = settings.theme,
                                    onPageChanged = { page, total -> viewModel.onPageChanged(page, total) },
                                    onTotalPages = { viewModel.updatePageCount(it) },
                                    onChaptersFound = { viewModel.setChapters(it) },
                                    opinions = opinions,
                                    onTextSelectionAction = { action, text, id -> 
                                        when(action) {
                                            "annotate", "create" -> viewModel.onTextSelected("annotate", text)
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
                                    }
                                )
                            }
                        }
                        ResourceType.MARKDOWN -> TextViewerContent(
                            filePath = res.filePath ?: "",
                            extractedText = res.extractedText,
                            settings = settings,
                            context = context,
                            onChaptersParsed = { viewModel.setChapters(it) },
                            onTextSelected = { text, x, y -> 
                                if (text.isNotBlank()) viewModel.onTextSelected("annotate", text, x, y)
                            },
                            searchQuery = viewModel.searchQuery,
                            searchMatchIndex = viewModel.currentSearchIndex,
                            searchResults = searchResults,
                            onScrollToOffset = null
                        )
                        else -> {
                            UnsupportedFileView(
                                resourceType = res.type.name,
                                onBackClick = onBackClick,
                                onOpenWithClick = {
                                    res.filePath?.let { path ->
                                        try {
                                            val uri = if (path.startsWith("content://") || path.startsWith("http")) {
                                                path.toUri()
                                            } else {
                                                androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    java.io.File(path)
                                                )
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "*/*")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Open with"))
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context, "Cannot open file", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // ── Navigation Taps Overlays (Sides Only) ──
                if (isPaged) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Previous Page Zone (15% width)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.15f)
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { viewModel.previousPage() })
                                }
                        )
                        
                        // Center Area (70% width) - empty so touches pass through to content/toggle
                        Box(modifier = Modifier.fillMaxHeight().weight(0.70f))
                        
                        // Next Page Zone (15% width)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.15f)
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { viewModel.nextPage() })
                                }
                        )
                    }
                }
            }
        }

        // ── Top Bar (Animated) ──
        AnimatedVisibility(
            visible = viewModel.showTopBar,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            FileViewerTopBar(
                title = resource?.title ?: "Document",
                chapterName = viewModel.currentChapterName,
                onBackClick = onBackClick
            )
        }

        // ── Bottom Bar (Animated) ──
        AnimatedVisibility(
            visible = viewModel.showBottomBar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column {
                val isCurrentPageBookmarked = bookmarks.any { it.page == viewModel.currentPage }
                
                ReaderBottomBar(
                    currentPage = viewModel.currentPage,
                    totalPages = viewModel.totalPages,
                    isBookmarked = isCurrentPageBookmarked,
                    chapters = viewModel.chapters.collectAsState().value,
                    onPageSlide = { viewModel.jumpToPage(it) },
                    onPreviousPage = { viewModel.previousPage() },
                    onNextPage = { viewModel.nextPage() },
                    onBookmarkToggle = { 
                        if (!isCurrentPageBookmarked) {
                            viewModel.openBookmarkDialog() 
                        }
                    }
                )

                ReaderActionBar(
                    onSearchClick = { viewModel.toggleSearchPanel() },
                    onCustomiseClick = { viewModel.toggleSettingsSheet() },
                    onBookmarksClick = { viewModel.toggleBookmarksSheet() },
                    onChaptersClick = { viewModel.toggleChaptersSheet() }
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
                onDismiss = { viewModel.closeBookmarkDialog() },
                onConfirm = { opinion, confidence ->
                    viewModel.saveBookmarkAndOpinion(opinion, confidence)
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

        if (viewModel.showSelectionAnnotationDialog && viewModel.selectedText != null) {
            SelectionAnnotationDialog(
                selectedText = viewModel.selectedText!!,
                onDismiss = { viewModel.closeSelectionAnnotationDialog() },
                onConfirm = { opinion, score ->
                    viewModel.saveAnnotation(opinion, score)
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
                onDismiss = { editingOpinion = null },
                onConfirm = { updatedOpinion, score ->
                    val fullOpinion = "> $quote\n\n$updatedOpinion"
                    viewModel.updateAnnotation(op.id, fullOpinion, score)
                    editingOpinion = null
                }
            )
        }
    }
}

@Composable
fun UnsupportedFileView(
    resourceType: String,
    onBackClick: () -> Unit,
    onOpenWithClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        // Gradient background effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GrayMatterColors.Error.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Elegant Icon Container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(GrayMatterColors.SurfaceDark)
                    .border(1.dp, GrayMatterColors.Neutral800.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Subtle inner glow
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Icon(
                    imageVector = Icons.Outlined.FileOpen,
                    contentDescription = null,
                    tint = GrayMatterColors.Error.copy(alpha = 0.8f),
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Format Not Supported",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = GrayMatterColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Gray Matter currently focuses on PDF and Markdown for the best experience. Support for $resourceType is not available yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = GrayMatterColors.Neutral500,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onOpenWithClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrayMatterColors.SurfaceDark,
                    contentColor = GrayMatterColors.TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GrayMatterColors.Neutral700),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            ) {
                Text(
                    "Open with...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrayMatterColors.Primary,
                    contentColor = GrayMatterColors.OnPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "Back to Library",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun FileViewerTopBar(
    title: String,
    chapterName: String?,
    onBackClick: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
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

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    isBookmarked: Boolean,
    chapters: List<ChapterOutline> = emptyList(),
    onPageSlide: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    var slidingValue by remember(currentPage) { mutableFloatStateOf(if (totalPages > 1) currentPage.toFloat() / (totalPages - 1) else 0f) }
    
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousPage, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
                }
                
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Slider(
                        value = slidingValue,
                        onValueChange = { slidingValue = it },
                        onValueChangeFinished = {
                            val newPage = (slidingValue * (totalPages - 1)).toInt().coerceIn(0, totalPages - 1)
                            onPageSlide(newPage)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    
                    // Chapter markers
                    if (totalPages > 1 && chapters.isNotEmpty()) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp) // Align with slider track
                        ) {
                            val trackWidth = maxWidth
                            chapters.forEach { chapter ->
                                val position = chapter.targetPage.toFloat() / (totalPages - 1)
                                Box(
                                    modifier = Modifier
                                        .offset(x = trackWidth * position)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onNextPage, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onBookmarkToggle) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        null,
                        tint = if (isBookmarked) Color(0xFFFFD700) else Color.White
                    )
                }
            }
            
            val displayPage = if (totalPages > 1) (slidingValue * (totalPages - 1)).toInt() + 1 else 1
            Text(
                "$displayPage / $totalPages",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ReaderActionBar(
    onSearchClick: () -> Unit,
    onCustomiseClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onChaptersClick: () -> Unit
) {
    val showTOC by remember { mutableStateOf(true) } // Could be conditional
    
    Surface(
        color = Color.Black.copy(alpha = 0.92f),
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
fun BookmarkOpinionDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    var confidence by remember { mutableFloatStateOf(0.7f) }
    
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
                        onClick = { onConfirm(text, (confidence * 100).toInt()) },
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
}

@Composable
fun SelectionAnnotationDialog(
    selectedText: String,
    initialOpinion: String = "",
    initialConfidence: Float = 0.7f,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var opinion by remember { mutableStateOf(initialOpinion) }
    var confidence by remember { mutableFloatStateOf(initialConfidence) }

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
                        onClick = { onConfirm(opinion, (confidence * 100).toInt()) },
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
