package com.payslipmax.pdfparser.insights

import kotlinx.serialization.Serializable

@Serializable
enum class ConfidenceTier {
    HIGH,
    MODERATE,
    PRELIMINARY,
}

@Serializable
data class TaxConfidenceResult(
    val scorePct: Int,
    val tier: ConfidenceTier,
    val explanation: String,
)

object ConfidenceScoringEngine {
    fun computeScore(
        parsedMonthCount: Int,
        grossPayVariancePct: Double = 0.0,
        hasUserDeclarations: Boolean = false,
    ): TaxConfidenceResult {
        val baseScore = 35.0
        val monthWeight = (minOf(12, maxOf(1, parsedMonthCount)) / 12.0) * 45.0
        val monthStepBonus = if (parsedMonthCount >= 4) 15.0 else 0.0
        val variancePenalty = minOf(20.0, grossPayVariancePct * 100.0)
        val userBonus = if (hasUserDeclarations) 10.0 else 0.0

        val totalScore = (baseScore + monthWeight + monthStepBonus - variancePenalty + userBonus).toInt().coerceIn(10, 100)

        val (tier, explanation) =
            when {
                totalScore >= 85 -> ConfidenceTier.HIGH to "High Confidence: $parsedMonthCount months parsed with stable regular pay."
                totalScore >= 60 -> ConfidenceTier.MODERATE to "Moderate Confidence: $parsedMonthCount months parsed. Upload additional months to increase projection accuracy."
                else -> ConfidenceTier.PRELIMINARY to "Preliminary Estimate: Based on $parsedMonthCount month payslip data."
            }

        return TaxConfidenceResult(
            scorePct = totalScore,
            tier = tier,
            explanation = explanation,
        )
    }
}
