package com.payslipmax.pdfparser.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [diagnosticSuggestionFor] — the read-only lookup Phase 3's UI uses to surface the Tier 6 diagnostic hint. */
class ConfidenceThresholdsDiagnosticTest {
    private fun payslip(diagnosticSuggestion: DiagnosticSuggestion?) =
        ParsedPayslip(
            file = "test.pdf",
            year = 2024,
            monthNum = 1,
            monthName = "January",
            dateStr = "01/2024",
            officer = Officer("Officer Officer", "16/000/000000X", "AR*****90G"),
            earnings = Earnings(),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(0.0, 0.0, 0.0),
            taxAndSavings = null,
            diagnosticSuggestion = diagnosticSuggestion,
        )

    @Test
    fun returnsReasonWhenFieldKeyMatches() {
        val suggestion = DiagnosticSuggestion(fieldKey = "basicPay", reason = "Off by ~50000, likely swapped with an adjacent row")
        assertEquals(
            "Off by ~50000, likely swapped with an adjacent row",
            payslip(suggestion).diagnosticSuggestionFor("basicPay"),
        )
    }

    @Test
    fun returnsNullWhenFieldKeyDoesNotMatch() {
        val suggestion = DiagnosticSuggestion(fieldKey = "basicPay", reason = "Off by ~50000")
        assertNull(payslip(suggestion).diagnosticSuggestionFor("dsopSubscription"))
    }

    @Test
    fun returnsNullWhenNoDiagnosticSuggestion() {
        assertNull(payslip(diagnosticSuggestion = null).diagnosticSuggestionFor("basicPay"))
    }
}
