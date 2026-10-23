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
    const val GRANT_QUOTED_ANNUAL = "grant_quoted_annual"
    const val GRANT_QUOTED_REJOIN = "grant_quoted_rejoin"
    const val GRANT_QUOTED_RECALL = "grant_quoted_recall"

    private const val ANNUAL_CAP_DAYS = 60
    private const val AFFIX_MAX_DAYS = 5

    /** The answers the checks read, by slot id. */
    private const val DAYS = "days"
    private const val EARLIER_AL = "earlier_al"
    private const val LEAVE_FROM = "leave_from"
    private const val LEAVE_TO = "leave_to"
    private const val PREFIX_FIRST = "prefix_first"
    private const val SUFFIX_LAST = "suffix_last"
    private const val GRANT = "grant"

    /** Leave a casual leave may not be combined with. This batch holds the annual set; the rest of Chapter 5 joins it with its templates. */
    private val otherLeave = setOf("GTDAL", "GTDALCF", "EXTNAL")

    val all: List<Part2Rule> =
        listOf(
            checkRule(AL_YEAR_CAP) { yearTotal(it) > ANNUAL_CAP_DAYS },
            checkRule(AL_AFFIX_LIMIT) { affixDays(it) > AFFIX_MAX_DAYS },
            checkRule(CL_COMBINATION) { context -> context.event.any { it.template.code in otherLeave } },
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
