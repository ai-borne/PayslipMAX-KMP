package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.DutyDays
import com.payslipmax.pdfparser.guide.preclaim.DutySpan

/** Field positions beyond the six columns of [Part2ColumnPosition]. */
object EntryPosition {
    const val NOTE = 6
}

enum class EntryProblem { MISSING, TOO_LONG, INVALID_DATE, AFTER_TO }

/** One box on the form: [caption] is the manual's own words, empty where the manual prints none (a required one then points to the manual page). */
data class EntryField(val position: Int, val required: Boolean, val caption: String)

/** What the officer typed. The dates are null until picked; [data] is Data 1 to 4 as typed. */
data class EntryInput(
    val from: CalendarDate?,
    val to: CalendarDate?,
    val data: List<String>,
    val note: String,
)

/**
 * What the form for one casualty asks, built from that casualty's Annexure III row (ODP 2026) and nothing else: From is always asked and
 * required (the Bank is in From order; where the ODP prints no From column, [fromPrinted] is false and the screen calls it a plain date),
 * To and each Data column appear only where the ODP prints them, and are required only where it says so. No rule is guessed from a caption.
 * [manualPage] is where a captionless required column is explained.
 */
class EntryForm(
    val code: String,
    val fields: List<EntryField>,
    val manualPage: Int,
    val fromPrinted: Boolean = true,
) {
    /** Every problem, by field position; empty means the input can be added. */
    fun check(input: EntryInput): Map<Int, EntryProblem> {
        val problems = mutableMapOf<Int, EntryProblem>()
        dates(input, problems)
        for (field in fields.filter { it.position >= Part2ColumnPosition.DATA_FIRST }) {
            val text = input.data.getOrNull(field.position - Part2ColumnPosition.DATA_FIRST).orEmpty().trim()
            when {
                text.length > Part2Limits.TEXT_MAX -> problems[field.position] = EntryProblem.TOO_LONG
                field.required && text.isEmpty() -> problems[field.position] = EntryProblem.MISSING
                EntryDataRules.isDate(field.caption) && text.isNotEmpty() && EntryDataRules.parse(text) == null -> problems[field.position] = EntryProblem.INVALID_DATE
            }
        }
        if (input.note.trim().length > Part2Limits.TEXT_MAX) problems[EntryPosition.NOTE] = EntryProblem.TOO_LONG
        return problems
    }

    private fun dates(
        input: EntryInput,
        problems: MutableMap<Int, EntryProblem>,
    ) {
        val from = input.from
        val hasTo = fields.any { it.position == Part2ColumnPosition.TO }
        val to = input.to.takeIf { hasTo }
        when {
            from == null -> problems[Part2ColumnPosition.FROM] = EntryProblem.MISSING
            !DutyDays.exists(from) -> problems[Part2ColumnPosition.FROM] = EntryProblem.INVALID_DATE
        }
        when {
            !hasTo -> Unit
            to == null -> if (fields.first { it.position == Part2ColumnPosition.TO }.required) problems[Part2ColumnPosition.TO] = EntryProblem.MISSING
            !DutyDays.exists(to) -> problems[Part2ColumnPosition.TO] = EntryProblem.INVALID_DATE
        }
        if (from != null && to != null && problems.isEmpty() && DutyDays.dayNumber(from) > DutyDays.dayNumber(to)) {
            problems[Part2ColumnPosition.FROM] = EntryProblem.AFTER_TO
        }
    }

    /** The Data index of the casualty's day-count box, if it has one. */
    private val dayCountIndex: Int? =
        fields.firstOrNull { it.position >= Part2ColumnPosition.DATA_FIRST && EntryDataRules.isDayCount(it.caption) }?.let { it.position - Part2ColumnPosition.DATA_FIRST }

    /**
     * When the From or To date changes, the day-count box follows the new span. It is only replaced while it still holds the span from the
     * old dates (or is empty), so a count the officer typed is never overwritten by a date change, and clearing the box stays cleared.
     */
    fun withSpanDays(
        before: EntryInput,
        after: EntryInput,
    ): EntryInput {
        val index = dayCountIndex ?: return after
        if (before.from == after.from && before.to == after.to) return after
        val current = after.data.getOrElse(index) { "" }
        if (current.isNotEmpty() && current != span(before)) return after
        return after.copy(data = after.data.mapIndexed { i, old -> if (i == index) span(after) else old })
    }

    /** The days the From-To span covers, both ends counted (as the portal counts it), or "" while the dates do not make a span. */
    private fun span(input: EntryInput): String {
        val to = input.to.takeIf { fields.any { it.position == Part2ColumnPosition.TO } }
        return (DutyDays.inclusive(input.from, to) as? DutySpan.Days)?.count?.toString().orEmpty()
    }

    /** The entry for a clean [input] (trimmed, with a column the casualty does not use dropped), or null while anything is wrong. */
    fun toEntry(
        input: EntryInput,
        id: String,
        orderId: String?,
        createdAt: Long,
        updatedAt: Long,
    ): BankEntry? {
        if (check(input).isNotEmpty()) return null
        val from = input.from ?: return null
        val used = fields.map { it.position }.toSet()
        val data = List(Part2Limits.DATA_COLUMNS) { i -> if (Part2ColumnPosition.DATA_FIRST + i in used) input.data.getOrNull(i).orEmpty().trim() else "" }
        return BankEntry(id, code, from, input.to.takeIf { Part2ColumnPosition.TO in used }, data, input.note.trim(), orderId, createdAt, updatedAt)
    }

    companion object {
        /** The form for [code], or null when the bundle does not know it. */
        fun of(
            index: Part2LookupIndex,
            code: String,
        ): EntryForm? {
            val page = index.code(code) ?: return null
            val lines = page.columns.associateBy { it.position }

            fun caption(position: Int) = lines[position]?.caption.orEmpty()
            val fields =
                listOf(
                    EntryField(Part2ColumnPosition.FROM, required = true, caption = caption(Part2ColumnPosition.FROM)),
                ) +
                    page.columns.filter { it.position >= Part2ColumnPosition.TO }.map { EntryField(it.position, it.need == Part2Need.REQUIRED, it.caption) }
            return EntryForm(code, fields, page.page, fromPrinted = Part2ColumnPosition.FROM in lines)
        }
    }
}
