package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.telemetry.TelemetrySanitizer
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuideViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGuideRepository()
    private val crashReporter = FakeCrashReporter()
    private val viewModel = GuideViewModel(repository, crashReporter, dispatcher)

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun nothingIsReadUntilTheGuideIsFirstOpened() =
        test {
            // Plan baseline: Guide data loads lazily on first open, never at app launch.
            testScheduler.advanceUntilIdle()
            assertEquals(GuideUiState.Loading, viewModel.uiState.value)
            assertEquals(0, repository.loadCount)
        }

    @Test
    fun loadShowsOneTilePerAreaInBundleOrder() =
        test {
            viewModel.load()
            testScheduler.advanceUntilIdle()

            val ready = assertIs<GuideUiState.Ready>(viewModel.uiState.value)
            assertEquals(listOf("travel", "pay"), ready.areas.map { it.id })
            assertEquals(listOf(2, 1), ready.areas.map { it.caseCount })
        }

    @Test
    fun theBundleIsLoadedOnceHoweverOftenTheTabOpens() =
        test {
            viewModel.load()
            testScheduler.advanceUntilIdle()
            viewModel.load()
            viewModel.load()
            testScheduler.advanceUntilIdle()
            assertEquals(1, repository.loadCount)
        }

    @Test
    fun aFailedLoadShowsTheErrorAndRetryRecovers() =
        test {
            repository.result = GuideLoadResult.Failed(GuideLoadError.MALFORMED)
            viewModel.load()
            testScheduler.advanceUntilIdle()
            assertEquals(GuideUiState.Failed(GuideLoadError.MALFORMED), viewModel.uiState.value)

            repository.result = GuideBundleParser.parse(SyntheticGuideBundle.JSON)
            viewModel.retry()
            testScheduler.advanceUntilIdle()
            assertIs<GuideUiState.Ready>(viewModel.uiState.value)
            assertEquals(2, repository.loadCount)
        }

    @Test
    fun aFailureReportsOnlyItsErrorCode() =
        test {
            // Plan "Fail loudly in production": never a blank tab, and only the code leaves the device.
            repository.result = GuideLoadResult.Failed(GuideLoadError.UNSUPPORTED_VERSION)
            viewModel.load()
            testScheduler.advanceUntilIdle()

            val (throwable, metadata) = crashReporter.exceptions.single()
            assertEquals(mapOf(GUIDE_LOAD_ERROR_KEY to "UNSUPPORTED_VERSION"), metadata)
            assertEquals("guide_load_failed_UNSUPPORTED_VERSION", throwable.message)
            assertTrue(TelemetrySanitizer.isKeyAllowed(GUIDE_LOAD_ERROR_KEY), "the key must pass the sanitizer")
            assertTrue(crashReporter.logs.isEmpty() && crashReporter.keys.isEmpty())
        }

    @Test
    fun aSuccessfulLoadReportsNothing() =
        test {
            viewModel.load()
            testScheduler.advanceUntilIdle()
            assertTrue(crashReporter.exceptions.isEmpty())
        }

    @Test
    fun areaContentListsCaseTilesWithCountsAndRuleSubtitles() =
        test {
            viewModel.load()
            testScheduler.advanceUntilIdle()

            val travel = viewModel.area("travel")!!
            assertEquals("Travel", travel.title)
            // A case's count is every card its feed will list: the ones homed there plus "also relevant here".
            assertEquals(
                listOf(
                    GuideCaseTile(SyntheticGuideBundle.BIG_CASE, "Daily allowance on duty", "Rule 114", 8),
                    GuideCaseTile("ltc-home", "Home town LTC", "Rule 177A", 3),
                ),
                travel.cases,
            )
            assertEquals("", viewModel.area("pay")!!.cases.single().subtitle)
        }

    @Test
    fun unknownIdsAndAnUnloadedBundleResolveToNull() =
        test {
            assertNull(viewModel.area("travel"), "nothing resolves before loading")
            viewModel.load()
            testScheduler.advanceUntilIdle()
            assertNull(viewModel.area("nowhere"))
            assertNull(viewModel.feed("nowhere", facet = null))
            assertNull(viewModel.card("RB-NONE"))
            assertEquals("Home town LTC", viewModel.feed("ltc-home", facet = null)!!.title)
            assertEquals("Synthetic card RB-P1?", viewModel.card("RB-P1")!!.title)
        }

    private fun loaded(block: suspend TestScope.() -> Unit) =
        test {
            viewModel.load()
            testScheduler.advanceUntilIdle()
            block()
        }

    @Test
    fun aBigFeedOffersFacetChipsWithCountsInBundleOrder() =
        loaded {
            val feed = viewModel.feed(SyntheticGuideBundle.BIG_CASE, facet = null)!!

            assertEquals("Rule 114", feed.subtitle)
            assertEquals(8, feed.totalCount)
            assertEquals(
                listOf(
                    GuideFacetCount("Q", "Who qualifies", 2),
                    GuideFacetCount("H", "How much", 3),
                    GuideFacetCount("C", "How to claim", 2),
                    GuideFacetCount("L", "Limits and traps", 1),
                ),
                feed.facets,
            )
            assertEquals(8, feed.rows.size)
            assertEquals("How much", feed.rows.first().facetLabel, "facet labels are bundle content")
        }

    @Test
    fun aChosenFacetFiltersTheRowsButNotTheChipCounts() =
        loaded {
            val feed = viewModel.feed(SyntheticGuideBundle.BIG_CASE, facet = "H")!!

            assertEquals("H", feed.selectedFacet)
            assertEquals(listOf("RB-T1", "RB-T2", "RB-T7"), feed.rows.map { it.cardId })
            assertEquals(8, feed.totalCount, "the All chip still counts every card")
        }

    @Test
    fun aSmallFeedHasNoChipsAndIgnoresARestoredFacet() =
        loaded {
            val feed = viewModel.feed("ltc-home", facet = "Q")!!

            assertTrue(feed.facets.isEmpty())
            assertNull(feed.selectedFacet)
            assertEquals(3, feed.rows.size)
        }

    @Test
    fun anAlsoRelevantRowNamesTheCasesItIsHomedIn() =
        loaded {
            val rows = viewModel.feed("ltc-home", facet = null)!!.rows

            assertEquals(listOf(null, null, "Daily allowance on duty"), rows.map { it.alsoHomeTitle })
        }

    @Test
    fun cardContentHidesPlaceholderBulletsAndKeepsTheRest() =
        loaded {
            val card = viewModel.card(SyntheticGuideBundle.PERSONAL_CARD)!!

            assertEquals("How much", card.facetLabel)
            assertEquals(emptyList(), card.body.key, "its only key bullet is a placeholder")
            assertEquals(listOf("Your amount: Level {level} = Rs {food_rate}/day"), card.body.figureTemplates)
            assertEquals(listOf("A form"), card.body.attach)
            assertEquals("Rule 114 TR", card.cite)
        }

    @Test
    fun aFeedsBreadcrumbLeadsToGuideHomeAndItsArea() =
        loaded {
            val stack = listOf(GuideDestination.Home, GuideDestination.Area("travel"), GuideDestination.Case("ltc-home"))

            val crumbs = viewModel.crumbs(stack)

            assertEquals(listOf(GuideStrings.breadcrumbHome, "Travel"), crumbs.map { it.label })
            assertEquals(listOf(emptyList(), listOf(GuideDestination.Area("travel"))), crumbs.map { it.path })
        }

    @Test
    fun aCardsBreadcrumbUsesTheFeedItWasOpenedFrom() =
        loaded {
            // RB-T1 is homed in td-da but also listed in ltc-home: opened from ltc-home, it leads back there.
            val feed = GuideDestination.Case("ltc-home", facet = null)
            val stack = listOf(GuideDestination.Home, GuideDestination.Area("travel"), feed, GuideDestination.Card("RB-T1"))

            val crumbs = viewModel.crumbs(stack)

            assertEquals(listOf(GuideStrings.breadcrumbHome, "Travel", "Home town LTC"), crumbs.map { it.label })
            assertEquals(listOf(GuideDestination.Area("travel"), feed), crumbs.last().path)
        }

    @Test
    fun aCardOpenedFromElsewhereLeadsToItsOwnCase() =
        loaded {
            val stack = listOf(GuideDestination.Home, GuideDestination.Search, GuideDestination.Card("RB-P2"))

            val crumbs = viewModel.crumbs(stack)

            assertEquals(listOf(GuideStrings.breadcrumbHome, "Pay", "House rent"), crumbs.map { it.label })
            assertEquals(listOf(GuideDestination.Area("pay"), GuideDestination.Case("pay-hra")), crumbs.last().path)
        }
}
