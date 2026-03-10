package com.example.graymatter.android.ui.fileviewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.graymatter.android.ui.theme.GrayMatterColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun TextSelectionOverlay(
    characters: List<PdfCharacter>,
    imageSize: IntSize,
    bitmapSize: IntSize,
    density: Float,
    autoCropRect: android.graphics.Rect?,
    cropPadding: Int,
    opinions: List<com.example.graymatter.domain.Opinion> = emptyList(),
    onActionCompleted: (action: String, selectedText: String?, id: String?) -> Unit
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    
    // Tap to show popup for existing annotations
    var showAnnotationPopupId by remember { mutableStateOf<String?>(null) }
    var annotationPopupOffset by remember { mutableStateOf<Offset?>(null) }

    val clipboardManager = LocalClipboardManager.current

    // Calculate ContentScale.Fit properties
    val scale = minOf(
        imageSize.width.toFloat() / bitmapSize.width,
        imageSize.height.toFloat() / bitmapSize.height
    )
    
    val scaledBmpWidth = bitmapSize.width * scale
    val scaledBmpHeight = bitmapSize.height * scale
    val offsetX = (imageSize.width - scaledBmpWidth) / 2f
    val offsetY = (imageSize.height - scaledBmpHeight) / 2f

    // The scale used when initially rendering the PDF to a bitmap
    val baseRenderScale = density * 1.5f

    // Mapping a point from screen to PDF coordinates
    fun screenToPdf(screenOffset: Offset): Offset {
        val xInBmp = (screenOffset.x - offsetX) / scale
        val yInBmp = (screenOffset.y - offsetY) / scale

        val cropLeft = autoCropRect?.left ?: 0
        val cropTop = autoCropRect?.top ?: 0
        val uncroppedX = xInBmp + cropLeft - (if (autoCropRect != null) cropPadding else 0)
        val uncroppedY = yInBmp + cropTop - (if (autoCropRect != null) cropPadding else 0)

        return Offset(
            x = uncroppedX / baseRenderScale,
            y = uncroppedY / baseRenderScale
        )
    }

    // Mapping a point from PDF to screen coordinates
    fun pdfToScreen(pdfX: Float, pdfY: Float): Offset {
        val uncroppedX = pdfX * baseRenderScale
        val uncroppedY = pdfY * baseRenderScale

        val cropLeft = autoCropRect?.left ?: 0
        val cropTop = autoCropRect?.top ?: 0
        val croppedX = uncroppedX - cropLeft + (if (autoCropRect != null) cropPadding else 0)
        val croppedY = uncroppedY - cropTop + (if (autoCropRect != null) cropPadding else 0)

        val screenX = croppedX * scale + offsetX
        val screenY = croppedY * scale + offsetY

        return Offset(screenX, screenY)
    }

    fun getCharIndexAt(pdfOffset: Offset): Int {
        if (characters.isEmpty()) return -1
        val exactMatch = characters.indexOfFirst { char ->
            val bounds = android.graphics.RectF(char.x, char.y, char.x + char.width, char.y + char.height)
            bounds.contains(pdfOffset.x, pdfOffset.y)
        }
        if (exactMatch != -1) return exactMatch
        
        return characters.withIndex().minByOrNull { (_, char) ->
            val cx = char.x + char.width / 2f
            val cy = char.y + char.height / 2f
            val dx = cx - pdfOffset.x
            val dy = cy - pdfOffset.y
            dx * dx + dy * dy
        }?.index ?: -1
    }

    val selectedCharacters = derivedStateOf {
        if (dragStart == null || dragEnd == null || characters.isEmpty()) return@derivedStateOf emptyList<PdfCharacter>()
        val startPdf = screenToPdf(dragStart!!)
        val endPdf = screenToPdf(dragEnd!!)

        val startIdx = getCharIndexAt(startPdf)
        val endIdx = getCharIndexAt(endPdf)

        if (startIdx == -1 || endIdx == -1) return@derivedStateOf emptyList<PdfCharacter>()

        val minIdx = minOf(startIdx, endIdx)
        val maxIdx = maxOf(startIdx, endIdx)

        characters.subList(minIdx, maxIdx + 1)
    }

    // Identify characters mapped to existing annoted opinions
    val pageText = remember(characters) { characters.joinToString("") { it.unicode } }
    val persistentHighlights = remember(opinions, characters) {
        opinions.mapNotNull { opinion ->
            var quote = ""
            if (opinion.text.startsWith("> ")) {
                val parts = opinion.text.split("\n\n")
                if (parts.isNotEmpty()) {
                    quote = parts[0].substring(2).trim()
                }
            }
            if (quote.isNotEmpty()) {
                val startIndex = pageText.indexOf(quote)
                if (startIndex != -1) {
                    val chars = characters.subList(startIndex, minOf(startIndex + quote.length, characters.size))
                    return@mapNotNull opinion.id to chars
                }
            }
            null
        }
    }

    // Determine position of start and end drag handles
    val startHandlePos = derivedStateOf {
        val chars = selectedCharacters.value
        if (chars.isNotEmpty()) {
            val first = chars.first()
            val screenPos = pdfToScreen(first.x, first.y + first.height)
            Offset(screenPos.x, screenPos.y)
        } else null
    }

    val endHandlePos = derivedStateOf {
        val chars = selectedCharacters.value
        if (chars.isNotEmpty()) {
            val last = chars.last()
            val screenPos = pdfToScreen(last.x + last.width, last.y + last.height)
            Offset(screenPos.x, screenPos.y)
        } else null
    }

    // Handle Dragging state for the handles themselves
    var isDraggingStartHandle by remember { mutableStateOf(false) }
    var isDraggingEndHandle by remember { mutableStateOf(false) }
    val handleRadius = 24.dp.value * density // 24dp touch target

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val pdfOffset = screenToPdf(offset)
                        val tappedCharIdx = getCharIndexAt(pdfOffset)
                        if (tappedCharIdx != -1) {
                            val tappedHighlight = persistentHighlights.find { (_, chars) ->
                                chars.any { it === characters[tappedCharIdx] }
                            }
                            if (tappedHighlight != null) {
                                showAnnotationPopupId = tappedHighlight.first
                                annotationPopupOffset = offset
                                dragStart = null
                                dragEnd = null
                                showPopup = false
                            } else {
                                showAnnotationPopupId = null
                                dragStart = null
                                dragEnd = null
                                showPopup = false
                            }
                        } else {
                            showAnnotationPopupId = null
                            dragStart = null
                            dragEnd = null
                            showPopup = false
                        }
                    }
                )
            }
            .pointerInput(handleRadius) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val sh = startHandlePos.value
                        val eh = endHandlePos.value
                        isDraggingStartHandle = false
                        isDraggingEndHandle = false
                        
                        if (sh != null && (offset - sh).getDistance() < handleRadius * 2) {
                            isDraggingStartHandle = true
                            showPopup = false
                        } else if (eh != null && (offset - eh).getDistance() < handleRadius * 2) {
                            isDraggingEndHandle = true
                            showPopup = false
                        }
                    },
                    onDrag = { change, _ ->
                        if (isDraggingStartHandle) {
                            change.consume()
                            dragStart = change.position
                        } else if (isDraggingEndHandle) {
                            change.consume()
                            dragEnd = change.position
                        }
                    },
                    onDragEnd = {
                        if (isDraggingStartHandle || isDraggingEndHandle) {
                            showPopup = true
                            isDraggingStartHandle = false
                            isDraggingEndHandle = false
                        }
                    },
                    onDragCancel = {
                        isDraggingStartHandle = false
                        isDraggingEndHandle = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragStart = offset
                        dragEnd = offset
                        showPopup = false
                        showAnnotationPopupId = null
                    },
                    onDrag = { change, _ ->
                        dragEnd = change.position
                    },
                    onDragEnd = {
                        if (selectedCharacters.value.isNotEmpty()) {
                            showPopup = true
                        } else {
                            dragStart = null
                            dragEnd = null
                        }
                    },
                    onDragCancel = {
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val selectionColor = GrayMatterColors.CocoaBrown.copy(alpha = 0.4f)
            val highlightColor = GrayMatterColors.Gamboge.copy(alpha = 0.4f)
            
            // Draw Persistent Highlights First
            for ((_, chars) in persistentHighlights) {
                for (char in chars) {
                    val pScreen = pdfToScreen(char.x, char.y)
                    val screenW = char.width * baseRenderScale * scale
                    val screenH = char.height * baseRenderScale * scale
                    drawRect(
                        color = highlightColor,
                        topLeft = pScreen,
                        size = androidx.compose.ui.geometry.Size(screenW, screenH)
                    )
                }
            }

            // Draw Active Selection
            for (char in selectedCharacters.value) {
                val pScreen = pdfToScreen(char.x, char.y)
                val screenW = char.width * baseRenderScale * scale
                val screenH = char.height * baseRenderScale * scale
                drawRect(
                    color = selectionColor,
                    topLeft = pScreen,
                    size = androidx.compose.ui.geometry.Size(screenW, screenH)
                )
            }

            // Draw Drag Handles
            val sh = startHandlePos.value
            val eh = endHandlePos.value
            if (sh != null && eh != null) {
                // Left Handle (Start)
                drawCircle(
                    color = GrayMatterColors.CocoaBrown,
                    radius = 8.dp.toPx(),
                    center = sh
                )
                // Stem Left
                drawLine(
                    color = GrayMatterColors.CocoaBrown,
                    start = sh.copy(y = sh.y - 8.dp.toPx()),
                    end = sh.copy(y = sh.y - 20.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )

                // Right Handle (End)
                drawCircle(
                    color = GrayMatterColors.CocoaBrown,
                    radius = 8.dp.toPx(),
                    center = eh
                )
                // Stem Right
                drawLine(
                    color = GrayMatterColors.CocoaBrown,
                    start = eh.copy(y = eh.y - 8.dp.toPx()),
                    end = eh.copy(y = eh.y - 20.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Selected Text Popup
        if (showPopup && dragEnd != null) {
            val popupX = dragEnd!!.x.toInt()
            val popupY = (dragEnd!!.y - 140f).toInt()
            
            Popup(offset = IntOffset(popupX.coerceAtLeast(0), popupY.coerceAtLeast(0))) {
                Row(
                    modifier = Modifier
                        .background(GrayMatterColors.SurfaceDark, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    TextButton(onClick = {
                        val text = selectedCharacters.value.sortedWith(compareBy({ it.y }, { it.x })).joinToString("") { it.unicode }
                        clipboardManager.setText(AnnotatedString(text))
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                        onActionCompleted("copy", text, null)
                    }) {
                        Text("Copy", color = Color.White)
                    }
                    TextButton(onClick = {
                        val text = selectedCharacters.value.sortedWith(compareBy({ it.y }, { it.x })).joinToString("") { it.unicode }
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                        onActionCompleted("annotate", text, null)
                    }) {
                        Text("Annotate", color = Color.White)
                    }
                }
            }
        }

        // Tapped Persistent Highlight Popup
        val pId = showAnnotationPopupId
        val pOffset = annotationPopupOffset
        if (pId != null && pOffset != null) {
            Popup(offset = IntOffset(pOffset.x.toInt(), (pOffset.y - 140f).toInt().coerceAtLeast(0))) {
                Row(
                    modifier = Modifier
                        .background(GrayMatterColors.SurfaceDark, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    TextButton(onClick = {
                        val chars = persistentHighlights.find { it.first == pId }?.second ?: emptyList()
                        val text = chars.sortedWith(compareBy({ it.y }, { it.x })).joinToString("") { it.unicode }
                        clipboardManager.setText(AnnotatedString(text))
                        showAnnotationPopupId = null
                        annotationPopupOffset = null
                        onActionCompleted("copy", text, pId)
                    }) {
                        Text("Copy", color = Color.White)
                    }
                    TextButton(onClick = {
                        val chars = persistentHighlights.find { it.first == pId }?.second ?: emptyList()
                        val text = chars.sortedWith(compareBy({ it.y }, { it.x })).joinToString("") { it.unicode }
                        showAnnotationPopupId = null
                        annotationPopupOffset = null
                        onActionCompleted("edit", text, pId)
                    }) {
                        Text("Edit", color = Color.White)
                    }
                    TextButton(onClick = {
                        showAnnotationPopupId = null
                        annotationPopupOffset = null
                        onActionCompleted("delete", null, pId)
                    }) {
                        Text("Delete", color = GrayMatterColors.Error)
                    }
                }
            }
        }
    }
}
