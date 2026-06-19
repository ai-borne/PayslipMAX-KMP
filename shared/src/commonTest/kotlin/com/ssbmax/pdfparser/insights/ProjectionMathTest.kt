package com.ssbmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectionMathTest {
    @Test
    fun testZeroEverythingStaysZero() {
        val result = ProjectionMath.calculateProjection(0.0, 0.0, 10)
        assertEquals(0.0, result.projectedBalance)
        assertEquals(0.0, result.totalContributions)
        assertEquals(0.0, result.totalInterest)
        assertEquals(10, result.years)
    }

    @Test
    fun testZeroSubscriptionBalanceGrowsWithInterest() {
        val result = ProjectionMath.calculateProjection(100_000.0, 0.0, 1)
        assertTrue(result.projectedBalance > 100_000.0, "Balance should grow with interest")
        assertEquals(0.0, result.totalContributions)
        assertEquals(1, result.years)
    }

    @Test
    fun testContributionsAccumulateWithZeroRate() {
        val result = ProjectionMath.calculateProjection(0.0, 12_000.0, 1, annualRate = 0.0)
        assertEquals(144_000.0, result.totalContributions, 0.01)
        assertEquals(144_000.0, result.projectedBalance, 0.01)
        assertEquals(0.0, result.totalInterest, 0.01)
    }

    @Test
    fun testInterestIsPositiveWithNonZeroRate() {
        val result = ProjectionMath.calculateProjection(0.0, 10_000.0, 5)
        assertTrue(result.totalInterest > 0.0, "Interest should accumulate over time")
        assertTrue(result.projectedBalance > result.totalContributions)
    }

    @Test
    fun testHigherSubscriptionYieldsHigherBalance() {
        val low = ProjectionMath.calculateProjection(0.0, 5_000.0, 10)
        val high = ProjectionMath.calculateProjection(0.0, 10_000.0, 10)
        assertTrue(high.projectedBalance > low.projectedBalance)
    }
}
