package com.example.graymatter.android.ui.fileviewer

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.domain.ChapterOutline
import com.example.graymatter.domain.ReadingSettings
import java.io.File

/**
 * Text/Markdown viewer with heading-based chapter parsing.
 * Uses a read-only BasicTextField to enable programmatically accessible text selection.
 */
@Composable
fun TextViewerContent(
    filePath: String,
    extractedText: String?,
    settings: ReadingSettings,
    context: Context,
    onChaptersParsed: (List<ChapterOutline>) -> Unit,
    onTextSelected: (String, Float, Float) -> Unit,
    searchQuery: String = "",
    searchMatchIndex: Int = 0,
    searchResults: List<com.example.graymatter.domain.SearchResult> = emptyList(),
    onScrollToOffset: (() -> Unit)? = null
) {
    val content = remember(filePath, extractedText) {
        if (!extractedText.isNullOrBlank()) {
            extractedText
        } else if (filePath.isNotBlank()) {
            try {
                File(filePath).readText()
            } catch (e: Exception) {
                "Error reading file: ${e.message}"
            }
        } else {
            "No content available."
        }
    }

    var textFieldValue by remember(content) { mutableStateOf(TextFieldValue(content)) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    LaunchedEffect(content) {
        onChaptersParsed(parseHeadings(content))
    }

    // Monitor selection changes
    LaunchedEffect(textFieldValue.selection) {
        val selection = textFieldValue.selection
        if (!selection.collapsed && textLayoutResult != null) {
            val selectedText = textFieldValue.text.substring(selection.start, selection.end)
            
            // Calculate coordinates for the popup
            // We take the top-middle of the selection
            try {
                val startPath = textLayoutResult!!.getCursorRect(selection.start)
                val endPath = textLayoutResult!!.getCursorRect(selection.end)
                
                val x = (startPath.left + endPath.left) / 2 + containerPosition.x
                val y = startPath.top + containerPosition.y
                
                onTextSelected(selectedText, x, y)
            } catch (_: Exception) {
                onTextSelected(selectedText, 0f, 0f)
            }
        } else if (selection.collapsed) {
            onTextSelected("", 0f, 0f)
        }
    }

    val themeColors = getThemeColors(settings.theme)
    val textColor = themeColors.text
    val bgColor = themeColors.background

    val fontFamily = when (settings.typeface) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    val scrollState = rememberScrollState()

    // Scroll to matched search result when search navigation triggers
    val currentResult = searchResults.getOrNull(searchMatchIndex)
    LaunchedEffect(currentResult?.matchStart, searchMatchIndex) {
        val matchOffset = currentResult?.matchStart ?: return@LaunchedEffect
        val layout = textLayoutResult ?: return@LaunchedEffect
        if (matchOffset in 0 until content.length) {
            try {
                val rect = layout.getCursorRect(matchOffset)
                val targetY = (rect.top - 200f).coerceAtLeast(0f).toInt()
                scrollState.animateScrollTo(targetY)
            } catch (_: Exception) { /* offset out of range */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = settings.margins.dp)
            .onGloballyPositioned { containerPosition = it.positionInWindow() }
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            readOnly = true,
            textStyle = TextStyle(
                color = textColor,
                fontSize = settings.fontSize.sp,
                fontFamily = fontFamily,
                lineHeight = (settings.fontSize * settings.lineSpacing).sp
            ),
            cursorBrush = SolidColor(Color.Transparent), // Hide cursor in read-only
            onTextLayout = { result: TextLayoutResult -> textLayoutResult = result },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 64.dp)
        )
    }
}

private fun parseHeadings(text: String): List<ChapterOutline> {
    val lines = text.split("\n")
    val chapters = mutableListOf<ChapterOutline>()
    var lineIndex = 0
    for (line in lines) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length - 1
            val title = trimmed.dropWhile { it == '#' }.trim()
            if (title.isNotEmpty()) {
                chapters.add(ChapterOutline(title, level.coerceAtMost(3), lineIndex))
            }
        }
        lineIndex++
    }
    return chapters
}
