package com.payslipmax.pdfparser.engine

import com.payslipmax.pdfparser.parser.PositionedToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: [ColumnBoundaryDetector] decides how many columns every payslip table has, and so which cell an amount lands in. Its merge gap
 * (85pt) is tuned against real PCDA layouts: below it, neighbouring left-edges are one column (jitter); at or above it they are two.
 * Nothing pinned that number, so a "harmless" retune could silently merge or split real columns on documents the corpus samples thinly.
 */
class ColumnBoundaryDetectorTest {
    private fun token(
        x: Float,
        y: Float = 0f,
    ) = PositionedToken("t", x, y, 20f, 10f)

    /** Three rows with a token left-edge at each of [xs]: every x is a persistent alignment peak (seen in 2+ rows). */
    private fun rowsWithPeaksAt(vararg xs: Float): List<DetectedRow> =
        (0 until 3).map { r -> DetectedRow(r, xs.map { token(it, r * 20f) }, r * 20f + 5f) }

    @Test
    fun twoPeaksJustUnderTheMergeGapAreOneColumn() {
        val bands = ColumnBoundaryDetector.discoverColumnBands(rowsWithPeaksAt(10f, 94f))

        assertEquals(1, bands.size, "a gap of 84pt is jitter inside one column")
        assertEquals(10f, bands.single().alignmentX, "the earlier peak stays the column's anchor")
    }

    @Test
    fun twoPeaksExactlyAtTheMergeGapAreTwoColumns() {
        val bands = ColumnBoundaryDetector.discoverColumnBands(rowsWithPeaksAt(10f, 95f))

        assertEquals(listOf(10f, 95f), bands.map { it.alignmentX }, "a gap of 85pt is the smallest that separates two columns")
    }

    @Test
    fun aTightFourColumnRowCollapsesToTheColumnsThatAreFarEnoughApart() {
        // 20 and 100 are 80pt apart (< 85) so they merge; 200 and 300 are each >= 85pt from the previous kept anchor.
        val bands = ColumnBoundaryDetector.discoverColumnBands(rowsWithPeaksAt(20f, 100f, 200f, 300f))

        assertEquals(listOf(20f, 200f, 300f), bands.map { it.alignmentX })
    }

    @Test
    fun theBandsTileTheWholePageWithNoGapOrOverlap() {
        val bands = ColumnBoundaryDetector.discoverColumnBands(rowsWithPeaksAt(10f, 95f, 300f))

        assertEquals(0f, bands.first().minX)
        assertEquals(10000f, bands.last().maxX)
        bands.zipWithNext().forEach { (left, right) -> assertEquals(left.maxX, right.minX, "a token must belong to exactly one band") }
        assertEquals(listOf(0, 1, 2), bands.map { it.columnIndex })
    }

    @Test
    fun aLeftEdgeSeenInOnlyOneRowIsNotAColumn() {
        val rows = listOf(DetectedRow(0, listOf(token(10f), token(200f)), 5f), DetectedRow(1, listOf(token(10f)), 25f))

        val bands = ColumnBoundaryDetector.discoverColumnBands(rows)

        assertEquals(listOf(10f), bands.map { it.alignmentX }, "x=200 appears once, so it is noise, not a persistent alignment")
    }

    @Test
    fun withNoPersistentAlignmentTheWholePageIsOneLowConfidenceColumn() {
        val bands = ColumnBoundaryDetector.discoverColumnBands(listOf(DetectedRow(0, listOf(token(10f), token(200f)), 5f)))

        assertEquals(listOf(ColumnBand(0, 0f, 1000f, 0f, 0.5f)), bands)
    }

    @Test
    fun noTokensMeansNoBands() {
        assertTrue(ColumnBoundaryDetector.discoverColumnBands(emptyList()).isEmpty())
    }
}
