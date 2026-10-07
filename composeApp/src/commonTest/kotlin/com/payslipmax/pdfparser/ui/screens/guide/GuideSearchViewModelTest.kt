package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Search states and the owner rule that the query lives in memory only (2026-10-07): it is not in the saved Guide
 * stack, not in telemetry, and gone when the app is killed.
 */
class GuideSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGuideRepository()
    private val crashReporter = FakeCrashReporter()
    private val guide = GuideViewModel(repository, crashReporter, dispatcher)
    private val search = GuideSearchViewModel(guide, dispatcher).also { it.setUnlocked(true) }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private fun TestScope.loadGuide() {
        guide.load()
        testScheduler.advanceUntilIdle()
    }

    private fun TestScope.type(text: String): GuideSearchState {
        search.onQueryChange(text)
        testScheduler.advanceUntilIdle()
        return search.state.value
    }

    private fun GuideSearchState.ids() = assertIs<GuideSearchState.Results>(this).rows.map { it.cardId }

    @Test
    fun nothingShowsUntilSomethingIsTypedAndTheGuideIsLoaded() =
        test {
            assertEquals(GuideSearchState.Idle, type("114"), "the Guide is not loaded yet")
            loadGuide()
            assertIs<GuideSearchState.Results>(search.state.value, "loading re-runs the query typed before it")
            assertEquals(GuideSearchState.Idle, type("  "), "a blank query is idle, not an error")
        }

    @Test
    fun oneLetterIsTooShortToSearch() =
        test {
            loadGuide()
            assertEquals(GuideSearchState.TooShort, type("a"))
            assertEquals(GuideSearchState.TooShort, type("?"))
        }

    @Test
    fun aRuleNumberFindsTheCardsOfTheCaseWhoseRuleLineHasIt() =
        test {
            loadGuide()

            assertEquals(listOf("RB-T9", "RB-T10"), type("Rule 177").ids())
            assertEquals(type("177A").ids(), type("rule 177a").ids())
        }

    @Test
    fun aResultNamesTheCaseItLivesInAndAnUnmatchedQueryIsAnEmptyResultNotAnError() =
        test {
            loadGuide()

            val row = assertIs<GuideSearchState.Results>(type("177")).rows.first()
            assertEquals("Home town LTC", row.caseTitle)
            assertEquals("Synthetic card RB-T9?", row.title)
            assertEquals("A one-line answer for RB-T9.", row.answer)
            assertEquals(GuideSearchState.Results(emptyList(), unlockedScope = true), type("zebra"))
        }

    @Test
    fun theQueryIsCutAtItsLimitSoInputToTheScanIsBounded() =
        test {
            search.onQueryChange("x".repeat(MAX_SEARCH_QUERY_LENGTH + 50))
            assertEquals(MAX_SEARCH_QUERY_LENGTH, search.query.value.length)
        }

    @Test
    fun clearEmptiesTheQueryAndTheResults() =
        test {
            loadGuide()
            type("177")

            search.clear()
            testScheduler.advanceUntilIdle()

            assertEquals("", search.query.value)
            assertEquals(GuideSearchState.Idle, search.state.value)
        }

    @Test
    fun theQueryIsNotSavedWithTheGuideStackAndIsGoneAfterProcessDeath() =
        test {
            loadGuide()
            type("177")
            val nav = GuideNavState(listOf(GuideDestination.Search, GuideDestination.Card("RB-T9")))

            val saved = with(GuideNavStateSaver) { SaverScope { true }.save(nav) } as List<*>
            val restored = GuideNavStateSaver.restore(saved)!!

            assertEquals(listOf("search", "card|RB-T9"), saved, "only ids are written, never the query")
            assertEquals(nav.stack, restored.stack)
            // A killed app is a new process: a new object over the same Guide has no query and no results.
            val afterRestart = GuideSearchViewModel(guide, dispatcher)
            testScheduler.advanceUntilIdle()
            assertEquals("", afterRestart.query.value)
            assertEquals(GuideSearchState.Idle, afterRestart.state.value)
        }

    @Test
    fun searchNeverReachesTelemetry() =
        test {
            loadGuide()
            type("177")
            type("a")
            search.clear()

            assertTrue(crashReporter.exceptions.isEmpty() && crashReporter.logs.isEmpty() && crashReporter.keys.isEmpty(), "a query is never sent")
        }

    @Test
    fun aLockedUserSearchesTitlesAndRuleNumbersButNeverTheLockedFields() =
        test {
            loadGuide()
            search.setUnlocked(false)

            // "details" is only in each card's locked details text ("Longer details for RB-..."); "key" only in a key bullet.
            assertTrue(type("longer details").ids().isEmpty(), "details are not searched for a free user")
            assertTrue(type("short key point").ids().isEmpty(), "key points are not searched for a free user")
            assertEquals(13, type("synthetic card").ids().size, "titles are")
            assertEquals(13, type("rule 114").ids().size, "rule numbers are")
        }

    @Test
    fun unlockingWidensTheSameQueryWithoutRetypingIt() =
        test {
            loadGuide()
            search.setUnlocked(false)
            assertTrue(type("longer details").ids().isEmpty())

            search.setUnlocked(true)
            testScheduler.advanceUntilIdle()

            assertEquals(13, (search.state.value as GuideSearchState.Results).rows.size)
        }

    @Test
    fun aSearchStartsLockedSoAScreenThatForgetsToSetAccessCannotLeak() =
        test {
            val fresh = GuideSearchViewModel(guide, dispatcher)
            loadGuide()
            fresh.onQueryChange("longer details")
            testScheduler.advanceUntilIdle()

            assertTrue((fresh.state.value as GuideSearchState.Results).rows.isEmpty())
        }

    @Test
    fun resultRowsCarryTheirTrustChipsButNoLockedText() =
        test {
            loadGuide()

            val rows = assertIs<GuideSearchState.Results>(type("synthetic card")).rows.associateBy { it.cardId }

            assertTrue(rows.getValue(SyntheticGuideBundle.UNVERIFIED_CARD).trust.unverified)
            assertTrue(rows.getValue(SyntheticGuideBundle.NO_CITE_CARD).trust.noOfficialSource)
            assertEquals(SyntheticGuideBundle.RATES_AS_OF, rows.getValue(SyntheticGuideBundle.PERSONAL_CARD).trust.ratesAsOf)
        }

    @Test
    fun resultsFoundUnderAnotherScopeAreNeverShownToTheCurrentOne() =
        test {
            loadGuide()
            val full = assertIs<GuideSearchState.Results>(type("longer details"))
            assertTrue(full.unlockedScope)
            assertEquals(full, full.visibleTo(unlocked = true))

            // Entitlement revoked while this state is still cached: it must not be shown, not even for one frame.
            assertEquals(GuideSearchState.Idle, full.visibleTo(unlocked = false))

            search.setUnlocked(false)
            testScheduler.advanceUntilIdle()
            val preview = assertIs<GuideSearchState.Results>(search.state.value)
            assertTrue(!preview.unlockedScope)
            assertEquals(preview, preview.visibleTo(unlocked = false))
            assertEquals(GuideSearchState.Idle, preview.visibleTo(unlocked = true), "stale narrower results are hidden too")
            assertEquals(GuideSearchState.TooShort, GuideSearchState.TooShort.visibleTo(unlocked = false))
        }
}
