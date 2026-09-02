package com.payslipmax.pdfparser.insights

import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DualRegimeEngineAuditTest {
    @Test
    fun testSampleScreenshotCalculationWith75kStdDeduction() {
        val gross = 3621936.0
        val oldDeductions = 403500.0

        val outcome =
            DualRegimeEngine.compareRegimes(
                grossIncome = gross,
                oldRegimeDeductions = oldDeductions,
                fy = "2026-27",
            )
        val result = (outcome as? RegimeComparisonOutcome.Available)?.result ?: fail("Expected resolvable rules for FY 2026-27")

        // Phase 1 (D1/D2/D6 fix): New Regime now uses the correct FY2025-26+ slabs (4/8/12/16/20/24L),
        // not the stale FY2023-24-vintage (3/7/10/12/15L) hardcoded copy this test used to pin.
        // Verify New Regime Standard Deduction = ₹75,000 applied (Tax: ₹6,69,844, was wrongly ₹7,84,244)
        assertEquals(669844.0, round(result.newRegime.totalTaxPayable))

        // Verify Old Regime Tax = ₹7,93,552 (unaffected -- old regime slabs were already correct post-FY2017-18)
        assertEquals(793552.0, round(result.oldRegime.totalTaxPayable))

        // Verify Winner Regime is NEW (saving ₹1,23,708, was wrongly computed as ₹9,308)
        assertEquals("NEW", result.winnerRegime)
        assertEquals(123708.0, round(result.annualSavings))
    }
}
