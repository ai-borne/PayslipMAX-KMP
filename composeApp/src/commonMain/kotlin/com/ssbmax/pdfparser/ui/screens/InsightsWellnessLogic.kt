package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.ui.theme.AppStrings

data class WellnessDriver(
    val title: String,
    val pointImpact: Int,
    val improvePath: String?,
)

fun breakdownWellnessDrivers(engineResult: EngineResult): List<WellnessDriver> {
    val drivers = mutableListOf<WellnessDriver>()

    for (anomaly in engineResult.anomalies) {
        when (anomaly.type) {
            "MISSING_ALLOWANCE" -> drivers.add(
                WellnessDriver("${AppStrings.wellnessTitleMissingAllowance} ${anomaly.field}", -15, AppStrings.wellnessImproveMissingAllowance),
            )
            "SALARY_LOSS" -> drivers.add(
                WellnessDriver(AppStrings.wellnessTitleSalaryLoss, -20, AppStrings.wellnessImproveSalaryLoss),
            )
            "DEDUCTION_SPIKE" -> drivers.add(
                WellnessDriver("${AppStrings.wellnessTitleDeductionSpike} ${anomaly.field}", -10, AppStrings.wellnessImproveDeductionSpike),
            )
            "TPTA_ENTITLEMENT" -> drivers.add(
                WellnessDriver(AppStrings.wellnessTitleTpta, -10, AppStrings.wellnessImproveTptaEntitlement),
            )
            "DSOP_COMPLIANCE" -> drivers.add(
                WellnessDriver(
                    title = AppStrings.wellnessTitleDsop,
                    pointImpact = if (anomaly.amount > 0.0) -25 else -5,
                    improvePath = AppStrings.wellnessDsopNonCompliance,
                ),
            )
        }
    }

    val savingRate = engineResult.monthlySavingRate
    when {
        savingRate >= 20.0 -> drivers.add(
            WellnessDriver("${AppStrings.wellnessSavingsRateLabel} ${savingRate.toInt()}%", +15, null),
        )
        savingRate >= 10.0 -> drivers.add(
            WellnessDriver("${AppStrings.wellnessSavingsRateLabel} ${savingRate.toInt()}%", +5, AppStrings.wellnessImproveSavingsRate),
        )
        else -> drivers.add(
            WellnessDriver("${AppStrings.wellnessSavingsRateLabel} ${savingRate.toInt()}%", 0, AppStrings.wellnessImproveSavingsRate),
        )
    }

    if (engineResult.anomalies.isEmpty() && savingRate >= 10.0) {
        drivers.add(WellnessDriver(AppStrings.wellnessNoIssuesBonus, +10, null))
    }

    return drivers
}
