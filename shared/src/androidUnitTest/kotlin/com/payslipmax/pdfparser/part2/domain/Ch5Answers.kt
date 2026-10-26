package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.text

/** Synthetic answers for the Chapter 5 batch tests (phase 8): invented dates and order numbers only, never one from a real order. */
internal object Ch5Answers {
    fun on(
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = CalendarDate(year, month, day)

    /** A leave from [from] to [to] with no prefix or suffix. */
    fun leave(
        from: CalendarDate,
        to: CalendarDate,
        vararg more: Pair<String, SlotValue>,
    ): Map<String, SlotValue> = mapOf("leave_from" to SlotValue.DateValue(from), "leave_to" to SlotValue.DateValue(to)) + more

    val order = ref("0123/26", 6, 20)
    val letter = ref("L-77/26", 5, 2)

    fun request(
        code: String,
        values: Map<String, SlotValue>,
        vararg answers: String,
    ) = OccurrenceRequest(code, values, answers.toSet())

    fun rowOf(
        code: String,
        vararg requests: OccurrenceRequest,
    ): Part2Occurrence = RealPart2.rows(RealPart2.draft(*requests)).single { it.code == code }

    fun codes(vararg requests: OccurrenceRequest): List<String> = RealPart2.rows(RealPart2.draft(*requests)).map { it.code }

    fun findings(vararg requests: OccurrenceRequest): List<RuleFinding> = RealPart2.draft(*requests).findings

    val furlough: Map<String, SlotValue> = leave(on(7, 6), on(8, 4), "prefix_first" to date(7, 4), "cycle_from" to number(2024), "cycle_to" to number(2026), "leave_kind" to text("furlough"))
}
