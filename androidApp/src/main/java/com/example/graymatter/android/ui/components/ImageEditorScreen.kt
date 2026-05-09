package com.example.graymatter.android.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imageUri: Uri,
    initialText: String = "",
    initialConfidence: Int = 100,
    onBackClick: () -> Unit,
    onSave: (Bitmap, String, Int) -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(initialText) }
    var confidence by remember { mutableFloatStateOf(initialConfidence / 100f) }

    // Load bitmap from URI
    LaunchedEffect(imageUri) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (originalBitmap == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GrayMatterColors.Primary)
        }
    } else {
        // Crop state: normalized [0..1] coordinates relative to the displayed image
        var cropLeft by remember { mutableFloatStateOf(0.05f) }
        var cropTop by remember { mutableFloatStateOf(0.05f) }
        var cropRight by remember { mutableFloatStateOf(0.95f) }
        var cropBottom by remember { mutableFloatStateOf(0.95f) }
        var isCropped by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Edit Image", style = MaterialTheme.typography.titleMedium, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val cr = if (isCropped) floatArrayOf(cropLeft, cropTop, cropRight, cropBottom) else null
                            val finalBitmap = processBitmap(originalBitmap!!, rotation, flipHorizontal, cr)
                            onSave(finalBitmap, text, (confidence * 100).toInt())
                        }) {
                            Text("Done", color = GrayMatterColors.TypeVisual, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0A0A0A)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("Add optional caption...", color = GrayMatterColors.Neutral500) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GrayMatterColors.TypeVisual,
                                unfocusedBorderColor = GrayMatterColors.Neutral700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Confidence", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(16.dp))
                            Slider(
                                value = confidence,
                                onValueChange = { confidence = it },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = GrayMatterColors.TypeVisual,
                                    activeTrackColor = GrayMatterColors.TypeVisual
                                )
                            )
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EditorAction(icon = Icons.Default.RotateRight, label = "Rotate") {
                                rotation = (rotation + 90f) % 360f
                            }
                            EditorAction(icon = Icons.Default.Flip, label = "Flip") {
                                flipHorizontal = !flipHorizontal
                            }
                            EditorAction(icon = Icons.Default.RestartAlt, label = "Reset") {
                                cropLeft = 0.05f; cropTop = 0.05f
                                cropRight = 0.95f; cropBottom = 0.95f
                                isCropped = false
                                rotation = 0f
                                flipHorizontal = false
                            }
                        }
                    }
                }
            },
            containerColor = Color.Black
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ImageWithFreeFormCrop(
                    bitmap = originalBitmap!!,
                    rotation = rotation,
                    flipHorizontal = flipHorizontal,
                    cropLeft = cropLeft,
                    cropTop = cropTop,
                    cropRight = cropRight,
                    cropBottom = cropBottom,
                    onCropChange = { l, t, r, b ->
                        cropLeft = l; cropTop = t; cropRight = r; cropBottom = b
                        isCropped = true
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

private enum class DragHandle {
    NONE, TOP_LEFT, TOP, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM, BOTTOM_LEFT, LEFT, CENTER
}

@Composable
private fun ImageWithFreeFormCrop(
    bitmap: Bitmap,
    rotation: Float,
    flipHorizontal: Boolean,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    onCropChange: (Float, Float, Float, Float) -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()

        val isRotated = (rotation / 90f).roundToInt() % 2 != 0
        val imgW = if (isRotated) bitmap.height else bitmap.width
        val imgH = if (isRotated) bitmap.width else bitmap.height

        val scale = minOf(maxW / imgW, maxH / imgH)
        val displayW = imgW * scale
        val displayH = imgH * scale

        val handleRadius = with(density) { 10.dp.toPx() }
        val hitArea = with(density) { 24.dp.toPx() }
        val minCropFraction = 0.05f // minimum 5% of image dimension

        Box(
            modifier = Modifier.size(
                width = (displayW / density.density).dp,
                height = (displayH / density.density).dp
            )
        ) {
            // Draw the actual image
            Canvas(modifier = Modifier.fillMaxSize()) {
                val matrix = Matrix().apply {
                    postRotate(rotation, bitmap.width / 2f, bitmap.height / 2f)
                    if (flipHorizontal) {
                        postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                    }
                    val currentWidth = if (isRotated) bitmap.height else bitmap.width
                    val currentHeight = if (isRotated) bitmap.width else bitmap.height
                    val s = minOf(size.width / currentWidth, size.height / currentHeight)
                    postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                    postScale(s, s)
                    postTranslate(size.width / 2f, size.height / 2f)
                }
                drawContext.canvas.nativeCanvas.drawBitmap(bitmap, matrix, null)
            }

            // Crop overlay with drag interaction
            var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
            
            val currentCropLeft by rememberUpdatedState(cropLeft)
            val currentCropTop by rememberUpdatedState(cropTop)
            val currentCropRight by rememberUpdatedState(cropRight)
            val currentCropBottom by rememberUpdatedState(cropBottom)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(displayW, displayH) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val cx = currentCropLeft * displayW
                                val cy = currentCropTop * displayH
                                val cw = (currentCropRight - currentCropLeft) * displayW
                                val ch = (currentCropBottom - currentCropTop) * displayH
                                val cr = cx + cw
                                val cb = cy + ch
                                val midX = cx + cw / 2
                                val midY = cy + ch / 2

                                activeHandle = when {
                                    // Corners first (highest priority)
                                    offset.near(cx, cy, hitArea) -> DragHandle.TOP_LEFT
                                    offset.near(cr, cy, hitArea) -> DragHandle.TOP_RIGHT
                                    offset.near(cx, cb, hitArea) -> DragHandle.BOTTOM_LEFT
                                    offset.near(cr, cb, hitArea) -> DragHandle.BOTTOM_RIGHT
                                    // Edge midpoints
                                    offset.near(midX, cy, hitArea) -> DragHandle.TOP
                                    offset.near(midX, cb, hitArea) -> DragHandle.BOTTOM
                                    offset.near(cx, midY, hitArea) -> DragHandle.LEFT
                                    offset.near(cr, midY, hitArea) -> DragHandle.RIGHT
                                    // Inside crop = move
                                    offset.x in cx..cr && offset.y in cy..cb -> DragHandle.CENTER
                                    else -> DragHandle.NONE
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / displayW
                                val dy = dragAmount.y / displayH

                                var nL = currentCropLeft
                                var nT = currentCropTop
                                var nR = currentCropRight
                                var nB = currentCropBottom

                                when (activeHandle) {
                                    DragHandle.TOP_LEFT -> { nL += dx; nT += dy }
                                    DragHandle.TOP -> { nT += dy }
                                    DragHandle.TOP_RIGHT -> { nR += dx; nT += dy }
                                    DragHandle.RIGHT -> { nR += dx }
                                    DragHandle.BOTTOM_RIGHT -> { nR += dx; nB += dy }
                                    DragHandle.BOTTOM -> { nB += dy }
                                    DragHandle.BOTTOM_LEFT -> { nL += dx; nB += dy }
                                    DragHandle.LEFT -> { nL += dx }
                                    DragHandle.CENTER -> {
                                        val w = currentCropRight - currentCropLeft
                                        val h = currentCropBottom - currentCropTop
                                        nL = (currentCropLeft + dx).coerceIn(0f, 1f - w)
                                        nT = (currentCropTop + dy).coerceIn(0f, 1f - h)
                                        nR = nL + w
                                        nB = nT + h
                                    }
                                    DragHandle.NONE -> {}
                                }

                                // Enforce bounds & minimum size
                                if (activeHandle != DragHandle.CENTER && activeHandle != DragHandle.NONE) {
                                    nL = nL.coerceIn(0f, nR - minCropFraction)
                                    nT = nT.coerceIn(0f, nB - minCropFraction)
                                    nR = nR.coerceIn(nL + minCropFraction, 1f)
                                    nB = nB.coerceIn(nT + minCropFraction, 1f)
                                }

                                onCropChange(nL, nT, nR, nB)
                            },
                            onDragEnd = { activeHandle = DragHandle.NONE }
                        )
                    }
            ) {
                val cxPx = cropLeft * size.width
                val cyPx = cropTop * size.height
                val crPx = cropRight * size.width
                val cbPx = cropBottom * size.height
                val cropW = crPx - cxPx
                val cropH = cbPx - cyPx

                val dimColor = Color.Black.copy(alpha = 0.55f)

                // Draw 4 dim strips around the crop area (GPU-safe, no BlendMode needed)
                // Top strip
                drawRect(dimColor, topLeft = Offset.Zero, size = Size(size.width, cyPx))
                // Bottom strip
                drawRect(dimColor, topLeft = Offset(0f, cbPx), size = Size(size.width, size.height - cbPx))
                // Left strip (between top and bottom)
                drawRect(dimColor, topLeft = Offset(0f, cyPx), size = Size(cxPx, cropH))
                // Right strip (between top and bottom)
                drawRect(dimColor, topLeft = Offset(crPx, cyPx), size = Size(size.width - crPx, cropH))

                // Crop border
                val accentColor = GrayMatterColors.TypeVisual
                drawRect(
                    color = accentColor,
                    topLeft = Offset(cxPx, cyPx),
                    size = Size(cropW, cropH),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Rule of thirds grid
                val thirdW = cropW / 3f
                val thirdH = cropH / 3f
                for (i in 1..2) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(cxPx + thirdW * i, cyPx),
                        end = Offset(cxPx + thirdW * i, cbPx),
                        strokeWidth = 0.5f.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(cxPx, cyPx + thirdH * i),
                        end = Offset(crPx, cyPx + thirdH * i),
                        strokeWidth = 0.5f.dp.toPx()
                    )
                }

                // Corner handles (L-shaped brackets)
                val cornerLen = min(cropW, cropH) * 0.12f
                val cornerStroke = 3.dp.toPx()
                val corners = listOf(
                    Offset(cxPx, cyPx) to arrayOf(Offset(cxPx + cornerLen, cyPx), Offset(cxPx, cyPx + cornerLen)),
                    Offset(crPx, cyPx) to arrayOf(Offset(crPx - cornerLen, cyPx), Offset(crPx, cyPx + cornerLen)),
                    Offset(cxPx, cbPx) to arrayOf(Offset(cxPx + cornerLen, cbPx), Offset(cxPx, cbPx - cornerLen)),
                    Offset(crPx, cbPx) to arrayOf(Offset(crPx - cornerLen, cbPx), Offset(crPx, cbPx - cornerLen)),
                )
                corners.forEach { (corner, ends) ->
                    drawLine(accentColor, corner, ends[0], strokeWidth = cornerStroke)
                    drawLine(accentColor, corner, ends[1], strokeWidth = cornerStroke)
                }

                // Edge midpoint handles (small bars)
                val edgeMids = listOf(
                    Offset(cxPx + cropW / 2, cyPx),   // top
                    Offset(cxPx + cropW / 2, cbPx),   // bottom
                    Offset(cxPx, cyPx + cropH / 2),    // left
                    Offset(crPx, cyPx + cropH / 2),    // right
                )
                val barHalf = 12.dp.toPx()
                edgeMids.forEachIndexed { idx, pt ->
                    if (idx < 2) {
                        // horizontal bar for top/bottom
                        drawLine(accentColor, Offset(pt.x - barHalf, pt.y), Offset(pt.x + barHalf, pt.y), strokeWidth = cornerStroke)
                    } else {
                        // vertical bar for left/right
                        drawLine(accentColor, Offset(pt.x, pt.y - barHalf), Offset(pt.x, pt.y + barHalf), strokeWidth = cornerStroke)
                    }
                }
            }
        }
    }
}

private fun Offset.near(x: Float, y: Float, radius: Float): Boolean {
    return (this.x - x) * (this.x - x) + (this.y - y) * (this.y - y) <= radius * radius
}

private fun processBitmap(
    original: Bitmap,
    rotation: Float,
    flipHorizontal: Boolean,
    cropNormalized: FloatArray? // [left, top, right, bottom] in 0..1
): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(rotation)
    if (flipHorizontal) matrix.postScale(-1f, 1f)

    var result = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

    if (cropNormalized != null) {
        val x = (cropNormalized[0] * result.width).toInt().coerceIn(0, result.width - 1)
        val y = (cropNormalized[1] * result.height).toInt().coerceIn(0, result.height - 1)
        val w = ((cropNormalized[2] - cropNormalized[0]) * result.width).toInt().coerceIn(1, result.width - x)
        val h = ((cropNormalized[3] - cropNormalized[1]) * result.height).toInt().coerceIn(1, result.height - y)
        result = Bitmap.createBitmap(result, x, y, w, h)
    }

    return result
}
