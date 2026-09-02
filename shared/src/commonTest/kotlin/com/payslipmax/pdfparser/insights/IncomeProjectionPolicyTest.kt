package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals

class IncomeProjectionPolicyTest {
    @Test
    fun testAnnualMultiplierIsTwelveOverMonthsElapsed() {
        assertEquals(12.0, IncomeProjectionPolicy.annualMultiplier(1), 0.0001)
        assertEquals(3.0, IncomeProjectionPolicy.annualMultiplier(4), 0.0001)
        assertEquals(1.0, IncomeProjectionPolicy.annualMultiplier(12), 0.0001)
    }

    @Test
    fun testArrearsAddedBackVerbatimNotAnnualised() {
        // 90,000/mo regular pay + 10,000 one-off arrears, 1 month elapsed -> 90,000*12 + 10,000, not
        // (90,000+10,000)*12 (D4).
        val projected = IncomeProjectionPolicy.projectAnnualGross(ytdGross = 100000.0, ytdArrears = 10000.0, ytdReimbursements = 0.0, monthsElapsedInFy = 1)
        assertEquals(1090000.0, projected, 0.01)
    }

    @Test
    fun testReimbursementsDroppedEntirelyNeitherAnnualisedNorAddedBack() {
        // 5,000 reimbursement must vanish from the projection completely (D5) -- 95,000*12, not
        // 100,000*12 and not (95,000*12)+5,000.
        val projected = IncomeProjectionPolicy.projectAnnualGross(ytdGross = 100000.0, ytdArrears = 0.0, ytdReimbursements = 5000.0, monthsElapsedInFy = 1)
        assertEquals(1140000.0, projected, 0.01)
    }

    @Test
    fun testApr2026EvidenceBaseFigure() {
        // docs/Plan/04_TaxPlannerGoldStandard.md Phase 3 acceptance: gross 3,01,828; arrears 10,086
        // (ARR-DA + ARR-TPTADA); reimbursements 1,657 (ETKT/adjPayAndAllce); 1 month elapsed.
        val projected =
            IncomeProjectionPolicy.projectAnnualGross(
                ytdGross = 301828.0,
                ytdArrears = 10086.0,
                ytdReimbursements = 1657.0,
                monthsElapsedInFy = 1,
            )
        assertEquals(3491106.0, projected, 0.01)
    }

    @Test
    fun testRegularGrossNeverGoesNegative() {
        val projected =
            IncomeProjectionPolicy.projectAnnualGross(
                ytdGross = 5000.0,
                ytdArrears = 4000.0,
                ytdReimbursements = 4000.0,
                monthsElapsedInFy = 1,
            )
        // regular YTD would be -3,000 without the floor; floored to 0 -> 0*12 + 4,000 arrears.
        assertEquals(4000.0, projected, 0.01)
    }
}
