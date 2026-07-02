@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectGetWidth
import platform.Foundation.NSMakeRange
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
 * Splits [text] into whitespace-delimited word ranges (start inclusive, end exclusive). Pure and
 * PDFKit-free, so it is unit-testable with synthetic strings (see `IosTokenCoordinatesTest`).
 * Used by [extractPageTokens] to find word boundaries in [PDFPage.string] before resolving each
 * word's visual bounds via PDFKit.
 */
internal fun findWordRanges(text: String): List<IntRange> {
    val ranges = ArrayList<IntRange>()
    var wordStart = -1
    for (i in text.indices) {
        if (text[i].isWhitespace()) {
            if (wordStart >= 0) ranges.add(wordStart until i)
            wordStart = -1
        } else if (wordStart < 0) {
            wordStart = i
        }
    }
    if (wordStart >= 0) ranges.add(wordStart until text.length)
    return ranges
}

/**
 * Emits word-level [PositionedToken]s for a whole PDFKit page by walking [PDFPage.string] via
 * [findWordRanges], then obtaining each word's visual bounds via [PDFPage.selectionForRange] +
 * [PDFSelection.boundsForPage].
 *
 * [selectionForRange] uses [PDFPage.string] indices (as Apple's PDFKit docs specify), giving
 * reliable word-level positions even for PDFs where [PDFPage.characterBoundsAtIndex] returns
 * corrupted glyph bounding boxes — e.g. PCDA payslip PDFs where the per-character bounds are
 * inflated (BPAY reported width 131 pt instead of 27 pt) causing every column-band misclassification.
 *
 * Exception-safe: returns an empty list for null string or empty page.
 */
internal fun extractPageTokens(
    page: PDFPage,
    pageHeight: Double,
): List<PositionedToken> {
    val pageString = page.string ?: return emptyList()
    if (pageString.isEmpty()) return emptyList()

    val tokens = ArrayList<PositionedToken>()
    for (range in findWordRanges(pageString)) {
        val selection =
            page.selectionForRange(
                NSMakeRange(range.first.toULong(), (range.last - range.first + 1).toULong()),
            ) ?: continue
        val bounds = selection.boundsForPage(page)
        val w = CGRectGetWidth(bounds).toFloat()
        val h = CGRectGetHeight(bounds).toFloat()
        if (w <= 0f || h <= 0f) continue
        val x = CGRectGetMinX(bounds).toFloat()
        val yBot = CGRectGetMinY(bounds)
        val y = IosTokenCoordinates.topDownY(yBot, h.toDouble(), pageHeight).toFloat()
        tokens.add(
            PositionedToken(
                text = pageString.substring(range.first, range.last + 1),
                x = x,
                y = y,
                width = w,
                height = h,
                fontSize = h,
                isBold = false,
            ),
        )
    }
    return tokens
}
