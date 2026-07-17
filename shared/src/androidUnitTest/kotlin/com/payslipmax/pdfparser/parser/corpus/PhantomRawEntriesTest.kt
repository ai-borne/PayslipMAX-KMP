package com.payslipmax.pdfparser.parser.corpus

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit tests for [CorpusFixtures.phantomRawEntries] (Phase 0 — the D1 assertion surface for
 * `PhantomFreeCorpusInvariantTest`). This is a diagnostic helper only; it has no effect on parsing.
 */
class PhantomRawEntriesTest {
    private fun payslip(
        rawEarnings: Map<String, Double> = emptyMap(),
        rawDeductions: Map<String, Double> = emptyMap(),
    ) = ParsedPayslip(
        file = "test.pdf",
        year = 2024,
        monthNum = 1,
        monthName = "Jan",
        dateStr = "01-Jan-2024",
        officer = Officer(name = "Officer", accountNo = "16/000/000000X", pan = "AR*****90G"),
        earnings = Earnings(),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(grossPay = 0.0, totalDeductions = 0.0, netRemittance = 0.0),
        taxAndSavings = null,
        rawEarnings = rawEarnings,
        rawDeductions = rawDeductions,
    )

    @Test
    fun `TPTADA landmine values are flagged by this diagnostic (guardrail belongs to Phase 2, not here)`() {
        // Documents a known, deliberate limitation: phantomRawEntries is a cheap value/label-shape
        // diagnostic for D1, not a removal rule, so it is intentionally naive about real allowances
        // whose printed rupee value collides with the bare-year range (TPTADA = 1908 in Dec-2024,
        // 2088 in Jan-2026 — the sprint's own documented landmine). Verified against the full 140-
        // fixture corpus (Phase 0): TPTADA always matches a standard key and never actually reaches
        // the raw channel today, so this never produces a false D1 violation in practice. The real
        // never-delete-real-pay guardrail is Phase 2's PlausibilityBackstopTest, which is
        // geometry/label-gated (never value-only) before anything is removed.
        assertEquals(1, CorpusFixtures.phantomRawEntries(payslip(rawEarnings = mapOf("TPTADA" to 1908.0))).size)
        assertEquals(1, CorpusFixtures.phantomRawEntries(payslip(rawEarnings = mapOf("TPTADA" to 2088.0))).size)
    }

    @Test
    fun `bare statement-title year is phantom-shaped`() {
        val parsed = payslip(rawDeductions = mapOf("STATEMENT OF ACCOUNT FOR" to 2015.0))
        val entries = CorpusFixtures.phantomRawEntries(parsed)
        assertEquals(1, entries.size)
        assertTrue(entries.first().contains("2015.0"))
    }

    @Test
    fun `pin-shaped six-digit value is phantom-shaped`() {
        val parsed = payslip(rawEarnings = mapOf("Glibar Maidan Pune" to 411001.0))
        assertEquals(1, CorpusFixtures.phantomRawEntries(parsed).size)
    }

    @Test
    fun `label with no alphabetic content is phantom-shaped regardless of amount`() {
        val parsed = payslip(rawDeductions = mapOf("271739" to 500.0))
        assertEquals(1, CorpusFixtures.phantomRawEntries(parsed).size)
    }

    @Test
    fun `year-shaped amount with a real label is still flagged (Phase 1-2 will geometry-gate this)`() {
        // phantomRawEntries is a value/label-shape diagnostic only, not the production removal rule —
        // this documents that a real in-body item colliding with the year range would need Phase 1's
        // vertical bounding (not this helper) to be told apart from a genuine footer/header phantom.
        val parsed = payslip(rawEarnings = mapOf("Next Increment Date" to 2027.0))
        assertEquals(1, CorpusFixtures.phantomRawEntries(parsed).size)
    }

    @Test
    fun `non-phantom-shaped raw entries are ignored`() {
        val parsed =
            payslip(
                rawEarnings = mapOf("SomeUnknownAllce" to 1234.0),
                rawDeductions = mapOf("AnotherUnknownRecovery" to 99.0),
            )
        assertTrue(CorpusFixtures.phantomRawEntries(parsed).isEmpty())
    }
}
