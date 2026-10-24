package com.payslipmax.pdfparser.part2.print

import com.payslipmax.pdfparser.utils.PdfLayoutSpec
import com.payslipmax.pdfparser.utils.SlipLayoutSpec
import com.payslipmax.pdfparser.utils.SlipLayoutSpec.Font
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * WHY: the clerk keys the paper in page by page, so a row cut in two, an order heading stranded at the foot, a blank page, a missing page or
 * a page label overlapping the text is a wrong order. These pin the page breaks of `SlipLayout` (the content of each page is `SlipLayoutTest`).
 */
class SlipLayoutPagingTest {
    // Half the point size per character: deterministic, so a line of BODY text holds 90 characters.
    private val measurer = SlipTextMeasurer { text, font -> text.length * font.size * 0.5 }

    private fun row(
        n: Int,
        details: Int = 2,
        narrative: String = "Narrative R$n",
    ) = SlipDocumentRow("CODE$n", narrative, List(details) { "Detail D$n.$it" })

    private fun document(
        vararg orders: SlipDocumentOrder,
        lastOrder: String = "Last Part II order: ____",
    ) = SlipDocument("Slip title", lastOrder, "Draft notice", orders.toList(), "Page {page} of {pages}")

    private fun order(
        n: Int,
        rows: List<SlipDocumentRow>,
    ) = SlipDocumentOrder("Order $n · Group A", rows)

    private fun pages(document: SlipDocument) = SlipLayout.paginate(document, measurer)

    private fun List<SlipPage>.body() = map { page -> page.texts.filter { it.font != Font.FOOTER } }

    private fun SlipText.width() = measurer.width(text, font)

    private fun pageOf(
        pages: List<SlipPage>,
        needle: String,
    ) = pages.indices.filter { i -> pages[i].texts.any { it.text == needle } }

    @Test
    fun aRowBlockIsNeverSplitAcrossPagesWhenItFitsOnOne() {
        val rows = List(40) { row(it + 1, details = 4) }
        val pages = pages(document(order(1, rows)))

        assertTrue(pages.size > 1)
        for (n in 1..40) {
            val code = pageOf(pages, "CODE$n").single()
            assertEquals(listOf(code), pageOf(pages, "Narrative R$n"))
            assertEquals(listOf(code), pageOf(pages, "Detail D$n.3"))
        }
    }

    @Test
    fun anOrderHeadingIsNeverLeftAloneAtTheFootOfAPage() {
        var broken = 0
        for (detailLines in 0..14) {
            val orders = List(12) { n -> order(n + 1, listOf(row(n + 1, details = detailLines))) }
            val pages = pages(document(*orders.toTypedArray()))
            if (pages.size > 1) broken++
            assertEquals(12, pages.flatMap { it.texts }.count { it.font == Font.HEADING }, "every heading is drawn")
            for (page in pages.body()) {
                assertNotEquals(Font.HEADING, page.last().font, "a heading ends a page with $detailLines detail lines")
            }
        }
        assertTrue(broken >= 10, "the loop must really cross page breaks")
    }

    @Test
    fun aBlockTallerThanAPageIsSplitByLineAndLosesNothing() {
        val tall = SlipDocumentRow("TALLCODE", "Narrative", List(120) { "line $it" })
        val pages = pages(document(order(1, listOf(tall))))

        assertTrue(pages.size >= 2)
        val lines = pages.flatMap { it.texts }.filter { it.text.startsWith("line ") }.map { it.text }
        assertEquals(List(120) { "line $it" }, lines)
    }

    @Test
    fun everyPageCarriesItsLabelRightAlignedInTheFooter() {
        val pages = pages(document(order(1, List(40) { row(it + 1, details = 4) })))

        assertTrue(pages.size > 1)
        pages.forEachIndexed { i, page ->
            val footer = page.texts.single { it.font == Font.FOOTER }
            assertEquals("Page ${i + 1} of ${pages.size}", footer.text)
            assertEquals(PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN, footer.x + ceil(footer.width()).toInt())
            assertEquals(PdfLayoutSpec.contentBottom - Font.FOOTER.lineHeight, footer.top)
        }
    }

    @Test
    fun theSameSlipAlwaysLaysOutTheSame() {
        val doc = document(order(1, List(25) { row(it + 1, details = 3) }))

        assertTrue(pages(doc).size > 1)
        assertEquals(pages(doc), pages(doc))
    }

    @Test
    fun aPageStartsWithNoGapAndNoRuleAboveItsFirstBlock() {
        val pages = pages(document(order(1, List(40) { row(it + 1, details = 4) })))

        val second = pages[1]
        assertEquals(PdfLayoutSpec.MARGIN, second.texts.first().top)
        assertTrue(second.rules.none { it.y <= second.texts.first().top })
    }

    @Test
    fun aBlockThatEndsExactlyAtTheFootOfThePageStaysOnIt() {
        // Page one: title 20 + last order 15 + notice 14, a 14 gap, a 18 heading, an 8 gap, then a row of exactly the room left (635).
        val row = SlipDocumentRow("EXACT", List(31) { "n$it" }.joinToString("\n"), List(11) { "d$it" })

        val pages = pages(document(order(1, listOf(row))))

        assertEquals(1, pages.size, "a row that fills the page to the last point still fits")
        assertEquals(SlipLayoutSpec.bodyBottom, pages.single().texts.filter { it.font != Font.FOOTER }.maxOf { it.top + it.font.lineHeight })
    }

    @Test
    fun aBlockOnePointTooTallMovesToTheNextPage() {
        val row = SlipDocumentRow("OVER", List(31) { "n$it" }.joinToString("\n"), List(11) { "d$it" } + "one more")

        assertEquals(listOf(1), pageOf(pages(document(order(1, listOf(row)))), "OVER"), "the row moves whole, with its heading, to page two")
    }

    @Test
    fun noBodyLineReachesIntoTheFooter() {
        val pages = pages(document(order(1, List(40) { row(it + 1, details = 4) })))

        for (page in pages) {
            val footer = page.texts.single { it.font == Font.FOOTER }
            assertTrue(page.texts.filter { it.font != Font.FOOTER }.all { it.top + it.font.lineHeight <= footer.top }, "body text ends above the page label")
        }
    }

    @Test
    fun aSlipWithNothingToDrawIsOneBlankPageWithItsLabelAndNeverNoPage() {
        val pages = pages(SlipDocument("", "", "", emptyList(), "Page {page} of {pages}"))

        assertEquals(1, pages.size, "a PDF with no page cannot be written")
        assertEquals(listOf("Page 1 of 1"), pages.single().texts.map { it.text })
    }

    @Test
    fun aRowAsTallAsAWholePageStartsAFreshPageWithoutLeavingABlankOneBeforeIt() {
        // 16 + 22 * 15 + 27 * 14 = 724, exactly the body of a page.
        val full = SlipDocumentRow("FULL", List(22) { "n$it" }.joinToString("\n"), List(27) { "d$it" })

        val pages = pages(document(order(1, listOf(row(1), full))))

        assertEquals(2, pages.size)
        assertTrue(pages.body().all { it.isNotEmpty() })
        assertEquals(listOf(1), pageOf(pages, "FULL"))
        assertEquals(PdfLayoutSpec.MARGIN, pages[1].texts.first().top, "and it starts at the top margin, with no gap above it")
    }

    @Test
    fun aSlipNeverStartsWithABlankPageEvenWhenItsHeaderIsEmpty() {
        val full = SlipDocumentRow("FULL", List(22) { "n$it" }.joinToString("\n"), List(27) { "d$it" })

        val pages = pages(SlipDocument("", "", "", listOf(order(1, listOf(full))), "Page {page} of {pages}"))

        assertTrue(pages.body().all { it.isNotEmpty() }, "no page is blank")
        assertEquals(PdfLayoutSpec.MARGIN, pages.first().texts.first().top)
    }

    @Test
    fun aBlockTallerThanAPageStartsRightAfterTheRowBeforeItInsteadOfLeavingAGap() {
        val tall = SlipDocumentRow("TALLCODE", "Narrative", List(120) { "line $it" })

        val pages = pages(document(order(1, listOf(row(1), tall))))

        assertEquals(pageOf(pages, "Detail D1.1"), pageOf(pages, "TALLCODE"), "it begins on the page where the previous row ended")
    }
}
