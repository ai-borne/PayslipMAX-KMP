package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.insights.Anomaly
import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.ui.theme.AppStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WellnessDriversTest {

    private fun makeEngine(
        anomalies: List<Anomaly> = emptyList(),
        savingRate: Double = 0.0,
        healthScore: Int = 100,
    ) = EngineResult(
        healthScore = healthScore,
        anomalies = anomalies,
        monthlySavingRate = savingRate,
        taxRatio = 4.0,
    )

    private fun anomaly(type: String, amount: Double = 0.0) = Anomaly(
        type = type,
        field = "basicPay",
        amount = amount,
        month = "08/2024",
        description = "Test anomaly: $type",
    )

    // ── Anomaly point impacts ────────────────────────────────────────────────

    @Test
    fun testSalaryLossGivesMinus20() {
        val drivers = breakdownWellnessDrivers(makeEngine(anomalies = listOf(anomaly("SALARY_LOSS"))))
        val driver = drivers.first { it.title == AppStrings.wellnessTitleSalaryLoss }
        assertEquals(-20, driver.pointImpact)
        assertNotNull(driver.improvePath)
    }

    @Test
    fun testMissingAllowanceGivesMinus15() {
        val drivers = breakdownWellnessDrivers(makeEngine(anomalies = listOf(anomaly("MISSING_ALLOWANCE"))))
        val driver = drivers.first { it.title.startsWith(AppStrings.wellnessTitleMissingAllowance) }
        assertEquals(-15, driver.pointImpact)
        assertNotNull(driver.improvePath)
    }

    @Test
    fun testDeductionSpikeGivesMinus10() {
        val drivers = breakdownWellnessDrivers(makeEngine(anomalies = listOf(anomaly("DEDUCTION_SPIKE"))))
        val driver = drivers.first { it.title.startsWith(AppStrings.wellnessTitleDeductionSpike) }
        assertEquals(-10, driver.pointImpact)
        assertNotNull(driver.improvePath)
    }

    @Test
    fun testTptaEntitlementGivesMinus10() {
        val drivers = breakdownWellnessDrivers(makeEngine(anomalies = listOf(anomaly("TPTA_ENTITLEMENT"))))
        val driver = drivers.first { it.title == AppStrings.wellnessTitleTpta }
        assertEquals(-10, driver.pointImpact)
        assertNotNull(driver.improvePath)
    }

    @Test
    fun testDsopComplianceCriticalGivesMinus25() {
        val drivers = breakdownWellnessDrivers(makeEngine(anomalies = listOf(anomaly("DSOP_COMPLIANCE", amount = 5000.0))))
        val driver = drivers.first { it.title == AppStrings.wellnessTitleDsop }
        assertEquals(-25, driver.pointImpact)
        assertNotNull(driver.improvePath)
    }

    @Test
    fun testDsopComplianceMinorGivesMinus5() {
        val drivers = breakdownWellnessDrivers(makeEngine(anomalies = listOf(anomaly("DSOP_COMPLIANCE", amount = 0.0))))
        val driver = drivers.first { it.title == AppStrings.wellnessTitleDsop }
        assertEquals(-5, driver.pointImpact)
    }

    // ── Savings rate bonuses ─────────────────────────────────────────────────

    @Test
    fun testGreatSavingsRateGivesPlus15() {
        val drivers = breakdownWellnessDrivers(makeEngine(savingRate = 22.0))
        val driver = drivers.first { it.title.contains(AppStrings.wellnessSavingsRateLabel) }
        assertEquals(+15, driver.pointImpact)
        assertNull(driver.improvePath)
    }

    @Test
    fun testGoodSavingsRateGivesPlus5() {
        val drivers = breakdownWellnessDrivers(makeEngine(savingRate = 15.0))
        val driver = drivers.first { it.title.contains(AppStrings.wellnessSavingsRateLabel) }
        assertEquals(+5, driver.pointImpact)
        assertNotNull(driver.improvePath)
    }

    @Test
    fun testLowSavingsRateGivesZero() {
        val drivers = breakdownWellnessDrivers(makeEngine(savingRate = 5.0))
        val driver = drivers.first { it.title.contains(AppStrings.wellnessSavingsRateLabel) }
        assertEquals(0, driver.pointImpact)
        assertNotNull(driver.improvePath)
    }

    // ── Clean payslip bonus ──────────────────────────────────────────────────

    @Test
    fun testCleanPayslipBonusAppearsWhenNoAnomaliesAndGoodSavings() {
        val drivers = breakdownWellnessDrivers(makeEngine(savingRate = 12.0))
        assertTrue(drivers.any { it.title == AppStrings.wellnessNoIssuesBonus })
        val bonus = drivers.first { it.title == AppStrings.wellnessNoIssuesBonus }
        assertEquals(+10, bonus.pointImpact)
        assertNull(bonus.improvePath)
    }

    @Test
    fun testCleanPayslipBonusAbsentWhenAnomalyPresent() {
        val drivers = breakdownWellnessDrivers(makeEngine(anomalies = listOf(anomaly("SALARY_LOSS")), savingRate = 12.0))
        assertTrue(drivers.none { it.title == AppStrings.wellnessNoIssuesBonus })
    }

    @Test
    fun testCleanPayslipBonusAbsentWhenSavingsRateLow() {
        val drivers = breakdownWellnessDrivers(makeEngine(savingRate = 5.0))
        assertTrue(drivers.none { it.title == AppStrings.wellnessNoIssuesBonus })
    }

    // ── Improve path nullability rules ───────────────────────────────────────

    @Test
    fun testImprovePathNullForGreatSavingsRate() {
        val drivers = breakdownWellnessDrivers(makeEngine(savingRate = 25.0))
        val driver = drivers.first { it.title.contains(AppStrings.wellnessSavingsRateLabel) }
        assertNull(driver.improvePath, "No improve path needed for already-great savings rate")
    }

    @Test
    fun testAllAnomalyDriversHaveImprovePaths() {
        val types = listOf("SALARY_LOSS", "MISSING_ALLOWANCE", "DEDUCTION_SPIKE", "TPTA_ENTITLEMENT")
        types.forEach { type ->
            val drivers = breakdownWellnessDrivers(makeEngine(anomalies = listOf(anomaly(type))))
            val anomalyDrivers = drivers.filter { it.pointImpact < 0 }
            anomalyDrivers.forEach { driver ->
                assertNotNull(driver.improvePath, "Driver for $type must have an improvePath")
            }
        }
    }

    // ── Multi-anomaly accumulation ───────────────────────────────────────────

    @Test
    fun testMultipleAnomaliesAllAppear() {
        val anomalies = listOf(anomaly("SALARY_LOSS"), anomaly("MISSING_ALLOWANCE"))
        val drivers = breakdownWellnessDrivers(makeEngine(anomalies = anomalies))
        val penalties = drivers.filter { it.pointImpact < 0 }
        assertEquals(2, penalties.size)
    }
}
