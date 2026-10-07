package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.telemetry.TelemetrySanitizer
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
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
            assertNull(viewModel.case("nowhere"))
            assertNull(viewModel.card("RB-NONE"))
            assertEquals("Home town LTC", viewModel.case("ltc-home")!!.title)
            assertEquals("Synthetic card RB-P1?", viewModel.card("RB-P1")!!.title)
        }
}
