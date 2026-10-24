package com.payslipmax.pdfparser.part2.pages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: a stored page must fit the 500 KB cap, but the first quality that fits is the best one the officer can keep: quality is stepped
 * down from the start, one try per step, never below the floor, and a page that still does not fit there is refused rather than turned
 * into unreadable text. The codec is a function here, so the rule is proved once for both platforms.
 */
class JpegFitTest {
    private val tried = mutableListOf<Int>()

    private fun sizeAt(
        sizes: Map<Int, Int>,
    ): (Int) -> ByteArray =
        { quality ->
            tried += quality
            ByteArray(sizes.getValue(quality)) { quality.toByte() }
        }

    @Test
    fun theFirstQualityThatFitsIsKeptAndNothingLowerIsTried() {
        val kept = JpegFit.fit(1000, 85, 10, 40, sizeAt(mapOf(85 to 1500, 75 to 1200, 65 to 900, 55 to 500)))

        assertEquals(listOf(85, 75, 65), tried)
        assertContentEquals(ByteArray(900) { 65 }, kept)
    }

    @Test
    fun aPageThatFitsAtTheStartIsEncodedOnce() {
        JpegFit.fit(1000, 85, 10, 40, sizeAt(mapOf(85 to 1000)))

        assertEquals(listOf(85), tried)
    }

    @Test
    fun exactlyTheCapFitsAndOneByteOverDoesNot() {
        assertEquals(1000, JpegFit.fit(1000, 85, 10, 40, sizeAt(mapOf(85 to 1000)))?.size)
        tried.clear()
        assertEquals(999, JpegFit.fit(1000, 85, 10, 40, sizeAt(mapOf(85 to 1001, 75 to 999)))?.size)
    }

    @Test
    fun theFloorIsTriedEvenWhenTheStepsSkipOverIt() {
        val sizes = (listOf(85, 75, 65, 55, 45, 40)).associateWith { 2000 }

        assertNull(JpegFit.fit(1000, 85, 10, 40, sizeAt(sizes)))
        assertEquals(listOf(85, 75, 65, 55, 45, 40), tried, "45 - 10 would be 35, below the floor, so the floor itself is the last try")
    }

    @Test
    fun aStepThatLandsOnTheFloorTriesItOnce() {
        assertNull(JpegFit.fit(1000, 70, 10, 40, sizeAt(mapOf(70 to 2000, 60 to 2000, 50 to 2000, 40 to 2000))))
        assertEquals(listOf(70, 60, 50, 40), tried)
    }

    @Test
    fun aPageThatFitsOnlyAtTheFloorIsKept() {
        assertEquals(500, JpegFit.fit(1000, 60, 10, 40, sizeAt(mapOf(60 to 2000, 50 to 2000, 40 to 500)))?.size)
    }

    @Test
    fun aStartBelowTheFloorTriesTheFloorOnly() {
        JpegFit.fit(1000, 30, 5, 40, sizeAt(mapOf(40 to 500)))

        assertEquals(listOf(40), tried)
    }

    @Test
    fun aCodecThatProducesNothingIsNotAFit() {
        assertNull(
            JpegFit.fit(1000, 85, 10, 40) {
                tried += it
                ByteArray(0)
            },
        )
        assertEquals(listOf(85, 75, 65, 55, 45, 40), tried, "an empty result is a failure at that quality, not a page that fits")
    }

    @Test
    fun aCodecThatRefusesIsNotAFitEitherAndTheNextQualityIsTried() {
        val kept =
            JpegFit.fit(1000, 85, 10, 40) { quality ->
                tried += quality
                if (quality == 85) null else ByteArray(10) { 1 }
            }

        assertEquals(listOf(85, 75), tried)
        assertEquals(10, kept?.size)
    }
}
