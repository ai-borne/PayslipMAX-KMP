package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * One saved draft as a list shows it: the codes requested (each once), when it was last edited, and the span of the orders the officer
 * asked for once the draft is complete. [toString] hides the dates and codes, like [Part2Draft].
 */
data class Part2DraftSummary(
    val id: String,
    val codes: List<String>,
    val updatedAt: Long,
    val from: CalendarDate?,
    val to: CalendarDate?,
) {
    override fun toString(): String = "Part2DraftSummary(codes=${codes.size})"

    companion object {
        /** The span is the earliest From and the latest To of the requested occurrences only: the orders linked to them are not what the list is about. */
        fun of(
            draft: Part2Draft,
            engine: Part2DraftEngine,
        ): Part2DraftSummary {
            val rows = (engine.draft(draft.event) as? DraftResult.Drafted)?.slip?.orders.orEmpty().flatMap { it.occurrences }.filter { it.origin == Part2Origin.Requested }
            return Part2DraftSummary(
                id = draft.id,
                codes = draft.event.requests.map { it.code }.distinct(),
                updatedAt = draft.updatedAt,
                from = rows.mapNotNull { it.from }.minByOrNull(CivilCalendar::dayNumber),
                to = rows.mapNotNull { it.to }.maxByOrNull(CivilCalendar::dayNumber),
            )
        }
    }
}
