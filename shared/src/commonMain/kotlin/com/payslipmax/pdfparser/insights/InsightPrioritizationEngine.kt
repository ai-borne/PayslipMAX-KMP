package com.payslipmax.pdfparser.insights

import kotlin.math.abs

data class ScoredAnomaly(
    val anomaly: Anomaly,
    val score: Double,
)

object InsightPrioritizationEngine {
    fun prioritize(
        anomalies: List<Anomaly>,
        previousAnomalies: List<Anomaly> = emptyList(),
    ): List<Anomaly> {
        val scoredList =
            anomalies.map { anomaly ->
                val imp = getImportance(anomaly.type, anomaly.amount)
                val conf = getConfidence(anomaly.type)
                val nov = getNovelty(anomaly.type, previousAnomalies)
                val act = getActionability(anomaly.type)
                val prem = getPremiumValue(anomaly.type)

                val score = (imp * 0.35) + (conf * 0.20) + (nov * 0.15) + (act * 0.20) + (prem * 0.10)
                ScoredAnomaly(anomaly, score)
            }

        return scoredList
            .filter { it.score >= 7.0 }
            .sortedWith(
                compareByDescending<ScoredAnomaly> { it.score }
                    .thenByDescending { abs(it.anomaly.amount) },
            )
            .map { it.anomaly }
            .take(4)
    }

    private fun getImportance(
        type: String,
        amount: Double,
    ): Int {
        return when (type) {
            "SALARY_LOSS" -> 9
            "RENT_RECOVERY_RISK" -> 9
            "MISSING_ALLOWANCE" -> 8
            "DEBIT_RECOVERY" -> 8
            "ARREARS_AUDIT" -> 8
            "TPTA_ENTITLEMENT" -> 7
            "DEDUCTION_SPIKE" -> 7
            "DSOP_COMPLIANCE" -> if (amount > 0.0) 7 else 5
            "DSOP_MILESTONE" -> 6
            "TAX_PROJECTION" -> 6
            else -> 5
        }
    }

    private fun getConfidence(type: String): Int {
        return when (type) {
            "SALARY_LOSS", "DEBIT_RECOVERY", "ARREARS_AUDIT", "TPTA_ENTITLEMENT", "DSOP_COMPLIANCE", "DSOP_MILESTONE" -> 10
            "MISSING_ALLOWANCE", "DEDUCTION_SPIKE", "TAX_PROJECTION" -> 9
            "RENT_RECOVERY_RISK" -> 8
            else -> 8
        }
    }

    private fun getNovelty(
        type: String,
        previousAnomalies: List<Anomaly>,
    ): Int {
        val wasPresent = previousAnomalies.any { it.type == type }
        return if (wasPresent) 2 else 10
    }

    private fun getActionability(type: String): Int {
        return when (type) {
            "RENT_RECOVERY_RISK" -> 10
            "TPTA_ENTITLEMENT" -> 9
            "DSOP_COMPLIANCE" -> 9
            "SALARY_LOSS", "MISSING_ALLOWANCE" -> 8
            "DEBIT_RECOVERY", "DEDUCTION_SPIKE" -> 7
            "TAX_PROJECTION" -> 6
            "ARREARS_AUDIT", "DSOP_MILESTONE" -> 5
            else -> 5
        }
    }

    private fun getPremiumValue(type: String): Int {
        return when (type) {
            "SALARY_LOSS", "RENT_RECOVERY_RISK" -> 10
            "MISSING_ALLOWANCE", "ARREARS_AUDIT" -> 9
            "TPTA_ENTITLEMENT", "DEBIT_RECOVERY", "DSOP_COMPLIANCE" -> 8
            "DEDUCTION_SPIKE" -> 7
            "DSOP_MILESTONE", "TAX_PROJECTION" -> 6
            else -> 6
        }
    }
}
