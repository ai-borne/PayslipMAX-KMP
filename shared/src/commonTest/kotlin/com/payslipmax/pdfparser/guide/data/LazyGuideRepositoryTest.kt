package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideBundleSource
import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Guide data must cost nothing at app launch and be read once per process: the bundle is 250 KB of JSON,
 * and every Guide screen asks for it. A failed read must not stick, so the error screen's retry can succeed.
 */
class LazyGuideRepositoryTest {
    private class CountingSource(
        var text: () -> String = { SyntheticGuideBundle.JSON },
    ) : GuideBundleSource {
        var reads = 0

        override suspend fun readText(): String {
            reads++
            return text()
        }
    }

    @Test
    fun nothingIsReadUntilTheFirstLoad() =
        runTest {
            val source = CountingSource()
            LazyGuideRepository(source, StandardTestDispatcher(testScheduler))

            assertEquals(0, source.reads)
        }

    @Test
    fun theBundleIsReadOnceAndSharedByEveryCaller() =
        runTest {
            val source = CountingSource()
            val repository = LazyGuideRepository(source, StandardTestDispatcher(testScheduler))

            val results = List(5) { async { repository.load() } }.awaitAll() + repository.load()

            assertEquals(1, source.reads)
            val first = assertIs<GuideLoadResult.Loaded>(results.first())
            results.forEach { assertSame(first.bundle, assertIs<GuideLoadResult.Loaded>(it).bundle) }
        }

    @Test
    fun aFailedReadIsNotCachedSoRetryCanSucceed() =
        runTest {
            val source = CountingSource(text = { throw IllegalStateException("resource missing") })
            val repository = LazyGuideRepository(source, StandardTestDispatcher(testScheduler))

            assertEquals(GuideLoadError.READ_FAILED, assertIs<GuideLoadResult.Failed>(repository.load()).error)

            source.text = { SyntheticGuideBundle.JSON }
            assertIs<GuideLoadResult.Loaded>(repository.load())
            assertEquals(2, source.reads)
        }

    @Test
    fun anInvalidBundleIsAnErrorStateAndIsRetriedNextTime() =
        runTest {
            val source = CountingSource(text = { "{}" })
            val repository = LazyGuideRepository(source, StandardTestDispatcher(testScheduler))

            assertEquals(GuideLoadError.MALFORMED, assertIs<GuideLoadResult.Failed>(repository.load()).error)
            repository.load()
            assertEquals(2, source.reads)
        }

    @Test
    fun cancellationIsNotSwallowedAsAReadFailure() =
        runTest {
            val source = CountingSource(text = { throw CancellationException("screen left") })
            val repository = LazyGuideRepository(source, StandardTestDispatcher(testScheduler))

            assertFailsWith<CancellationException> { repository.load() }
        }
}
