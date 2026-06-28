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
 * Emits word-level [PositionedToken]s for a whole PDFKit page (Phase 2 iOS adapter), normalizing
 * PDFKit's bottom-up bounds to the common top-down convention so common code sees one coordinate
 * system regardless of platform. Mirrors the word-extraction strategy of [extractTextSpatially]
 * but returns positioned tokens instead of a cropped string.
 */
internal fun extractPageTokens(
    page: PDFPage,
    pageHeight: Double,
): List<PositionedToken> {
    val pageString = page.string ?: return emptyList()
    val tokens = ArrayList<PositionedToken>()
    for (match in Regex("\\S+").findAll(pageString)) {
        val wordStr = match.value
        val nsRange = NSMakeRange(match.range.first.toULong(), wordStr.length.toULong())
        val selection = page.selectionForRange(nsRange) ?: continue
        val bounds = selection.boundsForPage(page)
        val cx = CGRectGetMinX(bounds)
        val cyBottom = CGRectGetMinY(bounds)
        val cw = CGRectGetWidth(bounds)
        val ch = CGRectGetHeight(bounds)
        if (cw == 0.0 || ch == 0.0) continue

        tokens.add(
            PositionedToken(
                text = wordStr,
                x = cx.toFloat(),
                y = IosTokenCoordinates.topDownY(cyBottom, ch, pageHeight).toFloat(),
                width = cw.toFloat(),
                height = ch.toFloat(),
                fontSize = ch.toFloat(),
                isBold = false,
            ),
        )
    }
    return tokens
}
