package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankFigures

/**
 * The rule family for the year-end accumulation of annual leave (Part II plan, phase 8; ODP-2026 pp.133-134). A family may name codes and
 * slot ids, which the engine and the screens must not; the ids here are the ones `part2/rules.json` declares and
 * `part2/templates/ch5_yearend.json` cites. The arithmetic is [AcclveCalculator]'s; the words that print are the manual's (p.133 (a)).
 */
object AcclveRules {
    const val PREV_PHRASE = "acclve_prev_phrase"
    const val PREV_DATA = "acclve_prev_data"
    const val SUM_TEXT = "acclve_sum_text"
    const val TOTAL = "acclve_total"
    const val ACCRUAL_30 = "acclve_accrual_30"
    const val YEAR_60 = "acclve_year_60"
    const val OVER_CAP = "acclve_over_cap"
    const val TOO_EARLY = "acclve_too_early"

    /** The yes/no answer for "is this accumulation published on your retirement?", which lifts the 1 March rule (note 7). */
    const val ANSWER_RETIRING = "acclve_retiring"

    /** The code this family drafts: the one the reminder planner answers a year end with. */
    const val CODE = ReminderRules.YEAR_END_REQUEST

    /** The slots the rules read; the leave year is [LeaveRules.YEAR], shared with every annual template (the year-end reminder finds an ACCLVE by it). */
    private const val PREV_TOTAL = "prev_total"
    private const val LTC_DAYS = "ltc_days"
    private const val ACCRUED = "accrued"
    private const val AL_USED = "al_used"
    private const val TOTAL_SLOT = "total"

    /** Words of the manual (p.133 (a)): printed after the previous total when it was cut to the cap, and before the date it is as on. */
    private const val RESTRICTED_WORDS = " (Restricted to "
    private const val AS_ON_WORDS = " as on "

    /** The study leave taken against annual leave counts as annual leave used (note 5); it is a request of its own code and carries the same slots. */
    private val ANNUAL_USED: Set<String> = LeaveRules.ANNUAL_LEAVE + "STDLVAL"

    val all: List<Part2Rule> =
        listOf(
            deriveRule(PREV_PHRASE, ::previousPhrase),
            deriveRule(PREV_DATA, ::previousData),
            deriveRule(SUM_TEXT, ::sumText),
            deriveRule(TOTAL) { context -> total(context.values)?.let(SlotValue::NumberValue) },
            checkRule(ACCRUAL_30) { (number(it.request.values, ACCRUED) ?: 0) > AcclveCalculator.ACCRUAL_CAP_DAYS },
            checkRule(YEAR_60) { (number(it.request.values, ACCRUED) ?: 0) + (number(it.request.values, AL_USED) ?: 0) > AcclveCalculator.YEAR_USE_CAP_DAYS },
            checkRule(OVER_CAP) { (it.occurrence.days(TOTAL_SLOT) ?: 0) > AcclveCalculator.TOTAL_CAP_DAYS },
            checkRule(TOO_EARLY) { context ->
                val year = number(context.request.values, LeaveRules.YEAR)
                year != null && AcclveCalculator.publishedTooEarly(context.today, year, ANSWER_RETIRING in context.request.answers)
            },
        )

    /** One question: whether this accumulation is for retirement. Its wording is `Part2Strings`' under this id. */
    val questions: List<AnswerQuestion> = listOf(AnswerQuestion.YesNo("acclve_retiring", ANSWER_RETIRING, setOf(TOO_EARLY)))

    /** What this code fills from the Leave Bank (phase 8b). */
    val fills: List<LeaveBankFill> = listOf(Fill)

    /** The (d) figure an ACCLVE request works out to, or null when its figures are not all answered. */
    fun totalOf(request: OccurrenceRequest): Int? = total(request.values)

    /** Days of annual leave debited to [year] by [requests] (the printed drafts' requests): the grants and extensions of that year and the study leave taken against it. */
    fun alUsed(
        year: Int,
        requests: List<OccurrenceRequest>,
    ): Int =
        requests.filter { it.code in ANNUAL_USED && number(it.values, LeaveRules.YEAR) == year }.sumOf { request ->
            val from = realDate(request, LeaveRules.LEAVE_FROM)
            val to = realDate(request, LeaveRules.LEAVE_TO)
            if (from != null && to != null) CivilCalendar.inclusiveDays(from, to) ?: 0 else 0
        }

    private fun number(
        values: Map<String, SlotValue>,
        slot: String,
    ): Int? = (values[slot] as? SlotValue.NumberValue)?.number

    /** A stored date, only if the calendar can hold it: storage does not bound a date's value and the calendar counts years one by one, so a crafted backup must not reach the count. */
    private fun realDate(
        request: OccurrenceRequest,
        slot: String,
    ): CalendarDate? = (request.values[slot] as? SlotValue.DateValue)?.date?.takeIf(CivilCalendar::exists)

    private fun total(values: Map<String, SlotValue>): Int? {
        val previous = number(values, PREV_TOTAL) ?: return null
        val ltc = number(values, LTC_DAYS) ?: return null
        val accrued = number(values, ACCRUED) ?: return null
        return AcclveCalculator.total(previous, ltc, accrued)
    }

    /** "380 (Restricted to 300) as on 31/12/2013": the previous total as the manual's (a) prints it, with the restriction when it applies. */
    private fun previousPhrase(context: DeriveContext): SlotValue? {
        val previous = number(context.values, PREV_TOTAL) ?: return null
        val on = lastDayOfPreviousYear(context.values) ?: return null
        val restricted = if (AcclveCalculator.restricted(previous)) "$RESTRICTED_WORDS${AcclveCalculator.TOTAL_CAP_DAYS})" else ""
        return SlotValue.TextValue("$previous$restricted$AS_ON_WORDS${CivilCalendar.format(on)}")
    }

    /** "300-2013": Data 3, the previous total as used and the year it is from. */
    private fun previousData(context: DeriveContext): SlotValue? {
        val previous = number(context.values, PREV_TOTAL) ?: return null
        val on = lastDayOfPreviousYear(context.values) ?: return null
        return SlotValue.TextValue("${AcclveCalculator.capped(previous)}-${on.year}")
    }

    /** "(300-10+0)": the sum as (d) prints it. */
    private fun sumText(context: DeriveContext): SlotValue? {
        val previous = number(context.values, PREV_TOTAL) ?: return null
        val ltc = number(context.values, LTC_DAYS) ?: return null
        val accrued = number(context.values, ACCRUED) ?: return null
        return SlotValue.TextValue("(${AcclveCalculator.capped(previous)}-$ltc+$accrued)")
    }

    /** 31 December of the year before the leave year, printed as text only (no calendar arithmetic), so every year a number answer can hold drafts (the form asks only for what the engine reads, so a hidden slot must never refuse a slip) and the year before year 0 is year 0. */
    private fun lastDayOfPreviousYear(values: Map<String, SlotValue>): CalendarDate? =
        number(values, LeaveRules.YEAR)?.let { CalendarDate((it - 1).coerceAtLeast(0), DECEMBER, LAST_DAY) }

    private const val DECEMBER = 12
    private const val LAST_DAY = 31

    /**
     * What the form fills for the year end (DECISIONS Round 30): the previous total from the statement's row of the year before (else from the
     * officer's own finished draft for that year), the LTC days from the statement, the leave used from the printed drafts and (c) from it.
     * The LTC days are filled only when the statement is held, since "0 days encashed" would otherwise be a claim, not a fact.
     */
    private object Fill : LeaveBankFill {
        override val code = CODE
        override val slots = setOf(PREV_TOTAL, LTC_DAYS, ACCRUED, AL_USED)

        override fun fill(
            request: OccurrenceRequest,
            inputs: LeaveBankInputs,
        ): FillOutcome {
            val year = number(request.values, LeaveRules.YEAR) ?: return FillOutcome.NeedsYear
            val earlier = inputs.drafted.firstNotNullOfOrNull { other -> if (other.code == CODE && number(other.values, LeaveRules.YEAR) == year - 1) totalOf(other) else null }
            val figures = LeaveBankFigures.suggest(year, inputs.rows, earlier, alUsed(year, inputs.printed))
            val values =
                buildMap {
                    figures.previousTotal?.let { put(PREV_TOTAL, it) }
                    if (inputs.rows.isNotEmpty()) put(LTC_DAYS, figures.ltcDays)
                    put(AL_USED, figures.alUsed)
                    put(ACCRUED, figures.accrued)
                }.mapNotNull { (slot, days) -> days.takeIf { it in 0..Part2Limits.NUMBER_MAX }?.let { slot to SlotValue.NumberValue(it) } }.toMap()
            return FillOutcome.Values(values)
        }

        override fun notes(
            request: OccurrenceRequest,
            rows: List<LeaveEntry>,
        ): List<FillNote> = number(request.values, AL_USED)?.let { listOf(FillNote.AccrualLimit(it, AcclveCalculator.accrual(it))) }.orEmpty()

        override fun crossCheck(
            request: OccurrenceRequest,
            rows: List<LeaveEntry>,
        ): YearCheck? {
            val year = number(request.values, LeaveRules.YEAR) ?: return null
            return YearCheck(year, LeaveBankFigures.crossCheck(year, totalOf(request) ?: return null, rows))
        }
    }
}
