package com.payslipmax.pdfparser.guide.domain

/**
 * The words of the user's notes, per card, for search. Built from decrypted note text that the app already holds in memory
 * and handed to [GuideSearchIndex.search] with each query: the index never fetches, keeps or saves it, and neither does
 * this class, which has no storage, no logging and a [toString] that prints a count. A blank note has no words and is
 * left out. Only an unlocked user's notes are ever put in one (the caller decides; the index also ignores it in a preview).
 */
class GuideSearchNotes private constructor(private val wordsByCard: Map<String, Set<String>>) {
    fun words(cardId: String): Set<String> = wordsByCard[cardId].orEmpty()

    // The same rule as GuideNote: a string template or a crash key must never be able to print what the user wrote.
    override fun toString(): String = "GuideSearchNotes(${wordsByCard.size})"

    companion object {
        val None = GuideSearchNotes(emptyMap())

        fun of(textByCard: Map<String, String>): GuideSearchNotes =
            GuideSearchNotes(
                textByCard
                    .mapValues { (_, text) -> GuideRuleNumberParser.words(text).toSet() }
                    .filterValues { it.isNotEmpty() },
            )
    }
}
