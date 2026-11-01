package com.payslipmax.pdfparser.utils

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the index is read on paper, often a photocopy, by a clerk, so what the spec says must stay readable: dark ink on white that a copier
 * keeps, rules that show, lines tall enough for their type, and room for the page label. These pin properties, not the exact values.
 */
class IndexLayoutSpecTest {
    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun contrastOnWhite(argb: Long) = 1.05 / (luminance(argb) + 0.05)

    @Test
    fun theInksAreOpaqueAndReadableOnWhiteAtTheWcagBodyTextLevel() {
        for (ink in listOf(IndexLayoutSpec.INK, IndexLayoutSpec.QUIET_INK)) {
            assertEquals(0xFF, (ink shr 24) and 0xFF, "opaque")
            assertTrue(contrastOnWhite(ink) >= 7.0, "contrast ${contrastOnWhite(ink)}")
        }
        assertTrue(contrastOnWhite(IndexLayoutSpec.INK) >= contrastOnWhite(IndexLayoutSpec.QUIET_INK), "the main text is never lighter than the detail")
    }

    @Test
    fun theRuleShowsOnPaperButIsLighterThanTheText() {
        assertTrue(contrastOnWhite(IndexLayoutSpec.RULE) in 2.0..contrastOnWhite(IndexLayoutSpec.QUIET_INK))
        assertTrue(IndexLayoutSpec.RULE_WIDTH > 0.0 && IndexLayoutSpec.RULE_WIDTH <= 1.0)
    }

    @Test
    fun everyTypeStyleHasALineTallerThanItsTypeAndNothingIsTooSmallToRead() {
        for (font in IndexLayoutSpec.Font.entries) assertTrue(font.lineHeight > font.size, "${font.name}: a line must be taller than its type")
        assertEquals(PdfLayoutSpec.BODY_FONT_SIZE, IndexLayoutSpec.Font.CODE.size)
        assertEquals(PdfLayoutSpec.TITLE_FONT_SIZE, IndexLayoutSpec.Font.TITLE.size)
        assertTrue(IndexLayoutSpec.Font.entries.all { it.size >= 9 }, "nothing is set smaller than a clerk can read on a photocopy")
    }

    @Test
    fun theFooterHasRoomTheBodyEndsAboveItAndAYearIsSetApartMoreThanARow() {
        assertTrue(IndexLayoutSpec.FOOTER_RESERVE >= IndexLayoutSpec.Font.FOOTER.lineHeight)
        assertEquals(PdfLayoutSpec.contentBottom - IndexLayoutSpec.FOOTER_RESERVE, IndexLayoutSpec.bodyBottom)
        assertTrue(IndexLayoutSpec.YEAR_GAP > IndexLayoutSpec.ROW_GAP && IndexLayoutSpec.ROW_GAP > 0)
    }
}
