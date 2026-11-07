package com.payslipmax.pdfparser.part2.print

import com.payslipmax.pdfparser.part2.print.IndexFixtures.document
import com.payslipmax.pdfparser.part2.print.IndexFixtures.measurer
import com.payslipmax.pdfparser.part2.print.IndexFixtures.year
import com.payslipmax.pdfparser.utils.IndexLayoutSpec
import com.payslipmax.pdfparser.utils.IndexLayoutSpec.Font
import com.payslipmax.pdfparser.utils.PdfLayoutSpec
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the officer carries this paper to a clerk, so every casualty must be on it once, in the order the Bank shows, never cut in half
 * by a page break, never under a year heading left alone at the foot of a page, and never off the edge of the paper. An empty Bank has
 * nothing to print. Layout is pure, so each rule is proven here and the platforms only draw.
 */
class IndexLayoutTest {
    private fun pages(vararg years: IndexYear) = IndexLayout.paginate(IndexFixtures.document(years.toList()), measurer)

    private fun body(page: IndexPage) = page.texts.filter { it.font != Font.FOOTER }

    @Test
    fun anEmptyBankPrintsNothing() {
        assertEquals(emptyList(), IndexLayout.paginate(IndexFixtures.document(emptyList()), measurer), "no page, not even a title page")
    }

    @Test
    fun theFirstPageOpensWithTheTitleThePrintedOnLineAndTheNoticeInThatOrder() {
        val first = body(pages(year("2026", 2)).first()).take(3)

        assertEquals(listOf("Index title", "Printed on 1 Nov 2026", "Reference copy"), first.map { it.text })
        assertEquals(Font.TITLE, first[0].font)
        assertEquals(PdfLayoutSpec.MARGIN, first[0].top)
    }

    @Test
    fun theTitleIsOnlyOnTheFirstPage() {
        val all = IndexFixtures.pages

        assertTrue(all.size >= 3, "the fixture must be several pages: ${all.size}")
        assertEquals(1, all.sumOf { page -> page.texts.count { it.text == "Index title" } })
    }

    @Test
    fun everyPageEndsWithItsNumberOfTheTotalAtTheRightMargin() {
        val all = IndexFixtures.pages

        all.forEachIndexed { i, page ->
            val footer = page.texts.last()
            assertEquals("Page ${i + 1} of ${all.size}", footer.text)
            assertEquals(Font.FOOTER, footer.font)
            assertEquals(PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN, footer.x + ceil(measurer.width(footer.text, Font.FOOTER)).toInt())
        }
    }

    @Test
    fun aFullPageHoldsFifteenRowsOfTwoDetailLines() {
        // Room 724 pt (page less margins and the footer). A row is 15 + 13 + 13 = 41 pt and 6 pt between rows: 15 rows = 699 pt, 16 = 746 pt.
        val middle = pages(year("2026", 40))[1]

        assertEquals(15, body(middle).count { it.font == Font.CODE })
    }

    @Test
    fun nothingIsDrawnBelowTheBodyAndNothingPastTheRightMargin() {
        assertTrue(IndexFixtures.pages.size >= 3)
        for (page in IndexFixtures.pages) for (text in body(page)) {
            assertTrue(text.top + text.font.lineHeight <= IndexLayoutSpec.bodyBottom, "line at ${text.top}")
            assertTrue(text.x + measurer.width(text.text, text.font) <= PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN)
        }
    }

    @Test
    fun aRowIsNeverSplitAcrossPages() {
        assertTrue(IndexFixtures.pages.size >= 3)
        for (page in IndexFixtures.pages) {
            val fonts = body(page).map { it.font }.dropWhile { it != Font.CODE && it != Font.HEADING }
            fonts.forEachIndexed { i, font -> if (font == Font.CODE) assertEquals(listOf(Font.DETAIL, Font.DETAIL), fonts.drop(i + 1).take(2), "a headline keeps its two details") }
        }
    }

    @Test
    fun aYearHeadingIsNeverTheLastThingOnAPage() {
        // Every alignment of the first year's length against the page, so a heading lands at the foot somewhere.
        for (rows in 1..40) for (page in pages(year("2026", rows), year("2025", 3, 100)).also { assertTrue(it.isNotEmpty()) }) {
            assertFalse(body(page).last().font == Font.HEADING, "a heading ended a page with $rows rows before it")
        }
    }

    @Test
    fun yearsAndRowsComeOutInTheOrderGiven() {
        val order = IndexFixtures.pages.flatMap { body(it) }.filter { it.font == Font.HEADING || it.font == Font.CODE }.map { it.text }

        val expected = document.years.flatMap { listOf(it.heading) + it.rows.map { row -> row.headline } }
        assertEquals(expected, order)
    }

    @Test
    fun aWordWiderThanThePageIsBrokenSoItStaysOnThePaper() {
        val wide = IndexRow("X".repeat(300), listOf("y".repeat(300)))

        for (page in pages(IndexYear("2026", listOf(wide)))) for (text in body(page)) {
            assertTrue(text.x + measurer.width(text.text, text.font) <= PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN, "${text.font} line runs off the page")
        }
    }

    @Test
    fun thePagesCarryExactlyTheWordsOfTheDocumentAndNothingElse() {
        val onPaper = IndexFixtures.pages.flatMap { body(it) }.joinToString(" ") { it.text }.split(' ').filter { it.isNotEmpty() }.sorted()

        val given =
            (listOf(document.title, document.printedOn, document.notice) + document.years.flatMap { listOf(it.heading) + it.rows.flatMap { r -> listOf(r.headline) + r.details } })
                .joinToString(" ").split(' ').filter { it.isNotEmpty() }.sorted()
        assertEquals(given, onPaper)
    }

    @Test
    fun everyYearHeadingHasAHairlineUnderItAcrossTheContentWidth() {
        val all = pages(year("2026", 3), year("2025", 3, 10)).single()

        val headings = body(all).filter { it.font == Font.HEADING }
        assertEquals(2, headings.size)
        for (heading in headings) {
            val rule = all.rules.single { it.y == heading.top + heading.font.lineHeight }
            assertEquals(PdfLayoutSpec.MARGIN, rule.x1)
            assertEquals(PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN, rule.x2)
        }
    }
}
