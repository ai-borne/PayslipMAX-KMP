package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * The rule family for cancelling a published order (Part II plan, phase 6c; ODP-2026 pp.226-227): CANCEL cancels every occurrence of an
 * order, CANCELS one occurrence. A family may name codes and slot ids, which the engine and the screens must not; the ids here are the
 * ones `part2/rules.json` declares and `part2/templates/cancel.json` cites. Severity and citation live in the declaration.
 */
object CancelRules {
    /** The code that cancels every occurrence of an order (CANCEL, p.226), and the one that cancels a single occurrence (CANCELS, p.227). Named for what they do so no file outside a family prints a code. */
    const val ALL_OCCURRENCES = "CANCEL"
    const val ONE_OCCURRENCE = "CANCELS"

    /** The slot both codes quote the cancelled order in (its number and date). */
    const val CANCELLED = "cancelled"

    const val CANCEL_QUOTED = "cancel_quoted"
    const val CANCEL_TIME_BARRED = "cancel_time_barred"
    const val CANCELS_QUOTED = "cancels_quoted"
    const val CANCELS_TIME_BARRED = "cancels_time_barred"

    /** The two codes that cancel an order; a slip holding either is a cancel slip. */
    val CODES: Set<String> = setOf(ALL_OCCURRENCES, ONE_OCCURRENCE)

    /** The reference boxes that quote a published Part II order of the officer's vault, so the form offers the vault for them. */
    val VAULT_ORDER_SLOTS: Set<String> = setOf(CANCELLED)

    /** Notes 4 (p.226) and 6 (p.227): a time-bar sanction may be enclosed for an occurrence more than this many years old. */
    private const val TIME_BAR_YEARS = 2

    val all: List<Part2Rule> =
        listOf(
            quoted(CANCEL_QUOTED),
            checkRule(CANCEL_TIME_BARRED, ::timeBarred),
            quoted(CANCELS_QUOTED),
            checkRule(CANCELS_TIME_BARRED, ::timeBarred),
        )

    private fun quoted(id: String) = checkRule(id) { it.request.values[CANCELLED] !is SlotValue.RefValue }

    /** The order being cancelled is more than two years older than [RuleContext.today]. Nothing quoted, no real date or no device date judges nothing. */
    private fun timeBarred(context: RuleContext): Boolean {
        val today = context.today ?: return false
        val date = (context.request.values[CANCELLED] as? SlotValue.RefValue)?.ref?.date?.takeIf { CivilCalendar.exists(it) } ?: return false
        return CivilCalendar.dayNumber(today) > CivilCalendar.dayNumber(anniversary(date))
    }

    /** The same date [TIME_BAR_YEARS] years later; 29 February, which then does not exist, falls on 28 February. */
    private fun anniversary(date: CalendarDate): CalendarDate {
        val later = CalendarDate(date.year + TIME_BAR_YEARS, date.month, date.day)
        return if (CivilCalendar.exists(later)) later else later.copy(day = date.day - 1)
    }
}
