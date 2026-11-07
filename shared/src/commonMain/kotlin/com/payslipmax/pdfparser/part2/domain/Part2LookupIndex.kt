package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.domain.GuideRuleNumberParser
import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2ColumnRow
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2RelatedLink
import com.payslipmax.pdfparser.guide.model.Part2Words

/** Every ranking weight in one place. A query word scores its best field; an entry scores the sum over its words. */
object Part2SearchRanking {
    /** A serial typed whole, or a code typed whole. */
    const val EXACT = 100

    /** A code that starts with the word. */
    const val CODE_PREFIX = 80

    /** A name word that starts with the word. */
    const val NAME_WORD = 40
}

/** Whether [id] is a ribbon the officer can open: Learn shows all of them, so any ribbon the bundle carries. */
fun Part2Lookup.knowsRibbon(id: String): Boolean = ribbons.any { it.id == id }

/** Whether [code] is any one of a serial's codes; search reaches every code page. */
fun Part2Lookup.knowsCode(code: String): Boolean = serials.any { code in it.codes }

/**
 * The Part II lookup as the Learn screens read it, built once per loaded bundle (it never changes while the app runs): a tile for each
 * ribbon, a ribbon as one flat list under its case names, a casualty page and a search over all of the serials. Pure and plain scans
 * with no regex, so it stays linear on Kotlin/Native. A query is read here and nowhere else: nothing in this class stores, saves or logs it.
 * No group is ever inferred: a serial the manual gives none has none.
 */
class Part2LookupIndex(private val lookup: Part2Lookup) {
    private class Entry(val page: Part2CodePage, val nameWords: Set<String>)

    private val pages = lookup.serials.associateBy { it.serial }
    private val byCode = lookup.serials.flatMap { page -> page.codes.map { it to page } }.toMap()
    private val columns = lookup.columns.associateBy { it.serial }
    private val about = lookup.about.flatMap { page -> page.serials.map { it to page } }.toMap()
    private val related = lookup.related.groupBy { it.code }
    private val entries = lookup.serials.map { Entry(it, GuideRuleNumberParser.words(it.name).toSet()) }

    fun ribbons(): List<Part2RibbonTile> = lookup.ribbons.map { r -> Part2RibbonTile(r.id, r.title, r.cases.sumOf { it.serials.size }) }

    /** A ribbon's cases as headings over the casualties each homes; the serials a case only "also" lists are homed, and shown, elsewhere. */
    fun ribbon(id: String): Part2RibbonContent? =
        lookup.ribbons.firstOrNull { it.id == id }?.let { r ->
            Part2RibbonContent(r.id, r.title, r.cases.map { case -> Part2RibbonSection(case.title, case.serials.mapNotNull { pages[it]?.let(::row) }) })
        }

    /** The page for any one of a serial's codes (exact capitals, as the manual prints them). */
    fun code(code: String): Part2CodeContent? =
        byCode[code]?.let { page ->
            Part2CodeContent(
                serial = page.serial,
                codes = page.codes,
                name = page.name,
                group = page.group?.let { letter -> lookup.groups[letter]?.let { Part2GroupInfo(letter, it) } },
                chapter = page.chapter,
                page = page.page,
                columns = columns[page.serial]?.let(::columnLines).orEmpty(),
                about = about[page.serial]?.let { Part2AboutContent(it.what, it.whenSeen, it.ready, it.watch) },
                related = page.codes.flatMap { related[it].orEmpty() }.flatMap { it.withCodes }.distinctBy { it.code }.mapNotNull(::relatedLine),
            )
        }

    /** Only the columns an order uses: a `none` column has no line, and the position says which of the six it is. */
    private fun columnLines(row: Part2ColumnRow): List<Part2ColumnLine> =
        (listOf(row.from, row.to) + row.data).mapIndexedNotNull { position, column ->
            val need =
                when (column.need) {
                    Part2Words.REQUIRED -> Part2Need.REQUIRED
                    Part2Words.OPTIONAL -> Part2Need.OPTIONAL
                    else -> return@mapIndexedNotNull null
                }
            Part2ColumnLine(position, need, column.caption)
        }

    /** A companion the lookup cannot name is left out: a link with no name would be a dead end. */
    private fun relatedLine(link: Part2RelatedLink): Part2RelatedLine? =
        byCode[link.code]?.let { Part2RelatedLine(link.code, it.name, if (link.basis == Part2Words.MANUAL) Part2LinkBasis.MANUAL else Part2LinkBasis.PRACTICE, link.page) }

    /** Every word must match somewhere; best first, equal scores keep the manual's order. */
    fun search(query: String): Part2SearchResult {
        if (query.isBlank()) return Part2SearchResult.Idle
        if (!GuideSearchIndex.isSearchable(query)) return Part2SearchResult.TooShort
        val tokens = query.split(' ', '\t', '\n', '\r').filter { it.isNotEmpty() }
        val serialTokens = tokens.filter { it in pages }
        val words = tokens.filter { it !in pages }
        if (words.any(::isSerialShaped)) return Part2SearchResult.Results(emptyList())
        val terms = words.flatMap(GuideRuleNumberParser::words).distinct()
        val hits = entries.mapNotNull { score(it, serialTokens, terms) }.sortedByDescending { it.first }
        return Part2SearchResult.Results(hits.map { it.second })
    }

    private fun score(
        entry: Entry,
        serialTokens: List<String>,
        terms: List<String>,
    ): Pair<Int, Part2CodeRow>? {
        if (serialTokens.any { it != entry.page.serial }) return null
        var total = if (serialTokens.isEmpty()) 0 else Part2SearchRanking.EXACT
        var open: String? = null
        for (term in terms) {
            val code = codeFor(entry.page, term)
            total +=
                when {
                    code != null -> {
                        open = open ?: code.code
                        if (code.exact) Part2SearchRanking.EXACT else Part2SearchRanking.CODE_PREFIX
                    }
                    entry.nameWords.any { it.startsWith(term) } -> Part2SearchRanking.NAME_WORD
                    else -> return null
                }
        }
        return total to row(entry.page, openCode = open)
    }

    private class CodeHit(val code: String, val exact: Boolean)

    /** The code of [page] that [term] equals, else the first it starts, else null. */
    private fun codeFor(
        page: Part2CodePage,
        term: String,
    ): CodeHit? {
        page.codes.firstOrNull { it.lowercase() == term }?.let { return CodeHit(it, exact = true) }
        return page.codes.firstOrNull { it.lowercase().startsWith(term) }?.let { CodeHit(it, exact = false) }
    }

    private fun row(
        page: Part2CodePage,
        openCode: String? = null,
    ) = Part2CodeRow(page.serial, page.codes, openCode ?: page.codes.first(), page.name, page.group)

    /** Digits and dots with at least one dot, such as "3.2": a serial that is not one, which finds nothing rather than words. */
    private fun isSerialShaped(token: String): Boolean = '.' in token && token.all { it.isDigit() || it == '.' }
}
