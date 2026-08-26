package com.example.graymatter.android.ui.components

import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.graymatter.android.R
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ════════════════════════════════════════════════════════════════════════
//  Main Tutorial Overlay
// ════════════════════════════════════════════════════════════════════════

@Composable
fun TutorialOverlay(onDismiss: () -> Unit) {
    var currentSlide by remember { mutableIntStateOf(0) }
    var canAdvance by remember { mutableStateOf(true) }

    // Cross-slide narrative state
    var selectedResourceType by remember { mutableStateOf<String?>(null) }
    var selectedOpinion by remember { mutableStateOf<String?>(null) }
    var selectedTopic by remember { mutableStateOf<String?>(null) }

    val totalSlides = 9

    fun advanceTo(slide: Int) {
        currentSlide = slide
        canAdvance = slide == 0 || slide == totalSlides - 1
    }

    fun goBack() {
        if (currentSlide > 0) {
            currentSlide--
            canAdvance = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Skip button
            Text(
                text = "Skip",
                color = GrayMatterTheme.colors.neutral500,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 8.dp)
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )

            // Main Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GrayMatterTheme.colors.surface)
                    .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(24.dp))
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentSlide,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "slide_transition"
                    ) { page ->
                        when (page) {
                            0 -> SlideWelcome()
                            1 -> SlideBigPlus(onTapPlus = { advanceTo(2) })
                            2 -> SlidePickResource(onPick = { type ->
                                selectedResourceType = type
                                canAdvance = true
                            })
                            3 -> SlideFirstOpinion(
                                resourceType = selectedResourceType ?: "Resource",
                                onComplete = { opinion ->
                                    selectedOpinion = opinion
                                    canAdvance = true
                                }
                            )
                            4 -> SlideTopic(onPick = { topic ->
                                selectedTopic = topic
                                canAdvance = true
                            })
                            5 -> SlidePdfReader(onComplete = { canAdvance = true })
                            6 -> SlideEntryTypes(onAllDiscovered = { canAdvance = true })
                            7 -> SlideGraph(
                                topic = selectedTopic ?: "My Topic",
                                resource = selectedResourceType ?: "File",
                                opinion = selectedOpinion ?: "My thought",
                                onExplored = { canAdvance = true }
                            )
                            8 -> SlideReady()
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(totalSlides) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (i == currentSlide) GrayMatterTheme.colors.primary
                                            else GrayMatterTheme.colors.neutral800
                                        )
                                )
                            }
                        }

                        if (currentSlide == totalSlides - 1) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary)
                            ) {
                                Text("Get Started", color = GrayMatterTheme.colors.onPrimary)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (currentSlide > 0) {
                                    TextButton(onClick = { goBack() }) {
                                        Text("Back", color = GrayMatterTheme.colors.neutral500)
                                    }
                                }
                                AnimatedVisibility(visible = canAdvance) {
                                    Button(
                                        onClick = { advanceTo(currentSlide + 1) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary)
                                    ) {
                                        Text("Next", color = GrayMatterTheme.colors.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 0: Welcome
// ════════════════════════════════════════════════════════════════════════

@Composable
private fun SlideWelcome() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo_full),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Text(
            text = "Welcome to Relatrix",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = "Let's build your first piece of knowledge.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minHeight = 60.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 1: The Big Plus
// ════════════════════════════════════════════════════════════════════════

@Composable
private fun SlideBigPlus(onTapPlus: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.InsertDriveFile, null,
                tint = GrayMatterColors.TypeLookupMain.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp).offset(x = (-50).dp, y = (-40).dp))
            Icon(Icons.Default.Language, null,
                tint = GrayMatterColors.TypeLink.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp).offset(x = 50.dp, y = (-30).dp))
            Icon(Icons.Default.Edit, null,
                tint = GrayMatterColors.TypeOpinion.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp).offset(x = 0.dp, y = 55.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .clip(CircleShape)
                    .background(GrayMatterTheme.colors.primary.copy(alpha = 0.08f))
                    .border(2.dp, GrayMatterTheme.colors.primary.copy(alpha = 0.4f), CircleShape)
                    .clickable { onTapPlus() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null,
                    tint = GrayMatterTheme.colors.primary,
                    modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("This is how everything starts",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp))
        Text("Tap the + to begin.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterTheme.colors.textSecondary, textAlign = TextAlign.Center)
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 2: Pick a Resource Type
// ════════════════════════════════════════════════════════════════════════

private data class ResourceOption(val name: String, val icon: ImageVector, val color: Color)

@Composable
private fun SlidePickResource(onPick: (String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    val iconColor = GrayMatterTheme.colors.textPrimary
    val resources = remember(iconColor) { listOf(
        ResourceOption("File", Icons.Default.InsertDriveFile, iconColor),
        ResourceOption("Link", Icons.Default.Language, iconColor),
        ResourceOption("Note", Icons.Default.Edit, iconColor)
    ) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Add a Resource",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp))
        Text("What would you like to add? Pick one.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            resources.forEach { res ->
                val isSelected = selected == res.name
                val alpha = if (selected != null && !isSelected) 0.3f else 1f
                Box(
                    modifier = Modifier
                        .weight(1f).aspectRatio(1f)
                        .graphicsLayer { this.alpha = alpha }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) res.color.copy(alpha = 0.15f) else GrayMatterTheme.colors.neutral900)
                        .border(if (isSelected) 2.dp else 1.dp,
                            if (isSelected) res.color else GrayMatterTheme.colors.neutral800,
                            RoundedCornerShape(16.dp))
                        .clickable(enabled = selected == null) { selected = res.name; onPick(res.name) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(res.icon, null, tint = res.color, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(res.name, style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) res.color else GrayMatterTheme.colors.textSecondary)
                    }
                }
            }
        }

        AnimatedVisibility(visible = selected != null) {
            Text("✓ ${selected} added!",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = GrayMatterColors.TypeOpinion, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 3: Your First Opinion (with confidence slider)
// ════════════════════════════════════════════════════════════════════════

@Composable
private fun SlideFirstOpinion(resourceType: String, onComplete: (String) -> Unit) {
    var opinionText by remember { mutableStateOf("") }
    var confidence by remember { mutableFloatStateOf(50f) }
    var completed by remember { mutableStateOf(false) }
    val suggestions = remember { listOf("Interesting concept", "Need to revisit", "Key insight") }

    val confidenceLabel = when {
        confidence <= 25f -> "Uncertain"
        confidence <= 50f -> "Somewhat sure"
        confidence <= 75f -> "Confident"
        else -> "Very confident"
    }
    val resourceIcon = when (resourceType) {
        "Link" -> Icons.Default.Language
        "Note" -> Icons.Default.Edit
        else -> Icons.Default.InsertDriveFile
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Mini resource card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GrayMatterTheme.colors.neutral900)
                .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Icon(resourceIcon, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("My First $resourceType", style = MaterialTheme.typography.bodyMedium,
                color = GrayMatterTheme.colors.textPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Your First Opinion",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp))
        Text("Capture your preconceived notion or initial thought about this ${resourceType.lowercase()}.",
            style = MaterialTheme.typography.bodySmall,
            color = GrayMatterTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp))

        if (!completed) {
            OutlinedTextField(
                value = opinionText, onValueChange = { opinionText = it },
                placeholder = { Text("What's your first thought?", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth().heightIn(max = 72.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GrayMatterColors.TypeOpinion,
                    unfocusedBorderColor = GrayMatterTheme.colors.neutral700,
                    focusedTextColor = GrayMatterTheme.colors.textPrimary,
                    unfocusedTextColor = GrayMatterTheme.colors.textPrimary,
                    cursorColor = GrayMatterColors.TypeOpinion
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Suggestions",
                style = MaterialTheme.typography.labelSmall,
                color = GrayMatterTheme.colors.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { s ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GrayMatterColors.TypeOpinion.copy(alpha = 0.1f))
                            .border(1.dp, GrayMatterColors.TypeOpinion.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .clickable { opinionText = s }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text(s, style = MaterialTheme.typography.labelSmall, color = GrayMatterColors.TypeOpinion) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Confidence: ${confidence.toInt()}% · $confidenceLabel",
                style = MaterialTheme.typography.labelSmall,
                color = GrayMatterTheme.colors.textSecondary, modifier = Modifier.fillMaxWidth())
            Slider(
                value = confidence, onValueChange = { confidence = it }, valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = GrayMatterColors.TypeOpinion,
                    activeTrackColor = GrayMatterColors.TypeOpinion,
                    inactiveTrackColor = GrayMatterColors.TypeOpinion.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { if (opinionText.isNotBlank()) { completed = true; onComplete(opinionText) } },
                enabled = opinionText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrayMatterColors.TypeOpinion,
                    disabledContainerColor = GrayMatterColors.TypeOpinion.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            ) { Text("Save Opinion") }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GrayMatterColors.TypeOpinion.copy(alpha = 0.1f))
                    .border(1.dp, GrayMatterColors.TypeOpinion.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextSnippet, null, tint = GrayMatterColors.TypeOpinion, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Opinion", style = MaterialTheme.typography.labelMedium, color = GrayMatterColors.TypeOpinion)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("\"$opinionText\"", style = MaterialTheme.typography.bodyMedium, color = GrayMatterTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Confidence: ${confidence.toInt()}%", style = MaterialTheme.typography.labelSmall, color = GrayMatterTheme.colors.textSecondary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("This is an Opinion, your core unit of thinking.",
                style = MaterialTheme.typography.bodySmall, color = GrayMatterColors.TypeOpinion, textAlign = TextAlign.Center)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 4: Organize into a Topic
// ════════════════════════════════════════════════════════════════════════

@Composable
private fun SlideTopic(onPick: (String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    val topics = remember { listOf("Philosophy", "Science", "My Notes") }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Organize into a Topic",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp))
        Text("Every resource belongs to a Topic. Pick one.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            topics.forEach { topic ->
                val isSelected = selected == topic
                val alpha = if (selected != null && !isSelected) 0.3f else 1f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .graphicsLayer { this.alpha = alpha }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) GrayMatterColors.TypeBookmark.copy(alpha = 0.15f) else GrayMatterTheme.colors.neutral900)
                        .border(if (isSelected) 2.dp else 1.dp,
                            if (isSelected) GrayMatterColors.TypeBookmark else GrayMatterTheme.colors.neutral800,
                            RoundedCornerShape(12.dp))
                        .clickable(enabled = selected == null) { selected = topic; onPick(topic) }
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Folder, null, tint = GrayMatterColors.TypeBookmark, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(topic, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) GrayMatterColors.TypeBookmark else GrayMatterTheme.colors.textPrimary)
                    if (isSelected) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text("1 resource", style = MaterialTheme.typography.labelSmall,
                            color = GrayMatterColors.TypeBookmark.copy(alpha = 0.7f))
                    }
                }
            }
        }

        AnimatedVisibility(visible = selected != null) {
            Text("✓ Organized! Topics help you synthesize later.",
                style = MaterialTheme.typography.bodySmall, color = GrayMatterColors.TypeBookmark,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 5: PDF Reader
// ════════════════════════════════════════════════════════════════════════

@Composable
private fun SlidePdfReader(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        onComplete()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Dedicated PDF Reader",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp))
        Text("Active reading with 5 entry types.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp))

        // Mini PDF page mockup
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GrayMatterTheme.colors.neutral900)
                .border(1.dp, GrayMatterTheme.colors.neutral800, RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fake text lines
                Box(Modifier.fillMaxWidth(0.9f).height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(GrayMatterTheme.colors.neutral700))
                Box(Modifier.fillMaxWidth(0.7f).height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(GrayMatterTheme.colors.neutral700))
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Highlighted line
                Box(Modifier.fillMaxWidth(0.85f).height(10.dp).clip(RoundedCornerShape(4.dp))
                    .background(GrayMatterColors.TypeAnnotation.copy(alpha = 0.4f)))
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(Modifier.fillMaxWidth(0.6f).height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(GrayMatterTheme.colors.neutral700))
                Box(Modifier.fillMaxWidth(0.8f).height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(GrayMatterTheme.colors.neutral700))
            }
            
            // Bookmark icon overlay
            Icon(
                Icons.Default.Bookmark, 
                contentDescription = null,
                tint = GrayMatterColors.TypeBookmark,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-4).dp)
                    .size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 6: Types of Entries (tap to discover)
// ════════════════════════════════════════════════════════════════════════

private data class EntryTypeInfo(val name: String, val icon: ImageVector, val color: Color, val description: String)

@Composable
private fun SlideEntryTypes(onAllDiscovered: () -> Unit) {
    val entryTypes = remember { listOf(
        EntryTypeInfo("Opinions", Icons.Default.TextSnippet, GrayMatterColors.TypeOpinion, "Your personal thoughts"),
        EntryTypeInfo("Annotations", Icons.Default.Highlight, GrayMatterColors.TypeAnnotation, "Highlights with notes"),
        EntryTypeInfo("Bookmarks", Icons.Default.Bookmark, GrayMatterColors.TypeBookmark, "Saved places in text"),
        EntryTypeInfo("Templates", Icons.Default.ListAlt, GrayMatterColors.TypeTemplate, "Structured forms"),
        EntryTypeInfo("Lookups", Icons.Default.MenuBook, GrayMatterColors.TypeLookupMain, "Search definitions"),
        EntryTypeInfo("Vision", Icons.Default.ImageSearch, GrayMatterColors.TypeVisual, "Image entry")
    ) }

    var discovered by remember { mutableStateOf(setOf<Int>()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Types of Entries",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp))
        Text("Like an opinion, there are other ways to capture knowledge. Tap each to discover.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            entryTypes.forEachIndexed { index, entry ->
                val isDiscovered = index in discovered
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(when {
                            isSelected -> entry.color.copy(alpha = 0.2f)
                            isDiscovered -> entry.color.copy(alpha = 0.1f)
                            else -> GrayMatterTheme.colors.neutral900
                        })
                        .border(if (isSelected) 2.dp else 1.dp,
                            if (isDiscovered) entry.color.copy(alpha = 0.5f) else GrayMatterTheme.colors.neutral800,
                            CircleShape)
                        .clickable {
                            selectedIndex = index
                            val nd = discovered + index
                            discovered = nd
                            if (nd.size >= entryTypes.size) onAllDiscovered()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(entry.icon, null,
                        tint = if (isDiscovered) entry.color else GrayMatterTheme.colors.neutral700,
                        modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "entry_type_desc"
        ) { idx ->
            if (idx >= 0 && idx < entryTypes.size) {
                val entry = entryTypes[idx]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(entry.color.copy(alpha = 0.1f))
                        .border(1.dp, entry.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Icon(entry.icon, null, tint = entry.color, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(entry.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = entry.color)
                        Text(entry.description, style = MaterialTheme.typography.bodySmall, color = GrayMatterTheme.colors.textSecondary)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(56.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("${discovered.size} / ${entryTypes.size} discovered",
            style = MaterialTheme.typography.labelMedium,
            color = if (discovered.size >= entryTypes.size) GrayMatterColors.TypeOpinion else GrayMatterTheme.colors.neutral500,
            textAlign = TextAlign.Center)
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 7: The Relatrix Graph
//  Topic = pointy hexagon, Resource = triangle, Opinion = green circle
//  All entry types connected to the resource triangle
// ════════════════════════════════════════════════════════════════════════

private enum class NodeShape { HEXAGON, TRIANGLE, CIRCLE }

private data class GraphNode(
    val label: String,
    val color: Color,
    val fx: Float,
    val fy: Float,
    val shape: NodeShape = NodeShape.CIRCLE,
    val highlighted: Boolean = false,
    val labelBottom: String? = null
)

// Draw a pointy-top/bottom hexagon
private fun DrawScope.drawHexagonShape(center: Offset, radius: Float, color: Color, style: androidx.compose.ui.graphics.drawscope.DrawStyle = androidx.compose.ui.graphics.drawscope.Fill) {
    val path = Path()
    for (i in 0..5) {
        val angle = (2.0 * PI * i / 6.0 - PI / 2.0).toFloat()
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = style)
}

// Draw a pointy-top equilateral triangle
private fun DrawScope.drawTriangleShape(center: Offset, radius: Float, color: Color, style: androidx.compose.ui.graphics.drawscope.DrawStyle = androidx.compose.ui.graphics.drawscope.Fill) {
    val path = Path()
    for (i in 0..2) {
        val angle = (2.0 * PI * i / 3.0 - PI / 2.0).toFloat()
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = style)
}

@Composable
private fun SlideGraph(
    topic: String,
    resource: String,
    opinion: String,
    onExplored: () -> Unit
) {
    var visibleNodes by remember { mutableIntStateOf(0) }
    var tappedNodes by remember { mutableStateOf(setOf<Int>()) }
    var selectedNode by remember { mutableIntStateOf(-1) }

    val totalNodes = 8

    LaunchedEffect(Unit) {
        onExplored() // Make exploring optional: instantly allow Next
        for (i in 1..totalNodes) { delay(350L); visibleNodes = i }
    }
    LaunchedEffect(tappedNodes.size) {
        if (tappedNodes.size >= totalNodes) onExplored()
    }

    val nodes = listOf(
        GraphNode(topic, GrayMatterTheme.colors.neutral400, 0.5f, 0.2f, NodeShape.HEXAGON, labelBottom = "Topic [Folder]"),
        GraphNode("My $resource", GrayMatterTheme.colors.neutral400, 0.5f, 0.5f, NodeShape.TRIANGLE, labelBottom = "Resource [Link, File, Note]"),
        GraphNode(opinion, GrayMatterColors.TypeOpinion, 0.2f, 0.7f, highlighted = true, labelBottom = "Entries"),
        GraphNode("Bookmark", GrayMatterColors.TypeBookmark, 0.2f, 0.85f),
        GraphNode("Annotation", GrayMatterColors.TypeAnnotation, 0.35f, 0.85f),
        GraphNode("Template", GrayMatterColors.TypeTemplate, 0.5f, 0.85f),
        GraphNode("Lookup", GrayMatterColors.TypeLookupMain, 0.65f, 0.85f),
        GraphNode("Vision", GrayMatterColors.TypeVisual, 0.8f, 0.85f)
    )
    val edges = listOf(0 to 1, 1 to 2, 1 to 3, 1 to 4, 1 to 5, 1 to 6, 1 to 7)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("The Relatrix",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp))
        Text("Your knowledge, visualized in a 3D relationship matrix. Tap nodes to explore.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp))

        // Graph canvas
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(visibleNodes) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            nodes.forEachIndexed { i, node ->
                                if (i < visibleNodes) {
                                    val pos = Offset(node.fx * w, node.fy * h)
                                    if ((tapOffset - pos).getDistance() < 60f) {
                                        selectedNode = i
                                        tappedNodes = tappedNodes + i
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw edges
                edges.forEach { (from, to) ->
                    if (from < visibleNodes && to < visibleNodes) {
                        drawLine(
                            Color.White.copy(alpha = 0.12f),
                            Offset(nodes[from].fx * w, nodes[from].fy * h),
                            Offset(nodes[to].fx * w, nodes[to].fy * h),
                            strokeWidth = 2f
                        )
                    }
                }

                // Draw nodes
                nodes.forEachIndexed { i, node ->
                    if (i < visibleNodes) {
                        val center = Offset(node.fx * w, node.fy * h)
                        val isSelected = selectedNode == i

                        when (node.shape) {
                            NodeShape.HEXAGON -> {
                                drawHexagonShape(center, 32f, Color.White, style = Stroke(3f))
                                if (isSelected) drawHexagonShape(center, 36f, Color.White, style = Stroke(2f))
                            }
                            NodeShape.TRIANGLE -> {
                                drawTriangleShape(center, 30f, Color.White, style = Stroke(3f))
                                if (isSelected) drawTriangleShape(center, 34f, Color.White, style = Stroke(2f))
                            }
                            NodeShape.CIRCLE -> {
                                if (node.highlighted) {
                                    drawCircle(node.color.copy(alpha = 0.25f), radius = 18f, center = center)
                                }
                                drawCircle(node.color, radius = 12f, center = center)
                                if (isSelected) drawCircle(Color.White, radius = 16f, center = center, style = Stroke(2f))
                            }
                        }
                    }
                }
            }
            
            // Labels positioned over the canvas, next to the shapes
            nodes.forEachIndexed { i, node ->
                if (i < visibleNodes && node.labelBottom != null) {
                    Text(
                        text = node.labelBottom,
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMatterTheme.colors.neutral400,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (maxWidth * node.fx) + 24.dp, // positioned to the right of the shape
                                y = (maxHeight * node.fy) - 8.dp
                            )
                            .width(120.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Node tooltip
        AnimatedContent(
            targetState = selectedNode,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "node_tooltip"
        ) { idx ->
            if (idx >= 0 && idx < nodes.size) {
                val node = nodes[idx]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(node.color.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (node.shape == NodeShape.CIRCLE) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(node.color))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(node.label, style = MaterialTheme.typography.bodySmall, color = GrayMatterTheme.colors.textPrimary)
                }
            } else {
                Box(modifier = Modifier.height(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("${tappedNodes.size} / $totalNodes explored",
            style = MaterialTheme.typography.labelSmall,
            color = if (tappedNodes.size >= totalNodes) GrayMatterColors.TypeOpinion else GrayMatterTheme.colors.neutral500)
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Slide 8: You're Ready
// ════════════════════════════════════════════════════════════════════════

@Composable
private fun SlideReady() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.RocketLaunch, null,
            tint = GrayMatterColors.CustomizedAccent,
            modifier = Modifier.size(64.dp).padding(bottom = 16.dp))
        Text("You're Ready",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = GrayMatterTheme.colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp))
        Text("Start building your knowledge.",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMatterTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minHeight = 60.dp))
    }
}
