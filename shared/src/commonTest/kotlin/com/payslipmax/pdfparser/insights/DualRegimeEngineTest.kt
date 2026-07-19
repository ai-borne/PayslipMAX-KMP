package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DualRegimeEngineTest {
    @Test
    fun testOldRegimeTaxCalculation() {
        // Gross 12L, Deductions 2L (80C + 80D + HRA)
        // Std Ded = 50k. Net Taxable = 12L - 50k - 2L = 9,50,000.
        // Tax: 0-2.5L @ 0 = 0
        // 2.5L-5L @ 5% = 12,500
        // 5L-9.5L @ 20% = 90,000
        // Total Base Tax = 1,02,500. Cess @ 4% = 4,100. Total Tax = 1,06,600.
        val detail = DualRegimeEngine.calculateOldRegimeTax(grossIncome = 1200000.0, deductionsAndExemptions = 200000.0)

        assertEquals(950000.0, detail.netTaxableIncome)
        assertEquals(102500.0, detail.baseTax)
        assertEquals(4100.0, detail.cess)
        assertEquals(106600.0, detail.totalTaxPayable)
    }

    @Test
    fun testNewRegimeRebateAndMarginalReliefFy2425() {
        // Net Taxable 7,00,000 -> Tax before rebate = 3L-6L @ 5% (15k) + 6L-7L @ 10% (10k) = 25,000.
        // Sec 87A rebate = 25,000. Total Tax = 0.
        val detailBelow7L = DualRegimeEngine.calculateNewRegimeTax(grossIncome = 750000.0, fy = "2024-25") // 7.5L gross - 50k std ded = 7L
        assertEquals(700000.0, detailBelow7L.netTaxableIncome)
        assertEquals(0.0, detailBelow7L.totalTaxPayable)

        // Net Taxable 7,10,000 -> Base Tax without marginal relief = 15k (3-6L) + 10k (6-7L) + 1k (7-7.1L @ 15%) = 26,000.
        // Excess income over 7L = 10,000.
        // Marginal relief caps tax at excess income (10,000). Tax = 10,000 + 4% cess = 10,400.
        val detailWithRelief = DualRegimeEngine.calculateNewRegimeTax(grossIncome = 760000.0, fy = "2024-25") // 7.6L gross - 50k std ded = 7.1L
        assertEquals(710000.0, detailWithRelief.netTaxableIncome)
        assertEquals(10000.0, detailWithRelief.baseTax)
        assertEquals(10400.0, detailWithRelief.totalTaxPayable)
    }

    @Test
    fun testNewRegimeRebateFy2526() {
        // FY 25-26: Std Ded 75k. Gross 12.75L -> Net Taxable 12L.
        // 0-4L @ 0 = 0
        // 4L-8L @ 5% = 20,000
        // 8L-12L @ 10% = 40,000
        // Base Tax = 60,000. Sec 87A rebate = 60,000. Total Tax = 0.
        val detail = DualRegimeEngine.calculateNewRegimeTax(grossIncome = 1275000.0, fy = "2025-26")
        assertEquals(1200000.0, detail.netTaxableIncome)
        assertEquals(0.0, detail.totalTaxPayable)
    }

    @Test
    fun testRegimeComparisonAndBreakEven() {
        // Gross 15L. Current Old Deductions = 1.5L.
        val result = DualRegimeEngine.compareRegimes(grossIncome = 1500000.0, oldRegimeDeductions = 150000.0, fy = "2024-25")

        assertTrue(result.winnerRegime == "NEW" || result.winnerRegime == "OLD")
        assertTrue(result.breakEvenDeduction > 0.0)
    }
}
