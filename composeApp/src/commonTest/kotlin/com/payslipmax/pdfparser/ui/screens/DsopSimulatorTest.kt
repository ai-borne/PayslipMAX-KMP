package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.ProjectionMath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DsopSimulatorTest {
    @Test
    fun testAnnualContributionUnderLimit() {
        val monthly = 40000f
        val annual = monthly * 12
        val exceedsLimit = annual > 500000f
        assertFalse(exceedsLimit, "₹40k monthly contribution (₹4.8L annual) should not exceed ₹5L limit")
    }

    @Test
    fun testAnnualContributionOverLimit() {
        val monthly = 42000f
        val annual = monthly * 12
        val exceedsLimit = annual > 500000f
        assertTrue(exceedsLimit, "₹42k monthly contribution (₹5.04L annual) should exceed ₹5L limit")
    }

    @Test
    fun testProjectionMathIntegration() {
        val initialBalance = 100000.0
        val monthly = 10000.0

        val result5 = ProjectionMath.calculateProjection(initialBalance, monthly, 5)
        assertTrue(result5.projectedBalance > initialBalance, "5-year projected balance should be greater than initial balance")
        assertEquals(5, result5.years, "Result should be for 5 years")

        val result10 = ProjectionMath.calculateProjection(initialBalance, monthly, 10)
        assertTrue(result10.projectedBalance > result5.projectedBalance, "10-year projected balance should be greater than 5-year balance")

        val result15 = ProjectionMath.calculateProjection(initialBalance, monthly, 15)
        assertTrue(result15.projectedBalance > result10.projectedBalance, "15-year projected balance should be greater than 10-year balance")
    }
}
