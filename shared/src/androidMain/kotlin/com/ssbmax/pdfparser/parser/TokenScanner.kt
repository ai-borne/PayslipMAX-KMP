package com.ssbmax.pdfparser.parser

import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

/**
 * Emits word-level [PositionedToken]s from a PDFBox page (Phase 2 Android adapter).
 *
 * PDFBox already reports direction-adjusted coordinates in a top-down convention (`yDirAdj` grows
 * downward), so no Y inversion is needed here — unlike the iOS adapter. Glyphs delivered per line by
 * [writeString] are grouped into words by horizontal gap; each word becomes one token whose bounding
 * box is the union of its glyph boxes. This is the raw, un-cropped IR; column/row structure is
 * reconstructed later in common code (Phase 3), not guessed here.
 */
open class TokenScanner : PDFTextStripper() {
    init {
        sortByPosition = true
    }

    private val tokens = mutableListOf<PositionedToken>()

    /** Horizontal gap (in points) beyond which two adjacent glyphs start a new word. */
    private val wordGap = 3f

    fun tokens(): List<PositionedToken> = tokens.toList()

    public override fun writeString(
        text: String?,
        textPositions: MutableList<TextPosition>?,
    ) {
        if (textPositions.isNullOrEmpty()) return

        var word = mutableListOf<TextPosition>()
        var lastRight = -1f
        for (tp in textPositions) {
            val glyph = tp.unicode ?: ""
            val isSpace = glyph.isBlank()
            if (isSpace || (lastRight >= 0f && tp.xDirAdj - lastRight > wordGap)) {
                flush(word)
                word = mutableListOf()
            }
            if (isSpace) {
                lastRight = -1f
            } else {
                word.add(tp)
                lastRight = tp.xDirAdj + tp.widthDirAdj
            }
        }
        flush(word)
    }

    private fun flush(glyphs: List<TextPosition>) {
        if (glyphs.isEmpty()) return
        val text = glyphs.joinToString("") { it.unicode ?: "" }
        if (text.isBlank()) return

        val minX = glyphs.minOf { it.xDirAdj }
        val minY = glyphs.minOf { it.yDirAdj }
        val maxRight = glyphs.maxOf { it.xDirAdj + it.widthDirAdj }
        val maxBottom = glyphs.maxOf { it.yDirAdj + it.heightDir }
        val avgFontSize = glyphs.map { it.fontSizeInPt }.average().toFloat()
        val isBold =
            glyphs.any { tp ->
                val fontName = tp.font?.name ?: ""
                fontName.contains("Bold", ignoreCase = true) ||
                    fontName.contains("Heavy", ignoreCase = true) ||
                    fontName.contains("Black", ignoreCase = true)
            }
        tokens.add(
            PositionedToken(
                text = text,
                x = minX,
                y = minY,
                width = maxRight - minX,
                height = maxBottom - minY,
                fontSize = avgFontSize,
                isBold = isBold,
            ),
        )
    }
}
