package com.payslipmax.pdfparser.part2.print

import com.payslipmax.pdfparser.utils.PdfLayoutSpec
import com.payslipmax.pdfparser.utils.SlipLayoutSpec
import com.payslipmax.pdfparser.utils.SlipLayoutSpec.Font
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the clerk keys the slip into HRMS from paper, field by field, so a line cut off at the margin, a row split from its code or an
 * order heading stranded at the foot of a page is a wrong order. The layout is pure (the platform only draws it), so its rules are
 * pinned here: A4 from `PdfLayoutSpec`, type and colours from `SlipLayoutSpec`, wrapping, page breaks and the page label.
 */
class SlipLayoutTest {
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
    fun aSmallSlipIsOnePageWithTheTitleFirstThenTheLastOrderThenTheNotice() {
        val pages = pages(document(order(1, listOf(row(1)))))

        assertEquals(1, pages.size)
        val texts = pages.single().texts
        assertEquals(listOf("Slip title", "Last Part II order: ____", "Draft notice"), texts.take(3).map { it.text })
        assertEquals(listOf(Font.TITLE, Font.BODY, Font.DETAIL), texts.take(3).map { it.font })
        assertEquals(PdfLayoutSpec.MARGIN, texts.first().top, "the slip starts at the top margin")
    }

    @Test
    fun anOrderIsItsHeadingThenEachRowAsCodeNarrativeAndDetailLines() {
        val pages = pages(document(order(1, listOf(row(1), row(2)))))

        val texts = pages.single().texts.drop(3).filter { it.font != Font.FOOTER }
        assertEquals(
            listOf("Order 1 · Group A", "CODE1", "Narrative R1", "Detail D1.0", "Detail D1.1", "CODE2", "Narrative R2", "Detail D2.0", "Detail D2.1"),
            texts.map { it.text },
        )
        assertEquals(listOf(Font.HEADING, Font.CODE, Font.BODY, Font.DETAIL, Font.DETAIL, Font.CODE, Font.BODY, Font.DETAIL, Font.DETAIL), texts.map { it.font })
    }

    @Test
    fun everyLineStaysInsideTheMarginsOfTheA4Page() {
        val rows = List(30) { row(it, details = 3, narrative = "word ".repeat(70).trim()) }
        val pages = pages(document(order(1, rows), order(2, rows)))

        assertTrue(pages.size > 2)
        for (text in pages.flatMap { it.texts }) {
            assertTrue(text.x >= PdfLayoutSpec.MARGIN, "left margin")
            assertTrue(text.x + text.width() <= PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN + 0.001, "right margin: ${text.font}")
            assertTrue(text.top >= PdfLayoutSpec.MARGIN, "top margin")
            val bottom = if (text.font == Font.FOOTER) PdfLayoutSpec.contentBottom else SlipLayoutSpec.bodyBottom
            assertTrue(text.top + text.font.lineHeight <= bottom, "bottom margin: ${text.font}")
        }
    }

    @Test
    fun aLongNarrativeWrapsAtTheContentWidthAndKeepsEveryWord() {
        val narrative = (1..80).joinToString(" ") { "w$it" }
        val lines = pages(document(order(1, listOf(row(1, narrative = narrative))))).single().texts.filter { it.font == Font.BODY && it.text.startsWith("w") }

        assertTrue(lines.size > 1)
        assertTrue(lines.all { it.width() <= PdfLayoutSpec.contentWidth })
        assertEquals(narrative, lines.joinToString(" ") { it.text })
    }

    @Test
    fun aWordWiderThanTheLineIsBrokenSoNothingRunsOffThePage() {
        val word = "x".repeat(300)
        val lines = pages(document(order(1, listOf(row(1, narrative = word))))).single().texts.filter { it.text.startsWith("x") }

        assertTrue(lines.size >= 4)
        assertTrue(lines.all { it.width() <= PdfLayoutSpec.contentWidth })
        assertEquals(word, lines.joinToString("") { it.text }, "every character is still printed")
    }

    @Test
    fun aNewlineInATextStartsANewLine() {
        val lines = pages(document(order(1, listOf(row(1, narrative = "first line\nsecond line"))))).single().texts

        assertTrue(lines.any { it.text == "first line" } && lines.any { it.text == "second line" })
    }

    @Test
    fun aDetailIsOneLineEachAndNothingIsDrawnForNothing() {
        val pages = pages(document(order(1, listOf(row(1, details = 0)))))

        assertTrue(pages.flatMap { it.texts }.none { it.text.isEmpty() })
        assertEquals(listOf("CODE1", "Narrative R1"), pages.single().texts.filter { it.font != Font.FOOTER }.drop(4).map { it.text })
    }

    @Test
    fun aHairlineSitsUnderAnOrderHeadingAndBetweenItsRows() {
        val page = pages(document(order(1, listOf(row(1), row(2), row(3))))).single()

        assertEquals(3, page.rules.size)
        assertTrue(page.rules.all { it.x1 == PdfLayoutSpec.MARGIN && it.x2 == PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN })
        val heading = page.texts.single { it.font == Font.HEADING }
        assertEquals(heading.top + Font.HEADING.lineHeight, page.rules.first().y, "the first rule is directly under the heading")
        for (n in 2..3) {
            val code = page.texts.single { it.text == "CODE$n" }
            assertEquals(code.top - SlipLayoutSpec.ROW_GAP / 2, page.rules[n - 1].y, "the rule before row $n sits in the middle of the gap above it")
        }
    }

    @Test
    fun rowsAreSeparatedByTheRowGapAndOrdersByTheOrderGap() {
        val page = pages(document(order(1, listOf(row(1), row(2))), order(2, listOf(row(3))))).single()

        fun top(text: String) = page.texts.single { it.text == text }.top

        val endOfRowOne = top("Detail D1.1") + Font.DETAIL.lineHeight
        assertEquals(SlipLayoutSpec.ROW_GAP, top("CODE2") - endOfRowOne)
        val endOfRowTwo = top("Detail D2.1") + Font.DETAIL.lineHeight
        assertEquals(SlipLayoutSpec.ORDER_GAP, top("Order 2 · Group A") - endOfRowTwo)
    }

    @Test
    fun aSlipWithNoOrdersIsOnePageWithTheHeaderAndItsLabel() {
        val pages = pages(document())

        assertEquals(1, pages.size)
        assertEquals(listOf("Slip title", "Last Part II order: ____", "Draft notice", "Page 1 of 1"), pages.single().texts.map { it.text })
    }
}
