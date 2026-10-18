package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideOptionGroup
import com.payslipmax.pdfparser.guide.model.GuidePreClaim
import com.payslipmax.pdfparser.guide.model.GuidePreClaimField
import com.payslipmax.pdfparser.guide.model.GuidePreClaimLimits
import com.payslipmax.pdfparser.guide.model.GuidePreClaimMove

/**
 * Rechecks on every load what `preclaim.py` enforced when the bundle was built, so a broken worksheet is refused instead of drawn:
 * a known move type with fields and documents, short plain-text labels, ids that are unique and well formed, a choice that names a
 * real option group, and a rule-picker option that links to a live card. A bundle with no pre-claim section is valid. Problems
 * name ids and field names only, never the worksheet text, and must never be logged or reported. Plain scans, no regex.
 */
internal object GuidePreClaimValidator {
    private val MOVES = setOf("PD", "TD", "LTC")
    private val KINDS = setOf("text", "date", "number", "amount", "tick", "choice")
    private val SOURCES = setOf("portal_filled", "officer_types", "app_suggests")
    private const val KIND_CHOICE = "choice"
    private const val KIND_TEXT = "text"
    private const val ID_MAX = 32
    private const val MAX_SHORT_WORDS = 3

    fun validate(bundle: GuideBundle): List<String> {
        val section = bundle.preclaim ?: return emptyList()
        val problems = mutableListOf<String>()
        problems += uploadProblems(section)
        val limits = section.limits
        if (listOf(limits.label, limits.hint, limits.textChars).any { it <= 0 }) return problems + "preclaim: limits must be above zero"
        val live = bundle.cards.filterNot { it.isReplaced }.map { it.id }.toSet()
        val known = bundle.cards.map { it.id }.toSet()
        section.options.forEach { (id, group) -> problems += groupProblems(id, group, limits, live, known) }
        problems += section.moves.keys.filter { it !in MOVES }.map { "preclaim: unknown move $it" }
        section.moves.filterKeys { it in MOVES }.forEach { (id, move) -> problems += moveProblems(id, move, limits, section.options) }
        return problems
    }

    private fun uploadProblems(section: GuidePreClaim): List<String> {
        val upload = section.upload
        return buildList {
            if (upload.files != 1) add("preclaim: upload files must be 1")
            if (upload.moreThanKb <= 0) add("preclaim: upload more_than_kb must be above zero")
            if (upload.lessThanMb <= 0) add("preclaim: upload less_than_mb must be above zero")
            if (upload.format != "PDF") add("preclaim: upload format must be PDF")
        }
    }

    private fun groupProblems(
        id: String,
        group: GuideOptionGroup,
        limits: GuidePreClaimLimits,
        live: Set<String>,
        known: Set<String>,
    ): List<String> {
        val where = "preclaim: options $id"
        val problems = mutableListOf<String>()
        if (!isId(id)) problems += "$where: group id is malformed"
        if (group.options.isEmpty()) problems += "$where: no options"
        problems += duplicates(where, group.options.map { it.id })
        for (option in group.options) {
            val at = "$where option ${option.id}"
            if (!isId(option.id)) problems += "$at: id is malformed"
            problems += textProblems(at, "label", option.label, limits.label)
            problems += textProblems(at, "use_when", option.useWhen, limits.hint, required = group.cardLinked)
            problems += textProblems(at, "short", option.short, limits.label, required = false)
            if (option.short.isNotBlank() && option.short.trim().split(Regex("\\s+")).size > MAX_SHORT_WORDS) {
                problems += "$at: short is over $MAX_SHORT_WORDS words"
            }
            problems +=
                when {
                    !group.cardLinked -> if (option.card.isEmpty()) emptyList() else listOf("$at: names a card but the group is not card_linked")
                    option.card.isEmpty() -> listOf("$at: needs a card")
                    option.card !in known -> listOf("$at: card ${option.card} is not in the bundle")
                    option.card !in live -> listOf("$at: card ${option.card} is replaced")
                    else -> emptyList()
                }
        }
        return problems
    }

    private fun moveProblems(
        id: String,
        move: GuidePreClaimMove,
        limits: GuidePreClaimLimits,
        groups: Map<String, GuideOptionGroup>,
    ): List<String> {
        val where = "preclaim: move $id"
        val problems = mutableListOf<String>()
        problems += textProblems(where, "title", move.title, limits.label)
        if (move.fields.isEmpty()) problems += "$where: no fields"
        if (move.documents.isEmpty()) problems += "$where: no documents"
        problems += duplicates("$where fields", move.fields.map { it.id })
        problems += duplicates("$where documents", move.documents.map { it.id })
        move.fields.forEach { problems += fieldProblems("$where field ${it.id}", it, limits, groups) }
        for (doc in move.documents) {
            val at = "$where document ${doc.id}"
            if (!isId(doc.id)) problems += "$at: id is malformed"
            problems += textProblems(at, "label", doc.label, limits.hint)
            problems += textProblems(at, "when", doc.whenNeeded, limits.hint, required = false)
        }
        move.notes.forEach { problems += textProblems(where, "note", it, limits.hint) }
        return problems
    }

    private fun fieldProblems(
        at: String,
        field: GuidePreClaimField,
        limits: GuidePreClaimLimits,
        groups: Map<String, GuideOptionGroup>,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (!isId(field.id)) problems += "$at: id is malformed"
        problems += textProblems(at, "label", field.label, limits.label)
        problems += textProblems(at, "hint", field.hint, limits.hint, required = field.unverified)
        if (field.kind !in KINDS) problems += "$at: kind is not one the app knows"
        if (field.source !in SOURCES) problems += "$at: source is not one the app knows"
        if (field.maxChars != 0 && (field.kind != KIND_TEXT || field.maxChars !in 1..limits.textChars)) {
            problems += "$at: max_chars must be 1 to ${limits.textChars} and only on a text field"
        }
        if (field.kind == KIND_CHOICE && field.options !in groups) problems += "$at: options names '${field.options}', which is not an option group"
        if (field.kind != KIND_CHOICE && field.options.isNotEmpty()) problems += "$at: options only applies to a choice field"
        return problems
    }

    /** Plain text only: the worksheet is drawn as text, so angle brackets are refused rather than escaped. */
    private fun textProblems(
        where: String,
        name: String,
        value: String,
        limit: Int,
        required: Boolean = true,
    ): List<String> =
        when {
            value.isBlank() -> if (required) listOf("$where: $name is empty") else emptyList()
            value.length > limit -> listOf("$where: $name is over $limit characters")
            '<' in value || '>' in value -> listOf("$where: $name has angle brackets (plain text only)")
            else -> emptyList()
        }

    private fun duplicates(
        where: String,
        ids: List<String>,
    ): List<String> = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.map { "$where: duplicate id $it" }

    /** Lowercase letters, digits and underscores, starting with a letter, at most [ID_MAX] long. */
    private fun isId(value: String): Boolean =
        value.isNotEmpty() && value.length <= ID_MAX && value[0] in 'a'..'z' && value.all { it in 'a'..'z' || it in '0'..'9' || it == '_' }
}
