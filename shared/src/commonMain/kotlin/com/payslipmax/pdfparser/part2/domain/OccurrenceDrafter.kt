package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.data.Part2Placeholders
import com.payslipmax.pdfparser.guide.data.hasAngleBrackets
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template

/** One template drafted for one request: the row for the slip (null when an answer was refused), the facts rules read, and the refusals. */
class DraftedTemplate(
    val occurrence: Part2Occurrence?,
    val drafted: DraftedOccurrence,
    val issues: List<DraftIssue>,
)

/**
 * Checks the officer's answers against one template and, if they are all acceptable, fills the template: From, To, Data 1-4, the
 * narrative, the authority line and the certificate. It knows no code: everything it does is driven by the template's slots and
 * columns. Answers are bounded, plain text and real dates; a refusal names the slot and the reason, never the value. Dates the manual
 * wants but the officer never types as such (first day away, last day away, the day after) are worked out here from the dates given,
 * in the order the template declares them, so each reads only what is already known.
 */
internal object OccurrenceDrafter {
    fun draft(
        template: Part2Template,
        origin: Part2Origin,
        group: String?,
        request: OccurrenceRequest,
    ): DraftedTemplate {
        val issues = mutableListOf<DraftIssue>()
        val required = requiredSlots(template)
        val computed = mutableMapOf<String, SlotValue>()
        for (slot in template.slots.filter { it.kind !in WORKED_OUT }) {
            issues += slotIssues(template.code, slot, request.values[slot.id], slot.id in required)
        }
        for (slot in template.slots.filter { it.kind in WORKED_OUT }) {
            issues += workedOut(template, slot, request, slot.id in required, issues, computed)
        }
        val answers: Map<String, SlotValue> = request.values + computed
        val printed = answers + template.slots.filter { it.ifMissing != null && it.id !in answers }.associate { it.id to SlotValue.TextValue(it.ifMissing.orEmpty()) }
        val from = columnDate(template.from, answers)
        val to = columnDate(template.to, answers)
        if (from != null && to != null && CivilCalendar.exists(from) && CivilCalendar.exists(to) && CivilCalendar.daysBetween(from, to) < 0) {
            issues += DraftIssue(template.code, template.to.slot.orEmpty(), DraftIssueReason.DATES_REVERSED)
        }
        val drafted = DraftedOccurrence(template, from, to, computed)
        if (issues.isNotEmpty()) return DraftedTemplate(null, drafted, issues.distinct())
        val row =
            Part2Occurrence(
                code = template.print ?: template.code,
                group = group,
                narrative = OccurrenceRenderer.render(template.narrative, printed),
                from = from,
                to = to,
                data = template.data.map { cell(it, answers, printed) },
                authority = template.authority?.let { OccurrenceRenderer.render(it, printed) },
                certificate = template.certificate?.let { OccurrenceRenderer.render(it, printed) },
                origin = origin,
                templateRev = template.rev,
            )
        return DraftedTemplate(row, drafted, emptyList())
    }

    /**
     * Slots the officer must answer: those a text or a required column uses. A reference is never required (its blank prints blank), nor
     * is a slot that says what prints without it, nor one only a check reads. A slot used only to work out another is not required
     * either: the slot worked out is, and it needs at least one date to work from.
     */
    internal fun requiredSlots(template: Part2Template): Set<String> {
        val columns = (listOf(template.from, template.to) + template.data).filter { it.need == "required" }
        val texts = listOfNotNull(template.narrative, template.authority, template.certificate) + columns.mapNotNull { it.text }
        val inTexts = texts.flatMap { text -> Part2Placeholders.scan(text).first.mapNotNull { Part2Placeholders.parse(it)?.slot } }
        val inColumns = columns.mapNotNull { column(it) }
        val exempt = template.slots.filter { it.kind == "ref" || it.ifMissing != null || it.forRule != null }.map { it.id }.toSet()
        return (inTexts + inColumns).toSet() - exempt
    }

    private fun column(column: Part2Column): String? = column.slot?.let { Part2Placeholders.parse(it)?.slot }

    private fun slotIssues(
        code: String,
        slot: Part2Slot,
        value: SlotValue?,
        required: Boolean,
    ): List<DraftIssue> {
        fun issue(reason: DraftIssueReason) = listOf(DraftIssue(code, slot.id, reason))
        if (value == null) return if (required) issue(DraftIssueReason.NOT_ANSWERED) else emptyList()
        return when (slot.kind) {
            "date" -> if (value is SlotValue.DateValue) dateIssue(value.date, ::issue) else issue(DraftIssueReason.WRONG_KIND)
            "number" -> if (value is SlotValue.NumberValue) numberIssue(value.number, ::issue) else issue(DraftIssueReason.WRONG_KIND)
            "text" -> if (value is SlotValue.TextValue) textIssue(value.text, ::issue) else issue(DraftIssueReason.WRONG_KIND)
            "ref" -> if (value is SlotValue.RefValue) refIssue(value.ref, ::issue) else issue(DraftIssueReason.WRONG_KIND)
            else -> issue(DraftIssueReason.WRONG_KIND)
        }
    }

    private fun dateIssue(
        date: CalendarDate,
        issue: (DraftIssueReason) -> List<DraftIssue>,
    ) = if (CivilCalendar.exists(date)) emptyList() else issue(DraftIssueReason.BAD_DATE)

    private fun numberIssue(
        number: Int,
        issue: (DraftIssueReason) -> List<DraftIssue>,
    ) = if (number in 0..Part2Limits.NUMBER_MAX) emptyList() else issue(DraftIssueReason.OUT_OF_RANGE)

    private fun textIssue(
        text: String,
        issue: (DraftIssueReason) -> List<DraftIssue>,
    ) = when {
        text.isBlank() -> issue(DraftIssueReason.NOT_ANSWERED)
        text.length > Part2Limits.TEXT_MAX -> issue(DraftIssueReason.TOO_LONG)
        hasAngleBrackets(text) -> issue(DraftIssueReason.MARKUP)
        else -> emptyList()
    }

    private fun refIssue(
        ref: OrderRef,
        issue: (DraftIssueReason) -> List<DraftIssue>,
    ) = when {
        ref.orderNo.isBlank() -> issue(DraftIssueReason.NOT_ANSWERED)
        ref.orderNo.length > Part2Limits.ORDER_NO_MAX -> issue(DraftIssueReason.TOO_LONG)
        hasAngleBrackets(ref.orderNo) -> issue(DraftIssueReason.MARKUP)
        !CivilCalendar.exists(ref.date) -> issue(DraftIssueReason.BAD_DATE)
        else -> emptyList()
    }

    /**
     * Works out a `days` slot (both days counted) or a computed date from the dates already known. When an input was refused, the slot is
     * not worked out and not blamed again; when nothing was given to work from, only a slot something needs is refused, on its own id.
     */
    private fun workedOut(
        template: Part2Template,
        slot: Part2Slot,
        request: OccurrenceRequest,
        required: Boolean,
        earlier: List<DraftIssue>,
        computed: MutableMap<String, SlotValue>,
    ): List<DraftIssue> {
        fun issue(reason: DraftIssueReason) = listOf(DraftIssue(template.code, slot.id, reason))
        val known = slot.between.map { ((request.values[it] ?: computed[it]) as? SlotValue.DateValue)?.date }
        val present = known.filterNotNull()
        if (earlier.any { it.slot in slot.between }) return emptyList()
        if (slot.kind == "days") {
            val (start, end) = known.getOrNull(0) to known.getOrNull(1)
            if (start == null || end == null) return if (required) issue(DraftIssueReason.NOT_ANSWERED) else emptyList()
            val count = CivilCalendar.inclusiveDays(start, end)
            if (count != null) computed[slot.id] = SlotValue.NumberValue(count)
            return if (count == null) listOf(DraftIssue(template.code, slot.between[1], DraftIssueReason.DATES_REVERSED)) else emptyList()
        }
        if (present.isEmpty()) return if (required) issue(DraftIssueReason.NOT_ANSWERED) else emptyList()
        val value =
            when (slot.kind) {
                "earliest" -> present.minBy { CivilCalendar.dayNumber(it) }
                "latest" -> present.maxBy { CivilCalendar.dayNumber(it) }
                else -> CivilCalendar.nextDay(present.first())
            } ?: return issue(DraftIssueReason.BAD_DATE)
        computed[slot.id] = SlotValue.DateValue(value)
        return emptyList()
    }

    private fun columnDate(
        column: Part2Column,
        answers: Map<String, SlotValue>,
    ): CalendarDate? =
        if (column.need == "none") null else (column.slot?.let { answers[it] } as? SlotValue.DateValue)?.date

    /** [answers] are what the officer gave and what was worked out; [printed] adds the words printed for an optional answer left out. */
    private fun cell(
        column: Part2Column,
        answers: Map<String, SlotValue>,
        printed: Map<String, SlotValue>,
    ): String {
        val ref = column.slot
        return when {
            column.need == "none" -> ""
            column.text != null -> OccurrenceRenderer.render(column.text, printed)
            ref == null -> ""
            column.need == "optional" && answers[Part2Placeholders.parse(ref)?.slot.orEmpty()] == null -> ""
            else -> OccurrenceRenderer.reference(ref, printed)
        }
    }

    /** Slots the app works out rather than asks for. */
    private val WORKED_OUT = listOf("days", "earliest", "latest", "next_day")

    /** Whether a slot of [kind] is worked out by the app, so the form never asks for it. */
    fun isWorkedOut(kind: String): Boolean = kind in WORKED_OUT
}
