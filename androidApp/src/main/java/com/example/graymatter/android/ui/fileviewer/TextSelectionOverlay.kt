package com.example.graymatter.android.ui.fileviewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
    zoomScale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    onEmptyTap: (Offset, Float) -> Unit = {_, _ -> },
    onActionCompleted: (action: String, selectedText: String?, id: String?) -> Unit
) {
    val offsetSaver = Saver<Offset?, String>(
        save = { it?.let { "${it.x},${it.y}" } ?: "" },
        restore = { 
            if (it.isEmpty()) null 
            else {
                val parts = it.split(",")
                Offset(parts[0].toFloat(), parts[1].toFloat())
            }
        }
    )

    var dragStart by rememberSaveable(stateSaver = offsetSaver) { mutableStateOf<Offset?>(null) }
    var dragEnd by rememberSaveable(stateSaver = offsetSaver) { mutableStateOf<Offset?>(null) }
    var showPopup by rememberSaveable { mutableStateOf(false) }
    
    // Tap to show popup for existing annotations
    var showAnnotationPopupId by remember { mutableStateOf<String?>(null) }
    var annotationPopupOffset by remember { mutableStateOf<Offset?>(null) }

    val pageText = remember(characters) { characters.joinToString("") { it.unicode } }

    LaunchedEffect(pageText) {
        dragStart = null
        dragEnd = null
        showPopup = false
        showAnnotationPopupId = null
        annotationPopupOffset = null
    }

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
        val cx = imageSize.width / 2f
        val cy = imageSize.height / 2f
        
        val unzoomedX = (screenOffset.x - panOffset.x - cx) / zoomScale + cx
        val unzoomedY = (screenOffset.y - panOffset.y - cy) / zoomScale + cy

        val xInBmp = (unzoomedX - offsetX) / scale
        val yInBmp = (unzoomedY - offsetY) / scale

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

        val cx = imageSize.width / 2f
        val cy = imageSize.height / 2f
        
        val zoomedX = (screenX - cx) * zoomScale + cx + panOffset.x
        val zoomedY = (screenY - cy) * zoomScale + cy + panOffset.y

        return Offset(zoomedX, zoomedY)
    }

    fun getCharIndexAt(pdfOffset: Offset, expandHitArea: Boolean = false): Int {
        if (characters.isEmpty()) return -1
        
        val padX = if (expandHitArea) 15f / baseRenderScale / scale / zoomScale else 0f
        val padY = if (expandHitArea) 15f / baseRenderScale / scale / zoomScale else 0f

        val exactMatch = characters.indexOfFirst { char ->
            val bounds = android.graphics.RectF(char.x - padX, char.y - padY, char.x + char.width + padX, char.y + char.height + padY)
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
    val startHandleInfo = derivedStateOf {
        val chars = selectedCharacters.value
        if (chars.isNotEmpty()) {
            val first = chars.first()
            val screenPosBase = pdfToScreen(first.x, first.y + first.height)
            val screenPosTop = pdfToScreen(first.x, first.y)
            Pair(Offset(screenPosBase.x, screenPosBase.y), screenPosBase.y - screenPosTop.y)
        } else null
    }

    val endHandleInfo = derivedStateOf {
        val chars = selectedCharacters.value
        if (chars.isNotEmpty()) {
            val last = chars.last()
            val screenPosBase = pdfToScreen(last.x + last.width, last.y + last.height)
            val screenPosTop = pdfToScreen(last.x + last.width, last.y)
            Pair(Offset(screenPosBase.x, screenPosBase.y), screenPosBase.y - screenPosTop.y)
        } else null
    }

    // Handle Dragging state for the handles themselves
    var isDraggingStartHandle by remember { mutableStateOf(false) }
    var isDraggingEndHandle by remember { mutableStateOf(false) }
    val handleHitRadius = 32.dp.value * density // 32dp touch target

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(persistentHighlights) {
                detectTapGestures(
                    onTap = { offset ->
                        val pdfOffset = screenToPdf(offset)
                        // Robust character-based hit check first
                        val tappedCharIdx = getCharIndexAt(pdfOffset, expandHitArea = true)
                        var tappedHighlightId: String? = null

                        if (tappedCharIdx != -1) {
                            tappedHighlightId = persistentHighlights.find { (_, chars) ->
                                chars.any { it === characters[tappedCharIdx] }
                            }?.first
                        }

                        // Backup bounding-box hit check for small gaps
                        if (tappedHighlightId == null) {
                            val expandByPdf = (12.dp.value * density) / (baseRenderScale * scale * zoomScale)
                            for ((id, chars) in persistentHighlights.asReversed()) {
                                val bounds = android.graphics.RectF(
                                    chars.minOf { it.x } - expandByPdf,
                                    chars.minOf { it.y } - expandByPdf,
                                    chars.maxOf { it.x + it.width } + expandByPdf,
                                    chars.maxOf { it.y + it.height } + expandByPdf
                                )
                                if (bounds.contains(pdfOffset.x, pdfOffset.y)) {
                                    tappedHighlightId = id
                                    break
                                }
                            }
                        }

                        if (tappedHighlightId != null) {
                            showAnnotationPopupId = tappedHighlightId
                            annotationPopupOffset = offset
                            dragStart = null
                            dragEnd = null
                            showPopup = false
                        } else {
                            showAnnotationPopupId = null
                            dragStart = null
                            dragEnd = null
                            showPopup = false
                            onEmptyTap(offset, imageSize.width.toFloat())
                        }
                    }
                )
            }
            .pointerInput(handleHitRadius) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val shInfo = startHandleInfo.value
                        val ehInfo = endHandleInfo.value
                        val sh = shInfo?.first
                        val eh = ehInfo?.first

                        val isStart = sh != null && (down.position - sh).getDistance() < handleHitRadius
                        val isEnd = eh != null && (down.position - eh).getDistance() < handleHitRadius

                        if (isStart || isEnd) {
                            isDraggingStartHandle = isStart
                            isDraggingEndHandle = isEnd
                            showPopup = false
                            down.consume()
                            var currentPos = if (isStart) (dragStart ?: down.position) else (dragEnd ?: down.position)

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null && change.pressed) {
                                    change.consume()
                                    currentPos += (change.position - change.previousPosition)
                                    if (isStart) dragStart = currentPos
                                    else dragEnd = currentPos
                                }
                            } while (change != null && change.pressed)

                            showPopup = true
                            isDraggingStartHandle = false
                            isDraggingEndHandle = false
                        }
                    }
                }
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
                    val screenW = char.width * baseRenderScale * scale * zoomScale
                    val screenH = char.height * baseRenderScale * scale * zoomScale
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
                val screenW = char.width * baseRenderScale * scale * zoomScale
                val screenH = char.height * baseRenderScale * scale * zoomScale
                drawRect(
                    color = selectionColor,
                    topLeft = pScreen,
                    size = androidx.compose.ui.geometry.Size(screenW, screenH)
                )
            }

            // Draw Drag Handles
            val shInfo = startHandleInfo.value
            val ehInfo = endHandleInfo.value
            if (shInfo != null && ehInfo != null) {
                val sh = shInfo.first
                val shHeight = shInfo.second
                val eh = ehInfo.first
                val ehHeight = ehInfo.second
                
                val handleRadiusPx = 8.dp.toPx()
                val handleColor = GrayMatterColors.CocoaBrown

                // Left Handle (Start): Tear drop down-left
                drawCircle(color = handleColor, radius = handleRadiusPx, center = sh.copy(x = sh.x - handleRadiusPx / 2f, y = sh.y + handleRadiusPx))
                drawLine(
                    color = handleColor,
                    start = sh,
                    end = sh.copy(y = sh.y - shHeight),
                    strokeWidth = 3.dp.toPx()
                )

                // Right Handle (End): Tear drop down-right
                drawCircle(color = handleColor, radius = handleRadiusPx, center = eh.copy(x = eh.x + handleRadiusPx / 2f, y = eh.y + handleRadiusPx))
                drawLine(
                    color = handleColor,
                    start = eh,
                    end = eh.copy(y = eh.y - ehHeight),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        // Selected Text Popup
        if (showPopup && selectedCharacters.value.isNotEmpty()) {
            val minX = selectedCharacters.value.minOf { pdfToScreen(it.x, it.y).x }
            val maxX = selectedCharacters.value.maxOf { pdfToScreen(it.x + it.width, it.y).x }
            val minY = selectedCharacters.value.minOf { pdfToScreen(it.x, it.y).y }
            val maxY = selectedCharacters.value.maxOf { pdfToScreen(it.x, it.y + it.height).y }
            
            val bubbleWidth = 200f
            val bubbleHeight = 140f
            
            val popupX = ((minX + maxX) / 2f - bubbleWidth / 2f).coerceIn(16f, (imageSize.width - bubbleWidth - 16f).coerceAtLeast(16f))
            var popupY = minY - bubbleHeight - 56f
            if (popupY < 16f) {
                popupY = maxY + 64f
            }
            
            Popup(offset = IntOffset(popupX.toInt(), popupY.toInt())) {
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
            val highlightChars = persistentHighlights.find { it.first == pId }?.second
            if (highlightChars != null && highlightChars.isNotEmpty()) {
                val minX = highlightChars.minOf { pdfToScreen(it.x, it.y).x }
                val maxX = highlightChars.maxOf { pdfToScreen(it.x + it.width, it.y).x }
                val minY = highlightChars.minOf { pdfToScreen(it.x, it.y).y }
                val maxY = highlightChars.maxOf { pdfToScreen(it.x, it.y + it.height).y }
                
                val bubbleWidth = 250f
                val bubbleHeight = 140f
                
                val popupX = ((minX + maxX) / 2f - bubbleWidth / 2f).coerceIn(16f, (imageSize.width - bubbleWidth - 16f).coerceAtLeast(16f))
                var popupY = minY - bubbleHeight - 56f
                if (popupY < 16f) {
                    popupY = maxY + 64f
                }
                
                Popup(offset = IntOffset(popupX.toInt(), popupY.toInt())) {
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
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                        onActionCompleted("delete", null, pId)
                    }) {
                        Text("Delete", color = GrayMatterColors.Error)
                    }
                }
                }
            }
        }
    }
}
