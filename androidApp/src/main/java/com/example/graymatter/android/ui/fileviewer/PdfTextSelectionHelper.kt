package com.example.graymatter.android.ui.fileviewer

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
