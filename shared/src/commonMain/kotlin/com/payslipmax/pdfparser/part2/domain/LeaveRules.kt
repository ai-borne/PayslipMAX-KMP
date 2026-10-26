package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * The rule family for leave orders: the manual's computable notes on annual and casual leave and on quoting the grant order (Part II
 * plan, phase 2b). A family may name codes and slot ids, which the engine must not; the ids here are the ones `part2/rules.json`
 * declares and `part2/templates/` cite, and a contract test keeps all three equal. Severity and citation live in the declaration.
 */
object LeaveRules {
    const val AL_YEAR_CAP = "al_year_cap"
    const val AL_AFFIX_LIMIT = "al_affix_limit"
    const val CL_COMBINATION = "cl_combination"
    const val SPCL_OVER_30 = "spcl_over_30"
    const val GRANT_QUOTED_ANNUAL = "grant_quoted_annual"
    const val GRANT_QUOTED_REJOIN = "grant_quoted_rejoin"
    const val GRANT_QUOTED_RECALL = "grant_quoted_recall"

    /** The yes/no answers behind the NCC, re-employed and same-year entitlements of note 3 (p.115). */
    const val ANSWER_NCC_REEMPLOYED = "ncc_reemployed"
    const val ANSWER_REEMPLOYED_SAME_YEAR = "reemployed_same_year"

    private const val ANNUAL_CAP_DAYS = 60
    private const val NCC_CAP_DAYS = 30
    private const val FEBRUARY = 2
    private const val JAN_LAST_DAY = 31
    private const val FEB_LAST_DAY_NON_LEAP = 28
    private const val RETIRE_EARLY_DAYS = 30
    private const val RETIRE_28_FEB_DAYS = 58
    private const val RETIRE_29_FEB_DAYS = 59
    private const val AFFIX_MAX_DAYS = 5
    private const val SPECIAL_CASUAL_MAX_DAYS = 30

    /** The answers the checks read, by slot id (the leave's own dates and year are also what the reminder rules read). */
    private const val DAYS = "days"
    private const val EARLIER_AL = "earlier_al"
    const val LEAVE_FROM = "leave_from"
    const val LEAVE_TO = "leave_to"
    private const val RETIRE_ON = "retire_on"
    private const val PREFIX_FIRST = "prefix_first"
    const val SUFFIX_LAST = "suffix_last"
    const val YEAR = "year"

    /** The slot a follow-up order (an extension, a rejoining, a recall) quotes its grant order in. */
    const val GRANT = "grant"
    const val CARRY_ORDER = "carry_order"

    /** The reference boxes that quote a published Part II order of the officer's vault, so the form offers the vault for them (and not for a Corps notification or a signal). */
    val VAULT_ORDER_SLOTS: Set<String> = setOf(GRANT, CARRY_ORDER)

    /** The questions behind note 3 (NCC and re-employed officers, p.115): both feed the annual-leave cap and the first also the furlough bar (p.119 note 7). Wording is `Part2Strings`'. */
    val questions: List<AnswerQuestion> =
        listOf(
            AnswerQuestion.YesNo("ncc_reemployed", ANSWER_NCC_REEMPLOYED, setOf(AL_YEAR_CAP, LongLeaveRules.FURLOUGH_NOT_NCC)),
            AnswerQuestion.YesNo("reemployed_same_year", ANSWER_REEMPLOYED_SAME_YEAR, setOf(AL_YEAR_CAP)),
        )

    /** The annual-leave grants. */
    val ANNUAL_LEAVE: Set<String> = setOf("GTDAL", "GTDALCF", "EXTNAL")

    /** The other Chapter 5 leave grants (phase 8). A casual leave may not be combined with any of them or with the annual set (p.117). */
    val OTHER_LEAVE: Set<String> =
        setOf("FURLEV", "EXTNFUR", "MTRNLV", "SPMTRNLV", "GTDCCL", "EOL", "TERLEV", "CHADPLVE", "PTRNLV", "PATLCHAD", "STDLEV", "STDLVAL", "STDLVFUR")

    /** The casual-leave grants, which carry the same dates but no annual-leave year. */
    val CASUAL_LEAVE: Set<String> = setOf("GTDCL", "GTDSPCL")

    val all: List<Part2Rule> =
        listOf(
            checkRule(AL_YEAR_CAP) { yearTotal(it) > yearCap(it.request) },
            checkRule(AL_AFFIX_LIMIT) { affixDays(it) > AFFIX_MAX_DAYS },
            checkRule(CL_COMBINATION) { context -> context.event.any { it.template.code in ANNUAL_LEAVE || it.template.code in OTHER_LEAVE } },
            checkRule(SPCL_OVER_30) { (it.occurrence.days(DAYS) ?: 0) > SPECIAL_CASUAL_MAX_DAYS },
            grantQuoted(GRANT_QUOTED_ANNUAL),
            grantQuoted(GRANT_QUOTED_REJOIN),
            grantQuoted(GRANT_QUOTED_RECALL),
        )

    private fun grantQuoted(id: String) = checkRule(id) { it.request.values[GRANT] !is SlotValue.RefValue }

    /** Days of this grant plus the leave the officer says was already taken this year. Nothing worked out yet counts as nothing. */
    private fun yearTotal(context: RuleContext): Int {
        val days = context.occurrence.days(DAYS) ?: return 0
        return days + ((context.request.values[EARLIER_AL] as? SlotValue.NumberValue)?.number ?: 0)
    }

    /**
     * The most annual leave the officer may have in the year (p.115): 60, or 30 for an NCC or re-employed officer (note 3; 60 again for one
     * retired and re-employed in the same year), and in the year of retirement no more than note 13 gives for the retirement date.
     */
    private fun yearCap(request: OccurrenceRequest): Int {
        val answers = request.answers
        val service = if (ANSWER_REEMPLOYED_SAME_YEAR in answers || ANSWER_NCC_REEMPLOYED !in answers) ANNUAL_CAP_DAYS else NCC_CAP_DAYS
        return minOf(service, retirementEntitlement(request) ?: ANNUAL_CAP_DAYS)
    }

    /** Note 13 (p.116): 31 January and February before its last day 30 days, the last day of February 58 (59 in a leap year), March on 60; nothing for earlier dates or another year. */
    private fun retirementEntitlement(request: OccurrenceRequest): Int? {
        val retire = (request.values[RETIRE_ON] as? SlotValue.DateValue)?.date?.takeIf { CivilCalendar.exists(it) } ?: return null
        val year = (request.values[YEAR] as? SlotValue.NumberValue)?.number
        if (year != null && year != retire.year) return null
        return when {
            retire.month > FEBRUARY -> ANNUAL_CAP_DAYS
            retire.month == FEBRUARY -> februaryEntitlement(retire)
            retire.day == JAN_LAST_DAY -> RETIRE_EARLY_DAYS
            else -> null
        }
    }

    private fun februaryEntitlement(date: CalendarDate): Int {
        if (CivilCalendar.nextDay(date)?.month == FEBRUARY) return RETIRE_EARLY_DAYS
        return if (date.day == FEB_LAST_DAY_NON_LEAP) RETIRE_28_FEB_DAYS else RETIRE_29_FEB_DAYS
    }

    /** Prefix days before the leave starts plus suffix days after it ends. A date inside the leave adds nothing. */
    private fun affixDays(context: RuleContext): Int {
        val from = dateOf(context, LEAVE_FROM)
        val to = dateOf(context, LEAVE_TO)
        val prefix = dateOf(context, PREFIX_FIRST)
        val suffix = dateOf(context, SUFFIX_LAST)
        val before = if (from != null && prefix != null) CivilCalendar.daysBetween(prefix, from).coerceAtLeast(0) else 0
        val after = if (to != null && suffix != null) CivilCalendar.daysBetween(to, suffix).coerceAtLeast(0) else 0
        return (before + after).toInt()
    }

    private fun dateOf(
        context: RuleContext,
        slot: String,
    ): CalendarDate? = (context.request.values[slot] as? SlotValue.DateValue)?.date?.takeIf { CivilCalendar.exists(it) }
}
