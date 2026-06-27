@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectGetWidth
import platform.Foundation.NSMakeRange
import platform.PDFKit.PDFPage

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
            ),
        )
    }
    return tokens
}

internal data class PdfChar(
    val char: Char,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val index: Int,
)

internal fun extractTextSpatially(
    page: PDFPage,
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
    charStartIndex: Int = 0,
    charEndIndex: Int = -1,
): String {
    val pageString = page.string ?: return ""
    val chars = ArrayList<PdfChar>()

    val actualEndIdx = if (charEndIndex < 0 || charEndIndex > pageString.length) pageString.length else charEndIndex
    val slicedString = pageString.substring(charStartIndex, actualEndIdx)

    // Extract by WORDS to reduce interop calls to PDFKit
    val words = Regex("\\S+").findAll(slicedString)
    for (match in words) {
        val wordStr = match.value
        val globalStartIdx = charStartIndex + match.range.first
        val nsRange = platform.Foundation.NSMakeRange(globalStartIdx.toULong(), wordStr.length.toULong())
        val selection = page.selectionForRange(nsRange) ?: continue
        val bounds = selection.boundsForPage(page)

        val cx = CGRectGetMinX(bounds)
        val cy = CGRectGetMinY(bounds)
        val cw = CGRectGetWidth(bounds)
        val ch = CGRectGetHeight(bounds)

        if (cw == 0.0 || ch == 0.0) continue

        // Check if the word is roughly within bounds
        if (cx >= xMin && cx <= xMax && cy >= yMin && cy <= yMax) {
            val charWidth = cw / wordStr.length
            for ((charIdx, char) in wordStr.withIndex()) {
                val charX = cx + (charIdx * charWidth)
                val globalIdx = globalStartIdx + charIdx
                chars.add(PdfChar(char, charX, cy, charWidth, ch, globalIdx))
            }
        }
    }

    if (chars.isEmpty()) return ""

    // Group characters into lines based on Y coordinate (descending, top of page is high Y)
    val sortedChars = chars.sortedWith(compareByDescending<PdfChar> { it.y }.thenBy { it.x })
    val lines = mutableListOf<MutableList<PdfChar>>()

    for (c in sortedChars) {
        val line =
            lines.find { lineChars ->
                val firstY = lineChars.first().y
                kotlin.math.abs(firstY - c.y) <= 3.0
            }
        if (line != null) {
            line.add(c)
        } else {
            lines.add(mutableListOf(c))
        }
    }

    // Sort lines by Y descending
    val sortedLines =
        lines.sortedByDescending { lineChars ->
            lineChars.first().y
        }

    // Reconstruct words with space thresholding
    val resultText = StringBuilder()
    for (line in sortedLines) {
        val sortedLine = line.sortedBy { it.x }
        val lineBuilder = StringBuilder()
        var prevChar: PdfChar? = null
        for (c in sortedLine) {
            if (prevChar != null) {
                val hasSpace = (prevChar.index + 1 until c.index).any { pageString[it].isWhitespace() }
                val gap = c.x - (prevChar.x + prevChar.w)
                if (hasSpace || gap > 3.0 || (c.index - prevChar.index) > 1) {
                    lineBuilder.append(' ')
                }
            }
            lineBuilder.append(c.char)
            prevChar = c
        }
        resultText.append(lineBuilder.toString().trim()).append("\n")
    }

    return resultText.toString().trim()
}
