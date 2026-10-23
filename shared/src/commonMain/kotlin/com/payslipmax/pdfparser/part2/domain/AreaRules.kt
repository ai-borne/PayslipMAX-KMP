package com.payslipmax.pdfparser.part2.domain

/**
 * The rule family for allowances tied to the area an officer serves in. Today it holds Risk & Hardship only (Part II plan, phase 2b):
 * the stop from the first day away and the re-grant under the officer's own rate code, seen in practice and not in the manual. The
 * HAA, field and SCCIA re-grants join it with their templates. An officer in an R&H station names their rate code in the leave form;
 * the code is carried as an answer, so the family needs no template-specific input.
 */
object AreaRules {
    const val RH_STATION = "rh_station"

    /** The answer for an R&H officer is this prefix followed by their rate code, for example the prefix + the officer's GTDRH code. */
    const val ANSWER_RH_PREFIX = "rh_code:"

    /** The R&H grant codes, Annexure II serials 3.1 to 3.11: one condition each, so a link can name the officer's own. */
    val rhCodes: List<String> = listOf("GTDRH11", "GTDRH12", "GTDRH13", "GTDRH21", "GTDRH22", "GTDRH23", "GTDRH31", "GTDRH32", "GTDRH33", "GTDRH77", "GTDRH60")

    val all: List<Part2Rule> =
        listOf(conditionRule(RH_STATION) { context -> rhCodes.any { ANSWER_RH_PREFIX + it in context.request.answers } }) +
            rhCodes.map { code -> conditionRule("rh_" + code.lowercase()) { ANSWER_RH_PREFIX + code in it.request.answers } }
}
