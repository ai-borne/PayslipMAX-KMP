package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.domain.TaxRegime
import kotlinx.serialization.Serializable

@Serializable
data class Opportunity(
    val id: String,
    val title: String,
    val unusedAmount: Double,
    val estTaxSaved: Double,
    val action: String,
)

/** ADR-2 fail-loud state: the active FY has no resolvable rule pack, so no regime/liability figures
 * were computed rather than silently substituting the nearest known FY's numbers. */
@Serializable
data class TaxRulesUnavailableInfo(
    val requestedFy: String,
    val nearestKnownFy: String,
)

@Serializable
data class OptimizationResult(
    val totalPotentialTaxSaving: Double,
    val marginalRatePct: Double,
    val regimeAssumed: String,
    val opportunities: List<Opportunity>,
    val dsopGapMonthly: Double,
    val dsopCorpusUpliftAtRetirement: Double,
    val fySummary: FyTaxLedgerSummary? = null,
    val regimeComparison: RegimeComparisonResult? = null,
    val exemptionBreakdown: TaxExemptionBreakdown? = null,
    val tdsRunway: TdsRunwayResult? = null,
    val storyNarrative: TaxStoryNarrative? = null,
    /** Phase 5 (ADR-3): null only when PCDA's own `totalTaxPayable` is unavailable (D3 guard). */
    val taxTrackReconciliation: TaxTrackReconciliation? = null,
    val belatedReturnTrapWarning: String? = null,
    val dsopWasteInsight: DsopWasteInsight? = null,
    val arrearsTransparency: ArrearsTransparencyInsight? = null,
    val midYearRegimeChange: MidYearRegimeChangeInsight? = null,
    /** Phase 5 (ADR-4): null when the active regime is already the cheaper one. */
    val regimeDecisionPlan: RegimeDecisionPlan? = null,
    /** Phase 6: PCDA's own page-4 figures, mirrored verbatim -- never recomputed for display. */
    val pcdaOfficialComputation: TaxAndSavings? = null,
    /** Phase 6: current net pay adjusted by the TDS-runway delta -- null if the active payslip has no PCDA tax page. */
    val projectedNextMonthNetPay: Double? = null,
    /** ADR-2 (Phase 7): non-null only when [fySummary]'s FY has no resolvable rule pack -- every other
     * regime/liability field above is left at its default in that case rather than computed wrong. */
    val taxRulesUnavailable: TaxRulesUnavailableInfo? = null,
)

object WealthOptimizationEngine {
    private const val DEFAULT_YEARS_TO_RETIREMENT = 20

    fun analyze(
        payslip: ParsedPayslip,
        yearsToRetirement: Int = DEFAULT_YEARS_TO_RETIREMENT,
    ): OptimizationResult {
        return analyzeLedger(listOf(payslip), payslip, null, yearsToRetirement)
    }

    fun analyzeLedger(
        payslips: List<ParsedPayslip>,
        selectedPayslip: ParsedPayslip? = null,
        targetFy: String? = null,
        yearsToRetirement: Int = DEFAULT_YEARS_TO_RETIREMENT,
    ): OptimizationResult {
        val activePayslip = selectedPayslip ?: payslips.lastOrNull() ?: return createFallbackResult()
        val fySummary = TaxLedgerAggregator.aggregateFy(payslips, targetFy)
        val activeRegime = activePayslip.taxAndSavings?.taxRegime ?: TaxRegime.OLD

        // Regime-neutral (always-OLD-hypothetical), capped exemptions (D8) -- this MUST NOT be the
        // regime-gated `exemptions` below, or an active-NEW user's old-regime comparison would be
        // computed as if they had zero deductions, breaking the switch-regime savings math (D9).
        val oldRegimeExemptions = DefenceTaxExemptionEngine.extractExemptions(fySummary)
        val regimeComp =
            when (
                val outcome =
                    DualRegimeEngine.compareRegimes(
                        grossIncome = fySummary.projectedAnnualGross,
                        oldRegimeDeductions = oldRegimeExemptions.totalOldRegimeDeductions,
                        fy = fySummary.financialYear,
                    )
            ) {
                // ADR-2: the FY has no resolvable rule pack -- degrade visibly (fySummary + an explicit
                // "rules unavailable" marker) rather than computing every downstream figure off a
                // silently-substituted nearest FY's numbers.
                is RegimeComparisonOutcome.RulesUnavailable ->
                    return OptimizationResult(
                        totalPotentialTaxSaving = 0.0,
                        marginalRatePct = 0.0,
                        regimeAssumed = activeRegime.name,
                        opportunities = emptyList(),
                        dsopGapMonthly = 0.0,
                        dsopCorpusUpliftAtRetirement = 0.0,
                        fySummary = fySummary,
                        taxRulesUnavailable = TaxRulesUnavailableInfo(outcome.requestedFy, outcome.nearestKnownFy),
                    )
                is RegimeComparisonOutcome.Available -> outcome.result
            }

        // Regime-conditional (D10): zeroed with an explicit reason under NEW, since old-regime-only
        // sections genuinely don't reduce this user's actual tax bill right now.
        val exemptions = DefenceTaxExemptionEngine.extractExemptions(fySummary, activeRegime = activeRegime)

        val activeTax = if (activeRegime == TaxRegime.NEW) regimeComp.newRegime.totalTaxPayable else regimeComp.oldRegime.totalTaxPayable
        // fySummary.financialYear already produced a resolved regimeComp above, so this FY is
        // guaranteed resolvable (ADR-2) -- the null branch is unreachable here.
        val marginalRate =
            deriveMarginalRate(
                netTaxableIncome = if (activeRegime == TaxRegime.NEW) regimeComp.newRegime.netTaxableIncome else regimeComp.oldRegime.netTaxableIncome,
                regime = activeRegime,
                fy = fySummary.financialYear,
            ) ?: 0.0

        val tdsRunway =
            TdsRunwayEngine.computeTdsRunway(
                ytdTdsDeducted = fySummary.ytdTaxDeducted,
                // D11/D12: "months so far" for the runway must be the FY-calendar position, not the
                // upload count, or a gap before the latest payslip makes the remaining-months split wrong
                // and can manufacture a false spike.
                parsedMonthCount = fySummary.monthsElapsedInFy,
                totalAnnualTaxLiability = activeTax,
                currentMonthlyTds = activePayslip.deductions.incomeTax,
            )

        val storyNarrative =
            ConversationalTaxNarrativeEngine.generateNarrative(
                payslips = payslips,
                fySummary = fySummary,
                projectedTax = activeTax,
            )

        // Phase 5 (ADR-3/ADR-4): two-track reconciliation and its corollaries, all derived from the
        // regime-neutral `regimeComp` already computed above -- no new tax computation introduced.
        val regimeDecisionPlan = RegimeDecisionPlanner.buildRegimeDecisionPlan(regimeComp, activeRegime)
        val taxTrackReconciliation = TwoTrackReconciliationEngine.reconcile(fySummary, regimeComp, activePayslip)
        val belatedReturnWarning = TwoTrackReconciliationEngine.belatedReturnTrapWarning(regimeComp)
        val dsopWaste = TwoTrackReconciliationEngine.dsopWasteInsight(fySummary, activeRegime, regimeComp)
        val arrearsInsight = TwoTrackReconciliationEngine.arrearsTransparency(fySummary)
        val midYearRegimeChange = RegimeDecisionPlanner.detectMidYearRegimeChange(payslips, fySummary.financialYear)

        val opportunities =
            buildList {
                // ADR-4: the PCDA intimation carries no tax-saving of its own (it only redirects future
                // withholding) -- only the ITR election actually delivers `annualSavings`, so summing
                // `estTaxSaved` across both entries can never double-count the same rupee figure.
                regimeDecisionPlan?.let { plan ->
                    add(
                        Opportunity(
                            id = plan.pcdaIntimationDecision.id,
                            title = plan.pcdaIntimationDecision.title,
                            unusedAmount = 0.0,
                            estTaxSaved = 0.0,
                            action = plan.pcdaIntimationDecision.action,
                        ),
                    )
                    add(
                        Opportunity(
                            id = plan.itrElectionDecision.id,
                            title = plan.itrElectionDecision.title,
                            unusedAmount = 0.0,
                            estTaxSaved = regimeComp.annualSavings,
                            action = plan.itrElectionDecision.action,
                        ),
                    )
                }
                if (exemptions.sec80C.headroom > 0.0) {
                    val monthlyIncrease = (exemptions.sec80C.headroom / 12.0).toInt()
                    add(
                        Opportunity(
                            id = "80c_dsop",
                            title = "80C: Increase DSOP Subscription",
                            unusedAmount = exemptions.sec80C.headroom,
                            estTaxSaved = exemptions.sec80C.headroom * marginalRate,
                            action = "Increase DSOP by ₹$monthlyIncrease/month to use full ₹1.5L limit.",
                        ),
                    )
                }
                if (exemptions.sec80CCD1B.headroom > 0.0) {
                    add(
                        Opportunity(
                            id = "80ccd_nps",
                            title = "80CCD(1B): NPS Additional Contribution",
                            unusedAmount = exemptions.sec80CCD1B.headroom,
                            estTaxSaved = exemptions.sec80CCD1B.headroom * marginalRate,
                            action = "Invest ₹${exemptions.sec80CCD1B.headroom.toInt()}/year in NPS for extra tax deduction.",
                        ),
                    )
                }
            }

        // Phase 6: next month's take-home, estimated by shifting the current month's net pay by the
        // same TDS-runway delta already computed above -- no second projection engine introduced.
        val projectedNextMonthNetPay =
            if (activePayslip.taxAndSavings?.totalTaxPayable != null) {
                maxOf(0.0, activePayslip.summary.netRemittance - (tdsRunway.projectedMonthlyTds - tdsRunway.currentMonthlyTds))
            } else {
                null
            }

        val dsopMonthly = activePayslip.deductions.dsopSubscription
        // Retirement-corpus room, not a tax-saving claim -- stays regime-neutral (uses the uncapped
        // headroom, not the NEW-regime-gated `exemptions`).
        val dsopGapMonthly = computeDsopGap(dsopMonthly, activePayslip.summary.grossPay, oldRegimeExemptions.sec80C.headroom)
        val closingBalance = activePayslip.taxAndSavings?.dsopFund?.closingBalance ?: 0.0
        val corpusUplift = computeCorpusUplift(dsopMonthly, dsopGapMonthly, closingBalance, yearsToRetirement)

        return OptimizationResult(
            totalPotentialTaxSaving = opportunities.sumOf { it.estTaxSaved },
            marginalRatePct = marginalRate,
            regimeAssumed = activeRegime.name,
            opportunities = opportunities,
            dsopGapMonthly = dsopGapMonthly,
            dsopCorpusUpliftAtRetirement = corpusUplift,
            fySummary = fySummary,
            regimeComparison = regimeComp,
            exemptionBreakdown = exemptions,
            tdsRunway = tdsRunway,
            storyNarrative = storyNarrative,
            taxTrackReconciliation = taxTrackReconciliation,
            belatedReturnTrapWarning = belatedReturnWarning,
            dsopWasteInsight = dsopWaste,
            arrearsTransparency = arrearsInsight,
            midYearRegimeChange = midYearRegimeChange,
            regimeDecisionPlan = regimeDecisionPlan,
            pcdaOfficialComputation = activePayslip.taxAndSavings,
            projectedNextMonthNetPay = projectedNextMonthNetPay,
        )
    }

    /** Delegates to [DualRegimeEngine.marginalRate] -- no hardcoded slab copy here (D2). Null when
     * [fy] has no resolvable rule pack (ADR-2). */
    fun deriveMarginalRate(
        netTaxableIncome: Double,
        regime: TaxRegime = TaxRegime.OLD,
        fy: String = "2026-27",
    ): Double? = DualRegimeEngine.marginalRate(netTaxableIncome, regime, fy)

    private fun computeDsopGap(
        dsopMonthly: Double,
        grossMonthly: Double,
        sec80CHeadroom: Double,
    ): Double {
        val annualDsop = dsopMonthly * 12.0
        val maxAllowedAnnual = (0.35 * grossMonthly * 12.0)
        val spaceLeftAnnual = maxOf(0.0, maxAllowedAnnual - annualDsop)
        val gapAnnual = minOf(spaceLeftAnnual, sec80CHeadroom)
        return gapAnnual / 12.0
    }

    private fun computeCorpusUplift(
        dsopMonthly: Double,
        gapMonthly: Double,
        currentBalance: Double,
        years: Int,
    ): Double {
        if (gapMonthly <= 0.0) return 0.0
        val rate = 0.071
        val months = years * 12
        var baseCorpus = currentBalance
        var upliftCorpus = currentBalance

        for (m in 1..months) {
            baseCorpus += dsopMonthly
            upliftCorpus += (dsopMonthly + gapMonthly)
            if (m % 12 == 0) {
                baseCorpus += baseCorpus * rate
                upliftCorpus += upliftCorpus * rate
            }
        }
        return maxOf(0.0, upliftCorpus - baseCorpus)
    }

    private fun createFallbackResult(): OptimizationResult {
        return OptimizationResult(
            totalPotentialTaxSaving = 0.0,
            marginalRatePct = 0.0,
            regimeAssumed = "NEW",
            opportunities = emptyList(),
            dsopGapMonthly = 0.0,
            dsopCorpusUpliftAtRetirement = 0.0,
        )
    }
}
