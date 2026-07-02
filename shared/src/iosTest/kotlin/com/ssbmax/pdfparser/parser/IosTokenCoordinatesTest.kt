package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The iOS adapter must invert PDFKit's bottom-up Y into the common top-down convention so common
 * grid code sees one coordinate system on both platforms. This guards that pure conversion.
 *
 * Also covers [findWordRanges], the pure word-boundary helper [extractPageTokens] uses to split
 * [platform.PDFKit.PDFPage.string] before resolving each word's bounds via PDFKit.
 */
class IosTokenCoordinatesTest {
    @Test
    fun `bottom-up box near page top maps to a small top-down y`() {
        // pageHeight 800; a box whose bottom edge is at 770 and height 20 sits at the very top.
        // Its top edge from the top of the page = 800 - (770 + 20) = 10.
        assertEquals(10.0, IosTokenCoordinates.topDownY(minYBottomUp = 770.0, height = 20.0, pageHeight = 800.0), 1e-9)
    }

    @Test
    fun `bottom-up box near page bottom maps to a large top-down y`() {
        // A box at the bottom (bottom edge y=0, height 20) → top-down top edge = 800 - 20 = 780.
        assertEquals(780.0, IosTokenCoordinates.topDownY(minYBottomUp = 0.0, height = 20.0, pageHeight = 800.0), 1e-9)
    }

    @Test
    fun `conversion is the exact inverse of itself across the page height`() {
        val pageHeight = 842.0
        val height = 12.0
        val bottomUp = 400.0
        val topDown = IosTokenCoordinates.topDownY(bottomUp, height, pageHeight)
        // Re-applying the same formula round-trips back to the original bottom-up coordinate.
        assertEquals(bottomUp, IosTokenCoordinates.topDownY(topDown, height, pageHeight), 1e-9)
    }

    // --- findWordRanges ---

    @Test
    fun `findWordRanges returns empty list for empty input`() {
        assertEquals(0, findWordRanges("").size)
    }

    @Test
    fun `findWordRanges returns empty list for whitespace-only input`() {
        assertEquals(0, findWordRanges("   \n\t ").size)
    }

    @Test
    fun `findWordRanges returns a single range for one word`() {
        val ranges = findWordRanges("Basic")
        assertEquals(listOf(0..4), ranges)
    }

    @Test
    fun `findWordRanges splits on whitespace`() {
        val ranges = findWordRanges("A B")
        assertEquals(listOf(0..0, 2..2), ranges)
    }

    @Test
    fun `findWordRanges preserves hyphenated ARR-hyphen as a single range`() {
        // "ARR-" must not be split; the hyphen is non-whitespace.
        val ranges = findWordRanges("ARR- 100")
        assertEquals(listOf(0..3, 5..7), ranges)
    }

    @Test
    fun `findWordRanges collapses consecutive whitespace and ignores leading-trailing whitespace`() {
        val ranges = findWordRanges("  Basic   Pay  ")
        assertEquals(listOf(2..6, 10..12), ranges)
    }
}
