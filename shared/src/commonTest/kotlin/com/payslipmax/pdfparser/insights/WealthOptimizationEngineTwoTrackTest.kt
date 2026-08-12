package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 5 (ADR-3/ADR-4) integration coverage for [WealthOptimizationEngine.analyzeLedger] --
 * split out of [WealthOptimizationEngineTest] to stay under the 300-LOC file limit, mirroring how
 * [DualRegimeEngineAuditTest] sits alongside [DualRegimeEngineTest].
 */
class WealthOptimizationEngineTwoTrackTest {
    private fun createPayslip(
        dsopMonthly: Double = 8_000.0,
        agifMonthly: Double = 5_000.0,
        grossPay: Double = 180_000.0,
        netTaxableIncome: Double = 800_000.0,
        dsopClosingBalance: Double = 500_000.0,
        taxRegime: TaxRegime = TaxRegime.OLD,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "test.pdf",
            year = 2025,
            monthNum = 4,
            monthName = "April",
            dateStr = "04/2025",
            officer = Officer("Test Officer", "12345", "XXXXX0000X"),
            earnings = Earnings(basicPay = 105_300.0, houseRentAllowance = 8000.0),
            deductions = Deductions(dsopSubscription = dsopMonthly, agif = agifMonthly, incomeTax = 5000.0),
            ledgerBalances = LedgerBalances(),
            summary =
                PayslipSummary(
                    grossPay = grossPay,
                    totalDeductions = dsopMonthly + agifMonthly + 5000.0,
                    netRemittance = grossPay - dsopMonthly - agifMonthly - 5000.0,
                ),
            taxAndSavings =
                TaxAndSavings(
                    grossSalaryYtd = grossPay,
                    netTaxableIncome = netTaxableIncome,
                    dsopFund = DsopFund(closingBalance = dsopClosingBalance),
                    taxRegime = taxRegime,
                ),
        )

    @Test
    fun testSwitchRegimeOpportunityReplacedByTwoAdr4Decisions() {
        // D9/ADR-4: the old crude "switch_regime" id must no longer appear; the checklist instead
        // carries the PCDA intimation and the ITR election as separate, differently-reversible items.
        val payslip = createPayslip(dsopMonthly = 8_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)

        assertNull(result.opportunities.find { it.id == "switch_regime" })
        val plan = result.regimeDecisionPlan
        if (plan != null) {
            assertTrue(result.opportunities.any { it.id == "pcda_intimation_regime" })
            assertTrue(result.opportunities.any { it.id == "itr_election_regime" })
            // The intimation itself carries no separate saving -- only the ITR election does, so the
            // total can never double-count the same rupee figure across both entries.
            assertEquals(0.0, result.opportunities.first { it.id == "pcda_intimation_regime" }.estTaxSaved)
        }
    }

    @Test
    fun testTaxTrackReconciliationPopulatedWhenPcdaTotalTaxPayableAvailable() {
        val payslip =
            createPayslip(taxRegime = TaxRegime.NEW).copy(
                taxAndSavings =
                    TaxAndSavings(
                        netTaxableIncome = 800_000.0,
                        totalTaxPayable = 700_000.0,
                        taxRegime = TaxRegime.NEW,
                    ),
            )
        val result = WealthOptimizationEngine.analyze(payslip)
        assertNotNull(result.taxTrackReconciliation)
        assertEquals("2025-26", result.taxTrackReconciliation?.financialYear)
    }

    @Test
    fun testTaxTrackReconciliationNullWhenPcdaTotalTaxPayableUnavailable() {
        val payslip = createPayslip().copy(taxAndSavings = TaxAndSavings(netTaxableIncome = 800_000.0, totalTaxPayable = null))
        val result = WealthOptimizationEngine.analyze(payslip)
        assertNull(result.taxTrackReconciliation)
    }

    @Test
    fun testDsopWasteInsightPresentUnderNewRegimeWithNonZeroDsop() {
        val payslip = createPayslip(dsopMonthly = 40_000.0, agifMonthly = 12_500.0, taxRegime = TaxRegime.NEW)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertNotNull(result.dsopWasteInsight)
        assertTrue(result.dsopWasteInsight!!.taxBenefitForgoneAnnual > 0.0)
    }

    @Test
    fun testDsopWasteInsightAbsentUnderOldRegime() {
        val payslip = createPayslip(dsopMonthly = 40_000.0, agifMonthly = 12_500.0, taxRegime = TaxRegime.OLD)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertNull(result.dsopWasteInsight)
    }

    @Test
    fun testApr2026EndToEndPopulatesArrearsTransparencyAndReconciliation() {
        val payslip =
            ParsedPayslip(
                file = "04_apr_2026.pdf",
                year = 2026,
                monthNum = 4,
                monthName = "April",
                dateStr = "04/2026",
                officer = Officer("Officer", "16/000/000000X", "AR*****90G"),
                earnings =
                    Earnings(
                        basicPay = 149000.0,
                        dearnessAllowance = 98700.0,
                        militaryServicePay = 15500.0,
                        transportAllowance = 3600.0,
                        transportAllowanceDa = 2160.0,
                        riskHardshipAllowance = 21125.0,
                        arrearsDa = 9870.0,
                        arrearsTptaDa = 216.0,
                        adjPayAndAllce = 1657.0,
                    ),
                deductions =
                    Deductions(
                        dsopSubscription = 40000.0,
                        agif = 12500.0,
                        incomeTax = 50425.0,
                        educationCess = 2017.0,
                        ticketRecovery = 3056.0,
                    ),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(grossPay = 301828.0, totalDeductions = 107998.0, netRemittance = 193830.0),
                taxAndSavings =
                    TaxAndSavings(
                        grossSalaryYtd = 586894.0,
                        totalTaxableIncome = 3487744.0,
                        standardDeduction = 75000.0,
                        netTaxableIncome = 3412740.0,
                        totalTaxPayable = 603822.0,
                        taxDeductedYtd = 99567.0,
                        cessDeductedYtd = 3983.0,
                        taxRegime = TaxRegime.NEW,
                    ),
            )

        val result = WealthOptimizationEngine.analyzeLedger(listOf(payslip), payslip, "2026-27")

        assertNotNull(result.arrearsTransparency)
        assertEquals(10086.0, result.arrearsTransparency!!.arrearsAmount, 0.01)
        assertNotNull(result.taxTrackReconciliation)
        assertEquals(603822.0, result.taxTrackReconciliation?.tdsTrackAnnual)
    }

    @Test
    fun testMidYearRegimeChangeFlowsThroughOptimizationResult() {
        // Not just a RegimeDecisionPlannerTest unit check -- confirms analyzeLedger actually wires
        // detectMidYearRegimeChange's result onto the OptimizationResult it returns.
        val april = createPayslip(taxRegime = TaxRegime.OLD).copy(monthNum = 4, dateStr = "04/2025")
        val june = createPayslip(taxRegime = TaxRegime.NEW).copy(monthNum = 6, dateStr = "06/2025")
        val result = WealthOptimizationEngine.analyzeLedger(listOf(april, june), targetFy = "2025-26")

        assertNotNull(result.midYearRegimeChange)
        assertTrue(result.midYearRegimeChange!!.detected)
        assertEquals(6, result.midYearRegimeChange!!.changeMonthNum)
    }
}
