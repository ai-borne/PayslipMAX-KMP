package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates

/** Builds the facts a rule reads without drafting a whole slip, so a rule is tested alone. Dates are synthetic. */
internal object RuleContexts {
    fun date(
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = SlotValue.DateValue(CalendarDate(year, month, day))

    fun number(value: Int) = SlotValue.NumberValue(value)

    fun ref(
        no: String,
        month: Int = 1,
        day: Int = 15,
        year: Int = 2026,
    ) = SlotValue.RefValue(OrderRef(no, CalendarDate(year, month, day)))

    /** A drafted occurrence of [code] carrying the worked-out [computed] values. */
    fun occurrence(
        code: String,
        computed: Map<String, SlotValue> = emptyMap(),
    ) = DraftedOccurrence(SyntheticPart2Templates.templates.first().copy(code = code), null, null, computed)

    /** The context for judging [code] with the officer's [values] and [answers], inside an event that also holds [others] (by code). */
    fun context(
        code: String,
        values: Map<String, SlotValue> = emptyMap(),
        answers: Set<String> = emptySet(),
        computed: Map<String, SlotValue> = emptyMap(),
        others: List<String> = emptyList(),
        today: CalendarDate? = null,
        known: Set<String>? = null,
    ): RuleContext {
        val mine = occurrence(code, computed)
        return RuleContext(OccurrenceRequest(code, values, answers), mine, listOf(mine) + others.map { occurrence(it) }, today, known)
    }
}
