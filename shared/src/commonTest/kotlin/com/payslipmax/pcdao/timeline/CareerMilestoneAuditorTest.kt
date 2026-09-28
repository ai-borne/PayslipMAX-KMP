package com.payslipmax.pcdao.timeline

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CareerMilestoneAuditorTest {
    private val auditor = CareerMilestoneAuditor()

    private fun createSlip(
        year: Int,
        monthNum: Int,
        monthName: String,
        basicPay: Double,
        da: Double = 84906.0,
        gross: Double = 250000.0,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "slip_${year}_$monthNum.pdf",
            year = year,
            monthNum = monthNum,
            monthName = monthName,
            dateStr = "$year-${if (monthNum < 10) "0$monthNum" else "$monthNum"}-01",
            officer = Officer("Col Rathore", "01/142", "ABCDE1234F"),
            earnings = Earnings(basicPay = basicPay, dearnessAllowance = da),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = gross, totalDeductions = 40000.0, netRemittance = gross - 40000.0),
            taxAndSavings = null,
        )

    @Test
    fun testAuditJanuaryIncrementVerified() {
        val dec2025 = createSlip(2025, 12, "December", basicPay = 144700.0)
        val jan2026 = createSlip(2026, 1, "January", basicPay = 149000.0)

        val milestones = auditor.auditMilestones(listOf(dec2025, jan2026))
        assertEquals(1, milestones.size)
        val m = milestones.first()
        assertEquals(MilestoneType.ANNUAL_INCREMENT_VERIFIED, m.type)
        assertEquals(4300.0, m.monetaryImpact)
        assertFalse(m.isAlert)
        assertTrue(m.description.contains("₹144700 to ₹149000"))
    }

    @Test
    fun testAuditJulyIncrementVerified() {
        val jun2025 = createSlip(2025, 6, "June", basicPay = 140000.0)
        val jul2025 = createSlip(2025, 7, "July", basicPay = 144700.0)

        val milestones = auditor.auditMilestones(listOf(jun2025, jul2025))
        assertEquals(1, milestones.size)
        assertEquals(MilestoneType.ANNUAL_INCREMENT_VERIFIED, milestones.first().type)
    }

    @Test
    fun testAuditDaRevisionAndArrearsSpike() {
        val mar2025 = createSlip(2025, 3, "March", basicPay = 144700.0, da = 84906.0, gross = 271739.0)
        val apr2025 = createSlip(2025, 4, "April", basicPay = 144700.0, da = 88110.0, gross = 318593.0)

        val milestones = auditor.auditMilestones(listOf(mar2025, apr2025))
        assertTrue(milestones.any { it.type == MilestoneType.DA_REVISION_CREDITED })
        assertTrue(milestones.any { it.type == MilestoneType.DA_ARREARS_SPIKE })
    }

    @Test
    fun testEmptyOnSinglePayslip() {
        val dec2025 = createSlip(2025, 12, "December", basicPay = 144700.0)
        val milestones = auditor.auditMilestones(listOf(dec2025))
        assertTrue(milestones.isEmpty())
    }
}
