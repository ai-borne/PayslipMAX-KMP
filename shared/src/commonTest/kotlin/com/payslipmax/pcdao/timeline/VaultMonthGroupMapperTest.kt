package com.payslipmax.pcdao.timeline

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultMonthGroupMapperTest {
    private fun createSlip(
        year: Int,
        monthNum: Int,
        monthName: String,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "slip_${year}_$monthNum.pdf",
            year = year,
            monthNum = monthNum,
            monthName = monthName,
            dateStr = "$year-${if (monthNum < 10) "0$monthNum" else "$monthNum"}-01",
            officer = Officer("Col Rathore", "01/142", "ABCDE1234F"),
            earnings = Earnings(basicPay = 144700.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 250000.0, totalDeductions = 40000.0, netRemittance = 210000.0),
            taxAndSavings = null,
        )

    @Test
    fun testSortNewestFirst() {
        val slips =
            listOf(
                createSlip(2025, 4, "April"),
                createSlip(2026, 8, "August"),
                createSlip(2025, 12, "December"),
                createSlip(2026, 1, "January"),
            )
        val sorted = VaultMonthGroupMapper.sortNewestFirst(slips)
        assertEquals(2026, sorted[0].year)
        assertEquals(8, sorted[0].monthNum)
        assertEquals(2026, sorted[1].year)
        assertEquals(1, sorted[1].monthNum)
        assertEquals(2025, sorted[2].year)
        assertEquals(12, sorted[2].monthNum)
        assertEquals(2025, sorted[3].year)
        assertEquals(4, sorted[3].monthNum)
    }

    @Test
    fun testFinancialYearLabel() {
        assertEquals("FY 2025-26", VaultMonthGroupMapper.getFinancialYearLabel(2025, 4))
        assertEquals("FY 2025-26", VaultMonthGroupMapper.getFinancialYearLabel(2025, 12))
        assertEquals("FY 2025-26", VaultMonthGroupMapper.getFinancialYearLabel(2026, 1))
        assertEquals("FY 2025-26", VaultMonthGroupMapper.getFinancialYearLabel(2026, 3))
        assertEquals("FY 2026-27", VaultMonthGroupMapper.getFinancialYearLabel(2026, 4))
        assertEquals("FY 2026-27", VaultMonthGroupMapper.getFinancialYearLabel(2026, 8))
    }

    @Test
    fun testGroupByFinancialYear() {
        val slips =
            listOf(
                createSlip(2025, 4, "April"),
                createSlip(2025, 5, "May"),
                createSlip(2026, 2, "February"),
                createSlip(2026, 6, "June"),
            )
        val grouped = VaultMonthGroupMapper.groupByFinancialYear(slips)
        assertTrue(grouped.containsKey("FY 2026-27"))
        assertTrue(grouped.containsKey("FY 2025-26"))
        assertEquals(1, grouped["FY 2026-27"]?.size)
        assertEquals(3, grouped["FY 2025-26"]?.size)
    }
}
