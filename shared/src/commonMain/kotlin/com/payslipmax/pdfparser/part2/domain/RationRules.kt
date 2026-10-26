package com.payslipmax.pdfparser.part2.domain

/**
 * The rule family for the ration allowance around leave (Part II plan, phase 2b): the ration four apply to an officer on ration
 * strength (ration notes 1.1, 1.5, 3 and 4, pp.97-98), who says so in the leave form. The id is declared in `part2/rules.json`.
 */
object RationRules {
    const val ON_RATION = "on_ration"

    /** The yes/no answer the leave form gives for "Are you on ration strength?". */
    const val ANSWER_ON_RATION = "ration"

    /** Leave of this many days or more is left to the unit's quarterly order (p.99 note 5). */
    private const val LONG_LEAVE_DAYS = 180
    private const val DAYS = "days"

    const val ON_RATION_UNDER_180 = "on_ration_under_180"
    const val RATION_LONG_LEAVE = "ration_long_leave"

    val all: List<Part2Rule> =
        listOf(
            conditionRule(ON_RATION) { ANSWER_ON_RATION in it.request.answers },
            conditionRule(ON_RATION_UNDER_180) { ANSWER_ON_RATION in it.request.answers && (it.occurrence.days(DAYS) ?: 0) < LONG_LEAVE_DAYS },
            checkRule(RATION_LONG_LEAVE) { ANSWER_ON_RATION in it.request.answers && (it.occurrence.days(DAYS) ?: 0) >= LONG_LEAVE_DAYS },
        )

    /** The form's one ration question; its wording is `Part2Strings`' under this id. */
    val questions: List<AnswerQuestion> = listOf(AnswerQuestion.YesNo("ration", ANSWER_ON_RATION, setOf(ON_RATION, ON_RATION_UNDER_180, RATION_LONG_LEAVE)))
}
