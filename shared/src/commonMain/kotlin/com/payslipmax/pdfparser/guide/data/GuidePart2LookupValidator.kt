package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Case
import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2LookupLimits
import com.payslipmax.pdfparser.guide.model.Part2Ribbon

/**
 * Rechecks on every load what `part2_codes_check.py` and `part2_nav.py` enforced when the bundle was built, so a damaged lookup is
 * refused instead of shown: well-formed serials, codes and pages, a group the manual defines (or none, for the two serials it leaves
 * blank), no code on two pages, plain bounded text, and a layout that homes every serial exactly once within the carried limits. A
 * bundle with no Part II section is valid. Problems name ids and field names only, never a name or title, and must never be logged
 * or reported. Plain scans, no regex, so it stays linear on Kotlin/Native.
 */
internal object GuidePart2LookupValidator {
    const val SUPPORTED_VERSION = 1

    /** The two serials whose group Annexure II leaves blank; the app shows "Group not printed in ODP-2026" and infers none. */
    val NO_GROUP_SERIALS = setOf("3.28", "11.8")
    private val GROUPS = setOf("A", "AM", "B", "C", "CM")
    private const val CODE_MIN = 2
    private const val CODE_MAX = 12
    private const val CHAPTER_MAX_DIGITS = 2
    private const val NUMBER_MAX_DIGITS = 3
    private const val PAGE_MAX = 400

    fun validate(bundle: GuideBundle): List<String> {
        val section = bundle.part2 ?: return emptyList()
        if (section.version != SUPPORTED_VERSION) return listOf("part2: version must be $SUPPORTED_VERSION")
        val limits = section.limits
        val numbers = with(limits) { listOf(text, titleWords, ribbonsMin, ribbonsMax, casesPerRibbon, caseSerialsMin, caseSerialsMax) }
        if (numbers.any { it <= 0 }) return listOf("part2: limits must be above zero")
        return groupProblems(section, limits) + codePageProblems(section, limits) + layoutProblems(section, limits)
    }

    private fun groupProblems(
        section: Part2Lookup,
        limits: Part2LookupLimits,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (section.groups.keys != GROUPS) problems += "part2: groups must be exactly ${GROUPS.joinToString()}"
        section.groups.forEach { (letter, meaning) -> problems += textProblems("part2: group $letter", "meaning", meaning, limits.text) }
        return problems
    }

    private fun codePageProblems(
        section: Part2Lookup,
        limits: Part2LookupLimits,
    ): List<String> {
        val problems = mutableListOf<String>()
        val serials = mutableSetOf<String>()
        val codes = mutableSetOf<String>()
        section.serials.forEachIndexed { index, page ->
            val where = if (isSerial(page.serial)) "part2: serial ${page.serial}" else "part2: serial at position ${index + 1}"
            if (!isSerial(page.serial)) problems += "$where: not a chapter.number serial"
            if (!serials.add(page.serial)) problems += "$where: duplicate serial"
            problems += pageProblems(where, page, section.groups, limits)
            page.codes.forEachIndexed { at, code ->
                when {
                    !isCode(code) -> problems += "$where: code at position ${at + 1} must be $CODE_MIN-$CODE_MAX capital letters or digits"
                    !codes.add(code) -> problems += "$where: duplicate code $code"
                }
            }
        }
        return problems
    }

    private fun pageProblems(
        where: String,
        page: Part2CodePage,
        groups: Map<String, String>,
        limits: Part2LookupLimits,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (isSerial(page.serial) && page.chapter != page.serial.substringBefore('.').toInt()) problems += "$where: chapter does not match the serial"
        if (page.codes.isEmpty()) problems += "$where: no codes"
        problems += textProblems(where, "name", page.name, limits.text)
        when {
            page.serial in NO_GROUP_SERIALS -> if (page.group != null) problems += "$where: the manual prints no group, so none may be inferred"
            page.group == null || page.group !in groups -> problems += "$where: group is not one the manual defines"
        }
        if (page.page !in 1..PAGE_MAX) problems += "$where: page must be a whole number from 1 to $PAGE_MAX"
        return problems
    }

    private fun layoutProblems(
        section: Part2Lookup,
        limits: Part2LookupLimits,
    ): List<String> {
        val problems = mutableListOf<String>()
        val known = section.serials.map { it.serial }.toSet()
        if (section.ribbons.size !in limits.ribbonsMin..limits.ribbonsMax) {
            problems += "part2: ${section.ribbons.size} ribbons, allowed ${limits.ribbonsMin} to ${limits.ribbonsMax}"
        }
        problems += duplicateIdProblems("part2 ribbon", section.ribbons.map { it.id })
        problems += duplicateIdProblems("part2 case", section.ribbons.flatMap { r -> r.cases.map { it.id } })
        val homes = mutableMapOf<String, String>()
        section.ribbons.forEach { problems += ribbonProblems(it, limits, known, homes) }
        known.filter { it !in homes }.forEach { problems += "part2: serial $it has no home" }
        return problems
    }

    private fun ribbonProblems(
        ribbon: Part2Ribbon,
        limits: Part2LookupLimits,
        known: Set<String>,
        homes: MutableMap<String, String>,
    ): List<String> {
        val where = "part2: ribbon ${ribbon.id}"
        val problems = mutableListOf<String>()
        if (!isGuideId(ribbon.id)) problems += "$where: id is malformed"
        problems += titleProblems(where, ribbon.title, limits)
        if (ribbon.cases.isEmpty() || ribbon.cases.size > limits.casesPerRibbon) {
            problems += "$where has ${ribbon.cases.size} cases, max ${limits.casesPerRibbon}"
        }
        ribbon.cases.forEach { problems += caseProblems(it, limits, known, homes) }
        return problems
    }

    private fun caseProblems(
        case: Part2Case,
        limits: Part2LookupLimits,
        known: Set<String>,
        homes: MutableMap<String, String>,
    ): List<String> {
        val where = "part2: case ${case.id}"
        val problems = mutableListOf<String>()
        if (!isGuideId(case.id)) problems += "$where: id is malformed"
        problems += titleProblems(where, case.title, limits)
        if (case.serials.size !in limits.caseSerialsMin..limits.caseSerialsMax) {
            problems += "$where has ${case.serials.size} serials, allowed ${limits.caseSerialsMin} to ${limits.caseSerialsMax}"
        }
        for (serial in case.serials) {
            when {
                serial !in known -> problems += "$where: unknown serial $serial"
                serial in homes -> problems += "part2: serial $serial homed twice (${homes[serial]}, ${case.id})"
                else -> homes[serial] = case.id
            }
        }
        problems += repeats(where, "also", case.also) + repeats(where, "lookup", case.lookup)
        for (item in case.also.distinct()) {
            if (item !in known) problems += "$where: also $item: unknown serial"
            if (item in case.serials) problems += "$where: also $item is homed in this case"
        }
        problems += case.lookup.distinct().filter { it !in case.serials }.map { "$where: lookup $it is not in the case" }
        return problems
    }

    private fun titleProblems(
        where: String,
        title: String,
        limits: Part2LookupLimits,
    ): List<String> =
        when {
            title.isBlank() -> listOf("$where: title is empty")
            wordCount(title) > limits.titleWords -> listOf("$where: title over ${limits.titleWords} words")
            '<' in title || '>' in title -> listOf("$where: title has angle brackets (plain text only)")
            else -> emptyList()
        }

    private fun repeats(
        where: String,
        key: String,
        items: List<String>,
    ): List<String> = items.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.map { "$where: $key $it repeats" }

    /** Whitespace-separated words, as `part2_nav.py` counts them. */
    private fun wordCount(text: String): Int {
        var count = 0
        var inWord = false
        for (ch in text) {
            val isWordChar = !ch.isWhitespace()
            if (isWordChar && !inWord) count++
            inWord = isWordChar
        }
        return count
    }

    /** `chapter.number`: one or two digits, a dot, one to three digits. */
    private fun isSerial(value: String): Boolean {
        val chapter = value.substringBefore('.', "")
        val number = value.substringAfter('.', "")
        return chapter.length in 1..CHAPTER_MAX_DIGITS && number.length in 1..NUMBER_MAX_DIGITS && (chapter + number).all { it in '0'..'9' }
    }

    private fun isCode(value: String): Boolean = value.length in CODE_MIN..CODE_MAX && value.all { it in 'A'..'Z' || it in '0'..'9' }
}
