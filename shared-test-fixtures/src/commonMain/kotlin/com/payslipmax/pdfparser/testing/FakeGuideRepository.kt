package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.GuideRepository
import com.payslipmax.pdfparser.guide.data.GuideBundleParser

/**
 * In-memory [GuideRepository]; by default serves [SyntheticGuideBundle] through the real parser and
 * validator. Pass a [GuideLoadResult.Failed] to drive error and retry states, or [enqueue] results that are
 * served first, one per load, before [result] (e.g. fail once, then succeed).
 */
class FakeGuideRepository(
    var result: GuideLoadResult = GuideBundleParser.parse(SyntheticGuideBundle.JSON),
) : GuideRepository {
    private val queued = ArrayDeque<GuideLoadResult>()
    var loadCount = 0
        private set

    fun enqueue(vararg results: GuideLoadResult) {
        queued.addAll(results)
    }

    override suspend fun load(): GuideLoadResult {
        loadCount++
        return queued.removeFirstOrNull() ?: result
    }
}
