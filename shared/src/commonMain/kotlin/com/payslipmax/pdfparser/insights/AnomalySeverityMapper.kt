package com.payslipmax.pdfparser.insights

/**
 * Single source of truth mapping an [Anomaly.type] string to its [InsightSeverity].
 *
 * Behavior-preserving: reproduces the map that previously lived inline in
 * `FinancialIntelligenceRepository.mapAnomalyTypeToSeverity()`. `OPPORTUNITY` is never produced here —
 * it is assigned to `WealthOptimization` opportunities by the UI-layer builder, not by this anomaly map.
 */
object AnomalySeverityMapper {
    fun severityOf(anomalyType: String): InsightSeverity =
        when (anomalyType) {
            "SALARY_LOSS", "DSOP_COMPLIANCE", "RENT_RECOVERY_RISK", "DEBIT_RECOVERY" -> InsightSeverity.IMPORTANT
            "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "DEDUCTION_SPIKE", "TAX_PROJECTION" -> InsightSeverity.WARNING
            else -> InsightSeverity.INFO
        }
}

/**
 * Maps to the legacy persisted vocabulary stored in `FinancialInsightEntity.severity` so existing
 * encrypted rows remain readable without a schema migration.
 */
fun InsightSeverity.toPersistedString(): String =
    when (this) {
        InsightSeverity.IMPORTANT -> "CRITICAL"
        InsightSeverity.OPPORTUNITY -> "SUCCESS"
        else -> name
    }
