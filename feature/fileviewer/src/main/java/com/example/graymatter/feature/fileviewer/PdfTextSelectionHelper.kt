package com.example.graymatter.feature.fileviewer

import android.graphics.RectF
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

data class PdfCharacter(
    val unicode: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

class PdfCharacterStripper : PDFTextStripper() {
    private val _characters = mutableListOf<PdfCharacter>()
    
    val characters: List<PdfCharacter>
        get() = _characters.toList()

    override fun processTextPosition(text: TextPosition) {
        _characters.add(
            PdfCharacter(
                unicode = text.unicode,
                x = text.xDirAdj,
                y = text.yDirAdj - text.heightDir,
                width = text.widthDirAdj,
                height = text.heightDir
            )
        )
        super.processTextPosition(text)
    }

    fun extractCharacters(document: PDDocument, page: Int): List<PdfCharacter> {
        _characters.clear()
        try {
            this.startPage = page + 1 // PDFBox pages are 1-indexed
            this.endPage = page + 1
            val dummy = OutputStreamWriter(ByteArrayOutputStream())
            writeText(document, dummy)
        } catch (e: Exception) {
            Log.e("PdfCharacterStripper", "Failed to extract text for page $page", e)
        }
        return characters
    }
}


data class AssembledPdfText(
    val text: String,
    val charIndices: List<Int>
)

fun List<PdfCharacter>.assemblePdfTextWithMap(): AssembledPdfText {
    if (this.isEmpty()) return AssembledPdfText("", emptyList())
    val sb = java.lang.StringBuilder()
    val indices = mutableListOf<Int>()
    
    sb.append(this[0].unicode)
    for (k in 0 until this[0].unicode.length) {
        indices.add(0)
    }
    
    for (i in 1 until this.size) {
        val prev = this[i - 1]
        val curr = this[i]
        
        val yDiff = Math.abs(curr.y - prev.y)
        
        if (yDiff > prev.height * 0.5f) {
            if (!sb.endsWith(" ") && !curr.unicode.startsWith(" ") && !sb.endsWith("-")) {
                sb.append(" ")
                indices.add(i)
            } else if (sb.endsWith("-") && !curr.unicode.startsWith(" ")) {
                sb.deleteCharAt(sb.length - 1)
                indices.removeAt(indices.size - 1)
            }
        } else {
            val xGap = curr.x - (prev.x + prev.width)
            if (xGap > prev.width * 0.5f && !sb.endsWith(" ") && !curr.unicode.startsWith(" ")) {
                sb.append(" ")
                indices.add(i)
            }
        }
        
        sb.append(curr.unicode)
        for (k in 0 until curr.unicode.length) {
            indices.add(i)
        }
    }
    
    val rawText = sb.toString()
    val cleanSb = java.lang.StringBuilder()
    val cleanIndices = mutableListOf<Int>()
    
    var j = 0
    while (j < rawText.length) {
        val c = rawText[j]
        if (c == '\r' || c == '\n') {
            if (cleanSb.isEmpty() || cleanSb.last() != ' ') {
                cleanSb.append(' ')
                cleanIndices.add(indices[j])
            }
            if (c == '\r' && j + 1 < rawText.length && rawText[j+1] == '\n') {
                j++
            }
        } else if (c == ' ') {
            if (cleanSb.isEmpty() || cleanSb.last() != ' ') {
                cleanSb.append(' ')
                cleanIndices.add(indices[j])
            }
        } else {
            cleanSb.append(c)
            cleanIndices.add(indices[j])
        }
        j++
    }
    
    var startIdx = 0
    while (startIdx < cleanSb.length && cleanSb[startIdx] == ' ') {
        startIdx++
    }
    var endIdx = cleanSb.length
    while (endIdx > startIdx && cleanSb[endIdx - 1] == ' ') {
        endIdx--
    }
    
    if (startIdx >= endIdx) return AssembledPdfText("", emptyList())
    
    return AssembledPdfText(
        cleanSb.substring(startIdx, endIdx),
        cleanIndices.subList(startIdx, endIdx)
    )
}

fun List<PdfCharacter>.assemblePdfText(): String {
    return this.assemblePdfTextWithMap().text
}
