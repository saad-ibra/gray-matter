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

    // Revamped Drag and Drop state
    var draggedTopicId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isOverTrash by remember { mutableStateOf(false) }
    
    // Layout tracking
    val itemBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    var trashZoneBounds by remember { mutableStateOf(Rect.Zero) }

    val baseTopics = if (searchQuery.isBlank()) topics else topics.filter { it.name.contains(searchQuery, ignoreCase = true) }
    var filteredTopics by remember(baseTopics) { mutableStateOf(baseTopics) }

    Box(modifier = modifier
        .fillMaxSize()
        .background(GrayMatterColors.BackgroundDark)
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredTopics,
                    key = { it.id }
                ) { topic ->
                    val isSelected = selectedTopics.contains(topic.id)
                    val isDragged = draggedTopicId == topic.id
                    var hasMoved by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .animateItemPlacement()
                            .onGloballyPositioned { coords ->
                                if (!isDragged) {
                                    itemBoundsMap[topic.id] = coords.boundsInRoot()
                                }
                            }
                            .graphicsLayer { alpha = if (isDragged) 0f else 1f }
                            .pointerInput(topic.id, selectionMode) {
                                if (selectionMode) return@pointerInput
                                
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggedTopicId = topic.id
                                        dragOffset = Offset.Zero
                                        isOverTrash = false
                                        hasMoved = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                        hasMoved = true
                                        
                                        val initialTopLeft = itemBoundsMap[topic.id]?.topLeft ?: Offset.Zero
                                        val touchPoint = initialTopLeft + change.position
                                        isOverTrash = trashZoneBounds.contains(touchPoint)
                                        
                                        if (!isOverTrash) {
                                            val targetTopicId = itemBoundsMap.entries.find { (id, rect) ->
                                                id != draggedTopicId && rect.contains(touchPoint)
                                            }?.key
                                            
                                            if (targetTopicId != null) {
                                                val fromIndex = filteredTopics.indexOfFirst { it.id == draggedTopicId }
                                                val toIndex = filteredTopics.indexOfFirst { it.id == targetTopicId }
                                                if (fromIndex != -1 && toIndex != -1) {
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
                                        } else if (draggedTopicId != null) {
                                            if (!hasMoved) {
                                                selectedTopics = setOf(draggedTopicId!!)
                                            } else {
                                                onUpdateOrder(filteredTopics.map { it.id })
                                            }
                                        }
                                        draggedTopicId = null
                                        dragOffset = Offset.Zero
                                        isOverTrash = false
                                    },
                                    onDragCancel = {
                                        draggedTopicId = null
                                        dragOffset = Offset.Zero
                                        isOverTrash = false
                                        filteredTopics = baseTopics
                                    }
                                )
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
                if (isOverTrash) GrayMatterColors.Error else GrayMatterColors.SurfaceDark,
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
                        trashZoneBounds = coords.boundsInRoot()
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

        // Dragged Item Replica
        if (draggedTopicId != null) {
            val topic = filteredTopics.find { it.id == draggedTopicId }
            val initialBounds = itemBoundsMap[draggedTopicId]
            if (topic != null && initialBounds != null) {
                val density = LocalDensity.current
                Box(
                    modifier = Modifier
                        .offset { 
                            IntOffset(
                                (initialBounds.left + dragOffset.x).roundToInt(),
                                (initialBounds.top + dragOffset.y).roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { initialBounds.width.toDp() },
                            height = with(density) { initialBounds.height.toDp() }
                        )
                        .scale(1.05f)
                        .alpha(0.9f)
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
