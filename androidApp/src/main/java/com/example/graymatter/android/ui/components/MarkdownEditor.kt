package com.example.graymatter.android.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import dev.jeziellago.compose.markdowntext.MarkdownText


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkdownEditor(
    title: String,
    initialText: String,
    onBackClick: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialPreviewMode: Boolean = false,
    onTitleChange: ((String) -> Unit)? = null,
    onShowReferenceSelector: (() -> Unit)? = null,
    referenceToInsert: String? = null,
    onReferenceInserted: () -> Unit = {},
    onTextChange: (String) -> Unit = {},
    onReferenceTap: ((String) -> Unit)? = null
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length)))
    }
    var isPreviewMode by remember { mutableStateOf(initialPreviewMode) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var editableTitle by remember { mutableStateOf(title) }
    // Track the last-saved text so we can distinguish "dirty" from "saved"
    var lastSavedText by remember { mutableStateOf(initialText) }

    val hasUnsavedChanges = textFieldValue.text != lastSavedText || (onTitleChange != null && editableTitle != title)

    // Intercept Android back button
    BackHandler(enabled = true) {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onBackClick()
        }
    }

    // Sync text state upstream
    LaunchedEffect(textFieldValue.text) {
        onTextChange(textFieldValue.text)
    }

    // Sync title state upstream when editable
    LaunchedEffect(editableTitle) {
        if (onTitleChange != null && editableTitle != title) {
            onTitleChange(editableTitle)
        }
    }

    LaunchedEffect(referenceToInsert) {
        if (referenceToInsert != null) {
            val txt = textFieldValue.text
            val cursor = textFieldValue.selection.start
            
            val hasBracket = cursor >= 2 && txt.take(cursor).endsWith("[[")
            val hasAt = cursor >= 1 && txt.take(cursor).endsWith("@")
            
            val replaceLen = if (hasBracket) 2 else if (hasAt) 1 else 0
            val newText = txt.take(cursor - replaceLen) + referenceToInsert + txt.substring(cursor)
            
            textFieldValue = TextFieldValue(newText, TextRange(cursor - replaceLen + referenceToInsert.length))
            onReferenceInserted()
        }
    }

    // Live-extracted [[references]] from note content
    val liveReferences = remember(textFieldValue.text) {
        val regex = Regex("\\[\\[(.*?)]]")
        regex.findAll(textFieldValue.text).map { it.groupValues[1] }.toList()
    }

    val wordCount = remember(textFieldValue.text) {
        textFieldValue.text.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
            .statusBarsPadding()
    ) {
        // Immersive Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (hasUnsavedChanges) {
                    showUnsavedDialog = true
                } else {
                    onBackClick()
                }
            }) {
                Icon(Icons.Default.Close, "Close", tint = GrayMatterColors.TextPrimary)
            }
            
            // Mode Indicator / Word Count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isPreviewMode) "PREVIEW" else "EDITOR",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = GrayMatterColors.Neutral500
                )
                if (!isPreviewMode && wordCount > 0) {
                    Text(
                        text = "$wordCount words",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMatterColors.Neutral600
                    )
                }
            }

            TextButton(
                onClick = {
                    onSave(textFieldValue.text)
                    lastSavedText = textFieldValue.text
                },
                enabled = editableTitle.isNotBlank() && hasUnsavedChanges
            ) {
                Text(
                    "Save", 
                    color = if (editableTitle.isNotBlank() && hasUnsavedChanges) GrayMatterColors.Primary else GrayMatterColors.Neutral700,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Premium Segmented Control (Mode Switcher)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = GrayMatterColors.Neutral900.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Write Tab
                Surface(
                    onClick = { isPreviewMode = false },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (!isPreviewMode) GrayMatterColors.Neutral800 else Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Write",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (!isPreviewMode) Color.White else GrayMatterColors.Neutral500
                        )
                    }
                }
                
                // Preview Tab
                Surface(
                    onClick = { isPreviewMode = true },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPreviewMode) GrayMatterColors.Neutral800 else Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isPreviewMode) Color.White else GrayMatterColors.Neutral500
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = GrayMatterColors.Neutral800.copy(alpha = 0.3f))

        // Single flow scrollable container
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (isPreviewMode) {
                // Preview Mode
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.displaySmall,
                            color = GrayMatterColors.TextPrimary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        // Split text by [[...]] references for mixed rendering
                        val rawText = textFieldValue.text
                        val refPattern = Regex("\\[\\[(.*?)]]")
                        val segments = remember(rawText) {
                            buildList {
                                var lastEnd = 0
                                for (match in refPattern.findAll(rawText)) {
                                    if (match.range.first > lastEnd) {
                                        add(PreviewSegment.Markdown(rawText.substring(lastEnd, match.range.first)))
                                    }
                                    add(PreviewSegment.Reference(match.groupValues[1]))
                                    lastEnd = match.range.last + 1
                                }
                                if (lastEnd < rawText.length) {
                                    add(PreviewSegment.Markdown(rawText.substring(lastEnd)))
                                }
                            }
                        }

                        segments.forEach { segment ->
                            when (segment) {
                                is PreviewSegment.Markdown -> {
                                    if (segment.text.isNotBlank()) {
                                        MarkdownText(
                                            markdown = segment.text,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color.White.copy(alpha = 0.9f),
                                                lineHeight = 28.sp
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                is PreviewSegment.Reference -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GrayMatterColors.TypeLink.copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .padding(vertical = 4.dp)
                                            .border(
                                                0.5.dp,
                                                GrayMatterColors.TypeLink.copy(alpha = 0.3f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (onReferenceTap != null) {
                                                    if (hasUnsavedChanges) {
                                                        onSave(textFieldValue.text)
                                                        lastSavedText = textFieldValue.text
                                                    }
                                                    onReferenceTap(segment.refText)
                                                }
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Link,
                                                null,
                                                tint = GrayMatterColors.TypeLink,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                segment.refText,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = GrayMatterColors.TypeLink
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Editor Mode (Single Flow)
                Column(
                    modifier = Modifier
                        .weight(1f) // Fix: Use weight instead of fillMaxSize to allow toolbar to show
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Title Section
                        if (onTitleChange != null) {
                            BasicTextField(
                                value = editableTitle,
                                onValueChange = {
                                    editableTitle = it
                                },
                                textStyle = MaterialTheme.typography.displaySmall.copy(
                                    color = GrayMatterColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(GrayMatterColors.Primary),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp),
                                decorationBox = { inner ->
                                    if (editableTitle.isEmpty()) {
                                        Text(
                                            "Untitled Note",
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                color = GrayMatterColors.Neutral800,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    inner()
                                }
                            )
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = GrayMatterColors.TextPrimary,
                                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                            )
                        }

                        // Content Section
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp)) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newValue -> 
                                    val oldTxt = textFieldValue.text
                                    val newTxt = newValue.text
                                    val cursor = newValue.selection.start
                                    
                                    if (newTxt.length > oldTxt.length) {
                                        if (cursor >= 2 && newTxt.take(cursor).endsWith("[[")) {
                                            onShowReferenceSelector?.invoke()
                                        } else if (cursor >= 1 && newTxt.take(cursor).endsWith("@")) {
                                            onShowReferenceSelector?.invoke()
                                        }
                                    }
                                    
                                    textFieldValue = newValue 
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 28.sp
                                ),
                                cursorBrush = SolidColor(GrayMatterColors.Primary),
                                visualTransformation = MarkdownVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (textFieldValue.text.isEmpty()) {
                                        Text("Start writing your thoughts...", color = GrayMatterColors.Neutral700, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }

                // Glassmorphic Floating Toolbar (Anchored above keyboard)
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                1.dp, 
                                GrayMatterColors.Neutral800.copy(alpha = 0.4f), 
                                RoundedCornerShape(24.dp)
                            ),
                        color = GrayMatterColors.SurfaceDark.copy(alpha = 0.85f),
                        tonalElevation = 8.dp
                    ) {
                        Column {
                            // Live References Header in Toolbar
                            if (liveReferences.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, null, tint = GrayMatterColors.TypeLink.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    liveReferences.forEach { ref ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GrayMatterColors.TypeLink.copy(alpha = 0.1f),
                                            modifier = Modifier.border(0.5.dp, GrayMatterColors.TypeLink.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        ) {
                                            Text(ref, style = MaterialTheme.typography.labelSmall, color = GrayMatterColors.TypeLink, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GrayMatterColors.Neutral800.copy(alpha = 0.3f)))
                            }

                            // Actions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MarkdownToolbarAction(Icons.Default.FormatBold) { textFieldValue = wrapSelection(textFieldValue, "**") }
                                MarkdownToolbarAction(Icons.Default.FormatItalic) { textFieldValue = wrapSelection(textFieldValue, "_") }
                                MarkdownToolbarAction(Icons.Default.Title) { textFieldValue = toggleLineStart(textFieldValue, "### ") }
                                MarkdownToolbarAction(Icons.AutoMirrored.Filled.FormatListBulleted) { textFieldValue = toggleLineStart(textFieldValue, "- ") }
                                MarkdownToolbarAction(Icons.Default.FormatQuote) { textFieldValue = toggleLineStart(textFieldValue, "> ") }
                                MarkdownToolbarAction(Icons.Default.Code) { textFieldValue = wrapSelection(textFieldValue, "`") }
                                
                                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = GrayMatterColors.Neutral800)
                                
                                // Reference Trigger
                                TextButton(
                                    onClick = {
                                        val txt = textFieldValue.text
                                        val cursor = textFieldValue.selection.start
                                        textFieldValue = TextFieldValue(txt.take(cursor) + "[[" + txt.substring(cursor), TextRange(cursor + 2))
                                        onShowReferenceSelector?.invoke()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = GrayMatterColors.TypeLink)
                                ) {
                                    Icon(Icons.Default.Attachment, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reference", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Unsaved Changes Dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes", color = Color.White) },
            text = { Text("You have unsaved changes. What would you like to do?", color = GrayMatterColors.TextSecondary) },
            containerColor = Color(0xFF1E1E22),
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onSave(textFieldValue.text)
                    lastSavedText = textFieldValue.text
                    onBackClick()
                }) {
                    Text("Save & Close", color = GrayMatterColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        onBackClick()
                    }) {
                        Text("Discard", color = Color.Red.copy(alpha = 0.8f))
                    }
                }
            }
        )
    }
}

/**
 * Live Markdown syntax highlighting transformation.
 */
class MarkdownVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val s = text.text
        val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
            append(s)
            
            // Highlight [[references]]
            Regex("\\[\\[(.*?)]]").findAll(s).forEach { match ->
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        color = GrayMatterColors.TypeLink,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
            
            // Highlight **bold**
            Regex("\\*\\*(.*?)\\*\\*").findAll(s).forEach { match ->
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color.White),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
            
            // Highlight _italic_
            Regex("_(.*?)_").findAll(s).forEach { match ->
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.White.copy(alpha = 0.9f)),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
            
            // Highlight `inline code`
            Regex("`(.*?)`").findAll(s).forEach { match ->
                addStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = Color.White.copy(alpha = 0.1f),
                        color = GrayMatterColors.Neutral300
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
        return androidx.compose.ui.text.input.TransformedText(annotatedString, androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}

private sealed class PreviewSegment {
    data class Markdown(val text: String) : PreviewSegment()
    data class Reference(val refText: String) : PreviewSegment()
}

@Composable
private fun MarkdownToolbarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, null, tint = GrayMatterColors.Neutral500, modifier = Modifier.size(20.dp))
    }
}

private fun wrapSelection(value: TextFieldValue, prefix: String, suffix: String = prefix): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val start = selection.start
    val end = selection.end

    return if (selection.collapsed) {
        val newText = StringBuilder(text).insert(start, prefix + suffix).toString()
        TextFieldValue(newText, TextRange(start + prefix.length))
    } else {
        val selected = text.substring(start, end)
        if (selected.startsWith(prefix) && selected.endsWith(suffix)) {
            val unwrapped = selected.removePrefix(prefix).removeSuffix(suffix)
            val newText = text.take(start) + unwrapped + text.substring(end)
            TextFieldValue(newText, TextRange(start, start + unwrapped.length))
        } else {
            val newText = text.take(start) + prefix + selected + suffix + text.substring(end)
            TextFieldValue(newText, TextRange(start, end + prefix.length + suffix.length))
        }
    }
}

private fun toggleLineStart(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    var lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
    if (lineStart < 0) lineStart = 0
    var lineEnd = text.indexOf('\n', selection.start)
    if (lineEnd < 0) lineEnd = text.length
    
    val line = text.substring(lineStart, lineEnd)
    return if (line.startsWith(prefix)) {
        val newText = text.take(lineStart) + line.removePrefix(prefix) + text.substring(lineEnd)
        TextFieldValue(newText, TextRange(selection.start - prefix.length))
    } else {
        val newText = text.take(lineStart) + prefix + line + text.substring(lineEnd)
        TextFieldValue(newText, TextRange(selection.start + prefix.length))
    }
}
