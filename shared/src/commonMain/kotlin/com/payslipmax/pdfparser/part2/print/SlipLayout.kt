package com.payslipmax.pdfparser.part2.print

import com.payslipmax.pdfparser.utils.PdfLayoutSpec
import com.payslipmax.pdfparser.utils.PdfLetterFormatter
import com.payslipmax.pdfparser.utils.SlipLayoutSpec
import com.payslipmax.pdfparser.utils.SlipLayoutSpec.Font
import kotlin.math.ceil

/** Width of text in a type style, in points. The page is the same on both platforms because only this comes from the platform's fonts. */
fun interface SlipTextMeasurer {
    fun width(
        text: String,
        font: Font,
    ): Double
}

/** One line of text on a page: [x] and [top] are the top left of its line box, in points from the top left of the page. */
data class SlipText(val x: Int, val top: Int, val text: String, val font: Font) {
    override fun toString(): String = "SlipText(${font.name})"
}

/** A horizontal hairline from [x1] to [x2] at [y]. */
data class SlipRule(val x1: Int, val x2: Int, val y: Int)

data class SlipPage(val texts: List<SlipText>, val rules: List<SlipRule>)

/**
 * Lays a [SlipDocument] out on A4 pages: pure, so the page model is unit-tested and each platform only draws it. A row is kept whole on
 * one page when it fits on one, an order's heading is kept with its first row, a block taller than a page flows line by line, and a
 * word wider than the page is broken so nothing runs off the paper. Every page carries its label at the foot.
 */
object SlipLayout {
    fun paginate(
        document: SlipDocument,
        measurer: SlipTextMeasurer,
    ): List<SlipPage> {
        val flow = PageFlow(measurer)
        flow.block(flow.lines(document.title, Font.TITLE) + flow.lines(document.lastOrder, Font.BODY) + flow.lines(document.notice, Font.DETAIL), 0)
        for (order in document.orders) flow.order(order)
        return flow.finish(document.pageLabel)
    }
}

private class Line(val text: String, val font: Font)

private class PageFlow(private val measurer: SlipTextMeasurer) {
    private val pages = mutableListOf<SlipPage>()
    private var texts = mutableListOf<SlipText>()
    private var rules = mutableListOf<SlipRule>()
    private var top = PdfLayoutSpec.MARGIN
    private var atPageTop = true

    /** The height of an empty page's body. */
    private val room = SlipLayoutSpec.bodyBottom - PdfLayoutSpec.MARGIN

    fun lines(
        text: String,
        font: Font,
    ): List<Line> =
        text.split('\n')
            .flatMap { PdfLetterFormatter.wrapLine(it, PdfLayoutSpec.contentWidth.toDouble(), breakLongWords = true) { piece -> measurer.width(piece, font) } }
            .filter { it.isNotEmpty() }
            .map { Line(it, font) }

    /** An order: its heading, a hairline, then each row; the heading never ends a page without its first row (or the first lines of it). */
    fun order(order: SlipDocumentOrder) {
        val heading = lines(order.title, Font.HEADING)
        val rows = order.rows.map { row -> lines(row.code, Font.CODE) + lines(row.narrative, Font.BODY) + row.details.flatMap { lines(it, Font.DETAIL) } }
        val headingHeight = height(heading)
        val first = rows.firstOrNull()?.let { height(it) } ?: 0
        reserve(SlipLayoutSpec.ORDER_GAP, headingHeight + if (first > 0) SlipLayoutSpec.ROW_GAP + first else 0)
        block(heading, SlipLayoutSpec.ORDER_GAP)
        rule(top)
        rows.forEachIndexed { i, row -> block(row, SlipLayoutSpec.ROW_GAP, ruleInGap = i > 0) }
    }

    /** Draws [block] after [gapBefore], on a new page when it does not fit here but would on a fresh one; a block taller than a page is drawn line by line. */
    fun block(
        block: List<Line>,
        gapBefore: Int,
        ruleInGap: Boolean = false,
    ) {
        if (block.isEmpty()) return
        reserve(gapBefore, if (height(block) > room) block.first().font.lineHeight else height(block))
        if (!atPageTop) {
            if (ruleInGap) rule(top + gapBefore / 2)
            top += gapBefore
        }
        for (line in block) {
            if (top + line.font.lineHeight > SlipLayoutSpec.bodyBottom) newPage()
            texts += SlipText(PdfLayoutSpec.MARGIN, top, line.text, line.font)
            top += line.font.lineHeight
            atPageTop = false
        }
    }

    fun finish(pageLabel: String): List<SlipPage> {
        newPage()
        val right = PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN
        return pages.mapIndexed { i, page ->
            val label = pageLabel.replace("{page}", (i + 1).toString()).replace("{pages}", pages.size.toString())
            val footer = SlipText(right - ceil(measurer.width(label, Font.FOOTER)).toInt(), PdfLayoutSpec.contentBottom - Font.FOOTER.lineHeight, label, Font.FOOTER)
            SlipPage(page.texts + footer, page.rules)
        }
    }

    private fun height(lines: List<Line>) = lines.sumOf { it.font.lineHeight }

    /** Starts a new page when [needed] points after [gapBefore] do not fit here (a block is never asked for more than a page). */
    private fun reserve(
        gapBefore: Int,
        needed: Int,
    ) {
        if (!atPageTop && top + gapBefore + needed > SlipLayoutSpec.bodyBottom) newPage()
    }

    private fun rule(y: Int) {
        rules += SlipRule(PdfLayoutSpec.MARGIN, PdfLayoutSpec.PAGE_WIDTH - PdfLayoutSpec.MARGIN, y)
    }

    private fun newPage() {
        pages += SlipPage(texts, rules)
        texts = mutableListOf()
        rules = mutableListOf()
        top = PdfLayoutSpec.MARGIN
        atPageTop = true
    }
}
