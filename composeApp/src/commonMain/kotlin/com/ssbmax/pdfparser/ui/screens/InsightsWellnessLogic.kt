package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.ui.theme.InsightsStrings

data class WellnessDriver(
    val title: String,
    val pointImpact: Int,
    val improvePath: String?,
)

fun breakdownWellnessDrivers(engineResult: EngineResult): List<WellnessDriver> {
    val drivers = mutableListOf<WellnessDriver>()

    for (anomaly in engineResult.anomalies) {
        when (anomaly.type) {
            "MISSING_ALLOWANCE" ->
                drivers.add(
                    WellnessDriver("${InsightsStrings.wellnessTitleMissingAllowance} ${anomaly.field}", -15, InsightsStrings.wellnessImproveMissingAllowance),
                )
            "SALARY_LOSS" ->
                drivers.add(
                    WellnessDriver(InsightsStrings.wellnessTitleSalaryLoss, -20, InsightsStrings.wellnessImproveSalaryLoss),
                )
            "DEDUCTION_SPIKE" ->
                drivers.add(
                    WellnessDriver("${InsightsStrings.wellnessTitleDeductionSpike} ${anomaly.field}", -10, InsightsStrings.wellnessImproveDeductionSpike),
                )
            "TPTA_ENTITLEMENT" ->
                drivers.add(
                    WellnessDriver(InsightsStrings.wellnessTitleTpta, -10, InsightsStrings.wellnessImproveTptaEntitlement),
                )
            "DSOP_COMPLIANCE" ->
                drivers.add(
                    WellnessDriver(
                        title = InsightsStrings.wellnessTitleDsop,
                        pointImpact = if (anomaly.amount > 0.0) -25 else -5,
                        improvePath = InsightsStrings.wellnessDsopNonCompliance,
                    ),
                )
        }
    }

    val savingRate = engineResult.monthlySavingRate
    when {
        savingRate >= 20.0 ->
            drivers.add(
                WellnessDriver("${InsightsStrings.wellnessSavingsRateLabel} ${savingRate.toInt()}%", +15, null),
            )
        savingRate >= 10.0 ->
            drivers.add(
                WellnessDriver("${InsightsStrings.wellnessSavingsRateLabel} ${savingRate.toInt()}%", +5, InsightsStrings.wellnessImproveSavingsRate),
            )
        else ->
            drivers.add(
                WellnessDriver("${InsightsStrings.wellnessSavingsRateLabel} ${savingRate.toInt()}%", 0, InsightsStrings.wellnessImproveSavingsRate),
            )
    }

    if (engineResult.anomalies.isEmpty() && savingRate >= 10.0) {
        drivers.add(WellnessDriver(InsightsStrings.wellnessNoIssuesBonus, +10, null))
    }

    return drivers
}
