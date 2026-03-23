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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.components.MarkdownEditor
import com.example.graymatter.domain.CustomTemplate
import java.util.Locale

/**
 * New Resource Screen.
 * Allows user to add a resource (link/file/note), an optional description, and first opinion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    viewModel: com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel,
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel,
    preSelectedTopicId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToAddToTopic: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val isSaving by viewModel.isImporting.collectAsState()
    val templates by viewModel.templates.collectAsState()
    
    var entryType by remember { mutableStateOf(EntryType.LINK) } // 0 = Link, 1 = File, 2 = Note
    var title by remember { mutableStateOf("") }
    var urlValue by remember { mutableStateOf("") }
    var opinionText by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var confidenceScore by remember { mutableFloatStateOf(0.7f) }

    var originalFileName by remember { mutableStateOf<String?>(null) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var showDescription by remember { mutableStateOf(false) }
    var isNoteEditorOpen by remember { mutableStateOf(false) }
    
    // Custom Template State
    var selectedTemplate by remember { mutableStateOf<CustomTemplate?>(null) }
    var templateFieldValues by remember { mutableStateOf(mapOf<String, String>()) }

    // Reference Selector State
    var showReferenceSelector by remember { mutableStateOf(false) }
    var selectedReferences by remember { mutableStateOf<List<com.example.graymatter.domain.ReferenceSelectorItem>>(emptyList()) }
    var referenceToInsert by remember { mutableStateOf<String?>(null) }

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

            fileUri = it
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        originalFileName = c.getString(nameIndex)
                    }
                }
            }
            if (originalFileName == null) {
                originalFileName = it.lastPathSegment ?: "Unknown file"
            }
            
            // Auto-fill title from filename if blank
            if (title.isBlank()) {
                title = originalFileName?.substringBeforeLast('.') ?: ""
            }
        }
    }

    // Auto-fill title from URL if blank
    LaunchedEffect(urlValue) {
        if (entryType == EntryType.LINK && urlValue.isNotBlank() && title.isBlank()) {
            title = inferTitleFromUrl(urlValue)
        }
    }

    if (isNoteEditorOpen) {
        MarkdownEditor(
            title = if (title.isBlank()) "New Note" else title,
            initialText = noteContent,
            onBackClick = { isNoteEditorOpen = false },
            onSave = { content ->
                noteContent = content
                isNoteEditorOpen = false
            },
            onShowReferenceSelector = { showReferenceSelector = true },
            referenceToInsert = referenceToInsert,
            onReferenceInserted = { referenceToInsert = null }
        )

        if (showReferenceSelector) {
            com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
                viewModel = referenceSelectorViewModel,
                onDismissRequest = { showReferenceSelector = false },
                onConfirm = { items ->
                    showReferenceSelector = false
                    if (items.isNotEmpty()) {
                        val item = items.first()
                        val text = when (item) {
                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> "Topic: ${item.name}"
                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> "Resource: ${item.title}"
                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> "Annotation: ${item.snippet.take(15)}..."
                        }
                        referenceToInsert = "[[$text]]"
                        // Also add to the global selected references for this entry
                        selectedReferences = (selectedReferences + items).distinctBy { it.id }
                    }
                }
            )
        }

        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
    ) {
        // Header
        NewEntryHeader(onBackClick = onNavigateBack, modifier = Modifier.statusBarsPadding())
        
        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Source Material Section
            SourceMaterialSection(
                selectedTab = entryType.ordinal,
                onTabChange = { 
                    entryType = EntryType.entries[it]
                },
                titleInput = title,
                onTitleChange = { title = it },
                urlInput = urlValue,
                onUrlChange = { urlValue = it },
                selectedFileName = originalFileName,
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
                            description,
                            onValueChange = { description = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = GrayMatterColors.TextPrimary),
                            modifier = Modifier.fillMaxSize(),
                            cursorBrush = SolidColor(GrayMatterColors.Primary),
                            decorationBox = { inner ->
                                if (description.isEmpty()) Text("Add context about this source...", color = GrayMatterColors.Neutral700, style = MaterialTheme.typography.bodyMedium)
                                inner()
                            }
                        )
                    }
                }
            }
            
            // Grouped Opinion and Confidence Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selectedTemplate != null) GrayMatterColors.CustomizedAccent.copy(alpha = 0.1f)
                        else GrayMatterColors.AppleGreen.copy(alpha = 0.1f)
                    )
                    .border(
                        1.dp, 
                        if (selectedTemplate != null) GrayMatterColors.CustomizedAccent.copy(alpha = 0.3f)
                        else GrayMatterColors.Citrine.copy(alpha = 0.3f), 
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Opinion Section
                CustomizedOpinionSection(
                    opinionInput = opinionText,
                    onOpinionChange = { opinionText = it },
                    templates = templates,
                    selectedTemplate = selectedTemplate,
                    onTemplateSelect = { 
                        selectedTemplate = it
                        it?.let { template ->
                            templateFieldValues = template.headings.associateWith { "" }
                            // template.relatedTo ignored

                        }
                    },
                    templateFieldValues = templateFieldValues,
                    onFieldValueChange = { heading, value ->
                        templateFieldValues = templateFieldValues.toMutableMap().apply { put(heading, value) }
                    }
                )

                // Knowledge Connections Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("KNOWLEDGE LINKS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = GrayMatterColors.TextSecondary)
                        TextButton(onClick = { showReferenceSelector = true }, contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Default.AddLink, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add connection")
                        }
                    }
                    if (selectedReferences.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            selectedReferences.forEach { ref ->
                                val text = when (ref) {
                                    is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                                    is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                                    is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                                }
                                InputChip(
                                    selected = true,
                                    onClick = { selectedReferences = selectedReferences.filter { it.id != ref.id } },
                                    label = { Text(text, maxLines = 1) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    } else {
                        Text("No specific links added.", color = GrayMatterColors.Neutral600, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Confidence Level Section
                ConfidenceLevelSection(
                    confidence = confidenceScore,
                    onConfidenceChange = { confidenceScore = it },
                    accentColor = if (selectedTemplate != null) GrayMatterColors.CustomizedAccent else GrayMatterColors.AppleGreen
                )
            }
        }
        
        // Save Button logic
        val isLinkValid = entryType == EntryType.LINK && urlValue.isNotBlank()
        val isFileValid = entryType == EntryType.FILE && fileUri != null
        val isNoteValid = entryType == EntryType.NOTE && title.isNotBlank()
        
        val isOpinionValid = if (selectedTemplate != null) {
            templateFieldValues.values.any { it.isNotBlank() }
        } else {
            opinionText.isNotBlank()
        }
        
        SaveButton(
            onClick = {
                coroutineScope.launch {
                    val finalDesc = description.takeIf { it.isNotBlank() }
                    
                    val finalOpinion = if (selectedTemplate != null) {
                        val sb = StringBuilder()
                        sb.appendLine("[TEMPLATE:${selectedTemplate!!.name}]")
                        selectedTemplate!!.headings.forEach { heading ->
                            val value = templateFieldValues[heading] ?: ""
                            if (value.isNotBlank()) {
                                sb.appendLine("### $heading")
                                sb.appendLine(value)
                                sb.appendLine()
                            }
                        }
                        sb.toString().trim()
                    } else {
                        opinionText
                    }

                    val newItemId = when (entryType) {
                        EntryType.LINK -> viewModel.createNewItem(
                            url = urlValue,
                            opinionText = finalOpinion,
                            confidence = (confidenceScore * 10).toInt(),
                            title = title.ifBlank { null },
                            description = finalDesc,
                            topicId = preSelectedTopicId,
                            referenceLinks = selectedReferences
                        )
                        EntryType.FILE -> {
                            val finalTitle = if (title.isNotBlank() && originalFileName != null && !title.contains(".")) {
                                val ext = originalFileName!!.substringAfterLast('.', "")
                                if (ext.isNotEmpty()) "$title.$ext" else title
                            } else if (title.isNotBlank()) title else originalFileName ?: "Unknown"

                            viewModel.createNewItemFromFile(
                                context = context,
                                fileName = originalFileName ?: "Unknown",
                                uri = fileUri ?: Uri.EMPTY,
                                opinionText = finalOpinion,
                                confidence = (confidenceScore * 10).toInt(),
                                title = finalTitle,
                                description = finalDesc,
                                topicId = preSelectedTopicId,
                                referenceLinks = selectedReferences
                            )
                        }
                        EntryType.NOTE -> {
                            val finalTitle = if (title.isNotBlank() && !title.endsWith(".md")) "$title.md" else (title.ifBlank { "Untitled.md" })
                            viewModel.createNewNote(
                                context = context,
                                title = finalTitle,
                                content = noteContent,
                                opinionText = finalOpinion,
                                confidence = (confidenceScore * 10).toInt(),
                                description = finalDesc,
                                topicId = preSelectedTopicId,
                                referenceLinks = selectedReferences
                            )
                        }
                    }

                    if (newItemId != null) {
                        if (preSelectedTopicId != null) {
                            onNavigateToHome()
                        } else {
                            onNavigateToAddToTopic(newItemId)
                        }
                    }
                }
            },
            enabled = (isLinkValid || isFileValid || isNoteValid) && isOpinionValid && !isSaving,
            isLoading = isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
        )
    }

    if (showReferenceSelector) {
        com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
            viewModel = referenceSelectorViewModel,
            onDismissRequest = { showReferenceSelector = false },
            onConfirm = { items ->
                showReferenceSelector = false
                selectedReferences = (selectedReferences + items).distinctBy { it.id }
            }
        )
    }
}

private fun inferTitleFromUrl(url: String): String {
    return try {
        val uri = Uri.parse(url)
        var clean = url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
        
        if (clean.endsWith("/")) clean = clean.dropLast(1)
        
        // Remove query parameters and fragments
        clean = clean.substringBefore("?").substringBefore("#")
        
        val parts = clean.split("/").filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""

        // Common non-title segments to ignore
        val ignoreKeywords = setOf(
            "articleshow", "article", "post", "blog", "news", "story", "p", "id", 
            "view", "details", "html", "php", "cms", "aspx", "category", "tag", "archives"
        )

        var slug = ""
        
        // Iterate backwards to find the most descriptive part
        for (i in parts.indices.reversed()) {
            val part = parts[i].lowercase()
            
            // Skip domain names (first part usually)
            if (i == 0 && parts.size > 1) continue
            
            // Skip technical IDs or short noise
            if (part.all { it.isDigit() || it == '.' } || 
                part.length < 4 || 
                ignoreKeywords.contains(part.substringBeforeLast(".")) ||
                part.contains("index.")
            ) continue
            
            // If it has hyphens or underscores, it's likely the title slug
            if (part.contains("-") || part.contains("_")) {
                slug = parts[i]
                break
            }
            
            // Fallback to the first non-ignored part from the end
            if (slug.isEmpty()) {
                slug = parts[i]
            }
        }
        
        if (slug.isEmpty()) slug = parts.last()

        // Final cleanup
        var formatted = slug
            .substringBeforeLast(".cms")
            .substringBeforeLast(".html")
            .substringBeforeLast(".php")
            .substringBeforeLast(".htm")
            .replace("-", " ")
            .replace("_", " ")
            .replace(Regex("\\s+"), " ") // Double spaces
            .trim()
        
        // Robust formatting: Title Case
        formatted = formatted.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        
        formatted
    } catch (e: Exception) {
        ""
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
            0 -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField(urlInput, onUrlChange, "https://example.com", Icons.Default.Public)
                    if (urlInput.isNotBlank()) {
                        InputField(titleInput, onTitleChange, "Resource Title", Icons.Default.Title)
                    }
                }
            }
            1 -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilePickerField(selectedFileName, onPickFile)
                    if (selectedFileName != null) {
                        InputField(titleInput, onTitleChange, "Resource Title", Icons.Default.Title)
                    }
                }
            }
            2 -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AddNoteContentButton(onClick = onAddNoteContent, hasContent = hasNoteContent)
                    if (hasNoteContent) {
                        InputField(titleInput, onTitleChange, "Note Title", Icons.Default.Title)
                    }
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
private fun CustomizedOpinionSection(
    opinionInput: String,
    onOpinionChange: (String) -> Unit,
    templates: List<CustomTemplate>,
    selectedTemplate: CustomTemplate?,
    onTemplateSelect: (CustomTemplate?) -> Unit,
    templateFieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit
) {
    val accentColor = if (selectedTemplate != null) GrayMatterColors.CustomizedAccent else GrayMatterColors.AppleGreen
    val bgColor = accentColor.copy(alpha = 0.08f)
    val borderColor = accentColor.copy(alpha = 0.2f)
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (selectedTemplate != null) "CUSTOM ENTRY" else "OPINION",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextSecondary
                )
                Text("(Required)", style = MaterialTheme.typography.labelSmall, color = accentColor.copy(alpha = 0.7f))
            }
            
            TemplateSelector(
                templates = templates,
                selectedTemplate = selectedTemplate,
                onTemplateSelect = onTemplateSelect
            )
        }

        if (selectedTemplate != null) {
            DynamicEntryForm(
                template = selectedTemplate,
                fieldValues = templateFieldValues,
                onFieldValueChange = onFieldValueChange
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(bgColor).border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                BasicTextField(value = opinionInput, onValueChange = onOpinionChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary, lineHeight = 24.sp), modifier = Modifier.fillMaxSize(), cursorBrush = SolidColor(GrayMatterColors.AppleGreen), decorationBox = { inner -> if (opinionInput.isEmpty()) Text("Type your understanding here...", color = GrayMatterColors.Neutral600, style = MaterialTheme.typography.bodyLarge); inner() })
            }
        }
    }
}

@Composable
private fun TemplateSelector(
    templates: List<CustomTemplate>,
    selectedTemplate: CustomTemplate?,
    onTemplateSelect: (CustomTemplate?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { showMenu = true },
            colors = ButtonDefaults.textButtonColors(contentColor = GrayMatterColors.Neutral500)
        ) {
            Icon(Icons.Default.DashboardCustomize, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedTemplate?.name ?: "Use Template",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Icon(Icons.Default.ArrowDropDown, null)
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(GrayMatterColors.SurfaceDark)
        ) {
            DropdownMenuItem(
                text = { Text("None (Plain Opinion)", color = Color.White) },
                onClick = {
                    onTemplateSelect(null)
                    showMenu = false
                }
            )
            templates.forEach { template ->
                DropdownMenuItem(
                    text = { Text(template.name, color = Color.White) },
                    onClick = {
                        onTemplateSelect(template)
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun DynamicEntryForm(
    template: CustomTemplate,
    fieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        template.headings.forEach { heading ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = heading.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = GrayMatterColors.Neutral500
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GrayMatterColors.Neutral900)
                        .border(
                            width = 1.dp,
                            color = GrayMatterColors.Neutral800,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = fieldValues[heading] ?: "",
                        onValueChange = { onFieldValueChange(heading, it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(GrayMatterColors.CustomizedAccent),
                        decorationBox = { inner ->
                            if ((fieldValues[heading] ?: "").isEmpty()) {
                                Text("Enter $heading...", color = GrayMatterColors.Neutral700)
                            }
                            inner()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfidenceLevelSection(
    confidence: Float, 
    onConfidenceChange: (Float) -> Unit,
    accentColor: Color = GrayMatterColors.AppleGreen
) {
    val bgColor = accentColor.copy(alpha = 0.08f)
    val borderColor = accentColor.copy(alpha = 0.2f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CONFIDENCE LEVEL", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = GrayMatterColors.TextSecondary)
            Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.TextPrimary)
        }
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bgColor).border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Slider(value = confidence, onValueChange = onConfidenceChange, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = GrayMatterColors.Neutral800))
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
