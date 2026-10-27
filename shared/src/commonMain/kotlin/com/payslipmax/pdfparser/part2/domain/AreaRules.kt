package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * The rule family for allowances tied to the area an officer serves in (Part II plan, phases 2b and 9). An officer names their own
 * code for each area they serve in (the form's questions below); the answer is carried as `prefix + code`, so the family needs no
 * template-specific input, and a leave order links to each code under that code's condition.
 *
 * - **R&H** (practice only): the stop from the first day away and the re-grant under the officer's own rate code.
 * - **HAA, SCCIA, field area** (manual, pp.49, 53, 55): a re-grant on rejoining from leave, and no stop (Round 9). HAA and the field
 *   allowances skip it after a short casual leave; SCCIA never skips it.
 * - **Transport Allowance** (manual, p.71 notes iii and v): STPTRAN and a re-grant only when the absence covers a full calendar month.
 */
object AreaRules {
    const val RH_STATION = "rh_station"

    /** An answer is its question's prefix followed by the officer's own code, for example the prefix + a GTDRH code. */
    const val ANSWER_RH_PREFIX = "rh_code:"
    const val ANSWER_HAA_PREFIX = "haa_code:"
    const val ANSWER_SCCIA_PREFIX = "sccia_code:"
    const val ANSWER_FIELD_PREFIX = "field_code:"
    const val ANSWER_TRAN_PREFIX = "tran_code:"

    /** The stop of the Transport Allowance, and the two dates the Transport Allowance rows print. */
    const val TRAN_STOP = "tran_stop"
    const val TRAN_STOP_ON = "tran_stop_on"
    const val TRAN_GRANT_ON = "tran_grant_on"

    /** A casual leave of fewer days than this draws no re-grant (HAA p.49 note ii: "less than 14 days"; field p.55 note 2: "less than 15 days"). The one home of both numbers. */
    const val HAA_CASUAL_MIN_DAYS = 14
    const val FIELD_CASUAL_MIN_DAYS = 15

    private const val DAYS = "days"
    private const val MONTHS = 12

    /** The R&H grant codes, Annexure II serials 3.1 to 3.11: one condition each, so a link can name the officer's own. */
    val rhCodes: List<String> = listOf("GTDRH11", "GTDRH12", "GTDRH13", "GTDRH21", "GTDRH22", "GTDRH23", "GTDRH31", "GTDRH32", "GTDRH33", "GTDRH77", "GTDRH60")

    /** HAA at the three categories, serials 3.13 to 3.15. */
    val haaCodes: List<String> = listOf("GTDHH11", "GTDHH31", "GTDHH32")

    /** SCCIA in a field, modified field and peace area, serials 3.19 to 3.21. */
    val scciaCodes: List<String> = listOf("GTDCIAFD", "GTDCIAMF", "GTDCIAPC")

    /** The field-area allowances, serials 3.23 to 3.25. */
    val fieldCodes: List<String> = listOf("GTDHAFA", "GTDCFAA", "GTDCMFA")

    /** The Transport Allowance grants at the five categories, serial 3.53. */
    val tranCodes: List<String> = listOf("GTDTRAN1", "GTDTRAN2", "GTDTRAN3", "GTDTRAN4", "GTDTRAN5")

    private fun ids(
        prefix: String,
        codes: List<String>,
    ) = codes.map { prefix + it.lowercase() }

    /** The form's area questions, one per area: the officer's own code or none. The wording is `Part2AreaStrings`' (and `Part2Strings`' for R&H) under the id. */
    val questions: List<AnswerQuestion> =
        listOf(
            AnswerQuestion.OneOf("rh_code", ANSWER_RH_PREFIX, rhCodes, setOf(RH_STATION) + ids("rh_", rhCodes)),
            AnswerQuestion.OneOf("haa_code", ANSWER_HAA_PREFIX, haaCodes, ids("haa_", haaCodes).toSet()),
            AnswerQuestion.OneOf("sccia_code", ANSWER_SCCIA_PREFIX, scciaCodes, ids("sccia_", scciaCodes).toSet()),
            AnswerQuestion.OneOf("field_code", ANSWER_FIELD_PREFIX, fieldCodes, ids("field_", fieldCodes).toSet()),
            AnswerQuestion.OneOf("tran_code", ANSWER_TRAN_PREFIX, tranCodes, ids("tran_", tranCodes).toSet() + TRAN_STOP),
        )

    val all: List<Part2Rule> =
        listOf(conditionRule(RH_STATION) { context -> rhCodes.any { ANSWER_RH_PREFIX + it in context.request.answers } }) +
            rhCodes.map { code -> conditionRule("rh_" + code.lowercase()) { ANSWER_RH_PREFIX + code in it.request.answers } } +
            haaCodes.map { regrant("haa_", ANSWER_HAA_PREFIX, it, HAA_CASUAL_MIN_DAYS) } +
            scciaCodes.map { regrant("sccia_", ANSWER_SCCIA_PREFIX, it, null) } +
            fieldCodes.map { regrant("field_", ANSWER_FIELD_PREFIX, it, FIELD_CASUAL_MIN_DAYS) } +
            tranCodes.map { code -> conditionRule("tran_" + code.lowercase()) { ANSWER_TRAN_PREFIX + code in it.request.answers && lostMonths(it.request.values) != null } } +
            listOf(
                conditionRule(TRAN_STOP) { context -> tranCodes.any { ANSWER_TRAN_PREFIX + it in context.request.answers } && lostMonths(context.request.values) != null },
                deriveRule(TRAN_STOP_ON) { context -> lostMonths(context.values)?.let { SlotValue.DateValue(it.stop) } },
                deriveRule(TRAN_GRANT_ON) { context -> lostMonths(context.values)?.let { SlotValue.DateValue(it.grant) } },
            )

    /** A re-grant under the officer's own [code]; after a casual leave of fewer than [casualMinDays] days (null: never) the allowance never stopped. */
    private fun regrant(
        idPrefix: String,
        answerPrefix: String,
        code: String,
        casualMinDays: Int?,
    ) = conditionRule(idPrefix + code.lowercase()) { answerPrefix + code in it.request.answers && !shortCasual(it, casualMinDays) }

    private fun shortCasual(
        context: RuleContext,
        minDays: Int?,
    ) = minDays != null && context.occurrence.template.code == LeaveRules.CASUAL && (context.occurrence.days(DAYS) ?: 0) < minDays

    /** The first day of the first calendar month the absence covers in full ([stop]) and the first day of the month after the last such month ([grant]). */
    private class LostMonths(
        val stop: CalendarDate,
        val grant: CalendarDate,
    )

    /**
     * The months of Transport Allowance an absence loses, or null when it covers none in full. The handbook (Pay & Allowances 2023, pp.124-125)
     * loses the allowance only for a calendar month wholly covered by leave and pays a part month in full, so the stop is dated the first of
     * the first whole month and the grant the first of the month after the last whole one. The absence runs from the earlier of the first
     * prefix day and the first day of leave to the later of the last day of leave and the last suffix day (Round 11). Every date is checked
     * against the calendar first, and nothing here loops.
     */
    private fun lostMonths(values: Map<String, SlotValue>): LostMonths? {
        val leaveFrom = LeaveRules.dateIn(values, LeaveRules.LEAVE_FROM) ?: return null
        val leaveTo = LeaveRules.dateIn(values, LeaveRules.LEAVE_TO) ?: return null
        val from = listOfNotNull(leaveFrom, LeaveRules.dateIn(values, LeaveRules.PREFIX_FIRST)).minBy { CivilCalendar.dayNumber(it) }
        val to = listOfNotNull(leaveTo, LeaveRules.dateIn(values, LeaveRules.SUFFIX_LAST)).maxBy { CivilCalendar.dayNumber(it) }
        val stop = if (from.day == 1) from else firstOfNextMonth(from)
        val after = firstOfNextMonth(stop)
        val nextToMonth = firstOfNextMonth(to)
        if (listOf(stop, after, nextToMonth).any { !CivilCalendar.exists(it) }) return null
        if (CivilCalendar.dayNumber(after) > CivilCalendar.dayNumber(to) + 1) return null
        val endsOnAMonthEnd = CivilCalendar.dayNumber(nextToMonth) == CivilCalendar.dayNumber(to) + 1
        return LostMonths(stop, if (endsOnAMonthEnd) nextToMonth else CalendarDate(to.year, to.month, 1))
    }

    private fun firstOfNextMonth(date: CalendarDate) = if (date.month < MONTHS) CalendarDate(date.year, date.month + 1, 1) else CalendarDate(date.year + 1, 1, 1)
}
