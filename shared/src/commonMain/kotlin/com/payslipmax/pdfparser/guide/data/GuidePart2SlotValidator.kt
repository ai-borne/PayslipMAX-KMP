package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.guide.model.Part2TemplateLimits

/**
 * The slot, placeholder and column rechecks of [GuidePart2TemplateValidator], mirroring `part2_slots.py`: every slot a text or a
 * column uses is declared, every declared slot is used, a reference is used by its number or its date, a column the manual leaves
 * empty names nothing. Problems name template codes, slot ids and field names only, never a word of content.
 */
internal object GuidePart2SlotValidator {
    val KINDS = listOf("date", "number", "text", "days", "ref")
    private val NEEDS = listOf("required", "optional", "none")
    private const val DATA_COLUMNS = 4

    /** Slot problems for one template, and the slot ids it declares (kind recorded in [kinds] for the cross-template check). */
    fun slotProblems(
        template: Part2Template,
        limits: Part2TemplateLimits,
        kinds: MutableMap<String, MutableSet<String>>,
    ): List<String> {
        val where = "template ${template.code}"
        val problems = mutableListOf<String>()
        if (template.slots.size > limits.slots) problems += "$where: has ${template.slots.size} slots, max ${limits.slots}"
        val declared = mutableMapOf<String, Part2Slot>()
        for (slot in template.slots) {
            val at = "$where: slot ${slot.id}"
            if (!isGuideId(slot.id)) problems += "$at: id must be lowercase letters, digits and underscores"
            if (slot.id in declared) problems += "$where: duplicate slot id ${slot.id}"
            declared[slot.id] = slot
            if (slot.kind !in KINDS) problems += "$at: kind must be ${KINDS.dropLast(1).joinToString()} or ${KINDS.last()}"
            kinds.getOrPut(slot.id) { mutableSetOf() }.add(slot.kind)
            problems += textProblems(at, "label", slot.label, limits.label) + textProblems(at, "help", slot.help, limits.help)
        }
        template.slots.forEach { problems += betweenProblems(it, where, declared) }
        val used = mutableSetOf<String>()
        problems += textUses(template, where, limits, declared, used)
        problems += columnUses(template, where, declared, used)
        used += template.slots.filter { it.kind == "days" }.flatMap { it.between }
        problems += declared.keys.filter { it !in used }.map { "$where: slot $it declared but never used" }
        return problems
    }

    private fun betweenProblems(
        slot: Part2Slot,
        where: String,
        declared: Map<String, Part2Slot>,
    ): List<String> {
        if (slot.kind != "days") return if (slot.between.isNotEmpty()) listOf("$where: slot ${slot.id}: only a days slot has between") else emptyList()
        val ok = slot.between.size == 2 && slot.between[0] != slot.between[1] && slot.between.all { declared[it]?.kind == "date" }
        return if (ok) emptyList() else listOf("$where: slot ${slot.id}: between must name two different date slots")
    }

    private fun useProblem(
        ref: SlotRef,
        where: String,
        declared: Map<String, Part2Slot>,
    ): String? {
        val kind = declared[ref.slot]?.kind
        return when {
            ref.slot !in declared -> "$where: slot ${ref.slot} used but not declared"
            kind == "ref" && ref.part == null -> "$where: slot ${ref.slot} is a reference, so use ${ref.slot}.no or ${ref.slot}.date"
            kind != "ref" && ref.part != null -> "$where: slot ${ref.slot} is not a reference, so it has no .${ref.part}"
            else -> null
        }
    }

    private fun textUses(
        template: Part2Template,
        where: String,
        limits: Part2TemplateLimits,
        declared: Map<String, Part2Slot>,
        used: MutableSet<String>,
    ): List<String> {
        val problems = mutableListOf<String>()
        val fields = listOf("narrative" to template.narrative, "authority" to template.authority, "certificate" to template.certificate)
        for ((field, text) in fields) {
            if (text == null) continue
            problems += textProblems(where, field, text, limits.narrative)
            val (refs, wellFormed) = Part2Placeholders.scan(text)
            val parsed = refs.map { Part2Placeholders.parse(it) }
            if (!wellFormed || null in parsed) problems += "$where: $field has a malformed placeholder"
            for (ref in parsed.filterNotNull()) {
                used += ref.slot
                useProblem(ref, where, declared)?.let { problems += it }
            }
        }
        return problems
    }

    private fun columnUses(
        template: Part2Template,
        where: String,
        declared: Map<String, Part2Slot>,
        used: MutableSet<String>,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (template.data.size != DATA_COLUMNS) problems += "$where: data must have exactly $DATA_COLUMNS columns"
        val columns = listOf("from" to template.from, "to" to template.to) + template.data.mapIndexed { i, c -> "data${i + 1}" to c }
        for ((name, column) in columns) problems += columnProblems("$where: column $name", name, column, where, declared, used)
        return problems
    }

    private fun columnProblems(
        at: String,
        name: String,
        column: Part2Column,
        where: String,
        declared: Map<String, Part2Slot>,
        used: MutableSet<String>,
    ): List<String> {
        val ref = column.slot?.let { Part2Placeholders.parse(it) }
        return when {
            column.need !in NEEDS -> listOf("$at need must be required, optional or none")
            column.need == "none" -> if (column.slot != null) listOf("$at is not required but names a slot") else emptyList()
            column.slot.isNullOrEmpty() -> listOf("$at needs a slot")
            ref == null -> listOf("$at has a malformed slot reference")
            else -> {
                used += ref.slot
                val dateOnly = name == "from" || name == "to"
                val notDate = dateOnly && declared[ref.slot] != null && (declared[ref.slot]?.kind != "date" || ref.part != null)
                listOfNotNull(useProblem(ref, where, declared), if (notDate) "$at must name a date slot" else null)
            }
        }
    }
}
