package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template

/** One input of the leave form: the template's [slot] (its label and help are content) and whether the slip cannot be drafted without it. */
data class FormField(
    val slot: Part2Slot,
    val required: Boolean,
)

/**
 * What the leave form asks for one request, read from the same link expansion the engine drafts from ([Part2DraftEngine.templatesFor]),
 * so the form asks for exactly what the orders on the slip print: an officer who is not on ration strength is never asked when the
 * ration starts. The questions come from the rule families ([Part2Rules.questions]). Names no code and no slot.
 */
class LeaveFormPlanner(
    private val engine: Part2DraftEngine,
    private val rules: Part2Rules,
) {
    /** The inputs the request needs now: every slot the app does not work out, once per id, the first template to declare it deciding the place. */
    fun fields(request: OccurrenceRequest): List<FormField> {
        val templates = templates(request)
        val required = templates.flatMap { OccurrenceDrafter.requiredSlots(it) }.toSet()
        return templates
            .flatMap { it.slots }
            .filterNot { OccurrenceDrafter.isWorkedOut(it.kind) }
            .distinctBy(Part2Slot::id)
            .map { FormField(it, it.id in required) }
    }

    /** The yes/no and choice questions that decide which orders go with [code] or which checks it runs, whatever has been answered so far. */
    fun questions(code: String): List<AnswerQuestion> {
        val conditions = reachable(code).flatMap { template -> template.links.mapNotNull { it.condition } + template.rules }.toSet()
        return rules.questions.filter { question -> question.rules.any { it in conditions } }
    }

    /** The templates the request drafts now: its own and every linked one whose condition holds. */
    fun templates(request: OccurrenceRequest): List<Part2Template> = engine.templatesFor(request)

    /** Every template [code] can bring in, whatever the answers: a walk over the links that ignores conditions and cannot loop. */
    private fun reachable(code: String): List<Part2Template> {
        val seen = mutableSetOf<String>()
        val order = mutableListOf<Part2Template>()
        val queue = ArrayDeque(listOf(code))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            val template = engine.template(next)?.takeIf { seen.add(it.code) } ?: continue
            order += template
            template.links.forEach { queue.addLast(it.code) }
        }
        return order
    }
}
