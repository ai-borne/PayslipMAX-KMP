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
}
