package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryYearHeaderTest {

    @Test
    fun testCalculateYearlyStats() {
        val mock1 = ParsedPayslip(
            file = "file1.pdf", year = 2024, monthNum = 8, monthName = "August", dateStr = "08/2024",
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(basicPay = 100000.0, dearnessAllowance = 50000.0),
            deductions = Deductions(dsopSubscription = 20000.0, incomeTax = 15000.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(grossPay = 150000.0, totalDeductions = 35000.0, netRemittance = 115000.0),
            taxAndSavings = null
        )
        val mock2 = ParsedPayslip(
            file = "file2.pdf", year = 2024, monthNum = 9, monthName = "September", dateStr = "09/2024",
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(basicPay = 100000.0, dearnessAllowance = 50000.0),
            deductions = Deductions(dsopSubscription = 25000.0, incomeTax = 15000.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(grossPay = 150000.0, totalDeductions = 40000.0, netRemittance = 110000.0),
            taxAndSavings = null
        )

        val stats = calculateYearlyStats(listOf(mock1, mock2))

        assertEquals(225000.0, stats.totalNet)
        assertEquals(45000.0, stats.totalDsop)
    }
}
