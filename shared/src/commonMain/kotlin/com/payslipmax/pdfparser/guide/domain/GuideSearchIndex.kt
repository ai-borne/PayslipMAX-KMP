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
)

/**
 * Search over a loaded bundle, built once per load (the bundle never changes while the app runs). A query is its
 * words, and every word must match somewhere in the card: a letter word as the start of a word ("allow" finds
 * "allowance"), a number as a whole number (see [GuideRuleNumberParser.numberMatches]). A leading "Rule" or "Rules"
 * is dropped, so "Rule 114" and "114" ask the same. Case and punctuation never matter. The query is read here and
 * nowhere else: nothing in this class stores, saves or logs it.
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
            cards.map { card ->
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
    ): List<GuideSearchHit> {
        val terms = terms(query)
        if (terms.isEmpty()) return emptyList()
        return entries
            .mapNotNull { entry -> score(entry, terms, scope)?.let { GuideSearchHit(entry.card, it) } }
            .sortedByDescending { it.score }
    }

    /** The query words that must all match, with a leading "Rule" or "Rules" dropped. Empty when too short. */
    private fun terms(query: String): List<String> {
        if (!isSearchable(query)) return emptyList()
        val words = GuideRuleNumberParser.words(query)
        val withoutRule = if (words.size > 1 && words.first() in GuideRuleNumberParser.RULE_KEYWORDS) words.drop(1) else words
        return withoutRule.distinct()
    }

    /** The card's score, or null when any query word matches nowhere. */
    private fun score(
        entry: Entry,
        terms: List<String>,
        scope: GuideSearchScope,
    ): Int? {
        var total = 0
        for (term in terms) {
            val best = bestWeight(entry, term, scope)
            if (best == 0) return null
            total += best
        }
        return total
    }

    private fun bestWeight(
        entry: Entry,
        term: String,
        scope: GuideSearchScope,
    ): Int {
        val isNumber = term.first().isDigit()

        fun hits(words: Set<String>) = words.any { word -> if (isNumber) GuideRuleNumberParser.numberMatches(term, word) else word.startsWith(term) }
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
    }
}
