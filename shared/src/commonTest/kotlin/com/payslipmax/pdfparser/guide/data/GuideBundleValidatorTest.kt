package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Every rule here protects a screen from a broken bundle: a dangling id is a dead tile, an orphan card is
 * unreachable, a text past the compiler's limits overflows the card layout, and a raw `{` outside a personal
 * card would show a template placeholder to the user.
 */
class GuideBundleValidatorTest {
    private val valid: GuideBundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON)).bundle

    private fun problems(bundle: GuideBundle) = GuideBundleValidator.validate(bundle)

    private fun assertProblem(
        bundle: GuideBundle,
        fragment: String,
    ) {
        val found = problems(bundle)
        assertTrue(found.any { fragment in it }, "expected a problem containing '$fragment', got $found")
    }

    private fun GuideBundle.editCard(
        id: String,
        edit: (GuideCard) -> GuideCard,
    ) = copy(cards = cards.map { if (it.id == id) edit(it) else it })

    private fun GuideBundle.editCase(
        id: String,
        cards: List<String>? = null,
        also: List<String>? = null,
    ) = copy(
        nav =
            nav.map { area ->
                area.copy(cases = area.cases.map { if (it.id == id) it.copy(cards = cards ?: it.cards, also = also ?: it.also) else it })
            },
    )

    @Test
    fun theSyntheticBundleIsValid() {
        assertEquals(emptyList(), problems(valid))
    }

    @Test
    fun aCaseListingAMissingCardIsDangling() {
        assertProblem(valid.editCase("pay-hra", cards = listOf("RB-P1", "RB-P2", "RB-P3", "RB-NOPE")), "unknown card RB-NOPE")
        assertProblem(valid.editCase("pay-hra", also = listOf("RB-NOPE")), "unknown also-card RB-NOPE")
    }

    @Test
    fun aCardNoCaseListsIsAnOrphan() {
        assertProblem(valid.editCase("pay-hra", cards = listOf("RB-P1", "RB-P2")), "RB-P3 has no home")
    }

    @Test
    fun aCardListedByTwoCasesIsHomedTwice() {
        assertProblem(valid.editCase("pay-hra", cards = listOf("RB-P1", "RB-P2", "RB-P3", "RB-T9")), "RB-T9 homed twice")
    }

    @Test
    fun aCardsNavMustNameTheCaseThatListsIt() {
        assertProblem(valid.editCard("RB-P1") { it.copy(nav = "ltc-home") }, "RB-P1 nav")
    }

    @Test
    fun duplicateIdsAreRejected() {
        assertProblem(valid.copy(cards = valid.cards + valid.cards.last()), "duplicate card id RB-P3")
        assertProblem(valid.copy(nav = valid.nav + valid.nav.last()), "duplicate area id pay")
    }

    @Test
    fun aFacetMissingFromTheLabelsIsRejected() {
        assertProblem(valid.editCard("RB-P1") { it.copy(facet = "Z") }, "RB-P1 facet")
    }

    @Test
    fun requiredTextMustBePresent() {
        assertProblem(valid.editCard("RB-P1") { it.copy(title = " ") }, "RB-P1 title")
        assertProblem(valid.editCard("RB-P1") { it.copy(answer = "") }, "RB-P1 answer")
        assertProblem(valid.editCard("RB-P1") { it.copy(key = emptyList()) }, "RB-P1 key")
    }

    @Test
    fun theCompilersLimitsAreRechecked() {
        val long = List(30) { "word" }.joinToString(" ")
        assertProblem(valid.editCard("RB-P1") { it.copy(title = long) }, "RB-P1 title")
        assertProblem(valid.editCard("RB-P1") { it.copy(answer = long) }, "RB-P1 answer")
        assertProblem(valid.editCard("RB-P1") { it.copy(watch = listOf("a", "b", "c", "d")) }, "RB-P1 watch")
        assertProblem(valid.editCard("RB-P1") { it.copy(key = listOf(long)) }, "RB-P1 key")
        assertProblem(valid.editCard("RB-P1") { it.copy(details = List(121) { "w" }.joinToString(" ")) }, "RB-P1 details")
        val nearLimit = List(12) { "w" }.joinToString(" ")
        assertProblem(
            valid.editCard("RB-P1") { it.copy(key = List(3) { nearLimit }, attach = List(3) { nearLimit }, watch = List(3) { nearLimit }) },
            "RB-P1 visible",
        )
    }

    @Test
    fun aRawPlaceholderAnywhereOnAnyCardIsRejected() {
        // Cards carry no placeholders (the "your figure" line comes from the bundle's figures), so a braced name in any
        // text of any card, personal or not, would be shown raw.
        for (id in listOf("RB-P1", SyntheticGuideBundle.PERSONAL_CARD)) {
            for (edit in listOf<(GuideCard) -> GuideCard>(
                { it.copy(title = "Rs {food_rate}?") },
                { it.copy(answer = "Rs {food_rate}.") },
                { it.copy(key = listOf("Rs {food_rate}")) },
                { it.copy(attach = listOf("Rs {food_rate}")) },
                { it.copy(watch = listOf("Rs {food_rate}")) },
                { it.copy(cite = "Rule {n}") },
                { it.copy(details = "Rs {food_rate}") },
            )) {
                assertProblem(valid.editCard(id, edit), "$id has a placeholder")
            }
        }
    }

    @Test
    fun theRatesMonthMustBeAYearAndMonth() {
        assertProblem(valid.copy(ratesAsOf = "Jan 2026"), "rates_as_of")
        assertProblem(valid.copy(ratesAsOf = "2026-13"), "rates_as_of")
    }

    @Test
    fun wordsAreCountedTheWayTheCompilerCountsThem() {
        // compile.py counts runs of [A-Za-z0-9₹%.,/'()&+-]; "=", "{", "}" and "_" separate words.
        assertEquals(6, countGuideWords("Level {level} = Rs {food_rate}/day"))
        assertEquals(4, countGuideWords("Rs 1,200 (₹) per-day"))
        assertEquals(0, countGuideWords("  = : "))
    }

    @Test
    fun aFigureMustBelongToAPersonalCardThatExists() {
        val figures = SyntheticGuideFigures.figures
        val food = figures.figures.getValue("food_rate")
        val onPersonalCard = figures.copy(figures = mapOf("food_rate" to food.copy(card = SyntheticGuideBundle.PERSONAL_CARD)))
        assertEquals(emptyList(), problems(valid.copy(figures = onPersonalCard)))

        val unknownCard = figures.copy(figures = mapOf("food_rate" to food.copy(card = "RB-NOPE")))
        assertProblem(valid.copy(figures = unknownCard), "figure food_rate: unknown card RB-NOPE")

        val noSpec = figures.copy(figures = mapOf("food_rate" to food.copy(card = SyntheticGuideBundle.NO_CITE_CARD)))
        assertProblem(valid.copy(figures = noSpec), "figure food_rate: card ${SyntheticGuideBundle.NO_CITE_CARD} has no personal spec")
    }

    @Test
    fun aBundleWithoutFiguresIsStillValid() {
        assertEquals(null, valid.figures)
        assertEquals(emptyList(), problems(valid))
    }
}
