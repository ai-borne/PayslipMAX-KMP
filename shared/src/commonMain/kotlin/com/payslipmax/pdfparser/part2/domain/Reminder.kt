package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate

/** What a reminder asks the officer to do. The words are the screens' (`Part2DueStrings`), never a code or a date. */
enum class ReminderKind {
    /** Draft the rejoining slip: the leave has ended (the return date). */
    RETURN,

    /** Add the published order to the vault: the slip was printed and no numbered, dated order is linked to it. */
    ADD_ORDER,

    /** Draft the year-end accumulation for a leave year (note 7 p.134). */
    YEAR_END,
}

/**
 * One reminder the planner works out from the drafts and the vault: [kind], the draft to open ([draftId]), the instant it is due ([at],
 * epoch milliseconds, [ReminderPolicy.hour] o'clock where the officer is) and that local day ([dueOn]). [sequence] counts the repeats of
 * [ReminderKind.ADD_ORDER] from 0; [year] is the leave year of [ReminderKind.YEAR_END]. [key] names the reminder and survives a rebuild, so a
 * scheduler can replace by key. [toString] hides everything: a reminder's draft and date must not reach a log through a string template.
 */
data class Reminder(
    val kind: ReminderKind,
    val draftId: String,
    val at: Long,
    val dueOn: CalendarDate,
    val sequence: Int = 0,
    val year: Int? = null,
) {
    val key: String
        get() =
            when (kind) {
                ReminderKind.RETURN -> "return:$draftId"
                ReminderKind.ADD_ORDER -> "add-order:$draftId:$sequence"
                ReminderKind.YEAR_END -> "year-end:$year"
            }

    override fun toString(): String = "Reminder($kind)"
}

/** A reminder that is due now; [overdue] is true when its day has passed (the planner says, so no screen compares dates). */
data class DueReminder(
    val reminder: Reminder,
    val overdue: Boolean,
)

/**
 * What the planner answers: [due] is what the officer has to do now, the oldest first; [upcoming] is what comes next, the nearest first and
 * no more than [ReminderPolicy.upcomingMax]. A scheduler rebuilds from [upcoming] alone.
 */
data class ReminderPlan(
    val due: List<DueReminder>,
    val upcoming: List<Reminder>,
) {
    override fun toString(): String = "ReminderPlan(due=${due.size}, upcoming=${upcoming.size})"

    companion object {
        val Empty = ReminderPlan(emptyList(), emptyList())
    }
}
