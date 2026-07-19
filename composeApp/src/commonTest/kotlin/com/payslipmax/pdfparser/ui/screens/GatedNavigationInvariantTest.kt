package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.Opportunity
import com.payslipmax.pdfparser.insights.OptimizationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-producer regression guard for the Smart Insights paywall bypass: `InsightUiModel` could carry
 * an `actionTarget` pointing at a PRO screen with no `FeatureGate` attached, while `RecommendedActionUiModel`
 * (and `ProFeatureMeta`) always did — so the click site that forgot to check ended up being the free door in.
 *
 * [gateForScreen] (the PRO catalog's reverse lookup) is the single source of truth for which gate
 * protects which screen. Every model that can drive `onNavigateTo(Screen)` must agree with it — this
 * test walks the real output of both current producers rather than re-asserting hardcoded per-type
 * expectations, so it also catches a *third* producer added later without a gate.
 */
class GatedNavigationInvariantTest {
    private fun record(
        dateStr: String = "02/2026",
        dsopSubscription: Double = 10_000.0,
    ) = LedgerRecordEntity(
        dateStr = dateStr, year = 2026, monthNum = 2, basicPay = 50_000.0, dearnessAllowance = 0.0,
        militaryServicePay = 0.0, transportAllowance = 0.0, transportAllowanceDa = 0.0, houseRentAllowance = 0.0,
        grossPay = 120_000.0, dsopSubscription = dsopSubscription, incomeTax = 10_000.0, netPay = 100_000.0,
    )

    private fun state(
        anomalies: List<Anomaly>,
        opportunities: List<Opportunity>,
        dsopSubscription: Double = 10_000.0,
        dsopGapMonthly: Double = 2_000.0,
    ) = InsightsState(
        currentRecord = record(dsopSubscription = dsopSubscription),
        previousRecord = record(dateStr = "01/2026"),
        historySorted = emptyList(),
        engineResult = EngineResult(healthScore = 80, anomalies = anomalies, monthlySavingRate = 8.0, taxRatio = 8.0),
        scoreDelta = null,
        optimizationResult =
            OptimizationResult(
                totalPotentialTaxSaving = opportunities.sumOf { it.estTaxSaved },
                marginalRatePct = 0.2,
                regimeAssumed = "OLD",
                opportunities = opportunities,
                dsopGapMonthly = dsopGapMonthly,
                dsopCorpusUpliftAtRetirement = 0.0,
            ),
        previousMonthLabel = "January",
    )

    @Test
    fun `every Smart Insights card targeting a catalog screen carries the catalog's gate`() {
        val anomalies =
            listOf(
                Anomaly("SALARY_LOSS", "field", 1000.0, "02/2026", "detail"),
                Anomaly("DSOP_COMPLIANCE", "field", 1000.0, "02/2026", "detail"),
                Anomaly("TAX_PROJECTION", "field", 1000.0, "02/2026", "detail"),
            )
        val opportunity = Opportunity(id = "80c", title = "80C Headroom", unusedAmount = 50_000.0, estTaxSaved = 10_000.0, action = "Increase DSOP")
        val insights = buildSmartInsights(state(anomalies, listOf(opportunity)))
        val targeted = insights.filter { it.actionTarget != null }

        assertTrue(targeted.isNotEmpty(), "test setup must actually exercise targeted cards")
        for (card in targeted) {
            assertEquals(
                gateForScreen(card.actionTarget!!),
                card.gate,
                "\"${card.title}\" -> ${card.actionTarget} must carry the PRO catalog's gate for that screen",
            )
        }
    }

    @Test
    fun `every Recommended Action targeting a catalog screen carries the catalog's gate`() {
        val anomaly = Anomaly("SALARY_LOSS", "field", 1000.0, "02/2026", "detail")
        val actions =
            buildRecommendedActions(
                state(anomalies = listOf(anomaly), opportunities = emptyList()),
                shownInsights = emptyList(),
            )

        assertTrue(actions.isNotEmpty(), "test setup must actually exercise recommended actions")
        for (action in actions) {
            assertEquals(
                gateForScreen(action.target),
                action.gate,
                "\"${action.title}\" -> ${action.target} must carry the PRO catalog's gate for that screen",
            )
        }
    }
}
