package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.insights.Opportunity
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks the presentation logic that will drive the redesigned SmartInsightsSection. The screen isn't
 * wired to this builder yet (Phase 4), so these are the only tests protecting its behavior for now.
 */
class SmartInsightsBuilderTest {
    /** No deduction data and identical earnings component values by default, so [buildFreeFindingCards]
     *  stays empty and doesn't leak an extra trailing card into tests that aren't about free findings. */
    private fun record(
        dateStr: String = "02/2026",
        netPay: Double = 100_000.0,
        dsopSubscription: Double = 0.0,
        incomeTax: Double = 0.0,
        basicPay: Double = 50_000.0,
        dearnessAllowance: Double = 0.0,
    ) = LedgerRecordEntity(
        dateStr = dateStr, year = 2026, monthNum = 2, basicPay = basicPay, dearnessAllowance = dearnessAllowance,
        militaryServicePay = 0.0, transportAllowance = 0.0, transportAllowanceDa = 0.0, houseRentAllowance = 0.0,
        grossPay = 120_000.0, dsopSubscription = dsopSubscription, incomeTax = incomeTax, netPay = netPay,
    )

    private fun anomaly(
        type: String,
        amount: Double = 1000.0,
    ) = Anomaly(type, "field", amount, "02/2026", "$type detail")

    private fun state(
        anomalies: List<Anomaly> = emptyList(),
        opportunities: List<Opportunity> = emptyList(),
        previousRecord: LedgerRecordEntity? = record(dateStr = "01/2026"),
        currentRecord: LedgerRecordEntity = record(),
    ) = InsightsState(
        currentRecord = currentRecord,
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
    )

    @Test
    fun `no history falls back to a single INFO first-statement card`() {
        val insights = buildSmartInsights(state(previousRecord = null))
        assertEquals(1, insights.size)
        assertEquals(InsightSeverity.INFO, insights.single().severity)
        assertEquals(InsightsStrings.smartInsightsFirstStatementTitle, insights.single().title)
    }

    @Test
    fun `anomaly cards are ordered by the prioritization engine score highest first`() {
        // SALARY_LOSS (importance 9) must outrank DSOP_MILESTONE (importance 6), regardless of input order.
        val insights = buildSmartInsights(state(anomalies = listOf(anomaly("DSOP_MILESTONE"), anomaly("SALARY_LOSS"))))
        assertEquals(
            listOf("SALARY_LOSS", "DSOP_MILESTONE"),
            insights.map { InsightPrioritizationOrderProbe.typeOf(it) },
        )
    }

    @Test
    fun `each anomaly carries its Phase-1 SSOT severity not a re-derived one`() {
        val insights = buildSmartInsights(state(anomalies = listOf(anomaly("SALARY_LOSS"), anomaly("DSOP_MILESTONE"))))
        val bySeverity = insights.associate { InsightPrioritizationOrderProbe.typeOf(it) to it.severity }
        assertEquals(InsightSeverity.IMPORTANT, bySeverity.getValue("SALARY_LOSS"))
        assertEquals(InsightSeverity.INFO, bySeverity.getValue("DSOP_MILESTONE"))
    }

    @Test
    fun `an anomaly type with no mapped remediation flow gets no action target`() {
        val card = buildSmartInsights(state(anomalies = listOf(anomaly("DSOP_MILESTONE")))).single()
        assertNull(card.actionTarget)
    }

    @Test
    fun `an anomaly with an existing representation-draft flow gets an action target and label`() {
        val card = buildSmartInsights(state(anomalies = listOf(anomaly("SALARY_LOSS")))).single()
        assertEquals(Screen.Representation, card.actionTarget)
        assertEquals(InsightsStrings.wellnessImproveSalaryLoss, card.actionLabel)
    }

    @Test
    fun `an anomaly the engine never emits - eg rent-recovery suppressed by active HRA - produces no card`() {
        // The auditor itself is responsible for the licenseFee/HRA gating; the builder is a pure
        // pass-through, so an anomalies list without RENT_RECOVERY_RISK must not synthesize one.
        val insights = buildSmartInsights(state(anomalies = listOf(anomaly("SALARY_LOSS"))))
        assertTrue(insights.none { InsightPrioritizationOrderProbe.typeOf(it) == "RENT_RECOVERY_RISK" })
    }

    @Test
    fun `wealth-optimization opportunities become OPPORTUNITY cards routed to Tax Planner`() {
        val opportunity = Opportunity(id = "80c_dsop", title = "80C Headroom", unusedAmount = 50_000.0, estTaxSaved = 10_000.0, action = "Increase DSOP")
        val card = buildSmartInsights(state(opportunities = listOf(opportunity))).single()
        assertEquals(InsightSeverity.OPPORTUNITY, card.severity)
        assertEquals(Screen.TaxPlanning, card.actionTarget)
        assertEquals("Increase DSOP", card.actionLabel)
        assertEquals(formatCurrency(10_000.0), card.amountLabel)
    }

    @Test
    fun `anomaly cards are prioritized ahead of opportunity cards`() {
        val opportunity = Opportunity(id = "80c_dsop", title = "80C Headroom", unusedAmount = 50_000.0, estTaxSaved = 10_000.0, action = "Increase DSOP")
        val insights = buildSmartInsights(state(anomalies = listOf(anomaly("SALARY_LOSS")), opportunities = listOf(opportunity)))
        assertEquals(InsightSeverity.IMPORTANT, insights.first().severity)
        assertEquals(InsightSeverity.OPPORTUNITY, insights.last().severity)
    }

    // ── Folded free findings (approved decision: fold the useful KeyFindings/AiHighlights signals
    // into Smart Insights as INFO cards, rather than deleting them outright in Phase 4). ──

    @Test
    fun `no deduction data yields no largest-deduction free-finding card`() {
        val insights = buildSmartInsights(state())
        assertTrue(insights.none { it.title == InsightsStrings.freeFindingLargestDeductionTitle })
    }

    @Test
    fun `income tax at or above DSOP surfaces income tax as the largest deduction`() {
        val current = record(incomeTax = 12_000.0, dsopSubscription = 10_000.0)
        val insights = buildSmartInsights(state(currentRecord = current))
        val card = insights.single { it.title == InsightsStrings.freeFindingLargestDeductionTitle }
        assertEquals(InsightSeverity.INFO, card.severity)
        assertEquals(InsightsStrings.freeFindingLargestDeductionTax, card.explanation)
        assertEquals(formatCurrency(12_000.0), card.amountLabel)
    }

    @Test
    fun `DSOP above income tax surfaces DSOP as the largest deduction`() {
        val current = record(incomeTax = 5_000.0, dsopSubscription = 15_000.0)
        val insights = buildSmartInsights(state(currentRecord = current))
        val card = insights.single { it.title == InsightsStrings.freeFindingLargestDeductionTitle }
        assertEquals(InsightsStrings.freeFindingLargestDeductionDsop, card.explanation)
        assertEquals(formatCurrency(15_000.0), card.amountLabel)
    }

    @Test
    fun `a material MoM component change surfaces the biggest-change free-finding card`() {
        val current = record(basicPay = 55_000.0, dearnessAllowance = 0.0)
        val previous = record(dateStr = "01/2026", basicPay = 50_000.0, dearnessAllowance = 0.0)
        val insights = buildSmartInsights(state(currentRecord = current, previousRecord = previous))
        val card = insights.single { it.title == InsightsStrings.freeFindingComponentChangeTitle }
        assertEquals(InsightSeverity.INFO, card.severity)
        assertEquals(formatCurrency(5_000.0), card.amountLabel)
    }

    @Test
    fun `a below-threshold component change yields no biggest-change free-finding card`() {
        val current = record(basicPay = 50_005.0)
        val previous = record(dateStr = "01/2026", basicPay = 50_000.0)
        val insights = buildSmartInsights(state(currentRecord = current, previousRecord = previous))
        assertTrue(insights.none { it.title == InsightsStrings.freeFindingComponentChangeTitle })
    }

    @Test
    fun `free-finding cards are appended after anomaly and opportunity cards not interleaved`() {
        val opportunity = Opportunity(id = "80c_dsop", title = "80C Headroom", unusedAmount = 50_000.0, estTaxSaved = 10_000.0, action = "Increase DSOP")
        val current = record(incomeTax = 12_000.0, dsopSubscription = 10_000.0)
        val insights =
            buildSmartInsights(
                state(currentRecord = current, anomalies = listOf(anomaly("SALARY_LOSS")), opportunities = listOf(opportunity)),
            )
        assertEquals(InsightSeverity.IMPORTANT, insights[0].severity)
        assertEquals(InsightSeverity.OPPORTUNITY, insights[1].severity)
        assertEquals(InsightsStrings.freeFindingLargestDeductionTitle, insights[2].title)
    }

    /** Anomaly type isn't on [InsightUiModel] itself (by design — it's a display model), so tests recover it from the title label SSOT. */
    private object InsightPrioritizationOrderProbe {
        private val labelToType =
            listOf("SALARY_LOSS", "DSOP_MILESTONE", "RENT_RECOVERY_RISK").associateBy { anomalyCategoryLabel(it) }

        fun typeOf(model: InsightUiModel): String = labelToType.getValue(model.title)
    }
}
