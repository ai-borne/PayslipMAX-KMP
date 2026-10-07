package com.payslipmax.pdfparser.guide

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * The JVM proves the bundle is correct, not that Kotlin/Native parses it fast (see `ParserUtilsIosPerfTest`
 * for the regex stall that motivated these checks). This reads the real bundle from the iOS app's compose
 * resources, so it also proves the file is packaged on iOS, then times parse plus validation.
 */
class GuideLoaderIosPerfTest {
    @Test
    fun theShippedBundleParsesWithinBudgetOnNative() =
        runTest {
            val text = GuideBundleContract.readShippedBundleText()

            val mark = TimeSource.Monotonic.markNow()
            val bundle = GuideBundleContract.parseShippedBundle(text)
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            assertTrue(elapsedMs < PARSE_BUDGET_MS, "parse and validate took ${elapsedMs}ms, budget ${PARSE_BUDGET_MS}ms")
            GuideBundleContract.assertMatchesCompiledDataset(bundle)
            GuideBundleContract.assertTilesMatchDataset(bundle)
        }

    @Test
    fun everyFeedAndCardBuildsWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())

            // Index, all 44 feeds and all 402 cards: far more than one screen ever builds at once.
            val mark = TimeSource.Monotonic.markNow()
            GuideBundleContract.assertFeedsAndCardsMatchDataset(bundle)
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds

            assertTrue(elapsedMs < FEEDS_BUDGET_MS, "feeds and cards took ${elapsedMs}ms, budget ${FEEDS_BUDGET_MS}ms")
        }

    private companion object {
        // Generous for a debug simulator build: the first Guide open waits on this, and a quadratic
        // regression in the parser or validator would blow far past it.
        const val PARSE_BUDGET_MS = 1_500L

        // The same margin for the E3 feed and card derivations (plain list and string scans, no regex).
        const val FEEDS_BUDGET_MS = 1_500L
    }
}
