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
)

object WealthOptimizationEngine {
    private const val LIMIT_80C = 150_000.0
    private const val NPS_80CCD1B = 50_000.0
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
            )

        val latestMonthlyTds = activePayslip.deductions.incomeTax
        val tdsRunway =
            TdsRunwayEngine.computeTdsRunway(
                ytdTdsDeducted = fySummary.ytdTaxDeducted,
                parsedMonthCount = fySummary.parsedMonthCount,
                totalAnnualTaxLiability = activeTax,
                currentMonthlyTds = latestMonthlyTds,
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
        )
    }

    fun deriveMarginalRate(
        netTaxableIncome: Double,
        regime: TaxRegime = TaxRegime.OLD,
    ): Double =
        if (regime == TaxRegime.NEW) {
            when {
                netTaxableIncome <= 300_000.0 -> 0.0
                netTaxableIncome <= 700_000.0 -> 0.05
                netTaxableIncome <= 1_000_000.0 -> 0.10
                netTaxableIncome <= 1_200_000.0 -> 0.15
                netTaxableIncome <= 1_500_000.0 -> 0.20
                else -> 0.30
            }
        } else {
            when {
                netTaxableIncome <= 250_000.0 -> 0.0
                netTaxableIncome <= 500_000.0 -> 0.05
                netTaxableIncome <= 1_000_000.0 -> 0.20
                else -> 0.30
            }
        }

    private fun computeDsopGap(
        dsopMonthly: Double,
        grossPay: Double,
        annual80CHeadroom: Double,
    ): Double {
        val monthly80CFill = annual80CHeadroom / 12.0
        val roomTo20PctGross = maxOf(0.0, grossPay * 0.20 - dsopMonthly)
        return minOf(monthly80CFill, roomTo20PctGross)
    }

    private fun computeCorpusUplift(
        dsopMonthly: Double,
        dsopGapMonthly: Double,
        closingBalance: Double,
        years: Int,
    ): Double {
        if (dsopGapMonthly <= 0.0) return 0.0
        val current = ProjectionMath.calculateProjection(closingBalance, dsopMonthly, years)
        val enhanced = ProjectionMath.calculateProjection(closingBalance, dsopMonthly + dsopGapMonthly, years)
        return enhanced.projectedBalance - current.projectedBalance
    }

    private fun createFallbackResult(): OptimizationResult {
        return OptimizationResult(
            totalPotentialTaxSaving = 0.0,
            marginalRatePct = 0.0,
            regimeAssumed = "OLD",
            opportunities = emptyList(),
            dsopGapMonthly = 0.0,
            dsopCorpusUpliftAtRetirement = 0.0,
        )
    }
}
