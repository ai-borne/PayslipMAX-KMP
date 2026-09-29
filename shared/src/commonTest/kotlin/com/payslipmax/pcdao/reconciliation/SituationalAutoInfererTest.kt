package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SituationalAutoInfererTest {
    private val inferer = SituationalAutoInferer()

    private fun createTestPayslip(
        basicPay: Double = 149000.0,
        da: Double = 89400.0,
        tpta: Double = 7200.0,
        fieldAllowance: Double = 0.0,
        rhAllowance: Double = 0.0,
        dsop: Double = 45000.0,
        licenseFee: Double = 0.0,
        hra: Double = 0.0,
        cea: Double = 0.0,
        specialForces: Double = 0.0,
        year: Int = 2026,
        monthNum: Int = 3,
    ): ParsedPayslip {
        return ParsedPayslip(
            file = "test_payslip.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "March",
            dateStr = "$year-03-31",
            officer = Officer(name = "Col R. S. Rathore", accountNo = "01/142/987654", pan = "ABCDE1234F"),
            earnings =
                Earnings(
                    basicPay = basicPay,
                    dearnessAllowance = da,
                    militaryServicePay = 15500.0,
                    transportAllowance = tpta,
                    fieldAllowance = fieldAllowance,
                    riskHardshipAllowance = rhAllowance,
                    houseRentAllowance = hra,
                    childrenEducationAllowance = cea,
                    specialForcesPay = specialForces,
                ),
            deductions =
                Deductions(
                    dsopSubscription = dsop,
                    licenseFee = licenseFee,
                ),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = basicPay + da, totalDeductions = dsop + licenseFee, netRemittance = 100000.0),
            taxAndSavings = null,
        )
    }

    @Test
    fun testAutoInfersDaRateAnd50PercentThreshold() {
        val payslip = createTestPayslip(basicPay = 149000.0, da = 89400.0)
        val flags = inferer.inferFlags(payslip)
        assertEquals(60.0, flags.inferredDaPercent)
        assertTrue(flags.inferredDaCrossed50)

        val lowDaPayslip = createTestPayslip(basicPay = 100000.0, da = 46000.0)
        val lowFlags = inferer.inferFlags(lowDaPayslip)
        assertEquals(46.0, lowFlags.inferredDaPercent)
        assertFalse(lowFlags.inferredDaCrossed50)
    }

    @Test
    fun testAutoInfersRankLevel12AFromBasicPay() {
        val payslipCol = createTestPayslip(basicPay = 149000.0)
        val flagsCol = inferer.inferFlags(payslipCol)
        assertEquals("12A", flagsCol.inferredRankLevel)

        val payslipCapt = createTestPayslip(basicPay = 69000.0)
        val flagsCapt = inferer.inferFlags(payslipCapt)
        assertEquals("10", flagsCapt.inferredRankLevel)

        val payslipMaj = createTestPayslip(basicPay = 85300.0)
        val flagsMaj = inferer.inferFlags(payslipMaj)
        assertEquals("11", flagsMaj.inferredRankLevel)
    }

    @Test
    fun testAutoInfersPeaceHigherRateTpta() {
        val payslipHigher = createTestPayslip(tpta = 7200.0, fieldAllowance = 0.0)
        val flagsHigher = inferer.inferFlags(payslipHigher)
        assertTrue(flagsHigher.inferredPeaceHigher)
        assertFalse(flagsHigher.inferredField)

        val context = inferer.inferActiveContext(payslipHigher)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
    }

    @Test
    fun testAutoInfersFieldDeployment() {
        val payslipField = createTestPayslip(tpta = 0.0, fieldAllowance = 16900.0)
        val flagsField = inferer.inferFlags(payslipField)
        assertTrue(flagsField.inferredField)
        assertFalse(flagsField.inferredPeaceHigher)

        val context = inferer.inferActiveContext(payslipField)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.POST_FIELD_HAFAA))
    }

    @Test
    fun testAutoInfersHighDsopPacingOver5Lakhs() {
        val payslipHighDsop = createTestPayslip(dsop = 45000.0)
        val flagsHighDsop = inferer.inferFlags(payslipHighDsop)
        assertTrue(flagsHighDsop.inferredHighDsop)

        val context = inferer.inferActiveContext(payslipHighDsop)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.DSOP_HIGH_PACING))

        val payslipNormalDsop = createTestPayslip(dsop = 30000.0)
        val flagsNormal = inferer.inferFlags(payslipNormalDsop)
        assertFalse(flagsNormal.inferredHighDsop)
    }

    @Test
    fun testAutoInfersGovernmentAccommodation() {
        val payslipGovtAccomm = createTestPayslip(licenseFee = 1800.0, hra = 0.0)
        val flagsAccomm = inferer.inferFlags(payslipGovtAccomm)
        assertTrue(flagsAccomm.inferredGovtAccomm)

        val context = inferer.inferActiveContext(payslipGovtAccomm)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.HOUSE_GOVT_MQ))
    }

    @Test
    fun testAutoInfersSpecialForcesFactor() {
        val payslipSf = createTestPayslip(specialForces = 25000.0)
        val context = inferer.inferActiveContext(payslipSf)
        assertTrue(context.activeSpecializedFactors.contains(SpecializedMilitaryFactor.MARCOS_SPECIAL_FORCES))
    }

    @Test
    fun testOfficerWith6YearsTenureApproachingMajorTriggersPromotionAdvisory() {
        val commissionPayslip = createTestPayslip(basicPay = 56100.0, year = 2020, monthNum = 3)
        val currentPayslip = createTestPayslip(basicPay = 69000.0, year = 2026, monthNum = 3)
        val history = listOf(commissionPayslip, currentPayslip)

        assertTrue(inferer.inferPromotionEligibility(currentPayslip, history))

        val flags = inferer.inferFlags(currentPayslip, history)
        assertTrue(flags.inferredPromotionEligible)
        assertEquals(SituationalAutoInferer.PROMOTION_ADVISORY, flags.inferredPromotionAdvisory)

        val context = inferer.inferActiveContext(currentPayslip, history)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.PROMOTION_ACTIVE))

        // Negative check: single payslip or fresh commissioning does not auto-trigger
        assertFalse(inferer.inferPromotionEligibility(commissionPayslip, listOf(commissionPayslip)))
        val freshFlags = inferer.inferFlags(commissionPayslip, listOf(commissionPayslip))
        assertFalse(freshFlags.inferredPromotionEligible)
    }
}
