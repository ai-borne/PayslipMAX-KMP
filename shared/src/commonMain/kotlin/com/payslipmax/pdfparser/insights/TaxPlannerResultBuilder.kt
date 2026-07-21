package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.tax.TaxYearResolver

object TaxPlannerResultBuilder {
    fun buildTaxPlannerResult(
        grossSalary: Double,
        tdsYtd: Double,
        dsopYtd: Double,
        fieldAllowanceExemption: Double = 0.0,
        monthsAvailable: Int = 1,
        monthNum: Int = 4,
        year: Int = 2026,
    ): TaxPlannerResult {
        val resolvedYears = TaxYearResolver.resolve(monthNum, year)
        val fy = resolvedYears.financialYear
        val ay = resolvedYears.assessmentYear

        val rulePack =
            TaxRulePackInfo(
                financialYear = fy,
                assessmentYear = ay,
                version = "2026.1",
                verifiedOn = "01 Jul 2026",
                sourceLabel = "Rules FY $fy (AY $ay) · Offline",
                status = "verified",
            )

        val confidenceLabel =
            when {
                monthsAvailable >= 10 -> "High"
                monthsAvailable >= 6 -> "Medium"
                monthsAvailable >= 2 -> "Medium-low"
                else -> "Low"
            }
        val warnings =
            if (monthsAvailable < 12) {
                listOf("Upload remaining ${12 - monthsAvailable} month payslips to make projection 100% accurate.")
            } else {
                emptyList()
            }
        val dataCoverage =
            DataCoverageInfo(
                monthsAvailable = monthsAvailable,
                monthsRemaining = 12 - monthsAvailable,
                confidenceLabel = confidenceLabel,
                warnings = warnings,
            )

        val totalOldDeductionsExcludingStd = minOf(150000.0, dsopYtd * (12.0 / maxOf(1, monthsAvailable))) + fieldAllowanceExemption
        val regimeComp = DualRegimeEngine.compareRegimes(grossSalary, totalOldDeductionsExcludingStd, fy)

        val oldBreakdown =
            RegimeTaxBreakdown(
                standardDeduction = regimeComp.oldRegime.standardDeduction,
                allowedExemptions = regimeComp.oldRegime.totalDeductionsAndExemptions,
                taxableIncome = regimeComp.oldRegime.netTaxableIncome,
                taxBeforeCess = regimeComp.oldRegime.baseTax,
                surcharge = 0.0,
                cess = regimeComp.oldRegime.cess,
                totalTax = regimeComp.oldRegime.totalTaxPayable,
                effectiveRate = regimeComp.oldRegime.effectiveTaxRatePct,
            )

        val newBreakdown =
            RegimeTaxBreakdown(
                standardDeduction = regimeComp.newRegime.standardDeduction,
                allowedExemptions = regimeComp.newRegime.standardDeduction,
                taxableIncome = regimeComp.newRegime.netTaxableIncome,
                taxBeforeCess = regimeComp.newRegime.baseTax,
                surcharge = 0.0,
                cess = regimeComp.newRegime.cess,
                totalTax = regimeComp.newRegime.totalTaxPayable,
                effectiveRate = regimeComp.newRegime.effectiveTaxRatePct,
            )

        val activeTax = if (regimeComp.winnerRegime == "NEW") newBreakdown.totalTax else oldBreakdown.totalTax
        val remainingTax = maxOf(0.0, activeTax - tdsYtd)
        val monthsLeft = maxOf(1, 12 - monthsAvailable)
        val projectedMonthlyTds = remainingTax / monthsLeft
        val currentMonthlyTds = tdsYtd / maxOf(1, monthsAvailable)

        val tdsSummary =
            TdsRunwaySummary(
                deductedYtd = tdsYtd,
                remainingTax = remainingTax,
                projectedMonthlyTds = projectedMonthlyTds,
                spikeWarning = projectedMonthlyTds > currentMonthlyTds * 1.15,
            )

        val recSummary =
            TaxRecommendationSummary(
                bestRegime = regimeComp.winnerRegime,
                estimatedSavings = regimeComp.annualSavings,
                availableActions =
                    listOf(
                        RecommendedActionItem(
                            id = "80ccd_nps",
                            title = "80CCD(1B): NPS Additional Contribution",
                            description = "Invest ₹50,000/year in NPS for extra tax deduction.",
                            estimatedTaxImpact = 15000.0,
                        ),
                    ),
            )

        return TaxPlannerResult(
            financialYear = fy,
            assessmentYear = ay,
            rulePack = rulePack,
            dataCoverage = dataCoverage,
            income =
                IncomeBreakdown(
                    grossProjectedSalary = grossSalary,
                    exemptAllowances = fieldAllowanceExemption,
                    taxableSalaryBeforeDeductions = grossSalary - fieldAllowanceExemption,
                ),
            oldRegime = oldBreakdown,
            newRegime = newBreakdown,
            recommendation = recSummary,
            tds = tdsSummary,
        )
    }
}
