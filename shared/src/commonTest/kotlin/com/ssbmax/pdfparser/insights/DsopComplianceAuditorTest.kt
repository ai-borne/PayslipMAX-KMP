package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DsopComplianceAuditorTest {
    private fun createMockPayslip(
        basicPay: Double = 100000.0,
        dsop: Double = 10000.0,
        miscAdjYtd: Double = 0.0,
        closingBalance: Double = 0.0,
        monthNum: Int = 4
    ): ParsedPayslip {
        val gross = basicPay + 15500.0 // basic + msp
        return ParsedPayslip(
            file = "test.pdf",
            year = 2026,
            monthNum = monthNum,
            monthName = "MonthName",
            dateStr = "04/2026",
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(basicPay = basicPay, militaryServicePay = 15500.0),
            deductions = Deductions(dsopSubscription = dsop),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = gross, totalDeductions = dsop, netRemittance = gross - dsop),
            taxAndSavings = TaxAndSavings(
                dsopFund = DsopFund(
                    miscAdjYtd = miscAdjYtd,
                    closingBalance = closingBalance
                )
            )
        )
    }

    @Test
    fun testZeroDsopComplianceError() {
        val current = createMockPayslip(dsop = 0.0)
        val auditor = DsopComplianceAuditor()
        val result = auditor.audit(current, null, emptyList())

        val complianceAnomaly = result.find { it.type == "DSOP_COMPLIANCE" }
        assertTrue(complianceAnomaly != null)
        assertEquals(6000.0, complianceAnomaly.amount) // 6% of 100k
    }

    @Test
    fun testDsopMilestoneInMarch() {
        val current = createMockPayslip(miscAdjYtd = 45000.0, closingBalance = 800000.0, monthNum = 3)
        val auditor = DsopComplianceAuditor()
        val result = auditor.audit(current, null, emptyList())

        val milestone = result.find { it.type == "DSOP_MILESTONE" }
        assertTrue(milestone != null)
        assertEquals(45000.0, milestone.amount)
        assertTrue(milestone.description.contains("Tax-free annual interest"))
    }
}
