package com.payslipmax.pdfparser.parser

import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.util.TreeMap
import kotlin.math.abs

open class TableGridExtractor(
    private val yTableStart: Float,
    private val yTableEnd: Float,
    private val xSplit: Float,
    private val xRightBound: Float,
) : PDFTextStripper() {
    init {
        sortByPosition = true
    }

    private val rowMap = TreeMap<Float, MutableList<TextPosition>>()

    public override fun writeString(
        text: String?,
        textPositions: MutableList<TextPosition>?,
    ) {
        if (text == null || textPositions == null || textPositions.isEmpty()) return

        for (tp in textPositions) {
            val cy = tp.yDirAdj
            if (cy in yTableStart..yTableEnd) {
                val matchingY = rowMap.keys.firstOrNull { abs(it - cy) <= 3.0f } ?: cy
                rowMap.getOrPut(matchingY) { mutableListOf() }.add(tp)
            }
        }
    }

    fun extractColumnTexts(): Pair<String, String> {
        val leftSb = StringBuilder()
        val middleSb = StringBuilder()

        for ((_, positions) in rowMap) {
            val sortedPositions = positions.sortedBy { it.xDirAdj }

            val leftLine = buildLineText(sortedPositions.filter { it.xDirAdj < xSplit - 2f })
            val middleLine = buildLineText(sortedPositions.filter { it.xDirAdj in (xSplit - 2f)..xRightBound })

            if (leftLine.isNotEmpty()) leftSb.append(leftLine).append("\n")
            if (middleLine.isNotEmpty()) middleSb.append(middleLine).append("\n")
        }

        return Pair(leftSb.toString().trim(), middleSb.toString().trim())
    }

    private fun buildLineText(positions: List<TextPosition>): String {
        if (positions.isEmpty()) return ""
        val sb = StringBuilder()
        var lastX = -1f
        var lastW = 0f
        for (tp in positions) {
            val cx = tp.xDirAdj
            if (lastX >= 0f && cx - (lastX + lastW) > 3f) {
                sb.append(' ')
            }
            sb.append(tp.unicode)
            lastX = cx
            lastW = tp.widthDirAdj
        }
        return sb.toString().trim()
    }
}
