package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.domain.GuideRuleNumberParser
import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import com.payslipmax.pdfparser.guide.model.Part2Case
import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2Lookup

/** Every ranking weight in one place. A query word scores its best field; an entry scores the sum over its words. */
object Part2SearchRanking {
    /** A serial typed whole, or a code typed whole. */
    const val EXACT = 100

    /** A code that starts with the word. */
    const val CODE_PREFIX = 80

    /** A name word that starts with the word. */
    const val NAME_WORD = 40
}

/** Whether [id] is a ribbon the officer can open: it exists and its tiles ship (Round 8). */
fun Part2Lookup.knowsRibbon(id: String): Boolean = ribbons.any { it.built && it.id == id }

/** Whether [id] is a case inside a built ribbon. */
fun Part2Lookup.knowsCase(id: String): Boolean = ribbons.any { r -> r.built && r.cases.any { it.id == id } }

/** Whether [code] is any one of a serial's codes; a code in an unbuilt ribbon counts, because search reaches every code page. */
fun Part2Lookup.knowsCode(code: String): Boolean = serials.any { code in it.codes }

/**
 * The Part II lookup as the browse screens read it, built once per loaded bundle (it never changes while the app runs): tiles for the
 * built ribbons only, a case's codes, a code page and a search over all of the serials, built or not. Pure and plain scans with no
 * regex, so it stays linear on Kotlin/Native. A query is read here and nowhere else: nothing in this class stores, saves or logs it.
 * No group is ever inferred: a serial the manual gives none has none.
 */
class Part2LookupIndex(private val lookup: Part2Lookup) {
    private class Entry(val page: Part2CodePage, val nameWords: Set<String>)

    private val pages = lookup.serials.associateBy { it.serial }
    private val byCode = lookup.serials.flatMap { page -> page.codes.map { it to page } }.toMap()
    private val printed = lookup.templates.mapNotNull { t -> t.print?.let { t.code to it } }.toMap()
    private val builtRibbons = lookup.ribbons.filter { it.built }
    private val entries = lookup.serials.map { Entry(it, GuideRuleNumberParser.words(it.name).toSet()) }

    fun ribbons(): List<Part2RibbonTile> = builtRibbons.map { Part2RibbonTile(it.id, it.title, it.cases.size) }

    fun ribbon(id: String): Part2RibbonContent? = builtRibbons.firstOrNull { it.id == id }?.let { r -> Part2RibbonContent(r.id, r.title, r.cases.map(::tile)) }

    fun case(id: String): Part2CaseContent? =
        builtRibbons.flatMap { it.cases }.firstOrNull { it.id == id }?.let { case ->
            val lookupOnly = case.lookup.toSet()
            Part2CaseContent(
                id = case.id,
                title = case.title,
                span = span(case.serials),
                rows = case.serials.mapNotNull { pages[it]?.let { page -> row(page, page.serial in lookupOnly) } },
                also = case.also.mapNotNull { pages[it]?.let { page -> row(page, lookupOnly = false) } },
            )
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
                printedAs = page.codes.mapNotNull { c -> printed[c]?.let { Part2PrintedAs(c, it) } },
            )
        }

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
        return total to row(entry.page, lookupOnly = false, openCode = open)
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
        lookupOnly: Boolean,
        openCode: String? = null,
    ) = Part2CodeRow(page.serial, page.codes, openCode ?: page.codes.first(), page.name, page.group, lookupOnly)

    private fun tile(case: Part2Case) = Part2CaseTile(case.id, case.title, span(case.serials), case.serials.size)

    private fun span(serials: List<String>): Part2Span {
        val homed = serials.mapNotNull { pages[it] }
        if (homed.isEmpty()) return Part2Span(emptyList(), 0, 0)
        return Part2Span(homed.map { it.chapter }.distinct().sorted(), homed.minOf { it.page }, homed.maxOf { it.page })
    }

    /** Digits and dots with at least one dot, such as "3.2": a serial that is not one, which finds nothing rather than words. */
    private fun isSerialShaped(token: String): Boolean = '.' in token && token.all { it.isDigit() || it == '.' }
}
