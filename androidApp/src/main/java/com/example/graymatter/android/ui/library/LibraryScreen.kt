package com.example.graymatter.android.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
    @Suppress("UNUSED_PARAMETER") onRenameTopic: (String, String) -> Unit,
    onUpdateOrder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTopics by remember { mutableStateOf<Set<String>>(emptySet()) }
    
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

    val baseTopics = if (searchQuery.isBlank()) {
        topics
    } else {
        topics.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    // Maintain a local copy of topics for fluid rearrangement during drag
    val filteredTopicsState = remember(baseTopics) { mutableStateOf(baseTopics) }
    val filteredTopics = filteredTopicsState.value

    // Snapshot refs for parameters to ensure suspended loops see latest values
    val currentOnUpdateOrder by rememberUpdatedState(onUpdateOrder)
    val currentOnDeleteTopics by rememberUpdatedState(onDeleteTopics)
    val currentBaseTopics by rememberUpdatedState(baseTopics)

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

            val gridState = rememberLazyGridState()
            
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    // Fix: Disable grid scrolling during drag to prevent gesture interruption
                    userScrollEnabled = draggedTopicId.value == null,
                    contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { gridPositionInRoot.value = it.positionInRoot() }
                        .pointerInput(selectionMode) {
                            if (selectionMode) return@pointerInput
                            
                            // Fix: Loop to keep detector alive across multiple gestures (Fixes "One-and-Done")
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
                                            val relativeTouchPoint = touchPointInRoot.value - gridPositionInRoot.value
                                            val items = gridState.layoutInfo.visibleItemsInfo
                                            
                                            val target = items.find { item ->
                                                Rect(
                                                    item.offset.x.toFloat(), 
                                                    item.offset.y.toFloat(), 
                                                    (item.offset.x + item.size.width).toFloat(), 
                                                    (item.offset.y + item.size.height).toFloat()
                                                ).contains(relativeTouchPoint)
                                            }

                                            if (target != null && target.key != currentDraggedId && target.key != lastSwappedId.value) {
                                                val currentList = filteredTopicsState.value
                                                val fromIndex = currentList.indexOfFirst { it.id == currentDraggedId }
                                                val toIndex = currentList.indexOfFirst { it.id == target.key }
                                                
                                                if (fromIndex != -1 && toIndex != -1) {
                                                    val newList = currentList.toMutableList()
                                                    val itemToMove = newList.removeAt(fromIndex)
                                                    newList.add(toIndex, itemToMove)
                                                    filteredTopicsState.value = newList
                                                    lastSwappedId.value = target.key as? String
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
                                                currentOnDeleteTopics(listOf(draggedId))
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
                                .animateItemPlacement()
                                .graphicsLayer {
                                    // Hide the source item while its replica is being dragged
                                    alpha = if (draggedTopicId.value == topic.id) 0f else 1f
                                }
                        ) {
                            TopicCard(
                                topic = topic,
                                romanNumeral = toRomanNumeral(filteredTopics.indexOf(topic) + 1),
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
                                }
                            )
                        }
                    }
                }
            }

            if (selectionMode) {
                SelectionActionBar(
                    count = selectedTopics.size,
                    onDelete = {
                        onDeleteTopics(selectedTopics.toList())
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
                val density = LocalDensity.current
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
                        romanNumeral = toRomanNumeral(filteredTopics.indexOf(topic) + 1),
                        isSelected = false,
                        onClick = {},
                        enabled = false // Disable hits on replica to prevent pointer interference
                    )
                }
            }
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GrayMatterColors.BackgroundDark.copy(alpha = 0.4f))
                    .border(1.dp, GrayMatterColors.Neutral700.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = romanNumeral,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    color = GrayMatterColors.TextPrimary
                )
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

private fun toRomanNumeral(num: Int): String {
    val romanNumerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    return if (num in 1..10) romanNumerals[num - 1] else num.toString()
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
