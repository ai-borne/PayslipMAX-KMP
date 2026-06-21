package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.insights.Opportunity
import com.ssbmax.pdfparser.insights.OptimizationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaxPlannerViewStateTest {
    private fun makeOptResult(vararg opps: Opportunity) =
        OptimizationResult(
            totalPotentialTaxSaving = opps.sumOf { it.estTaxSaved },
            marginalRatePct = 0.20,
            regimeAssumed = "OLD",
            opportunities = opps.toList(),
            dsopGapMonthly = 1_000.0,
            dsopCorpusUpliftAtRetirement = 500_000.0,
        )

    private fun opp(
        id: String,
        title: String,
        unused: Double,
        taxSaved: Double,
    ) =
        Opportunity(id = id, title = title, unusedAmount = unused, estTaxSaved = taxSaved, action = "Do: $id")

    @Test
    fun testEmptyOpportunitiesReturnsEmptyList() {
        assertTrue(buildTaxOptViewItems(makeOptResult()).isEmpty())
    }

    @Test
    fun testSingleOpportunityMappedCorrectly() {
        val item = buildTaxOptViewItems(makeOptResult(opp("80c_dsop", "80C: DSOP", 60_000.0, 12_000.0))).single()
        assertEquals("80C: DSOP", item.title)
        assertEquals(60_000.0, item.unusedAmount)
        assertEquals(12_000.0, item.estTaxSaved)
    }

    @Test
    fun testMultipleOpportunitiesPreserveOrder() {
        val items =
            buildTaxOptViewItems(
                makeOptResult(
                    opp("80c_dsop", "80C: DSOP", 60_000.0, 12_000.0),
                    opp("80ccd_nps", "NPS 80CCD(1B)", 50_000.0, 10_000.0),
                ),
            )
        assertEquals(2, items.size)
        assertEquals("80C: DSOP", items[0].title)
        assertEquals("NPS 80CCD(1B)", items[1].title)
    }

    @Test
    fun testRegimeLabelContainsOldRegime() {
        val item = buildTaxOptViewItems(makeOptResult(opp("80c_dsop", "80C", 60_000.0, 12_000.0))).single()
        assertTrue(item.regimeLabel.contains("old regime", ignoreCase = true))
    }

    @Test
    fun testActionMappedFromOpportunity() {
        val item = buildTaxOptViewItems(makeOptResult(opp("80c_dsop", "80C", 60_000.0, 12_000.0))).single()
        assertEquals("Do: 80c_dsop", item.action)
    }

    @Test
    fun testEstTaxSavedAndUnusedAmountMappedCorrectly() {
        val item = buildTaxOptViewItems(makeOptResult(opp("80ccd_nps", "NPS", 50_000.0, 15_000.0))).single()
        assertEquals(50_000.0, item.unusedAmount)
        assertEquals(15_000.0, item.estTaxSaved)
    }

    @Test
    fun testZeroTaxSavedWhenMarginalRateIsZero() {
        val item =
            buildTaxOptViewItems(
                makeOptResult(opp("80c_dsop", "80C", 100_000.0, 0.0)),
            ).single()
        assertEquals(0.0, item.estTaxSaved)
    }
}
