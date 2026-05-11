package com.example.graymatter.android.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.Opinion
import com.example.graymatter.domain.ResourceEntryWithDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates branded PDF documents for resource histories.
 * Uses "Made by Relatrix" branding with Apps design tokens.
 */
object PdfExportService {

    private const val PAGE_WIDTH = 595   // A4 in points (72dpi)
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val LINE_HEIGHT = 18f
    private const val HEADER_HEIGHT = 80f

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

    fun generateResourceHistoryPdf(
        context: Context,
        details: ResourceEntryWithDetails,
        filteredOpinions: List<Opinion>? = null
    ): File? {
        val opinions = filteredOpinions?.sortedBy { it.createdAt }
            ?: details.opinions.sortedBy { it.createdAt }

        val document = PdfDocument()
        var pageNumber = 1
        var currentY = 0f
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Draw background
        canvas.drawColor(bgColor)

        // Draw header
        currentY = drawHeader(canvas, details.resource.title ?: "Untitled", currentY)
        currentY += 16f

        // Draw resource info
        val source = details.resource.url ?: details.resource.filePath?.substringAfterLast("/") ?: ""
        if (source.isNotEmpty()) {
            currentY = drawText(canvas, "Source: $source", MARGIN, currentY, textSecondary, 10f)
            currentY += 8f
        }

        val desc = details.resourceEntry.description
        if (!desc.isNullOrBlank()) {
            currentY = drawWrappedText(canvas, desc, MARGIN, currentY, textSecondary, 10f, PAGE_WIDTH - 2 * MARGIN)
            currentY += 12f
        }

        // Separator
        currentY = drawSeparator(canvas, currentY)
        currentY += 16f

        // Draw each opinion
        opinions.forEachIndexed { index, opinion ->
            // Estimate height needed for this opinion card
            val estimatedHeight = estimateOpinionHeight(opinion, index + 1)

            // Check if we need a new page
            if (currentY + estimatedHeight > PAGE_HEIGHT - MARGIN - 40f) {
                // Draw footer on current page
                drawFooter(canvas, pageNumber)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                canvas.drawColor(bgColor)
                currentY = MARGIN
            }

            currentY = drawOpinionCard(canvas, opinion, index + 1, currentY)
            currentY += 16f
        }

        // Draw footer on last page
        drawFooter(canvas, pageNumber)
        document.finishPage(page)

        // Write to file
        val outputFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.pdf")
        return try {
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            outputFile
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    fun sharePdf(context: Context, pdfFile: File, title: String) {
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, pdfFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
    }

    fun generateTopicPdf(
        context: Context,
        topic: com.example.graymatter.domain.Topic,
        resourceDetails: List<ResourceEntryWithDetails>
    ): File? {
        val document = PdfDocument()
        var pageNumber = 1
        var currentY = 0f
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Draw background
        canvas.drawColor(bgColor)

        // Draw Topic Header
        currentY = drawHeader(canvas, topic.name, 0f, "Topic Analysis")
        currentY += 16f

        // Topic Notes (Synthesis)
        if (!topic.notes.isNullOrBlank()) {
            currentY = drawText(canvas, "Topic Overview", MARGIN, currentY, accentGreen, 12f, true)
            currentY += 8f
            currentY = drawWrappedText(canvas, topic.notes!!, MARGIN, currentY, textPrimary, 11f, PAGE_WIDTH - 2 * MARGIN)
            currentY += 24f
        }

        // List Resources
        currentY = drawText(canvas, "Included Resources (${resourceDetails.size})", MARGIN, currentY, accentGreen, 12f, true)
        currentY += 12f

        resourceDetails.forEach { details ->
            // Check if we need a new page for the resource title
            if (currentY > PAGE_HEIGHT - MARGIN - 100f) {
                drawFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                canvas.drawColor(bgColor)
                currentY = MARGIN
            }

            // Resource title separator
            currentY = drawSeparator(canvas, currentY)
            currentY += 16f
            
            currentY = drawText(canvas, details.resource.title ?: "Untitled Resource", MARGIN, currentY, textPrimary, 14f, true)
            currentY += 8f
            
            val source = details.resource.url ?: details.resource.filePath?.substringAfterLast("/") ?: ""
            if (source.isNotEmpty()) {
                currentY = drawText(canvas, "Source: $source", MARGIN, currentY, textSecondary, 9f)
                currentY += 4f
            }
            
            currentY += 12f

            // Opinions for this resource
            details.opinions.sortedBy { it.createdAt }.forEachIndexed { index, opinion ->
                val estimatedHeight = estimateOpinionHeight(opinion, index + 1)

                if (currentY + estimatedHeight > PAGE_HEIGHT - MARGIN - 40f) {
                    drawFooter(canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    canvas.drawColor(bgColor)
                    currentY = MARGIN
                }

                currentY = drawOpinionCard(canvas, opinion, index + 1, currentY)
                currentY += 16f
            }
            
            currentY += 24f
        }

        // Draw footer on last page
        drawFooter(canvas, pageNumber)
        document.finishPage(page)

        // Write to file
        val outputFile = File(context.cacheDir, "topic_export_${System.currentTimeMillis()}.pdf")
        return try {
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            outputFile
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    private fun drawHeader(canvas: Canvas, title: String, startY: Float, subtitlePrefix: String = "Opinion History"): Float {
        var y = startY + MARGIN

        // Brand bar
        val brandPaint = Paint().apply {
            color = accentGreen
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, y - 4f, PAGE_WIDTH.toFloat(), y + 2f, brandPaint)
        y += 32f // Added line gap after green line

        // Title
        val titlePaint = Paint().apply {
            color = textPrimary
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(subtitlePrefix, MARGIN, y, titlePaint)
        y += 28f

        // Resource title
        val subtitlePaint = Paint().apply {
            color = textSecondary
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val displayTitle = if (title.length > 60) title.take(57) + "..." else title
        canvas.drawText(displayTitle, MARGIN, y, subtitlePaint)
        y += 12f

        // Date
        val datePaint = Paint().apply {
            color = neutralColor
            textSize = 10f
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
        canvas.drawText("Generated on $dateStr", MARGIN, y, datePaint)
        y += 8f

        return y
    }

    private fun drawSeparator(canvas: Canvas, y: Float): Float {
        val paint = Paint().apply {
            color = borderColor
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)
        return y + 4f
    }

    private fun drawOpinionCard(canvas: Canvas, opinion: Opinion, index: Int, startY: Float): Float {
        var y = startY
        val cardLeft = MARGIN
        val cardRight = PAGE_WIDTH - MARGIN
        val cardWidth = cardRight - cardLeft

        // Card background
        val cardPaint = Paint().apply {
            color = cardBg
            style = Paint.Style.FILL
        }
        val cardHeight = estimateOpinionHeight(opinion, index)
        val rect = RectF(cardLeft, y, cardRight, y + cardHeight)
        canvas.drawRoundRect(rect, 8f, 8f, cardPaint)

        // Card border
        val borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        y += 16f
        val innerLeft = cardLeft + 16f
        val innerWidth = cardWidth - 32f

        // Type badge + index
        val isAnnotation = opinion.text.startsWith("> ") || opinion.text.startsWith("[INDEX:")
        val isDictionary = opinion.text.startsWith("[DICT")
        val isTemplate = opinion.text.startsWith("[TEMPLATE:")
        val isVisual = opinion.imagePath != null

        val (typeName, accent) = when {
            isVisual -> "VISUAL" to accentVisual
            isDictionary -> "LOOKUP" to accentLookup
            isAnnotation -> "ANNOTATION" to accentAnnotation
            isTemplate -> "TEMPLATE" to accentTemplate
            opinion.pageNumber != null -> "BOOKMARK" to accentBookmark
            else -> "OPINION" to accentGreen
        }

        val badgePaint = Paint().apply {
            color = accent
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("$index. $typeName", innerLeft, y, badgePaint)

        // Date
        val datePaint = Paint().apply {
            color = neutralColor
            textSize = 9f
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(opinion.createdAt))
        val dateWidth = datePaint.measureText(dateStr)
        canvas.drawText(dateStr, cardRight - 16f - dateWidth, y, datePaint)

        y += 16f

        // Content text
        val cleanText = when {
            isAnnotation -> {
                opinion.text
                    .replace(Regex("\\[INDEX:\\d+\\]\\s*"), "")
                    .replace(Regex("^>\\s*"), "")
                    .trim()
            }
            isDictionary -> opinion.text.substringAfter("] ").trim()
            isTemplate -> {
                opinion.text.substringAfter("]\n")
                    .replace(Regex("### "), "")
                    .trim()
            }
            else -> opinion.text
        }

        y = drawWrappedText(canvas, cleanText, innerLeft, y, textPrimary, 11f, innerWidth)
        y += 8f

        // Draw visual image if present
        if (isVisual) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(opinion.imagePath, options)
                val imgWidth = options.outWidth
                val imgHeight = options.outHeight
                if (imgWidth > 0 && imgHeight > 0) {
                    val scale = innerWidth / imgWidth
                    val visualHeight = imgHeight * scale
                    
                    val bitmap = BitmapFactory.decodeFile(opinion.imagePath)
                    if (bitmap != null) {
                        val destRect = RectF(innerLeft, y, innerLeft + innerWidth, y + visualHeight)
                        canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        y += visualHeight + 12f
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {}
        }
        
        y += 4f

        // Confidence badge
        val confPaint = Paint().apply {
            color = textSecondary
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText("Confidence: ${opinion.confidenceScore}%", innerLeft, y, confPaint)

        // Page number if applicable
        if (opinion.pageNumber != null) {
            val pageText = "Page ${opinion.pageNumber!! + 1}"
            val pageWidth = confPaint.measureText(pageText)
            canvas.drawText(pageText, cardRight - 16f - pageWidth, y, confPaint)
        }

        y += 12f

        return startY + cardHeight
    }

    private fun estimateOpinionHeight(opinion: Opinion, index: Int): Float {
        val cleanText = when {
            opinion.text.startsWith("> ") || opinion.text.startsWith("[INDEX:") ->
                opinion.text
                    .replace(Regex("\\[INDEX:\\d+\\]\\s*"), "")
                    .replace(Regex("^>\\s*"), "")
                    .trim()
            opinion.text.startsWith("[DICT") -> opinion.text.substringAfter("] ").trim()
            opinion.text.startsWith("[TEMPLATE:") ->
                opinion.text.substringAfter("]\n").replace(Regex("### "), "").trim()
            else -> opinion.text
        }

        val paint = Paint().apply {
            textSize = 11f
            isAntiAlias = true
        }
        val availableWidth = PAGE_WIDTH - 2 * MARGIN - 32f
        val lineCount = estimateLineCount(cleanText, paint, availableWidth)

        // Image height estimation
        var visualHeight = 0f
        if (opinion.imagePath != null) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(opinion.imagePath, options)
                if (options.outWidth > 0) {
                    val scale = availableWidth / options.outWidth
                    visualHeight = options.outHeight * scale + 12f
                }
            } catch (e: Exception) {}
        }

        // Header (16) + badge line (16) + text lines + image + confidence (12) + padding (28)
        return 16f + 16f + (lineCount * LINE_HEIGHT) + visualHeight + 12f + 28f
    }

    private fun estimateLineCount(text: String, paint: Paint, maxWidth: Float): Int {
        if (text.isEmpty()) return 1
        val lines = text.split("\n")
        var count = 0
        for (line in lines) {
            if (line.isEmpty()) {
                count++
                continue
            }
            val textWidth = paint.measureText(line)
            count += ((textWidth / maxWidth).toInt() + 1).coerceAtLeast(1)
        }
        return count.coerceAtLeast(1)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, color: Int, size: Float, isBold: Boolean = false): Float {
        val paint = Paint().apply {
            this.color = color
            textSize = size
            isAntiAlias = true
            if (isBold) {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        canvas.drawText(text, x, y, paint)
        return y + LINE_HEIGHT
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, color: Int, size: Float, maxWidth: Float): Float {
        val paint = Paint().apply {
            this.color = color
            textSize = size
            isAntiAlias = true
        }

        var y = startY
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            if (paragraph.isEmpty()) {
                y += LINE_HEIGHT * 0.5f
                continue
            }

            val words = paragraph.split(" ")
            var currentLine = StringBuilder()
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) > maxWidth && currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine.toString(), x, y, paint)
                    y += LINE_HEIGHT
                    currentLine = StringBuilder(word)
                } else {
                    currentLine = StringBuilder(testLine)
                }
            }
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine.toString(), x, y, paint)
                y += LINE_HEIGHT
            }
        }
        return y
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val y = PAGE_HEIGHT - MARGIN / 2

        // Separator line
        val sepPaint = Paint().apply {
            color = borderColor
            strokeWidth = 0.5f
        }
        canvas.drawLine(MARGIN, y - 16f, PAGE_WIDTH - MARGIN, y - 16f, sepPaint)

        // "Made by Relatrix" branding
        val brandPaint = Paint().apply {
            color = accentGreen
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Made by Relatrix", MARGIN, y, brandPaint)

        // Page number
        val pagePaint = Paint().apply {
            color = neutralColor
            textSize = 9f
            isAntiAlias = true
        }
        val pageText = "Page $pageNumber"
        val pageWidth = pagePaint.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN - pageWidth, y, pagePaint)
    }
}
