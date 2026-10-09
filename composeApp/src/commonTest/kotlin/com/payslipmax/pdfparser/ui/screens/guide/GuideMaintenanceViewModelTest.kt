package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the Guide shows about rule changes: a Home row only when the bundle has a change entry, the list of entries with
 * links to their cards, and on a card the way to the rule in force or to the rule it replaced. The real bundle has no
 * change yet, so every test here runs on the synthetic rule-change bundle; the plain bundle must look exactly as before.
 */
class GuideMaintenanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private fun changedModel() = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideRuleChange.bundle())), FakeCrashReporter(), dispatcher)

    private fun plainModel() = GuideViewModel(FakeGuideRepository(), FakeCrashReporter(), dispatcher)

    private suspend fun TestScope.ready(model: GuideViewModel): GuideViewModel {
        model.load()
        testScheduler.advanceUntilIdle()
        return model
    }

    @Test
    fun homeOffersWhatsNewOnlyWhenTheBundleHasAChangeEntry() =
        test {
            assertEquals(SyntheticGuideRuleChange.LATEST, ready(changedModel()).whatsNewDate())
            assertNull(ready(plainModel()).whatsNewDate(), "today's real bundle: Home looks exactly as before")
            assertNull(ready(plainModel()).whatsNew())
        }

    @Test
    fun theListHoldsEveryEntryNewestFirstWithItsTextAndCardLinks() =
        test {
            val list = assertNotNull(ready(changedModel()).whatsNew())

            assertEquals(listOf(SyntheticGuideRuleChange.LATEST, SyntheticGuideRuleChange.OLDER_ENTRY), list.entries.map { it.date })
            val latest = list.entries.first().lines
            assertEquals(listOf(SyntheticGuideRuleChange.LATEST_TEXT, SyntheticGuideRuleChange.CLARIFIED_TEXT), latest.map { it.text })
            assertEquals(listOf("RB-T11", "RB-T9"), latest.first().cards.map { it.cardId })
            assertEquals("Synthetic card RB-T11?", latest.first().cards.first().title)
        }

    @Test
    fun aChangeLineNamingACardTheBundleLacksKeepsItsTextAndDropsTheLink() =
        test {
            val bundle = SyntheticGuideRuleChange.bundle()
            val damaged = bundle.copy(changes = bundle.changes.map { c -> c.copy(items = c.items.map { it.copy(cards = it.cards + "RB-GONE") }) })
            val model = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(damaged)), FakeCrashReporter(), dispatcher)

            val line = assertNotNull(ready(model).whatsNew()).entries.first().lines.first()

            assertEquals(SyntheticGuideRuleChange.LATEST_TEXT, line.text)
            assertTrue(line.cards.none { it.cardId == "RB-GONE" })
        }

    @Test
    fun aReplacedCardPointsAtTheRuleInForceAndKeepsItsOwnContent() =
        test {
            val card = assertNotNull(ready(changedModel()).card(SyntheticGuideRuleChange.OLD_CARD, unlocked = true))

            assertEquals(SyntheticGuideRuleChange.NEW_CARD, card.history.currentRuleId)
            assertNull(card.history.earlierRuleId)
            assertEquals(SyntheticGuideRuleChange.EFFECTIVE, card.trust.replacedUntil)
            assertNotNull(card.full, "an old claim still needs the old rule's words")
        }

    @Test
    fun aNewCardPointsBackAtTheRuleItReplacedWithTheDateItEnded() =
        test {
            val card = assertNotNull(ready(changedModel()).card(SyntheticGuideRuleChange.NEW_CARD, unlocked = true))

            assertEquals(SyntheticGuideRuleChange.OLD_CARD, card.history.earlierRuleId)
            assertEquals(SyntheticGuideRuleChange.EFFECTIVE, card.history.earlierBefore)
            assertNull(card.history.currentRuleId, "the card in force has no 'See current rule'")
            assertTrue(card.trust.updated)
        }

    @Test
    fun theHistoryLinksAreFreeAndLeaveTheLockedHalfEmpty() =
        test {
            val locked = assertNotNull(ready(changedModel()).card(SyntheticGuideRuleChange.OLD_CARD, unlocked = false))

            assertEquals(SyntheticGuideRuleChange.NEW_CARD, locked.history.currentRuleId)
            assertNull(locked.full, "ids and dates only; the paid half is still absent")
        }

    @Test
    fun aPlainCardHasNoHistory() =
        test {
            val card = assertNotNull(ready(plainModel()).card("RB-T5", unlocked = true))

            assertEquals(GuideCardHistory.None, card.history)
            assertFalse(card.trust.updated)
        }

    @Test
    fun aPinToAReplacedCardStillShowsAndCarriesTheReplacedChip() =
        test {
            val rows = ready(changedModel()).pinnedRows(listOf(SyntheticGuideRuleChange.OLD_CARD, "RB-T5"))

            assertEquals(listOf("RB-T9", "RB-T5"), rows.map { it.cardId })
            assertEquals(SyntheticGuideRuleChange.EFFECTIVE, rows.first().trust.replacedUntil)
        }

    @Test
    fun aFeedAndTheCaseTileAgreeAfterARuleChange() =
        test {
            val model = ready(changedModel())
            val feed = assertNotNull(model.feed(SyntheticGuideRuleChange.HOME_CASE, null))
            val tile = assertNotNull(model.area("travel")).cases.first { it.id == SyntheticGuideRuleChange.HOME_CASE }

            assertEquals(listOf("RB-T11", "RB-T10", "RB-T1"), feed.rows.map { it.cardId })
            assertEquals(feed.rows.size, tile.cardCount)
            assertTrue(feed.rows.first().trust.updated)
        }

    @Test
    fun searchNeverListsAReplacedCard() =
        test {
            val model = ready(changedModel())
            val search = GuideSearchViewModel(model, dispatcher)
            search.setUnlocked(true)
            search.onQueryChange("synthetic")
            testScheduler.advanceUntilIdle()

            val rows = (search.state.value as GuideSearchState.Results).rows

            assertFalse(rows.any { it.cardId == SyntheticGuideRuleChange.OLD_CARD })
            assertTrue(rows.any { it.cardId == SyntheticGuideRuleChange.NEW_CARD && it.trust.updated })
        }
}
