package com.example.graymatter.android.ui.newentry

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.topicsynthesis.OverallOpinionEditor

/**
 * New Resource Screen.
 * Allows user to add a resource (link/file/note), an optional description, and first opinion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    onBackClick: () -> Unit,
    onSaveClick: (type: EntryType, value: String, opinion: String, confidence: Int, fileName: String?, description: String?) -> Unit,
    isSaving: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Link, 1 = File, 2 = Note
    var titleInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var opinionInput by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var confidence by remember { mutableFloatStateOf(0.75f) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var showDescription by remember { mutableStateOf(false) }
    var isNoteEditorOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e("NewEntryScreen", "Failed to take persistable URI permission", e)
            }

            selectedFileUri = it
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        selectedFileName = c.getString(nameIndex)
                    }
                }
            }
            if (selectedFileName == null) {
                selectedFileName = it.lastPathSegment ?: "Unknown file"
            }
        }
    }

    if (isNoteEditorOpen) {
        OverallOpinionEditor(
            title = if (titleInput.isBlank()) "New Note" else titleInput,
            initialText = noteContent,
            onBackClick = { isNoteEditorOpen = false },
            onSave = { content ->
                noteContent = content
                isNoteEditorOpen = false
            }
        )
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
    ) {
        // Header
        NewEntryHeader(onBackClick = onBackClick, modifier = Modifier.statusBarsPadding())
        
        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Source Material Section (Reordered: Link, File, Note)
            SourceMaterialSection(
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                titleInput = titleInput,
                onTitleChange = { titleInput = it },
                urlInput = urlInput,
                onUrlChange = { urlInput = it },
                selectedFileName = selectedFileName,
                onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                onAddNoteContent = { isNoteEditorOpen = true },
                hasNoteContent = noteContent.isNotBlank()
            )

            // Optional Description Dropdown
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDescription = !showDescription }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (showDescription) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        null,
                        tint = GrayMatterColors.Neutral500,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Description (optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = GrayMatterColors.Neutral500
                    )
                }
                
                AnimatedVisibility(
                    visible = showDescription,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GrayMatterColors.SurfaceInput)
                            .border(1.dp, GrayMatterColors.SurfaceBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = descriptionInput,
                            onValueChange = { descriptionInput = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = GrayMatterColors.TextPrimary),
                            modifier = Modifier.fillMaxSize(),
                            cursorBrush = SolidColor(GrayMatterColors.Primary),
                            decorationBox = { inner ->
                                if (descriptionInput.isEmpty()) Text("Add context about this source...", color = GrayMatterColors.Neutral700, style = MaterialTheme.typography.bodyMedium)
                                inner()
                            }
                        )
                    }
                }
            }
            
            // Reverted Internal Reflection Section (Simple UI)
            InternalReflectionSection(
                opinionInput = opinionInput,
                onOpinionChange = { opinionInput = it }
            )

            // Confidence Level Section
            ConfidenceLevelSection(
                confidence = confidence,
                onConfidenceChange = { confidence = it }
            )
        }
        
        // Save Button logic: Link or File uploaded allows save; Note requires title.
        val isLinkValid = selectedTab == 0 && urlInput.isNotBlank()
        val isFileValid = selectedTab == 1 && selectedFileUri != null
        val isNoteValid = selectedTab == 2 && titleInput.isNotBlank()
        
        SaveButton(
            onClick = {
                val finalDesc = descriptionInput.takeIf { it.isNotBlank() }
                when (selectedTab) {
                    0 -> onSaveClick(EntryType.LINK, urlInput, opinionInput, (confidence * 100).toInt(), null, finalDesc)
                    1 -> selectedFileUri?.let { uri ->
                        onSaveClick(EntryType.FILE, uri.toString(), opinionInput, (confidence * 100).toInt(), selectedFileName, finalDesc)
                    }
                    2 -> {
                        // For notes, we pass noteContent as the file content (will be saved as .md)
                        onSaveClick(EntryType.NOTE, noteContent, opinionInput, (confidence * 100).toInt(), "${titleInput}.md", finalDesc)
                    }
                }
            },
            enabled = (isLinkValid || isFileValid || isNoteValid) && !isSaving,
            isLoading = isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
        )
    }
}

enum class EntryType { LINK, FILE, NOTE }

@Composable
private fun NewEntryHeader(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Default.Close, "Close", tint = GrayMatterColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
        Text("New Resource", style = MaterialTheme.typography.titleLarge, color = GrayMatterColors.TextPrimary, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun SourceMaterialSection(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    urlInput: String,
    onUrlChange: (String) -> Unit,
    selectedFileName: String?,
    onPickFile: () -> Unit,
    onAddNoteContent: () -> Unit,
    hasNoteContent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SOURCE MATERIAL", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = GrayMatterColors.TextSecondary)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GrayMatterColors.SurfaceBorder)
                .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            TabButton("Link", Icons.Default.Link, selectedTab == 0, { onTabChange(0) }, Modifier.weight(1f))
            TabButton("File", Icons.Default.Description, selectedTab == 1, { onTabChange(1) }, Modifier.weight(1f))
            TabButton("Note", Icons.Default.Note, selectedTab == 2, { onTabChange(2) }, Modifier.weight(1f))
        }
        
        when (selectedTab) {
            0 -> InputField(urlInput, onUrlChange, "https://example.com", Icons.Default.Public)
            1 -> FilePickerField(selectedFileName, onPickFile)
            2 -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField(titleInput, onTitleChange, "Note Title", Icons.Default.Title)
                    AddNoteContentButton(onClick = onAddNoteContent, hasContent = hasNoteContent)
                }
            }
        }
    }
}

@Composable
private fun AddNoteContentButton(onClick: () -> Unit, hasContent: Boolean) {
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
                imageVector = if (hasContent) Icons.Default.EditNote else Icons.Default.Add,
                contentDescription = null,
                tint = GrayMatterColors.Primary
            )
            Text(
                text = if (hasContent) "Edit Note Content" else "Add Note Content",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterColors.Primary
            )
        }
    }
}

@Composable
private fun InputField(value: String, onValueChange: (String) -> Unit, placeholder: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GrayMatterColors.SurfaceInput).border(1.dp, GrayMatterColors.SurfaceBorder, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = GrayMatterColors.Neutral500, modifier = Modifier.size(20.dp))
            BasicTextField(value = value, onValueChange = onValueChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary), modifier = Modifier.weight(1f), cursorBrush = SolidColor(GrayMatterColors.Primary), decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, color = GrayMatterColors.Neutral600, style = MaterialTheme.typography.bodyLarge); inner() })
        }
    }
}

@Composable
private fun FilePickerField(fileName: String?, onPickFile: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GrayMatterColors.SurfaceInput).border(2.dp, GrayMatterColors.SurfaceBorder, RoundedCornerShape(12.dp)).clickable(onClick = onPickFile).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (fileName != null) Icons.Default.AttachFile else Icons.Default.CloudUpload, null, tint = if (fileName != null) GrayMatterColors.Primary else GrayMatterColors.Neutral600, modifier = Modifier.size(if (fileName != null) 32.dp else 40.dp))
            Text(fileName ?: "Choose a file", style = MaterialTheme.typography.bodyMedium, color = if (fileName != null) GrayMatterColors.TextPrimary else GrayMatterColors.TextSecondary)
        }
    }
}

@Composable
private fun TabButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) GrayMatterColors.Primary else Color.Transparent).clickable(onClick = onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (selected) GrayMatterColors.OnPrimary else GrayMatterColors.TextSecondary)
            Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), color = if (selected) GrayMatterColors.OnPrimary else GrayMatterColors.TextSecondary)
        }
    }
}

@Composable
private fun InternalReflectionSection(opinionInput: String, onOpinionChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("INTERNAL REFLECTION", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = GrayMatterColors.TextSecondary)
        Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(GrayMatterColors.SurfaceInput).border(1.dp, GrayMatterColors.SurfaceBorder, RoundedCornerShape(12.dp)).padding(16.dp)) {
            BasicTextField(value = opinionInput, onValueChange = onOpinionChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary, lineHeight = 24.sp), modifier = Modifier.fillMaxSize(), cursorBrush = SolidColor(GrayMatterColors.Primary), decorationBox = { inner -> if (opinionInput.isEmpty()) Text("Type your understanding here...", color = GrayMatterColors.Neutral600, style = MaterialTheme.typography.bodyLarge); inner() })
        }
    }
}

@Composable
private fun ConfidenceLevelSection(confidence: Float, onConfidenceChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CONFIDENCE LEVEL", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = GrayMatterColors.TextSecondary)
            Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.TextPrimary)
        }
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GrayMatterColors.SurfaceInput).border(1.dp, GrayMatterColors.SurfaceBorder, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Slider(value = confidence, onValueChange = onConfidenceChange, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = GrayMatterColors.Primary, activeTrackColor = GrayMatterColors.Primary, inactiveTrackColor = GrayMatterColors.Neutral800))
        }
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit, enabled: Boolean, isLoading: Boolean, modifier: Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Primary, contentColor = GrayMatterColors.OnPrimary, disabledContainerColor = GrayMatterColors.Neutral700, disabledContentColor = GrayMatterColors.Neutral500)) {
        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GrayMatterColors.OnPrimary, strokeWidth = 2.dp)
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
            Text("Save Entry", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
