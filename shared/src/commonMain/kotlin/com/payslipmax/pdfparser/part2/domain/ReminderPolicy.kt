package com.payslipmax.pdfparser.part2.domain

/**
 * The one home for the reminder cadence (DECISIONS Round 10): every reminder is at [hour] o'clock where the officer is; "add the published
 * order" starts [addOrderFirstDays] days after printing and repeats every [addOrderEveryDays] days, at most [addOrderMax] times; the
 * year-end reminder is on [yearEndDay] of month [yearEndMonth] of the next year (note 7 p.134). [upcomingMax] is how many future
 * reminders are planned at a time, the nearest first (an operating system holds only so many). Changing a number here changes the planner
 * and nothing else; neither a screen nor a scheduler holds a copy.
 */
data class ReminderPolicy(
    val hour: Int = 9,
    val addOrderFirstDays: Int = 14,
    val addOrderEveryDays: Int = 30,
    val addOrderMax: Int = 6,
    val yearEndMonth: Int = 3,
    val yearEndDay: Int = 1,
    val upcomingMax: Int = 20,
)
