package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Phase 2: the iOS adapter must invert PDFKit's bottom-up Y into the common top-down convention so
 * common grid code sees one coordinate system on both platforms. This guards that pure conversion.
 *
 * Also covers [groupCharactersIntoWords], the pure Phase 2 word-grouping helper whose inputs model
 * PDFKit character data (bottom-up coordinates, synthetic zero-size glyphs).
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

    // --- groupCharactersIntoWords ---

    @Test
    fun `groupCharactersIntoWords returns empty list for empty input`() {
        assertEquals(0, groupCharactersIntoWords(emptyList(), pageHeight = 800.0).size)
    }

    @Test
    fun `groupCharactersIntoWords groups consecutive non-whitespace chars into a single token`() {
        // "Basic" in PDFKit bottom-up space: 5 chars each 8 px wide, 12 px tall, x starts at 100, y=200
        val chars =
            "Basic".mapIndexed { i, c ->
                CharBound(c.toString(), x = 100.0 + i * 8.0, y = 200.0, width = 8.0, height = 12.0)
            }
        val tokens = groupCharactersIntoWords(chars, pageHeight = 842.0)
        assertEquals(1, tokens.size)
        val t = tokens[0]
        assertEquals("Basic", t.text)
        assertEquals(100.0f, t.x)
        // topDownY(200.0, 12.0, 842.0) = 842.0 - (200.0 + 12.0) = 630.0
        assertEquals(630.0f, t.y)
        assertEquals(40.0f, t.width) // 5 chars × 8 px
        assertEquals(12.0f, t.height)
    }

    @Test
    fun `groupCharactersIntoWords splits tokens on whitespace`() {
        val chars =
            listOf(
                CharBound("A", x = 0.0, y = 10.0, width = 8.0, height = 12.0),
                CharBound(" ", x = 8.0, y = 10.0, width = 4.0, height = 12.0),
                CharBound("B", x = 12.0, y = 10.0, width = 8.0, height = 12.0),
            )
        val tokens = groupCharactersIntoWords(chars, pageHeight = 100.0)
        assertEquals(2, tokens.size)
        assertEquals("A", tokens[0].text)
        assertEquals("B", tokens[1].text)
    }

    @Test
    fun `groupCharactersIntoWords preserves hyphenated ARR-hyphen as single token`() {
        // "ARR-" must not be split; all four chars are non-whitespace.
        val chars =
            listOf(
                CharBound("A", x = 0.0, y = 10.0, width = 8.0, height = 12.0),
                CharBound("R", x = 8.0, y = 10.0, width = 8.0, height = 12.0),
                CharBound("R", x = 16.0, y = 10.0, width = 8.0, height = 12.0),
                CharBound("-", x = 24.0, y = 10.0, width = 6.0, height = 12.0),
            )
        val tokens = groupCharactersIntoWords(chars, pageHeight = 100.0)
        assertEquals(1, tokens.size)
        assertEquals("ARR-", tokens[0].text)
        assertEquals(30.0f, tokens[0].width) // 3 × 8 + 6
    }

    @Test
    fun `groupCharactersIntoWords skips zero-size synthetic glyphs`() {
        // PDFKit inserts zero-size synthetic characters (e.g., ligature components, word-boundary
        // markers). They must be ignored — neither breaking a word nor contributing to its bounds.
        val chars =
            listOf(
                CharBound("X", x = 0.0, y = 10.0, width = 8.0, height = 12.0),
                // soft hyphen, zero-size — skipped by the zero-bounds guard
                CharBound("­", x = 8.0, y = 10.0, width = 0.0, height = 0.0),
                CharBound(" ", x = 8.0, y = 10.0, width = 4.0, height = 12.0),
                CharBound("Y", x = 12.0, y = 10.0, width = 8.0, height = 12.0),
            )
        val tokens = groupCharactersIntoWords(chars, pageHeight = 100.0)
        assertEquals(2, tokens.size)
        assertEquals("X", tokens[0].text)
        assertEquals("Y", tokens[1].text)
    }

    @Test
    fun `groupCharactersIntoWords unions bounds across chars in a multi-height word`() {
        // Two chars with different heights: the word's bounding box should span both.
        // A: bottom-up y=10, h=10 → top=20; B: bottom-up y=8, h=14 → top=22
        val chars =
            listOf(
                CharBound("A", x = 0.0, y = 10.0, width = 8.0, height = 10.0),
                CharBound("B", x = 8.0, y = 8.0, width = 8.0, height = 14.0),
            )
        val tokens = groupCharactersIntoWords(chars, pageHeight = 100.0)
        assertEquals(1, tokens.size)
        val t = tokens[0]
        assertEquals("AB", t.text)
        assertEquals(0.0f, t.x)
        // wordMinX=0, wordMaxX=16 → width=16; wordMinY=8, wordMaxY=22 → height=14
        assertEquals(16.0f, t.width)
        assertEquals(14.0f, t.height)
        // topDownY(minY=8.0, h=14.0, pageHeight=100.0) = 100 - (8 + 14) = 78
        assertEquals(78.0f, t.y)
    }
}
