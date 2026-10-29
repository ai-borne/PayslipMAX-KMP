package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.AbsenceRules

/**
 * Copy for the area questions of the leave form (docs/Plan/12_Part2Orders_PhasePlan.md, Phase 9; ODP-2026 pp.49, 53, 55 and 70-71): how the
 * form words the question that lets the officer name their own HAA, SCCIA, field-area and Transport Allowance code. Kept apart from
 * [Part2Strings] (near its line limit). The R&H question is still [Part2Strings]'.
 */
object Part2AreaStrings {
    /** What the area check says, by the rule's id; null for an id that is not this file's. */
    fun finding(ruleId: String): String? =
        when (ruleId) {
            AbsenceRules.FIELD_SHORT_ABSENCE ->
                "Fewer than 15 days away. After a hospital stay this short the field area allowance is not re-granted, so no order is needed. After a course, go on."
            else -> null
        }

    /** The wording of an area question by the id its rule family gave it; the next copy file's wording for any other id. */
    fun question(id: String): String =
        when (id) {
            "haa_code" -> "Which High Altitude Allowance (HAA) category do you draw?"
            "sccia_code" -> "Which Special Compensatory Counter Insurgency Allowance (SCCIA) area do you serve in?"
            "field_code" -> "Which field-area allowance do you draw?"
            "tran_code" -> "Which Transport Allowance category do you draw?"
            else -> Part2LongLeaveStrings.question(id)
        }

    /**
     * The words for an option whose serial name does not tell it apart. The five Transport Allowance codes share one printed serial (3.53),
     * so each takes its category from the manual's note i (p.70); every other area code is told apart by its own serial name, and has none here.
     */
    fun optionName(code: String): String? =
        when (code) {
            "GTDTRAN1" -> "Category 1: lower rate"
            "GTDTRAN2" -> "Category 2: higher rate"
            "GTDTRAN3" -> "Category 3: with disability, lower rate"
            "GTDTRAN4" -> "Category 4: with disability, higher rate"
            "GTDTRAN5" -> "Category 5: Maj Gen and above"
            else -> null
        }
}
