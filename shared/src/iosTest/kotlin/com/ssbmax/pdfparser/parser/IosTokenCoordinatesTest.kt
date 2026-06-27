package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Phase 2: the iOS adapter must invert PDFKit's bottom-up Y into the common top-down convention so
 * common grid code sees one coordinate system on both platforms. This guards that pure conversion.
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
}
