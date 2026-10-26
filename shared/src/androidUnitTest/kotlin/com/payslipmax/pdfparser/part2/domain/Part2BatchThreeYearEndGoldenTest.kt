package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.on
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.order
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.request
import com.payslipmax.pdfparser.part2.domain.Ch5Answers.rowOf
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden slips for the year-end accumulation, the three LTC codes and the final encashment (Part II plan, phase 8; ODP-2026 pp.133-141),
 * on the real shipped templates and synthetic figures only. They pin what the owner reviews against the printed pages, including the
 * manual's own worked examples (p.133 and p.140).
 */
class Part2BatchThreeYearEndGoldenTest {
    private fun acclve(
        year: Int,
        previous: Int,
        ltc: Int,
        accrued: Int,
        vararg more: Pair<String, SlotValue>,
    ) = request("ACCLVE", mapOf("year" to number(year), "as_on" to date(12, 31, year), "prev_total" to number(previous), "ltc_days" to number(ltc), "accrued" to number(accrued)) + more)

    @Test
    fun theManualsWorkedExampleOfTheAccumulationPrintsAsOnPage133() {
        val row = rowOf("ACCLVE", acclve(2014, 380, 10, 0))

        assertEquals(
            "(a) Total accumulation of leave 380 (Restricted to 300) as on 31/12/2013 (b) Annual leave encashment claimed on LTC during current year (2014) for 10 days " +
                "against the previous year's balance of accumulation. (c) Annual Leave accumulated 0 days (No. of Days) for current year (2014). " +
                "(d) Total Annual Leave Accumulation at the credit of officer as on 31/12/2014 (31/12/2014) is 290. (300-10+0) days. (a-b+c)",
            row.narrative,
        )
        assertEquals(on(12, 31, 2014) to null, row.from to row.to, "col 4 the date, col 5 not required")
        assertEquals(listOf("0", "290", "300-2013", "10 days Annual Leave encashment claimed on LTC during 2014"), row.data, "p.133: Data 1 (c), Data 2 (d), Data 3 (a)-year, Data 4 the LTC leave")
        assertEquals("A", row.group)
    }

    @Test
    fun aRealYearsFiguresPrintTheTotalAStatementShowsAndWarnThatItPassesTheCap() {
        val request = acclve(2024, 282, 0, 19)
        val row = rowOf("ACCLVE", request)

        assertEquals("301", row.data[1])
        assertTrue(row.narrative.startsWith("(a) Total accumulation of leave 282 as on 31/12/2023 (b)"))
        assertEquals(listOf(RuleFinding("acclve_over_cap", RuleSeverity.WARN, "ACCLVE", LinkBasis.MANUAL, 133, "(a)")), RealPart2.draft(request).findings)
    }

    @Test
    fun theOrderIsAcknowledgedBeforeTheFirstOfMarchOfTheNextYearUnlessTheOfficerRetires() {
        val input = LeaveEventInput(listOf(acclve(2025, 282, 0, 19)))

        fun ids(
            today: CalendarDate,
            retiring: Boolean = false,
        ): List<String> {
            val request = acclve(2025, 282, 0, 19).let { if (retiring) it.copy(answers = setOf(AcclveRules.ANSWER_RETIRING)) else it }
            return (RealPart2.engine.draft(LeaveEventInput(listOf(request)), today) as DraftResult.Drafted).slip.findings.filter { it.ruleId == "acclve_too_early" }.map { it.severity.name }
        }
        assertEquals(listOf("ACK"), ids(on(2, 28, 2026)))
        assertEquals(emptyList(), ids(on(3, 1, 2026)))
        assertEquals(emptyList(), ids(on(2, 28, 2026), retiring = true))
        assertEquals(1, input.requests.size)
    }

    @Test
    fun theYearsAccumulationAndLeaveUsedAreCheckedAgainstTheirThirtyAndSixty() {
        val findings = RealPart2.draft(acclve(2025, 100, 0, 31, "al_used" to number(30))).findings.map { it.ruleId }.sorted()
        assertEquals(listOf("acclve_accrual_30", "acclve_year_60"), findings)
        assertEquals(emptyList(), RealPart2.draft(acclve(2025, 100, 0, 30, "al_used" to number(30))).findings)
    }

    @Test
    fun anAccumulationWithAFigureMissingIsRefusedByTheFigureAndNeverPrintedWithAGuess() {
        val partial = OccurrenceRequest("ACCLVE", mapOf("year" to number(2025), "as_on" to date(12, 31, 2025), "prev_total" to number(100)))
        val refused = RealPart2.engine.draft(LeaveEventInput(listOf(partial)))
        assertTrue(refused is DraftResult.Refused)
        assertEquals(setOf("ltc_days", "accrued"), (refused as DraftResult.Refused).issues.map { it.slot }.toSet())
    }

    @Test
    fun theAdvanceOfLeaveEncashmentOnLtcPrintsTheAccumulationAndDaysAndTheManualsCertificate() {
        val values = mapOf("journey_on" to date(11, 10), "ltc_days" to number(10), "accumulation" to number(220), "total_ltc" to number(50))
        val row = rowOf("LVELTCAD", request("LVELTCAD", values))

        assertEquals("Advance payment of encashment for 10 days AL, on availing LTC, out of total accumulation of 220 Days as per last year's leave accumulation (Col No 6).", row.narrative)
        assertEquals(on(11, 10) to null, row.from to row.to)
        assertEquals(listOf("220", "10", "", "50"), row.data, "p.134: Data 1 the accumulation, Data 2 the days; Data 3 only when the current year's leave is used (note 6.6)")
        assertTrue(row.certificate.orEmpty().startsWith("CERTIFICATE : ENCASHMENT OF LEAVE ON AVAILING LTC 1. Certificates for claiming advance"))
        assertTrue("1.4. Total leave accumulated up to the previous calendar year is 220 Days" in row.certificate.orEmpty())
        assertTrue("1.5. The total leave encashed on availing LTC during the entire year of service till date is 50 days inclusive of 10 days being availed now." in row.certificate.orEmpty())
    }

    @Test
    fun theEncashmentAfterTheJourneyPrintsTheFourLinesOfTheManualAndQuotesTheLastLtcOrder() {
        val values = mapOf("journey_on" to date(11, 10), "journey_to" to date(11, 20), "ltc_days" to number(10), "accumulation" to number(220), "balance" to number(60), "total_ltc" to number(50), "last_ltc" to order)
        val row = rowOf("LVELTC", request("LVELTC", values))

        assertEquals(
            "Allowed to encash 10 days AL, on availing LTC, out of total accumulation of 220 days till previous year. b. 60 days of balance of Leave LTC for encashment were available to be availed. " +
                "c. LTC journey from 10/11/2026 to 20/11/2026. d. Total leave encashed for availing LTC till date including above period is 50.",
            row.narrative,
        )
        assertEquals(listOf("220", "10", "60", "50"), row.data, "p.135")
        assertEquals("Annual leave last encashed for LTC vide Part II Order No 0123/26 dated 20/06/2026", row.authority, "note 7.8")
        assertTrue("2.5. The total leave encashed on availing LTC till date is 50 days inclusive of 10 days availed now." in row.certificate.orEmpty())
    }

    @Test
    fun theCancellationOfAnAdvanceQuotesTheAdvanceAndTheRefundInTheColumnsTheTableGives() {
        val values = mapOf("journey_on" to date(11, 10), "ltc_days" to number(10), "advance" to order, "mro" to ref("M-5/26", 12, 15), "refund" to text("12345"))
        val row = rowOf("CLVLTCAD", request("CLVLTCAD", values))

        assertEquals("a. Cancellation of no. of days (10) of Annual Leave encashment on LTC granted in advance vide Pt II order No 0123/26 & date 20/06/2026 b. Advance refunded vide MRO No M-5/26 & date 15/12/2026", row.narrative)
        assertEquals(listOf("M-5/26", "15/12/2026", "0123/26", "20/06/2026"), row.data, "p.139: Data 1 and 2 the MRO, Data 3 and 4 the Part II order")
        assertTrue("the amount of Rs. 12345 availed on account of advance" in row.certificate.orEmpty())
    }

    @Test
    fun theFinalEncashmentPrintsTheManualsOwnExampleWithItsBreakUpAndTotal() {
        val values = mapOf("breakup" to text("2011 30\n2013 30\n2015 30\n2017 30\n2018 30\n2019 30\n2020 20\n2021 20"))
        val row = rowOf("ENCASH", request("ENCASH", values))

        assertEquals("Allowed to encash 220 days of Annual Leave on retirement. Year-wise break up of Annual Leave accumulation: 2011 30, 2013 30, 2015 30, 2017 30, 2018 30, 2019 30, 2020 20, 2021 20, Total 220", row.narrative)
        assertEquals(null to null, row.from to row.to)
        assertEquals(listOf("", "220", "", ""), row.data, "p.140: the days are in Data 2 (col 7), not col 8")
    }

    @Test
    fun aBreakUpAbove300PrintsTheCapAndWarnsAndAYearAbove30IsWarnedAbout() {
        val long = (0 until 10).joinToString("\n") { "${2000 + it} 30" } + "\n2010 31"
        val slip = RealPart2.draft(request("ENCASH", mapOf("breakup" to text(long))))
        assertEquals("300", RealPart2.rows(slip).single().data[1])
        assertEquals(listOf("encash_over_cap", "encash_year_over_30"), slip.findings.map { it.ruleId }.sorted())
    }

    @Test
    fun aBreakUpNothingCanReadIsRefusedByItsSlotWithTheReasonTheFormWords() {
        fun reason(value: SlotValue?): DraftIssueReason {
            val values = listOfNotNull(value?.let { "breakup" to it }).toMap()
            val refused = RealPart2.engine.draft(LeaveEventInput(listOf(OccurrenceRequest("ENCASH", values)))) as DraftResult.Refused
            return refused.issues.single { it.slot == "breakup" }.reason
        }
        assertEquals(DraftIssueReason.NOT_ANSWERED, reason(null))
        assertEquals(DraftIssueReason.NOT_ANSWERED, reason(text("  ")))
        assertEquals(DraftIssueReason.WRONG_KIND, reason(text("2011")))
        assertEquals(DraftIssueReason.WRONG_KIND, reason(number(5)))
        assertEquals(DraftIssueReason.OUT_OF_RANGE, reason(text("1900 5")))
        assertEquals(DraftIssueReason.MARKUP, reason(text("2011 <b>")))
    }
}
