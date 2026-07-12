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
)

object WealthOptimizationEngine {
    private const val LIMIT_80C = 150_000.0
    private const val NPS_80CCD1B = 50_000.0
    private const val DEFAULT_YEARS_TO_RETIREMENT = 20

    fun analyze(
        payslip: ParsedPayslip,
        yearsToRetirement: Int = DEFAULT_YEARS_TO_RETIREMENT,
    ): OptimizationResult {
        val dsopMonthly = payslip.deductions.dsopSubscription
        val agifMonthly = payslip.deductions.agif
        val grossPay = payslip.summary.grossPay
        val netTaxableIncome = payslip.taxAndSavings?.netTaxableIncome ?: 0.0
        val dsopClosingBalance = payslip.taxAndSavings?.dsopFund?.closingBalance ?: 0.0

        val regime = payslip.taxAndSavings?.taxRegime ?: TaxRegime.OLD
        val marginalRate = deriveMarginalRate(netTaxableIncome, regime)

        val opportunities =
            if (regime == TaxRegime.NEW) {
                emptyList()
            } else {
                val annual80CUsed = (dsopMonthly + agifMonthly) * 12.0
                val annual80CHeadroom = maxOf(0.0, LIMIT_80C - annual80CUsed)
                buildOpportunities(annual80CHeadroom, marginalRate)
            }

        val dsopGapMonthly =
            if (regime == TaxRegime.NEW) {
                0.0
            } else {
                val annual80CUsed = (dsopMonthly + agifMonthly) * 12.0
                val annual80CHeadroom = maxOf(0.0, LIMIT_80C - annual80CUsed)
                computeDsopGap(dsopMonthly, grossPay, annual80CHeadroom)
            }
        val corpusUplift = computeCorpusUplift(dsopMonthly, dsopGapMonthly, dsopClosingBalance, yearsToRetirement)

        return OptimizationResult(
            totalPotentialTaxSaving = opportunities.sumOf { it.estTaxSaved },
            marginalRatePct = marginalRate,
            regimeAssumed = regime.name,
            opportunities = opportunities,
            dsopGapMonthly = dsopGapMonthly,
            dsopCorpusUpliftAtRetirement = corpusUplift,
        )
    }

    // Regime-specific slabs (FY 2024-25 / FY 2025-26); used to estimate marginal rate.
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

    private fun buildOpportunities(
        annual80CHeadroom: Double,
        marginalRate: Double,
    ): List<Opportunity> =
        buildList {
            if (annual80CHeadroom > 0.0) {
                val monthlyIncrease = (annual80CHeadroom / 12.0).toInt()
                add(
                    Opportunity(
                        id = "80c_dsop",
                        title = "80C: Increase DSOP Subscription",
                        unusedAmount = annual80CHeadroom,
                        estTaxSaved = annual80CHeadroom * marginalRate,
                        action = "Increase DSOP by ₹$monthlyIncrease/month to use full ₹1.5L limit (old regime est.)",
                    ),
                )
            }
            add(
                Opportunity(
                    id = "80ccd_nps",
                    title = "80CCD(1B): NPS Additional Contribution",
                    unusedAmount = NPS_80CCD1B,
                    estTaxSaved = NPS_80CCD1B * marginalRate,
                    action = "Invest ₹50,000/year in NPS (verify not already used elsewhere; old regime est.)",
                ),
            )
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
}
