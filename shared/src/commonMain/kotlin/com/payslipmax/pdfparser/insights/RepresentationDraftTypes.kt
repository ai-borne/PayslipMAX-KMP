package com.payslipmax.pdfparser.insights

/**
 * SSOT: which [Anomaly.type]s are provable/actionable enough that
 * [com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository] auto-generates a representation
 * draft for them. `composeApp`'s Smart Insights UI (`SmartInsightsBuilder`,
 * `RecommendedActions`) imports this same constant so a card routes to the Claim-Generator screen exactly
 * when a draft was actually generated — one definition, not two kept in sync by hand.
 *
 * [AnomalyTierMap.SALARY_LOSS] is deliberately excluded (P7-10, decided 2026-09-27): it is a bare net-pay
 * heuristic with no [Anomaly.expected]/[Anomaly.actual]/[Anomaly.authority] fields at all — nothing about
 * it is ever provable, so it belongs outside this set rather than sitting in it and silently never firing
 * a draft (which is what Phase 5's `isProven()` gate already did to it in practice; this makes that
 * explicit instead of implicit). [AnomalyTierMap.MISSING_ALLOWANCE] stays in — its HRA/MSP-Level-14 rules
 * are structural and in principle citable, it just has no verified [PayAuthorities] entry yet.
 */
val REPRESENTATION_DRAFT_TYPES =
    setOf(
        AnomalyTierMap.MISSING_ALLOWANCE,
        AnomalyTierMap.TPTA_ENTITLEMENT,
        AnomalyTierMap.INCREMENT_MISSED,
        AnomalyTierMap.MSP_SHORTFALL,
    )

/**
 * "Proven" per the Pay Audit vision statement: the payslips supply both the expected and actual amount,
 * and the finding cites a verified authority ([PayAuthorities]). A type in [REPRESENTATION_DRAFT_TYPES]
 * that lacks one of these on a given instance (e.g. a [AnomalyTierMap.MISSING_ALLOWANCE] finding with no
 * cited authority yet) does not generate a representation draft for that instance.
 */
fun Anomaly.isProven(): Boolean = expected != null && actual != null && authority != null
