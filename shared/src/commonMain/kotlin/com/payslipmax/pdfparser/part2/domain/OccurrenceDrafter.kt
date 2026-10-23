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
 * columns. Answers are bounded, plain text and real dates; a refusal names the slot and the reason, never the value.
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
        val days = mutableMapOf<String, Int>()
        for (slot in template.slots.filter { it.kind != "days" }) {
            issues += slotIssues(template.code, slot, request.values[slot.id], slot.id in required)
        }
        for (slot in template.slots.filter { it.kind == "days" }) {
            issues += daysIssues(template, slot, request, slot.id in required, issues, days)
        }
        val answers: Map<String, SlotValue> = request.values + days.mapValues { SlotValue.NumberValue(it.value) }
        val from = columnDate(template.from, answers)
        val to = columnDate(template.to, answers)
        if (from != null && to != null && CivilCalendar.exists(from) && CivilCalendar.exists(to) && CivilCalendar.daysBetween(from, to) < 0) {
            issues += DraftIssue(template.code, template.to.slot.orEmpty(), DraftIssueReason.DATES_REVERSED)
        }
        val drafted = DraftedOccurrence(template, from, to, days)
        if (issues.isNotEmpty()) return DraftedTemplate(null, drafted, issues.distinct())
        val row =
            Part2Occurrence(
                code = template.print ?: template.code,
                group = group,
                narrative = OccurrenceRenderer.render(template.narrative, answers),
                from = from,
                to = to,
                data = template.data.map { cell(it, answers) },
                authority = template.authority?.let { OccurrenceRenderer.render(it, answers) },
                certificate = template.certificate?.let { OccurrenceRenderer.render(it, answers) },
                origin = origin,
                templateRev = template.rev,
            )
        return DraftedTemplate(row, drafted, emptyList())
    }

    /** Slots the officer must answer: those a text or a required column uses. A reference is never required: its blank prints blank. */
    private fun requiredSlots(template: Part2Template): Set<String> {
        val texts = listOfNotNull(template.narrative, template.authority, template.certificate)
        val inTexts = texts.flatMap { text -> Part2Placeholders.scan(text).first.mapNotNull { Part2Placeholders.parse(it)?.slot } }
        val inColumns = (listOf(template.from, template.to) + template.data).filter { it.need == "required" }.mapNotNull { column(it) }
        return (inTexts + inColumns).toSet() - template.slots.filter { it.kind == "ref" }.map { it.id }.toSet()
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

    /** Counts a `days` slot between its two date slots, both days included. A reversed range is refused on the end date. */
    private fun daysIssues(
        template: Part2Template,
        slot: Part2Slot,
        request: OccurrenceRequest,
        required: Boolean,
        earlier: List<DraftIssue>,
        days: MutableMap<String, Int>,
    ): List<DraftIssue> {
        val (start, end) = slot.between.map { (request.values[it] as? SlotValue.DateValue)?.date }.let { it.getOrNull(0) to it.getOrNull(1) }
        if (start != null && end != null && CivilCalendar.exists(start) && CivilCalendar.exists(end)) {
            val count = CivilCalendar.inclusiveDays(start, end)
            if (count != null) days[slot.id] = count
            return if (count == null) listOf(DraftIssue(template.code, slot.between[1], DraftIssueReason.DATES_REVERSED)) else emptyList()
        }
        val blamed = earlier.any { it.slot in slot.between }
        return if (required && !blamed) listOf(DraftIssue(template.code, slot.id, DraftIssueReason.NOT_ANSWERED)) else emptyList()
    }

    private fun columnDate(
        column: Part2Column,
        answers: Map<String, SlotValue>,
    ): CalendarDate? =
        if (column.need == "none") null else (column.slot?.let { answers[it] } as? SlotValue.DateValue)?.date

    private fun cell(
        column: Part2Column,
        answers: Map<String, SlotValue>,
    ): String {
        val ref = column.slot
        return when {
            column.need == "none" || ref == null -> ""
            column.need == "optional" && answers[Part2Placeholders.parse(ref)?.slot.orEmpty()] == null -> ""
            else -> OccurrenceRenderer.reference(ref, answers)
        }
    }
}
