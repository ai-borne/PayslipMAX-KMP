package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WealthOptimizationEngineTest {
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
    fun testMarginalRateZeroBelowExemptionLimit() {
        assertEquals(0.0, WealthOptimizationEngine.deriveMarginalRate(200_000.0, TaxRegime.OLD))
    }

    @Test
    fun testMarginalRateFivePercent() {
        assertEquals(0.05, WealthOptimizationEngine.deriveMarginalRate(400_000.0, TaxRegime.OLD))
    }

    @Test
    fun testMarginalRateTwentyPercent() {
        assertEquals(0.20, WealthOptimizationEngine.deriveMarginalRate(700_000.0, TaxRegime.OLD))
    }

    @Test
    fun testMarginalRateThirtyPercent() {
        assertEquals(0.30, WealthOptimizationEngine.deriveMarginalRate(1_200_000.0, TaxRegime.OLD))
    }

    @Test
    fun testNewRegimeMarginalRates() {
        // Phase 1 (D1/D2/D6 fix): FY2025-26+ slabs (4/8/12/16/20/24L), not the stale FY2023-24-vintage
        // thresholds (3/7/9/12/15L) this test used to pin -- default fy is "2026-27".
        assertEquals(0.0, WealthOptimizationEngine.deriveMarginalRate(200_000.0, TaxRegime.NEW))
        assertEquals(0.05, WealthOptimizationEngine.deriveMarginalRate(600_000.0, TaxRegime.NEW))
        assertEquals(0.10, WealthOptimizationEngine.deriveMarginalRate(1_000_000.0, TaxRegime.NEW))
        assertEquals(0.15, WealthOptimizationEngine.deriveMarginalRate(1_400_000.0, TaxRegime.NEW))
        assertEquals(0.20, WealthOptimizationEngine.deriveMarginalRate(1_800_000.0, TaxRegime.NEW))
        assertEquals(0.25, WealthOptimizationEngine.deriveMarginalRate(2_200_000.0, TaxRegime.NEW))
        assertEquals(0.30, WealthOptimizationEngine.deriveMarginalRate(3_000_000.0, TaxRegime.NEW))
    }

    @Test
    fun test80CHeadroomPositiveWhenUnused() {
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        val opportunity = result.opportunities.find { it.id == "80c_dsop" }
        assertTrue(opportunity != null, "80C DSOP opportunity should exist when headroom is positive")
        assertEquals(66_000.0, opportunity.unusedAmount, 0.01)
    }

    @Test
    fun test80CHeadroomZeroWhenExceeded() {
        val payslip = createPayslip(dsopMonthly = 8_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        val opportunity = result.opportunities.find { it.id == "80c_dsop" }
        assertNull(opportunity, "80C DSOP opportunity should not exist when 80C limit is exceeded")
    }

    @Test
    fun testNpsOpportunityAlwaysPresent() {
        val payslip = createPayslip()
        val result = WealthOptimizationEngine.analyze(payslip)
        val nps = result.opportunities.find { it.id == "80ccd_nps" }
        assertTrue(nps != null, "NPS 80CCD(1B) opportunity must be included")
        assertEquals(50_000.0, nps.unusedAmount, 0.01)
    }

    @Test
    fun testRegimeIsDefaultOld() {
        val result = WealthOptimizationEngine.analyze(createPayslip())
        assertEquals("OLD", result.regimeAssumed)
    }

    @Test
    fun testModernTaxEngineOutputsPopulated() {
        val payslip = createPayslip()
        val result = WealthOptimizationEngine.analyze(payslip)

        assertNotNull(result.fySummary)
        assertNotNull(result.regimeComparison)
        assertNotNull(result.exemptionBreakdown)
        assertNotNull(result.tdsRunway)

        assertEquals("2025-26", result.fySummary?.financialYear)
        assertTrue(result.regimeComparison?.winnerRegime == "OLD" || result.regimeComparison?.winnerRegime == "NEW")
    }

    @Test
    fun testHandlesNullTaxAndSavings() {
        val payslip = createPayslip().copy(taxAndSavings = null)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals("OLD", result.regimeAssumed)
    }
}
