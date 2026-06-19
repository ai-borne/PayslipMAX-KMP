package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.insights.Anomaly
import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.insights.Opportunity
import com.ssbmax.pdfparser.insights.OptimizationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InsightsHeroLogicTest {

    private fun makeEngineResult(anomalies: List<Anomaly> = emptyList()) = EngineResult(
        healthScore = 80,
        anomalies = anomalies,
        monthlySavingRate = 15.0,
        taxRatio = 8.0,
    )

    private fun makeOptimizationResult(
        taxSaving: Double = 20000.0,
        action: String = "Increase DSOP by ₹1000/month",
    ) = OptimizationResult(
        totalPotentialTaxSaving = taxSaving,
        marginalRatePct = 0.20,
        regimeAssumed = "OLD",
        opportunities = listOf(
            Opportunity(
                id = "80c_dsop",
                title = "80C: Increase DSOP",
                unusedAmount = 100000.0,
                estTaxSaved = taxSaving,
                action = action,
            ),
        ),
        dsopGapMonthly = 1000.0,
        dsopCorpusUpliftAtRetirement = 500000.0,
    )

    private fun anomaly(type: String, amount: Double = 5000.0) = Anomaly(
        type = type,
        field = "basicPay",
        amount = amount,
        month = "06/2024",
        description = "Test: $type",
    )

    // ── Recovery branch selection ──────────────────────────────────────────

    @Test
    fun testMissingAllowanceTriggerRecoveryBranch() {
        val result = selectHeroBranch(
            makeEngineResult(listOf(anomaly("MISSING_ALLOWANCE", 3000.0))),
            makeOptimizationResult(),
        )
        assertIs<HeroBranch.Recovery>(result)
        assertEquals(3000.0, result.totalRecoverable)
    }

    @Test
    fun testSalaryLossTriggerRecoveryBranch() {
        val result = selectHeroBranch(
            makeEngineResult(listOf(anomaly("SALARY_LOSS", 15000.0))),
            makeOptimizationResult(),
        )
        assertIs<HeroBranch.Recovery>(result)
        assertEquals(15000.0, result.totalRecoverable)
    }

    @Test
    fun testTptaEntitlementTriggerRecoveryBranch() {
        val result = selectHeroBranch(
            makeEngineResult(listOf(anomaly("TPTA_ENTITLEMENT", 2500.0))),
            makeOptimizationResult(),
        )
        assertIs<HeroBranch.Recovery>(result)
        assertEquals(2500.0, result.totalRecoverable)
    }

    // ── Wealth branch selection ────────────────────────────────────────────

    @Test
    fun testEmptyAnomaliesSelectsWealthBranch() {
        val result = selectHeroBranch(makeEngineResult(), makeOptimizationResult(20000.0))
        assertIs<HeroBranch.Wealth>(result)
        assertEquals(20000.0, result.totalPotentialTaxSaving)
    }

    @Test
    fun testNonRecoverableAnomalySelectsWealthBranch() {
        val result = selectHeroBranch(
            makeEngineResult(listOf(anomaly("DEDUCTION_SPIKE"), anomaly("DSOP_COMPLIANCE"))),
            makeOptimizationResult(12000.0),
        )
        assertIs<HeroBranch.Wealth>(result)
        assertEquals(12000.0, result.totalPotentialTaxSaving)
    }

    // ── Multi-anomaly recovery sum ─────────────────────────────────────────

    @Test
    fun testMultipleRecoverableAnomaliesSumAmount() {
        val result = selectHeroBranch(
            makeEngineResult(listOf(
                anomaly("MISSING_ALLOWANCE", 3000.0),
                anomaly("SALARY_LOSS", 7000.0),
            )),
            makeOptimizationResult(),
        )
        assertIs<HeroBranch.Recovery>(result)
        assertEquals(10000.0, result.totalRecoverable)
        assertEquals(2, result.anomalies.size)
    }

    // ── Recovery primaryCause ──────────────────────────────────────────────

    @Test
    fun testRecoveryPrimaryCauseIsFirstAnomalyDescription() {
        val result = selectHeroBranch(
            makeEngineResult(listOf(
                anomaly("MISSING_ALLOWANCE", 3000.0),
                anomaly("SALARY_LOSS", 2000.0),
            )),
            makeOptimizationResult(),
        )
        assertIs<HeroBranch.Recovery>(result)
        assertEquals("Test: MISSING_ALLOWANCE", result.primaryCause)
    }

    // ── Wealth topOpportunityAction ────────────────────────────────────────

    @Test
    fun testWealthTopOpportunityIsFirstOpportunityAction() {
        val action = "Increase DSOP by ₹2000/month"
        val result = selectHeroBranch(
            makeEngineResult(),
            makeOptimizationResult(action = action),
        )
        assertIs<HeroBranch.Wealth>(result)
        assertEquals(action, result.topOpportunityAction)
    }

    @Test
    fun testWealthWithNoOpportunitiesHasEmptyAction() {
        val result = selectHeroBranch(
            makeEngineResult(),
            OptimizationResult(
                totalPotentialTaxSaving = 0.0,
                marginalRatePct = 0.0,
                regimeAssumed = "OLD",
                opportunities = emptyList(),
                dsopGapMonthly = 0.0,
                dsopCorpusUpliftAtRetirement = 0.0,
            ),
        )
        assertIs<HeroBranch.Wealth>(result)
        assertEquals("", result.topOpportunityAction)
    }

    // ── Mixed anomaly set: recoverable wins ───────────────────────────────

    @Test
    fun testRecoveryWhenMixedRecoverableAndNonRecoverable() {
        val result = selectHeroBranch(
            makeEngineResult(listOf(
                anomaly("DSOP_COMPLIANCE"),
                anomaly("MISSING_ALLOWANCE", 4000.0),
            )),
            makeOptimizationResult(),
        )
        assertIs<HeroBranch.Recovery>(result)
        assertEquals(4000.0, result.totalRecoverable)
        assertEquals(1, result.anomalies.size)
    }

    // ── Recovery anomalies list only has recoverable types ────────────────

    @Test
    fun testRecoveryAnomaliesListContainsOnlyRecoverableTypes() {
        val result = selectHeroBranch(
            makeEngineResult(listOf(
                anomaly("DEDUCTION_SPIKE", 1000.0),
                anomaly("TPTA_ENTITLEMENT", 5000.0),
                anomaly("DSOP_COMPLIANCE", 2000.0),
            )),
            makeOptimizationResult(),
        )
        assertIs<HeroBranch.Recovery>(result)
        assertTrue(result.anomalies.all { it.type == "TPTA_ENTITLEMENT" })
    }
}
