package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.domain.GuideStaleness
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
            assertNull(viewModel.card("RB-NONE", unlocked = true))
            assertEquals("Home town LTC", viewModel.feed("ltc-home", facet = null)!!.title)
            assertEquals("Synthetic card RB-P1?", viewModel.card("RB-P1", unlocked = true)!!.title)
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
            val card = viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true)!!
            val full = card.full!!

            assertEquals("How much", card.facetLabel)
            assertEquals(emptyList(), full.body.key, "its only key bullet is a placeholder")
            assertEquals(listOf("Your amount: Level {level} = Rs {food_rate}/day"), full.body.figureTemplates)
            assertEquals(listOf("A form"), full.body.attach)
            assertEquals("Rule 114 TR", full.cite)
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

    @Test
    fun aLockedCardHoldsOnlyTheFreeHalfNotJustHiddenText() =
        loaded {
            val card = viewModel.card("RB-T5", unlocked = false)!!

            assertNull(card.full, "key points, cite and details are absent from the state, not hidden by the UI")
            assertEquals("Synthetic card RB-T5?", card.title)
            assertEquals("A one-line answer for RB-T5.", card.answer)
            assertEquals("How much", viewModel.card("RB-T1", unlocked = false)!!.facetLabel)
            assertTrue(viewModel.card("RB-T5", unlocked = true)!!.full != null)
        }

    @Test
    fun trustChipsAreFreeSoALockedCardStillShowsThem() =
        loaded {
            assertTrue(viewModel.card(SyntheticGuideBundle.UNVERIFIED_CARD, unlocked = false)!!.trust.unverified)
            assertTrue(viewModel.card(SyntheticGuideBundle.NO_CITE_CARD, unlocked = false)!!.trust.noOfficialSource)
            assertEquals(SyntheticGuideBundle.RATES_AS_OF, viewModel.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = false)!!.trust.ratesAsOf)
        }

    @Test
    fun theFeedRowsCarryTrustChips() =
        loaded {
            val rows = viewModel.feed(SyntheticGuideBundle.BIG_CASE, facet = null)!!.rows.associateBy { it.cardId }

            assertTrue(rows.getValue(SyntheticGuideBundle.UNVERIFIED_CARD).trust.unverified)
            assertTrue(rows.getValue(SyntheticGuideBundle.AMENDED_CARD).trust.amended)
            assertTrue(!rows.getValue("RB-T5").trust.hasAny)
        }

    @Test
    fun theStaleNudgeFollowsTheInjectedClockAndOnlyAppliesToRateCards() =
        test {
            var now = GuideStaleness.epochDays(2026, 8, 1) * MILLIS_PER_DAY
            val guide = GuideViewModel(repository, crashReporter, dispatcher, nowMillis = { now })
            guide.load()
            testScheduler.advanceUntilIdle()

            assertEquals(false, guide.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = false)!!.ratesStale, "7 months old")
            now = GuideStaleness.epochDays(2026, 10, 7) * MILLIS_PER_DAY
            assertEquals(true, guide.card(SyntheticGuideBundle.PERSONAL_CARD, unlocked = false)!!.ratesStale, "9 months old")
            assertEquals(false, guide.card("RB-T5", unlocked = false)!!.ratesStale, "no RATES chip, no nudge however old")
        }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
