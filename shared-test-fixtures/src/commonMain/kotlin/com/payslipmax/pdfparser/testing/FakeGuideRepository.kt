package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.GuideRepository
import com.payslipmax.pdfparser.guide.data.GuideBundleParser

/**
 * In-memory [GuideRepository]; by default serves [SyntheticGuideBundle] through the real parser and
 * validator. Pass a [GuideLoadResult.Failed] to drive error and retry states.
 */
class FakeGuideRepository(
    var result: GuideLoadResult = GuideBundleParser.parse(SyntheticGuideBundle.JSON),
) : GuideRepository {
    var loadCount = 0
        private set

    override suspend fun load(): GuideLoadResult {
        loadCount++
        return result
    }
}
