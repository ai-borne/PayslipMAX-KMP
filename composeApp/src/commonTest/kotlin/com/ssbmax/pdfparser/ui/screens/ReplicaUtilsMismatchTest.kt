package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.domain.Deductions
import com.ssbmax.pdfparser.domain.Earnings
import com.ssbmax.pdfparser.domain.LedgerBalances
import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.domain.PayslipSummary
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
}
