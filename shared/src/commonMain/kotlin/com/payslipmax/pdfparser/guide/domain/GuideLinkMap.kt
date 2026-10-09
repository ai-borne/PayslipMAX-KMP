package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.insights.AnomalyTierMap

/**
 * The single table from a Pay Audit finding to the Claim Guide card that explains its rule (E7). Owner-approved
 * 2026-10-08; E9 (2026-10-09) moved the arrears rows and the HRA row to the new arrears and quarters cards; see
 * `docs/Plan/rule_cards/16_guide_phase_plan.md`. A (type, pay line) with no row has no link, so a
 * finding never opens a card nobody chose. Card ids are bundle data, checked against the real bundle by
 * `GuideBundleContract`; [decidedTypes] is checked against `PayAuditFindingTypes.TYPES`, so a new finding type
 * cannot ship without a decision.
 */
object GuideLinkMap {
    private const val HRA = "RB-SS-P114-quarters"
    private const val MSP = "RB-C13-05"
    private const val TRANSPORT_RATES = "RB-SS-P051-rates"
    private const val DA_ARREARS = "RB-RP-053-arrears"
    private const val ANNUAL_INCREMENT = "RB-C13-08"

    private val byType =
        mapOf(
            AnomalyTierMap.TPTA_ENTITLEMENT to TRANSPORT_RATES,
            AnomalyTierMap.ARREARS_AUDIT to DA_ARREARS,
            AnomalyTierMap.INCREMENT_MISSED to ANNUAL_INCREMENT,
            AnomalyTierMap.MSP_SHORTFALL to MSP,
        )

    private val missingAllowanceByField = mapOf("houseRentAllowance" to HRA, "militaryServicePay" to MSP)

    /** Pay Audit also shows an under-paid arrears as SALARY_LOSS on these lines (outside `PayAuditFindingTypes`). */
    private val arrearsLines = setOf("arrearsDa", "arrearsTptaDa")

    /** Finding types that have a mapping decision. */
    val decidedTypes: Set<String> = byType.keys + AnomalyTierMap.MISSING_ALLOWANCE

    /** Every card a finding can open. */
    val cardIds: Set<String> = byType.values.toSet() + missingAllowanceByField.values

    fun cardFor(
        type: String,
        field: String,
    ): String? =
        when (type) {
            AnomalyTierMap.MISSING_ALLOWANCE -> missingAllowanceByField[field]
            AnomalyTierMap.SALARY_LOSS -> DA_ARREARS.takeIf { field in arrearsLines }
            else -> byType[type]
        }
}
