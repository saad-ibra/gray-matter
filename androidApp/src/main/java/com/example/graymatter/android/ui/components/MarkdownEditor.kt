package com.example.graymatter.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun MarkdownEditor(
    title: String,
    initialText: String,
    onBackClick: () -> Unit,
    onSave: (String) -> Unit,
    initialPreviewMode: Boolean = false,
    onTitleChange: ((String) -> Unit)? = null,
    onShowReferenceSelector: (() -> Unit)? = null,
    referenceToInsert: String? = null,
    onReferenceInserted: () -> Unit = {},
    onTextChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length)))
    }
    var isPreviewMode by remember { mutableStateOf(initialPreviewMode) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var editableTitle by remember { mutableStateOf(title) }

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
            
            val hasBracket = cursor >= 2 && txt.substring(cursor - 2, cursor) == "[["
            val hasAt = cursor >= 1 && txt.substring(cursor - 1, cursor) == "@"
            
            val replaceLen = if (hasBracket) 2 else if (hasAt) 1 else 0
            val newText = txt.substring(0, cursor - replaceLen) + referenceToInsert + txt.substring(cursor)
            
            textFieldValue = TextFieldValue(newText, TextRange(cursor - replaceLen + referenceToInsert.length))
            onReferenceInserted()
        }
    }

    // Live-extracted [[references]] from note content
    val liveReferences = remember(textFieldValue.text) {
        val regex = Regex("\\[\\[(.*?)\\]\\]")
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
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (textFieldValue.text != initialText || editableTitle != title) {
                    showDiscardConfirm = true
                } else {
                    onBackClick()
                }
            }) {
                Icon(Icons.Default.Close, "Close", tint = GrayMatterColors.TextPrimary)
            }
            
            // Word count & mode label
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

            TextButton(onClick = { onSave(textFieldValue.text) }) {
                Text("Save", color = GrayMatterColors.Primary, fontWeight = FontWeight.Bold)
            }
        }

        // Mode Switcher
        TabRow(
            selectedTabIndex = if (isPreviewMode) 1 else 0,
            containerColor = GrayMatterColors.BackgroundDark,
            contentColor = GrayMatterColors.Primary,
            divider = { Divider(color = GrayMatterColors.Neutral800) }
        ) {
            Tab(selected = !isPreviewMode, onClick = { isPreviewMode = false }) {
                Text("Write", modifier = Modifier.padding(12.dp), color = if (!isPreviewMode) GrayMatterColors.Primary else GrayMatterColors.Neutral500)
            }
            Tab(selected = isPreviewMode, onClick = { isPreviewMode = true }) {
                Text("Preview", modifier = Modifier.padding(12.dp), color = if (isPreviewMode) GrayMatterColors.Primary else GrayMatterColors.Neutral500)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Inline Editable Title
            if (onTitleChange != null) {
                BasicTextField(
                    value = editableTitle,
                    onValueChange = {
                        editableTitle = it
                        onTitleChange(it)
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GrayMatterColors.TextPrimary
                    ),
                    cursorBrush = SolidColor(GrayMatterColors.Primary),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    decorationBox = { inner ->
                        if (editableTitle.isEmpty()) {
                            Text(
                                "Note Title",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GrayMatterColors.Neutral800
                                )
                            )
                        }
                        inner()
                    }
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            if (isPreviewMode) {
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    val processedMarkdown = textFieldValue.text.replace(
                        Regex("\\[\\[(.*?)\\]\\]"), 
                        "**<u>$1</u>**"
                    )
                    MarkdownText(
                        markdown = processedMarkdown,
                        color = Color.White,
                        modifier = Modifier.fillMaxSize().padding(bottom = 32.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue -> 
                            val oldTxt = textFieldValue.text
                            val newTxt = newValue.text
                            val cursor = newValue.selection.start
                            
                            if (newTxt.length > oldTxt.length) {
                                if (cursor >= 2 && newTxt.substring(cursor-2, cursor) == "[[") {
                                    onShowReferenceSelector?.invoke()
                                } else if (cursor >= 1 && newTxt.substring(cursor-1, cursor) == "@") {
                                    onShowReferenceSelector?.invoke()
                                }
                            }
                            
                            if (newTxt != oldTxt) {
                                onTextChange(newTxt)
                            }
                            
                            textFieldValue = newValue 
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = GrayMatterColors.TextPrimary,
                            lineHeight = 28.sp
                        ),
                        cursorBrush = SolidColor(GrayMatterColors.Primary),
                        modifier = Modifier.fillMaxSize(),
                        decorationBox = { inner ->
                            if (textFieldValue.text.isEmpty()) {
                                Text("Start writing your thoughts...", color = GrayMatterColors.Neutral700)
                            }
                            inner()
                        }
                    )
                }

                // Unified Footer Tooling (Glassmorphic Bar)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            1.dp, 
                            GrayMatterColors.Neutral800.copy(alpha = 0.5f), 
                            RoundedCornerShape(20.dp)
                        ),
                    color = GrayMatterColors.SurfaceDark.copy(alpha = 0.92f),
                    tonalElevation = 6.dp
                ) {
                    Column {
                        // Live Reference Chips (Top Section)
                        if (liveReferences.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = GrayMatterColors.Jonquil.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                liveReferences.forEach { ref ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GrayMatterColors.Jonquil.copy(alpha = 0.12f),
                                        modifier = Modifier.border(
                                            0.5.dp,
                                            GrayMatterColors.Jonquil.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                    ) {
                                        Text(
                                            text = ref,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GrayMatterColors.Jonquil,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(GrayMatterColors.Neutral800.copy(alpha = 0.5f))
                            )
                        }

                        // Formatting Toolbar (Bottom Section)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MarkdownToolbarAction(Icons.Default.FormatBold) { textFieldValue = wrapSelection(textFieldValue, "**") }
                            MarkdownToolbarAction(Icons.Default.FormatItalic) { textFieldValue = wrapSelection(textFieldValue, "_") }
                            MarkdownToolbarAction(Icons.Default.Title) { textFieldValue = toggleLineStart(textFieldValue, "### ") }
                            MarkdownToolbarAction(Icons.Default.FormatListBulleted) { textFieldValue = toggleLineStart(textFieldValue, "- ") }
                            MarkdownToolbarAction(Icons.Default.FormatQuote) { textFieldValue = toggleLineStart(textFieldValue, "> ") }
                            MarkdownToolbarAction(Icons.Default.Code) { textFieldValue = wrapSelection(textFieldValue, "`") }
                            MarkdownToolbarAction(Icons.Default.Link) { textFieldValue = wrapSelection(textFieldValue, "[", "](url)") }
                            
                            if (onShowReferenceSelector != null) {
                                VerticalDivider(
                                    modifier = Modifier.height(24.dp).padding(horizontal = 8.dp),
                                    color = GrayMatterColors.Neutral800
                                )
                                
                                // Enhanced Reference button
                                Surface(
                                    onClick = { 
                                        val txt = textFieldValue.text
                                        val cursor = textFieldValue.selection.start
                                        textFieldValue = TextFieldValue(
                                            txt.substring(0, cursor) + "[[" + txt.substring(cursor),
                                            TextRange(cursor + 2)
                                        )
                                        onShowReferenceSelector()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = GrayMatterColors.Primary.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Attachment,
                                            null,
                                            tint = GrayMatterColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Reference",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GrayMatterColors.Primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding().height(8.dp))
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard Changes?", color = Color.White) },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?", color = GrayMatterColors.TextSecondary) },
            containerColor = Color(0xFF1A1A1E),
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onBackClick()
                }) {
                    Text("Discard", color = GrayMatterColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
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
        // If selection is collapsed, just insert markers and move cursor between them
        val newText = StringBuilder(text).insert(start, prefix + suffix).toString()
        TextFieldValue(newText, TextRange(start + prefix.length))
    } else {
        val selected = text.substring(start, end)
        // Check if already wrapped
        if (selected.startsWith(prefix) && selected.endsWith(suffix)) {
            val unwrapped = selected.removePrefix(prefix).removeSuffix(suffix)
            val newText = text.substring(0, start) + unwrapped + text.substring(end)
            TextFieldValue(newText, TextRange(start, start + unwrapped.length))
        } else {
            val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
            TextFieldValue(newText, TextRange(start, end + prefix.length + suffix.length))
        }
    }
}

private fun toggleLineStart(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    // Find start of current line
    var lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
    if (lineStart < 0) lineStart = 0
    
    // Find end of current line
    var lineEnd = text.indexOf('\n', selection.start)
    if (lineEnd < 0) lineEnd = text.length
    
    val line = text.substring(lineStart, lineEnd)
    return if (line.startsWith(prefix)) {
        val newText = text.substring(0, lineStart) + line.removePrefix(prefix) + text.substring(lineEnd)
        TextFieldValue(newText, TextRange(selection.start - prefix.length))
    } else {
        val newText = text.substring(0, lineStart) + prefix + line + text.substring(lineEnd)
        TextFieldValue(newText, TextRange(selection.start + prefix.length))
    }
}
