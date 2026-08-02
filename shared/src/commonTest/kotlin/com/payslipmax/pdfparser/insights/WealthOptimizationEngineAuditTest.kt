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

        // Verify New Regime tax uses ₹75,000 std deduction -> ₹7,84,244
        assertEquals(784244.0, kotlin.math.round(result.newRegime.totalTax))

        // Verify Old Regime tax -> ₹7,93,552
        assertEquals(793552.0, kotlin.math.round(result.oldRegime.totalTax))

        // Verify Winner Regime is NEW
        assertEquals("NEW", result.recommendation.bestRegime)

        // Verify Effective Tax Rate is ~21.7% under New Regime (never hardcoded 8.2%)
        assertNotEquals(8.2, result.newRegime.effectiveRate)
        assertTrue(result.newRegime.effectiveRate > 20.0)
    }
}
