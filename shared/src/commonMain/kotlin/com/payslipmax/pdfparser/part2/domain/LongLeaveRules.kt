package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * The rule family for the rest of Chapter 5 (Part II plan, phase 8): furlough, terminal, child-care and adoption leave, study leave
 * and its extensions, overstayal and absence. A family may name codes and slot ids, which the engine must not; the ids here are the
 * ones `part2/rules.json` declares and `part2/templates/ch5_*.json` cite. Severity and citation live in the declaration.
 */
object LongLeaveRules {
    const val FURLOUGH_CYCLE = "furlough_cycle"
    const val FURLOUGH_NOT_NCC = "furlough_not_ncc"
    const val CCL_LIMITS = "ccl_limits"
    const val TERMINAL_LEAVE_28 = "terminal_leave_28"
    const val TERMINAL_WITH_ANNUAL = "terminal_with_annual"
    const val ADOPTION_LEAVE_180 = "adoption_leave_180"
    const val PATERNITY_ADOPTION_15 = "paternity_adoption_15"
    const val GRANT_QUOTED_FURLOUGH = "grant_quoted_furlough"
    const val GRANT_QUOTED_STUDY = "grant_quoted_study"
    const val GRANT_QUOTED_OVERSTAY = "grant_quoted_overstay"
    const val DECLARED_QUOTED_OVERSTAY = "declared_quoted_overstay"
    const val DECLARED_QUOTED_AWL = "declared_quoted_awl"
    const val REISSUE_ALLOWANCES = "reissue_allowances"

    /** The yes/no answer for "did the earlier order also grant or stop ration or Risk & Hardship allowance?". */
    const val ANSWER_EARLIER_ALLOWANCES = "earlier_allowances"

    /** The reference boxes that quote a published Part II order of the officer's vault: the order that declared an overstayal or an absence, and the one that notified an adoption. */
    val VAULT_ORDER_SLOTS: Set<String> = setOf(DECLARED, ADOPTION)

    /** The slots the checks read (the leave's own dates and day count are [LeaveRules]'). */
    const val DECLARED = "declared"
    const val ADOPTION = "adoption"
    private const val DAYS = "days"
    private const val CYCLE_FROM = "cycle_from"
    private const val CYCLE_TO = "cycle_to"
    private const val CCL_YEAR_DAYS = "ccl_year_days"

    /** Years a furlough cycle spans, counted from its first year to its last (p.118 note 4: the three-year cycle). */
    private const val CYCLE_LAST_OFFSET = 2

    /** Child care leave (p.120 note 12.7): the shortest and longest spell, and the most in a calendar year. */
    private const val CCL_SPELL_MIN = 5
    private const val CCL_SPELL_MAX = 30
    private const val CCL_YEAR_MAX = 60

    /** The days the narrative prints for terminal leave (p.118), child adoption leave (p.122) and paternity leave on adoption (p.124). */
    private const val TERMINAL_DAYS = 28
    private const val ADOPTION_DAYS = 180
    private const val PATERNITY_ADOPTION_DAYS = 15

    /** The form's question for a follow-up that moves the return day; its wording is `Part2Strings`' under this id. */
    val questions: List<AnswerQuestion> = listOf(AnswerQuestion.YesNo("earlier_allowances", ANSWER_EARLIER_ALLOWANCES, setOf(REISSUE_ALLOWANCES)))

    val all: List<Part2Rule> =
        listOf(
            checkRule(FURLOUGH_CYCLE, ::cycleBroken),
            checkRule(FURLOUGH_NOT_NCC) { LeaveRules.ANSWER_NCC_REEMPLOYED in it.request.answers },
            checkRule(CCL_LIMITS, ::cclBroken),
            checkRule(TERMINAL_LEAVE_28, ::terminalSpanWrong),
            checkRule(TERMINAL_WITH_ANNUAL, ::terminalNotFromAnnualStart),
            fixedDays(ADOPTION_LEAVE_180, ADOPTION_DAYS),
            fixedDays(PATERNITY_ADOPTION_15, PATERNITY_ADOPTION_DAYS),
            notQuoted(GRANT_QUOTED_FURLOUGH, LeaveRules.GRANT),
            notQuoted(GRANT_QUOTED_STUDY, LeaveRules.GRANT),
            notQuoted(GRANT_QUOTED_OVERSTAY, LeaveRules.GRANT),
            notQuoted(DECLARED_QUOTED_OVERSTAY, DECLARED),
            notQuoted(DECLARED_QUOTED_AWL, DECLARED),
            checkRule(REISSUE_ALLOWANCES) { ANSWER_EARLIER_ALLOWANCES in it.request.answers },
        )

    private fun notQuoted(
        id: String,
        slot: String,
    ) = checkRule(id) { it.request.values[slot] !is SlotValue.RefValue }

    /** The days the dates span must be exactly the days the narrative prints; nothing worked out yet is no breach. */
    private fun fixedDays(
        id: String,
        days: Int,
    ) = checkRule(id) { context -> context.occurrence.days(DAYS)?.let { it != days } ?: false }

    private fun numberOf(
        context: RuleContext,
        slot: String,
    ): Int? = (context.request.values[slot] as? SlotValue.NumberValue)?.number

    /** p.118 note 4: not a three-year cycle, or a leave that starts outside it. A cycle or leave date not given yet judges nothing. */
    private fun cycleBroken(context: RuleContext): Boolean {
        val first = numberOf(context, CYCLE_FROM) ?: return false
        val last = numberOf(context, CYCLE_TO) ?: return false
        if (last - first != CYCLE_LAST_OFFSET) return true
        val start = (context.request.values[LeaveRules.LEAVE_FROM] as? SlotValue.DateValue)?.date ?: return false
        return start.year !in first..last
    }

    /** p.120 note 12.7: a spell under 5 or over 30 days, or more than 60 days in the year counting this leave. */
    private fun cclBroken(context: RuleContext): Boolean {
        val spell = context.occurrence.days(DAYS)?.let { it < CCL_SPELL_MIN || it > CCL_SPELL_MAX } ?: false
        val year = numberOf(context, CCL_YEAR_DAYS)?.let { it > CCL_YEAR_MAX } ?: false
        return spell || year
    }

    /** The dates of terminal leave span 28 days plus the annual leave granted beside it (p.118 and p.119 note 10). */
    private fun terminalSpanWrong(context: RuleContext): Boolean {
        val span = context.occurrence.days(DAYS) ?: return false
        val annual = context.event.filter { it.template.code in LeaveRules.ANNUAL_LEAVE }.sumOf { it.days(DAYS) ?: 0 }
        return span - annual != TERMINAL_DAYS
    }

    /** p.119 note 10: beside annual leave, terminal leave starts on the day the annual leave commences. */
    private fun terminalNotFromAnnualStart(context: RuleContext): Boolean {
        val start = (context.request.values[LeaveRules.LEAVE_FROM] as? SlotValue.DateValue)?.date ?: return false
        val annual = context.event.filter { it.template.code in LeaveRules.ANNUAL_LEAVE }.mapNotNull { it.from }.minByOrNull { CivilCalendar.dayNumber(it) } ?: return false
        return start != annual
    }
}
