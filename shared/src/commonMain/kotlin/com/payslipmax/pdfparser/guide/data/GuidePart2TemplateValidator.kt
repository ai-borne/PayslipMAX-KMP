package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Link
import com.payslipmax.pdfparser.guide.model.Part2RuleDecl
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.guide.model.Part2TemplateLimits

/**
 * Rechecks on every load what `part2_templates.py` and `part2_rules.py` enforced when the bundle was built, so a damaged template is
 * refused instead of printed: a known code, a page citation, a known rule of the right kind, a link with a manual page or the
 * `practice` label, plain bounded text, declared slots and columns ([GuidePart2SlotValidator]), and the one printed-code override the
 * manual allows. A section with no rules and no templates is valid. Problems name ids and field names only, never a word of content,
 * and must never be logged or reported. Plain scans, no regex, so it stays linear on Kotlin/Native.
 */
internal object GuidePart2TemplateValidator {
    /** The one code the slip prints differently from the manual's Annexure II (Round 9): HRMS publishes REJOIN. */
    val PRINT_OVERRIDES = mapOf("REJOINAL" to "REJOIN")
    private const val PAGE_MAX = 400
    private val BASES = listOf("manual", "practice")

    fun validate(bundle: GuideBundle): List<String> {
        val section = bundle.part2 ?: return emptyList()
        val limits = section.templateLimits
        if (limits == null) {
            return if (section.rules.isEmpty() && section.templates.isEmpty()) emptyList() else listOf("part2 templates: template_limits are missing")
        }
        val numbers = with(limits) { listOf(narrative, label, help, slots, links, rules, note) }
        if (numbers.any { it <= 0 }) return listOf("part2 templates: limits must be above zero")
        val problems = mutableListOf<String>()
        problems += duplicateIdProblems("rule", section.rules.map { it.id })
        section.rules.forEach { problems += ruleProblems(it, limits) }
        val rules = section.rules.associateBy { it.id }
        val codes = section.serials.flatMap { it.codes }.toSet()
        val kinds = mutableMapOf<String, MutableSet<String>>()
        val seen = mutableSetOf<String>()
        for (template in section.templates) {
            if (!seen.add(template.code)) problems += "template ${template.code}: duplicate template"
            problems += templateProblems(template, limits, rules, codes, kinds)
        }
        problems += kinds.filterValues { it.size > 1 }.map { (id, found) -> "slot $id: declared with different kinds (${found.sorted().joinToString()})" }
        return problems
    }

    private fun ruleProblems(
        rule: Part2RuleDecl,
        limits: Part2TemplateLimits,
    ): List<String> {
        val where = "rule ${rule.id}"
        val problems = mutableListOf<String>()
        if (!isGuideId(rule.id)) problems += "$where: id must be lowercase letters, digits and underscores"
        when {
            rule.kind != "check" && rule.kind != "condition" && rule.kind != "derive" -> problems += "$where: kind must be check, condition or derive"
            rule.kind == "check" && rule.severity != "warn" && rule.severity != "ack" -> problems += "$where: a check needs severity warn or ack"
            rule.kind == "condition" && rule.severity != null -> problems += "$where: a condition has no severity"
            rule.kind == "derive" && rule.severity != null -> problems += "$where: a derive rule has no severity"
        }
        when {
            rule.basis !in BASES -> problems += "$where: basis must be manual or practice"
            rule.basis == "manual" && !validPage(rule.page) -> problems += "$where: a manual rule needs a page"
            rule.basis == "practice" && rule.page != null -> problems += "$where: a practice rule cites no page"
        }
        rule.note?.let { problems += textProblems(where, "note", it, limits.note) }
        return problems
    }

    private fun templateProblems(
        template: Part2Template,
        limits: Part2TemplateLimits,
        rules: Map<String, Part2RuleDecl>,
        codes: Set<String>,
        kinds: MutableMap<String, MutableSet<String>>,
    ): List<String> {
        val where = "template ${template.code}"
        val problems = mutableListOf<String>()
        if (template.code !in codes) problems += "$where: unknown code"
        if (!validPage(template.page)) problems += "$where: page must be a whole number from 1 to $PAGE_MAX"
        if (template.print != null && PRINT_OVERRIDES[template.code] != template.print) problems += "$where: print override must be REJOINAL to REJOIN"
        if (template.rev.length != Part2TemplateRev.LENGTH || !template.rev.all { it in '0'..'9' || it in 'a'..'f' }) {
            problems += "$where: rev must be ${Part2TemplateRev.LENGTH} lowercase hex characters"
        } else if (template.rev != Part2TemplateRev.of(template)) {
            problems += "$where: rev is not the revision of this template's words"
        }
        problems += GuidePart2SlotValidator.slotProblems(template, limits, kinds, rules.mapValues { it.value.kind })
        problems += checkProblems(template, where, limits, rules)
        problems += linkProblems(template, where, limits, rules, codes)
        return problems
    }

    private fun checkProblems(
        template: Part2Template,
        where: String,
        limits: Part2TemplateLimits,
        rules: Map<String, Part2RuleDecl>,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (template.rules.size > limits.rules) problems += "$where: has ${template.rules.size} rules, max ${limits.rules}"
        for (id in template.rules) {
            when {
                id !in rules -> problems += "$where: rule $id is unknown"
                rules.getValue(id).kind != "check" -> problems += "$where: rule $id is not a check"
            }
        }
        problems += template.rules.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.map { "$where: rule $it repeats" }
        return problems
    }

    private fun linkProblems(
        template: Part2Template,
        where: String,
        limits: Part2TemplateLimits,
        rules: Map<String, Part2RuleDecl>,
        codes: Set<String>,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (template.links.size > limits.links) problems += "$where: has ${template.links.size} links, max ${limits.links}"
        for (link in template.links) problems += oneLink(link, template.code, "$where: link to ${link.code}", limits, rules, codes)
        return problems
    }

    private fun oneLink(
        link: Part2Link,
        owner: String,
        at: String,
        limits: Part2TemplateLimits,
        rules: Map<String, Part2RuleDecl>,
        codes: Set<String>,
    ): List<String> {
        val problems = mutableListOf<String>()
        when {
            link.code !in codes -> problems += "$at: unknown code"
            link.code == owner -> problems += "$at: a template cannot link to itself"
        }
        when {
            link.basis !in BASES -> problems += "$at: basis must be manual or practice"
            link.basis == "manual" && !validPage(link.page) -> problems += "$at: a manual link needs a page"
            link.basis == "practice" && link.page != null -> problems += "$at: a practice link cites no page"
        }
        link.note?.let { problems += textProblems(at, "note", it, limits.note) }
        val condition = link.condition
        when {
            condition == null -> Unit
            condition !in rules -> problems += "$at: condition $condition is unknown"
            rules.getValue(condition).kind != "condition" -> problems += "$at: condition $condition is not a condition"
        }
        return problems
    }

    private fun validPage(page: Int?): Boolean = page != null && page in 1..PAGE_MAX
}
