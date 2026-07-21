package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaxRecommendationEngineTest {
    @Test
    fun testElssSuppressionWhen80CCappedByDsop() {
        val opportunities =
            TaxRecommendationEngine.generateOpportunities(
                annualDsop = 180000.0,
                annualAgif = 6000.0,
                marginalRate = 0.30,
                currentMonthNum = 11,
            )

        // Verify no ELSS recommendation is present
        val hasElssPrompt = opportunities.any { it.id.contains("elss", ignoreCase = true) }
        assertFalse(hasElssPrompt)

        // Verify DSOP auto-cap info card is present
        val hasDsopCapInfo = opportunities.any { it.id == "80c_dsop_capped" }
        assertTrue(hasDsopCapInfo)
    }

    @Test
    fun testNpsRecommendationWhenHeadroomAvailable() {
        val opportunities =
            TaxRecommendationEngine.generateOpportunities(
                annualDsop = 180000.0,
                annualAgif = 6000.0,
                marginalRate = 0.30,
                currentMonthNum = 11,
            )

        val npsOpp = opportunities.find { it.id == "80ccd_nps" }
        assertTrue(npsOpp != null)
        assertEquals(50000.0, npsOpp.unusedAmount)
        assertEquals(15000.0, npsOpp.estTaxSaved)
    }
}
