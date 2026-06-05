package com.example.graymatter.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.graymatter.android.R
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.theme.GrayMatterTheme

@Composable
fun TutorialOverlay(onDismiss: () -> Unit) {
    var currentSlide by remember { mutableIntStateOf(0) }

    val slides = listOf(
        TutorialSlide(
            title = "Welcome to Relatrix",
            description = "Your personal knowledge management system.",
            iconRes = R.drawable.app_logo_full,
            iconTint = Color.Unspecified
        ),
        TutorialSlide(
            title = "Add Resources",
            description = "Tap the + button to add PDFs, web links, notes, or images to your library.",
            illustration = { AddResourcesIllustration() },
            iconTint = GrayMatterTheme.colors.primary
        ),
        TutorialSlide(
            title = "Organize in Topics",
            description = "Every resource lives in a Topic. Group related resources together so you can easily synthesize them later.",
            illustration = { TopicOrganizationIllustration() },
            iconTint = GrayMatterTheme.colors.primary
        ),
        TutorialSlide(
            title = "Record Your Thinking",
            description = "Your library is just the beginning. The real value comes from capturing your understanding.",
            icon = Icons.Default.Edit,
            iconTint = GrayMatterColors.TypeOpinion
        ),
        TutorialSlide(
            title = "Types of Entries",
            description = "Relatrix supports many ways to capture your knowledge:",
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            iconTint = GrayMatterTheme.colors.primary,
            textAlign = TextAlign.Start,
            listItems = listOf(
                TutorialListItem(Icons.Default.TextSnippet, GrayMatterColors.TypeOpinion, "Opinions", "Your personal thoughts"),
                TutorialListItem(Icons.Default.Highlight, GrayMatterColors.TypeAnnotation, "Annotations", "Highlights with notes"),
                TutorialListItem(Icons.Default.ListAlt, GrayMatterColors.TypeTemplate, "Templates", "Structured forms"),
                TutorialListItem(Icons.Default.MenuBook, GrayMatterColors.TypeLookupMain, "Lookups", "Dictionary definitions"),
                TutorialListItem(Icons.Default.ImageSearch, GrayMatterColors.TypeVisual, "Vision", "Image analysis")
            )
        ),
        TutorialSlide(
            title = "Explore the Relatrix",
            description = "The knowledge graph visualizes connections. Tap a node to preview it, or double-tap to jump directly to it.",
            illustration = { RelatrixGraphIllustration() },
            iconTint = GrayMatterColors.TypeLink
        ),
        TutorialSlide(
            title = "You're Ready",
            description = "Start building your knowledge.",
            icon = Icons.Default.RocketLaunch,
            iconTint = GrayMatterColors.CustomizedAccent
        )
    )

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
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "slide_transition"
                    ) { page ->
                        val slide = slides[page]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (slide.illustration != null) {
                                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                                    slide.illustration.invoke()
                                }
                            } else if (slide.icon != null) {
                                Icon(
                                    imageVector = slide.icon,
                                    contentDescription = null,
                                    tint = slide.iconTint,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(bottom = 16.dp)
                                )
                            } else if (slide.iconRes != null) {
                                Image(
                                    painter = painterResource(id = slide.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(bottom = 16.dp)
                                )
                            }

                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = GrayMatterTheme.colors.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            if (slide.listItems.isNullOrEmpty()) {
                                Text(
                                    text = slide.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GrayMatterTheme.colors.textSecondary,
                                    textAlign = slide.textAlign,
                                    modifier = Modifier.defaultMinSize(minHeight = 60.dp)
                                )
                            } else {
                                Text(
                                    text = slide.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GrayMatterTheme.colors.textSecondary,
                                    textAlign = slide.textAlign,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    slide.listItems.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = item.iconTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = GrayMatterTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = " - ${item.description}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = GrayMatterTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            slides.indices.forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == currentSlide) GrayMatterTheme.colors.primary
                                            else GrayMatterTheme.colors.neutral800
                                        )
                                )
                            }
                        }

                        // Buttons
                        if (currentSlide < slides.size - 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (currentSlide > 0) {
                                    TextButton(onClick = { currentSlide-- }) {
                                        Text("Back", color = GrayMatterTheme.colors.neutral500)
                                    }
                                }
                                Button(
                                    onClick = { currentSlide++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary)
                                ) {
                                    Text("Next", color = GrayMatterTheme.colors.onPrimary)
                                }
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = GrayMatterTheme.colors.primary)
                            ) {
                                Text("Get Started", color = GrayMatterTheme.colors.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddResourcesIllustration() {
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        // A big + button with floating icons around it
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(GrayMatterTheme.colors.primary.copy(alpha = 0.1f))
                .border(1.dp, GrayMatterTheme.colors.primary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = GrayMatterTheme.colors.primary, modifier = Modifier.size(28.dp))
        }
        Icon(Icons.Default.TextSnippet, null, tint = GrayMatterColors.TypeOpinion, modifier = Modifier.size(24.dp).offset(x = (-30).dp, y = (-25).dp))
        Icon(Icons.Default.Image, null, tint = GrayMatterColors.TypeVisual, modifier = Modifier.size(24.dp).offset(x = 35.dp, y = (-15).dp))
        Icon(Icons.Default.PictureAsPdf, null, tint = GrayMatterColors.TypeLookupMain, modifier = Modifier.size(24.dp).offset(x = (-25).dp, y = 35.dp))
        Icon(Icons.Default.Language, null, tint = GrayMatterColors.TypeLink, modifier = Modifier.size(24.dp).offset(x = 30.dp, y = 30.dp))
    }
}

@Composable
private fun TopicOrganizationIllustration() {
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = GrayMatterColors.TypeBookmark,
            modifier = Modifier.size(72.dp).offset(y = 10.dp)
        )
        Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = GrayMatterTheme.colors.primary,
            modifier = Modifier.size(28.dp).offset(x = (-16).dp, y = (-24).dp)
        )
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = GrayMatterColors.TypeLink,
            modifier = Modifier.size(24.dp).offset(x = 24.dp, y = (-16).dp)
        )
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = GrayMatterTheme.colors.primary,
            modifier = Modifier.size(20.dp).offset(x = 0.dp, y = (-6).dp)
        )
    }
}

@Composable
private fun RelatrixGraphIllustration() {
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        val lineColor = GrayMatterColors.TypeLink.copy(alpha = 0.5f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val node1 = Offset(size.width * 0.15f, size.height * 0.25f)
            val node2 = Offset(size.width * 0.85f, size.height * 0.2f)
            val node3 = Offset(size.width * 0.75f, size.height * 0.85f)
            val node4 = Offset(size.width * 0.25f, size.height * 0.75f)

            // Draw lines
            drawLine(lineColor, center, node1, strokeWidth = 3f)
            drawLine(lineColor, center, node2, strokeWidth = 3f)
            drawLine(lineColor, center, node3, strokeWidth = 3f)
            drawLine(lineColor, center, node4, strokeWidth = 3f)
            drawLine(lineColor.copy(alpha = 0.2f), node1, node4, strokeWidth = 2f)
            drawLine(lineColor.copy(alpha = 0.2f), node2, node3, strokeWidth = 2f)

            // Draw nodes
            drawCircle(GrayMatterColors.TypeLink, radius = 20f, center = center)
            drawCircle(GrayMatterColors.TypeOpinion, radius = 12f, center = node1)
            drawCircle(GrayMatterColors.TypeAnnotation, radius = 14f, center = node2)
            drawCircle(GrayMatterColors.TypeTemplate, radius = 12f, center = node3)
            drawCircle(GrayMatterColors.TypeBookmark, radius = 10f, center = node4)
        }
    }
}

private data class TutorialSlide(
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val illustration: (@Composable () -> Unit)? = null,
    val iconTint: Color,
    val textAlign: TextAlign = TextAlign.Center,
    val listItems: List<TutorialListItem>? = null
)

private data class TutorialListItem(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val description: String
)
