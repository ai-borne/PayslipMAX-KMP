package com.payslipmax.pdfparser.insights

import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals

class DualRegimeEngineAuditTest {
    @Test
    fun testSampleScreenshotCalculationWith75kStdDeduction() {
        val gross = 3621936.0
        val oldDeductions = 403500.0

        val result =
            DualRegimeEngine.compareRegimes(
                grossIncome = gross,
                oldRegimeDeductions = oldDeductions,
                fy = "2026-27",
            )

        // Verify New Regime Standard Deduction = ₹75,000 applied (Tax: ₹7,84,244)
        assertEquals(784244.0, round(result.newRegime.totalTaxPayable))

        // Verify Old Regime Tax = ₹7,93,552
        assertEquals(793552.0, round(result.oldRegime.totalTaxPayable))

        // Verify Winner Regime is NEW (saving ₹9,308)
        assertEquals("NEW", result.winnerRegime)
        assertEquals(9308.0, round(result.annualSavings))
    }
}
