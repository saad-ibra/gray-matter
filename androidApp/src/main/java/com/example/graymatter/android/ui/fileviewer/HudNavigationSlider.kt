package com.example.graymatter.android.ui.fileviewer

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.domain.ChapterOutline

// ═══════════════════════════════════════════════════════════════
//  HUD NAVIGATION SLIDER — Futuristic dual-state page navigator
// ═══════════════════════════════════════════════════════════════

/** Accent color — electric white with a slight blue tinge */
private val HudAccent = Color(0xFFE8EAFF)
private val HudAccentDim = Color(0xFF9098B0)
val HudDeepDark = Color(0xFF08080C)
private val HudBorder = Color(0xFF22222E)
private val HudTrackBase = Color(0xFF1A1A26)
private val HudTrackFill = Color(0xFFD0D4F0)
private val HudGlow = Color(0xFFCCD0FF)

/**
 * Primary entry point. Renders the full expanded panel OR the slim idle strip
 * based on [isExpanded].
 */
@Composable
fun HudNavigationSlider(
    currentPage: Int,
    totalPages: Int,
    isExpanded: Boolean,
    isBookmarked: Boolean,
    chapters: List<ChapterOutline>,
    onPageSlide: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onBookmarkToggle: () -> Unit,
    isLeftHanded: Boolean = false
) {
    // Flatten chapters once and pre-compute fractional positions
    val flatChapters = remember(chapters) {
        fun flatten(list: List<ChapterOutline>): List<ChapterOutline> =
            list.flatMap { listOf(it) + flatten(it.children) }
        flatten(chapters).sortedBy { it.targetPage }.distinctBy { it.targetPage }
    }

    val chapterFractions = remember(flatChapters, totalPages) {
        if (totalPages <= 1) emptyList()
        else flatChapters.map { it.targetPage.toFloat() / (totalPages - 1).coerceAtLeast(1) }
    }

    val chapterPages = remember(flatChapters) {
        flatChapters.map { it.targetPage }.toSet()
    }

    // Current progress as fraction
    val progressFraction = remember(currentPage, totalPages) {
        if (totalPages <= 1) 0f
        else currentPage.toFloat() / (totalPages - 1).coerceAtLeast(1)
    }

    if (isExpanded) {
        ExpandedHudPanel(
            totalPages = totalPages,
            progressFraction = progressFraction,
            chapterFractions = chapterFractions,
            chapterPages = chapterPages,
            isBookmarked = isBookmarked,
            onPageSlide = onPageSlide,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            onBookmarkToggle = onBookmarkToggle,
            isLeftHanded = isLeftHanded
        )
    } else {
        IdleProgressStrip(
            progressFraction = progressFraction,
            chapterFractions = chapterFractions
        )
    }
}

// ───────────────────────────────────────────────────────────────
//  EXPANDED STATE — Full bottom panel
// ───────────────────────────────────────────────────────────────

@Composable
private fun ExpandedHudPanel(
    totalPages: Int,
    progressFraction: Float,
    chapterFractions: List<Float>,
    chapterPages: Set<Int>,
    isBookmarked: Boolean,
    onPageSlide: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onBookmarkToggle: () -> Unit,
    isLeftHanded: Boolean = false
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Dragging state
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(progressFraction) }
    var lastVibratedChapterPage by remember { mutableIntStateOf(-1) }
    var lastVibratedPage by remember { mutableIntStateOf(-1) }
    var lastVibrationTime by remember { mutableLongStateOf(0L) }

    // Use rememberUpdatedState so the pointerInput closure always sees current chapter data
    val currentChapterPages by rememberUpdatedState(chapterPages)

    // Sync dragFraction when not dragging
    LaunchedEffect(progressFraction) {
        if (!isDragging) {
            dragFraction = progressFraction
        }
    }

    val displayFraction = if (isDragging) dragFraction else progressFraction
    val displayPage = if (totalPages <= 1) 1
    else (displayFraction * (totalPages - 1).coerceAtLeast(1)).toInt().coerceIn(0, totalPages - 1) + 1

    // Subtle glow pulse animation
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Geometric frame border
                val borderWidth = 1.dp.toPx()
                drawRect(
                    color = HudBorder,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    style = Stroke(width = borderWidth)
                )
                // Top accent line
                drawLine(
                    color = HudAccentDim.copy(alpha = 0.4f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = borderWidth
                )
            }
            .background(HudDeepDark)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // ── Page counter row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = if (isLeftHanded) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page counter — monospace, zero-padded
            val pageDigits = totalPages.toString().length
            val pageStr = displayPage.toString().padStart(pageDigits, '0')
            val totalStr = totalPages.toString().padStart(pageDigits, '0')
            Text(
                text = "$pageStr / $totalStr",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = HudAccent
                )
            )
        }

        // ── Track + controls row (original layout: slider then buttons on right) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLeftHanded) {
                // Prev / Next buttons
                HudRepeatingButton(onClick = onPreviousPage, icon = Icons.Default.ChevronLeft)
                HudRepeatingButton(onClick = onNextPage, icon = Icons.Default.ChevronRight)

                Spacer(modifier = Modifier.width(4.dp))

                // Bookmark
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isBookmarked) Color(0xFFFFD700) else HudAccentDim,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Track canvas (fills remaining space)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .pointerInput(totalPages, chapterPages) {
                            if (totalPages <= 1) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = true)
                                isDragging = true
                                val trackWidth = size.width.toFloat()

                                fun xToFraction(x: Float): Float {
                                    return (x / trackWidth).coerceIn(0f, 1f)
                                }

                                fun fractionToPage(f: Float): Int {
                                    val maxPage = (totalPages - 1).coerceAtLeast(0)
                                    return (f * maxPage).toInt().coerceIn(0, maxPage)
                                }

                                val initialFraction = xToFraction(down.position.x)
                                dragFraction = initialFraction
                                val page = fractionToPage(initialFraction)
                                onPageSlide(page)
                                lastVibratedPage = page

                                // Haptic check
                                if (currentChapterPages.contains(page) && page != lastVibratedChapterPage) {
                                    lastVibratedChapterPage = page
                                    triggerHaptic(context)
                                } else if (!currentChapterPages.contains(page)) {
                                    lastVibratedChapterPage = -1
                                }

                                // Track drag movement
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        val finalFraction = xToFraction(change.position.x)
                                        dragFraction = finalFraction
                                        val finalPage = fractionToPage(finalFraction)
                                        onPageSlide(finalPage)
                                        isDragging = false
                                        break
                                    }
                                    val moveFraction = xToFraction(change.position.x)
                                    dragFraction = moveFraction
                                    val movePage = fractionToPage(moveFraction)
                                    onPageSlide(movePage)

                                    // Per-page haptic feedback (throttled to max ~33Hz to prevent crash or stutter)
                                    if (movePage != lastVibratedPage) {
                                        lastVibratedPage = movePage
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastVibrationTime > 30) {
                                            lastVibrationTime = currentTime
                                            try {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            } catch (_: Exception) { }
                                        }
                                    }

                                    // Strong haptic on chapter crossing
                                    if (currentChapterPages.contains(movePage) && movePage != lastVibratedChapterPage) {
                                        lastVibratedChapterPage = movePage
                                        triggerHaptic(context)
                                    } else if (!currentChapterPages.contains(movePage)) {
                                        lastVibratedChapterPage = -1
                                    }

                                    change.consume()
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawExpandedTrack(
                            fraction = displayFraction,
                            chapterFractions = chapterFractions,
                            glowAlpha = glowAlpha,
                            isDragging = isDragging
                        )
                    }
                }
            } else {
                // Track canvas (fills remaining space)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .pointerInput(totalPages, chapterPages) {
                            if (totalPages <= 1) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = true)
                                isDragging = true
                                val trackWidth = size.width.toFloat()

                                fun xToFraction(x: Float): Float {
                                    return (x / trackWidth).coerceIn(0f, 1f)
                                }

                                fun fractionToPage(f: Float): Int {
                                    val maxPage = (totalPages - 1).coerceAtLeast(0)
                                    return (f * maxPage).toInt().coerceIn(0, maxPage)
                                }

                                val initialFraction = xToFraction(down.position.x)
                                dragFraction = initialFraction
                                val page = fractionToPage(initialFraction)
                                onPageSlide(page)
                                lastVibratedPage = page

                                // Haptic check
                                if (currentChapterPages.contains(page) && page != lastVibratedChapterPage) {
                                    lastVibratedChapterPage = page
                                    triggerHaptic(context)
                                } else if (!currentChapterPages.contains(page)) {
                                    lastVibratedChapterPage = -1
                                }

                                // Track drag movement
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        val finalFraction = xToFraction(change.position.x)
                                        dragFraction = finalFraction
                                        val finalPage = fractionToPage(finalFraction)
                                        onPageSlide(finalPage)
                                        isDragging = false
                                        break
                                    }
                                    val moveFraction = xToFraction(change.position.x)
                                    dragFraction = moveFraction
                                    val movePage = fractionToPage(moveFraction)
                                    onPageSlide(movePage)

                                    // Per-page haptic feedback (throttled to max ~33Hz to prevent crash or stutter)
                                    if (movePage != lastVibratedPage) {
                                        lastVibratedPage = movePage
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastVibrationTime > 30) {
                                            lastVibrationTime = currentTime
                                            try {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            } catch (_: Exception) { }
                                        }
                                    }

                                    // Strong haptic on chapter crossing
                                    if (currentChapterPages.contains(movePage) && movePage != lastVibratedChapterPage) {
                                        lastVibratedChapterPage = movePage
                                        triggerHaptic(context)
                                    } else if (!currentChapterPages.contains(movePage)) {
                                        lastVibratedChapterPage = -1
                                    }

                                    change.consume()
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawExpandedTrack(
                            fraction = displayFraction,
                            chapterFractions = chapterFractions,
                            glowAlpha = glowAlpha,
                            isDragging = isDragging
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Prev / Next buttons
                HudRepeatingButton(onClick = onPreviousPage, icon = Icons.Default.ChevronLeft)
                HudRepeatingButton(onClick = onNextPage, icon = Icons.Default.ChevronRight)

                Spacer(modifier = Modifier.width(4.dp))

                // Bookmark
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isBookmarked) Color(0xFFFFD700) else HudAccentDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
//  IDLE STATE — Slim persistent progress strip
// ───────────────────────────────────────────────────────────────

@Composable
fun IdleProgressStrip(
    progressFraction: Float,
    chapterFractions: List<Float>
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(14.dp)
            // No background color — it floats completely transparent over the PDF
    ) {
        val trackY = size.height * 0.6f
        val trackThickness = 2.5.dp.toPx()

        // Base track — semi-transparent white inverted
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(0f, trackY),
            end = Offset(size.width, trackY),
            strokeWidth = trackThickness,
            cap = StrokeCap.Round,
            blendMode = androidx.compose.ui.graphics.BlendMode.Difference
        )

        // Fill — solid inverted
        val fillEnd = size.width * progressFraction
        if (fillEnd > 0f) {
            drawLine(
                color = Color.White,
                start = Offset(0f, trackY),
                end = Offset(fillEnd, trackY),
                strokeWidth = trackThickness,
                cap = StrokeCap.Round,
                blendMode = androidx.compose.ui.graphics.BlendMode.Difference
            )
        }

        // Chapter notches — inverted
        chapterFractions.forEach { cf ->
            val x = size.width * cf
            drawLine(
                color = Color.White,
                start = Offset(x, trackY - 4.dp.toPx()),
                end = Offset(x, trackY + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                blendMode = androidx.compose.ui.graphics.BlendMode.Difference
            )
        }

        // Triangle thumb — inverted
        val thumbX = fillEnd
        val triW = 6.dp.toPx()
        val triH = 5.5.dp.toPx()
        val triTop = trackY - triH - 1.5.dp.toPx()

        val triPath = Path().apply {
            moveTo(thumbX - triW, triTop)
            lineTo(thumbX + triW, triTop)
            lineTo(thumbX, triTop + triH)
            close()
        }

        // Solid inverted fill for the thumb
        drawPath(
            path = triPath,
            color = Color.White,
            style = Fill,
            blendMode = androidx.compose.ui.graphics.BlendMode.Difference
        )
    }
}

// ───────────────────────────────────────────────────────────────
//  CANVAS DRAW FUNCTIONS
// ───────────────────────────────────────────────────────────────

private fun DrawScope.drawExpandedTrack(
    fraction: Float,
    chapterFractions: List<Float>,
    glowAlpha: Float,
    isDragging: Boolean
) {
    val trackY = size.height * 0.55f
    val trackThickness = 2.dp.toPx()
    val fillEnd = size.width * fraction

    // Base track line
    drawLine(
        color = HudTrackBase,
        start = Offset(0f, trackY),
        end = Offset(size.width, trackY),
        strokeWidth = trackThickness,
        cap = StrokeCap.Butt
    )

    // Active fill with glow
    if (fillEnd > 0f) {
        // Glow line (broader, dim)
        drawLine(
            color = HudGlow.copy(alpha = glowAlpha * 0.3f),
            start = Offset(0f, trackY),
            end = Offset(fillEnd, trackY),
            strokeWidth = trackThickness * 3f,
            cap = StrokeCap.Butt
        )
        // Sharp fill line
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(HudTrackFill.copy(alpha = 0.4f), HudTrackFill),
                startX = 0f,
                endX = fillEnd
            ),
            start = Offset(0f, trackY),
            end = Offset(fillEnd, trackY),
            strokeWidth = trackThickness,
            cap = StrokeCap.Butt
        )
    }

    // Chapter notch marks — thin vertical lines
    chapterFractions.forEach { cf ->
        val x = size.width * cf
        val notchHeight = 5.dp.toPx()
        val isPassed = cf <= fraction
        drawLine(
            color = if (isPassed) HudAccent.copy(alpha = 0.6f) else HudAccentDim.copy(alpha = 0.3f),
            start = Offset(x, trackY - notchHeight),
            end = Offset(x, trackY + notchHeight),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Butt
        )
    }

    // ── Triangle thumb (pointing down onto the track) ──
    val thumbX = fillEnd
    val triWidth = if (isDragging) 7.dp.toPx() else 5.5.dp.toPx()
    val triHeight = if (isDragging) 8.dp.toPx() else 6.5.dp.toPx()
    val triTop = trackY - triHeight - 2.dp.toPx()

    val trianglePath = Path().apply {
        moveTo(thumbX - triWidth, triTop)
        lineTo(thumbX + triWidth, triTop)
        lineTo(thumbX, triTop + triHeight)
        close()
    }

    // Outer glow
    if (isDragging) {
        drawPath(
            path = trianglePath,
            color = HudGlow.copy(alpha = glowAlpha),
            style = Stroke(width = 2.5.dp.toPx())
        )
    }

    // Triangle border
    drawPath(
        path = trianglePath,
        color = HudAccent.copy(alpha = 0.9f),
        style = Stroke(width = 1.dp.toPx())
    )

    // Triangle fill
    drawPath(
        path = trianglePath,
        color = if (isDragging) HudAccent else HudAccent.copy(alpha = 0.7f),
        style = Fill
    )

    // Small line from triangle point to track (targeting line)
    drawLine(
        color = HudAccent.copy(alpha = if (isDragging) 0.6f else 0.3f),
        start = Offset(thumbX, triTop + triHeight),
        end = Offset(thumbX, trackY),
        strokeWidth = 0.5.dp.toPx()
    )
}

// ───────────────────────────────────────────────────────────────
//  REPEATING BUTTON (preserved from original)
// ───────────────────────────────────────────────────────────────

@Composable
private fun HudRepeatingButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            while (true) {
                onClick()
                kotlinx.coroutines.delay(200L)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = HudAccentDim,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ───────────────────────────────────────────────────────────────
//  HAPTIC HELPER
// ───────────────────────────────────────────────────────────────

private fun triggerHaptic(context: android.content.Context) {
    try {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        }
    } catch (_: Exception) { }
}

private fun triggerHapticLight(context: android.content.Context) {
    try {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(8, 40))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(8)
            }
        }
    } catch (_: Exception) { }
}
