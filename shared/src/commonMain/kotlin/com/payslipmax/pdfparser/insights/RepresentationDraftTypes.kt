package com.payslipmax.pdfparser.insights

/**
 * SSOT: which [Anomaly.type]s are provable/actionable enough that
 * [com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository] auto-generates a representation
 * draft for them. `composeApp`'s Smart Insights UI (`SmartInsightsBuilder`,
 * `RecommendedActions`) imports this same constant so a card routes to the Claim-Generator screen exactly
 * when a draft was actually generated — one definition, not two kept in sync by hand.
 */
val REPRESENTATION_DRAFT_TYPES =
    setOf(
        AnomalyTierMap.SALARY_LOSS,
        AnomalyTierMap.MISSING_ALLOWANCE,
        AnomalyTierMap.TPTA_ENTITLEMENT,
        AnomalyTierMap.INCREMENT_MISSED,
        AnomalyTierMap.MSP_SHORTFALL,
    )
