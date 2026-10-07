package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.di.GUIDE_BUNDLE_PATH
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.domain.CardTemplate
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.ui.screens.guide.cardContent
import com.payslipmax.pdfparser.ui.screens.guide.feedContent
import com.payslipmax.pdfparser.ui.screens.guide.toContent
import com.payslipmax.pdfparser.ui.screens.guide.toReady
import pdfparser.composeapp.generated.resources.Res
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract between the shipped bundle and the app, asserted on each platform after reading the real file
 * through the same compose resource path production uses (`GuideBundleAndroidContractTest`,
 * `GuideLoaderIosPerfTest`). Only these checks read the real 402 cards; logic tests use
 * `SyntheticGuideBundle`. When the dataset changes on purpose, update the counts here and in
 * `docs/Plan/rule_cards/tools/test_bundle.py` together.
 */
object GuideBundleContract {
    suspend fun readShippedBundleText(): String = Res.readBytes(GUIDE_BUNDLE_PATH).decodeToString()

    fun parseShippedBundle(text: String): GuideBundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle

    fun assertMatchesCompiledDataset(bundle: GuideBundle) {
        assertEquals(GuideBundleParser.SUPPORTED_MAJOR, bundle.version)
        assertEquals(402, bundle.cards.size)
        assertEquals(220, bundle.cards.count { it.domain == "travel" })
        assertEquals(182, bundle.cards.count { it.domain == "pay" })
        assertEquals(9, bundle.nav.size)
        assertEquals(44, bundle.nav.sumOf { it.cases.size })
        assertEquals(setOf("Q", "H", "C", "L"), bundle.facets.keys)
        // Owner, 2026-10-06: all 36 open-point cards ship, flagged "Unverified point".
        assertEquals(36, bundle.cards.count { it.unverified })
        // Owner, 2026-10-07: "No official source" is exactly the 31 cards with no cite.
        assertEquals(31, bundle.cards.count { it.hasNoOfficialSource })
        // Owner, 2026-10-07: rates current to the DA 60% step.
        assertEquals("2026-01", bundle.ratesAsOf)
        // RP-088 HBA ships with the chip until the owner confirms the rate from a primary letter.
        assertEquals(true, bundle.cards.firstOrNull { it.topic == "RP-088" }?.unverified)
    }

    /** The E2 tiles: 9 area tiles holding the 44 cases, and every card counted once in the case it is homed in. */
    fun assertTilesMatchDataset(bundle: GuideBundle) {
        val ready = bundle.toReady()
        assertEquals(9, ready.areas.size)
        assertEquals(44, ready.areas.sumOf { it.caseCount })
        val cases = bundle.nav.flatMap { it.toContent().cases }
        assertEquals(bundle.cards.size, bundle.nav.sumOf { area -> area.cases.sumOf { it.cards.size } })
        assertEquals(bundle.nav.sumOf { area -> area.cases.sumOf { it.cards.size + it.also.size } }, cases.sumOf { it.cardCount })
        assertTrue(cases.all { it.cardCount > 0 }, "no empty case tile")
    }

    /**
     * The E3 feeds and cards over all 402 cards: every case builds a feed listing all its cards, facet counts add up,
     * the six "also relevant here" links in ltc-rules name their real home, and no card shows a raw placeholder.
     */
    fun assertFeedsAndCardsMatchDataset(bundle: GuideBundle) {
        val index = bundle.toReady().index
        for (case in bundle.nav.flatMap { it.cases }) {
            val feed = assertNotNull(index.feedContent(case.id, facet = null), case.id)
            assertEquals(case.cards.size + case.also.size, feed.rows.size, case.id)
            if (feed.facets.isNotEmpty()) assertEquals(feed.totalCount, feed.facets.sumOf { it.count }, case.id)
        }
        val alsoRows = index.feedContent("ltc-rules", facet = null)!!.rows.filter { it.alsoHomeTitle != null }
        assertEquals(6, alsoRows.size)
        assertTrue(alsoRows.all { row -> index.card(row.cardId)!!.nav != "ltc-rules" && row.alsoHomeTitle!!.isNotBlank() })
        val cards = bundle.cards.map { assertNotNull(index.cardContent(it.id)) }
        val shown = cards.flatMap { listOf(it.title, it.answer, it.cite, it.details) + it.body.key + it.body.attach + it.body.watch }
        assertTrue(shown.none(CardTemplate::hasPlaceholder), "a placeholder would be shown raw")
        // The food-rate and CTG cards carry the two placeholder bullets that phase E6 fills.
        assertEquals(setOf("RB-SS-T181", "RB-SS-T254"), cards.filter { it.body.figureTemplates.isNotEmpty() }.map { it.id }.toSet())
    }
}
