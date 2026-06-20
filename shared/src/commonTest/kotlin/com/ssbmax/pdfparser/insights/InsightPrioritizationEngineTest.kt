package com.ssbmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightPrioritizationEngineTest {
    @Test
    fun testPrioritizationScoringAndFiltering() {
        val anomalies = listOf(
            Anomaly("DSOP_MILESTONE", "dsop", 45000.0, "03/2026", "DSOP Interest"), // low priority
            Anomaly("SALARY_LOSS", "netPay", 25000.0, "03/2026", "Salary Loss"), // high priority
            Anomaly("RENT_RECOVERY_RISK", "licenseFee", 12000.0, "03/2026", "Rent recovery") // high priority
        )

        val prioritized = InsightPrioritizationEngine.prioritize(anomalies)

        // DSOP_MILESTONE score: (6*0.35)+(10*0.2)+(10*0.15)+(5*0.2)+(6*0.1) = 2.1+2.0+1.5+1.0+0.6 = 7.2
        // SALARY_LOSS score: (9*0.35)+(10*0.2)+(10*0.15)+(8*0.2)+(10*0.1) = 3.15+2.0+1.5+1.6+1.0 = 9.25
        // RENT_RECOVERY_RISK score: (9*0.35)+(8*0.2)+(10*0.15)+(10*0.2)+(10*0.1) = 3.15+1.6+1.5+2.0+1.0 = 9.25

        // All are >= 7.0.
        // Sorted descending by score: SALARY_LOSS (9.25), RENT_RECOVERY_RISK (9.25), DSOP_MILESTONE (7.2).
        // SALARY_LOSS vs RENT_RECOVERY_RISK score tie-breaker: SALARY_LOSS amount (25k) > RENT_RECOVERY_RISK amount (12k).
        // Thus, order should be: SALARY_LOSS, RENT_RECOVERY_RISK, DSOP_MILESTONE.
        assertEquals(3, prioritized.size)
        assertEquals("SALARY_LOSS", prioritized[0].type)
        assertEquals("RENT_RECOVERY_RISK", prioritized[1].type)
        assertEquals("DSOP_MILESTONE", prioritized[2].type)
    }

    @Test
    fun testNoveltyScoringDropsScore() {
        // Rent recovery risk with NOV=10 has score 9.25 (>= 7.0)
        // Rent recovery risk with NOV=2 (already present last month) has score:
        // (9*0.35)+(8*0.2)+(2*0.15)+(10*0.2)+(10*0.1) = 3.15+1.6+0.3+2.0+1.0 = 8.05
        // Still >= 7.0, but prioritized lower than a new missing allowance (score: (8*0.35)+(9*0.2)+(10*0.15)+(8*0.2)+(9*0.1) = 2.8+1.8+1.5+1.6+0.9 = 8.6)
        val anomalies = listOf(
            Anomaly("RENT_RECOVERY_RISK", "licenseFee", 12000.0, "03/2026", "Rent recovery"),
            Anomaly("MISSING_ALLOWANCE", "transportAllowance", 3600.0, "03/2026", "Missing transport")
        )
        val prevAnomalies = listOf(
            Anomaly("RENT_RECOVERY_RISK", "licenseFee", 12000.0, "02/2026", "Rent recovery")
        )

        val prioritized = InsightPrioritizationEngine.prioritize(anomalies, prevAnomalies)

        assertEquals(2, prioritized.size)
        assertEquals("MISSING_ALLOWANCE", prioritized[0].type) // new allowance comes first due to novelty
        assertEquals("RENT_RECOVERY_RISK", prioritized[1].type)
    }

    @Test
    fun testMaxFourOutputLimit() {
        val anomalies = listOf(
            Anomaly("SALARY_LOSS", "netPay", 1.0, "03/2026", "Loss 1"),
            Anomaly("SALARY_LOSS", "netPay", 2.0, "03/2026", "Loss 2"),
            Anomaly("SALARY_LOSS", "netPay", 3.0, "03/2026", "Loss 3"),
            Anomaly("SALARY_LOSS", "netPay", 4.0, "03/2026", "Loss 4"),
            Anomaly("SALARY_LOSS", "netPay", 5.0, "03/2026", "Loss 5")
        )

        val prioritized = InsightPrioritizationEngine.prioritize(anomalies)
        assertEquals(4, prioritized.size)
    }
}
