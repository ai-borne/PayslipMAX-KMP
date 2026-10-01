package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShadowLedgerCeaReconciliationTest {
    private val reconciler = ShadowLedgerReconciler()

    private fun createPayslip(
        year: Int,
        monthNum: Int,
        earnings: Earnings = Earnings(),
        rawEarnings: Map<String, Double> = emptyMap(),
    ): ParsedPayslip =
        ParsedPayslip(
            file = "payslip_$year$monthNum.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "Month$monthNum",
            dateStr = "30-$monthNum-$year",
            officer = Officer("Col Rathore", "IC12345", "ABCDE1234F"),
            earnings = earnings.copy(basicPay = 149000.0, militaryServicePay = 15500.0),
            deductions = com.payslipmax.pdfparser.domain.Deductions(),
            ledgerBalances = com.payslipmax.pdfparser.domain.LedgerBalances(),
            summary = PayslipSummary(grossPay = 0.0, totalDeductions = 0.0, netRemittance = 0.0),
            taxAndSavings = null,
            rawEarnings = rawEarnings,
        )

    @Test
    fun testOngoingAcademicSessionYieldsInformationalNudgeWithZeroArrearsImpact() {
        val augPayslip = createPayslip(2026, 8)
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.CEA_ONE_CHILD),
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(augPayslip, context, listOf(augPayslip))
        val nudge = result.discrepancies.firstOrNull { it.id == "DISC_NUDGE_CEA" }

        assertTrue(nudge != null, "Ongoing session without claim should emit an informational nudge")
        assertEquals(DiscrepancySeverity.INFO, nudge.severity)
        assertEquals(DiscrepancyType.UNDERPAYMENT, nudge.type)
        assertEquals(0.0, nudge.annualImpact, "Informational nudge must not inflate annual unclaimed total")
        assertEquals(0.0, nudge.monthlyImpact)
        assertEquals(0.0, result.totalUnclaimedAnnual, "Result totalUnclaimedAnnual must be 0 for ongoing session")
        assertTrue(result.lineItems.none { it.allowanceKey == "CEA" })
    }

    @Test
    fun testCeaCreditedInEarlierMonthOfSameFySuppressesDiscrepancyInAugust() {
        val mayPayslip =
            createPayslip(
                year = 2026,
                monthNum = 5,
                earnings = Earnings(childrenEducationAllowance = 33750.0),
            )
        val augPayslip = createPayslip(year = 2026, monthNum = 8)
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.CEA_ONE_CHILD),
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(augPayslip, context, listOf(mayPayslip, augPayslip))
        assertNull(result.discrepancies.firstOrNull { it.id.contains("CEA") })
        assertEquals(0.0, result.totalUnclaimedAnnual)
    }

    @Test
    fun testCeaCreditedInRawEarningsEarlierInFySuppressesDiscrepancy() {
        val aprPayslip =
            createPayslip(
                year = 2026,
                monthNum = 4,
                rawEarnings = mapOf("ARR-CEA" to 33750.0),
            )
        val augPayslip = createPayslip(year = 2026, monthNum = 8)
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.CEA_ONE_CHILD),
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(augPayslip, context, listOf(aprPayslip, augPayslip))
        assertNull(result.discrepancies.firstOrNull { it.id.contains("CEA") })
        assertEquals(0.0, result.totalUnclaimedAnnual)
    }

    @Test
    fun testCompletedFinancialYearWithZeroClaimsFlagsWarningAndArrears() {
        // 12 months for FY 2025-26 with zero CEA
        val fullYearPayslips = (1..12).map { month -> createPayslip(if (month <= 3) 2026 else 2025, month) }
        val targetPayslip = fullYearPayslips.first { it.monthNum == 3 } // March 2026 end of FY
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.CEA_ONE_CHILD),
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(targetPayslip, context, fullYearPayslips)
        val disc = result.discrepancies.firstOrNull { it.id == "DISC_UNDERPAY_CEA" }

        assertTrue(disc != null, "Full year with 0 claim must flag genuine underpayment")
        assertEquals(DiscrepancySeverity.WARNING, disc.severity)
        assertEquals(33750.0, disc.annualImpact)
        assertEquals(2812.50, disc.monthlyImpact)
        assertEquals(33750.0, result.totalUnclaimedAnnual)
        assertTrue(result.lineItems.any { it.allowanceKey == "CEA" })
    }
}
