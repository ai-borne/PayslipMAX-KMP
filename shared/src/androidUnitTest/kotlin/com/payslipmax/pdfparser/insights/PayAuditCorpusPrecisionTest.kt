package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.parser.corpus.CorpusExpected
import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 2 precision gate: the timeline-based auditors run over one real (de-identified) officer's whole
 * history. This officer was paid correctly, so any finding from them is a false alarm that could offer a
 * complaint letter. Explaining a TPTA-free month needs the months on both sides of it, so the gate runs
 * with the full history; the last test pins what the app sees when it audits a month the moment it is
 * imported, before later payslips exist.
 */
class PayAuditCorpusPrecisionTest {
    private val history: List<ParsedPayslip> =
        CorpusFixtures.loadIndex()
            .map { CorpusFixtures.loadExpected(it) }
            .distinctBy { it.year * 100 + it.monthNum }
            .sortedBy { it.year * 100 + it.monthNum }
            .map { it.toParsedPayslip() }

    private fun findings(
        auditor: RuleAuditor,
        onlyEarlierMonths: Boolean = false,
    ): List<Anomaly> =
        history.flatMapIndexed { i, current ->
            auditor.audit(current, history.getOrNull(i - 1), if (onlyEarlierMonths) history.take(i) else history)
        }

    @Test
    fun noTimelineAuditorRaisesAFalseFindingOnTheCorpus() {
        val auditors = listOf(TptaEntitlementAuditor(), MissingAllowanceAuditor(), IncrementAuditor(), MspAuditor())
        val falseAlarms = auditors.flatMap { findings(it) }
        assertTrue(falseAlarms.isEmpty(), "False Pay Audit findings: ${falseAlarms.map { "${it.month} ${it.type}/${it.field}" }}")
    }

    @Test
    fun transitionMonthsWithoutTptaAreExplainedNotFlagged() {
        val noTpta =
            history
                .filter { it.earnings.basicPay > 0.0 && it.earnings.transportAllowance == 0.0 && PayMonth(it.year, it.monthNum) >= PayMonth(2017, 7) }
                .map { it.dateStr }
        // Dec 2019 (city class changes), Apr-May 2022 (relocation), Sep 2024 (Risk & Hardship posting starts).
        assertEquals(listOf("12/2019", "04/2022", "05/2022", "09/2024"), noTpta)
        assertTrue(findings(TptaEntitlementAuditor()).isEmpty())
    }

    @Test
    fun auditedAtImportBeforeLaterPayslipsExistTheTransitionMonthsAreStillFlagged() {
        // Known limitation (Phase 7): the explaining months (Jan 2020, Jun 2022, Oct 2024) are not yet stored.
        val flagged = findings(TptaEntitlementAuditor(), onlyEarlierMonths = true).map { it.month }
        assertEquals(listOf("12/2019", "05/2022", "09/2024"), flagged)
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
