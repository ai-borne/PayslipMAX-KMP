package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DualRegimeEngineTest {
    @Test
    fun testOldRegimeTaxCalculation() {
        val detail =
            DualRegimeEngine.calculateOldRegimeTax(
                grossIncome = 1200000.0,
                deductionsAndExemptions = 200000.0,
            )

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
            )
        assertEquals(700000.0, detailBelow7L.netTaxableIncome)
        assertEquals(0.0, detailBelow7L.totalTaxPayable)

        val detailWithRelief =
            DualRegimeEngine.calculateNewRegimeTax(
                grossIncome = 785000.0,
                fy = "2024-25",
            )
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
            )
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
            )

        assertTrue(result.winnerRegime == "NEW" || result.winnerRegime == "OLD")
        assertTrue(result.breakEvenDeduction > 0.0)
    }
}
