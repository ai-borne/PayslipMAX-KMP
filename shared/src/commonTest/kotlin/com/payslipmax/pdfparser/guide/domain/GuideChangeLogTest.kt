package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideChange
import com.payslipmax.pdfparser.guide.model.GuideChangeItem
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The "Updated" chip and the "What's new" list answer one question for the reader: what changed since I last looked.
 * Only the newest entry marks a card as updated, so a chip never lingers across releases.
 */
class GuideChangeLogTest {
    private val log = GuideChangeLog(SyntheticGuideRuleChange.bundle().changes)

    @Test
    fun anEmptyLogHasNoLatestEntryAndMarksNothing() {
        val empty = GuideChangeLog(emptyList())

        assertNull(empty.latest)
        assertTrue(empty.entries.isEmpty())
        assertFalse(empty.changedInLatest("RB-T1"))
    }

    @Test
    fun theLatestEntryIsTheNewestDateWhateverOrderTheBundleListsThem() {
        val shuffled = SyntheticGuideRuleChange.bundle().changes.reversed()

        val reordered = GuideChangeLog(shuffled)

        assertEquals(SyntheticGuideRuleChange.LATEST, reordered.latest?.date)
        assertEquals(listOf(SyntheticGuideRuleChange.LATEST, SyntheticGuideRuleChange.OLDER_ENTRY), reordered.entries.map { it.date })
    }

    @Test
    fun aCardNamedInTheLatestEntryIsUpdatedAndOneNamedOnlyInAnOlderEntryIsNot() {
        assertTrue(log.changedInLatest(SyntheticGuideRuleChange.NEW_CARD))
        assertTrue(log.changedInLatest(SyntheticGuideRuleChange.CLARIFIED_CARD))
        assertFalse(log.changedInLatest(SyntheticGuideRuleChange.OLDER_ENTRY_CARD), "an older entry's card is no longer new")
        assertFalse(log.changedInLatest("RB-NOPE"))
    }

    @Test
    fun latestEntriesAreCappedAndNewestFirst() {
        val many = (1..5).map { GuideChange("2026-0$it-01", listOf(GuideChangeItem("entry $it"))) }

        val limited = GuideChangeLog(many).latest(3)

        assertEquals(listOf("2026-05-01", "2026-04-01", "2026-03-01"), limited.map { it.date })
    }

    @Test
    fun anItemWithNoCardsIsFineAndMarksNothing() {
        val textOnly = GuideChangeLog(listOf(GuideChange("2026-11-15", listOf(GuideChangeItem("Wording tidied")))))

        assertEquals("2026-11-15", textOnly.latest?.date)
        assertFalse(textOnly.changedInLatest("RB-T1"))
    }
}
