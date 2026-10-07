package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideBundleSource
import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.GuideRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reads and validates the bundle on the first [load], off the caller's thread, then serves the same
 * [GuideLoadResult.Loaded] for the life of the process. Failures are not cached, so a retry reads again.
 */
class LazyGuideRepository(
    private val source: GuideBundleSource,
    private val dispatcher: CoroutineDispatcher,
) : GuideRepository {
    private val mutex = Mutex()
    private var loaded: GuideLoadResult.Loaded? = null

    override suspend fun load(): GuideLoadResult =
        mutex.withLock {
            loaded ?: withContext(dispatcher) { readAndParse() }.also { if (it is GuideLoadResult.Loaded) loaded = it }
        }

    private suspend fun readAndParse(): GuideLoadResult {
        val text =
            try {
                source.readText()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return GuideLoadResult.Failed(GuideLoadError.READ_FAILED)
            }
        return GuideBundleParser.parse(text)
    }
}
