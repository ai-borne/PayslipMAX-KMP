package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
import com.payslipmax.pdfparser.insights.LeaveBankSummary
import com.payslipmax.pdfparser.insights.Opportunity
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.AT_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.OVER_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.table
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The leave-cap Smart Insight (Leave Bank Phase 5). Leave accrued beyond PCDA's 300-day cap lapses, so the card must show from
 * the cap itself, not only above it, and on the first-statement path too (a user's first upload can be a December). It opens
 * Retirement Calculators behind that screen's catalog gate, hides the matching Recommended Action so the CTA isn't shown twice,
 * and states only the lapse fact (owner decision: no LTC-vs-retirement claim). Kept out of [SmartInsightsBuilderTest], which
 * is at 250 lines: these tests would take it past the 300-line limit.
 */
class LeaveCapInsightTest {
    private fun record(dateStr: String = "02/2026") =
        LedgerRecordEntity(
            dateStr = dateStr, year = 2026, monthNum = 2, basicPay = 50_000.0, dearnessAllowance = 0.0,
            militaryServicePay = 0.0, transportAllowance = 0.0, transportAllowanceDa = 0.0, houseRentAllowance = 0.0,
            grossPay = 120_000.0, dsopSubscription = 10_000.0, incomeTax = 0.0, netPay = 100_000.0,
        )

    private fun leaveBank(balanceDays: Int): LeaveBankSummary =
        assertNotNull(LeaveBankCalculator.summarize(listOf(payslip(2025, 12, table(balanceDays)))))

    private fun state(
        leaveBank: LeaveBankSummary?,
        previousRecord: LedgerRecordEntity? = record(dateStr = "01/2026"),
        anomalies: List<Anomaly> = emptyList(),
        opportunities: List<Opportunity> = emptyList(),
    ) = InsightsState(
        currentRecord = record(),
        previousRecord = previousRecord,
        historySorted = emptyList(),
        engineResult = EngineResult(healthScore = 80, anomalies = anomalies, monthlySavingRate = 8.0, taxRatio = 8.0),
        scoreDelta = null,
        optimizationResult =
            OptimizationResult(
                totalPotentialTaxSaving = opportunities.sumOf { it.estTaxSaved },
                marginalRatePct = 0.2,
                regimeAssumed = "OLD",
                opportunities = opportunities,
                dsopGapMonthly = 0.0,
                dsopCorpusUpliftAtRetirement = 0.0,
            ),
        previousMonthLabel = "January",
        leaveBank = leaveBank,
    )

    private fun leaveCards(
        state: InsightsState,
        enabled: Boolean = true,
    ): List<InsightUiModel> = buildSmartInsights(state, leaveBankEnabled = enabled).filter { it.title == LeaveBankStrings.capInsightTitle }

    @Test
    fun overTheCapTheCardNamesTheStatementThePrintedDaysAndTheLapseFactOnly() {
        val card = leaveCards(state(leaveBank(OVER_CAP_DAYS))).single()

        assertEquals(InsightSeverity.WARNING, card.severity)
        assertEquals("Leave at the 300-day cap", card.title)
        assertEquals("Your Dec 2025 statement shows 301 days, 1 over the cap. Leave accrued beyond 300 days lapses.", card.explanation)
        assertNull(card.amountLabel)
    }

    @Test
    fun atExactlyTheCapTheCardShowsBecauseTheNextAccrualLapses() {
        val card = leaveCards(state(leaveBank(AT_CAP_DAYS))).single()

        assertEquals("Your Dec 2025 statement shows 300 days, the cap. Leave accrued beyond 300 days lapses.", card.explanation)
    }

    @Test
    fun belowTheCapOrWithoutALeaveStatementThereIsNoCard() {
        assertTrue(leaveCards(state(leaveBank(AT_CAP_DAYS - 1))).isEmpty())
        assertTrue(leaveCards(state(leaveBank = null)).isEmpty())
    }

    @Test
    fun theFirstStatementPathKeepsItsCardAndAddsTheLeaveCardAfterIt() {
        val insights = buildSmartInsights(state(leaveBank(OVER_CAP_DAYS), previousRecord = null), leaveBankEnabled = true)

        assertEquals(listOf(InsightsStrings.smartInsightsFirstStatementTitle, LeaveBankStrings.capInsightTitle), insights.map { it.title })
    }

    @Test
    fun theCardOpensRetirementCalculatorsBehindThatScreensCatalogGate() {
        val card = leaveCards(state(leaveBank(OVER_CAP_DAYS))).single()

        assertEquals(Screen.RetirementCalculators, card.actionTarget)
        assertEquals(FeatureGate.RETIREMENT_CALCULATORS, card.gate)
        assertEquals(gateForScreen(Screen.RetirementCalculators), card.gate)
        assertEquals(LeaveBankStrings.capInsightAction, card.actionLabel)
    }

    @Test
    fun theCardSitsAfterAnomaliesAndBeforeTaxOpportunities() {
        val opportunity = Opportunity(id = "80c_dsop", title = "80C Headroom", unusedAmount = 50_000.0, estTaxSaved = 10_000.0, action = "Increase DSOP")
        val anomaly = Anomaly("SALARY_LOSS", "field", 1000.0, "02/2026", "detail")
        val insights =
            buildSmartInsights(
                state(leaveBank(OVER_CAP_DAYS), anomalies = listOf(anomaly), opportunities = listOf(opportunity)),
                leaveBankEnabled = true,
            )

        assertEquals(listOf(anomalyCategoryLabel("SALARY_LOSS"), LeaveBankStrings.capInsightTitle, "80C Headroom"), insights.take(3).map { it.title })
    }

    @Test
    fun whileTheCardShowsTheRetirementCalculatorsRecommendedActionIsNotShownTwice() {
        val atCap = state(leaveBank(OVER_CAP_DAYS))
        val belowCap = state(leaveBank(AT_CAP_DAYS - 1))

        val withCard = buildRecommendedActions(atCap, buildSmartInsights(atCap, leaveBankEnabled = true)).map { it.target }
        val withoutCard = buildRecommendedActions(belowCap, buildSmartInsights(belowCap, leaveBankEnabled = true)).map { it.target }

        assertTrue(Screen.RetirementCalculators !in withCard)
        assertTrue(Screen.RetirementCalculators in withoutCard)
    }

    @Test
    fun withTheKillSwitchOffThereIsNoCardEvenOverTheCap() {
        assertTrue(leaveCards(state(leaveBank(OVER_CAP_DAYS)), enabled = false).isEmpty())
        assertTrue(leaveCards(state(leaveBank(OVER_CAP_DAYS), previousRecord = null), enabled = false).isEmpty())
    }
}
