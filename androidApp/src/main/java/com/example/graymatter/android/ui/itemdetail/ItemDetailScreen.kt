package com.example.graymatter.android.ui.itemdetail

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.ItemWithDetails
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.Bookmark
import java.text.SimpleDateFormat
import java.util.*

/**
 * Item Details Screen.
 * Features a beautiful animated timeline where opinions and bookmark reflections are unified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemDetails: ItemWithDetails?,
    readingProgress: com.example.graymatter.domain.ReadingProgress?,
    onBackClick: () -> Unit,
    onOpenResource: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onUpdateDescription: (String?) -> Unit,
    onAddOpinion: (text: String, confidence: Int) -> Unit,
    onUpdateOpinion: (id: String, text: String, confidence: Int, createdAt: Long) -> Unit,
    onDeleteOpinion: (id: String) -> Unit,
    onRenameResource: (String) -> Unit,
    onEditNote: () -> Unit,
    onDeleteItem: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var description by remember(itemDetails?.item?.description) { mutableStateOf(itemDetails?.item?.description ?: "") }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark),
        containerColor = GrayMatterColors.BackgroundDark,
        floatingActionButton = {
            if (!isEditing) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GrayMatterColors.TextPrimary,
                    contentColor = GrayMatterColors.BackgroundDark,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Opinion",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            // Header
            ItemDetailHeader(
                onBackClick = onBackClick,
                isEditing = isEditing,
                onToggleEdit = {
                    if (isEditing) {
                        onUpdateDescription(description.takeIf { it.isNotBlank() })
                    }
                    isEditing = !isEditing
                },
                onDeleteClick = { showDeleteConfirm = true }
            )

            if (itemDetails != null) {
                // Unified sorted list of Opinions
                val sortedOpinions = remember(itemDetails.opinions) {
                    itemDetails.opinions.sortedBy { it.createdAt }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Resource Card
                    ResourceCard(
                        title = itemDetails.resource.title ?: "Untitled",
                        url = itemDetails.resource.url ?: itemDetails.resource.filePath ?: "",
                        savedTimeAgo = "Saved ${formatTimeAgo(itemDetails.item.firstOpinionAt)}",
                        readingProgress = readingProgress,
                        onOpenClick = onOpenResource,
                        onRenameClick = { if (isEditing) showRenameDialog = true },
                        showEditButton = isEditing && itemDetails.resource.type == com.example.graymatter.domain.ResourceType.MARKDOWN,
                        onEditClick = onEditNote
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Optional Description Section
                    SectionHeader(text = "DESCRIPTION")
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isEditing) {
                        DescriptionEditor(
                            value = description,
                            onValueChange = { description = it }
                        )
                    } else if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = GrayMatterColors.TextSecondary
                        )
                    } else {
                        Text(
                            text = "No description provided.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = GrayMatterColors.Neutral700
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Opinion History",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = GrayMatterColors.TextPrimary
                        )
                        
                        // Export History Button
                        TextButton(
                            onClick = onExport,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = GrayMatterColors.Primary)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export History", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Unified Timeline
                    OpinionTimeline(
                        opinions = sortedOpinions,
                        resourceId = itemDetails.resource.id,
                        isEditing = isEditing,
                        onUpdateOpinion = onUpdateOpinion,
                        onDeleteOpinion = onDeleteOpinion,
                        onJumpToPage = { resourceId, page ->
                            onOpenBookmark(Bookmark(id="", resourceId=resourceId, page=page, createdAt=0L))
                        }
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        if (showAddDialog) {
            OpinionEditDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { text, confidence ->
                    onAddOpinion(text, confidence)
                    showAddDialog = false
                }
            )
        }

        if (showRenameDialog && itemDetails != null) {
            RenameDialog(
                currentName = itemDetails.resource.title ?: "",
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    onRenameResource(newName)
                    showRenameDialog = false
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Item", color = Color.White) },
                text = { Text("Are you sure you want to delete this resource and all its opinions? This action cannot be undone.", color = GrayMatterColors.TextSecondary) },
                containerColor = Color(0xFF1A1A1E),
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDeleteItem()
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
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1E),
        title = { Text("Rename Resource", color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("Rename", color = GrayMatterColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        color = GrayMatterColors.Neutral500
    )
}

@Composable
private fun DescriptionEditor(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayMatterColors.SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary),
            modifier = Modifier.fillMaxWidth(),
            cursorBrush = SolidColor(GrayMatterColors.Primary),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Add a description...", color = GrayMatterColors.Neutral700)
                inner()
            }
        )
    }
}

@Composable
private fun ItemDetailHeader(
    onBackClick: () -> Unit,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.KeyboardArrowLeft, "Back", tint = GrayMatterColors.TextPrimary, modifier = Modifier.size(32.dp))
        }
        Text("Item Details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.TextPrimary)
        Row {
            if (isEditing) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = GrayMatterColors.Error,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            IconButton(onClick = onToggleEdit) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (isEditing) GrayMatterColors.Success else GrayMatterColors.TextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun ResourceCard(
    title: String,
    url: String,
    savedTimeAgo: String,
    readingProgress: com.example.graymatter.domain.ReadingProgress?,
    onOpenClick: () -> Unit,
    onRenameClick: () -> Unit,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.2.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenClick)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary,
                    modifier = Modifier.weight(1f).clickable(onClick = onRenameClick)
                )
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open",
                    tint = GrayMatterColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(url, style = MaterialTheme.typography.bodySmall, color = GrayMatterColors.Neutral500, maxLines = 2)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Schedule, null, tint = GrayMatterColors.Neutral600, modifier = Modifier.size(16.dp))
                Text(savedTimeAgo, style = MaterialTheme.typography.bodySmall, color = GrayMatterColors.Neutral600)
            }

            // Reading Progress Indicator
            if (readingProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${(readingProgress.percentComplete * 100).toInt()}% Read",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterColors.Primary
                        )
                        Text(
                            "Page ${readingProgress.currentPage + 1} of ${readingProgress.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterColors.Neutral500
                        )
                    }
                    LinearProgressIndicator(
                        progress = readingProgress.percentComplete.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = GrayMatterColors.Primary,
                        trackColor = GrayMatterColors.Neutral800
                    )
                }
            } else if (showEditButton) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayMatterColors.Primary,
                        contentColor = GrayMatterColors.OnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Note")
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayMatterColors.Neutral900,
                        contentColor = GrayMatterColors.Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Read / View")
                }
            }
        }
    }
}

@Composable
private fun OpinionTimeline(
    opinions: List<Opinion>,
    resourceId: String,
    isEditing: Boolean,
    onUpdateOpinion: (String, String, Int, Long) -> Unit,
    onDeleteOpinion: (String) -> Unit,
    onJumpToPage: (String, Int) -> Unit
) {
    Column {
        opinions.forEachIndexed { index, opinion ->
            OpinionTimelineItem(
                opinion = opinion,
                serialNumber = index + 1,
                isFirst = index == 0,
                isLast = index == opinions.lastIndex,
                isEditing = isEditing,
                onUpdate = { text, confidence, date -> onUpdateOpinion(opinion.id, text, confidence, date) },
                onDelete = { onDeleteOpinion(opinion.id) },
                onJump = {
                    opinion.pageNumber?.let { page ->
                        onJumpToPage(resourceId, page)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpinionTimelineItem(
    opinion: Opinion,
    serialNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isEditing: Boolean,
    onUpdate: (String, Int, Long) -> Unit,
    onDelete: () -> Unit,
    onJump: () -> Unit
) {
    var text by remember(opinion.text) { mutableStateOf(opinion.text) }
    var confidence by remember(opinion.confidenceScore) { mutableStateOf(opinion.confidenceScore.toFloat() / 100f) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    
    val hasPageNumber = opinion.pageNumber != null

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isFirst) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Physical thin line indicator
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            val dotSize = if (isFirst) 12.dp else 8.dp
            val dotTopPadding = 12.dp
            val lineColor = GrayMatterColors.Neutral800
            
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(dotTopPadding + dotSize / 2)
                        .background(lineColor)
                )
            }
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = dotTopPadding + dotSize / 2)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(lineColor)
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = dotTopPadding)
                    .size(dotSize)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(if (isFirst) GrayMatterColors.Primary else GrayMatterColors.Neutral600)
                    .border(2.dp, GrayMatterColors.BackgroundDark, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 16.dp else 48.dp)
                .then(if (hasPageNumber && !isEditing) Modifier.clickable { onJump() } else Modifier)
        ) {
            // Header: Serial Number, Title, Date & Confidence Badge
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Serial Number
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GrayMatterColors.SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = serialNumber.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = GrayMatterColors.Neutral500
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        val isAnnotation = opinion.text.startsWith("> ")
                        val hasPageNumber = opinion.pageNumber != null
                        
                        val (title, icon, color) = when {
                            isAnnotation -> Triple("ANNOTATION", Icons.Default.FormatQuote, Color(0xFFFF9800))
                            hasPageNumber -> Triple("BOOKMARK", Icons.Default.Bookmark, Color(0xFFFFD700))
                            else -> Triple("OPINION", Icons.Default.QuestionAnswer, Color(0xFF4CAF50))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = color
                            )
                        }

                        if (isEditing) {
                            DateChip(timestamp = opinion.createdAt, onClick = { showDateTimePicker = true })
                        } else {
                            Text(
                                text = formatFullDate(opinion.createdAt).uppercase(), 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), 
                                color = GrayMatterColors.Neutral600,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfidenceBadge(score = opinion.confidenceScore)
                    if (isEditing) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = GrayMatterColors.Error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            
            val isAnnotation = text.startsWith("> ")
            val hasPageNumber = opinion.pageNumber != null

            if (hasPageNumber && !isEditing) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Bookmark, null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                        Text("PAGE ${opinion.pageNumber!! + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), maxLines = 1, softWrap = false)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Opinion Content
            if (isEditing) {
                OpinionEditor(
                    text = text,
                    confidence = confidence,
                    onTextChange = { 
                        text = it 
                        onUpdate(it, (confidence * 100).toInt(), opinion.createdAt)
                    },
                    onConfidenceChange = { 
                        confidence = it 
                        onUpdate(text, (it * 100).toInt(), opinion.createdAt)
                    }
                )
            } else {
                val isAnnotation = text.startsWith("> ")
                val isOldBookmark = text.startsWith("[Page ")
                
                if (isAnnotation) {
                    // Split into quote and reflection
                    val parts = text.split("\n\n", limit = 2)
                    val quote = parts[0].removePrefix("> ").trim()
                    val reflection = if (parts.size > 1) parts[1].trim() else ""
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFF9800).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, Color(0xFFFF9800).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quote block (Gray)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.width(4.dp).height(IntrinsicSize.Min).background(Color(0xFFFF9800))) // Orange accent
                                Text(
                                    text = "\"$quote\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 24.sp),
                                    color = GrayMatterColors.Neutral400,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        // Reflection
                        if (reflection.isNotEmpty()) {
                            Text(
                                text = reflection,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                color = GrayMatterColors.TextPrimary
                            )
                        }
                    }
                } else if (hasPageNumber) {
                    // Bookmark or old style page opinion
                    val cleanText = if (isOldBookmark) text.replace(Regex("^\\[Page \\d+\\]\\s*"), "") else text
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFD700).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = cleanText,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = GrayMatterColors.TextPrimary
                        )
                    }
                } else {
                    // General Opinion (Green)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4CAF50).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = GrayMatterColors.TextPrimary
                        )
                    }
                }
                
                if (hasPageNumber && !isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Launch, null, tint = GrayMatterColors.Primary.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tap to jump back to page", fontSize = 11.sp, color = GrayMatterColors.Primary.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

    if (showDateTimePicker) {
        DateTimePicker(
            initialTimestamp = opinion.createdAt,
            onDismiss = { showDateTimePicker = false },
            onConfirm = { onUpdate(text, (confidence * 100).toInt(), it) }
        )
    }
}

@Composable
private fun DateChip(timestamp: Long, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GrayMatterColors.Neutral900)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatFullDate(timestamp).uppercase(), style = MaterialTheme.typography.labelSmall.copy(color = GrayMatterColors.Primary, fontWeight = FontWeight.Bold))
            Icon(Icons.Default.CalendarToday, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ConfidenceBadge(score: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = if (score > 50) Icons.Default.TrendingUp else Icons.Default.ArrowForward,
                contentDescription = null,
                tint = GrayMatterColors.Primary,
                modifier = Modifier.size(12.dp)
            )
            Text("${score/10}/10", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.TextPrimary, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun OpinionEditor(text: String, confidence: Float, onTextChange: (String) -> Unit, onConfidenceChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp)).border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp)).padding(12.dp)) {
            BasicTextField(value = text, onValueChange = onTextChange, textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary), modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(GrayMatterColors.Primary))
        }
        Slider(value = confidence, onValueChange = onConfidenceChange, colors = SliderDefaults.colors(thumbColor = GrayMatterColors.Primary, activeTrackColor = GrayMatterColors.Primary))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePicker(initialTimestamp: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var isTimeStep by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTimestamp)
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTimestamp }
    val timePickerState = rememberTimePickerState(initialHour = calendar.get(Calendar.HOUR_OF_DAY), initialMinute = calendar.get(Calendar.MINUTE))

    if (!isTimeStep) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = { isTimeStep = true }) { Text("Next", color = GrayMatterColors.Primary) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GrayMatterColors.Neutral500) } },
            colors = DatePickerDefaults.colors(containerColor = GrayMatterColors.SurfaceDark)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(selectedDayContainerColor = GrayMatterColors.Primary, selectedDayContentColor = GrayMatterColors.OnPrimary, todayContentColor = GrayMatterColors.Primary, todayDateBorderColor = GrayMatterColors.Primary))
        }
    } else {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
            Box(modifier = Modifier.clip(RoundedCornerShape(28.dp)).background(GrayMatterColors.SurfaceDark).padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Time", style = MaterialTheme.typography.titleMedium, color = GrayMatterColors.TextPrimary)
                    Spacer(modifier = Modifier.height(24.dp))
                    TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(selectorColor = GrayMatterColors.Primary, clockDialSelectedContentColor = GrayMatterColors.OnPrimary))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isTimeStep = false }) { Text("Back", color = GrayMatterColors.Neutral500) }
                        TextButton(onClick = {
                            val resultCal = Calendar.getInstance()
                            datePickerState.selectedDateMillis?.let { resultCal.timeInMillis = it }
                            resultCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            resultCal.set(Calendar.MINUTE, timePickerState.minute)
                            onConfirm(resultCal.timeInMillis)
                            onDismiss()
                        }) { Text("OK", color = GrayMatterColors.Primary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpinionEditDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    var confidence by remember { mutableFloatStateOf(0.7f) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GrayMatterColors.SurfaceDark).border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp)).padding(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add New Opinion", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GrayMatterColors.TextPrimary)
                BasicTextField(value = text, onValueChange = { text = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = GrayMatterColors.TextPrimary), modifier = Modifier.fillMaxWidth().height(120.dp).background(GrayMatterColors.SurfaceInput, RoundedCornerShape(12.dp)).border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp)).padding(16.dp), cursorBrush = SolidColor(GrayMatterColors.Primary), decorationBox = { inner -> if (text.isEmpty()) Text("Type your opinion here...", color = GrayMatterColors.Neutral600); inner() })
                Column {
                    Text("Confidence: ${(confidence * 10).toInt()}/10", style = MaterialTheme.typography.labelMedium, color = GrayMatterColors.Neutral500)
                    Slider(value = confidence, onValueChange = { confidence = it }, colors = SliderDefaults.colors(thumbColor = GrayMatterColors.Primary, activeTrackColor = GrayMatterColors.Primary))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = GrayMatterColors.Neutral500) }
                    Button(onClick = { onConfirm(text, (confidence * 100).toInt()) }, enabled = text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Primary, contentColor = GrayMatterColors.OnPrimary)) { Text("Save") }
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        days < 1 -> "today"
        days < 30 -> "$days days ago"
        else -> "${days / 30} months ago"
    }
}

private fun formatFullDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
