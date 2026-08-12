package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.TaxRegime
import kotlinx.serialization.Serializable

@Serializable
data class MonthlyLedgerItem(
    val monthName: String,
    val year: Int,
    val monthNum: Int,
    val tdsDeducted: Double,
    val dsopContribution: Double,
)

/**
 * Phase 8 (U3): the one-glance "Bottom Line Up Front" summary -- a plain-language liability sentence
 * plus a single "nothing to do" / "here's the one thing that needs attention" line, so the reader
 * doesn't have to synthesize that verdict themselves out of several cards' worth of numbers.
 */
@Serializable
data class TaxBlufSummary(
    val headline: String,
    val actionLine: String,
    val isActionRequired: Boolean,
)

@Serializable
data class TaxStoryNarrative(
    val financialYear: String,
    val assessmentYear: String,
    val parsedMonthCount: Int,
    val coverageHeader: String,
    val missingMonthNudge: String?,
    val monthlyLedgerList: List<MonthlyLedgerItem>,
    val totalTdsYtd: Double,
    val totalDsopYtd: Double,
    val projectedGross: Double,
    val projectedTax: Double,
    val effectiveTaxRatePct: Double,
)

object ConversationalTaxNarrativeEngine {
    fun generateNarrative(
        payslips: List<ParsedPayslip>,
        fySummary: FyTaxLedgerSummary,
        projectedTax: Double,
    ): TaxStoryNarrative {
        val activeFy = fySummary.financialYear
        val fyPayslips =
            payslips.filter { TaxLedgerAggregator.computeFinancialYear(it.year, it.monthNum) == activeFy }
                .sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum })

        val ledgerList =
            fyPayslips.map { payslip ->
                MonthlyLedgerItem(
                    monthName = payslip.monthName,
                    year = payslip.year,
                    monthNum = payslip.monthNum,
                    tdsDeducted = payslip.deductions.incomeTax,
                    dsopContribution = payslip.deductions.dsopSubscription,
                )
            }

        val count = ledgerList.size
        val monthWord = if (count == 1) "month" else "months"
        val coverageHeader = "In Assessment Year ${fySummary.assessmentYear} (FY ${fySummary.financialYear}), we have $count $monthWord of payslips available."

        // D17: `missingMonthNums` includes months later in the FY that haven't been issued yet -- those
        // can never be "uploaded", so only months already elapsed but not yet parsed are actionable.
        // A March payslip with April/May still missing is not a defect; a July payslip missing May is.
        val actionableMissingCount =
            fySummary.missingMonthNums.count { TaxLedgerAggregator.monthPositionInFy(it) <= fySummary.monthsElapsedInFy }
        val missingNudge =
            if (actionableMissingCount > 0) {
                val monthNoun = if (actionableMissingCount == 1) "payslip" else "payslips"
                "Upload $actionableMissingCount more $monthNoun from earlier this FY to sharpen your projection."
            } else {
                null
            }

        val totalGross = fySummary.projectedAnnualGross
        val effectiveRate = if (totalGross > 0) (projectedTax / totalGross) * 100.0 else 0.0
        val formattedRate = ((effectiveRate * 10).toInt()) / 10.0

        return TaxStoryNarrative(
            financialYear = fySummary.financialYear,
            assessmentYear = fySummary.assessmentYear,
            parsedMonthCount = count,
            coverageHeader = coverageHeader,
            missingMonthNudge = missingNudge,
            monthlyLedgerList = ledgerList,
            totalTdsYtd = fySummary.ytdTaxDeducted,
            totalDsopYtd = fySummary.ytdDsop,
            projectedGross = totalGross,
            projectedTax = projectedTax,
            effectiveTaxRatePct = formattedRate,
        )
    }

    /**
     * Phase 8 (U3): reuses numbers and sentences the engine already computed elsewhere -- no new tax
     * math, no re-derivation of any figure. [reconciliation]/[dsopWasteInsight]/[midYearRegimeChange]
     * are checked in that priority order because DSOP waste and a mid-year change are the more
     * actionable, specific findings; a merely-diverging TDS/liability reconciliation is the fallback
     * flag when neither of those fires.
     */
    fun buildBluf(
        regimeComparison: RegimeComparisonResult,
        reconciliation: TaxTrackReconciliation?,
        dsopWasteInsight: DsopWasteInsight?,
        midYearRegimeChange: MidYearRegimeChangeInsight?,
        parsedMonthCount: Int,
    ): TaxBlufSummary {
        val isNewWinner = regimeComparison.winnerRegime == TaxRegime.NEW.name
        val winningDetail = if (isNewWinner) regimeComparison.newRegime else regimeComparison.oldRegime
        val totalTax = winningDetail.totalTaxPayable

        val amountPhrase =
            if (parsedMonthCount in 1 until TaxLedgerAggregator.LOW_COVERAGE_MONTHS) {
                val lower = TaxLedgerAggregator.roundToNearestThousand(totalTax * (1.0 - TaxLedgerAggregator.LOW_COVERAGE_BAND))
                val upper = TaxLedgerAggregator.roundToNearestThousand(totalTax * (1.0 + TaxLedgerAggregator.LOW_COVERAGE_BAND))
                "between ₹${TaxLedgerAggregator.formatIndianCurrency(lower)} and ₹${TaxLedgerAggregator.formatIndianCurrency(upper)}"
            } else {
                "about ₹${TaxLedgerAggregator.formatIndianCurrency(TaxLedgerAggregator.roundToNearestThousand(totalTax))}"
            }
        val headline =
            "Your total tax for this financial year will be $amountPhrase -- that's what you'll actually owe " +
                "when you file, not just what's being deducted from your pay each month (TDS)."

        val actionLine =
            when {
                dsopWasteInsight != null -> dsopWasteInsight.message
                midYearRegimeChange?.detected == true ->
                    midYearRegimeChange.message
                        ?: "Your tax regime changed partway through this financial year -- see the details below."
                reconciliation != null && reconciliation.reconciliationType != ReconciliationType.MATCHED -> reconciliation.message
                else -> "Nothing needs your attention right now -- you're already on the tax regime that costs you the least."
            }
        val isActionRequired =
            dsopWasteInsight != null ||
                midYearRegimeChange?.detected == true ||
                (reconciliation != null && reconciliation.reconciliationType != ReconciliationType.MATCHED)

        return TaxBlufSummary(headline = headline, actionLine = actionLine, isActionRequired = isActionRequired)
    }
}
