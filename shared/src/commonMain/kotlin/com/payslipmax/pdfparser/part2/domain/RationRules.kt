package com.payslipmax.pdfparser.part2.domain

/**
 * The rule family for the ration allowance around leave (Part II plan, phase 2b): the ration four apply to an officer on ration
 * strength (ration notes 1.1, 1.5, 3 and 4, pp.97-98), who says so in the leave form. The id is declared in `part2/rules.json`.
 */
object RationRules {
    const val ON_RATION = "on_ration"

    /** The yes/no answer the leave form gives for "Are you on ration strength?". */
    const val ANSWER_ON_RATION = "ration"

    val all: List<Part2Rule> = listOf(conditionRule(ON_RATION) { ANSWER_ON_RATION in it.request.answers })

    /** The form's one ration question; its wording is `Part2Strings`' under this id. */
    val questions: List<AnswerQuestion> = listOf(AnswerQuestion.YesNo("ration", ANSWER_ON_RATION, setOf(ON_RATION)))
}
