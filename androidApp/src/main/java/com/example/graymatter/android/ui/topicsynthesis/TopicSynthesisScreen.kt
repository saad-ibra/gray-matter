package com.example.graymatter.android.ui.topicsynthesis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.domain.Topic

/**
 * Topic Synthesis Screen.
 * Lists resources and allows adding an overall opinion with a robust markdown editor.
 */
@Composable
fun TopicSynthesisScreen(
    topic: Topic?,
    resources: List<Resource>,
    onBackClick: () -> Unit,
    onAddResource: () -> Unit,
    onResourceClick: (Resource) -> Unit,
    onSaveOverallOpinion: (String) -> Unit,
    onDeleteTopic: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (topic == null) return

    var showEditor by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark),
        containerColor = GrayMatterColors.BackgroundDark
    ) { paddingValues ->
        if (showEditor) {
            OverallOpinionEditor(
                title = topic.name,
                initialText = topic.notes ?: "",
                onBackClick = { showEditor = false },
                onSave = { content -> 
                    onSaveOverallOpinion(content)
                    showEditor = false
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TopicHeader(
                    topicName = topic.name,
                    onBackClick = onBackClick,
                    onDeleteClick = { showDeleteConfirm = true }
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        ResourcesHeader(
                            count = resources.size,
                            onAddClick = onAddResource
                        )
                    }
                    
                    items(resources) { resource ->
                        ResourceItem(
                            resource = resource,
                            onClick = { onResourceClick(resource) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        OverallOpinionSection(
                            hasOpinion = !topic.notes.isNullOrBlank(),
                            onClick = { showEditor = true },
                            onExport = onExport
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Topic", color = Color.White) },
            text = { Text("Are you sure you want to delete this topic? Resources within this topic will not be deleted, but they will be unassigned from this topic.", color = GrayMatterColors.TextSecondary) },
            containerColor = Color(0xFF1A1A1E),
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteTopic()
                }) {
                    Text("Delete", color = GrayMatterColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}
@Composable
fun OverallOpinionEditor(
    title: String,
    initialText: String,
    onBackClick: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length)))
    }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var textAlign by remember { mutableStateOf(TextAlign.Left) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
            .statusBarsPadding()
            .imePadding() // Ensures the editor moves above the keyboard
    ) {
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
                text = "CONTENT EDITOR",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = GrayMatterColors.Neutral500
            )
            TextButton(onClick = { onSave(textFieldValue.text) }) {
                Text("Save", color = GrayMatterColors.Primary, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = GrayMatterColors.TextPrimary,
                    textAlign = textAlign,
                    lineHeight = 28.sp
                ),
                cursorBrush = SolidColor(GrayMatterColors.Primary),
                visualTransformation = MarkdownVisualTransformation(GrayMatterColors.Primary),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    if (textFieldValue.text.isEmpty()) {
                        Text("Share your thoughts...", color = GrayMatterColors.Neutral700)
                    }
                    inner()
                }
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp)),
            color = GrayMatterColors.SurfaceDark,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { textFieldValue = wrapSelection(textFieldValue, "**") }) {
                    Icon(Icons.Default.FormatBold, "Bold", tint = GrayMatterColors.Neutral500)
                }
                IconButton(onClick = { textFieldValue = wrapSelection(textFieldValue, "_") }) {
                    Icon(Icons.Default.FormatItalic, "Italic", tint = GrayMatterColors.Neutral500)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(GrayMatterColors.Neutral800))
                IconButton(onClick = { textAlign = TextAlign.Left }) {
                    Icon(Icons.Default.FormatAlignLeft, "Left", tint = if (textAlign == TextAlign.Left) GrayMatterColors.Primary else GrayMatterColors.Neutral500)
                }
                IconButton(onClick = { textAlign = TextAlign.Center }) {
                    Icon(Icons.Default.FormatAlignCenter, "Center", tint = if (textAlign == TextAlign.Center) GrayMatterColors.Primary else GrayMatterColors.Neutral500)
                }
                IconButton(onClick = { textAlign = TextAlign.Right }) {
                    Icon(Icons.Default.FormatAlignRight, "Right", tint = if (textAlign == TextAlign.Right) GrayMatterColors.Primary else GrayMatterColors.Neutral500)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(GrayMatterColors.Neutral800))
                IconButton(onClick = { textFieldValue = toggleLineStart(textFieldValue, "• ") }) {
                    Icon(Icons.Default.FormatListBulleted, "Bullets", tint = GrayMatterColors.Neutral500)
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

private fun wrapSelection(value: TextFieldValue, marker: String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val start = selection.start
    val end = selection.end
    
    return if (selection.collapsed) {
        if (start >= marker.length && end <= text.length - marker.length &&
            text.substring(start - marker.length, start) == marker &&
            text.substring(end, end + marker.length) == marker) {
            val newText = text.substring(0, start - marker.length) + 
                          text.substring(start, end) + 
                          text.substring(end + marker.length)
            TextFieldValue(newText, TextRange(start - marker.length))
        } else {
            val newText = StringBuilder(text).insert(start, marker + marker).toString()
            TextFieldValue(newText, TextRange(start + marker.length))
        }
    } else {
        val selected = text.substring(start, end)
        if (selected.startsWith(marker) && selected.endsWith(marker)) {
            val unwrapped = selected.removePrefix(marker).removeSuffix(marker)
            val newText = text.substring(0, start) + unwrapped + text.substring(end)
            return TextFieldValue(newText, TextRange(start, start + unwrapped.length))
        } else if (start >= marker.length && end <= text.length - marker.length &&
                   text.substring(start - marker.length, start) == marker &&
                   text.substring(end, end + marker.length) == marker) {
            val newText = text.substring(0, start - marker.length) + selected + text.substring(end + marker.length)
            return TextFieldValue(newText, TextRange(start - marker.length, end - marker.length))
        } else {
            val newText = text.substring(0, start) + marker + selected + marker + text.substring(end)
            return TextFieldValue(newText, TextRange(start, end + marker.length * 2))
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
        val newText = text.substring(0, lineStart) + line.removePrefix(prefix) + text.substring(lineEnd)
        TextFieldValue(newText, TextRange(selection.start - prefix.length))
    } else {
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        TextFieldValue(newText, TextRange(selection.start + prefix.length))
    }
}

class MarkdownVisualTransformation(private val highlightColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = buildAnnotatedString {
            val rawText = text.text
            append(rawText)
            Regex("""\*\*(.*?)\*\*""").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.first, match.range.first + 2)
                addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.last - 1, match.range.last + 1)
            }
            Regex("""_(.*?)_""").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
                addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.first, match.range.first + 1)
                addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.last, match.range.last + 1)
            }
            Regex("""(?m)^•\s""").findAll(rawText).forEach { match ->
                addStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold), match.range.first, match.range.first + 1)
            }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}

@Composable
private fun OverallOpinionSection(
    hasOpinion: Boolean,
    onClick: () -> Unit,
    onExport: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OVERALL OPINION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = GrayMatterColors.Neutral500
            )
            
            if (hasOpinion) {
                TextButton(onClick = onExport) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp), tint = GrayMatterColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Summary", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.Primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GrayMatterColors.Neutral900)
                .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = if (hasOpinion) Icons.Default.EditNote else Icons.Default.RateReview,
                    contentDescription = null,
                    tint = GrayMatterColors.Primary
                )
                Text(
                    text = if (hasOpinion) "Edit Overall Opinion" else "Add Overall Opinion",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.Primary
                )
            }
        }
    }
}

@Composable
private fun TopicHeader(
    topicName: String,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayMatterColors.BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.KeyboardArrowLeft, "Back", tint = GrayMatterColors.TextPrimary, modifier = Modifier.size(28.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOPIC", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = GrayMatterColors.Neutral500)
            Text(topicName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = GrayMatterColors.TextPrimary)
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Delete, "Delete Topic", tint = GrayMatterColors.Error, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ResourcesHeader(
    count: Int,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("RESOURCES ($count)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = GrayMatterColors.Neutral500)
        IconButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, "Add Resource", tint = GrayMatterColors.TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ResourceItem(
    resource: Resource,
    onClick: () -> Unit
) {
    val icon = when (resource.type) {
        ResourceType.WEB_LINK -> Icons.Default.Language
        ResourceType.MARKDOWN -> Icons.Default.EditNote
        ResourceType.PDF -> Icons.Default.PictureAsPdf
        ResourceType.IMAGE -> Icons.Default.Image
        else -> Icons.Default.Description
    }

    val typeLabel = when (resource.type) {
        ResourceType.WEB_LINK -> "URL"
        else -> "Local File"
    }

    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GrayMatterColors.SurfaceDark)
                    .border(1.dp, GrayMatterColors.Neutral800, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (resource.type == ResourceType.MARKDOWN) GrayMatterColors.Primary else GrayMatterColors.Neutral500,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title ?: resource.url ?: "Untitled",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = GrayMatterColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = if (resource.type == ResourceType.WEB_LINK) resource.url ?: "URL" else typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMatterColors.Neutral600,
                    maxLines = 1
                )
            }
        }
        Divider(color = GrayMatterColors.Neutral800.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
    }
}
