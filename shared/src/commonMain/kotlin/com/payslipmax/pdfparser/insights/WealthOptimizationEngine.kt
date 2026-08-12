package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
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
        val exemptions = DefenceTaxExemptionEngine.extractExemptions(fySummary)
        val regimeComp =
            DualRegimeEngine.compareRegimes(
                grossIncome = fySummary.projectedAnnualGross,
                oldRegimeDeductions = exemptions.totalOldRegimeDeductions,
                fy = fySummary.financialYear,
            )

        val activeRegime = activePayslip.taxAndSavings?.taxRegime ?: TaxRegime.OLD
        val activeTax = if (activeRegime == TaxRegime.NEW) regimeComp.newRegime.totalTaxPayable else regimeComp.oldRegime.totalTaxPayable
        val marginalRate =
            deriveMarginalRate(
                netTaxableIncome = if (activeRegime == TaxRegime.NEW) regimeComp.newRegime.netTaxableIncome else regimeComp.oldRegime.netTaxableIncome,
                regime = activeRegime,
                fy = fySummary.financialYear,
            )

        val tdsRunway =
            TdsRunwayEngine.computeTdsRunway(
                ytdTdsDeducted = fySummary.ytdTaxDeducted,
                parsedMonthCount = fySummary.parsedMonthCount,
                totalAnnualTaxLiability = activeTax,
                currentMonthlyTds = activePayslip.deductions.incomeTax,
            )

        val storyNarrative =
            ConversationalTaxNarrativeEngine.generateNarrative(
                payslips = payslips,
                fySummary = fySummary,
                projectedTax = activeTax,
            )

        val opportunities =
            buildList {
                if (regimeComp.winnerRegime != activeRegime.name && regimeComp.annualSavings > 0) {
                    add(
                        Opportunity(
                            id = "switch_regime",
                            title = "Switch to ${regimeComp.winnerRegime} Regime",
                            unusedAmount = 0.0,
                            estTaxSaved = regimeComp.annualSavings,
                            action = "Declare ${regimeComp.winnerRegime} Tax Regime to PCDA to save ₹${regimeComp.annualSavings.toInt()}/year.",
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

        val dsopMonthly = activePayslip.deductions.dsopSubscription
        val dsopGapMonthly = computeDsopGap(dsopMonthly, activePayslip.summary.grossPay, exemptions.sec80C.headroom)
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
        )
    }

    fun buildTaxPlannerResult(
        grossSalary: Double,
        tdsYtd: Double,
        dsopYtd: Double,
        fieldAllowanceExemption: Double = 0.0,
        monthsAvailable: Int = 1,
        monthNum: Int = 4,
        year: Int = 2026,
    ): TaxPlannerResult =
        TaxPlannerResultBuilder.buildTaxPlannerResult(
            grossSalary = grossSalary,
            tdsYtd = tdsYtd,
            dsopYtd = dsopYtd,
            fieldAllowanceExemption = fieldAllowanceExemption,
            monthsAvailable = monthsAvailable,
            monthNum = monthNum,
            year = year,
        )

    /** Delegates to [DualRegimeEngine.marginalRate] -- no hardcoded slab copy here (D2). */
    fun deriveMarginalRate(
        netTaxableIncome: Double,
        regime: TaxRegime = TaxRegime.OLD,
        fy: String = "2026-27",
    ): Double = DualRegimeEngine.marginalRate(netTaxableIncome, regime, fy)

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
