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

/**
 * "Proven" per the Pay Audit vision statement: the payslips supply both the expected and actual amount,
 * and the finding cites a verified authority ([PayAuthorities]). A type in [REPRESENTATION_DRAFT_TYPES]
 * that lacks one of these on a given instance (e.g. a [AnomalyTierMap.SALARY_LOSS] heuristic with no
 * evidence fields, or a [AnomalyTierMap.MISSING_ALLOWANCE] finding with no cited authority yet) does not
 * generate a representation draft for that instance.
 */
fun Anomaly.isProven(): Boolean = expected != null && actual != null && authority != null
