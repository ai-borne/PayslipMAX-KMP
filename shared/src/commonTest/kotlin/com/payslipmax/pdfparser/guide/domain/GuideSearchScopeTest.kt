package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The free preview (owner decision 2026-10-07) lets everyone search titles and rule numbers. A free user must not be
 * able to learn what the locked key points or details say by searching for a word in them, because the result
 * list would answer "does this card mention X" for text that is paywalled. These tests fail if the preview scope
 * ever looks at a hidden field; the one-line answer is shown free, but it is not searched either (titles only).
 */
class GuideSearchScopeTest {
    private val bundle = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle
    private val template = bundle.cards.first { it.id == "RB-P1" }

    private fun card(
        id: String,
        title: String = "Plain title $id",
        answer: String = "Plain answer.",
        key: List<String> = listOf("A plain key point"),
        attach: List<String> = emptyList(),
        watch: List<String> = emptyList(),
        details: String = "Plain details.",
        cite: String = "",
    ): GuideCard = template.copy(id = id, title = title, answer = answer, key = key, attach = attach, watch = watch, details = details, cite = cite)

    private fun indexOf(vararg cards: GuideCard) = GuideSearchIndex(bundle.copy(cards = cards.toList()))

    private fun GuideSearchIndex.ids(
        query: String,
        scope: GuideSearchScope,
    ) = search(query, scope).map { it.card.id }

    @Test
    fun previewScopeFindsTitlesButNeverKeyPointsAttachWatchOutAnswersOrDetails() {
        val index =
            indexOf(
                card("title", title = "Quokka allowance"),
                card("answer", answer = "Quokka is paid."),
                card("key", key = listOf("Quokka rate applies")),
                card("attach", attach = listOf("Quokka certificate")),
                card("watch", watch = listOf("Quokka trap")),
                card("details", details = "Quokka details."),
            )

        assertEquals(listOf("title"), index.ids("quokka", GuideSearchScope.PREVIEW))
        assertEquals(
            setOf("title", "answer", "key", "attach", "watch", "details"),
            index.ids("quokka", GuideSearchScope.FULL).toSet(),
            "Premium keeps searching every field",
        )
    }

    @Test
    fun previewScopeStillFindsRuleNumbersFromTheCiteAndTheCasesRuleLine() {
        val index = indexOf(card("cited", cite = "Rule 177B, TR 2014"), card("plain"))

        assertEquals(listOf("cited"), index.ids("177b", GuideSearchScope.PREVIEW))
        assertEquals(listOf("cited"), index.ids("Rule 177B", GuideSearchScope.PREVIEW))
    }

    @Test
    fun aNumberInHiddenTextDoesNotMatchInPreviewButAWordInTheTitleDoes() {
        val index = indexOf(card("hidden", details = "Pays 1770 rupees a day."), card("shown", title = "Fixed 1770 days"))

        assertEquals(listOf("shown"), index.ids("1770", GuideSearchScope.PREVIEW))
    }

    @Test
    fun everyWordMustStillMatchInPreview() {
        val index = indexOf(card("a", title = "Linen lumpsum", details = "Zebra"))

        assertEquals(listOf("a"), index.ids("linen lumpsum", GuideSearchScope.PREVIEW))
        assertTrue(index.ids("linen zebra", GuideSearchScope.PREVIEW).isEmpty(), "zebra is only in the hidden details")
    }

    @Test
    fun fullIsTheDefaultSoExistingCallersKeepTheirBehaviour() {
        val index = indexOf(card("details", details = "Quokka details."))

        assertEquals(listOf("details"), index.search("quokka").map { it.card.id })
    }

    @Test
    fun aTitleHitRanksTheSameInBothScopes() {
        val index = indexOf(card("one", title = "Linen A", details = "Linen"), card("two", title = "Linen B"))

        assertEquals(
            index.search("linen", GuideSearchScope.FULL).map { it.card.id to it.score },
            index.search("linen", GuideSearchScope.PREVIEW).map { it.card.id to it.score },
        )
    }
}
