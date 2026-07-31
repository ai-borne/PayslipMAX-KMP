package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DsopAssetComparisonCardTest {
    @Test
    fun calculateAssetProjectionsReturnsFourAssetClasses() {
        val results = calculateAssetProjections(initialBalance = 100_000.0, monthlyContribution = 10_000.0, years = 15)
        assertEquals(4, results.size)
        assertTrue(results.any { it.isTaxFree })
        val dsop = results.first { it.isTaxFree }
        assertEquals(0.0, dsop.taxDeduction)
        assertTrue(dsop.netTaxFreeBalance > 0.0)
    }

    @Test
    fun compoundValueCalculationGrowsWithPositiveRate() {
        val valZeroRate = calculateCompoundValue(initialBalance = 100_000.0, monthlyContribution = 10_000.0, years = 5, annualRate = 0.0)
        val valPosRate = calculateCompoundValue(initialBalance = 100_000.0, monthlyContribution = 10_000.0, years = 5, annualRate = 0.071)
        assertEquals(700_000.0, valZeroRate, 0.01)
        assertTrue(valPosRate > valZeroRate)
    }
}
