package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
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
    fun aPlaceholderBraceOutsideAPersonalCardIsRejected() {
        assertProblem(valid.editCard("RB-P1") { it.copy(details = "Rs {hra} a month") }, "RB-P1 has a placeholder")
        // The personal card legitimately carries {level} and {food_rate}.
        assertEquals(emptyList(), problems(valid.editCard(SyntheticGuideBundle.PERSONAL_CARD) { it.copy(details = "Rs {food_rate}") }))
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
}
