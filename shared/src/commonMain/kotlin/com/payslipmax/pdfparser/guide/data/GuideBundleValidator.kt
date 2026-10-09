package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.domain.CardTemplate
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideCard
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Rechecks, on every load, the rules `compile.py` enforced when the bundle was built: ids resolve, every card
 * has exactly one home (a replaced card has none), text stays within the card layout's limits, and no card carries a template placeholder. Problems name card ids for tests and debugging only; they must never be logged or reported.
 * Plain string scans, no regex, so it stays linear on Kotlin/Native.
 */
object GuideBundleValidator {
    /** Reviewer notes (`open`) and internal provenance (`from`) stay in the repo; a bundle carrying them is rejected. */
    private val INTERNAL_CARD_FIELDS = setOf("from", "open")

    fun hasInternalFields(root: JsonObject): Boolean =
        (root["cards"] as? JsonArray).orEmpty().any { card -> (card as? JsonObject)?.keys?.any { it in INTERNAL_CARD_FIELDS } == true }

    fun validate(bundle: GuideBundle): List<String> {
        val problems = mutableListOf<String>()
        if (!isYearMonth(bundle.ratesAsOf)) problems += "rates_as_of '${bundle.ratesAsOf}' is not YYYY-MM"
        problems += duplicates("area", bundle.nav.map { it.id })
        problems += duplicates("case", bundle.nav.flatMap { area -> area.cases.map { it.id } })
        problems += duplicates("card", bundle.cards.map { it.id })
        checkHomes(bundle, problems)
        bundle.cards.forEach { checkCard(it, bundle, problems) }
        problems += GuideFigureValidator.validate(bundle)
        return problems
    }

    private fun checkHomes(
        bundle: GuideBundle,
        problems: MutableList<String>,
    ) {
        val cardsById = bundle.cards.associateBy { it.id }
        val homes = mutableMapOf<String, String>()
        for (case in bundle.nav.flatMap { it.cases }) {
            for (id in case.cards) {
                val card = cardsById[id]
                when {
                    card == null -> problems += "case ${case.id}: unknown card $id"
                    card.isReplaced -> problems += "card $id is replaced but homed in ${case.id}"
                    id in homes -> problems += "card $id homed twice (${homes[id]}, ${case.id})"
                    card.nav != case.id -> problems += "card $id nav '${card.nav}' but listed in ${case.id}"
                }
                homes.getOrPut(id) { case.id }
            }
            case.also.filter { it !in cardsById }.forEach { problems += "case ${case.id}: unknown also-card $it" }
            case.also.filter { cardsById[it]?.isReplaced == true }.forEach { problems += "card $it is replaced but homed in ${case.id}" }
        }
        bundle.cards.filter { it.id !in homes && !it.isReplaced }.forEach { problems += "card ${it.id} has no home" }
    }

    private fun checkCard(
        card: GuideCard,
        bundle: GuideBundle,
        problems: MutableList<String>,
    ) {
        val limits = bundle.limits
        val id = card.id
        if (card.isReplaced && (card.replacedBy == id || bundle.cards.none { it.id == card.replacedBy })) {
            problems += "card $id replaced_by ${card.replacedBy} is not another card"
        }
        if (card.facet !in bundle.facets) problems += "card $id facet '${card.facet}' has no label"
        if (card.title.isBlank() || countGuideWords(card.title) > limits.title) problems += "card $id title empty or over ${limits.title} words"
        if (card.answer.isBlank() || countGuideWords(card.answer) > limits.answer) problems += "card $id answer empty or over ${limits.answer} words"
        if (card.key.isEmpty()) problems += "card $id key has no bullets"
        for ((section, bullets) in listOf("key" to card.key, "attach" to card.attach, "watch" to card.watch)) {
            if (bullets.size > limits.bullets) problems += "card $id $section has ${bullets.size} bullets, max ${limits.bullets}"
            if (bullets.any { countGuideWords(it) > limits.bulletWords }) problems += "card $id $section bullet over ${limits.bulletWords} words"
        }
        val visible = (listOf(card.answer, card.cite) + card.key + card.attach + card.watch).sumOf(::countGuideWords)
        if (visible > limits.visible) problems += "card $id visible text $visible words, max ${limits.visible}"
        if (countGuideWords(card.details) > limits.details) problems += "card $id details over ${limits.details} words"
        // The "your figure" line comes from the bundle's figures, so no card text may carry a template placeholder.
        if (card.allText().any(CardTemplate::hasPlaceholder)) problems += "card $id has a placeholder"
    }

    private fun GuideCard.allText(): List<String> = listOf(title, answer, cite, details) + key + attach + watch

    private fun duplicates(
        kind: String,
        ids: List<String>,
    ): List<String> = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.map { "duplicate $kind id $it" }

    private fun isYearMonth(value: String): Boolean {
        if (value.length != 7 || value[4] != '-') return false
        val digits = value.removeRange(4, 5)
        if (!digits.all { it in '0'..'9' }) return false
        return value.substring(5).toInt() in 1..12
    }
}

/** Counts words exactly as `compile.py` does: runs of ASCII letters, digits and `₹%.,/'()&+-`. */
fun countGuideWords(text: String): Int {
    var count = 0
    var inWord = false
    for (ch in text) {
        val isWordChar = ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch in WORD_PUNCTUATION
        if (isWordChar && !inWord) count++
        inWord = isWordChar
    }
    return count
}

private const val WORD_PUNCTUATION = "₹%.,/'()&+-"
