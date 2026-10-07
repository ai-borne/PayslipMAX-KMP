package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
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
    private val search = GuideSearchViewModel(guide, dispatcher)

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
            assertEquals(GuideSearchState.Results(emptyList()), type("zebra"))
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
}
