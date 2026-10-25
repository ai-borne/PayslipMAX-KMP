package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.part2.domain.AnswerQuestion
import com.payslipmax.pdfparser.part2.domain.DraftIssue
import com.payslipmax.pdfparser.part2.domain.DraftIssueReason
import com.payslipmax.pdfparser.part2.domain.DraftResult
import com.payslipmax.pdfparser.part2.domain.FormField
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.LeaveFormPlanner
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2DraftEngine
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.Part2LookupIndex
import com.payslipmax.pdfparser.part2.domain.Part2Occurrence
import com.payslipmax.pdfparser.part2.domain.Part2Origin
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.Part2Slip
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.PrintedStatus
import com.payslipmax.pdfparser.part2.domain.RuleFinding
import com.payslipmax.pdfparser.part2.domain.RuleSeverity
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import com.payslipmax.pdfparser.utils.localUtcOffsetMillis

/**
 * Derives what the draft screens draw from the officer's event, and nothing else: the same answers always give the same screen, so a
 * restored draft looks exactly as it did. Every sentence comes from `Part2Strings` (found by a rule's id or an issue's reason) or from
 * the bundle (labels, names, narratives), never from a composable. An answer not given yet is listed, not scolded; one refused is
 * named on its own field.
 */
internal class LeaveDraftStateBuilder(
    section: Part2Lookup,
    private val rules: Part2Rules,
    private val localOffset: (atMillis: Long) -> Long = ::localUtcOffsetMillis,
) {
    private val engine = Part2DraftEngine(section, rules)
    private val planner = LeaveFormPlanner(engine, rules)
    private val index = Part2LookupIndex(section)
    private val options = index.draftOptions().map { AddOption(it.code, it.name) }

    fun build(
        event: LeaveEventInput,
        acked: Set<String>,
        saveFailed: Boolean,
        printed: PrintedSnapshot? = null,
        printing: Boolean = false,
        grants: List<OrderRef> = emptyList(),
    ): LeaveDraftUiState {
        val sections = event.requests.mapIndexed { i, request -> section(i, request, event.requests.size) }
        val result = engine.draft(event)
        val drafted = (result as? DraftResult.Drafted)?.slip
        return LeaveDraftUiState(
            status = DraftStatus.READY,
            sections = sections,
            addable = if (event.requests.size >= Part2Limits.REQUESTS_MAX) emptyList() else options,
            missing = if (result is DraftResult.Drafted) emptyList() else sections.flatMap { s -> s.fields.filter { it.required && it.error == null && s.isUnanswered(it) }.map { it.label } }.distinct(),
            slip = drafted?.let { slip(it, acked) },
            saveFailed = saveFailed,
            printed = printed?.let { printedUi(it, drafted, templateRevs(event)) },
            printing = printing,
            grants = grants.map { GrantChoiceUi(it.orderNo, it.date, Part2VaultStrings.grantChoice(it.orderNo, format(it.date))) },
        )
    }

    /** What the clerk would be handed for [event], or null while the answers are incomplete. */
    fun snapshotOf(
        event: LeaveEventInput,
        at: Long,
    ): PrintedSnapshot? = (engine.draft(event) as? DraftResult.Drafted)?.let { PrintedSnapshot.of(it.slip, templateRevs(event), at) }

    /** The ids of the inputs the form asks for [request] now, in order. */
    fun fieldIds(request: OccurrenceRequest): List<String> = planner.fields(request).map { it.slot.id }

    /** The questions the form asks for [code]. */
    fun questionsFor(code: String): List<AnswerQuestion> = planner.questions(code)

    /** The revision of every template the event drafts, by code, for the saved draft. */
    fun templateRevs(event: LeaveEventInput): Map<String, String> =
        event.requests.flatMap(planner::templates).associate { it.code to it.rev }.entries.take(Part2Limits.REVS_MAX).associate { it.toPair() }

    private fun section(
        i: Int,
        request: OccurrenceRequest,
        count: Int,
    ): RequestSection {
        val issues = ((engine.draft(LeaveEventInput(listOf(request))) as? DraftResult.Refused)?.issues).orEmpty()
        return RequestSection(
            index = i,
            code = request.code,
            title = Part2Strings.sectionTitle(i + 1, request.code),
            name = index.code(request.code)?.name ?: request.code,
            questions = planner.questions(request.code).map { question(it, request) },
            fields = planner.fields(request).map { field(it, request, issues) },
            removable = count > 1,
        )
    }

    private fun question(
        question: AnswerQuestion,
        request: OccurrenceRequest,
    ): QuestionUi =
        when (question) {
            is AnswerQuestion.YesNo -> QuestionUi(question.id, Part2Strings.question(question.id), emptyList(), question.answer in request.answers, null)
            is AnswerQuestion.OneOf ->
                QuestionUi(
                    question.id,
                    Part2Strings.question(question.id),
                    question.options.map { OptionUi(it, index.code(it)?.name ?: it) },
                    false,
                    question.options.firstOrNull { question.prefix + it in request.answers },
                )
        }

    private fun field(
        field: FormField,
        request: OccurrenceRequest,
        issues: List<DraftIssue>,
    ): FieldUi {
        val slot = field.slot
        val refused = issues.firstOrNull { it.slot == slot.id && it.reason != DraftIssueReason.NOT_ANSWERED }
        return FieldUi(slot.id, slot.label, slot.help, field.required, valueOf(slot.kind, request.values[slot.id]), refused?.let { Part2Strings.fieldIssue(slot.label, it.reason) })
    }

    private fun valueOf(
        kind: String,
        value: SlotValue?,
    ): FieldValue =
        when (kind) {
            "date" -> FieldValue.Date((value as? SlotValue.DateValue)?.date?.takeIf(LeaveEventEditor::isSet))
            "number" -> FieldValue.Digits((value as? SlotValue.NumberValue)?.number?.toString().orEmpty())
            "ref" -> (value as? SlotValue.RefValue)?.ref.let { FieldValue.Reference(it?.orderNo.orEmpty(), it?.date?.takeIf(LeaveEventEditor::isSet)) }
            else -> FieldValue.Words((value as? SlotValue.TextValue)?.text.orEmpty())
        }

    private fun RequestSection.isUnanswered(field: FieldUi): Boolean =
        when (val value = field.value) {
            is FieldValue.Date -> value.date == null
            is FieldValue.Digits -> value.text.isEmpty()
            is FieldValue.Words -> value.text.isBlank()
            is FieldValue.Reference -> value.number.isBlank() && value.date == null
        }

    private fun slip(
        slip: Part2Slip,
        acked: Set<String>,
    ): SlipUi {
        val findings = slip.findings.map { finding(it, ticked = key(it) in acked) }
        return SlipUi(
            lastOrder = slip.lastOrder?.let { Part2Strings.lastOrderLine(it.orderNo, format(it.date)) } ?: Part2Strings.lastOrderBlank,
            orders = slip.orders.mapIndexed { n, order -> SlipOrderUi(Part2Strings.orderTitle(n + 1, order.groups), order.occurrences.map(::row)) },
            findings = findings,
            undrafted = slip.undrafted.map { Part2Strings.undraftedLine(it.code, Part2Strings.citation(it.origin.basis, it.origin.page, it.origin.note)) },
            readyToPrint = findings.filter { it.ack }.all { it.ticked },
        )
    }

    /** When it was printed (the day as the drafts list dates an edit) and, when the paper no longer matches, why. */
    private fun printedUi(
        printed: PrintedSnapshot,
        slip: Part2Slip?,
        revs: Map<String, String>,
    ): PrintedUi =
        PrintedUi(
            Part2Strings.printedOnLine(format(PreClaimDates.dayOfInstant(printed.at, localOffset(printed.at)))),
            when (printed.statusFor(slip, revs)) {
                PrintedStatus.CURRENT -> null
                PrintedStatus.CHANGED -> Part2Strings.changedSincePrinted
                PrintedStatus.TEMPLATE_UPDATED -> Part2Strings.templateUpdatedSincePrinted
            },
        )

    private fun row(row: Part2Occurrence): SlipRowUi =
        SlipRowUi(
            code = row.code,
            narrative = row.narrative,
            from = row.from?.let(::format).orEmpty(),
            to = row.to?.let(::format).orEmpty(),
            data = row.data,
            authority = row.authority,
            certificate = row.certificate,
            source = (row.origin as? Part2Origin.Linked)?.let { Part2Strings.citation(it.basis, it.page, it.note) }.orEmpty(),
        )

    private fun format(date: CalendarDate) = CivilCalendar.format(date)

    companion object {
        private fun key(finding: RuleFinding) = "${finding.ruleId}|${finding.code}"

        /** One finding as the preview draws it: its words from the rule's id, its citation from the declaration. */
        fun finding(
            finding: RuleFinding,
            ticked: Boolean,
        ): FindingUi =
            FindingUi(
                key = key(finding),
                ack = finding.severity == RuleSeverity.ACK,
                text = Part2Strings.finding(finding.ruleId) ?: Part2Strings.findingUnknown,
                citation = Part2Strings.citation(finding.basis, finding.page, finding.note),
                ticked = ticked,
            )
    }
}
