package com.example.graymatter.android.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.theme.PlayfairDisplayFontFamily
import com.example.graymatter.domain.Topic
import kotlinx.coroutines.delay
import java.util.TreeMap
import kotlin.math.roundToInt

/**
 * Library Screen matching the "Topics & Synthesis" design mockup.
 * Shows topics in a 2-column grid with Roman numeral icons.
 * Features: Long-press to rearrange, Drag to trash to delete, Multi-select.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    topics: List<Topic>,
    onTopicClick: (Topic) -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToHome: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onCreateClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToGraph: () -> Unit,
    onDeleteTopics: (List<String>) -> Unit,
    onUndoDeleteTopics: (List<String>) -> Unit,
    onRenameTopic: (String, String) -> Unit,
    onExportTopic: (Topic) -> Unit,
    onViewTopicInRelatrix: (String) -> Unit,
    onUpdateOrder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTopics by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    var deletedTopicsInfo by remember { mutableStateOf<List<String>?>(null) }
    var renamingTopic by remember { mutableStateOf<Topic?>(null) }
    
    val selectionMode = selectedTopics.isNotEmpty()
    val haptic = LocalHapticFeedback.current

    // Core Drag State - Using State objects for stable access in suspended blocks
    val draggedTopicId = remember { mutableStateOf<String?>(null) }
    val touchPointInRoot = remember { mutableStateOf(Offset.Zero) }
    val touchAnchorOffset = remember { mutableStateOf(Offset.Zero) }
    val draggedItemSize = remember { mutableStateOf(Size.Zero) }
    val isOverTrash = remember { mutableStateOf(false) }
    val trashZoneBoundsInRoot = remember { mutableStateOf(Rect.Zero) }
    val gridPositionInRoot = remember { mutableStateOf(Offset.Zero) }
    val screenPositionInRoot = remember { mutableStateOf(Offset.Zero) }
    val lastSwappedId = remember { mutableStateOf<String?>(null) }
    val lastSwapTimeMs = remember { mutableStateOf(0L) }
    val gridViewportHeight = remember { mutableStateOf(0f) }

    val baseTopics = if (searchQuery.isBlank()) {
        topics
    } else {
        topics.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    // Maintain a local copy of topics for fluid rearrangement during drag
    val filteredTopicsState = remember { mutableStateOf(baseTopics) }
    val filteredTopics = filteredTopicsState.value

    // Pre-compute index map to avoid indexOf during recomposition
    val topicIndexMap = remember(filteredTopics) {
        filteredTopics.withIndex().associate { (index, topic) -> topic.id to index }
    }

    // Sync with parent data when not actively dragging (preserves animation state)
    LaunchedEffect(baseTopics) {
        if (draggedTopicId.value == null) {
            filteredTopicsState.value = baseTopics
        }
    }

    // Snapshot refs for parameters to ensure suspended loops see latest values
    val currentOnUpdateOrder by rememberUpdatedState(onUpdateOrder)
    val currentOnDeleteTopics by rememberUpdatedState(onDeleteTopics)
    val currentBaseTopics by rememberUpdatedState(baseTopics)

    // Snappy spring so displaced items settle before the next swap fires
    val dragAnimationSpec = remember {
        spring<IntOffset>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    val density = LocalDensity.current
    val autoScrollThresholdPx = remember { with(density) { 80.dp.toPx() } }
    val autoScrollMaxSpeedPx  = remember { with(density) { 1200.dp.toPx() } } // px/sec at the very edge

    val gridState = rememberLazyGridState()

    // ── Auto-scroll engine: runs ~60 fps while dragging near top/bottom edges ──
    LaunchedEffect(draggedTopicId.value) {
        if (draggedTopicId.value == null) return@LaunchedEffect

        while (draggedTopicId.value != null) {
            val vpHeight = gridViewportHeight.value
            if (vpHeight <= 0f) { delay(16L); continue }

            val touchY  = touchPointInRoot.value.y
            val gridTop = gridPositionInRoot.value.y
            val relY    = touchY - gridTop  // touch position inside the grid viewport

            val scrollPx = when {
                // Near top edge → scroll UP (negative)
                relY < autoScrollThresholdPx -> {
                    val proximity = (1f - (relY / autoScrollThresholdPx).coerceIn(0f, 1f))
                    -(autoScrollMaxSpeedPx * proximity * proximity) / 60f
                }
                // Near bottom edge → scroll DOWN (positive)
                relY > vpHeight - autoScrollThresholdPx -> {
                    val proximity = (1f - ((vpHeight - relY) / autoScrollThresholdPx).coerceIn(0f, 1f))
                    (autoScrollMaxSpeedPx * proximity * proximity) / 60f
                }
                else -> 0f
            }

            if (scrollPx != 0f) {
                gridState.scrollBy(scrollPx)
            }
            delay(16L)
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(GrayMatterColors.BackgroundDark)
        .onGloballyPositioned { screenPositionInRoot.value = it.positionInRoot() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // gridState is declared above so the auto-scroll LaunchedEffect can access it
            
            Box(modifier = Modifier.weight(1f)) {
                if (filteredTopics.isEmpty() && searchQuery.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = GrayMatterColors.Neutral600,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No resources yet. Start by adding a new entry.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GrayMatterColors.Neutral500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateToHome,
                            colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Primary)
                        ) {
                            Text("Go Home", color = Color.Black)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        // Disable grid scrolling during drag to prevent gesture interruption
                        userScrollEnabled = draggedTopicId.value == null,
                        contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coords ->
                                gridPositionInRoot.value = coords.positionInRoot()
                                gridViewportHeight.value = coords.size.height.toFloat()
                            }
                            .pointerInput(selectionMode) {
                                if (selectionMode) return@pointerInput
                                
                                // Loop to keep detector alive across multiple gestures
                                while (true) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            val items = gridState.layoutInfo.visibleItemsInfo
                                            val item = items.find { 
                                                Rect(
                                                    it.offset.x.toFloat(), 
                                                    it.offset.y.toFloat(), 
                                                    (it.offset.x + it.size.width).toFloat(), 
                                                    (it.offset.y + it.size.height).toFloat()
                                                ).contains(offset)
                                            }
                                            if (item != null) {
                                                val topic = filteredTopicsState.value.getOrNull(item.index)
                                                if (topic != null) {
                                                    draggedTopicId.value = topic.id
                                                    val rootTouchStart = gridPositionInRoot.value + offset
                                                    touchPointInRoot.value = rootTouchStart
                                                    
                                                    val itemRootTopLeft = gridPositionInRoot.value + Offset(item.offset.x.toFloat(), item.offset.y.toFloat())
                                                    touchAnchorOffset.value = rootTouchStart - itemRootTopLeft
                                                    draggedItemSize.value = Size(item.size.width.toFloat(), item.size.height.toFloat())
                                                    lastSwappedId.value = null
                                                    lastSwapTimeMs.value = 0L
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            val currentDraggedId = draggedTopicId.value ?: return@detectDragGesturesAfterLongPress
                                            change.consume()
                                            touchPointInRoot.value += dragAmount
                                            
                                            // 1. Check intersection with trash in root space
                                            isOverTrash.value = trashZoneBoundsInRoot.value.contains(touchPointInRoot.value)
                                            
                                            if (!isOverTrash.value) {
                                                // 2. Search for swap target relative to grid
                                                // Use center-point of dragged item for more stable swap detection
                                                val dragCenterInRoot = touchPointInRoot.value - touchAnchorOffset.value + Offset(
                                                    draggedItemSize.value.width / 2f,
                                                    draggedItemSize.value.height / 2f
                                                )
                                                val relativeCenterPoint = dragCenterInRoot - gridPositionInRoot.value
                                                val items = gridState.layoutInfo.visibleItemsInfo
                                                
                                                // Use a 60% inner threshold to create a dead-zone and prevent rapid flip-flop swaps
                                                val target = items.find { item ->
                                                    val insetX = item.size.width * 0.2f
                                                    val insetY = item.size.height * 0.2f
                                                    Rect(
                                                        item.offset.x.toFloat() + insetX,
                                                        item.offset.y.toFloat() + insetY,
                                                        (item.offset.x + item.size.width).toFloat() - insetX,
                                                        (item.offset.y + item.size.height).toFloat() - insetY
                                                    ).contains(relativeCenterPoint)
                                                }

                                                val now = System.nanoTime() / 1_000_000L
                                                val swapCooldownMs = 200L

                                                if (target != null && target.key != currentDraggedId && target.key != lastSwappedId.value
                                                    && (now - lastSwapTimeMs.value) >= swapCooldownMs
                                                ) {
                                                    val currentList = filteredTopicsState.value
                                                    val fromIndex = currentList.indexOfFirst { it.id == currentDraggedId }
                                                    val toIndex = currentList.indexOfFirst { it.id == target.key }
                                                    
                                                    if (fromIndex != -1 && toIndex != -1) {
                                                        val newList = currentList.toMutableList()
                                                        val itemToMove = newList.removeAt(fromIndex)
                                                        newList.add(toIndex, itemToMove)
                                                        filteredTopicsState.value = newList
                                                        lastSwappedId.value = target.key as? String
                                                        lastSwapTimeMs.value = now
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                } else if (target == null) {
                                                    lastSwappedId.value = null
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val draggedId = draggedTopicId.value
                                            if (draggedId != null) {
                                                if (isOverTrash.value) {
                                                    val ids = listOf(draggedId)
                                                    currentOnDeleteTopics(ids)
                                                    deletedTopicsInfo = ids
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } else {
                                                    currentOnUpdateOrder(filteredTopicsState.value.map { it.id })
                                                }
                                            }
                                            draggedTopicId.value = null
                                            isOverTrash.value = false
                                            lastSwappedId.value = null
                                        },
                                        onDragCancel = {
                                            draggedTopicId.value = null
                                            isOverTrash.value = false
                                            lastSwappedId.value = null
                                            filteredTopicsState.value = currentBaseTopics
                                        }
                                    )
                                }
                            }
                    ) {
                        items(
                            items = filteredTopics,
                            key = { it.id }
                        ) { topic ->
                            Box(
                                modifier = Modifier
                                    .animateItemPlacement(dragAnimationSpec)
                                    .graphicsLayer {
                                        // Hide the source item while its replica is being dragged
                                        alpha = if (draggedTopicId.value == topic.id) 0f else 1f
                                    }
                            ) {
                                TopicCard(
                                    topic = topic,
                                    romanNumeral = toRomanNumeral((topicIndexMap[topic.id] ?: 0) + 1),
                                    isSelected = selectedTopics.contains(topic.id),
                                    onClick = {
                                        if (selectionMode) {
                                            selectedTopics = if (selectedTopics.contains(topic.id)) {
                                                selectedTopics - topic.id
                                            } else {
                                                selectedTopics + topic.id
                                            }
                                        } else if (draggedTopicId.value == null) {
                                            onTopicClick(topic)
                                        }
                                    },
                                    onDelete = {
                                        val ids = listOf(topic.id)
                                        currentOnDeleteTopics(ids)
                                        deletedTopicsInfo = ids
                                    },
                                    onRename = { renamingTopic = topic },
                                    onExport = { onExportTopic(topic) },
                                    onViewInRelatrix = { onViewTopicInRelatrix(topic.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (selectionMode) {
                SelectionActionBar(
                    count = selectedTopics.size,
                    onDelete = {
                        val ids = selectedTopics.toList()
                        currentOnDeleteTopics(ids)
                        deletedTopicsInfo = ids
                        selectedTopics = emptySet()
                    },
                    onClear = { selectedTopics = emptySet() }
                )
            }
        }

        // Floating Trash Zone
        AnimatedVisibility(
            visible = draggedTopicId.value != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .zIndex(10f)
        ) {
            val overTrashValue = isOverTrash.value
            val scale by animateFloatAsState(if (overTrashValue) 1.2f else 1f, label = "trashScale")
            val containerColor by animateColorAsState(
                targetValue = if (overTrashValue) GrayMatterColors.Error else GrayMatterColors.SurfaceDark,
                label = "trashColor"
            )
            
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(containerColor)
                    .border(2.dp, GrayMatterColors.Error.copy(alpha = 0.5f), CircleShape)
                    .onGloballyPositioned { coords ->
                        trashZoneBoundsInRoot.value = coords.boundsInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Drop to Delete",
                    tint = if (overTrashValue) Color.White else GrayMatterColors.Error,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Dragged Item Replica (anchored to root coordinates for jitter-free movement)
        if (draggedTopicId.value != null) {
            val topic = filteredTopics.find { it.id == draggedTopicId.value }
            if (topic != null) {
                val localOffset = touchPointInRoot.value - screenPositionInRoot.value - touchAnchorOffset.value
                
                Box(
                    modifier = Modifier
                        .offset { 
                            IntOffset(
                                localOffset.x.roundToInt(),
                                localOffset.y.roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { draggedItemSize.value.width.toDp() },
                            height = with(density) { draggedItemSize.value.height.toDp() }
                        )
                        .scale(1.05f)
                        .alpha(0.85f)
                        .zIndex(20f)
                ) {
                    TopicCard(
                        topic = topic,
                        romanNumeral = toRomanNumeral((topicIndexMap[topic.id] ?: 0) + 1),
                        isSelected = false,
                        onClick = {},
                        onDelete = {},
                        onRename = {},
                        onExport = {},
                        onViewInRelatrix = {},
                        enabled = false // Disable hits on replica to prevent pointer interference
                    )
                }
            }
        }


        if (deletedTopicsInfo != null) {
            com.example.graymatter.android.ui.components.UndoSnackbar(
                message = "${deletedTopicsInfo!!.size} topic(s) deleted",
                onUndo = {
                    onUndoDeleteTopics(deletedTopicsInfo!!)
                    deletedTopicsInfo = null
                },
                onDismissRequest = {
                    deletedTopicsInfo = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .imePadding()
            )
        }

        if (renamingTopic != null) {
            com.example.graymatter.android.ui.components.RenameTopicDialog(
                initialName = renamingTopic!!.name,
                onDismiss = { renamingTopic = null },
                onConfirm = { newName ->
                    onRenameTopic(renamingTopic!!.id, newName)
                    renamingTopic = null
                }
            )
        }
    }
    
    // Safety cleanup
    DisposableEffect(Unit) {
        onDispose {
            draggedTopicId.value = null
            isOverTrash.value = false
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GrayMatterColors.Neutral500,
                modifier = Modifier.size(22.dp)
            )
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = GrayMatterColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(GrayMatterColors.TextPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search Library...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GrayMatterColors.Neutral600
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    romanNumeral: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onViewInRelatrix: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) GrayMatterColors.Primary.copy(alpha = 0.2f) else GrayMatterColors.SurfaceDark)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GrayMatterColors.Primary else GrayMatterColors.Neutral800,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Roman Numeral Container - Variable size curved rectangle
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GrayMatterColors.BackgroundDark.copy(alpha = 0.4f))
                        .border(1.dp, GrayMatterColors.Neutral700.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = romanNumeral,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlayfairDisplayFontFamily,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        ),
                        color = GrayMatterColors.TextPrimary
                    )
                }
                
                if (enabled) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp).offset(x = 8.dp, y = (-8).dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = GrayMatterColors.Neutral500)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(GrayMatterColors.SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Topic", color = GrayMatterColors.TextPrimary) },
                                onClick = {
                                    showMenu = false
                                    onRename()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = GrayMatterColors.Primary) }
                            )

                            DropdownMenuItem(
                                text = { Text("Export Topic", color = GrayMatterColors.TextPrimary) },
                                onClick = {
                                    showMenu = false
                                    onExport()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null, tint = GrayMatterColors.Primary) }
                            )

                            DropdownMenuItem(
                                text = { Text("View in Relatrix", color = GrayMatterColors.TextPrimary) },
                                onClick = {
                                    showMenu = false
                                    onViewInRelatrix()
                                },
                                leadingIcon = { Icon(Icons.Default.Hub, null, tint = Color.White) }
                            )

                            Divider(color = GrayMatterColors.Neutral800, modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("Delete", color = GrayMatterColors.Error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = GrayMatterColors.Error) }
                            )
                        }
                    }
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = GrayMatterColors.TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${topic.resourceCount} resources",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = GrayMatterColors.Neutral500
                )
            }
            
            Text(
                text = "Updated ${formatTimeAgo(topic.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = GrayMatterColors.Neutral600
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(16.dp),
        color = GrayMatterColors.SurfaceDark,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, null, tint = GrayMatterColors.TextPrimary)
                }
                Text("$count selected", style = MaterialTheme.typography.titleSmall, color = GrayMatterColors.TextPrimary)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = GrayMatterColors.Error)
                }
            }
        }
    }
}

private fun toRomanNumeral(number: Int): String {
    if (number < 1) return number.toString()
    val map = TreeMap<Int, String>(Comparator.reverseOrder())
    map[1000] = "M"
    map[900] = "CM"
    map[500] = "D"
    map[400] = "CD"
    map[100] = "C"
    map[90] = "XC"
    map[50] = "L"
    map[40] = "XL"
    map[10] = "X"
    map[9] = "IX"
    map[5] = "V"
    map[4] = "IV"
    map[1] = "I"
    
    var n = number
    val sb = StringBuilder()
    for ((value, symbol) in map) {
        while (n >= value) {
            sb.append(symbol)
            n -= value
        }
    }
    return sb.toString()
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val hours = diff / (1000 * 60 * 60)
    val days = hours / 24
    
    return when {
        hours < 1 -> "just now"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        else -> "${days / 30}mo ago"
    }
}
