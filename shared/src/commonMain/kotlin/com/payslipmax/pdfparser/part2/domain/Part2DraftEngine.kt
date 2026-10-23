package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.data.hasAngleBrackets
import com.payslipmax.pdfparser.guide.model.Part2Link
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
    private val declarations: Map<String, Part2RuleDecl> = section.rules.associateBy { it.id }

    fun draft(input: LeaveEventInput): DraftResult {
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
            expand(request, template, drafted, undrafted)
        }
        issues += drafted.flatMap { it.second.issues }
        if (issues.isNotEmpty()) return DraftResult.Refused(issues.distinct())
        val rows = drafted.mapNotNull { it.second.occurrence }
        val event = drafted.map { it.second.drafted }
        return DraftResult.Drafted(Part2Slip(input.lastOrder, SlipOrdering.arrange(rows), findings(drafted, event), undrafted.distinct()))
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

    /** Drafts [first] and then every template its links bring in, each code once per request (a link that points back is a no-op). */
    private fun expand(
        request: OccurrenceRequest,
        first: Part2Template,
        drafted: MutableList<Pair<OccurrenceRequest, DraftedTemplate>>,
        undrafted: MutableList<UndraftedLink>,
    ) {
        val seen = mutableSetOf(first.code)
        val queue = ArrayDeque(listOf(first to (Part2Origin.Requested as Part2Origin)))
        while (queue.isNotEmpty()) {
            val (template, origin) = queue.removeFirst()
            val result = OccurrenceDrafter.draft(template, origin, groups[template.code], request)
            drafted += request to result
            for (link in template.links) {
                if (!applies(link, request, result.drafted)) continue
                val linked = Part2Origin.Linked(basisOf(link.basis), link.page, link.note)
                val target = templates[link.code]
                when {
                    target == null -> undrafted += UndraftedLink(link.code, linked)
                    seen.add(target.code) -> queue.addLast(target to linked)
                }
            }
        }
    }

    /** A link with no condition always applies; one whose condition the app does not know does not (the contract test stops that shipping). */
    private fun applies(
        link: Part2Link,
        request: OccurrenceRequest,
        parent: DraftedOccurrence,
    ): Boolean {
        val id = link.condition ?: return true
        return rules.condition(id)?.holds(RuleContext(request, parent, listOf(parent))) == true
    }

    private fun findings(
        drafted: List<Pair<OccurrenceRequest, DraftedTemplate>>,
        event: List<DraftedOccurrence>,
    ): List<RuleFinding> =
        drafted.flatMap { (request, result) ->
            val printed = result.drafted.template.let { it.print ?: it.code }
            result.drafted.template.rules.mapNotNull { id ->
                val decl = declarations[id]
                val rule = rules.check(id)
                if (decl == null || rule == null || !rule.violated(RuleContext(request, result.drafted, event))) return@mapNotNull null
                RuleFinding(id, if (decl.severity == "ack") RuleSeverity.ACK else RuleSeverity.WARN, printed, basisOf(decl.basis), decl.page, decl.note)
            }
        }.distinctBy { it.ruleId to it.code }

    private fun basisOf(basis: String) = if (basis == "manual") LinkBasis.MANUAL else LinkBasis.PRACTICE

    private companion object {
        const val LAST_ORDER = "last_order"
    }
}
