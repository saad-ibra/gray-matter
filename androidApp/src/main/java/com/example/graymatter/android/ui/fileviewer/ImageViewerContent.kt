package com.example.graymatter.android.ui.fileviewer

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.io.File

/**
 * Full-screen image viewer with pinch-to-zoom and pan gestures.
 */
@Composable
fun ImageViewerContent(
    filePath: String,
    context: Context
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(File(filePath))
                .crossfade(true)
                .build()
        )

        Image(
            painter = painter,
            contentDescription = "Document Image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        )
    }
}
