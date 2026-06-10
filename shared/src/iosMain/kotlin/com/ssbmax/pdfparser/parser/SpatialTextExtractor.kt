@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectGetHeight
import platform.PDFKit.PDFPage

internal data class PdfChar(
    val char: Char,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val index: Int
)

internal fun extractTextSpatially(
    page: PDFPage,
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double
): String {
    val pageString = page.string ?: return ""
    val chars = ArrayList<PdfChar>()

    println("[PdfParserDebug] extractTextSpatially: xMin=$xMin, xMax=$xMax, yMin=$yMin, yMax=$yMax, pageStringLength=${pageString.length}")

    for (i in 0 until pageString.length) {
        val char = pageString[i]
        if (char.isWhitespace()) continue

        val selection = page.selectionForRange(platform.Foundation.NSMakeRange(i.toULong(), 1UL)) ?: continue
        val rectVal = selection.boundsForPage(page)

        val cx = CGRectGetMinX(rectVal)
        val cy = CGRectGetMinY(rectVal)
        val cw = CGRectGetWidth(rectVal)
        val ch = CGRectGetHeight(rectVal)

        if (cw == 0.0 || ch == 0.0) continue

        if (cx >= xMin && cx <= xMax && cy >= yMin && cy <= yMax) {
            chars.add(PdfChar(char, cx, cy, cw, ch, i))
        }
    }

    println("[PdfParserDebug] Filtered chars count: ${chars.size}")
    if (chars.isEmpty()) return ""

    // Group characters into lines based on Y coordinate (descending, top of page is high Y)
    val sortedChars = chars.sortedWith(compareByDescending<PdfChar> { it.y }.thenBy { it.x })
    val lines = mutableListOf<MutableList<PdfChar>>()

    for (c in sortedChars) {
        val line = lines.find { lineChars ->
            val avgY = lineChars.map { it.y }.average()
            kotlin.math.abs(avgY - c.y) < 5.0
        }
        if (line != null) {
            line.add(c)
        } else {
            lines.add(mutableListOf(c))
        }
    }

    // Sort lines by Y descending
    val sortedLines = lines.sortedByDescending { lineChars ->
        lineChars.map { it.y }.average()
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
