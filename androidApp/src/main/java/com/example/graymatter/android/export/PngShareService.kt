package com.example.graymatter.android.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.graymatter.domain.Opinion
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates shareable PNG images for individual knowledge history items.
 * Uses the "Apps" design system with dark theme and branded elements.
 */
object PngShareService {

    private const val IMAGE_WIDTH = 1080
    private const val PADDING = 64f
    private const val CORNER_RADIUS = 32f

    private val bgColor = Color.rgb(0, 0, 0)
    private val cardBg = Color.rgb(12, 12, 12)
    private val textPrimary = Color.rgb(255, 255, 255)
    private val textSecondary = Color.rgb(161, 161, 170)
    private val accentGreen = Color.rgb(142, 162, 88)
    private val accentBookmark = Color.rgb(196, 168, 78)
    private val accentAnnotation = Color.rgb(196, 122, 90)
    private val accentTemplate = Color.rgb(126, 106, 140)
    private val accentVisual = Color.rgb(90, 158, 140)
    private val accentLookup = Color.rgb(191, 90, 106)
    private val borderColor = Color.rgb(30, 30, 34)
    private val neutralColor = Color.rgb(98, 98, 106)

    fun generateOpinionImage(
        context: Context,
        opinion: Opinion,
        resourceTitle: String? = null
    ): File? {
        // Determine type
        val isAnnotation = opinion.text.startsWith("> ") || opinion.text.startsWith("[INDEX:")
        val isDictionary = opinion.text.startsWith("[DICT")
        val isTemplate = opinion.text.startsWith("[TEMPLATE:")
        val isVisual = opinion.imagePath != null
        val hasPage = opinion.pageNumber != null

        val (typeName, accentColor) = when {
            isVisual -> "VISUAL" to accentVisual
            isDictionary -> "LOOKUP" to accentLookup
            isAnnotation -> "ANNOTATION" to accentAnnotation
            isTemplate -> "TEMPLATE" to accentTemplate
            hasPage -> "BOOKMARK" to accentBookmark
            else -> "OPINION" to accentGreen
        }

        // Clean text
        val cleanText = when {
            isAnnotation -> {
                val cleaned = opinion.text
                    .replace(Regex("\\[INDEX:\\d+\\]\\s*"), "")
                    .replace(Regex("^>\\s*"), "")
                    .trim()
                cleaned
            }
            isDictionary -> opinion.text.substringAfter("] ").trim()
            isTemplate -> {
                opinion.text.substringAfter("]\n")
                    .replace(Regex("### "), "▸ ")
                    .trim()
            }
            else -> opinion.text
        }

        // Measure text height
        val textPaint = Paint().apply {
            color = textPrimary
            textSize = 36f
            isAntiAlias = true
        }
        val textWidth = IMAGE_WIDTH - 2 * PADDING - 80f
        val textLines = wrapText(cleanText, textPaint, textWidth)

        // Calculate image height
        val headerHeight = 120f
        var textHeight = textLines.size * 52f
        
        // Image support for visual entries
        var visualImageBitmap: Bitmap? = null
        var visualImageHeight = 0f
        if (isVisual) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(opinion.imagePath, options)
                
                val imgWidth = options.outWidth
                val imgHeight = options.outHeight
                
                if (imgWidth > 0 && imgHeight > 0) {
                    val scale = (IMAGE_WIDTH - 2 * PADDING - 32f) / imgWidth
                    visualImageHeight = imgHeight * scale
                    
                    // Load actual bitmap scaled down if needed to avoid OOM
                    val loadOptions = BitmapFactory.Options().apply {
                        inSampleSize = if (imgWidth > 2000) 2 else 1
                    }
                    visualImageBitmap = BitmapFactory.decodeFile(opinion.imagePath, loadOptions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val metaHeight = 80f
        val footerHeight = 100f
        val totalHeight = (PADDING + headerHeight + 32f + textHeight + (if (isVisual) visualImageHeight + 32f else 0f) + 32f + metaHeight + footerHeight + PADDING).toInt()

        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(bgColor)

        var y = PADDING

        // Accent stripe at top
        val stripePaint = Paint().apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), 6f, stripePaint)

        // Card background
        val cardLeft = PADDING - 16f
        val cardRight = IMAGE_WIDTH - PADDING + 16f
        val cardTop = y - 8f
        val cardBottom = totalHeight - PADDING - footerHeight + 16f
        val cardPaint = Paint().apply {
            color = cardBg
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cardLeft, cardTop, cardRight, cardBottom), CORNER_RADIUS, CORNER_RADIUS, cardPaint)
        val cardBorderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(cardLeft, cardTop, cardRight, cardBottom), CORNER_RADIUS, CORNER_RADIUS, cardBorderPaint)

        // Type badge
        val badgePaint = Paint().apply {
            color = accentColor
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.15f
        }
        canvas.drawText(typeName, PADDING + 16f, y + 36f, badgePaint)

        // Confidence pill
        val confText = "${opinion.confidenceScore}%"
        val confPaint = Paint().apply {
            color = textSecondary
            textSize = 24f
            isAntiAlias = true
        }
        val confWidth = confPaint.measureText(confText)
        val pillPaint = Paint().apply {
            color = Color.rgb(26, 26, 30)
            style = Paint.Style.FILL
        }
        val pillLeft = cardRight - 32f - confWidth - 24f
        canvas.drawRoundRect(RectF(pillLeft, y + 12f, cardRight - 32f, y + 48f), 16f, 16f, pillPaint)
        canvas.drawText(confText, pillLeft + 12f, y + 40f, confPaint)

        y += 56f

        // Date
        val datePaint = Paint().apply {
            color = neutralColor
            textSize = 22f
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(opinion.createdAt))
        canvas.drawText(dateStr, PADDING + 16f, y + 24f, datePaint)

        // Page indicator
        if (hasPage) {
            val pageStr = "Page ${opinion.pageNumber!! + 1}"
            val pageWidth = datePaint.measureText(pageStr)
            canvas.drawText(pageStr, cardRight - 32f - pageWidth, y + 24f, datePaint)
        }

        y += 56f

        // Separator
        val sepPaint = Paint().apply {
            color = borderColor
            strokeWidth = 1f
        }
        canvas.drawLine(PADDING + 16f, y, cardRight - 32f, y, sepPaint)
        y += 24f

        // Content text
        textLines.forEach { line ->
            canvas.drawText(line, PADDING + 24f, y, textPaint)
            y += 52f
        }

        y += 16f

        // Draw visual image if present
        if (visualImageBitmap != null) {
            val destRect = RectF(PADDING + 16f, y, cardRight - 32f, y + visualImageHeight)
            canvas.drawBitmap(visualImageBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
            y += visualImageHeight + 32f
            visualImageBitmap.recycle()
        }

        // Meta section
        if (resourceTitle != null) {
            val metaPaint = Paint().apply {
                color = textSecondary
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }
            val truncTitle = if (resourceTitle.length > 50) resourceTitle.take(47) + "..." else resourceTitle
            canvas.drawText("from: $truncTitle", PADDING + 16f, y, metaPaint)
        }

        // Footer - branding
        val footerY = totalHeight - PADDING
        val footerSepY = footerY - 40f
        canvas.drawLine(PADDING, footerSepY, IMAGE_WIDTH - PADDING, footerSepY, sepPaint)

        val brandPaint = Paint().apply {
            color = accentColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Made by Relatrix", PADDING, footerY, brandPaint)

        val appPaint = Paint().apply {
            color = neutralColor
            textSize = 20f
            isAntiAlias = true
        }
        val appText = "GrayMatter"
        val appWidth = appPaint.measureText(appText)
        canvas.drawText(appText, IMAGE_WIDTH - PADDING - appWidth, footerY, appPaint)

        // Save to cache
        val outputFile = File(context.cacheDir, "share_opinion_${System.currentTimeMillis()}.png")
        return try {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            outputFile
        } catch (e: Exception) {
            bitmap.recycle()
            null
        }
    }

    fun shareImage(context: Context, imageFile: File, title: String) {
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, imageFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Knowledge"))
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            if (paragraph.isEmpty()) {
                result.add("")
                continue
            }
            val words = paragraph.split(" ")
            var currentLine = StringBuilder()
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) > maxWidth && currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    currentLine = StringBuilder(testLine)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }
        return if (result.isEmpty()) listOf("") else result
    }
}
