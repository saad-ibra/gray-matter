package com.example.graymatter.android.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
    onNavigateToHome: () -> Unit,
    onCreateClick: () -> Unit,
    onNavigateToGraph: () -> Unit,
    onDeleteTopics: (List<String>) -> Unit,
    onRenameTopic: (String, String) -> Unit,
    onUpdateOrder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTopics by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    val selectionMode = selectedTopics.isNotEmpty()
    val haptic = LocalHapticFeedback.current

    // Drag and Drop State
    var draggedTopicId by remember { mutableStateOf<String?>(null) }
    var touchPointInRoot by remember { mutableStateOf(Offset.Zero) }
    var touchAnchorOffset by remember { mutableStateOf(Offset.Zero) }
    var draggedItemSize by remember { mutableStateOf(Size.Zero) }
    
    var gridPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var screenPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    val itemBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    var trashZoneBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var isOverTrash by remember { mutableStateOf(false) }

    val baseTopics = if (searchQuery.isBlank()) {
        topics
    } else {
        topics.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    // Maintain a local copy of topics for fluid rearrangement during drag
    var filteredTopics by remember(baseTopics) { mutableStateOf(baseTopics) }

    Box(modifier = modifier
        .fillMaxSize()
        .background(GrayMatterColors.BackgroundDark)
        .onGloballyPositioned { screenPositionInRoot = it.positionInRoot() }
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
                    contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { gridPositionInRoot = it.positionInRoot() }
                        .pointerInput(selectionMode, filteredTopics) {
                            if (selectionMode) return@pointerInput
                            
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val rootOffset = gridPositionInRoot + offset
                                    val hit = itemBoundsMap.entries.find { it.value.contains(rootOffset) }
                                    if (hit != null) {
                                        draggedTopicId = hit.key
                                        touchPointInRoot = rootOffset
                                        touchAnchorOffset = rootOffset - hit.value.topLeft
                                        draggedItemSize = hit.value.size
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    touchPointInRoot += dragAmount
                                    
                                    // Check intersection with trash
                                    isOverTrash = trashZoneBoundsInRoot.contains(touchPointInRoot)
                                    
                                    if (!isOverTrash) {
                                        val target = itemBoundsMap.entries.find { 
                                            it.key != draggedTopicId && it.value.contains(touchPointInRoot) 
                                        }
                                        if (target != null) {
                                            val fromIndex = filteredTopics.indexOfFirst { it.id == draggedTopicId }
                                            val toIndex = filteredTopics.indexOfFirst { it.id == target.key }
                                            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                                val newList = filteredTopics.toMutableList()
                                                val item = newList.removeAt(fromIndex)
                                                newList.add(toIndex, item)
                                                filteredTopics = newList
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (isOverTrash && draggedTopicId != null) {
                                        onDeleteTopics(listOf(draggedTopicId!!))
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (draggedTopicId != null) {
                                        onUpdateOrder(filteredTopics.map { it.id })
                                    }
                                    draggedTopicId = null
                                    isOverTrash = false
                                },
                                onDragCancel = {
                                    draggedTopicId = null
                                    isOverTrash = false
                                    filteredTopics = baseTopics
                                }
                            )
                        }
                ) {
                    items(
                        items = filteredTopics,
                        key = { it.id }
                    ) { topic ->
                        Box(
                            modifier = Modifier
                                .animateItemPlacement()
                                .onGloballyPositioned { coords ->
                                    // Update tracked bounds for hit detection
                                    itemBoundsMap[topic.id] = coords.boundsInRoot()
                                }
                                .graphicsLayer {
                                    // Hide original item during drag
                                    alpha = if (draggedTopicId == topic.id) 0f else 1f
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
                                    } else if (draggedTopicId == null) {
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
            visible = draggedTopicId != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .zIndex(10f)
        ) {
            val scale by animateFloatAsState(if (isOverTrash) 1.2f else 1f, label = "trashScale")
            val containerColor by animateColorAsState(
                targetValue = if (isOverTrash) GrayMatterColors.Error else GrayMatterColors.SurfaceDark,
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
                        trashZoneBoundsInRoot = coords.boundsInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Drop to Delete",
                    tint = if (isOverTrash) Color.White else GrayMatterColors.Error,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Dragged Item Replica (anchored to root coordinates for jitter-free movement)
        if (draggedTopicId != null) {
            val topic = filteredTopics.find { it.id == draggedTopicId }
            if (topic != null) {
                val density = LocalDensity.current
                // Calculate position relative to this container's top-left
                val localOffset = touchPointInRoot - screenPositionInRoot - touchAnchorOffset
                
                Box(
                    modifier = Modifier
                        .offset { 
                            IntOffset(
                                localOffset.x.roundToInt(),
                                localOffset.y.roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { draggedItemSize.width.toDp() },
                            height = with(density) { draggedItemSize.height.toDp() }
                        )
                        .scale(1.05f)
                        .alpha(0.85f)
                        .zIndex(20f)
                ) {
                    TopicCard(
                        topic = topic,
                        romanNumeral = toRomanNumeral(filteredTopics.indexOf(topic) + 1),
                        isSelected = false,
                        onClick = {}
                    )
                }
            }
        }
    }
    
    // Safety cleanup
    DisposableEffect(Unit) {
        onDispose {
            draggedTopicId = null
            isOverTrash = false
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
    modifier: Modifier = Modifier
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
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Roman numeral icon
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
