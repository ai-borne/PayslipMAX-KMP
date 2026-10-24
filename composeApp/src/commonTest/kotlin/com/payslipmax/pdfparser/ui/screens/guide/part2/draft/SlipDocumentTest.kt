package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_HILL
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the clerk keys the paper in from what the officer checked on the screen, so the paper and the preview are worded in one place and
 * must agree line for line. The paper is also the one thing the officer hands to someone else: it carries only the rows (no check, no
 * "seen in practice" label, no identity), the manual's certificate as the manual words it, and the fixed lines the app adds.
 */
class SlipDocumentTest {
    private val builder = LeaveDraftStateBuilder(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry())

    private fun day(
        m: Int,
        d: Int,
    ) = SlotValue.DateValue(CalendarDate(2026, m, d))

    private val values = mapOf("leave_from" to day(7, 5), "leave_to" to day(9, 4), "year" to SlotValue.NumberValue(2026), "away_from" to day(7, 1), "rejoin" to day(9, 5))

    private fun slip(request: OccurrenceRequest) = builder.build(LeaveEventInput(listOf(request)), emptySet(), false).slip!!

    private fun row(
        from: String = "",
        to: String = "",
        data: List<String> = listOf("", "", "", ""),
        authority: String? = null,
        certificate: String? = null,
    ) = SlipRowUi("CODE", "Narrative", from, to, data, authority, certificate, "")

    @Test
    fun aRowsDetailLinesAreItsFilledColumnsInColumnOrder() {
        val lines = row("05/07/2026", "04/09/2026", listOf("a", "", "c", ""), "Signal 12", "CERTIFICATE: ok").detailLines()

        assertEquals(
            listOf(Part2Strings.fromToLine("05/07/2026", "04/09/2026"), Part2Strings.dataLine(1, "a"), Part2Strings.dataLine(3, "c"), Part2Strings.authorityLine("Signal 12"), "CERTIFICATE: ok"),
            lines,
        )
    }

    @Test
    fun aColumnTheManualLeavesEmptyHasNoLineAndALoneDateIsStillOne() {
        assertTrue(row().detailLines().isEmpty())
        assertEquals(listOf(Part2Strings.fromToLine("05/07/2026", "")), row(from = "05/07/2026").detailLines())
        assertEquals(listOf(Part2Strings.fromToLine("", "09/07/2026")), row(to = "09/07/2026").detailLines())
    }

    @Test
    fun aCertificatePrintsAsTheManualWordsItWithNoLabelOfOurs() {
        assertEquals(listOf("CERTIFICATE: RATION MONEY ALLOWANCE 1. text"), row(certificate = "CERTIFICATE: RATION MONEY ALLOWANCE 1. text").detailLines())
    }

    @Test
    fun theDocumentIsTheSlipsOrdersAndRowsWithTheFixedLinesFromPart2Strings() {
        val slip = slip(OccurrenceRequest(GRANT, values, setOf(ANSWER_RATION)))

        val document = slip.toDocument()

        assertEquals(Part2Strings.slipDocumentTitle, document.title)
        assertEquals(Part2Strings.slipNotice, document.notice)
        assertEquals(Part2Strings.pageLabel, document.pageLabel)
        assertEquals(slip.lastOrder, document.lastOrder)
        assertEquals(slip.orders.map { it.title }, document.orders.map { it.title })
        assertEquals(slip.orders.flatMap { o -> o.rows.map { it.code } }, document.orders.flatMap { o -> o.rows.map { it.code } })
        assertEquals(slip.orders.flatMap { o -> o.rows.map { it.narrative } }, document.orders.flatMap { o -> o.rows.map { it.narrative } })
        assertEquals(slip.orders.flatMap { o -> o.rows.map { it.detailLines() } }, document.orders.flatMap { o -> o.rows.map { it.details } })
    }

    @Test
    fun theDocumentCarriesNoSourceLabelNoCheckAndNoTickText() {
        val slip = slip(OccurrenceRequest(GRANT, values, setOf(ANSWER_RATION, ANSWER_HILL)))
        val paper = slip.toDocument().let { d -> listOf(d.title, d.lastOrder, d.notice, d.pageLabel) + d.orders.flatMap { o -> listOf(o.title) + o.rows.flatMap { listOf(it.code, it.narrative) + it.details } } }

        val sources = slip.orders.flatMap { it.rows }.map { it.source }.filter { it.isNotEmpty() }
        assertTrue(sources.isNotEmpty(), "the slip must really have linked rows with a source")
        assertTrue(slip.findings.isNotEmpty(), "and a check")
        for (word in sources + slip.findings.flatMap { listOf(it.text, it.citation) } + Part2Strings.ackTick + Part2Strings.seenInPractice) {
            assertTrue(paper.none { word in it }, "'$word' is for the officer, not the clerk")
        }
    }

    @Test
    fun theDocumentNamesNoOneAndHoldsOnlyWhatTheOfficerAnsweredAndTheTemplateSays() {
        val slip = slip(OccurrenceRequest(EXTENSION, mapOf("ext_from" to day(9, 5), "new_to" to day(9, 20))))

        val text = slip.toDocument().let { d -> (listOf(d.title, d.lastOrder, d.notice) + d.orders.flatMap { o -> o.rows.flatMap { listOf(it.narrative) + it.details } }).joinToString("\n") }

        assertTrue("AR*****90G" !in text && "PAN" !in text, "the placeholder a payslip without a PAN yields must never reach the paper")
        assertTrue(Part2Strings.lastOrderBlank in text, "a blank last order stays blank")
    }

    @Test
    fun aSlipWithNoRowsOfASourceHasNoneInItsRowsEither() {
        val slip = slip(OccurrenceRequest(EXTENSION, mapOf("ext_from" to day(9, 5), "new_to" to day(9, 20))))

        assertNull(slip.orders.flatMap { it.rows }.firstOrNull { it.certificate != null })
        assertEquals(slip.orders.size, slip.toDocument().orders.size)
    }
}
