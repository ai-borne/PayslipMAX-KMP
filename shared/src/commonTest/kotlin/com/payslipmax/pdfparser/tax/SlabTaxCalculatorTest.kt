package com.payslipmax.pdfparser.tax

import com.payslipmax.pdfparser.tax.rules.NewRegimeSlabTable
import com.payslipmax.pdfparser.tax.rules.OldRegimeSlabTable
import kotlin.test.Test
import kotlin.test.assertEquals

/** Boundary coverage at every slab edge, for every FY-versioned table (Phase 1 plan requirement). */
class SlabTaxCalculatorTest {
    @Test
    fun oldRegimePreFy2017_18UsesTenPercentBand() {
        val slabs = OldRegimeSlabTable.forFy("2016-17")
        assertEquals(0.0, SlabTaxCalculator.computeTax(100000.0, slabs))
        assertEquals(0.0, SlabTaxCalculator.computeTax(250000.0, slabs))
        assertEquals(25000.0, SlabTaxCalculator.computeTax(500000.0, slabs))
        assertEquals(75000.0, SlabTaxCalculator.computeTax(750000.0, slabs))
        assertEquals(125000.0, SlabTaxCalculator.computeTax(1000000.0, slabs))
        assertEquals(275000.0, SlabTaxCalculator.computeTax(1500000.0, slabs))
    }

    @Test
    fun oldRegimeFromFy2017_18UsesFivePercentBand() {
        val slabs = OldRegimeSlabTable.forFy("2017-18")
        assertEquals(0.0, SlabTaxCalculator.computeTax(250000.0, slabs))
        assertEquals(12500.0, SlabTaxCalculator.computeTax(500000.0, slabs))
        assertEquals(62500.0, SlabTaxCalculator.computeTax(750000.0, slabs))
        assertEquals(112500.0, SlabTaxCalculator.computeTax(1000000.0, slabs))
        assertEquals(262500.0, SlabTaxCalculator.computeTax(1500000.0, slabs))
    }

    @Test
    fun oldRegimeUnchangedThroughFy2026_27() {
        val slabs = OldRegimeSlabTable.forFy("2026-27")
        assertEquals(112500.0, SlabTaxCalculator.computeTax(1000000.0, slabs))
    }

    @Test
    fun newRegimeFy2023_24Slabs() {
        val slabs = NewRegimeSlabTable.forFy("2023-24")
        assertEquals(0.0, SlabTaxCalculator.computeTax(300000.0, slabs))
        assertEquals(15000.0, SlabTaxCalculator.computeTax(600000.0, slabs))
        assertEquals(45000.0, SlabTaxCalculator.computeTax(900000.0, slabs))
        assertEquals(90000.0, SlabTaxCalculator.computeTax(1200000.0, slabs))
        assertEquals(150000.0, SlabTaxCalculator.computeTax(1500000.0, slabs))
        assertEquals(300000.0, SlabTaxCalculator.computeTax(2000000.0, slabs))
    }

    @Test
    fun newRegimeFy2024_25Slabs() {
        val slabs = NewRegimeSlabTable.forFy("2024-25")
        assertEquals(0.0, SlabTaxCalculator.computeTax(300000.0, slabs))
        assertEquals(20000.0, SlabTaxCalculator.computeTax(700000.0, slabs))
        assertEquals(50000.0, SlabTaxCalculator.computeTax(1000000.0, slabs))
        assertEquals(80000.0, SlabTaxCalculator.computeTax(1200000.0, slabs))
        assertEquals(140000.0, SlabTaxCalculator.computeTax(1500000.0, slabs))
        assertEquals(290000.0, SlabTaxCalculator.computeTax(2000000.0, slabs))
    }

    @Test
    fun newRegimeFy2025_26AndOnwardSlabs() {
        for (fy in listOf("2025-26", "2026-27")) {
            val slabs = NewRegimeSlabTable.forFy(fy)
            assertEquals(0.0, SlabTaxCalculator.computeTax(400000.0, slabs), "fy=$fy")
            assertEquals(20000.0, SlabTaxCalculator.computeTax(800000.0, slabs), "fy=$fy")
            assertEquals(60000.0, SlabTaxCalculator.computeTax(1200000.0, slabs), "fy=$fy")
            assertEquals(120000.0, SlabTaxCalculator.computeTax(1600000.0, slabs), "fy=$fy")
            assertEquals(200000.0, SlabTaxCalculator.computeTax(2000000.0, slabs), "fy=$fy")
            assertEquals(300000.0, SlabTaxCalculator.computeTax(2400000.0, slabs), "fy=$fy")
            assertEquals(480000.0, SlabTaxCalculator.computeTax(3000000.0, slabs), "fy=$fy")
        }
    }

    @Test
    fun apr2026GroundTruthReproducesExactly() {
        // docs/Plan/04_TaxPlannerGoldStandard.md S1.1: netTaxableIncome 3412740 -> Total Tax Payable 603822.
        val slabs = NewRegimeSlabTable.forFy("2026-27")
        assertEquals(603822.0, SlabTaxCalculator.computeTax(3412740.0, slabs))
    }

    @Test
    fun marginalRateMatchesActiveBand() {
        val slabs = NewRegimeSlabTable.forFy("2025-26")
        assertEquals(0.0, SlabTaxCalculator.marginalRate(200000.0, slabs))
        assertEquals(0.05, SlabTaxCalculator.marginalRate(600000.0, slabs))
        assertEquals(0.30, SlabTaxCalculator.marginalRate(3000000.0, slabs))
    }
}
