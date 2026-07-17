package com.payslipmax.pdfparser.insights

/** Monetization tier of a detected [Anomaly] (D6 of the PRO Monetization Rollout). */
enum class AnomalyTier { FREE, PRO }

/**
 * Single source of truth mapping an [Anomaly.type] string to its [AnomalyTier].
 *
 * D6: the two most critical, trust-building checks stay FREE ([SALARY_LOSS], [DEDUCTION_SPIKE]);
 * every high-value auditor is PRO and gated behind
 * [com.payslipmax.pdfparser.subscription.FeatureGate.ANOMALY_DETECTION].
 *
 * The engine stays gate-agnostic — gating is a caller concern (the display layer partitions on this
 * map). Unknown types default to [AnomalyTier.PRO] (fail-closed: a new auditor that ships an
 * unclassified type is locked, never leaked for free, until it is deliberately added here).
 */
object AnomalyTierMap {
    const val SALARY_LOSS = "SALARY_LOSS"
    const val DEDUCTION_SPIKE = "DEDUCTION_SPIKE"
    const val MISSING_ALLOWANCE = "MISSING_ALLOWANCE"
    const val TPTA_ENTITLEMENT = "TPTA_ENTITLEMENT"
    const val ARREARS_AUDIT = "ARREARS_AUDIT"
    const val DSOP_COMPLIANCE = "DSOP_COMPLIANCE"
    const val DSOP_MILESTONE = "DSOP_MILESTONE"
    const val TAX_PROJECTION = "TAX_PROJECTION"
    const val RENT_RECOVERY_RISK = "RENT_RECOVERY_RISK"
    const val DEBIT_RECOVERY = "DEBIT_RECOVERY"

    val tiers: Map<String, AnomalyTier> =
        mapOf(
            SALARY_LOSS to AnomalyTier.FREE,
            DEDUCTION_SPIKE to AnomalyTier.FREE,
            MISSING_ALLOWANCE to AnomalyTier.PRO,
            TPTA_ENTITLEMENT to AnomalyTier.PRO,
            ARREARS_AUDIT to AnomalyTier.PRO,
            DSOP_COMPLIANCE to AnomalyTier.PRO,
            DSOP_MILESTONE to AnomalyTier.PRO,
            TAX_PROJECTION to AnomalyTier.PRO,
            RENT_RECOVERY_RISK to AnomalyTier.PRO,
            DEBIT_RECOVERY to AnomalyTier.PRO,
        )

    fun tierFor(type: String): AnomalyTier = tiers[type] ?: AnomalyTier.PRO
}
