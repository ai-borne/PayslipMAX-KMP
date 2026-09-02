package com.payslipmax.pdfparser.tax

import com.payslipmax.pdfparser.tax.rules.NewRegimeSlabTable
import com.payslipmax.pdfparser.tax.rules.OldRegimeSlabTable
import kotlin.test.Test
import kotlin.test.assertEquals

/** D14/ADR-1: surcharge above Rs 50L, with marginal relief, and the new-regime 25% cap. */
class SurchargeCalculatorTest {
    private val newSlabs = NewRegimeSlabTable.forFy("2025-26")
    private val oldSlabs = OldRegimeSlabTable.forFy("2025-26")

    @Test
    fun noSurchargeBelow50L() {
        val net = 4_000_000.0
        val baseTax = SlabTaxCalculator.computeTax(net, newSlabs)
        assertEquals(0.0, SurchargeCalculator.computeSurcharge(net, baseTax, newSlabs, isNewRegime = true))
    }

    @Test
    fun marginalReliefJustAbove50LThreshold() {
        val net = 5_100_000.0
        val baseTax = SlabTaxCalculator.computeTax(net, newSlabs)
        val surcharge = SurchargeCalculator.computeSurcharge(net, baseTax, newSlabs, isNewRegime = true)
        // Without relief the 10% tier would add Rs 1,11,000; relief caps total tax+surcharge growth to
        // the Rs 1,00,000 income increase over the threshold.
        assertEquals(70000.0, surcharge, 1.0)
        assertEquals(1180000.0, baseTax + surcharge, 1.0)
    }

    @Test
    fun flatFifteenPercentWellAbove1CrNoReliefNeeded() {
        val net = 15_000_000.0
        val baseTax = SlabTaxCalculator.computeTax(net, newSlabs)
        val surcharge = SurchargeCalculator.computeSurcharge(net, baseTax, newSlabs, isNewRegime = true)
        assertEquals(baseTax * 0.15, surcharge, 1.0)
    }

    @Test
    fun marginalReliefJustAbove2CrThreshold() {
        val net = 20_500_000.0
        val baseTax = SlabTaxCalculator.computeTax(net, newSlabs)
        val surcharge = SurchargeCalculator.computeSurcharge(net, baseTax, newSlabs, isNewRegime = true)
        // Without relief the 25% tier would add far more than the Rs 5,00,000 income increase over 2Cr allows.
        assertEquals(1187000.0, surcharge, 1.0)
    }

    @Test
    fun flatTwentyFivePercentWellAbove2CrNoReliefNeeded() {
        val net = 25_000_000.0
        val baseTax = SlabTaxCalculator.computeTax(net, newSlabs)
        val surcharge = SurchargeCalculator.computeSurcharge(net, baseTax, newSlabs, isNewRegime = true)
        assertEquals(baseTax * 0.25, surcharge, 1.0)
    }

    @Test
    fun newRegimeCappedAt25PercentAbove5Cr() {
        val net = 60_000_000.0
        val baseTax = SlabTaxCalculator.computeTax(net, newSlabs)
        val surcharge = SurchargeCalculator.computeSurcharge(net, baseTax, newSlabs, isNewRegime = true)
        assertEquals(baseTax * 0.25, surcharge, 1.0)
    }

    @Test
    fun oldRegimeReaches37PercentAbove5Cr() {
        val net = 60_000_000.0
        val baseTax = SlabTaxCalculator.computeTax(net, oldSlabs)
        val surcharge = SurchargeCalculator.computeSurcharge(net, baseTax, oldSlabs, isNewRegime = false)
        assertEquals(baseTax * 0.37, surcharge, 1.0)
    }
}
