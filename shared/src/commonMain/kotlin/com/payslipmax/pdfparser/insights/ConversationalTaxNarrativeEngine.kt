package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlinx.serialization.Serializable

@Serializable
data class MonthlyLedgerItem(
    val monthName: String,
    val year: Int,
    val monthNum: Int,
    val tdsDeducted: Double,
    val dsopContribution: Double,
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
    val peerBenchmarkRatePct: Double,
    val effectiveTaxRateMessage: String,
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

        val missingNudge =
            if (fySummary.missingMonthNums.isNotEmpty()) {
                "Upload remaining ${fySummary.missingMonthNums.size} month payslips to make your projection 100% accurate."
            } else {
                null
            }

        val totalGross = fySummary.projectedAnnualGross
        val effectiveRate = if (totalGross > 0) (projectedTax / totalGross) * 100.0 else 0.0
        val formattedRate = ((effectiveRate * 10).toInt()) / 10.0

        val optResult =
            DualRegimeEngine.compareRegimes(
                grossIncome = totalGross,
                oldRegimeDeductions = fySummary.ytdDsop + 50000.0 + 253500.0,
                fy = activeFy,
            )
        val minTax = minOf(optResult.oldRegime.totalTaxPayable, optResult.newRegime.totalTaxPayable)
        val bestAchievableRate = if (totalGross > 0) (minTax / totalGross) * 100.0 else 0.0
        val formattedBestRate = ((bestAchievableRate * 10).toInt()) / 10.0

        val grossText = TaxLedgerAggregator.formatIndianCurrency(totalGross)
        val taxText = TaxLedgerAggregator.formatIndianCurrency(projectedTax)
        val narrativeMsg = "You are paying $taxText in tax out of $grossText total salary ($formattedRate% of income)."

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
            peerBenchmarkRatePct = formattedBestRate,
            effectiveTaxRateMessage = narrativeMsg,
        )
    }
}
