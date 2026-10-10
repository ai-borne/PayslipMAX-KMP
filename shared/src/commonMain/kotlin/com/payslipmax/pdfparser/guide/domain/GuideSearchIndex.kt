package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideCard

/** Every ranking weight in one place. A query word scores its best field; a card scores the sum over its words. */
object GuideSearchRanking {
    /** A rule number in the card's cite, or in the rule line of the case it is homed in. */
    const val RULE_NUMBER = 100
    const val TITLE = 60
    const val ANSWER = 30
    const val BULLETS = 20
    const val DETAILS = 10

    /** A word found only in the user's own note: below every field of the card itself. */
    const val NOTE = 5

    /** A query shorter than this (letters and digits) is not searched: one letter matches half the Guide. */
    const val MIN_QUERY_CHARS = 2
}

/**
 * What a search may read. [FULL] is Premium: every field. [PREVIEW] is the free preview (owner decision 2026-10-07):
 * titles and rule numbers only, so a free user cannot learn from the result list what the locked key points,
 * attach, watch-out or details say, and the one-line answer (shown free) is not searched either.
 */
enum class GuideSearchScope { FULL, PREVIEW }

/** A card that matched, with its score; a higher score ranks first. */
data class GuideSearchHit(
    val card: GuideCard,
    val score: Int,
    /** True when at least one query word matched only in the user's note, so the card is shown as "In your note". */
    val matchedInNote: Boolean = false,
)

/**
 * Search over a loaded bundle, built once per load (the bundle never changes while the app runs). A replaced card is not
 * searched: its successor is, and the successor links back to it. A query is its
 * words, and every word must match somewhere in the card: a letter word as the start of a word ("allow" finds
 * "allowance"), a number as a whole number (see [GuideRuleNumberParser.numberMatches]). A leading "Rule" or "Rules"
 * is dropped, so "Rule 114" and "114" ask the same. Case and punctuation never matter. The query is read here and
 * nowhere else: nothing in this class stores, saves or logs it. The user's notes are handed in with each search
 * ([GuideSearchNotes]), never fetched or kept, and only the full scope reads them.
 */
class GuideSearchIndex(
    bundle: GuideBundle,
) {
    private class Entry(
        val card: GuideCard,
        val title: Set<String>,
        val answer: Set<String>,
        val bullets: Set<String>,
        val details: Set<String>,
        val rules: Set<String>,
    )

    private val entries: List<Entry> =
        bundle.cards.let { cards ->
            val caseRules = bundle.nav.flatMap { it.cases }.associate { it.id to GuideRuleNumberParser.ruleNumbers(it.sub) }
            cards.filterNot { it.isReplaced }.map { card ->
                val body = CardTemplate.body(card)
                Entry(
                    card = card,
                    title = wordSet(card.title),
                    answer = wordSet(card.answer),
                    bullets = wordSet((body.key + body.attach + body.watch).joinToString(" ")),
                    details = wordSet(card.details),
                    rules = GuideRuleNumberParser.ruleNumbers(card.cite) + caseRules[card.nav].orEmpty(),
                )
            }
        }

    /** The matching cards, best first; equal scores keep the bundle's order. Empty for a query that is too short. */
    fun search(
        query: String,
        scope: GuideSearchScope = GuideSearchScope.FULL,
        notes: GuideSearchNotes = GuideSearchNotes.None,
    ): List<GuideSearchHit> {
        val terms = terms(query)
        if (terms.isEmpty()) return emptyList()
        // The free preview reads titles and rule numbers only; a note is never part of it.
        val readable = if (scope == GuideSearchScope.FULL) notes else GuideSearchNotes.None
        return entries
            .mapNotNull { entry -> score(entry, terms, scope, readable.words(entry.card.id))?.let { GuideSearchHit(entry.card, it.total, it.viaNote) } }
            .sortedByDescending { it.score }
    }

    private class Scored(val total: Int, val viaNote: Boolean)

    /** The query words that must all match, with a leading "Rule" or "Rules" dropped. Empty when too short. */
    private fun terms(query: String): List<String> {
        if (!isSearchable(query)) return emptyList()
        val words = GuideRuleNumberParser.words(query)
        val withoutRule = if (words.size > 1 && words.first() in GuideRuleNumberParser.RULE_KEYWORDS) words.drop(1) else words
        return withoutRule.distinct()
    }

    /** The card's score, or null when any query word matches nowhere (in the card, or in the note when one is readable). */
    private fun score(
        entry: Entry,
        terms: List<String>,
        scope: GuideSearchScope,
        noteWords: Set<String>,
    ): Scored? {
        var total = 0
        var viaNote = false
        for (term in terms) {
            val best = bestWeight(entry, term, scope)
            if (best > 0) {
                total += best
            } else if (noteWords.isNotEmpty() && matches(noteWords, term)) {
                total += GuideSearchRanking.NOTE
                viaNote = true
            } else {
                return null
            }
        }
        return Scored(total, viaNote)
    }

    private fun bestWeight(
        entry: Entry,
        term: String,
        scope: GuideSearchScope,
    ): Int {
        val isNumber = term.first().isDigit()

        fun hits(words: Set<String>) = matches(words, term)
        return when {
            isNumber && hits(entry.rules) -> GuideSearchRanking.RULE_NUMBER
            hits(entry.title) -> GuideSearchRanking.TITLE
            scope == GuideSearchScope.PREVIEW -> 0
            hits(entry.answer) -> GuideSearchRanking.ANSWER
            hits(entry.bullets) -> GuideSearchRanking.BULLETS
            hits(entry.details) -> GuideSearchRanking.DETAILS
            else -> 0
        }
    }

    companion object {
        /** False for an empty or one-letter query, which shows a hint instead of results. */
        fun isSearchable(query: String): Boolean = GuideRuleNumberParser.words(query).sumOf { it.length } >= GuideSearchRanking.MIN_QUERY_CHARS

        private fun wordSet(text: String): Set<String> = GuideRuleNumberParser.words(text).toSet()

        /** A letter word matches as the start of a word, a number as a whole number: the same rule for a card field and a note. */
        private fun matches(
            words: Set<String>,
            term: String,
        ): Boolean = words.any { word -> if (term.first().isDigit()) GuideRuleNumberParser.numberMatches(term, word) else word.startsWith(term) }
    }
}
