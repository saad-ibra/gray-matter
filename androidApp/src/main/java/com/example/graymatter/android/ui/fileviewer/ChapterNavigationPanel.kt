package com.example.graymatter.android.ui.fileviewer

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.domain.ChapterOutline

/**
 * Collapsible Table of Contents (TOC) panel.
 * Displays a hierarchical chapter tree with expand/collapse,
 * per-chapter progress indicators, and current chapter highlighting.
 */
@Composable
fun ChapterNavigationPanel(
    chapters: List<ChapterOutline>,
    currentPage: Int,
    totalPages: Int,
    onChapterClick: (ChapterOutline) -> Unit,
    onClose: () -> Unit
) {
    // Moved outside LazyColumn to fix @Composable invocation error
    val flattenedChapters = flattenForDisplay(chapters)

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 320.dp)
            .fillMaxWidth(0.8f),
        color = Color(0xFF1A1A1E),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Contents",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            if (chapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No chapters detected",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                ) {
                    items(flattenedChapters) { item ->
                        ChapterRow(
                            chapter = item.chapter,
                            level = item.displayLevel,
                            isExpanded = item.isExpanded,
                            hasChildren = item.hasChildren,
                            isCurrent = isCurrentChapter(item.chapter, chapters, currentPage),
                            chapterProgress = calculateChapterProgress(item.chapter, chapters, currentPage, totalPages),
                            onToggleExpand = item.onToggle,
                            onClick = { onChapterClick(item.chapter) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterOutline,
    level: Int,
    isExpanded: Boolean,
    hasChildren: Boolean,
    isCurrent: Boolean,
    chapterProgress: Float,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit
) {
    val bgColor = if (isCurrent) Color.White.copy(alpha = 0.08f) else Color.Transparent
    val textColor = if (isCurrent) Color.White else Color.White.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(start = (16 + level * 20).dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/collapse toggle
        if (hasChildren) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    "Toggle",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        // Chapter title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                color = textColor,
                fontSize = if (level == 0) 15.sp else 13.sp,
                fontWeight = if (level == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Chapter progress bar
            if (chapterProgress > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = chapterProgress,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = Color(0xFF22C55E),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }

        // Page number
        Text(
            text = "${chapter.targetPage + 1}",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp
        )

        // Current chapter indicator
        if (isCurrent) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF22C55E))
            )
        }
    }
}

// -- Helper data and functions for collapsible tree --

data class FlatChapterItem(
    val chapter: ChapterOutline,
    val displayLevel: Int,
    val isExpanded: Boolean,
    val hasChildren: Boolean,
    val onToggle: () -> Unit
)

@Composable
private fun flattenForDisplay(chapters: List<ChapterOutline>): List<FlatChapterItem> {
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val result = mutableListOf<FlatChapterItem>()

    fun addChapters(items: List<ChapterOutline>, level: Int) {
        for (chapter in items) {
            val key = "${chapter.title}_${chapter.targetPage}"
            val isExpanded = expandedState[key] ?: (level == 0)
            val hasChildren = chapter.children.isNotEmpty()

            result.add(
                FlatChapterItem(
                    chapter = chapter,
                    displayLevel = level,
                    isExpanded = isExpanded,
                    hasChildren = hasChildren,
                    onToggle = {
                        expandedState[key] = !(expandedState[key] ?: (level == 0))
                    }
                )
            )

            if (isExpanded && hasChildren) {
                addChapters(chapter.children, level + 1)
            }
        }
    }

    addChapters(chapters, 0)
    return result
}

private fun isCurrentChapter(
    chapter: ChapterOutline,
    allChapters: List<ChapterOutline>,
    currentPage: Int
): Boolean {
    val flat = flattenAll(allChapters)
    val idx = flat.indexOf(chapter)
    if (idx == -1) return false
    val nextChapter = flat.getOrNull(idx + 1)
    return currentPage >= chapter.targetPage && (nextChapter == null || currentPage < nextChapter.targetPage)
}

private fun calculateChapterProgress(
    chapter: ChapterOutline,
    allChapters: List<ChapterOutline>,
    currentPage: Int,
    totalPages: Int
): Float {
    val flat = flattenAll(allChapters)
    val idx = flat.indexOf(chapter)
    if (idx == -1) return 0f

    val chapterStart = chapter.targetPage
    val chapterEnd = flat.getOrNull(idx + 1)?.targetPage ?: totalPages
    val chapterLength = chapterEnd - chapterStart

    if (chapterLength <= 0) return 0f
    if (currentPage < chapterStart) return 0f
    if (currentPage >= chapterEnd) return 1f

    return ((currentPage - chapterStart).toFloat() / chapterLength).coerceIn(0f, 1f)
}

private fun flattenAll(chapters: List<ChapterOutline>): List<ChapterOutline> {
    val result = mutableListOf<ChapterOutline>()
    for (ch in chapters) {
        result.add(ch)
        result.addAll(flattenAll(ch.children))
    }
    return result
}
