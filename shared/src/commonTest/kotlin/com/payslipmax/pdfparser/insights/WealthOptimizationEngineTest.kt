package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WealthOptimizationEngineTest {
    private fun createPayslip(
        dsopMonthly: Double = 8_000.0,
        agifMonthly: Double = 5_000.0,
        grossPay: Double = 180_000.0,
        netTaxableIncome: Double = 800_000.0,
        dsopClosingBalance: Double = 500_000.0,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "test.pdf",
            year = 2025,
            monthNum = 4,
            monthName = "April",
            dateStr = "04/2025",
            officer = Officer("Test Officer", "12345", "XXXXX0000X"),
            earnings = Earnings(basicPay = 105_300.0),
            deductions = Deductions(dsopSubscription = dsopMonthly, agif = agifMonthly),
            ledgerBalances = LedgerBalances(),
            summary =
                PayslipSummary(
                    grossPay = grossPay,
                    totalDeductions = dsopMonthly + agifMonthly,
                    netRemittance = grossPay - dsopMonthly - agifMonthly,
                ),
            taxAndSavings =
                TaxAndSavings(
                    netTaxableIncome = netTaxableIncome,
                    dsopFund = DsopFund(closingBalance = dsopClosingBalance),
                ),
        )

    // ──────────────────────────────────────────────────────────────────────────
    // Marginal rate derivation
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun testMarginalRateZeroBelowExemptionLimit() {
        assertEquals(0.0, WealthOptimizationEngine.deriveMarginalRate(200_000.0))
    }

    @Test
    fun testMarginalRateFivePercent() {
        assertEquals(0.05, WealthOptimizationEngine.deriveMarginalRate(400_000.0))
    }

    @Test
    fun testMarginalRateTwentyPercent() {
        assertEquals(0.20, WealthOptimizationEngine.deriveMarginalRate(700_000.0))
    }

    @Test
    fun testMarginalRateThirtyPercent() {
        assertEquals(0.30, WealthOptimizationEngine.deriveMarginalRate(1_200_000.0))
    }

    @Test
    fun testMarginalRateAtExactSlabBoundary() {
        // Exactly 5L → lower boundary of 20% band
        assertEquals(0.20, WealthOptimizationEngine.deriveMarginalRate(500_001.0))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 80C headroom
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun test80CHeadroomPositiveWhenUnused() {
        // dsop=2000, agif=5000 → annual = 84000 → headroom = 66000
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        val opportunity = result.opportunities.find { it.id == "80c_dsop" }
        assertTrue(opportunity != null, "80C DSOP opportunity should exist when headroom is positive")
        assertEquals(66_000.0, opportunity.unusedAmount, 0.01)
    }

    @Test
    fun test80CHeadroomZeroWhenExceeded() {
        // dsop=8000, agif=5000 → annual = 156000 > 150000 → headroom = 0
        val payslip = createPayslip(dsopMonthly = 8_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        val opportunity = result.opportunities.find { it.id == "80c_dsop" }
        assertNull(opportunity, "80C DSOP opportunity should not exist when 80C limit is already exceeded")
    }

    @Test
    fun test80CHeadroomExactlyAtLimit() {
        // dsop=7500, agif=5000 → annual = 150000 → headroom = 0
        val payslip = createPayslip(dsopMonthly = 7_500.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        val opportunity = result.opportunities.find { it.id == "80c_dsop" }
        assertNull(opportunity, "No opportunity when annual contributions exactly hit 80C limit")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // NPS 80CCD(1B) opportunity
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun testNpsOpportunityAlwaysPresent() {
        val payslip = createPayslip()
        val result = WealthOptimizationEngine.analyze(payslip)
        val nps = result.opportunities.find { it.id == "80ccd_nps" }
        assertTrue(nps != null, "NPS 80CCD(1B) opportunity must always be included")
        assertEquals(50_000.0, nps.unusedAmount, 0.01)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tax saving estimates
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun testTaxSavedFromBothOpportunities() {
        // dsop=2000, agif=5000 → headroom=66000; netTaxable=700K → rate=20%
        // 80C tax saved = 66000 * 0.20 = 13200
        // NPS tax saved = 50000 * 0.20 = 10000 → total = 23200
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 5_000.0, netTaxableIncome = 700_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals(23_200.0, result.totalPotentialTaxSaving, 0.01)
    }

    @Test
    fun testTaxSavedOnlyNpsWhenNo80CRoom() {
        // dsop=8000, agif=5000 → headroom=0; netTaxable=700K → rate=20%
        // Only NPS: 50000 * 0.20 = 10000
        val payslip = createPayslip(dsopMonthly = 8_000.0, agifMonthly = 5_000.0, netTaxableIncome = 700_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals(10_000.0, result.totalPotentialTaxSaving, 0.01)
    }

    @Test
    fun testTaxSavedZeroWhenIncomeBelowExemption() {
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 5_000.0, netTaxableIncome = 200_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals(0.0, result.totalPotentialTaxSaving, 0.01)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Regime label
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun testRegimeIsAlwaysOld() {
        val result = WealthOptimizationEngine.analyze(createPayslip())
        assertEquals("OLD", result.regimeAssumed)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DSOP gap calculation
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun testDsopGapPositiveWhenHeadroomExists() {
        // dsop=2000, agif=5000 → headroom=66000 → monthly gap = min(5500, 34000) = 5500
        // 20% gross = 36000; current dsop = 2000; room to 20% = 34000
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 5_000.0, grossPay = 180_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals(5_500.0, result.dsopGapMonthly, 0.01)
    }

    @Test
    fun testDsopGapZeroWhenNo80CRoom() {
        val payslip = createPayslip(dsopMonthly = 8_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals(0.0, result.dsopGapMonthly)
    }

    @Test
    fun testDsopGapCappedBy20PercentGross() {
        // dsop=2000, agif=0 → headroom = 150000 - 24000 = 126000, monthly fill = 10500
        // But 20% of gross = 50000*0.20 = 10000; room to 20% = 10000-2000 = 8000
        // gap = min(10500, 8000) = 8000
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 0.0, grossPay = 50_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals(8_000.0, result.dsopGapMonthly, 0.01)
    }

    @Test
    fun testDsopGapZeroWhenAlreadyAt20PctGross() {
        // dsop=10000, 20% of 50000 = 10000 → no gap
        val payslip = createPayslip(dsopMonthly = 10_000.0, agifMonthly = 0.0, grossPay = 50_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals(0.0, result.dsopGapMonthly)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Corpus uplift
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun testCorpusUpliftPositiveWhenGapExists() {
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 5_000.0, dsopClosingBalance = 500_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertTrue(result.dsopCorpusUpliftAtRetirement > 0.0, "Corpus uplift must be positive when monthly gap > 0")
    }

    @Test
    fun testCorpusUpliftZeroWhenNoGap() {
        val payslip = createPayslip(dsopMonthly = 8_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals(0.0, result.dsopCorpusUpliftAtRetirement)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Null taxAndSavings (graceful degradation)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun testHandlesNullTaxAndSavings() {
        val payslip = createPayslip().copy(taxAndSavings = null)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals("OLD", result.regimeAssumed)
        assertEquals(0.0, result.marginalRatePct)
    }
}
