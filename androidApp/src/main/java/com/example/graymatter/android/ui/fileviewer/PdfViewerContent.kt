package com.example.graymatter.android.ui.fileviewer

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Native PDF viewer with Auto-Crop and Theme support.
 */
@Composable
fun PdfViewerContent(
    filePath: String,
    currentPage: Int,
    autoCrop: Boolean = true,
    theme: String = "daylight",
    onPageChanged: (page: Int, total: Int) -> Unit,
    onTotalPages: (Int) -> Unit,
    onChaptersFound: (List<com.example.graymatter.domain.ChapterOutline>) -> Unit = {},
    onTextSelectionAction: (action: String, text: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isUrl = remember(filePath) { filePath.startsWith("http") }

    if (isUrl) {
        val displayUrl = remember(filePath) { "https://docs.google.com/gview?embedded=true&url=$filePath" }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    webViewClient = WebViewClient()
                    loadUrl(displayUrl)
                }
            }
        )
    } else {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
        var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var loadError by remember { mutableStateOf<String?>(null) }
        
        val cropCache = remember { mutableMapOf<Int, Rect>() }
        // Low resolution cache for instant placeholder display
        val lowResPageCache = remember { android.util.LruCache<Int, Bitmap>(15) }
        // High resolution cache for sharp reading
        val pageCache = remember { android.util.LruCache<Int, Bitmap>(5) }
        val renderMutex = remember { Mutex() }
        var pdDocument by remember { mutableStateOf<PDDocument?>(null) }
        var extractedCharacters by remember { mutableStateOf<List<PdfCharacter>>(emptyList()) }

        LaunchedEffect(filePath) {
            withContext(Dispatchers.IO) {
                try {
                    isLoading = true
                    loadError = null
                    Log.d("PdfViewer", "Attempting to open PDF: $filePath")
                    
                    try {
                        PDFBoxResourceLoader.init(context)
                    } catch (e: Exception) {
                        Log.e("PdfViewer", "PDFBox init failed", e)
                    }
                    
                    val uri = Uri.parse(filePath)
                    val descriptor = if (uri.scheme == "content") {
                        context.contentResolver.openFileDescriptor(uri, "r")
                    } else {
                        val file = File(filePath)
                        if (file.exists()) {
                            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        } else {
                            null
                        }
                    }

                    if (descriptor == null) {
                        loadError = "Could not open file descriptor for: $filePath"
                        Log.e("PdfViewer", loadError!!)
                        return@withContext
                    }

                    try {
                        val inputStream = if (uri.scheme == "content") {
                            context.contentResolver.openInputStream(uri)
                        } else {
                            java.io.FileInputStream(File(filePath))
                        }
                        if (inputStream != null) {
                            val doc = PDDocument.load(inputStream)
                            pdDocument = doc
                            
                            // Extract Table of Contents (Outlines)
                            val outline = doc.documentInformation.title // Just checking doc
                            val root = doc.documentCatalog.documentOutline
                            if (root != null) {
                                val chapters = extractChaptersRecursive(doc, root.firstChild)
                                if (chapters.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        onChaptersFound(chapters)
                                    }
                                }
                            }
                            inputStream.close()
                        }
                    } catch (e: Exception) {
                        Log.e("PdfViewer", "Failed to load PDDocument for text selection", e)
                    }

                    pfd = descriptor
                    val r = PdfRenderer(descriptor)
                    renderer = r
                    withContext(Dispatchers.Main) {
                        onTotalPages(r.pageCount)
                    }
                } catch (e: SecurityException) {
                    Log.e("PdfViewer", "Permission denied for PDF: $filePath", e)
                    loadError = "Permission denied. Please try re-importing the file."
                } catch (e: Exception) {
                    Log.e("PdfViewer", "Failed to open PDF", e)
                    loadError = "Failed to open PDF: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
            }
        }

        LaunchedEffect(currentPage, renderer, autoCrop) {
            val r = renderer ?: return@LaunchedEffect
            if (currentPage < 0 || currentPage >= r.pageCount) return@LaunchedEffect

            // Extract text in background concurrently
            val doc = pdDocument
            if (doc != null) {
                this.launch(Dispatchers.IO) {
                    try {
                        val stripper = PdfCharacterStripper()
                        val chars = stripper.extractCharacters(doc, currentPage)
                        if (isActive) {
                            withContext(Dispatchers.Main) {
                                extractedCharacters = chars
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e("PdfViewer", "Failed to extract text for page: $currentPage", e)
                    }
                }
            }

            val cachedLowRes = lowResPageCache.get(currentPage)
            val cachedHighRes = pageCache.get(currentPage)

            if (cachedHighRes != null) {
                bitmap = cachedHighRes
                onPageChanged(currentPage, r.pageCount)
            } else if (cachedLowRes != null) {
                bitmap = cachedLowRes
                onPageChanged(currentPage, r.pageCount)
            }

            this.launch(Dispatchers.IO) {
                // If we have nothing cached, instantly fetch and display the low-res placeholder
                if (cachedHighRes == null && cachedLowRes == null && isActive) {
                    val lowRes = getOrRenderPdfPage(currentPage, r, context, autoCrop, cropCache, pageCache, lowResPageCache, renderMutex, isLowRes = true)
                    if (lowRes != null && isActive && bitmap == null) {
                        withContext(Dispatchers.Main) {
                            bitmap = lowRes
                            onPageChanged(currentPage, r.pageCount)
                        }
                    }
                }

                if (!isActive) return@launch
                
                // Always try to fetch/render high res for the actual display
                val b = getOrRenderPdfPage(currentPage, r, context, autoCrop, cropCache, pageCache, lowResPageCache, renderMutex, isLowRes = false)
                if (b != null && isActive && bitmap != cachedHighRes) {
                    withContext(Dispatchers.Main) {
                        bitmap = b
                        onPageChanged(currentPage, r.pageCount)
                    }
                }

                // Prefetch surrounding pages (Low Res first for speed, then High Res)
                if (isActive) {
                    val prefetchPages = listOf(currentPage + 1, currentPage - 1).filter { it in 0 until r.pageCount }
                    
                    for (p in prefetchPages) {
                        if (!isActive) break
                        getOrRenderPdfPage(p, r, context, autoCrop, cropCache, pageCache, lowResPageCache, renderMutex, isLowRes = true)
                    }
                    for (p in prefetchPages) {
                        if (!isActive) break
                        getOrRenderPdfPage(p, r, context, autoCrop, cropCache, pageCache, lowResPageCache, renderMutex, isLowRes = false)
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    renderer?.close()
                    pfd?.close()
                    pdDocument?.close()
                } catch (_: Exception) {}
            }
        }

        val themeColorFilter = remember(theme) { getPdfThemeFilter(theme) }
        val bgColor = getThemeBackgroundColor(theme)

        Box(
            modifier = Modifier.fillMaxSize().background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = GrayMatterColors.Primary)
            } else if (loadError != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Unable to load PDF",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loadError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayMatterColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                bitmap?.let { b ->
                    var imageLayoutSize by remember { mutableStateOf(IntSize.Zero) }
                    val density = LocalDensity.current.density
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = b.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().onSizeChanged { imageLayoutSize = it },
                            contentScale = ContentScale.Fit,
                            colorFilter = themeColorFilter
                        )
                        
                        // Text Selection Overlay
                        if (extractedCharacters.isNotEmpty() && imageLayoutSize != IntSize.Zero) {
                            TextSelectionOverlay(
                                characters = extractedCharacters,
                                imageSize = imageLayoutSize,
                                bitmapSize = IntSize(b.width, b.height),
                                density = density,
                                autoCropRect = cropCache[currentPage],
                                onActionCompleted = { action, text ->
                                    if (text != null) {
                                        onTextSelectionAction(action, text)
                                    }
                                }
                            )
                        }
                    }
                } ?: Text("Preparing page...", color = GrayMatterColors.TextSecondary)
            }
        }
    }
}

/**
 * Generates a ColorFilter based on the selected theme to apply to the rendered PDF bitmap.
 */
private fun getPdfThemeFilter(theme: String): ColorFilter? {
    return when (theme) {
        "night", "amoled_black" -> {
            // Invert colors: Black text on White -> White text on Black
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "twilight" -> {
            // Invert + Indigo tint
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -0.8f, 0f, 0f, 0f, 200f,
                0f, -0.8f, 0f, 0f, 200f,
                0f, 0f, -0.5f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "console" -> {
            // Invert + Green tint
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -0.2f, 0f, 0f, 0f, 50f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -0.2f, 0f, 50f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "sepia", "vintage_parchment" -> {
            // Apply warm sepia tint
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "paper_classic", "book_linen" -> {
            // Very subtle warm tint
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 0.95f, 0f, 0f, 0f,
                0f, 0f, 0.85f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        else -> null
    }
}

private suspend fun getOrRenderPdfPage(
    pageIndex: Int,
    renderer: PdfRenderer,
    context: android.content.Context,
    autoCrop: Boolean,
    cropCache: MutableMap<Int, Rect>,
    pageCache: android.util.LruCache<Int, Bitmap>,
    lowResPageCache: android.util.LruCache<Int, Bitmap>,
    renderMutex: Mutex,
    isLowRes: Boolean
): Bitmap? {
    val targetCache = if (isLowRes) lowResPageCache else pageCache
    val cached = targetCache.get(pageIndex)
    if (cached != null) return cached

    return withContext(Dispatchers.IO) {
        try {
            renderMutex.withLock {
                // Double check cache after acquiring lock
                val doubleCheck = targetCache.get(pageIndex)
                if (doubleCheck != null) return@withLock doubleCheck
                
                // If asking for high res but we already have low res, don't block heavily
                if (!isActive) return@withLock null

                val page = renderer.openPage(pageIndex)
                val density = context.resources.displayMetrics.density
                
                // Adjust scale based on low/high res context
                val scale = if (isLowRes) 0.5f else 1.5f
                val baseWidth = (page.width * density * scale).toInt()
                val baseHeight = (page.height * density * scale).toInt()
                
                var b = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888)
                b.eraseColor(android.graphics.Color.WHITE)
                page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                if (autoCrop && !isLowRes) {
                    val cropRect = cropCache[pageIndex] ?: detectContentBounds(b)
                    cropCache[pageIndex] = cropRect
                    
                    val padding = (16 * density).toInt()
                    val paddedRect = Rect(
                        (cropRect.left - padding).coerceAtLeast(0),
                        (cropRect.top - padding).coerceAtLeast(0),
                        (cropRect.right + padding).coerceAtMost(b.width),
                        (cropRect.bottom + padding).coerceAtMost(b.height)
                    )
                    
                    if (paddedRect.width() > 0 && paddedRect.height() > 0) {
                        b = Bitmap.createBitmap(b, paddedRect.left, paddedRect.top, paddedRect.width(), paddedRect.height())
                    }
                } else if (autoCrop && isLowRes) {
                    // Fast fake crop for low-res if we already have the Rect from a previous high-res render
                    val cachedRect = cropCache[pageIndex]
                    if (cachedRect != null) {
                        // The scale difference between low-res (0.5f) and high-res (1.5f) is 3x
                        val scaleFactor = 3f
                        val scaledLeft = (cachedRect.left / scaleFactor).toInt().coerceAtLeast(0)
                        val scaledTop = (cachedRect.top / scaleFactor).toInt().coerceAtLeast(0)
                        val scaledWidth = (cachedRect.width() / scaleFactor).toInt().coerceAtMost(b.width - scaledLeft)
                        val scaledHeight = (cachedRect.height() / scaleFactor).toInt().coerceAtMost(b.height - scaledTop)
                        
                        if (scaledWidth > 0 && scaledHeight > 0) {
                            b = Bitmap.createBitmap(b, scaledLeft, scaledTop, scaledWidth, scaledHeight)
                        }
                    }
                }
                
                targetCache.put(pageIndex, b)
                b
            }
        } catch (e: Throwable) {
            Log.e("PdfViewer", "Failed to render page $pageIndex", e)
            null
        }
    }
}

@Composable
private fun getThemeBackgroundColor(theme: String): Color {
    return when (theme) {
        "night" -> Color(0xFF1A1A1A)
        "amoled_black" -> Color.Black
        "twilight" -> Color(0xFF12122A)
        "console" -> Color(0xFF0D0D0D)
        "sepia" -> Color(0xFFF4ECD8)
        "vintage_parchment" -> Color(0xFFE8DCC4)
        "paper_classic" -> Color(0xFFFDFCF8)
        "book_linen" -> Color(0xFFF2F0E6)
        else -> Color.White
    }
}


private fun extractChaptersRecursive(
    document: PDDocument,
    item: PDOutlineItem?,
    level: Int = 0
): List<com.example.graymatter.domain.ChapterOutline> {
    val results = mutableListOf<com.example.graymatter.domain.ChapterOutline>()
    var current = item
    while (current != null) {
        val destination = current.destination
        val pageNumber = when {
            destination is PDPageDestination -> {
                document.documentCatalog.pages.indexOf(destination.page)
            }
            current.action is PDActionGoTo -> {
                val action = current.action as PDActionGoTo
                val dest = action.destination
                if (dest is PDPageDestination) {
                    document.documentCatalog.pages.indexOf(dest.page)
                } else -1
            }
            else -> -1
        }

        if (pageNumber != -1) {
            results.add(
                com.example.graymatter.domain.ChapterOutline(
                    title = current.title,
                    level = level,
                    targetPage = pageNumber,
                    children = extractChaptersRecursive(document, current.firstChild, level + 1)
                )
            )
        }
        current = current.nextSibling
    }
    return results
}

/**
 * Detects the bounding box of non-white content in the bitmap for auto-cropping.
 */
private fun detectContentBounds(bitmap: Bitmap): Rect {
    val width = bitmap.width
    val height = bitmap.height
    var firstX = width
    var firstY = height
    var lastX = 0
    var lastY = 0

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixels[y * width + x]
            // Check if pixel is not white (with a small tolerance)
            if (android.graphics.Color.red(color) < 250 || 
                android.graphics.Color.green(color) < 250 || 
                android.graphics.Color.blue(color) < 250) {
                if (x < firstX) firstX = x
                if (y < firstY) firstY = y
                if (x > lastX) lastX = x
                if (y > lastY) lastY = y
            }
        }
    }

    return if (firstX < lastX && firstY < lastY) {
        Rect(firstX, firstY, lastX, lastY)
    } else {
        Rect(0, 0, width, height)
    }
}
