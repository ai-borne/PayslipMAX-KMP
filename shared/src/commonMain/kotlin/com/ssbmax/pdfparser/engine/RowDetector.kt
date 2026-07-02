package com.ssbmax.pdfparser.engine

import com.ssbmax.pdfparser.parser.PositionedToken

/** Data container representing a detected visual line of tokens. */
data class DetectedRow(
    val rowIndex: Int,
    val tokens: List<PositionedToken>,
    val centerY: Float,
)

/** Detector that clusters tokens into visual rows based on baseline y-coordinates. */
object RowDetector {
    private const val MIN_TOLERANCE = 3f

    fun detectRows(tokens: List<PositionedToken>): List<DetectedRow> {
        val usable = tokens.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return emptyList()

        val medianHeight = medianOf(usable.map { it.height })
        // 0.25× keeps rowTolerance at MIN_TOLERANCE (3pt) for both PDFBox (~6pt heights) and
        // PDFKit (~12pt heights, which reports line-box rather than character height).
        val rowTolerance = maxOf(MIN_TOLERANCE, medianHeight * 0.25f)

        val sorted = usable.sortedBy { it.centerY }
        val rows = mutableListOf<MutableList<PositionedToken>>()
        var refCenter = Float.NaN

        for (token in sorted) {
            if (rows.isEmpty() || token.centerY - refCenter > rowTolerance) {
                rows.add(mutableListOf(token))
                refCenter = token.centerY
            } else {
                rows.last().add(token)
            }
        }

        return rows.mapIndexed { idx, rowTokens ->
            val sortedRowTokens = rowTokens.sortedBy { it.x }
            val avgY = sortedRowTokens.map { it.centerY }.average().toFloat()
            DetectedRow(rowIndex = idx, tokens = sortedRowTokens, centerY = avgY)
        }
    }

    private fun medianOf(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
    }
}
