package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.parser.corpus.CorpusExpected
import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 3 gate (docs/Plan/09_PayAudit_PhasePlan.md), updated by Phase 8's P7-13 fix (walking back to the
 * last trusted month instead of skipping a transition after an untrusted one): coverage of
 * [PayLineChangeExplainer] over one real (de-identified) officer's whole history — 93 of 106
 * month-to-month moves in the tracked pay-line fields carry a reason (87.7%). The 13 gaps are pinned, not
 * just counted, so a new unexplained field or month fails loud instead of quietly widening the known gap
 * (CLAUDE.md "fail loud").
 */
class PayLineChangeExplanationCorpusTest {
    private val history: List<ParsedPayslip> =
        CorpusFixtures.loadIndex()
            .map { CorpusFixtures.loadExpected(it) }
            .distinctBy { it.year * 100 + it.monthNum }
            .sortedBy { it.year * 100 + it.monthNum }
            .map { it.toParsedPayslip() }

    private val timeline = ServiceTimelineBuilder.build(history)

    private val explanations: List<ChangeExplanation> =
        history.flatMap { current -> PayLineChangeExplainer.explain(current, history, timeline) }

    @Test
    fun everyTrackedChangeIsExplainedExceptTheKnownLicenceFeeRateGap() {
        val unexplained = explanations.filter { it.reason == null }
        // The timeline models quarters occupancy as a start/stop toggle only; the officer's own
        // accommodation-rent bracket (which moves the fee while quarters stay occupied) is not ported —
        // Phase 3 carry-over, docs/Plan/09_PayAudit_PhasePlan.md.
        assertEquals(
            listOf(
                "2020/3", "2020/6", "2020/7", "2020/11",
                "2021/2", "2021/3", "2021/5",
                "2022/10", "2022/11",
                "2023/3", "2023/4", "2023/8",
                "2024/12",
            ),
            unexplained.map { "${it.month.year}/${it.month.month}" },
            "Unexplained changes: ${unexplained.map { "${it.month} ${it.field} ${it.from}->${it.to}" }}",
        )
        assertTrue(unexplained.all { it.field == "licenseFee" })
    }

    @Test
    fun coverageGateStaysAtLeastEightyFivePercent() {
        assertEquals(106, explanations.size, "Tracked-change count moved; re-check the pinned gap list too")
        val coverage = explanations.count { it.reason != null }.toDouble() / explanations.size
        assertTrue(coverage >= 0.85, "Coverage dropped to $coverage")
    }

    private fun CorpusExpected.toParsedPayslip() =
        ParsedPayslip(
            file = filename,
            year = year,
            monthNum = monthNum,
            monthName = "",
            dateStr = "${monthNum.toString().padStart(2, '0')}/$year",
            officer = officer,
            earnings = earnings,
            deductions = deductions,
            ledgerBalances = LedgerBalances(),
            summary = summary,
            taxAndSavings = taxAndSavings,
        )
}
