package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.parser.corpus.CorpusExpected
import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs [DaArrearsAuditor] over one officer's real (de-identified) payslip history. Every DA arrears
 * payment in it was correct, so any SALARY_LOSS here is a false alarm that would offer the user a
 * complaint letter for money they were never owed.
 */
class DaArrearsAuditorCorpusTest {
    private val history: List<ParsedPayslip> =
        CorpusFixtures.loadIndex()
            .map { CorpusFixtures.loadExpected(it) }
            .distinctBy { it.year * 100 + it.monthNum }
            .sortedBy { it.year * 100 + it.monthNum }
            .map { it.toParsedPayslip() }

    private val findings: List<Anomaly> =
        history.flatMapIndexed { i, current ->
            DaArrearsAuditor().audit(current, history.getOrNull(i - 1), history.take(i))
        }

    @Test
    fun correctArrearsAcrossTheCorpusRaiseNoSalaryLoss() {
        val falseAlarms = findings.filter { it.type == "SALARY_LOSS" }
        assertTrue(falseAlarms.isEmpty(), "False DA arrears alarms: $falseAlarms")
    }

    @Test
    fun everySeventhCpcDaRiseWithArrearsIsVerified() {
        val verifiedDaMonths =
            findings
                .filter { it.type == "ARREARS_AUDIT" && it.description.contains("Dearness Allowance (DA)") }
                .map { it.month }
                .toSet()
        val expected = setOf("10/2017", "04/2018", "10/2018", "03/2019", "10/2019", "03/2024", "10/2024", "04/2025", "10/2025", "04/2026")
        assertEquals(expected, verifiedDaMonths)
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
