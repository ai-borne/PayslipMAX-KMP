package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.ReminderKind

/**
 * Copy for the Part II Due list and the card shown after a payslip import (docs/Plan/12_Part2Orders_PhasePlan.md, Phase 7a). Kept apart from
 * [Part2Strings] so that file does not near the 300-line limit. The words are generic: they say what to do, never a code, an order number or
 * a date; a date appears only through [dueLine], which is handed one. Phase 7b's notification text lives here too.
 */
object Part2DueStrings {
    // The Part II home row.
    const val rowTitle = "Due"
    const val rowNothingDue = "Nothing is due right now."

    fun rowDue(count: Int): String = if (count == 1) "1 thing is due." else "$count things are due."

    // The Due screen.
    const val screenTitle = "Due"
    const val screenSubtitle = "Reminders that come from your printed slips"
    const val empty = "Nothing is due. Reminders appear here after you print a slip."
    const val dueHeading = "Due now"
    const val upcomingHeading = "Coming up"
    const val unreadable = "Some items on this device could not be read, so reminders for them are not shown."
    const val untitled = "A slip"

    /** What the officer is asked to do. [year] is the leave year of a year-end reminder. */
    fun action(
        kind: ReminderKind,
        year: Int?,
    ): String =
        when (kind) {
            ReminderKind.RETURN -> "Draft your rejoining slip"
            ReminderKind.ADD_ORDER -> "Add the published order"
            ReminderKind.YEAR_END -> if (year == null) "Draft the year-end leave accumulation" else "Draft the year-end leave accumulation for $year"
        }

    fun dueLine(
        date: String,
        overdue: Boolean,
    ): String = if (overdue) "Overdue since $date" else "Due $date"

    // The card on the Dashboard after an import.
    const val cardTitle = "Part II reminders"
    const val cardOpen = "See what is due"
    const val cardNotNow = "Not now"

    fun cardBody(count: Int): String = if (count == 1) "1 reminder from your slips is due." else "$count reminders from your slips are due."
}
