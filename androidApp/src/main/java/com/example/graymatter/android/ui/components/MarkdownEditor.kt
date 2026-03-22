package com.example.graymatter.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    onShowReferenceSelector: (() -> Unit)? = null,
    referenceToInsert: String? = null,
    onReferenceInserted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length)))
    }
    var isPreviewMode by remember { mutableStateOf(initialPreviewMode) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(referenceToInsert, textFieldValue.selection) {
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
                if (textFieldValue.text != initialText) {
                    showDiscardConfirm = true
                } else {
                    onBackClick()
                }
            }) {
                Icon(Icons.Default.Close, "Close", tint = GrayMatterColors.TextPrimary)
            }
            
            Text(
                text = if (isPreviewMode) "PREVIEW" else "EDITOR",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = GrayMatterColors.Neutral500
            )

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
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterColors.TextPrimary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (isPreviewMode) {
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    MarkdownText(
                        markdown = textFieldValue.text,
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
                                Text("Type in Markdown here...", color = GrayMatterColors.Neutral700)
                            }
                            inner()
                        }
                    )
                }

                // Formatting Toolbar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp)),
                    color = GrayMatterColors.SurfaceDark
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = 0,
                        edgePadding = 8.dp,
                        containerColor = Color.Transparent,
                        indicator = {},
                        divider = {}
                    ) {
                        MarkdownToolbarAction(Icons.Default.FormatBold) { textFieldValue = wrapSelection(textFieldValue, "**") }
                        MarkdownToolbarAction(Icons.Default.FormatItalic) { textFieldValue = wrapSelection(textFieldValue, "_") }
                        MarkdownToolbarAction(Icons.Default.Title) { textFieldValue = toggleLineStart(textFieldValue, "### ") }
                        MarkdownToolbarAction(Icons.Default.FormatListBulleted) { textFieldValue = toggleLineStart(textFieldValue, "- ") }
                        MarkdownToolbarAction(Icons.Default.FormatQuote) { textFieldValue = toggleLineStart(textFieldValue, "> ") }
                        MarkdownToolbarAction(Icons.Default.Code) { textFieldValue = wrapSelection(textFieldValue, "`") }
                        MarkdownToolbarAction(Icons.Default.Link) { textFieldValue = wrapSelection(textFieldValue, "[", "](url)") }
                        if (onShowReferenceSelector != null) {
                            MarkdownToolbarAction(Icons.Default.AddLink) {
                                val txt = textFieldValue.text
                                val cursor = textFieldValue.selection.start
                                textFieldValue = TextFieldValue(
                                    txt.substring(0, cursor) + "[[" + txt.substring(cursor),
                                    TextRange(cursor + 2)
                                )
                                onShowReferenceSelector()
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
