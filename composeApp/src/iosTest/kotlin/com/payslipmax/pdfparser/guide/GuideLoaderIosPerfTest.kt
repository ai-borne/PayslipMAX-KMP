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

    private companion object {
        // Generous for a debug simulator build: the first Guide open waits on this, and a quadratic
        // regression in the parser or validator would blow far past it.
        const val PARSE_BUDGET_MS = 1_500L
    }
}
