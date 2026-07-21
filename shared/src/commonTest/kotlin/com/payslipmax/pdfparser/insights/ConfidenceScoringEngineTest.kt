package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfidenceScoringEngineTest {
    @Test
    fun testFullYearHighConfidence() {
        val result =
            ConfidenceScoringEngine.computeScore(
                parsedMonthCount = 12,
                grossPayVariancePct = 0.02,
                hasUserDeclarations = true,
            )

        assertEquals(100, result.scorePct)
        assertEquals(ConfidenceTier.HIGH, result.tier)
    }

    @Test
    fun testPartialYearModerateConfidence() {
        val result =
            ConfidenceScoringEngine.computeScore(
                parsedMonthCount = 4,
                grossPayVariancePct = 0.05,
                hasUserDeclarations = false,
            )

        assertEquals(60, result.scorePct)
        assertEquals(ConfidenceTier.MODERATE, result.tier)
    }

    @Test
    fun testSingleMonthPreliminaryConfidence() {
        val result =
            ConfidenceScoringEngine.computeScore(
                parsedMonthCount = 1,
                grossPayVariancePct = 0.0,
                hasUserDeclarations = false,
            )

        assertEquals(38, result.scorePct)
        assertEquals(ConfidenceTier.PRELIMINARY, result.tier)
    }
}
