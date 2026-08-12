package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.tax.TaxYearResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WealthOptimizationEngineAuditTest {
    @Test
    fun testApril2026SinglePayslipTaxPlannerResult() {
        val resolved = TaxYearResolver.resolve(4, 2026)
        assertEquals("2026-27", resolved.financialYear)
        assertEquals("2027-28", resolved.assessmentYear)

        val result =
            WealthOptimizationEngine.buildTaxPlannerResult(
                grossSalary = 3621936.0,
                tdsYtd = 50425.0,
                dsopYtd = 40000.0,
                fieldAllowanceExemption = 253500.0,
                monthsAvailable = 1,
                monthNum = 4,
                year = 2026,
            )

        // Verify FY & AY match resolver
        assertEquals("2026-27", result.financialYear)
        assertEquals("2027-28", result.assessmentYear)
        assertEquals("Rules FY 2026-27 (AY 2027-28) · Offline", result.rulePack.sourceLabel)

        // Verify Confidence is Low (1 month available)
        assertEquals("Low", result.dataCoverage.confidenceLabel)
        assertEquals(1, result.dataCoverage.monthsAvailable)

        // Phase 1 (D1/D2/D6 fix): New Regime now uses the correct FY2025-26+ slabs, not the stale
        // FY2023-24-vintage copy this test used to pin. Verify New Regime tax uses ₹75,000 std
        // deduction -> ₹6,69,844 (was wrongly ₹7,84,244).
        assertEquals(669844.0, kotlin.math.round(result.newRegime.totalTax))

        // Verify Old Regime tax -> ₹7,93,552 (unaffected -- old regime slabs were already correct)
        assertEquals(793552.0, kotlin.math.round(result.oldRegime.totalTax))

        // Verify Winner Regime is NEW
        assertEquals("NEW", result.recommendation.bestRegime)

        // Verify Effective Tax Rate is ~18.5% under New Regime (never hardcoded 8.2%; the pre-Phase-1
        // stale slabs inflated this to ~21.7% -- the corrected FY2025-26+ slabs bring it down).
        assertNotEquals(8.2, result.newRegime.effectiveRate)
        assertTrue(result.newRegime.effectiveRate > 15.0)
    }
}
