package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.data.hasAngleBrackets
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2RuleDecl
import com.payslipmax.pdfparser.guide.model.Part2Template

/**
 * Turns what the officer answered into the slip a clerk types into HRMS. It knows no code: the templates in [section] say what each
 * code prints and which orders go with it, the [rules] registry runs the manual's computable checks, and [SlipOrdering] arranges the
 * orders (paras 6.4 and 7.1). Pure and deterministic: the same input always gives the same slip, whatever order the answers were
 * entered in. There is no payslip input anywhere: only the officer's answers and the shipped templates.
 */
class Part2DraftEngine(
    section: Part2Lookup,
    private val rules: Part2Rules,
) {
    private val templates: Map<String, Part2Template> = section.templates.associateBy { it.code }
    private val groups: Map<String, String?> = section.serials.flatMap { page -> page.codes.map { it to page.group } }.toMap()
    private val knownCodes: Set<String> = groups.keys + section.templates.mapNotNull { it.print }
    private val declarations: Map<String, Part2RuleDecl> = section.rules.associateBy { it.id }
    private val expander = LinkExpander(templates, groups, rules)

    fun draft(
        input: LeaveEventInput,
        today: CalendarDate? = null,
    ): DraftResult {
        val requestIssue = requestIssue(input)
        if (requestIssue != null) return DraftResult.Refused(listOf(requestIssue))
        val issues = lastOrderIssues(input.lastOrder).toMutableList()
        val drafted = mutableListOf<Pair<OccurrenceRequest, DraftedTemplate>>()
        val undrafted = mutableListOf<UndraftedLink>()
        for (request in input.requests) {
            val template = templates[request.code]
            if (template == null) {
                issues += DraftIssue(request.code, "", DraftIssueReason.UNKNOWN_CODE)
                continue
            }
            expander.expand(request, template, drafted, undrafted)
        }
        issues += drafted.flatMap { it.second.issues }
        if (issues.isNotEmpty()) return DraftResult.Refused(issues.distinct())
        val rows = drafted.mapNotNull { it.second.occurrence }
        val event = drafted.map { it.second.drafted }
        return DraftResult.Drafted(Part2Slip(input.lastOrder, SlipOrdering.arrange(rows), findings(drafted, event, today), undrafted.distinct()))
    }

    /** The shipped template for [code], or null. */
    fun template(code: String): Part2Template? = templates[code]

    /** The templates [request] drafts now: its own and every linked one whose condition holds. Empty for a code with no template. */
    fun templatesFor(request: OccurrenceRequest): List<Part2Template> {
        val first = templates[request.code] ?: return emptyList()
        val drafted = mutableListOf<Pair<OccurrenceRequest, DraftedTemplate>>()
        expander.expand(request, first, drafted, mutableListOf())
        return drafted.map { it.second.drafted.template }
    }

    private fun requestIssue(input: LeaveEventInput): DraftIssue? =
        when {
            input.requests.isEmpty() -> DraftIssue("", "", DraftIssueReason.NOTHING_REQUESTED)
            input.requests.size > Part2Limits.REQUESTS_MAX -> DraftIssue("", "", DraftIssueReason.TOO_MANY)
            else -> null
        }

    private fun lastOrderIssues(last: OrderRef?): List<DraftIssue> {
        val reason =
            when {
                last == null -> null
                last.orderNo.isBlank() -> DraftIssueReason.NOT_ANSWERED
                last.orderNo.length > Part2Limits.ORDER_NO_MAX -> DraftIssueReason.TOO_LONG
                hasAngleBrackets(last.orderNo) -> DraftIssueReason.MARKUP
                !CivilCalendar.exists(last.date) -> DraftIssueReason.BAD_DATE
                else -> null
            }
        return listOfNotNull(reason?.let { DraftIssue("", LAST_ORDER, it) })
    }

    private fun findings(
        drafted: List<Pair<OccurrenceRequest, DraftedTemplate>>,
        event: List<DraftedOccurrence>,
        today: CalendarDate?,
    ): List<RuleFinding> =
        drafted.flatMap { (request, result) ->
            val printed = result.drafted.template.let { it.print ?: it.code }
            result.drafted.template.rules.mapNotNull { id ->
                val decl = declarations[id]
                val rule = rules.check(id)
                if (decl == null || rule == null || !rule.violated(RuleContext(request, result.drafted, event, today, knownCodes))) return@mapNotNull null
                RuleFinding(id, if (decl.severity == "ack") RuleSeverity.ACK else RuleSeverity.WARN, printed, basisOf(decl.basis), decl.page, decl.note)
            }
        }.distinctBy { it.ruleId to it.code }

    private companion object {
        const val LAST_ORDER = "last_order"
    }
}
