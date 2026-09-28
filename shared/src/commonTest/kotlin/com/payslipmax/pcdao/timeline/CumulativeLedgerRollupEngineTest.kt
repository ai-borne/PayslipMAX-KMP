package com.payslipmax.pcdao.timeline

import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CumulativeLedgerRollupEngineTest {
    private val engine = CumulativeLedgerRollupEngine()

    private fun createSlip(
        year: Int,
        monthNum: Int,
        monthName: String,
        basicPay: Double,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "slip_${year}_$monthNum.pdf",
            year = year,
            monthNum = monthNum,
            monthName = monthName,
            dateStr = "$year-${if (monthNum < 10) "0$monthNum" else "$monthNum"}-01",
            officer = Officer("Col Rathore", "01/142", "ABCDE1234F"),
            earnings = Earnings(basicPay = basicPay, militaryServicePay = 15500.0, houseRentAllowance = 0.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 250000.0, totalDeductions = 40000.0, netRemittance = 210000.0),
            taxAndSavings = null,
        )

    @Test
    fun testCumulativeArrearsRollupWithChangingBasicPay() {
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.HOUSE_FAMILY_SPR),
                customDaPercent = 50.0,
            )
        // 2 months at 144,700 (20% HRA = 28,940/mo -> 57,880)
        // 2 months at 149,000 (20% HRA = 29,800/mo -> 59,600)
        // Total expected = 117,480
        val slips =
            listOf(
                createSlip(2025, 11, "November", 144700.0),
                createSlip(2025, 12, "December", 144700.0),
                createSlip(2026, 1, "January", 149000.0),
                createSlip(2026, 2, "February", 149000.0),
            )

        val rollup = engine.calculateRollup(slips, context)

        assertEquals(4, rollup.auditedMonthCount)
        assertEquals(117480.0, rollup.totalUnderpaidArrears)
        assertEquals(4, rollup.monthlyBreakdowns.size)
        assertEquals(28940.0, rollup.monthlyBreakdowns[0].arrearsDue)
        assertEquals(28940.0, rollup.monthlyBreakdowns[1].arrearsDue)
        assertEquals(29800.0, rollup.monthlyBreakdowns[2].arrearsDue)
        assertEquals(29800.0, rollup.monthlyBreakdowns[3].arrearsDue)
        assertTrue(rollup.summaryText.contains("117480"))
    }

    @Test
    fun testCleanLedgerNoArrears() {
        val context = ActiveSituationalContext(activeTileIds = emptySet())
        val slips =
            listOf(
                createSlip(2026, 1, "January", 149000.0),
                createSlip(2026, 2, "February", 149000.0),
            )

        val rollup = engine.calculateRollup(slips, context)
        assertEquals(0.0, rollup.totalUnderpaidArrears)
        assertTrue(rollup.monthlyBreakdowns.isEmpty())
        assertTrue(rollup.summaryText.contains("Clean ledger"))
    }
}
