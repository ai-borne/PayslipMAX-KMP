package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Search is the fast way to a card when the user knows a word or a rule number. These tests pin what makes a
 * result trustworthy: a number matches as a number, the best match comes first, and nothing the user typed is
 * kept. Cards are built from the synthetic bundle, so a rate edit in the real 402 cards never breaks them.
 */
class GuideSearchIndexTest {
    private val bundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON)).bundle
    private val template = bundle.cards.first { it.id == "RB-P1" } // homed in "pay-hra", whose rule line is empty

    private fun card(
        id: String,
        title: String = "Plain title $id",
        answer: String = "Plain answer.",
        key: List<String> = listOf("A plain key point"),
        details: String = "Plain details.",
        cite: String = "",
        nav: String = "pay-hra",
    ): GuideCard = template.copy(id = id, title = title, answer = answer, key = key, details = details, cite = cite, nav = nav)

    private fun indexOf(vararg cards: GuideCard) = GuideSearchIndex(bundle.copy(cards = cards.toList()))

    private fun GuideSearchIndex.ids(query: String) = search(query).map { it.card.id }

    @Test
    fun aRuleNumberDoesNotMatchALongerNumberThatStartsWithIt() {
        val index =
            indexOf(
                card("r177", cite = "Rule 177, TR 2014"),
                card("r1770", cite = "Rule 1770, TR 2014"),
                card("amount", details = "Pays 1770 rupees a day."),
            )

        assertEquals(listOf("r177"), index.ids("177"))
    }

    @Test
    fun aBareNumberFindsItsLetteredRulesAndALetteredQueryFindsOnlyThatRule() {
        val index = indexOf(card("a", cite = "Rule 177A, TR 2014"), card("b", cite = "Rule 177B(i)(g), TR 2014"), card("plain", cite = "Rule 177, TR 2014"))

        assertEquals(setOf("a", "b", "plain"), index.ids("177").toSet())
        assertEquals(listOf("b"), index.ids("177B"))
    }

    @Test
    fun ruleBeforeTheNumberChangesNothing() {
        val index = indexOf(card("r114", cite = "Rule 114, TR 2014"), card("other", cite = "Rule 115, TR 2014"))

        assertEquals(listOf("r114"), index.ids("114"))
        assertEquals(listOf("r114"), index.ids("Rule 114"))
        assertEquals(listOf("r114"), index.ids("RULES  114."))
    }

    @Test
    fun aCasesRuleLineGivesItsCardsTheRuleEvenWithoutACite() {
        // "ltc-home" is "Rule 177A" in the synthetic bundle; this card has no cite of its own.
        val index = indexOf(card("noCite", cite = "", nav = "ltc-home"), card("elsewhere", cite = ""))

        assertEquals(listOf("noCite"), index.ids("177a"))
    }

    @Test
    fun caseAndPunctuationAreIgnored() {
        val index = indexOf(card("ltc", title = "Family's LTC journey: warrant or fare refund?"))

        assertEquals(listOf("ltc"), index.ids("FAMILYS ltc"))
        assertEquals(listOf("ltc"), index.ids("family's, LTC!"))
    }

    @Test
    fun aLetterWordMatchesTheStartOfAWordSoTypingFindsAsYouGo() {
        val index = indexOf(card("pa", title = "Parachute Allowance: who qualifies?"))

        assertEquals(listOf("pa"), index.ids("allow"))
        assertTrue(index.ids("lowance").isEmpty(), "the middle of a word is not a match")
    }

    @Test
    fun anEmptyOrOneLetterQueryFindsNothingAndIsNotSearchable() {
        val index = indexOf(card("a", title = "A card"))

        for (query in listOf("", " ", "a", " a ", "?!", "'")) {
            assertTrue(index.search(query).isEmpty(), "'$query' must not search")
            assertFalse(GuideSearchIndex.isSearchable(query), "'$query'")
        }
        assertTrue(GuideSearchIndex.isSearchable("85"))
        assertTrue(GuideSearchIndex.isSearchable("ta"))
    }

    @Test
    fun aTitleMatchOutranksADetailsMatch() {
        val index = indexOf(card("inDetails", details = "Mentions the lumpsum here."), card("inTitle", title = "Lumpsum for linen"))

        assertEquals(listOf("inTitle", "inDetails"), index.ids("lumpsum"))
        assertTrue(GuideSearchRanking.TITLE > GuideSearchRanking.ANSWER)
        assertTrue(GuideSearchRanking.ANSWER > GuideSearchRanking.BULLETS)
        assertTrue(GuideSearchRanking.BULLETS > GuideSearchRanking.DETAILS)
    }

    @Test
    fun aRuleNumberOutranksAWordMatchOfTheSameNumber() {
        val index = indexOf(card("inTitle", title = "Fixed 114 days"), card("byRule", cite = "Rule 114, TR 2014"))

        assertEquals(listOf("byRule", "inTitle"), index.ids("114"))
    }

    @Test
    fun equalScoresKeepTheBundleOrder() {
        val index = indexOf(card("first", title = "Linen A"), card("second", title = "Linen B"), card("third", title = "Linen C"))

        assertEquals(listOf("first", "second", "third"), index.ids("linen"))
    }

    @Test
    fun everyWordOfTheQueryMustMatchSomewhereInTheCard() {
        val index = indexOf(card("both", title = "Linen lumpsum"), card("one", title = "Linen only"))

        assertEquals(listOf("both"), index.ids("linen lumpsum"))
        assertTrue(index.ids("linen zebra").isEmpty())
    }

    @Test
    fun placeholderBulletsAreNotSearchedBecauseTheyAreNeverShown() {
        val index = indexOf(card("personal", key = listOf("Visible bullet", "Level {level} = Rs {zzfig}/day")))

        assertEquals(listOf("personal"), index.ids("visible"))
        assertTrue(index.ids("zzfig").isEmpty())
    }

    @Test
    fun theSyntheticBundleSearchesEndToEnd() {
        // Only the two cards homed in "ltc-home" (rule line "Rule 177A") carry 177A; every card cites Rule 114.
        assertEquals(setOf("RB-T9", "RB-T10"), GuideSearchIndex(bundle).ids("177a").toSet())
        assertEquals(bundle.cards.size, GuideSearchIndex(bundle).search("rule 114").size)
    }
}
