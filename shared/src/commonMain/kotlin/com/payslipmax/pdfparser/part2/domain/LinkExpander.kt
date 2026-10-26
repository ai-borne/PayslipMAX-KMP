package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.model.Part2Link
import com.payslipmax.pdfparser.guide.model.Part2Template

internal fun basisOf(basis: String) = if (basis == "manual") LinkBasis.MANUAL else LinkBasis.PRACTICE

/**
 * Follows a request's links: its own template, then every template a link brings in whose condition holds, each code once per request
 * (a link that points back is a no-op). The engine drafts from what it finds and the leave form asks for the slots of what it finds, so
 * both read one expansion and can never disagree about which orders an answer adds. Knows no code.
 */
internal class LinkExpander(
    private val templates: Map<String, Part2Template>,
    private val groups: Map<String, String?>,
    private val rules: Part2Rules,
) {
    /** Drafts [first] and its linked templates into [drafted]; a link whose template does not exist goes to [undrafted], never dropped. */
    fun expand(
        request: OccurrenceRequest,
        first: Part2Template,
        drafted: MutableList<Pair<OccurrenceRequest, DraftedTemplate>>,
        undrafted: MutableList<UndraftedLink>,
    ) {
        val seen = mutableSetOf(first.code)
        val queue = ArrayDeque(listOf(first to (Part2Origin.Requested as Part2Origin)))
        while (queue.isNotEmpty()) {
            val (template, origin) = queue.removeFirst()
            val result = OccurrenceDrafter.draft(template, origin, groups[template.code], request, rules)
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
}
