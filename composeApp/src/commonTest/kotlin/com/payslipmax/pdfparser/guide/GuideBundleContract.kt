package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.di.GUIDE_BUNDLE_PATH
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.domain.CardTemplate
import com.payslipmax.pdfparser.guide.domain.GuideLinkMap
import com.payslipmax.pdfparser.guide.domain.GuidePins
import com.payslipmax.pdfparser.guide.domain.GuideRuleNumberParser
import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import com.payslipmax.pdfparser.guide.domain.GuideSearchScope
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.subscription.LaunchFlags
import com.payslipmax.pdfparser.ui.screens.guide.cardContent
import com.payslipmax.pdfparser.ui.screens.guide.feedContent
import com.payslipmax.pdfparser.ui.screens.guide.shareNote
import com.payslipmax.pdfparser.ui.screens.guide.toContent
import com.payslipmax.pdfparser.ui.screens.guide.toReady
import com.payslipmax.pdfparser.ui.theme.GuideStrings
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
        val cards = bundle.cards.map { assertNotNull(index.cardContent(it.id, unlocked = true, nowMillis = 0L)) }
        val shown = cards.flatMap { card -> card.full!!.let { listOf(card.title, card.answer, it.cite, it.details) + it.body.key + it.body.attach + it.body.watch } }
        assertTrue(shown.none(CardTemplate::hasPlaceholder), "a placeholder would be shown raw")
    }

    /**
     * The E5 trust chips and Premium preview over all 402 cards: each chip count matches the dataset's own counts, a
     * locked card holds only the free half, and the preview search never reads a locked field (a word that is in no
     * title or rule line finds nothing, however many key points and details carry it).
     */
    fun assertTrustAndPreviewMatchDataset(bundle: GuideBundle) {
        val index = bundle.toReady().index
        val locked = bundle.cards.map { assertNotNull(index.cardContent(it.id, unlocked = false, nowMillis = 0L)) }
        assertTrue(locked.all { it.full == null }, "a locked card holds no key points, cite or details")
        assertEquals(36, locked.count { it.trust.unverified })
        assertEquals(31, locked.count { it.trust.noOfficialSource })
        assertEquals(bundle.cards.count { "RATES" in it.chips }, locked.count { it.trust.ratesAsOf == bundle.ratesAsOf })
        assertEquals(bundle.cards.count { "AMENDED" in it.chips }, locked.count { it.trust.amended })
        assertTrue(bundle.cards.any { "RATES" in it.chips } && bundle.cards.any { "AMENDED" in it.chips }, "both chips are in use")
        val searchIndex = GuideSearchIndex(bundle)
        for (card in bundle.cards) {
            val body = CardTemplate.body(card)
            val ownTitle = GuideRuleNumberParser.words(card.title)
            val hidden = GuideRuleNumberParser.words((body.key + body.attach + body.watch).joinToString(" ")).filter { it.length > 3 && it.all(Char::isLetter) }
            // A word that starts no word of this card's own title: only its locked text can make the card match it.
            val word = hidden.firstOrNull { w -> ownTitle.none { it.startsWith(w) } } ?: continue
            assertTrue(searchIndex.search(word, GuideSearchScope.PREVIEW).none { it.card.id == card.id }, "preview search leaked a hidden word of ${card.id}")
            assertTrue(searchIndex.search(word, GuideSearchScope.FULL).any { it.card.id == card.id }, "full search finds ${card.id}")
        }
    }

    /**
     * Owner rule (2026-10-07): charging for key points, cite and details starts only when no card carrying the Rates chip is
     * still an "Unverified point" (RP-088 HBA 8.5% is one). With the paywall off this holds nothing back; the day someone flips
     * [LaunchFlags.GUIDE_PAYWALL_ENABLED] while such a card remains, this fails and names the cards.
     */
    fun assertPaywallOnlyWhenNoUnverifiedRateCard(bundle: GuideBundle) {
        if (!LaunchFlags.GUIDE_PAYWALL_ENABLED) return
        val blocking = bundle.cards.filter { "RATES" in it.chips && it.unverified }.map { it.id }
        assertTrue(blocking.isEmpty(), "the paywall is on while rate cards are still unverified: $blocking")
    }

    /**
     * E7: every card a Pay Audit finding can open is in the shipped bundle, so no link can lead to a card that is missing. The owner
     * approved the ids in the E7 mapping table; a dataset change that drops one fails here, not on a user's phone.
     */
    fun assertPayAuditLinksPointAtRealCards(bundle: GuideBundle) {
        val ids = bundle.cards.map { it.id }.toSet()
        assertTrue(GuideLinkMap.cardIds.isNotEmpty())
        assertEquals(emptySet(), GuideLinkMap.cardIds - ids, "Pay Audit links to cards the bundle does not hold")
        val index = bundle.toReady().index
        for (id in GuideLinkMap.cardIds) assertNotNull(index.cardContent(id, unlocked = true, nowMillis = 0L), id)
    }

    /**
     * E8: on all 402 real cards a pin is accepted (every id is a plain card id, else the pin would silently do nothing), the claim note
     * builds, holds the title and exactly the cite, carries the warning for the 36 unverified cards and for no other, and never leaks a raw
     * placeholder or the card's details block.
     */
    fun assertPinsAndShareNotesMatchDataset(bundle: GuideBundle) {
        val index = bundle.toReady().index
        val warning = GuideStrings.shareUnverified
        var warned = 0
        for (card in bundle.cards) {
            assertTrue(GuidePins.Empty.toggle(card.id).isPinned(card.id), "${card.id} cannot be pinned")
            val note = assertNotNull(index.cardContent(card.id, unlocked = true, nowMillis = 0L)?.shareNote(), card.id)
            assertTrue(note.contains(card.title), card.id)
            assertEquals(card.cite.isNotBlank(), note.contains("${GuideStrings.shareAuthority} ${card.cite}"), card.id)
            assertEquals(card.unverified, note.contains(warning), card.id)
            assertTrue(!note.contains('{'), card.id)
            if (card.details.isNotBlank()) assertTrue(!note.contains(card.details), card.id)
            if (card.unverified) warned++
        }
        assertEquals(36, warned)
    }

    /**
     * The E4 search over all 402 cards. Every rule number a cite names (typed as "Rule N") finds its card, every card is found by its own
     * title, and a fixed set of real queries all answer: "177" is the LTC family and never "1770", and "Rule 114"
     * asks what "114" asks. This is a correctness workload (about 700 searches), not a timing one: see [realisticQueries].
     */
    fun assertSearchMatchesDataset(bundle: GuideBundle) {
        val index = GuideSearchIndex(bundle)
        for (card in bundle.cards) {
            for (rule in GuideRuleNumberParser.ruleNumbers(card.cite)) {
                // "Rule 2" rather than "2": a single digit is a one-character query, which is not searched.
                assertTrue(index.search("Rule $rule").any { it.card.id == card.id }, "rule $rule of ${card.id} is not found")
            }
            assertTrue(index.search(card.title).any { it.card.id == card.id }, "${card.id} is not found by its own title")
        }
        val ltc = index.search("177").map { it.card.id }
        assertTrue(ltc.isNotEmpty(), "177 finds the LTC rules")
        assertTrue(index.search("177B").map { it.card.id }.let { exact -> exact.isNotEmpty() && ltc.containsAll(exact) })
        // The middle of a cited range ("Rules 88 to 91", "Rules 265 to 277") is a rule too.
        assertTrue(index.search("Rule 89").any { it.card.id == "RB-SS-P034" })
        assertTrue(index.search("Rule 270").any { it.card.id == "RB-C18-05" })
        assertEquals(index.search("114").map { it.card.id }, index.search("Rule 114").map { it.card.id })
        for (query in listOf("85A", "allowance", "LTC", "HRA", "transport", "family", "leave", "claim")) {
            assertTrue(index.search(query).isNotEmpty(), "'$query' finds something")
        }
    }

    /**
     * What a person types, for the iOS timing test: whole queries, then two phrases typed one letter at a time (the
     * screen searches on every keystroke, so each prefix is a search).
     */
    val realisticQueries: List<String> =
        listOf("177", "177B", "Rule 114", "85A", "hra", "ltc family", "food", "leave", "claim") +
            typedLetterByLetter("transport allowance") + typedLetterByLetter("family ltc")

    private fun typedLetterByLetter(phrase: String): List<String> = (1..phrase.length).map { phrase.take(it) }
}
