package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.subscription.FeatureGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks [buildRecommendedActions]: each candidate is gated by its own data condition, and — per the
 * approved redesign decision — a candidate whose target [Screen] is already surfaced by a visible
 * Smart Insights card must be dropped, so a paid CTA never appears twice on the same screen.
 */
class RecommendedActionsLogicTest {
    private fun record(dsopSubscription: Double = 10_000.0) =
        LedgerRecordEntity(
            dateStr = "02/2026", year = 2026, monthNum = 2, basicPay = 50_000.0, dearnessAllowance = 0.0,
            militaryServicePay = 0.0, transportAllowance = 0.0, transportAllowanceDa = 0.0, houseRentAllowance = 0.0,
            grossPay = 120_000.0, dsopSubscription = dsopSubscription, incomeTax = 10_000.0, netPay = 100_000.0,
        )

    private fun state(
        anomalies: List<Anomaly> = emptyList(),
        totalPotentialTaxSaving: Double = 0.0,
        dsopGapMonthly: Double = 0.0,
        dsopSubscription: Double = 10_000.0,
    ) = InsightsState(
        currentRecord = record(dsopSubscription = dsopSubscription),
        previousRecord = record(),
        historySorted = emptyList(),
        engineResult = EngineResult(healthScore = 80, anomalies = anomalies, monthlySavingRate = 8.0, taxRatio = 8.0),
        scoreDelta = null,
        optimizationResult =
            OptimizationResult(
                totalPotentialTaxSaving = totalPotentialTaxSaving,
                marginalRatePct = 0.2,
                regimeAssumed = "OLD",
                opportunities = emptyList(),
                dsopGapMonthly = dsopGapMonthly,
                dsopCorpusUpliftAtRetirement = 0.0,
            ),
        momChanges = emptyList(),
        previousMonthLabel = "January",
    )

    private fun insight(actionTarget: Screen?) =
        InsightUiModel(title = "t", explanation = "e", severity = InsightSeverity.INFO, actionTarget = actionTarget)

    @Test
    fun `no data conditions met yields no recommendations`() {
        val actions = buildRecommendedActions(state(dsopSubscription = 0.0), shownInsights = emptyList())
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `tax saving opportunity surfaces a Tax Planner recommendation`() {
        val actions = buildRecommendedActions(state(totalPotentialTaxSaving = 5_000.0, dsopSubscription = 0.0), shownInsights = emptyList())
        assertTrue(actions.any { it.gate == FeatureGate.TAX_PLANNER && it.target == Screen.TaxPlanning })
    }

    @Test
    fun `Tax Planner recommendation is dropped when already shown on a visible insight card`() {
        val actions =
            buildRecommendedActions(
                state(totalPotentialTaxSaving = 5_000.0, dsopSubscription = 0.0),
                shownInsights = listOf(insight(Screen.TaxPlanning)),
            )
        assertTrue(actions.none { it.gate == FeatureGate.TAX_PLANNER })
    }

    @Test
    fun `a representation-eligible anomaly surfaces a Claim Generator recommendation`() {
        val anomaly = Anomaly("SALARY_LOSS", "field", 1000.0, "02/2026", "detail")
        val actions = buildRecommendedActions(state(anomalies = listOf(anomaly), dsopSubscription = 0.0), shownInsights = emptyList())
        assertTrue(actions.any { it.gate == FeatureGate.CLAIM_GENERATOR && it.target == Screen.Representation })
    }

    @Test
    fun `Claim Generator recommendation is dropped when already shown on a visible insight card`() {
        val anomaly = Anomaly("SALARY_LOSS", "field", 1000.0, "02/2026", "detail")
        val actions =
            buildRecommendedActions(
                state(anomalies = listOf(anomaly), dsopSubscription = 0.0),
                shownInsights = listOf(insight(Screen.Representation)),
            )
        assertTrue(actions.none { it.gate == FeatureGate.CLAIM_GENERATOR })
    }

    @Test
    fun `dsop headroom surfaces a DSOP Simulator recommendation`() {
        val actions = buildRecommendedActions(state(dsopGapMonthly = 2_000.0, dsopSubscription = 0.0), shownInsights = emptyList())
        assertTrue(actions.any { it.gate == FeatureGate.DSOP_SIMULATOR && it.target == Screen.RetirementPlanning })
    }

    @Test
    fun `active dsop subscription surfaces a Retirement Calculators recommendation`() {
        val actions = buildRecommendedActions(state(dsopSubscription = 10_000.0), shownInsights = emptyList())
        assertTrue(actions.any { it.gate == FeatureGate.RETIREMENT_CALCULATORS && it.target == Screen.RetirementCalculators })
    }

    @Test
    fun `no CTA target ever appears twice across the returned list`() {
        val anomaly = Anomaly("SALARY_LOSS", "field", 1000.0, "02/2026", "detail")
        val actions =
            buildRecommendedActions(
                state(anomalies = listOf(anomaly), totalPotentialTaxSaving = 5_000.0, dsopGapMonthly = 2_000.0, dsopSubscription = 10_000.0),
                shownInsights = emptyList(),
            )
        assertEquals(actions.map { it.target }.distinct().size, actions.size)
    }
}
