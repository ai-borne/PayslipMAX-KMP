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
    const val CANCELS_CODE_UNKNOWN = "cancels_code_unknown"

    /** The slot CANCELS types the occurrence's code in, and the optional date of the occurrence only the time bar reads. */
    const val CANCELLED_CODE = "cancelled_code"
    const val OCCURRENCE_ON = "occurrence_on"

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
            checkRule(CANCELS_CODE_UNKNOWN, ::codeUnknown),
        )

    private fun quoted(id: String) = checkRule(id) { it.request.values[CANCELLED] !is SlotValue.RefValue }

    /**
     * The occurrence is more than two years older than [RuleContext.today]. Para 13 counts from the occurrence; an order is never published
     * before it, so the earlier of the occurrence date the officer typed (optional) and the date of the order being cancelled decides.
     * Nothing to go on, no real date or no device date judges nothing.
     */
    private fun timeBarred(context: RuleContext): Boolean {
        val today = context.today ?: return false
        val earliest = listOfNotNull(orderDate(context.request.values[CANCELLED]), dateOf(context.request.values[OCCURRENCE_ON])).minByOrNull { CivilCalendar.dayNumber(it) } ?: return false
        return CivilCalendar.dayNumber(today) > CivilCalendar.dayNumber(anniversary(earliest))
    }

    private fun orderDate(value: SlotValue?): CalendarDate? = (value as? SlotValue.RefValue)?.ref?.date?.takeIf { CivilCalendar.exists(it) }

    private fun dateOf(value: SlotValue?): CalendarDate? = (value as? SlotValue.DateValue)?.date?.takeIf { CivilCalendar.exists(it) }

    /** The code the officer typed for CANCELS is not one the manual lists or the slip prints. Nothing typed, not text, or no list judges nothing. */
    private fun codeUnknown(context: RuleContext): Boolean {
        val known = context.knownCodes ?: return false
        val typed = (context.request.values[CANCELLED_CODE] as? SlotValue.TextValue)?.text ?: return false
        return typed.trim() !in known
    }

    /** The same date [TIME_BAR_YEARS] years later; 29 February, which then does not exist, falls on 28 February. */
    private fun anniversary(date: CalendarDate): CalendarDate {
        val later = CalendarDate(date.year + TIME_BAR_YEARS, date.month, date.day)
        return if (CivilCalendar.exists(later)) later else later.copy(day = date.day - 1)
    }
}
