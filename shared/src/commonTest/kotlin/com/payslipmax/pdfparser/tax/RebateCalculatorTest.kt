package com.payslipmax.pdfparser.tax

import kotlin.test.Test
import kotlin.test.assertEquals

/** S87A rebate: old regime is a hard cliff at 5L/Rs 12,500; new regime has marginal relief above its threshold. */
class RebateCalculatorTest {
    @Test
    fun oldRegimeFullyRebatedAtThreshold() {
        // netTaxable=500000 -> rawTax=12500 under the from-2017-18 5% band, exactly absorbed by the cap.
        val result = RebateCalculator.applyOldRegimeRebate(500000.0, 12500.0, 500000.0, 12500.0)
        assertEquals(0.0, result)
    }

    @Test
    fun oldRegimeNoRebateJustAboveThreshold() {
        val result = RebateCalculator.applyOldRegimeRebate(500001.0, 12500.05, 500000.0, 12500.0)
        assertEquals(12500.05, result, 0.001)
    }

    @Test
    fun newRegimeFullyRebatedAtOrBelowThreshold() {
        // FY2024-25: threshold 7L, cap 25,000. netTaxable=700000 -> rawTax=20000, fully absorbed.
        val result = RebateCalculator.applyNewRegimeRebate(700000.0, 20000.0, 700000.0, 25000.0)
        assertEquals(0.0, result)
    }

    @Test
    fun newRegimeMarginalReliefJustAboveThreshold() {
        // netTaxable=710000 (FY2024-25): rawTax=21000, but income only exceeds the 7L threshold by 10,000
        // -> marginal relief caps tax at 10,000 rather than the full 21,000.
        val result = RebateCalculator.applyNewRegimeRebate(710000.0, 21000.0, 700000.0, 25000.0)
        assertEquals(10000.0, result)
    }

    @Test
    fun newRegimeNoReliefFarAboveThreshold() {
        // Well past the marginal-relief zone: rawTax is already below the income-above-threshold cap.
        val result = RebateCalculator.applyNewRegimeRebate(2000000.0, 290000.0, 700000.0, 25000.0)
        assertEquals(290000.0, result)
    }

    @Test
    fun newRegimeFy2025_26HigherThresholdAndCap() {
        // FY2025-26: threshold 12L, cap 60,000. netTaxable=1200000 -> rawTax=60000, fully absorbed.
        val result = RebateCalculator.applyNewRegimeRebate(1200000.0, 60000.0, 1200000.0, 60000.0)
        assertEquals(0.0, result)
    }
}
