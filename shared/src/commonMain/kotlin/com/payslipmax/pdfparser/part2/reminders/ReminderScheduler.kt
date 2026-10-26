package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.part2.domain.Reminder

/**
 * Hands the planner's reminders to the operating system. The platform keeps no date arithmetic and no rules of its own: it is given
 * `ReminderPlan.upcoming` (each reminder's instant is the planner's) and replaces what it holds, matching by [Reminder.key]. What it holds
 * is never the source of truth; the drafts are, and [ReminderSync] rebuilds it from them whenever they change.
 */
interface ReminderScheduler {
    /**
     * Makes the pending reminders exactly [reminders]: any pending one whose key is not listed is cancelled, a listed one is (re)placed at
     * its instant. An empty list cancels everything. Safe to call again with the same list.
     */
    fun replaceAll(reminders: List<Reminder>)
}
