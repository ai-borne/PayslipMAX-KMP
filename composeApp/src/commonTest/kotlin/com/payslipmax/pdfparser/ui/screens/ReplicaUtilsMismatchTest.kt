package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReplicaUtilsMismatchTest {
    private fun rawPayslip(
        rawEarnings: Map<String, Double> = emptyMap(),
        rawDeductions: Map<String, Double> = emptyMap(),
        grossPay: Double = 0.0,
        totalDeductions: Double = 0.0,
    ) = ParsedPayslip(
        file = "test.pdf",
        year = 2025,
        monthNum = 1,
        monthName = "January",
        dateStr = "01/2025",
        officer = Officer("Officer Officer", "16/000/000000X", "AR*****90G"),
        earnings = Earnings(),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(grossPay, totalDeductions, grossPay - totalDeductions),
        taxAndSavings = null,
        rawEarnings = rawEarnings,
        rawDeductions = rawDeductions,
    )

    @Test
    fun miscRowAppearsInRawCreditsWhenUnderExtracted() {
        val payslip = rawPayslip(rawEarnings = mapOf("BPAY" to 140000.0), grossPay = 145000.0)
        val credits = getCreditsList(payslip)
        val misc = credits.find { it.code == "MISC" }
        assertNotNull(misc, "MISC row must appear when items under-count grossPay")
        assertEquals(5000.0, misc.amount)
        assertEquals("miscEarnings", misc.fieldKey)
        assertEquals(2, credits.size)
    }

    @Test
    fun miscRowDoesNotAppearInRawCreditsWhenSumMatchesGross() {
        val payslip = rawPayslip(rawEarnings = mapOf("BPAY" to 145000.0), grossPay = 145000.0)
        val credits = getCreditsList(payslip)
        assertTrue(credits.none { it.code == "MISC" }, "MISC must not appear when sum equals grossPay")
        assertEquals(1, credits.size)
    }

    @Test
    fun miscRowDoesNotAppearInRawCreditsWhenPhantomOvercount() {
        val payslip = rawPayslip(rawEarnings = mapOf("BPAY" to 150000.0), grossPay = 145000.0)
        val credits = getCreditsList(payslip)
        assertTrue(credits.none { it.code == "MISC" }, "MISC must not be added when items exceed grossPay")
        assertEquals(1, credits.size)
    }

    @Test
    fun miscRowAppearsInRawDebitsWhenUnderExtracted() {
        val payslip = rawPayslip(rawDeductions = mapOf("DSOP" to 30000.0), totalDeductions = 35000.0)
        val debits = getDebitsList(payslip)
        val misc = debits.find { it.code == "MISC" }
        assertNotNull(misc, "MISC row must appear when items under-count totalDeductions")
        assertEquals(5000.0, misc.amount)
        assertEquals("miscDeductions", misc.fieldKey)
        assertEquals(2, debits.size)
    }

    @Test
    fun miscRowDoesNotAppearInRawDebitsWhenSumMatchesTotal() {
        val payslip = rawPayslip(rawDeductions = mapOf("DSOP" to 35000.0), totalDeductions = 35000.0)
        val debits = getDebitsList(payslip)
        assertTrue(debits.none { it.code == "MISC" })
        assertEquals(1, debits.size)
    }

    @Test
    fun creditsMismatchIsZeroAfterMiscAbsorbsUnderExtraction() {
        val payslip = rawPayslip(rawEarnings = mapOf("BPAY" to 140000.0), grossPay = 145000.0)
        assertEquals(0.0, creditsMismatch(payslip))
    }

    @Test
    fun creditsMismatchIsPositiveWhenPhantomEntryPresent() {
        val payslip = rawPayslip(rawEarnings = mapOf("BPAY" to 150000.0), grossPay = 145000.0)
        assertEquals(5000.0, creditsMismatch(payslip))
    }

    @Test
    fun debitsMismatchIsZeroAfterMiscAbsorbsUnderExtraction() {
        val payslip = rawPayslip(rawDeductions = mapOf("DSOP" to 30000.0), totalDeductions = 35000.0)
        assertEquals(0.0, debitsMismatch(payslip))
    }

    @Test
    fun debitsMismatchIsPositiveWhenPhantomEntryPresent() {
        val payslip = rawPayslip(rawDeductions = mapOf("DSOP" to 40000.0), totalDeductions = 35000.0)
        assertEquals(5000.0, debitsMismatch(payslip))
    }

    // Regression: an unrelated unmatched entry in rawEarnings (e.g. a genuinely novel allowance code
    // the parser couldn't map to a standard field) must not hide an otherwise fully itemized
    // structured earnings breakdown. This is the shape of the real Mar 2025 payslip bug: earnings
    // were correctly classified into standard fields, but any non-empty rawEarnings map used to make
    // getCreditsList() discard all of them and fall back to a single synthetic MISC line instead.
    @Test
    fun structuredEarningsSurviveAlongsideUnrelatedRawEntry() {
        val payslip =
            ParsedPayslip(
                file = "test.pdf",
                year = 2025,
                monthNum = 3,
                monthName = "March",
                dateStr = "03/2025",
                officer = Officer("Officer Officer", "16/000/000000X", "AR*****90G"),
                earnings =
                    Earnings(
                        basicPay = 144700.0,
                        dearnessAllowance = 84906.0,
                        militaryServicePay = 15500.0,
                        transportAllowance = 3600.0,
                        transportAllowanceDa = 1908.0,
                        riskHardshipAllowance = 21125.0,
                    ),
                deductions = Deductions(dsopSubscription = 40000.0, agif = 10000.0, incomeTax = 44646.0, educationCess = 1786.0),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(grossPay = 271739.0 + 500.0, totalDeductions = 96432.0, netRemittance = 175807.0),
                taxAndSavings = null,
                rawEarnings = mapOf("SPECIALDUTY" to 500.0),
                rawDeductions = emptyMap(),
            )

        val credits = getCreditsList(payslip)
        assertEquals(setOf("BPAY", "DA", "MSP", "TPTA", "TPTADA", "RHA", "SPECIALDUTY"), credits.map { it.code }.toSet())
        assertTrue(credits.none { it.code == "MISC" }, "MISC must not appear when structured + raw items already sum to grossPay")
        assertEquals(271739.0 + 500.0, credits.sumOf { it.amount })
    }
}
