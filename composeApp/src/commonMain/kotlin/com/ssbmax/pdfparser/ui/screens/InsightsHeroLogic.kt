package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.insights.Anomaly
import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.insights.OptimizationResult

private val RECOVERABLE_TYPES = setOf("MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "SALARY_LOSS")

sealed class HeroBranch {
    data class Recovery(
        val totalRecoverable: Double,
        val primaryCause: String,
        val anomalies: List<Anomaly>,
    ) : HeroBranch()

    data class Wealth(
        val totalPotentialTaxSaving: Double,
        val topOpportunityAction: String,
    ) : HeroBranch()
}

fun selectHeroBranch(
    engineResult: EngineResult,
    optimizationResult: OptimizationResult,
): HeroBranch {
    val recoverableAnomalies = engineResult.anomalies.filter { it.type in RECOVERABLE_TYPES }
    return if (recoverableAnomalies.isNotEmpty()) {
        HeroBranch.Recovery(
            totalRecoverable = recoverableAnomalies.sumOf { it.amount },
            primaryCause = recoverableAnomalies.first().description,
            anomalies = recoverableAnomalies,
        )
    } else {
        HeroBranch.Wealth(
            totalPotentialTaxSaving = optimizationResult.totalPotentialTaxSaving,
            topOpportunityAction = optimizationResult.opportunities.firstOrNull()?.action ?: "",
        )
    }
}
