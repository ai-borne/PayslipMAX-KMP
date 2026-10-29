package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * The rule family for an absence that is not a leave order (Part II plan, Phase 12; DECISIONS Round 44; ODP-2026 p.55 note 2). The field-area
 * allowances are re-granted on rejoining from leave, hospitalisation or a course, except after a hospital stay of fewer than 15 days. An officer
 * back from either drafts the re-grant from the field code itself; the app cannot tell a short stay in hospital from a short course, so it warns
 * and the officer decides. The 15 is [AreaRules.FIELD_CASUAL_MIN_DAYS], the one home of that number.
 */
object AbsenceRules {
    const val FIELD_SHORT_ABSENCE = "field_short_absence"

    val all: List<Part2Rule> = listOf(checkRule(FIELD_SHORT_ABSENCE) { context -> startedFromAFieldCode(context) && daysAway(context.request.values)?.let { it < AreaRules.FIELD_CASUAL_MIN_DAYS } == true })

    /** True for a draft started FROM a field-area code. The same code linked from a leave order has that leave's code on the request and is the leave's business, never judged here. */
    private fun startedFromAFieldCode(context: RuleContext) = context.request.code in AreaRules.fieldCodes

    /** The days from the first to the last day away, both counted, or null when either date is missing or the last is before the first. */
    private fun daysAway(values: Map<String, SlotValue>): Long? {
        val from = LeaveRules.dateIn(values, LeaveRules.LEAVE_FROM) ?: return null
        val to = LeaveRules.dateIn(values, LeaveRules.LEAVE_TO) ?: return null
        return (CivilCalendar.dayNumber(to) - CivilCalendar.dayNumber(from) + 1).takeIf { it >= 1 }
    }
}
