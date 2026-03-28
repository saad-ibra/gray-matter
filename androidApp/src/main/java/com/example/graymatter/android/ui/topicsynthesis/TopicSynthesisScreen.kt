package com.example.graymatter.android.ui.topicsynthesis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.domain.Topic
import com.example.graymatter.android.ui.components.MarkdownEditor

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
    onSaveOverallOpinion: (String, List<com.example.graymatter.domain.ReferenceSelectorItem>) -> Unit,
    onDeleteTopic: () -> Unit,
    onRenameTopic: (String) -> Unit,
    onExport: () -> Unit,
    onLoadLinks: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.graymatter.domain.ReferenceSelectorItem>>,
    referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel? = null,
    modifier: Modifier = Modifier
) {
    if (topic == null) return

    var showEditor by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var showReferenceSelector by remember { mutableStateOf(false) }
    var selectedReferences by remember { mutableStateOf<List<com.example.graymatter.domain.ReferenceSelectorItem>>(emptyList()) }
    var referenceToInsert by remember { mutableStateOf<String?>(null) }
    var currentEditorText by remember { mutableStateOf(topic.notes ?: "") }

    // Load existing references on startup
    LaunchedEffect(topic.id) {
        if (referenceSelectorViewModel != null) {
            onLoadLinks(topic.id).collect { links ->
                selectedReferences = links
            }
        }
    }

    // Robust reference synchronizer (matches NewEntryScreen)
    LaunchedEffect(currentEditorText) {
        val regex = Regex("\\[\\[(.*?)\\]\\]")
        val foundTexts = regex.findAll(currentEditorText).map { it.groupValues[1] }.toSet()
        
        selectedReferences = selectedReferences.filter { ref ->
            val refText = when (ref) {
                is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
            }
            foundTexts.contains(refText) || foundTexts.any { it.endsWith(refText) }
        }.distinctBy { it.id }
    }
    
    var showRenameDialog by remember { mutableStateOf(false) }
    var newTopicName by remember { mutableStateOf(topic.name) }

    if (showEditor) {
        MarkdownEditor(
            title = topic.name,
            initialText = topic.notes ?: "",
            onBackClick = { showEditor = false },
            onSave = { content -> 
                onSaveOverallOpinion(content, selectedReferences)
                showEditor = false
            },
            onTextChange = { currentEditorText = it },
            initialPreviewMode = topic.notes?.isNotBlank() == true,
            onShowReferenceSelector = { 
                referenceSelectorViewModel?.clearSelection()
                showReferenceSelector = true 
            },
            referenceToInsert = referenceToInsert,
            onReferenceInserted = { referenceToInsert = null }
        )
        
        if (showReferenceSelector && referenceSelectorViewModel != null) {
            com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
                viewModel = referenceSelectorViewModel,
                onDismissRequest = { showReferenceSelector = false },
                onConfirm = { items ->
                    showReferenceSelector = false
                    if (items.isNotEmpty()) {
                        selectedReferences = (selectedReferences + items).distinctBy { it.id }
                        val item = items.first()
                        val text = when (item) {
                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> "Topic: ${item.name}"
                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> "Resource: ${item.title}"
                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> "Opinion: ${item.snippet.take(15)}..."
                        }
                        referenceToInsert = "[[$text]]"
                    }
                }
            )
        }
    } else {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(GrayMatterColors.BackgroundDark),
            containerColor = GrayMatterColors.BackgroundDark
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TopicHeader(
                    topicName = topic.name,
                    onBackClick = onBackClick,
                    onRenameClick = { showRenameDialog = true },
                    onDeleteClick = { showDeleteConfirm = true },
                    onExportClick = onExport
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
                            onClick = { showEditor = true }
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

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Topic", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newTopicName,
                    onValueChange = { newTopicName = it },
                    label = { Text("Topic Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = GrayMatterColors.Primary
                    )
                )
            },
            containerColor = Color(0xFF1A1A1E),
            confirmButton = {
                TextButton(onClick = {
                    onRenameTopic(newTopicName)
                    showRenameDialog = false
                }) {
                    Text("Rename", color = GrayMatterColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}


@Composable
private fun TopicHeader(
    topicName: String,
    onBackClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayMatterColors.BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.KeyboardArrowLeft, "Back", tint = GrayMatterColors.TextPrimary, modifier = Modifier.size(28.dp))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "TOPIC", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                color = GrayMatterColors.Neutral500,
                textAlign = TextAlign.Center
            )
            Text(
                text = topicName, 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), 
                color = GrayMatterColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        
        var showMenu by remember { mutableStateOf(false) }
        
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, "Menu", tint = GrayMatterColors.TextPrimary, modifier = Modifier.size(24.dp))
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(GrayMatterColors.SurfaceDark)
            ) {
                DropdownMenuItem(
                    text = { Text("Rename Topic", color = GrayMatterColors.TextPrimary) },
                    onClick = {
                        showMenu = false
                        onRenameClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = GrayMatterColors.Primary) }
                )

                DropdownMenuItem(
                    text = { Text("Export Topic", color = GrayMatterColors.TextPrimary) },
                    onClick = {
                        showMenu = false
                        onExportClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = GrayMatterColors.Primary) }
                )
                
                Divider(color = GrayMatterColors.Neutral800)
                
                DropdownMenuItem(
                    text = { Text("Delete Topic", color = GrayMatterColors.Error) },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = GrayMatterColors.Error) }
                )
            }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GrayMatterColors.BackgroundDark)
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
            Icon(Icons.Default.ChevronRight, null, tint = GrayMatterColors.Neutral700)
        }
    }
}
@Composable
private fun OverallOpinionSection(
    hasOpinion: Boolean,
    onClick: () -> Unit
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
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (hasOpinion) GrayMatterColors.KnowledgeBlue.copy(alpha = 0.15f) else GrayMatterColors.SurfaceDark)
                .border(1.dp, if (hasOpinion) GrayMatterColors.KnowledgeBlue.copy(alpha = 0.4f) else GrayMatterColors.Neutral800, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = if (hasOpinion) Icons.Default.EditNote else Icons.Default.RateReview,
                    contentDescription = null,
                    tint = if (hasOpinion) GrayMatterColors.KnowledgeBlue else GrayMatterColors.Primary
                )
                Text(
                    text = if (hasOpinion) "Edit Overall Opinion" else "Add Overall Opinion",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (hasOpinion) GrayMatterColors.KnowledgeBlue else GrayMatterColors.Primary
                )
            }
        }
    }
}
