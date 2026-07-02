package com.ssbmax.pdfparser.engine

import com.ssbmax.pdfparser.parser.PositionedToken

/** Represents an inferred column interval with left/right bounds and confidence score. */
data class ColumnBand(
    val columnIndex: Int,
    val minX: Float,
    val maxX: Float,
    val alignmentX: Float,
    val confidenceScore: Float,
)

/** Detector that infers N stable column bands using persistent alignment peaks and projection profiles. */
object ColumnBoundaryDetector {
    private const val ALIGNMENT_TOLERANCE = 8f

    fun discoverColumnBands(rows: List<DetectedRow>): List<ColumnBand> {
        val allTokens = rows.flatMap { it.tokens }
        if (allTokens.isEmpty()) return emptyList()

        val alignmentPeaks = findAlignmentPeaks(rows)
        if (alignmentPeaks.isEmpty()) {
            return listOf(ColumnBand(0, 0f, 1000f, 0f, 0.5f))
        }

        return buildBandsFromPeaks(alignmentPeaks, allTokens)
    }

    private fun findAlignmentPeaks(rows: List<DetectedRow>): List<Float> {
        val allTokens = rows.flatMap { it.tokens }
        val sortedX = allTokens.map { it.x }.sorted()
        val rawPeaks = mutableListOf<Float>()

        for (x in sortedX) {
            val existing = rawPeaks.find { kotlin.math.abs(it - x) <= ALIGNMENT_TOLERANCE }
            if (existing == null) {
                // Count distinct rows where a token starts at this x coordinate
                val rowCount =
                    rows.count { row ->
                        row.tokens.any { kotlin.math.abs(it.x - x) <= ALIGNMENT_TOLERANCE }
                    }
                if (rowCount >= 2) {
                    rawPeaks.add(x)
                }
            }
        }

        // Merge raw peaks that belong to the same functional column region (< 40pt gap)
        val mergedPeaks = mutableListOf<Float>()
        for (p in rawPeaks.sorted()) {
            if (mergedPeaks.isEmpty()) {
                mergedPeaks.add(p)
            } else {
                val last = mergedPeaks.last()
                if (p - last < 85f) {
                    // keep earlier alignment anchor for column start
                } else {
                    mergedPeaks.add(p)
                }
            }
        }

        return mergedPeaks
    }

    private fun buildBandsFromPeaks(
        peaks: List<Float>,
        tokens: List<PositionedToken>,
    ): List<ColumnBand> {
        val bands = mutableListOf<ColumnBand>()
        for (i in peaks.indices) {
            val peakX = peaks[i]
            val minX = if (i == 0) 0f else (peaks[i - 1] + peakX) / 2f
            val maxX = if (i == peaks.lastIndex) 10000f else (peakX + peaks[i + 1]) / 2f

            val tokensInBand = tokens.filter { it.x >= minX && it.x < maxX }
            val conf = if (tokensInBand.isNotEmpty()) 0.95f else 0.5f

            bands.add(
                ColumnBand(
                    columnIndex = i,
                    minX = minX,
                    maxX = maxX,
                    alignmentX = peakX,
                    confidenceScore = conf,
                ),
            )
        }
        return bands
    }
}
