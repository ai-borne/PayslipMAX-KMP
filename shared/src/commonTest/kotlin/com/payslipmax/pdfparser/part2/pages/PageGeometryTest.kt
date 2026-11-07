package com.payslipmax.pdfparser.part2.pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: every stored page must have its long edge at most 1600 px (DECISIONS Round 10) without being stretched, skewed or enlarged, and a
 * decoder must be told to read a huge photo at a fraction of its size instead of in full. One pure rule for both platforms.
 */
class PageGeometryTest {
    @Test
    fun aLandscapePageShrinksToTheLongEdgeKeepingItsShape() {
        assertEquals(1600 to 1067, PageGeometry.fit(3000, 2000, 1600))
    }

    @Test
    fun aPortraitPageShrinksTheSameWay() {
        assertEquals(1067 to 1600, PageGeometry.fit(2000, 3000, 1600))
    }

    @Test
    fun aPageAlreadyWithinTheLimitIsNeverEnlarged() {
        assertEquals(800 to 600, PageGeometry.fit(800, 600, 1600))
        assertEquals(1600 to 1200, PageGeometry.fit(1600, 1200, 1600))
    }

    @Test
    fun aPageOnePixelOverShrinksJustEnough() {
        assertEquals(1600 to 1200, PageGeometry.fit(1601, 1201, 1600))
    }

    @Test
    fun theShortEdgeNeverReachesZero() {
        assertEquals(1600 to 1, PageGeometry.fit(10000, 1, 1600))
        assertEquals(1600 to 2, PageGeometry.fit(10000, 10, 1600))
    }

    @Test
    fun hugeDimensionsDoNotOverflow() {
        assertEquals(1600 to 1, PageGeometry.fit(Int.MAX_VALUE, 1000, 1600))
    }

    @Test
    fun noSizeAtAllIsNotAPage() {
        assertNull(PageGeometry.fit(0, 100, 1600))
        assertNull(PageGeometry.fit(100, 0, 1600))
        assertNull(PageGeometry.fit(-5, 100, 1600))
    }

    @Test
    fun aDecoderReadsAtTheLargestPowerOfTwoFractionThatStillKeepsTheTargetEdge() {
        assertEquals(1, PageGeometry.sampleSize(3000, 2000, 1600), "3000 / 2 = 1500 would fall below the 1600 target")
        assertEquals(2, PageGeometry.sampleSize(6000, 4000, 1600))
        assertEquals(2, PageGeometry.sampleSize(3200, 100, 1600), "exactly the target after halving is allowed")
        assertEquals(8, PageGeometry.sampleSize(20000, 100, 1600))
        assertEquals(1, PageGeometry.sampleSize(800, 600, 1600))
    }

    @Test
    fun theLongEdgeDecidesNotTheWidth() {
        assertEquals(2, PageGeometry.sampleSize(100, 6000, 1600))
    }

    @Test
    fun theDecodeLimitIsInclusiveAndTakenInLongArithmetic() {
        assertTrue(PageGeometry.pixelsWithinLimit(10_000, 10_000), "exactly the limit is allowed")
        assertFalse(PageGeometry.pixelsWithinLimit(10_000, 10_001))
        assertTrue(PageGeometry.pixelsWithinLimit(12_000, 2_000), "a wide panorama is judged by its area, not by its width squared")
        assertFalse(PageGeometry.pixelsWithinLimit(50_000, 50_000), "2.5 billion pixels is negative in 32-bit arithmetic")
        assertFalse(PageGeometry.pixelsWithinLimit(65_535, 65_535))
        assertFalse(PageGeometry.pixelsWithinLimit(Int.MAX_VALUE, Int.MAX_VALUE))
        assertTrue(PageGeometry.pixelsWithinLimit(3_000, 2_000))
    }
}
