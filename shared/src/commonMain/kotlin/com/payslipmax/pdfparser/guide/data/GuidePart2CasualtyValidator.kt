package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2AboutPage
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2ColumnRow
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2RelatedLink
import com.payslipmax.pdfparser.guide.model.Part2RelatedRow

/**
 * Rechecks on every load what `part2_columns_check.py`, `part2_about_check.py` and `part2_related_check.py` enforced when the bundle was
 * built, so a damaged casualty section is refused instead of shown: a need the form builds fields from, plain bounded text, serials and
 * codes that exist, and a manual link that names its page. Each of the three lists is optional (a bundle built before them carries
 * none); the caps come from the section's own limits, so Kotlin holds no second copy of them. Problems name ids and field names only,
 * never a caption or page text, and must never be logged or reported. Plain scans, no regex, so it stays linear on Kotlin/Native.
 */
internal object GuidePart2CasualtyValidator {
    private val NEEDS = setOf("required", "optional", "none")
    private const val DATA_COLUMNS = 4
    private const val PAGE_MAX = 400

    fun validate(bundle: GuideBundle): List<String> {
        val section = bundle.part2 ?: return emptyList()
        val problems = mutableListOf<String>()
        if (section.columns.isNotEmpty()) problems += columnProblems(section)
        if (section.about.isNotEmpty()) problems += aboutProblems(section)
        if (section.related.isNotEmpty()) problems += relatedProblems(section)
        return problems
    }

    private fun columnProblems(section: Part2Lookup): List<String> {
        val limit = section.limits.caption
        if (limit <= 0) return listOf("part2: limits.caption must be above zero when columns are present")
        val known = section.serials.map { it.serial }.toSet()
        val problems = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (row in section.columns) {
            val where = "part2: columns ${row.serial}"
            if (row.serial !in known) problems += "$where: unknown serial"
            if (!seen.add(row.serial)) problems += "$where: duplicate"
            problems += rowProblems(where, row, limit)
        }
        known.filter { it !in seen }.forEach { problems += "part2: columns serial $it: no columns" }
        return problems
    }

    private fun rowProblems(
        where: String,
        row: Part2ColumnRow,
        limit: Int,
    ): List<String> {
        val problems = mutableListOf<String>()
        problems += columnProblems("$where from", row.from, limit)
        problems += columnProblems("$where to", row.to, limit)
        if (row.data.size != DATA_COLUMNS) problems += "$where: data must hold exactly four columns"
        row.data.forEachIndexed { at, column -> problems += columnProblems("$where data ${at + 1}", column, limit) }
        return problems
    }

    private fun columnProblems(
        where: String,
        column: Part2Column,
        limit: Int,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (column.need !in NEEDS) problems += "$where: need must be required, optional or none"
        if (column.need == "none" && column.caption.isNotEmpty()) problems += "$where: a none column carries no caption"
        if (column.caption.length > limit) problems += "$where: caption is over $limit characters"
        if (hasAngleBrackets(column.caption)) problems += "$where: caption has angle brackets (plain text only)"
        return problems
    }

    private fun aboutProblems(section: Part2Lookup): List<String> {
        val limit = section.limits.aboutPart
        if (limit <= 0) return listOf("part2: limits.about_part must be above zero when about pages are present")
        val known = section.serials.map { it.serial }.toSet()
        val homes = mutableSetOf<String>()
        val problems = mutableListOf<String>()
        for (page in section.about) {
            val where = "part2: about ${page.id}"
            if (page.serials.firstOrNull() != page.id) problems += "$where: id must be the first of its serials"
            for (serial in page.serials) {
                if (serial !in known) problems += "$where: unknown serial $serial"
                if (!homes.add(serial)) problems += "$where: serial $serial is on two pages (twice)"
            }
            problems += partProblems(where, page, limit)
        }
        return problems
    }

    private fun partProblems(
        where: String,
        page: Part2AboutPage,
        limit: Int,
    ): List<String> =
        textProblems(where, "what", page.what, limit) + textProblems(where, "when", page.whenSeen, limit) +
            textProblems(where, "ready", page.ready, limit) + textProblems(where, "watch", page.watch, limit)

    private fun relatedProblems(section: Part2Lookup): List<String> {
        val codes = section.serials.flatMap { it.codes }.toSet()
        val problems = mutableListOf<String>()
        val sources = mutableSetOf<String>()
        for (row in section.related) {
            val where = "part2: related ${row.code}"
            if (row.code !in codes) problems += "$where: unknown code"
            if (!sources.add(row.code)) problems += "$where: listed twice"
            problems += linkProblems(where, row, codes)
        }
        return problems
    }

    private fun linkProblems(
        where: String,
        row: Part2RelatedRow,
        codes: Set<String>,
    ): List<String> {
        if (row.withCodes.isEmpty()) return listOf("$where: no links")
        val problems = mutableListOf<String>()
        val targets = mutableSetOf<String>()
        for (link in row.withCodes) {
            if (link.code !in codes) problems += "$where: ${link.code} is an unknown code"
            if (link.code == row.code) problems += "$where: links to itself"
            if (!targets.add(link.code)) problems += "$where: ${link.code} repeats"
            problems += evidenceProblems("$where ${link.code}", link)
        }
        return problems
    }

    private fun evidenceProblems(
        where: String,
        link: Part2RelatedLink,
    ): List<String> =
        when (link.basis) {
            "manual" -> if (link.page == null || link.page !in 1..PAGE_MAX) listOf("$where: a manual link needs a printed page") else emptyList()
            "practice" -> if (link.page != null) listOf("$where: a practice link carries no page") else emptyList()
            else -> listOf("$where: basis must be manual or practice")
        }
}
