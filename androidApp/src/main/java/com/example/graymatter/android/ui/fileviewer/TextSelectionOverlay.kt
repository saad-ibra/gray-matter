package com.example.graymatter.android.ui.fileviewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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

@Composable
fun TextSelectionOverlay(
    characters: List<PdfCharacter>,
    imageSize: IntSize,
    bitmapSize: IntSize,
    density: Float,
    autoCropRect: android.graphics.Rect?,
    onActionCompleted: (action: String, selectedText: String?) -> Unit
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var showPopup by remember { mutableStateOf(false) }

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

    // Mapping a point from screen to PDF coordinates
    fun screenToPdf(screenOffset: Offset): Offset {
        val xInBmp = (screenOffset.x - offsetX) / scale
        val yInBmp = (screenOffset.y - offsetY) / scale

        val uncroppedX = xInBmp + (autoCropRect?.left ?: 0)
        val uncroppedY = yInBmp + (autoCropRect?.top ?: 0)

        // b.width and b.height are density * 1.5f scaled.
        val baseScale = density * 1.5f
        return Offset(
            x = uncroppedX / baseScale,
            y = uncroppedY / baseScale
        )
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

    // Checking linear selection
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragStart = offset
                        dragEnd = offset
                        showPopup = false
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
        // Draw highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hColor = Color(0x66007AFF) // Semi-transparent blue
            for (char in selectedCharacters.value) {
                // PDF -> uncropped -> cropped -> screen
                val baseScale = density * 1.5f
                val uncroppedX = char.x * baseScale
                val uncroppedY = char.y * baseScale
                val uncroppedW = char.width * baseScale
                val uncroppedH = char.height * baseScale

                val croppedX = uncroppedX - (autoCropRect?.left ?: 0)
                val croppedY = uncroppedY - (autoCropRect?.top ?: 0)

                val screenX = croppedX * scale + offsetX
                val screenY = croppedY * scale + offsetY
                val screenW = uncroppedW * scale
                val screenH = uncroppedH * scale

                // To align the blue box effectively we scale exactly directly off char bounds
                // without any vertical baseline bleed
                drawRect(
                    color = hColor,
                    topLeft = Offset(screenX, screenY),
                    size = androidx.compose.ui.geometry.Size(screenW, screenH)
                )
            }
        }

        if (showPopup && dragEnd != null) {
            val popupX = dragEnd!!.x.toInt()
            val popupY = (dragEnd!!.y - 120f).toInt() // show slightly above
            
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
                        onActionCompleted("copy", text)
                    }) {
                        Text("Copy", color = Color.White)
                    }
                    TextButton(onClick = {
                        val text = selectedCharacters.value.sortedWith(compareBy({ it.y }, { it.x })).joinToString("") { it.unicode }
                        dragStart = null
                        dragEnd = null
                        showPopup = false
                        onActionCompleted("annotate", text)
                    }) {
                        Text("Annotate", color = Color.White)
                    }
                }
            }
        }
    }
}
