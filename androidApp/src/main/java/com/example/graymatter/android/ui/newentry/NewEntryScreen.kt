package com.example.graymatter.android.ui.newentry

import com.example.graymatter.android.ui.theme.GrayMatterTheme
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.graymatter.android.ui.components.TemplateSelector
import com.example.graymatter.android.ui.components.DynamicEntryForm
import com.example.graymatter.android.ui.viewmodel.DraftingViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
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
    templateViewModel: com.example.graymatter.android.ui.viewmodel.TemplateViewModel,
    draftingViewModel: com.example.graymatter.android.ui.viewmodel.DraftingViewModel,
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel,
    preSelectedTopicId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToAddToTopic: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val isSaving by draftingViewModel.isImporting.collectAsState()
    val templates by templateViewModel.templates.collectAsState()

    val entryType by draftingViewModel.entryType.collectAsState()
    val title by draftingViewModel.draftTitle.collectAsState()
    val urlValue by draftingViewModel.draftUrl.collectAsState()
    val opinionText by draftingViewModel.draftOpinion.collectAsState()
    val noteContent by draftingViewModel.draftNoteContent.collectAsState()
    val description by draftingViewModel.draftDescription.collectAsState()
    val confidenceScore by draftingViewModel.draftConfidence.collectAsState()
    val currentImagePath by draftingViewModel.draftImagePath.collectAsState()

    var showImageSourcePicker by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var originalFileName by remember { mutableStateOf<String?>(null) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var showDescription by remember { mutableStateOf(false) }
    var isNoteEditorOpen by remember { mutableStateOf(false) }
    
    // Custom Template State
    var selectedTemplate by remember { mutableStateOf<CustomTemplate?>(null) }
    var templateFieldValues by remember { mutableStateOf(mapOf<String, String>()) }

    // Reference Selector State
    var showReferenceSelector by remember { mutableStateOf(false) }
    var noteSelectedReferences by remember { mutableStateOf<List<com.example.graymatter.domain.ReferenceSelectorItem>>(emptyList()) }
    var opinionSelectedReferences by remember { mutableStateOf<List<com.example.graymatter.domain.ReferenceSelectorItem>>(emptyList()) }
    var referenceToInsert by remember { mutableStateOf<String?>(null) }
    var showTemplateEditor by remember { mutableStateOf(false) }

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
            
            // Refined title extraction: Remove special characters (_ -) and apply Title Case
            val extractedTitle = originalFileName?.substringBeforeLast('.')
                ?.replace(Regex("[_\\-]"), " ") // Replace underscores and hyphens with spaces
                ?.replace(Regex("\\s+"), " ") // Normalize multiple spaces
                ?.trim()
                ?.split(" ")
                ?.filter { it.isNotBlank() }
                ?.joinToString(" ") { word -> 
                    word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } 
                }
                ?: ""

            if (extractedTitle.isNotBlank()) {
                draftingViewModel.updateTitle(extractedTitle)
            }
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = com.example.graymatter.android.util.FileUtils.copyUriToInternalStorage(context, it, "visual_entry_${java.util.UUID.randomUUID()}.jpg")
            draftingViewModel.updateImagePath(path)
            selectedTemplate = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val path = com.example.graymatter.android.util.FileUtils.copyUriToInternalStorage(context, tempCameraUri!!, "visual_entry_${java.util.UUID.randomUUID()}.jpg")
            draftingViewModel.updateImagePath(path)
            selectedTemplate = null
        }
    }

    // Removed: Auto-fill title from URL (now manual via "Load" button)
    /*
    LaunchedEffect(urlValue) {
        if (entryType == EntryType.LINK && urlValue.isNotBlank() && title.isBlank()) {
            title = inferTitleFromUrl(urlValue)
        }
    }
    */

    val onShowReferenceSelector = {
        referenceSelectorViewModel.clearSelection()
        showReferenceSelector = true
    }
    
    if (isNoteEditorOpen) {
        MarkdownEditor(
            title = title, // Pass the current title (will show placeholder if empty)
            initialText = noteContent,
            onBackClick = { isNoteEditorOpen = false },
            onSave = { content ->
                draftingViewModel.updateNoteContent(content)
                isNoteEditorOpen = false
            },
            onTextChange = { content ->
                // Live sync for background display and pruning
                draftingViewModel.updateNoteContent(content)
            },
            onTitleChange = { draftingViewModel.updateTitle(it) },
            onShowReferenceSelector = {
                onShowReferenceSelector()
            },
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
                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> "Knowledge: ${item.snippet.take(15)}..."
                        }
                        referenceToInsert = "[[$text]]"
                        // Also add to the note references
                        noteSelectedReferences = (noteSelectedReferences + items).distinctBy { it.id }
                    }
                }
            )
        }

        return
    }

    // Auto-sync note selected references with content: remove if [[text]] is deleted
    // Improved Sync & Re-hydration logic
    androidx.compose.runtime.LaunchedEffect(noteContent, isNoteEditorOpen) {
        // Only run synchronization when the editor is NOT open or just closed
        if (isNoteEditorOpen) return@LaunchedEffect
        
        val regex = Regex("\\[\\[(.*?)\\]\\]")
        val foundTexts = regex.findAll(noteContent).map { it.groupValues[1] }.toSet()
        
        noteSelectedReferences = noteSelectedReferences.filter { ref ->
            val refText = when (ref) {
                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
            }
            // Some refs might have prefixes like "Topic: " or "Resource: " from previous insertion logic
            // We check both the raw text and the prefixed version
            foundTexts.contains(refText) || 
            foundTexts.any { it.endsWith(refText) }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayMatterTheme.colors.background)
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
                    draftingViewModel.updateEntryType(DraftingViewModel.EntryType.values()[it])
                },
                titleInput = title,
                onTitleChange = { draftingViewModel.updateTitle(it) },
                urlInput = urlValue,
                onUrlChange = { draftingViewModel.updateUrl(it) },
                onLoadTitle = { 
                    draftingViewModel.updateTitle(draftingViewModel.extractTitleFromUrl(urlValue))
                },
                onClearUrl = {
                    draftingViewModel.updateUrl("")
                    draftingViewModel.updateTitle("")
                },
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
                        tint = GrayMatterTheme.colors.neutral500,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Description (optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = GrayMatterTheme.colors.neutral500
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
                            .background(GrayMatterTheme.colors.surfaceInput)
                            .border(1.dp, GrayMatterTheme.colors.surfaceBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            description,
                            onValueChange = { draftingViewModel.updateDescription(it) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = GrayMatterTheme.colors.textPrimary),
                            modifier = Modifier.fillMaxSize(),
                            cursorBrush = SolidColor(GrayMatterTheme.colors.primary),
                            decorationBox = { inner ->
                                if (description.isEmpty()) Text("Add context about this source...", color = GrayMatterTheme.colors.neutral700, style = MaterialTheme.typography.bodyMedium)
                                inner()
                            }
                        )
                    }
                }
            }

            // KNOWLEDGE LINKS display section — for notes, shown between source material and opinion
            if (entryType == DraftingViewModel.EntryType.NOTE) {
                // Auto-extract [[...]] references from noteContent
                val autoExtractedRefs = remember(noteContent, noteSelectedReferences) {
                    val regex = Regex("\\[\\[(.*?)\\]\\]")
                    val rawHits = regex.findAll(noteContent).map { it.groupValues[1] }.toList()
                    
                    // Filter out hits that are already in noteSelectedReferences to avoid duplicates
                    val manualRefTexts = noteSelectedReferences.map { ref ->
                        when (ref) {
                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                        }
                    }.toSet()
                    
                    rawHits.filter { hit ->
                        manualRefTexts.none { it == hit || hit.endsWith(it) }
                    }.distinct()
                }

                if (autoExtractedRefs.isNotEmpty() || noteSelectedReferences.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GrayMatterTheme.colors.primary.copy(alpha = 0.06f))
                            .border(1.dp, GrayMatterTheme.colors.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Link,
                                null,
                                tint = GrayMatterTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "KNOWLEDGE LINKS",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = GrayMatterTheme.colors.textSecondary
                            )
                        }

                        // Auto-extracted [[...]] references from note content
                        if (autoExtractedRefs.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                autoExtractedRefs.forEach { ref ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GrayMatterTheme.colors.primary.copy(alpha = 0.12f),
                                        modifier = Modifier.border(
                                            0.5.dp,
                                            GrayMatterTheme.colors.primary.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Tag,
                                                null,
                                                tint = GrayMatterTheme.colors.primary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                ref,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GrayMatterTheme.colors.primary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Manually added references within the note
                        if (noteSelectedReferences.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                            noteSelectedReferences.forEach { ref ->
                                val text = when (ref) {
                                    is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                                    is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                                    is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GrayMatterColors.TypeLink.copy(alpha = 0.12f),
                                    modifier = Modifier.border(
                                        0.5.dp,
                                        GrayMatterColors.TypeLink.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Text(
                                        text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GrayMatterColors.TypeLink,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                        Text(
                            "Links are extracted from note content. Edit within the note.",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterTheme.colors.neutral600
                        )
                    }
                }
            }
            
            // Grouped Opinion and Confidence Container
            val entryAccentColor = when {
                currentImagePath != null -> GrayMatterColors.TypeVisual
                selectedTemplate != null -> GrayMatterColors.TypeTemplate
                else -> GrayMatterColors.TypeOpinion
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(entryAccentColor.copy(alpha = 0.1f))
                    .border(1.dp, entryAccentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Opinion Section
                CustomizedOpinionSection(
                    opinionInput = opinionText,
                    onOpinionChange = { draftingViewModel.updateOpinion(it) },
                    templates = templates,
                    selectedTemplate = selectedTemplate,
                    onTemplateSelect = { template ->
                        selectedTemplate = template
                        if (template != null) {
                            // Selecting a template clears image mode
                            draftingViewModel.updateImagePath(null)
                            templateFieldValues = template.headings.associateWith { "" }
                        }
                    },
                    templateFieldValues = templateFieldValues,
                    onFieldValueChange = { heading, value ->
                        templateFieldValues = templateFieldValues.toMutableMap().apply { put(heading, value) }
                    },
                    onCreateTemplate = { showTemplateEditor = true },
                    onShowImageSourcePicker = { showImageSourcePicker = true },
                    currentImagePath = currentImagePath,
                    onImagePathChange = { path ->
                        draftingViewModel.updateImagePath(path)
                        if (path != null) {
                            // Picking an image clears template mode
                            selectedTemplate = null
                        }
                    }
                )

                // Knowledge Connections Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("KNOWLEDGE LINKS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.textSecondary)
                        TextButton(
                            onClick = { 
                                referenceSelectorViewModel.clearSelection()
                                showReferenceSelector = true 
                            }, 
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.AddLink, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add connection")
                        }
                    }
                    if (opinionSelectedReferences.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            opinionSelectedReferences.forEach { ref ->
                                val text = when (ref) {
                                    is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                                    is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                                    is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                                }
                                InputChip(
                                    selected = true,
                                    onClick = { opinionSelectedReferences = opinionSelectedReferences.filter { it.id != ref.id } },
                                    label = { Text(text, maxLines = 1) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    } else {
                        Text("No specific links added.", color = GrayMatterTheme.colors.neutral600, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Confidence Level Section
                ConfidenceLevelSection(
                    confidence = confidenceScore,
                    onConfidenceChange = { draftingViewModel.updateConfidence(it) },
                    accentColor = entryAccentColor
                )
        
        // Save Button logic
        val isLinkValid = entryType == DraftingViewModel.EntryType.LINK && urlValue.isNotBlank()
        val isFileValid = entryType == DraftingViewModel.EntryType.FILE && fileUri != null
        val isNoteValid = entryType == DraftingViewModel.EntryType.NOTE && title.isNotBlank()
        
        val isOpinionValid = true
        
        SaveButton(
            onClick = {
                coroutineScope.launch {
                    val finalDesc = description.takeIf { it.isNotBlank() }
                    
                    val finalOpinion = if (selectedTemplate != null && currentImagePath == null) {
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
                        DraftingViewModel.EntryType.LINK -> draftingViewModel.createNewResourceEntry(
                            url = urlValue,
                            opinionText = finalOpinion,
                            confidence = (confidenceScore * 100).toInt(),
                            title = title.ifBlank { null },
                            description = finalDesc,
                            topicId = preSelectedTopicId,
                            referenceLinks = opinionSelectedReferences,
                            imagePath = currentImagePath
                        )
                        DraftingViewModel.EntryType.FILE -> {
                            val finalTitle = if (title.isNotBlank() && originalFileName != null && !title.contains(".")) {
                                val ext = originalFileName!!.substringAfterLast('.', "")
                                if (ext.isNotEmpty()) "$title.$ext" else title
                            } else if (title.isNotBlank()) title else originalFileName ?: "Unknown"

                            draftingViewModel.createNewResourceEntryFromFile(
                                context = context,
                                fileName = originalFileName ?: "Unknown",
                                uri = fileUri ?: Uri.EMPTY,
                                opinionText = finalOpinion,
                                confidence = (confidenceScore * 100).toInt(),
                                title = finalTitle,
                                description = finalDesc,
                                topicId = preSelectedTopicId,
                                referenceLinks = opinionSelectedReferences,
                                imagePath = currentImagePath
                            )
                        }
                        DraftingViewModel.EntryType.NOTE -> {
                            val finalTitle = if (title.isNotBlank() && !title.endsWith(".md")) "$title.md" else (title.ifBlank { "Untitled.md" })
                            draftingViewModel.createNewNote(
                                context = context,
                                title = finalTitle,
                                content = noteContent,
                                opinionText = finalOpinion,
                                confidence = (confidenceScore * 100).toInt(),
                                description = finalDesc,
                                topicId = preSelectedTopicId,
                                referenceLinks = noteSelectedReferences,
                                opinionReferenceLinks = opinionSelectedReferences,
                                imagePath = currentImagePath
                            )
                        }
                    }

                    if (newItemId != null) {
                        draftingViewModel.resetDraft()
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
}

    if (showReferenceSelector) {
        com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
            viewModel = referenceSelectorViewModel,
            onDismissRequest = { showReferenceSelector = false },
            onConfirm = { items ->
                showReferenceSelector = false
                opinionSelectedReferences = (opinionSelectedReferences + items).distinctBy { it.id }
            }
        )
    }

    if (showTemplateEditor) {
        com.example.graymatter.android.ui.components.TemplateEditorDialog(
            template = com.example.graymatter.domain.CustomTemplate(java.util.UUID.randomUUID().toString(), "", listOf("")),
            onDismiss = { showTemplateEditor = false },
            onSave = { updated ->
                templateViewModel.saveTemplate(updated)
                showTemplateEditor = false
            }
        )
    }

    if (showImageSourcePicker) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImageSourcePicker = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { showImageSourcePicker = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(GrayMatterTheme.colors.surface)
                        .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable(enabled = false) {} // prevent dismiss when tapping sheet
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Add Image", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.textPrimary)
                            IconButton(onClick = { showImageSourcePicker = false }) {
                                Icon(Icons.Default.Close, null, tint = GrayMatterTheme.colors.neutral600)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Take Photo option
                        Surface(
                            onClick = {
                                showImageSourcePicker = false
                                tempCameraUri = com.example.graymatter.android.util.FileUtils.createTempImageUri(context)
                                tempCameraUri?.let { cameraLauncher.launch(it) }
                            },
                            color = GrayMatterTheme.colors.surfaceInput,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).background(GrayMatterColors.TypeVisual.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoCamera, null, tint = GrayMatterColors.TypeVisual, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text("Take Photo", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = GrayMatterTheme.colors.textPrimary)
                                    Text("Capture with camera", style = MaterialTheme.typography.bodySmall, color = GrayMatterTheme.colors.neutral500)
                                }
                            }
                        }

                        // Gallery option
                        Surface(
                            onClick = {
                                showImageSourcePicker = false
                                galleryPickerLauncher.launch("image/*")
                            },
                            color = GrayMatterTheme.colors.surfaceInput,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).background(GrayMatterColors.TypeVisual.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, null, tint = GrayMatterColors.TypeVisual, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text("Choose from Gallery", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = GrayMatterTheme.colors.textPrimary)
                                    Text("Pick an existing image", style = MaterialTheme.typography.bodySmall, color = GrayMatterTheme.colors.neutral500)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
    }
}


@Composable
private fun NewEntryHeader(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Default.Close, "Close", tint = GrayMatterTheme.colors.textPrimary, modifier = Modifier.size(24.dp))
        }
        Text("New Resource", style = MaterialTheme.typography.titleLarge, color = GrayMatterTheme.colors.textPrimary, modifier = Modifier.align(Alignment.Center))
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
    onLoadTitle: () -> Unit,
    onClearUrl: () -> Unit,
    selectedFileName: String?,
    onPickFile: () -> Unit,
    onAddNoteContent: () -> Unit,
    hasNoteContent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SOURCE MATERIAL", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.textSecondary)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GrayMatterTheme.colors.surfaceBorder)
                .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            TabButton("Link", Icons.Default.Link, selectedTab == 0, { onTabChange(0) }, Modifier.weight(1f))
            TabButton("File", Icons.Default.Description, selectedTab == 1, { onTabChange(1) }, Modifier.weight(1f))
            TabButton("Note", Icons.Default.Note, selectedTab == 2, { onTabChange(2) }, Modifier.weight(1f))
        }
        
        when (selectedTab) {
            0 -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField(
                        value = urlInput, 
                        onValueChange = { 
                            onUrlChange(it)
                            // Clear title if URL is edited to force re-load/verification
                            if (titleInput.isNotEmpty()) onTitleChange("") 
                        }, 
                        placeholder = "https://example.com", 
                        leadingIcon = Icons.Default.Public,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (urlInput.isNotBlank()) {
                                    IconButton(
                                        onClick = onLoadTitle,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowForward, 
                                            "Load Title", 
                                            tint = GrayMatterTheme.colors.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onClearUrl,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close, 
                                            "Clear", 
                                            tint = GrayMatterTheme.colors.neutral600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
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
            .background(GrayMatterTheme.colors.neutral900)
            .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = if (hasContent) Icons.Default.EditNote else Icons.Default.Add,
                contentDescription = null,
                tint = GrayMatterTheme.colors.primary
            )
            Text(
                text = if (hasContent) "Edit Note Content" else "Add Note Content",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterTheme.colors.primary
            )
        }
    }
}

@Composable
private fun InputField(
    value: String, 
    onValueChange: (String) -> Unit, 
    placeholder: String, 
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GrayMatterTheme.colors.surfaceInput).border(1.dp, GrayMatterTheme.colors.surfaceBorder, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(leadingIcon, null, tint = GrayMatterTheme.colors.neutral500, modifier = Modifier.size(20.dp))
            BasicTextField(
                value = value, 
                onValueChange = onValueChange, 
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterTheme.colors.textPrimary), 
                modifier = Modifier.weight(1f), 
                cursorBrush = SolidColor(GrayMatterTheme.colors.primary), 
                decorationBox = { inner -> 
                    if (value.isEmpty()) Text(placeholder, color = GrayMatterTheme.colors.neutral600, style = MaterialTheme.typography.bodyLarge)
                    inner() 
                }
            )
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun FilePickerField(fileName: String?, onPickFile: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GrayMatterTheme.colors.surfaceInput).border(2.dp, GrayMatterTheme.colors.surfaceBorder, RoundedCornerShape(12.dp)).clickable(onClick = onPickFile).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (fileName != null) Icons.Default.AttachFile else Icons.Default.CloudUpload, null, tint = if (fileName != null) GrayMatterTheme.colors.primary else GrayMatterTheme.colors.neutral600, modifier = Modifier.size(if (fileName != null) 32.dp else 40.dp))
            Text(fileName ?: "Choose a file", style = MaterialTheme.typography.bodyMedium, color = if (fileName != null) GrayMatterTheme.colors.textPrimary else GrayMatterTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun TabButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) GrayMatterTheme.colors.primary else Color.Transparent).clickable(onClick = onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (selected) GrayMatterTheme.colors.onPrimary else GrayMatterTheme.colors.textSecondary)
            Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), color = if (selected) GrayMatterTheme.colors.onPrimary else GrayMatterTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun CustomizedOpinionSection(
    opinionInput: String,
    onOpinionChange: (String) -> Unit,
    templates: List<com.example.graymatter.domain.CustomTemplate>,
    selectedTemplate: com.example.graymatter.domain.CustomTemplate?,
    onTemplateSelect: (com.example.graymatter.domain.CustomTemplate?) -> Unit,
    templateFieldValues: Map<String, String>,
    onFieldValueChange: (String, String) -> Unit,
    onCreateTemplate: () -> Unit,
    onShowImageSourcePicker: () -> Unit,
    currentImagePath: String?,
    onImagePathChange: (String?) -> Unit
) {
    val isVisualMode = currentImagePath != null
    val accentColor = when {
        isVisualMode -> GrayMatterColors.TypeVisual
        selectedTemplate != null -> GrayMatterColors.TypeTemplate
        else -> GrayMatterColors.TypeOpinion
    }
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
                    text = when {
                        isVisualMode -> "VISUAL"
                        selectedTemplate != null -> "CUSTOM ENTRY"
                        else -> "OPINION"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                    color = GrayMatterTheme.colors.textSecondary
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onShowImageSourcePicker,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.AddAPhoto, 
                        "Add Image", 
                        tint = GrayMatterTheme.colors.textSecondary, 
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (!isVisualMode) {
                    TemplateSelector(
                        templates = templates,
                        selectedTemplate = selectedTemplate,
                        onTemplateSelect = onTemplateSelect,
                        onCreateTemplate = onCreateTemplate
                    )
                }
            }
        }

        if (isVisualMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GrayMatterTheme.colors.neutral900)
            ) {
                AsyncImage(
                    model = java.io.File(currentImagePath!!),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Remove image button
                IconButton(
                    onClick = { onImagePathChange(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // Caption field (optional for visual entries)
            BasicTextField(
                value = opinionInput,
                onValueChange = onOpinionChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = GrayMatterTheme.colors.textPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GrayMatterTheme.colors.surfaceInput, RoundedCornerShape(12.dp))
                    .border(1.dp, GrayMatterColors.TypeVisual.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                cursorBrush = SolidColor(GrayMatterColors.TypeVisual),
                decorationBox = { inner ->
                    if (opinionInput.isEmpty()) Text("Add a caption (optional)...", color = GrayMatterTheme.colors.neutral600, style = MaterialTheme.typography.bodyMedium)
                    inner()
                }
            )
        } else if (selectedTemplate != null) {
            DynamicEntryForm(
                template = selectedTemplate,
                fieldValues = templateFieldValues,
                onFieldValueChange = onFieldValueChange
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(bgColor).border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                BasicTextField(
                    value = opinionInput, 
                    onValueChange = onOpinionChange, 
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterTheme.colors.textPrimary, lineHeight = 24.sp), 
                    modifier = Modifier.fillMaxSize(), 
                    cursorBrush = SolidColor(GrayMatterColors.TypeOpinion), 
                    decorationBox = { inner -> 
                        if (opinionInput.isEmpty()) Text("Type your understanding here...", color = GrayMatterTheme.colors.neutral600, style = MaterialTheme.typography.bodyLarge)
                        inner() 
                    }
                )
            }
        }
    }
}


// Removed local DynamicEntryForm in favor of shared component in ui.components


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
            Text("CONFIDENCE LEVEL", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.textSecondary)
            Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GrayMatterTheme.colors.textPrimary)
        }
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bgColor).border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Slider(
                value = confidence, 
                onValueChange = onConfidenceChange, 
                valueRange = 0f..1f, 
                colors = SliderDefaults.colors(
                    thumbColor = accentColor, 
                    activeTrackColor = accentColor, 
                    inactiveTrackColor = GrayMatterTheme.colors.neutral800
                )
            )
        }
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit, enabled: Boolean, isLoading: Boolean, modifier: Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary, contentColor = GrayMatterTheme.colors.onPrimary, disabledContainerColor = GrayMatterTheme.colors.neutral700, disabledContentColor = GrayMatterTheme.colors.neutral500)) {
        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GrayMatterTheme.colors.onPrimary, strokeWidth = 2.dp)
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
            Text("Save Entry", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
