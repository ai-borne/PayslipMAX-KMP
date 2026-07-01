@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectGetWidth
import platform.PDFKit.PDFPage

/*
 * Phase 4 note: the legacy string-crop `extractTextSpatially` (and its `PdfChar`) were removed when the
 * iOS parser cut over to the token IR. Only the token adapter ([extractPageTokens]) remains here.
 */

/**
 * Pure coordinate conversion for the iOS token adapter (Phase 2). PDFKit reports bounds in a
 * bottom-up coordinate system (origin bottom-left, y growing upward), while the common token IR
 * uses a single top-down convention (origin top-left, y growing downward — see [PositionedToken]).
 * Isolated here as a pure function so it is unit-testable without PDFKit.
 */
internal object IosTokenCoordinates {
    /** Converts a PDFKit bounding box (its bottom edge [minYBottomUp], [height]) to top-left Y. */
    fun topDownY(
        minYBottomUp: Double,
        height: Double,
        pageHeight: Double,
    ): Double = pageHeight - (minYBottomUp + height)
}

/**
 * Raw PDFKit character data in bottom-up (PDFKit) coordinate space. [x]/[y] are the left/bottom
 * edges of the glyph bounding box as returned by [PDFPage.characterBoundsAtIndex]. Used by
 * [groupCharactersIntoWords] to group page characters into word tokens without holding a PDFKit
 * reference, making the grouping logic unit-testable with synthetic inputs.
 */
internal data class CharBound(
    val char: String,
    val x: Double,
    /** Bottom edge in PDFKit's bottom-up coordinate space. */
    val y: Double,
    val width: Double,
    val height: Double,
)

/**
 * Groups consecutive non-whitespace [CharBound]s into [PositionedToken]s by unioning their bounds
 * and converting from PDFKit bottom-up to the common top-down convention. Pure — no PDFKit
 * dependency — so it is unit-testable with synthetic inputs (see `IosTokenCoordinatesTest`).
 *
 * Zero-size glyphs (PDFKit synthetic characters with no visual extent, e.g., ligature components
 * or word-boundary markers inserted by PDFKit into [PDFPage.string]) are silently skipped — they
 * carry no geometry and must not break a word or corrupt its bounding box.
 */
internal fun groupCharactersIntoWords(
    charBounds: List<CharBound>,
    pageHeight: Double,
): List<PositionedToken> {
    val tokens = ArrayList<PositionedToken>()
    val wordText = StringBuilder()
    var wordMinX = 0.0
    var wordMinY = 0.0
    var wordMaxX = 0.0
    var wordMaxY = 0.0

    fun flush() {
        if (wordText.isEmpty()) return
        val w = wordMaxX - wordMinX
        val h = wordMaxY - wordMinY
        tokens.add(
            PositionedToken(
                text = wordText.toString(),
                x = wordMinX.toFloat(),
                y = IosTokenCoordinates.topDownY(wordMinY, h, pageHeight).toFloat(),
                width = w.toFloat(),
                height = h.toFloat(),
                fontSize = h.toFloat(),
                isBold = false,
            ),
        )
        wordText.clear()
    }

    for (cb in charBounds) {
        if (cb.char.isEmpty() || cb.char[0].isWhitespace()) {
            flush()
            continue
        }
        // Zero-size glyphs: PDFKit inserts synthetic characters into page.string (e.g., to mark word
        // boundaries or represent ligature components). They have no visual extent and no physical
        // position in document space — skip them entirely so they don't corrupt word bounds.
        if (cb.width == 0.0 && cb.height == 0.0) continue
        if (wordText.isEmpty()) {
            wordMinX = cb.x
            wordMinY = cb.y
            wordMaxX = cb.x + cb.width
            wordMaxY = cb.y + cb.height
        } else {
            if (cb.x < wordMinX) wordMinX = cb.x
            if (cb.y < wordMinY) wordMinY = cb.y
            val maxX = cb.x + cb.width
            val maxY = cb.y + cb.height
            if (maxX > wordMaxX) wordMaxX = maxX
            if (maxY > wordMaxY) wordMaxY = maxY
        }
        wordText.append(cb.char)
    }
    flush()

    return tokens
}

/**
 * Emits word-level [PositionedToken]s for a whole PDFKit page using [PDFPage.numberOfCharacters]
 * and [PDFPage.characterBoundsAtIndex] for geometry, then groups them via [groupCharactersIntoWords].
 *
 * This replaces the previous [PDFPage.string] + [PDFPage.selectionForRange] approach, which had an
 * index-space bug: PDFKit inserts synthetic word-boundary spaces into [PDFPage.string], inflating
 * Kotlin regex match indices so that [NSMakeRange] pointed to the wrong internal glyph — producing
 * consistently offset bounds (the dominant GEOMETRY_OFFSET divergence seen in Phase 1). By walking
 * character-by-character with [characterBoundsAtIndex], the glyph index and bounds index stay in
 * lockstep, eliminating the mismatch.
 *
 * Exception-safe: returns an empty list for null [PDFPage.string] or zero character count.
 */
internal fun extractPageTokens(
    page: PDFPage,
    pageHeight: Double,
): List<PositionedToken> {
    val charCount = page.numberOfCharacters.toInt()
    if (charCount <= 0) return emptyList()
    val pageString = page.string ?: return emptyList()
    // Guard against any edge case where PDFKit's character count exceeds the string length.
    val safeCount = minOf(charCount, pageString.length)

    val charBounds = ArrayList<CharBound>(safeCount)
    for (i in 0 until safeCount) {
        val bounds = page.characterBoundsAtIndex(i.toLong())
        charBounds.add(
            CharBound(
                char = pageString[i].toString(),
                x = CGRectGetMinX(bounds),
                y = CGRectGetMinY(bounds),
                width = CGRectGetWidth(bounds),
                height = CGRectGetHeight(bounds),
            ),
        )
    }

    return groupCharactersIntoWords(charBounds, pageHeight)
}
