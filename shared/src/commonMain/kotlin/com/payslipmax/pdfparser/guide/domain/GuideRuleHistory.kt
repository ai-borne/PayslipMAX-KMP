package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideCard

/**
 * The chain of dated rules behind a card. A replaced card stays in the data (arrears are worked out under the old rule) and
 * names its successor in `replaced_by`. Every walk keeps a visited set: `compile.py` ships no loop, but a damaged or
 * hand-edited bundle must end the walk, not hang the screen. Cards, not ids, come back so a caller needs no second lookup.
 */
class GuideRuleHistory(cards: List<GuideCard>) {
    private val byId: Map<String, GuideCard> = cards.associateBy { it.id }
    private val replacedCardsBySuccessor: Map<String, List<GuideCard>> = cards.filter { it.isReplaced }.groupBy { it.replacedBy }

    fun isReplaced(cardId: String): Boolean = byId[cardId]?.isReplaced == true

    /** The date (YYYY-MM-DD) a replaced card stopped applying; null for a current or unknown card. */
    fun replacedOn(cardId: String): String? = byId[cardId]?.takeIf { it.isReplaced }?.until?.takeIf { it.isNotBlank() }

    /**
     * The rule in force: [cardId] itself when it is current, else the end of its `replaced_by` chain. Null for an unknown
     * card, a chain that loops or one that names a card the bundle does not hold.
     */
    fun currentRule(cardId: String): GuideCard? {
        if (loopsBack(cardId)) return null
        var card = byId[cardId]
        while (card != null && card.isReplaced) card = byId[card.replacedBy]
        return card
    }

    /** True when following `replaced_by` from [cardId] comes back to a card already passed: no rule in force, and no end. */
    fun loopsBack(cardId: String): Boolean {
        val visited = mutableSetOf<String>()
        var card = byId[cardId]
        while (card != null && card.isReplaced) {
            if (!visited.add(card.id)) return true
            card = byId[card.replacedBy]
        }
        return false
    }

    /** The rules that came before [cardId], nearest first. Empty for the oldest rule; each card is listed once. */
    fun earlierRules(cardId: String): List<GuideCard> {
        val visited = mutableSetOf(cardId)
        val found = mutableListOf<GuideCard>()
        var frontier = listOf(cardId)
        while (frontier.isNotEmpty()) {
            val next = mutableListOf<String>()
            for (id in frontier) {
                for (earlier in replacedCardsBySuccessor[id].orEmpty()) {
                    if (visited.add(earlier.id)) {
                        found += earlier
                        next += earlier.id
                    }
                }
            }
            frontier = next
        }
        return found
    }

    /** The date (YYYY-MM-DD) before which [cardId]'s nearest earlier rule applied: that rule's end date. Null when none. */
    fun earlierBefore(cardId: String): String? = earlierRules(cardId).firstOrNull()?.until?.takeIf { it.isNotBlank() }
}
