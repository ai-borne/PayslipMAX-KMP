package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.TaxRegime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

private fun RegimeTaxOutcome.requireAvailable(): RegimeTaxDetail =
    when (this) {
        is RegimeTaxOutcome.Available -> detail
        is RegimeTaxOutcome.RulesUnavailable -> fail("Expected resolvable rules for a known FY, got RulesUnavailable($requestedFy)")
    }

private fun RegimeComparisonOutcome.requireAvailable(): RegimeComparisonResult =
    when (this) {
        is RegimeComparisonOutcome.Available -> result
        is RegimeComparisonOutcome.RulesUnavailable -> fail("Expected resolvable rules for a known FY, got RulesUnavailable($requestedFy)")
    }

class DualRegimeEngineTest {
    @Test
    fun testOldRegimeTaxCalculation() {
        val detail =
            DualRegimeEngine.calculateOldRegimeTax(
                grossIncome = 1200000.0,
                deductionsAndExemptions = 200000.0,
            ).requireAvailable()

        assertEquals(950000.0, detail.netTaxableIncome)
        assertEquals(102500.0, detail.baseTax)
        assertEquals(4100.0, detail.cess)
        assertEquals(106600.0, detail.totalTaxPayable)
    }

    @Test
    fun testNewRegimeRebateAndMarginalReliefFy2425() {
        val detailBelow7L =
            DualRegimeEngine.calculateNewRegimeTax(
                grossIncome = 775000.0,
                fy = "2024-25",
            ).requireAvailable()
        assertEquals(700000.0, detailBelow7L.netTaxableIncome)
        assertEquals(0.0, detailBelow7L.totalTaxPayable)

        val detailWithRelief =
            DualRegimeEngine.calculateNewRegimeTax(
                grossIncome = 785000.0,
                fy = "2024-25",
            ).requireAvailable()
        assertEquals(710000.0, detailWithRelief.netTaxableIncome)
        assertEquals(10000.0, detailWithRelief.baseTax)
        assertEquals(10400.0, detailWithRelief.totalTaxPayable)
    }

    @Test
    fun testNewRegimeRebateFy2526() {
        val detail =
            DualRegimeEngine.calculateNewRegimeTax(
                grossIncome = 775000.0,
                fy = "2025-26",
            ).requireAvailable()
        assertEquals(700000.0, detail.netTaxableIncome)
        assertEquals(0.0, detail.totalTaxPayable)
    }

    @Test
    fun testRegimeComparisonAndBreakEven() {
        val result =
            DualRegimeEngine.compareRegimes(
                grossIncome = 1500000.0,
                oldRegimeDeductions = 150000.0,
                fy = "2024-25",
            ).requireAvailable()

        assertTrue(result.winnerRegime == "NEW" || result.winnerRegime == "OLD")
        assertTrue(result.breakEvenDeduction > 0.0)
    }

    @Test
    fun testWinnerRegimeFlipsOnlyAtBreakEven() {
        // Phase 2 (D9): once capped (not uncapped) deductions feed this comparison, the recommendation
        // must flip exactly at breakEvenDeduction -- never before it, on the strength of a coincidentally
        // large but disallowed exemption.
        val gross = 2000000.0
        val fy = "2026-27"
        val breakEven = DualRegimeEngine.compareRegimes(gross, 0.0, fy).requireAvailable().breakEvenDeduction

        val belowBreakEven = DualRegimeEngine.compareRegimes(gross, breakEven - 10000.0, fy).requireAvailable()
        assertEquals("NEW", belowBreakEven.winnerRegime)

        val atOrAboveBreakEven = DualRegimeEngine.compareRegimes(gross, breakEven + 10000.0, fy).requireAvailable()
        assertEquals("OLD", atOrAboveBreakEven.winnerRegime)
    }

    @Test
    fun testCalculateOldRegimeTaxReturnsRulesUnavailableForUnknownFy() {
        val outcome = DualRegimeEngine.calculateOldRegimeTax(grossIncome = 1000000.0, fy = "1999-2000")
        val unavailable = outcome as? RegimeTaxOutcome.RulesUnavailable ?: fail("Expected RulesUnavailable")
        assertEquals("1999-2000", unavailable.requestedFy)
    }

    @Test
    fun testCompareRegimesReturnsRulesUnavailableForUnknownFy() {
        val outcome = DualRegimeEngine.compareRegimes(grossIncome = 1000000.0, oldRegimeDeductions = 0.0, fy = "1999-2000")
        val unavailable = outcome as? RegimeComparisonOutcome.RulesUnavailable ?: fail("Expected RulesUnavailable")
        assertEquals("1999-2000", unavailable.requestedFy)
    }

    @Test
    fun testMarginalRateReturnsNullForUnknownFy() {
        val rate =
            DualRegimeEngine.marginalRate(
                netTaxableIncome = 1000000.0,
                regime = TaxRegime.OLD,
                fy = "1999-2000",
            )
        assertEquals(null, rate)
    }
}
