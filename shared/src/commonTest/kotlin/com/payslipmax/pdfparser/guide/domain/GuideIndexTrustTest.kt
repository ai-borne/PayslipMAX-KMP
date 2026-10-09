package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * One place decides what a card's chips say, so a feed row, a search result, a pinned row and the card screen can never
 * disagree about "Updated" or "Replaced on". Without any change in the bundle (today's real bundle) nothing new is drawn.
 */
class GuideIndexTrustTest {
    private val changed = GuideIndex(SyntheticGuideRuleChange.bundle())
    private val plain = GuideIndex((GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle)

    private fun trust(id: String) = changed.trust(changed.card(id)!!)

    @Test
    fun aCardNamedInTheLatestEntryIsUpdated() {
        assertTrue(trust(SyntheticGuideRuleChange.NEW_CARD).updated)
        assertTrue(trust(SyntheticGuideRuleChange.CLARIFIED_CARD).updated)
        assertNull(trust(SyntheticGuideRuleChange.NEW_CARD).replacedUntil)
    }

    @Test
    fun aReplacedCardShowsItsEndDateAndNeverUpdatedEvenWhenTheLatestEntryNamesIt() {
        val old = trust(SyntheticGuideRuleChange.OLD_CARD)

        assertEquals(SyntheticGuideRuleChange.EFFECTIVE, old.replacedUntil)
        assertFalse(old.updated, "Replaced is the stronger message; one card never carries both")
    }

    @Test
    fun aCardNamedOnlyInAnOlderEntryOrNotAtAllHasNoChangeChip() {
        assertFalse(trust(SyntheticGuideRuleChange.OLDER_ENTRY_CARD).updated)
        assertFalse(trust("RB-T5").hasAny)
    }

    @Test
    fun theTrustChipsSurviveAlongsideTheChangeChips() {
        // RB-T1 is a RATES card; the change chips are added to the old chips, never instead of them.
        val rates = changed.trust(changed.card("RB-T1")!!)
        assertEquals(SyntheticGuideBundle.RATES_AS_OF, rates.ratesAsOf)
    }

    @Test
    fun aBundleWithNoChangesAndNoReplacedCardShowsNothingNew() {
        val allTrust = plain.bundle.cards.map { plain.trust(it) }

        assertTrue(allTrust.none { it.updated || it.replacedUntil != null })
        assertEquals(plain.bundle.cards.map { GuideTrust.of(it, plain.bundle.ratesAsOf) }, allTrust, "identical to the pre-M4 chips")
        assertNull(plain.changeLog.latest)
        assertFalse(plain.history.isReplaced("RB-T9"))
    }

    @Test
    fun hasAnyCountsTheNewChips() {
        val none = GuideTrust(null, amended = false, unverified = false, noOfficialSource = false)

        assertFalse(none.hasAny)
        assertTrue(none.copy(updated = true).hasAny)
        assertTrue(none.copy(replacedUntil = "2026-11-15").hasAny)
    }
}
