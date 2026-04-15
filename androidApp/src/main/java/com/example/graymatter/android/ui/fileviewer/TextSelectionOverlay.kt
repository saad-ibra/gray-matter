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
import androidx.compose.ui.input.pointer.PointerEventPass
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
    autoCropRect: android.graphics.RectF?,
    cropPadding: Int,
    opinions: List<com.example.graymatter.domain.Opinion> = emptyList(),
    globalLookupWords: Map<String, com.example.graymatter.domain.Opinion> = emptyMap(),
    zoomScale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    renderScale: Float = density * 1.5f,
    onEmptyTap: (Offset, Float) -> Unit = {_, _ -> },
    onSelectionChange: (Boolean) -> Unit = {},
    onNavigateToLookupOrigin: (opinionId: String, itemId: String) -> Unit = { _, _ -> },
    onActionCompleted: (action: String, selectedText: String?, id: String?, startIndex: Int?) -> Unit
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
    
    // Tap to show popup for global dictionary highlights
    var showGlobalDictPopupId by remember { mutableStateOf<String?>(null) }
    var globalDictPopupOffset by remember { mutableStateOf<Offset?>(null) }

    val pageText = remember(characters) { characters.joinToString("") { it.unicode } }

    LaunchedEffect(pageText) {
        dragStart = null
        dragEnd = null
        showPopup = false
        showAnnotationPopupId = null
        annotationPopupOffset = null
        showGlobalDictPopupId = null
        globalDictPopupOffset = null
    }

    // Helper to group characters into contiguous line rectangles (in PDF coordinates)
    fun groupCharactersIntoRects(chars: List<PdfCharacter>): List<android.graphics.RectF> {
        if (chars.isEmpty()) return emptyList()
        val rects = mutableListOf<android.graphics.RectF>()
        
        var currentLineMinX = chars.first().x
        var currentLineMaxX = chars.first().x + chars.first().width
        var currentLineMinY = chars.first().y
        var currentLineMaxY = chars.first().y + chars.first().height
        
        for (i in 1 until chars.size) {
            val char = chars[i]
            // If the char is roughly on the same line (y overlap)
            val centerY = char.y + char.height / 2f
            if (centerY >= currentLineMinY && centerY <= currentLineMaxY) {
                currentLineMinX = minOf(currentLineMinX, char.x)
                currentLineMaxX = maxOf(currentLineMaxX, char.x + char.width)
                currentLineMinY = minOf(currentLineMinY, char.y)
                currentLineMaxY = maxOf(currentLineMaxY, char.y + char.height)
            } else {
                // New line
                rects.add(android.graphics.RectF(currentLineMinX, currentLineMinY, currentLineMaxX, currentLineMaxY))
                currentLineMinX = char.x
                currentLineMaxX = char.x + char.width
                currentLineMinY = char.y
                currentLineMaxY = char.y + char.height
            }
        }
        rects.add(android.graphics.RectF(currentLineMinX, currentLineMinY, currentLineMaxX, currentLineMaxY))
        return rects
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

    // Dynamic scale used for the current bitmap
    val baseRenderScale = renderScale

    // Mapping a point from screen to PDF coordinates
    fun screenToPdf(screenOffset: Offset): Offset {
        val cx = imageSize.width / 2f
        val cy = imageSize.height / 2f
        
        val unzoomedX = (screenOffset.x - panOffset.x - cx) / zoomScale + cx
        val unzoomedY = (screenOffset.y - panOffset.y - cy) / zoomScale + cy

        val xInBmp = (unzoomedX - offsetX) / scale
        val yInBmp = (unzoomedY - offsetY) / scale

        val cropLeft = autoCropRect?.left ?: 0f
        val cropTop = autoCropRect?.top ?: 0f
        
        val padding = if (autoCropRect != null) cropPadding else 0
        
        return Offset(
            x = (xInBmp - padding) / baseRenderScale + cropLeft,
            y = (yInBmp - padding) / baseRenderScale + cropTop
        )
    }

    // Mapping a point from PDF to screen coordinates
    fun pdfToScreen(pdfX: Float, pdfY: Float): Offset {
        val cropLeft = autoCropRect?.left ?: 0f
        val cropTop = autoCropRect?.top ?: 0f
        
        val padding = if (autoCropRect != null) cropPadding else 0
        
        val croppedX = (pdfX - cropLeft) * baseRenderScale + padding
        val croppedY = (pdfY - cropTop) * baseRenderScale + padding

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

        val startIdx = getCharIndexAt(dragStart!!)
        val endIdx = getCharIndexAt(dragEnd!!)

        if (startIdx == -1 || endIdx == -1) return@derivedStateOf emptyList<PdfCharacter>()

        val minIdx = minOf(startIdx, endIdx)
        val maxIdx = maxOf(startIdx, endIdx)

        characters.subList(minIdx, maxIdx + 1)
    }

    val isSelectionActive = selectedCharacters.value.isNotEmpty() || showAnnotationPopupId != null || showGlobalDictPopupId != null
    LaunchedEffect(isSelectionActive) {
        onSelectionChange(isSelectionActive)
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
            } else if (opinion.text.startsWith("[DICT] ")) {
                quote = opinion.text.removePrefix("[DICT] ").trim()
                val startIndex = pageText.indexOf(quote)
                if (startIndex != -1) {
                    val chars = characters.subList(startIndex, minOf(startIndex + quote.length, characters.size))
                    return@mapNotNull opinion.id to chars
                }
            } else if (opinion.text.startsWith("[DICT:")) {
                val match = Regex("\\[DICT:(\\d+)\\] (.*)").find(opinion.text)
                if (match != null) {
                    val index = match.groupValues[1].toInt()
                    quote = match.groupValues[2].trim()
                    if (index >= 0 && index + quote.length <= characters.size) {
                        val chars = characters.subList(index, minOf(index + quote.length, characters.size))
                        val textAtRange = chars.joinToString("") { it.unicode }
                        if (textAtRange.equals(quote, ignoreCase = true)) {
                            return@mapNotNull opinion.id to chars
                        } else {
                            val fallbackIndex = pageText.indexOf(quote)
                            if (fallbackIndex != -1) {
                                val fallbackChars = characters.subList(fallbackIndex, minOf(fallbackIndex + quote.length, characters.size))
                                return@mapNotNull opinion.id to fallbackChars
                            }
                        }
                    }
                }
            }

            if (quote.isNotEmpty() && !opinion.text.startsWith("[DICT")) {
                val startIndex = pageText.indexOf(quote)
                if (startIndex != -1) {
                    val chars = characters.subList(startIndex, minOf(startIndex + quote.length, characters.size))
                    return@mapNotNull opinion.id to chars
                }
            }
            null
        }
    }

    // Global dictionary highlights: find all instances of dictionary phrases
    // that are NOT already covered by local persistentHighlights
    val globalLookupHighlights = remember(globalLookupWords, characters, persistentHighlights) {
        if (globalLookupWords.isEmpty() || characters.isEmpty()) return@remember emptyList<Triple<String, List<PdfCharacter>, com.example.graymatter.domain.Opinion>>()
        
        val results = mutableListOf<Triple<String, List<PdfCharacter>, com.example.graymatter.domain.Opinion>>()
        
        for ((phrase, originOpinion) in globalLookupWords) {
            // Find all instances of this phrase in pageText (case-insensitive)
            val lowerPageText = pageText.lowercase()
            var searchStart = 0
            while (searchStart < lowerPageText.length) {
                val idx = lowerPageText.indexOf(phrase, searchStart)
                if (idx == -1) break
                val endIdx = minOf(idx + phrase.length, characters.size)
                if (endIdx <= characters.size && idx < characters.size) {
                    val matchChars = characters.subList(idx, endIdx)
                    // Check if this range is already covered by a local persistent highlight
                    val isCoveredLocally = persistentHighlights.any { (_, localChars) ->
                        localChars.any { lc -> matchChars.any { mc -> mc === lc } }
                    }
                    if (!isCoveredLocally) {
                        results.add(Triple("global_dict_${originOpinion.id}_$idx", matchChars, originOpinion))
                    }
                }
                searchStart = idx + phrase.length
            }
        }
        results
    }

    // Determine position of start and end drag handles
    val startHandleInfo = derivedStateOf {
        val chars = selectedCharacters.value
        if (chars.isNotEmpty()) {
            val first = chars.first()
            val screenPosBase = pdfToScreen(first.x, first.y + first.height)
            val screenPosTop = pdfToScreen(first.x, first.y)
            
            // Scaled padding to maintain relative shape during zoom
            val hPad = 0.dp.value * density * zoomScale
            val vPad = 5.dp.value * density * zoomScale
            
            val handlePos = Offset(screenPosBase.x - hPad, screenPosBase.y + vPad)
            val height = (screenPosBase.y - screenPosTop.y) + vPad * 2
            Pair(handlePos, height)
        } else null
    }

    val endHandleInfo = derivedStateOf {
        val chars = selectedCharacters.value
        if (chars.isNotEmpty()) {
            val last = chars.last()
            val screenPosBase = pdfToScreen(last.x + last.width, last.y + last.height)
            val screenPosTop = pdfToScreen(last.x + last.width, last.y)
            
            // Scaled padding to maintain relative shape during zoom
            val hPad = 0.dp.value * density * zoomScale
            val vPad = 5.dp.value * density * zoomScale
            
            val handlePos = Offset(screenPosBase.x + hPad, screenPosBase.y + vPad)
            val height = (screenPosBase.y - screenPosTop.y) + vPad * 2
            Pair(handlePos, height)
        } else null
    }

    // Handle Dragging state for the handles themselves
    var isDraggingStartHandle by remember { mutableStateOf(false) }
    var isDraggingEndHandle by remember { mutableStateOf(false) }
    val handleHitRadius = 32.dp.value * density // 32dp touch target

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(persistentHighlights, globalLookupHighlights, zoomScale, panOffset) {
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
                            showGlobalDictPopupId = null
                            globalDictPopupOffset = null
                            dragStart = null
                            dragEnd = null
                            showPopup = false
                        } else {
                            // Check global dictionary highlights
                            val tappedGlobalDict = globalLookupHighlights.find { (_, chars, _) ->
                                if (tappedCharIdx != -1) {
                                    chars.any { it === characters[tappedCharIdx] }
                                } else {
                                    val expandByPdf = (12.dp.value * density) / (baseRenderScale * scale * zoomScale)
                                    val bounds = android.graphics.RectF(
                                        chars.minOf { it.x } - expandByPdf,
                                        chars.minOf { it.y } - expandByPdf,
                                        chars.maxOf { it.x + it.width } + expandByPdf,
                                        chars.maxOf { it.y + it.height } + expandByPdf
                                    )
                                    bounds.contains(pdfOffset.x, pdfOffset.y)
                                }
                            }
                            if (tappedGlobalDict != null) {
                                showGlobalDictPopupId = tappedGlobalDict.first
                                globalDictPopupOffset = offset
                                showAnnotationPopupId = null
                                annotationPopupOffset = null
                                dragStart = null
                                dragEnd = null
                                showPopup = false
                            } else {
                                showAnnotationPopupId = null
                                annotationPopupOffset = null
                                showGlobalDictPopupId = null
                                globalDictPopupOffset = null
                                dragStart = null
                                dragEnd = null
                                showPopup = false
                                onEmptyTap(offset, imageSize.width.toFloat())
                            }
                        }
                    }
                )
            }
            .pointerInput(handleHitRadius) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val shInfo = startHandleInfo.value
                        val ehInfo = endHandleInfo.value
                        val sh = shInfo?.first
                        val eh = ehInfo?.first

                        val distStart = if (sh != null) (down.position - sh).getDistance() else Float.MAX_VALUE
                        val distEnd = if (eh != null) (down.position - eh).getDistance() else Float.MAX_VALUE

                        val hitRadius = handleHitRadius * 1.3f // Generous touch target
                        val isStart = distStart < hitRadius && distStart <= distEnd
                        val isEnd = distEnd < hitRadius && !isStart

                        if (isStart || isEnd) {
                            isDraggingStartHandle = isStart
                            isDraggingEndHandle = isEnd
                            showPopup = false
                            down.consume()

                            // Calculate the initial offset between touch and handle center to prevent jumping
                            val touchOffset = if (isStart) sh!! - down.position else eh!! - down.position

                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null && change.pressed) {
                                    change.consume()
                                    // Use absolute position tracking instead of deltas for 1:1 finger tracking
                                    val currentScreenPos = change.position + touchOffset
                                    
                                    // Convert screen position back to document space and save
                                    val newDocPos = screenToPdf(currentScreenPos)
                                    if (isStart) dragStart = newDocPos
                                    else dragEnd = newDocPos
                                }
                            } while (change != null && change.pressed)

                            showPopup = true
                            isDraggingStartHandle = false
                            isDraggingEndHandle = false
                        }
                    }
                }
            }
            .pointerInput(zoomScale, panOffset) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val docPos = screenToPdf(offset)
                        dragStart = docPos
                        dragEnd = docPos
                        showPopup = false
                        showAnnotationPopupId = null
                        showGlobalDictPopupId = null
                    },
                    onDrag = { change, _ ->
                        dragEnd = screenToPdf(change.position)
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
            
            // Scaled padding to maintain relative shape during zoom
            val hPad = 0.dp.toPx() * zoomScale
            val vPad = 5.dp.toPx() * zoomScale
            
            // Draw Persistent Highlights First
            for ((id, chars) in persistentHighlights) {
                val opinion = opinions.find { it.id == id }
                val isDictionary = opinion?.text?.startsWith("[DICT") == true
                val isAnnotation = opinion?.text?.startsWith("> ") == true
                val color = when {
                    isDictionary -> Color(0xFFD32F2F).copy(alpha = 0.4f)
                    isAnnotation -> GrayMatterColors.Gamboge.copy(alpha = 0.4f)
                    else -> GrayMatterColors.Success.copy(alpha = 0.4f) // opinion = green
                }
                val lineRects = groupCharactersIntoRects(chars)
                for (rect in lineRects) {
                    val pScreen = pdfToScreen(rect.left, rect.top)
                    val screenW = rect.width() * baseRenderScale * scale * zoomScale
                    val screenH = rect.height() * baseRenderScale * scale * zoomScale
                    
                    val paddedX = pScreen.x - hPad
                    val paddedY = pScreen.y - vPad
                    val paddedW = screenW + hPad * 2
                    val paddedH = screenH + vPad * 2
                    
                    drawRect(
                        color = color,
                        topLeft = Offset(paddedX, paddedY),
                        size = androidx.compose.ui.geometry.Size(paddedW, paddedH)
                    )
                }
            }

            // Draw Global Lookup Highlights (pink)
            for ((_, chars, _) in globalLookupHighlights) {
                val lineRects = groupCharactersIntoRects(chars)
                for (rect in lineRects) {
                    val pScreen = pdfToScreen(rect.left, rect.top)
                    val screenW = rect.width() * baseRenderScale * scale * zoomScale
                    val screenH = rect.height() * baseRenderScale * scale * zoomScale
                    
                    val paddedX = pScreen.x - hPad
                    val paddedY = pScreen.y - vPad
                    val paddedW = screenW + hPad * 2
                    val paddedH = screenH + vPad * 2
                    
                    drawRect(
                        color = Color(0xFFF6B3B3).copy(alpha = 0.4f),
                        topLeft = Offset(paddedX, paddedY),
                        size = androidx.compose.ui.geometry.Size(paddedW, paddedH)
                    )
                }
            }

            // Draw Active Selection
            val selectionRects = groupCharactersIntoRects(selectedCharacters.value)
            for (rect in selectionRects) {
                val pScreen = pdfToScreen(rect.left, rect.top)
                val screenW = rect.width() * baseRenderScale * scale * zoomScale
                val screenH = rect.height() * baseRenderScale * scale * zoomScale
                
                val paddedX = pScreen.x - hPad
                val paddedY = pScreen.y - vPad
                val paddedW = screenW + hPad * 2
                val paddedH = screenH + vPad * 2
                
                drawRect(
                    color = selectionColor,
                    topLeft = Offset(paddedX, paddedY),
                    size = androidx.compose.ui.geometry.Size(paddedW, paddedH)
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
            
            val bubbleWidth = 320f
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
                        val textChars = selectedCharacters.value
                        val text = textChars.joinToString("") { it.unicode }
                        val startIndex = characters.indexOf(textChars.first())
                        clipboardManager.setText(AnnotatedString(text))
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                        onActionCompleted("copy", text, null, startIndex)
                    }) {
                        Text("Copy", color = Color.White)
                    }
                    TextButton(onClick = {
                        val textChars = selectedCharacters.value
                        val text = textChars.joinToString("") { it.unicode }
                        val startIndex = characters.indexOf(textChars.first())
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                        onActionCompleted("annotate", text, null, startIndex)
                    }) {
                        Text("Annotate", color = Color.White)
                    }
                    TextButton(onClick = {
                        val textChars = selectedCharacters.value
                        val text = textChars.joinToString("") { it.unicode }
                        val startIndex = characters.indexOf(textChars.first())
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                        onActionCompleted("dictionary", text, null, startIndex)
                    }) {
                        Text("Look Up", color = Color(0xFFC6280B))
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
                        val text = chars.joinToString("") { it.unicode }
                        clipboardManager.setText(AnnotatedString(text))
                        showAnnotationPopupId = null
                        annotationPopupOffset = null
                        onActionCompleted("copy", text, pId, null)
                    }) {
                        Text("Copy", color = Color.White)
                    }
                    
                    // Check if it's a lookup/dictionary entry (robust prefix check)
                    val isDictionary = opinions.find { it.id == pId }?.text?.startsWith("[DICT") == true
                    
                    if (isDictionary) {
                        TextButton(onClick = {
                            val chars = persistentHighlights.find { it.first == pId }?.second ?: emptyList()
                            val text = chars.joinToString("") { it.unicode }
                            showAnnotationPopupId = null
                            annotationPopupOffset = null
                            onActionCompleted("dictionary", text, pId, null)
                        }) {
                            Text("Look Up", color = Color(0xFFC6280B))
                        }
                    } else {
                        // For non-dictionary entries (Opinions, Annotations)
                        TextButton(onClick = {
                            val chars = persistentHighlights.find { it.first == pId }?.second ?: emptyList()
                            val text = chars.joinToString("") { it.unicode }
                            showAnnotationPopupId = null
                            annotationPopupOffset = null
                            onActionCompleted("edit", text, pId, null)
                        }) {
                            Text("Edit", color = Color.White)
                        }
                    }

                    TextButton(onClick = {
                        showAnnotationPopupId = null
                        annotationPopupOffset = null
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                        onActionCompleted("delete", null, pId, null)
                    }) {
                        Text("Delete", color = GrayMatterColors.Error)
                    }
                }
                }
            }
        }

        // Tapped Global Lookup Popup
        val gdId = showGlobalDictPopupId
        val gdOffset = globalDictPopupOffset
        if (gdId != null && gdOffset != null) {
            val globalDictItem = globalLookupHighlights.find { it.first == gdId }
            if (globalDictItem != null) {
                val highlightChars = globalDictItem.second
                val minX = highlightChars.minOf { pdfToScreen(it.x, it.y).x }
                val maxX = highlightChars.maxOf { pdfToScreen(it.x + it.width, it.y).x }
                val minY = highlightChars.minOf { pdfToScreen(it.x, it.y).y }
                val maxY = highlightChars.maxOf { pdfToScreen(it.x, it.y + it.height).y }
                
                val bubbleWidth = 340f
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
                            val text = highlightChars.joinToString("") { it.unicode }
                            clipboardManager.setText(AnnotatedString(text))
                            showGlobalDictPopupId = null
                            globalDictPopupOffset = null
                            onActionCompleted("copy", text, null, null)
                        }) {
                            Text("Copy", color = Color.White)
                        }

                        TextButton(onClick = {
                            showGlobalDictPopupId = null
                            globalDictPopupOffset = null
                            val originOp = globalDictItem.third
                            onNavigateToLookupOrigin(originOp.id, originOp.itemId)
                        }) {
                            Text("Source", color = Color.White)
                        }

                        TextButton(onClick = {
                            val textChars = highlightChars
                            val text = textChars.joinToString("") { it.unicode }
                            val startIndex = characters.indexOf(textChars.first())
                            showGlobalDictPopupId = null
                            globalDictPopupOffset = null
                            val originOp = globalDictItem.third
                            onActionCompleted("dictionary", text, originOp.id, startIndex)
                        }) {
                            Text("Look Up", color = Color(0xFFC6280B))
                        }

                        TextButton(onClick = {
                            showGlobalDictPopupId = null
                            globalDictPopupOffset = null
                            val originOp = globalDictItem.third
                            onActionCompleted("delete", null, originOp.id, null)
                        }) {
                            Text("Delete", color = GrayMatterColors.Error)
                        }
                    }
                }
            }
        }
    }
}
