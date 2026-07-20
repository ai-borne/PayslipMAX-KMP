package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConversationalTaxNarrativeEngineTest {
    private fun createPayslip(
        year: Int,
        monthNum: Int,
        monthName: String,
        grossPay: Double = 150000.0,
        incomeTax: Double = 12500.0,
        dsop: Double = 15000.0,
    ): ParsedPayslip {
        return ParsedPayslip(
            file = "payslip_${year}_$monthNum.pdf",
            year = year,
            monthNum = monthNum,
            monthName = monthName,
            dateStr = "$monthNum/$year",
            officer = Officer("John Doe", "12345", "ABCDE1234F"),
            earnings = Earnings(basicPay = 90000.0, dearnessAllowance = 45000.0),
            deductions = Deductions(incomeTax = incomeTax, dsopSubscription = dsop),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = grossPay, totalDeductions = incomeTax + dsop, netRemittance = grossPay - (incomeTax + dsop)),
            taxAndSavings = TaxAndSavings(grossSalaryYtd = grossPay, taxDeductedYtd = incomeTax),
        )
    }

    @Test
    fun testNarrativeGenerationSingleMonth() {
        val p1 = createPayslip(2025, 4, "April 2025")
        val summary = TaxLedgerAggregator.aggregateFy(listOf(p1), "2025-26")

        val narrative =
            ConversationalTaxNarrativeEngine.generateNarrative(
                payslips = listOf(p1),
                fySummary = summary,
                projectedTax = 215400.0,
            )

        assertEquals("2025-26", narrative.financialYear)
        assertEquals("2026-27", narrative.assessmentYear)
        assertEquals(1, narrative.parsedMonthCount)
        assertEquals(1, narrative.monthlyLedgerList.size)
        assertEquals(12500.0, narrative.monthlyLedgerList[0].tdsDeducted)
        assertEquals(15000.0, narrative.monthlyLedgerList[0].dsopContribution)
        assertNotNull(narrative.missingMonthNudge)
        assertTrue(narrative.effectiveTaxRatePct > 0.0)
    }

    @Test
    fun testNarrativeGenerationMultiMonth() {
        val payslips =
            listOf(
                createPayslip(2025, 4, "April 2025"),
                createPayslip(2025, 5, "May 2025"),
                createPayslip(2025, 6, "June 2025"),
                createPayslip(2025, 7, "July 2025"),
            )
        val summary = TaxLedgerAggregator.aggregateFy(payslips, "2025-26")

        val narrative =
            ConversationalTaxNarrativeEngine.generateNarrative(
                payslips = payslips,
                fySummary = summary,
                projectedTax = 215400.0,
            )

        assertEquals(4, narrative.parsedMonthCount)
        assertEquals(4, narrative.monthlyLedgerList.size)
        assertEquals(50000.0, narrative.totalTdsYtd)
        assertEquals(60000.0, narrative.totalDsopYtd)
    }
}
