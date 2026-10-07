package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import com.payslipmax.pdfparser.guide.domain.GuideSearchScope
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

    @Test
    fun searchBuildsAndAnswersWithinBudgetOnNative() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())

            // The index over all 402 cards, then the fixed set of realistic queries (a search per keystroke). Plain
            // string scans, so a regex or quadratic regression on Native would blow these budgets.
            val buildMark = TimeSource.Monotonic.markNow()
            val index = GuideSearchIndex(bundle)
            val buildMs = buildMark.elapsedNow().inWholeMilliseconds
            val queryMark = TimeSource.Monotonic.markNow()
            val hits = GuideBundleContract.realisticQueries.sumOf { index.search(it).size }
            val queriesMs = queryMark.elapsedNow().inWholeMilliseconds

            println("guide search on Native: index ${buildMs}ms, ${GuideBundleContract.realisticQueries.size} queries ${queriesMs}ms")
            assertTrue(hits > 0)
            assertTrue(buildMs < SEARCH_BUDGET_MS, "index build took ${buildMs}ms, budget ${SEARCH_BUDGET_MS}ms")
            assertTrue(queriesMs < SEARCH_BUDGET_MS, "queries took ${queriesMs}ms, budget ${SEARCH_BUDGET_MS}ms")
        }

    @Test
    fun searchGivesTheSameAnswersOnNativeAsOnTheJvm() =
        runTest {
            // The full correctness workload, untimed: it proves Native reads words and rule numbers the same way.
            GuideBundleContract.assertSearchMatchesDataset(GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()))
        }

    @Test
    fun previewSearchAndTrustBehaveOnNativeAsOnTheJvm() =
        runTest {
            val bundle = GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText())

            // E5: the free preview searches titles and rule numbers only; time its realistic queries like the full ones.
            val index = GuideSearchIndex(bundle)
            val queryMark = TimeSource.Monotonic.markNow()
            val previewHits = GuideBundleContract.realisticQueries.sumOf { index.search(it, GuideSearchScope.PREVIEW).size }
            val queriesMs = queryMark.elapsedNow().inWholeMilliseconds

            println("guide preview search on Native: ${GuideBundleContract.realisticQueries.size} queries ${queriesMs}ms")
            assertTrue(previewHits > 0)
            assertTrue(queriesMs < SEARCH_BUDGET_MS, "preview queries took ${queriesMs}ms, budget ${SEARCH_BUDGET_MS}ms")
            // The chip counts, the locked cards and the no-leak preview check, untimed: a correctness workload.
            GuideBundleContract.assertTrustAndPreviewMatchDataset(bundle)
        }

    private companion object {
        // Generous for a debug simulator build: the first Guide open waits on this, and a quadratic
        // regression in the parser or validator would blow far past it.
        const val PARSE_BUDGET_MS = 1_500L

        // The same margin for the E3 feed and card derivations (plain list and string scans, no regex).
        const val FEEDS_BUDGET_MS = 1_500L

        // E4 search, for the index build and for the realistic queries each: the same margin again.
        const val SEARCH_BUDGET_MS = 1_500L
    }
}
