package com.ssbmax.pdfparser.parser.detection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatementPeriodExtractorTest {
    @Test
    fun extractsAnchoredNumericPeriod() {
        val period = extractStatementPeriod("STATEMENT OF ACCOUNT FOR 03/2025\nBPAY 144700")
        assertEquals(StatementPeriod(month = 3, year = 2025), period)
    }

    @Test
    fun extractsAnchoredNamedMonthPeriod() {
        val period = extractStatementPeriod("STATEMENT OF ACCOUNT FOR March 2025\nBPAY 144700")
        assertEquals(StatementPeriod(month = 3, year = 2025), period)
    }

    @Test
    fun prefersAnchoredPeriodOverUnrelatedStandaloneDate() {
        val period =
            extractStatementPeriod(
                "STATEMENT OF ACCOUNT FOR 03/2025\nBalance of DSOP Subscription as of 31/03/2025",
            )
        assertEquals(StatementPeriod(month = 3, year = 2025), period)
    }

    @Test
    fun fallsBackToStandaloneDateWithoutAnchor() {
        val period = extractStatementPeriod("Some header text\nRef date: 07/2024\nBPAY 130000")
        assertEquals(StatementPeriod(month = 7, year = 2024), period)
    }

    @Test
    fun returnsNullWhenNoDateIsPresent() {
        val period = extractStatementPeriod("Unrecognized document structure without any date")
        assertNull(period)
    }
}
