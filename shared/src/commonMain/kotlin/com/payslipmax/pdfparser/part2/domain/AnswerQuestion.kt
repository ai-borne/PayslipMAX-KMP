package com.payslipmax.pdfparser.part2.domain

/**
 * A question the leave form asks the officer whose answer a rule condition reads (for example "Are you on ration strength?"). The
 * question's wording is app copy and lives in `Part2Strings`, found by [id]; the options of a [OneOf] are manual codes. A rule family
 * owns its questions, so a new family adds its own and the form and the engine stay as they are. The [rules] are the condition ids
 * that read the answer: a question is asked for a code exactly when one of its links names one of them.
 */
sealed interface AnswerQuestion {
    val id: String
    val rules: Set<String>

    /** Answered yes by holding [answer] in a request's answers. */
    data class YesNo(
        override val id: String,
        val answer: String,
        override val rules: Set<String>,
    ) : AnswerQuestion

    /** Answered by one of [options], held as [prefix] followed by the option; no option held means none. */
    data class OneOf(
        override val id: String,
        val prefix: String,
        val options: List<String>,
        override val rules: Set<String>,
    ) : AnswerQuestion
}
