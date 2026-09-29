package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SituationalAutoInfererArmyTest {
    private val inferer = SituationalAutoInferer()

    private fun createArmyPayslip(
        basicPay: Double = 121200.0,
        da: Double = 65448.0,
        tpta: Double = 0.0,
        fieldAllowance: Double = 0.0,
        rhAllowance: Double = 0.0,
        hra: Double = 0.0,
        npa: Double = 0.0,
        technicalAllowance: Double = 0.0,
        licenseFee: Double = 0.0,
        daysWorked: Int? = null,
    ): ParsedPayslip {
        return ParsedPayslip(
            file = "army_auto_infer_test.pdf",
            year = 2026,
            monthNum = 3,
            monthName = "March",
            dateStr = "2026-03-31",
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
                    nonPracticingAllowance = npa,
                    technicalAllowance = technicalAllowance,
                    daysWorked = daysWorked,
                ),
            deductions = Deductions(licenseFee = licenseFee),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = basicPay + da, totalDeductions = licenseFee, netRemittance = basicPay),
            taxAndSavings = null,
        )
    }

    @Test
    fun testAutoInfersNpaAndActivatesCadreAmcNpaAndSpecializedFactor() {
        val payslip = createArmyPayslip(basicPay = 121200.0, npa = 24240.0)
        val flags = inferer.inferFlags(payslip)
        assertTrue(flags.inferredNpaActive)

        val context = inferer.inferActiveContext(payslip)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.CADRE_AMC_NPA))
        assertTrue(context.activeSpecializedFactors.contains(SpecializedMilitaryFactor.NON_PRACTICING_ALLOWANCE_AMC))
    }

    @Test
    fun testAutoInfersCfaaStandardAndEscalatedRates() {
        val payslipStandard = createArmyPayslip(fieldAllowance = 10500.0)
        val flagsStandard = inferer.inferFlags(payslipStandard)
        assertTrue(flagsStandard.inferredCfaaActive)
        assertFalse(flagsStandard.inferredCmfaaActive)

        val contextStandard = inferer.inferActiveContext(payslipStandard)
        assertTrue(contextStandard.activeTileIds.contains(SituationalTileKeys.POST_FIELD_CFAA))
        assertFalse(contextStandard.activeTileIds.contains(SituationalTileKeys.POST_FIELD_HAFAA))
        assertFalse(contextStandard.activeTileIds.contains(SituationalTileKeys.POST_FIELD_CMFAA))

        val payslipEscalated = createArmyPayslip(fieldAllowance = 13125.0)
        val flagsEscalated = inferer.inferFlags(payslipEscalated)
        assertTrue(flagsEscalated.inferredCfaaActive)

        val contextEscalated = inferer.inferActiveContext(payslipEscalated)
        assertTrue(contextEscalated.activeTileIds.contains(SituationalTileKeys.POST_FIELD_CFAA))
    }

    @Test
    fun testAutoInfersCmfaaStandardAndEscalatedRates() {
        val payslipStandard = createArmyPayslip(fieldAllowance = 6300.0)
        val flagsStandard = inferer.inferFlags(payslipStandard)
        assertTrue(flagsStandard.inferredCmfaaActive)
        assertFalse(flagsStandard.inferredCfaaActive)

        val contextStandard = inferer.inferActiveContext(payslipStandard)
        assertTrue(contextStandard.activeTileIds.contains(SituationalTileKeys.POST_FIELD_CMFAA))
        assertFalse(contextStandard.activeTileIds.contains(SituationalTileKeys.POST_FIELD_HAFAA))
        assertFalse(contextStandard.activeTileIds.contains(SituationalTileKeys.POST_FIELD_CFAA))

        val payslipEscalated = createArmyPayslip(fieldAllowance = 7875.0)
        val flagsEscalated = inferer.inferFlags(payslipEscalated)
        assertTrue(flagsEscalated.inferredCmfaaActive)

        val contextEscalated = inferer.inferActiveContext(payslipEscalated)
        assertTrue(contextEscalated.activeTileIds.contains(SituationalTileKeys.POST_FIELD_CMFAA))
    }

    @Test
    fun testAutoInfersTwoLocationConcessionWhenFieldAndHraBothPresent() {
        val payslipHafaaTlc = createArmyPayslip(fieldAllowance = 16900.0, hra = 24000.0)
        val flags = inferer.inferFlags(payslipHafaaTlc)
        assertTrue(flags.inferredTwoLocationConcession)
        assertTrue(flags.inferredField)
        assertTrue(flags.inferredHraActive)

        val context = inferer.inferActiveContext(payslipHafaaTlc)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.HOUSE_FAMILY_SPR))
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.POST_FIELD_HAFAA))
    }

    @Test
    fun testAutoInfersTwoLocationConcessionWithCfaaAndHra() {
        val payslipCfaaTlc = createArmyPayslip(fieldAllowance = 10500.0, hra = 18000.0)
        val flags = inferer.inferFlags(payslipCfaaTlc)
        assertTrue(flags.inferredTwoLocationConcession)
        assertTrue(flags.inferredCfaaActive)

        val context = inferer.inferActiveContext(payslipCfaaTlc)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.POST_FIELD_CFAA))
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.HOUSE_FAMILY_SPR))
    }

    @Test
    fun testDoesNotInferTwoLocationConcessionWhenFieldWithoutHra() {
        val payslipNoHra = createArmyPayslip(fieldAllowance = 16900.0, hra = 0.0, licenseFee = 1800.0)
        val flags = inferer.inferFlags(payslipNoHra)
        assertFalse(flags.inferredTwoLocationConcession)

        val context = inferer.inferActiveContext(payslipNoHra)
        assertFalse(context.activeTileIds.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.HOUSE_GOVT_MQ))
    }

    @Test
    fun testAutoInfersTechnicalPayAndActivatesCadreTechnicalOfficer() {
        val payslip = createArmyPayslip(technicalAllowance = 3000.0)
        val flags = inferer.inferFlags(payslip)
        assertTrue(flags.inferredTechnicalActive)

        val context = inferer.inferActiveContext(payslip)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.CADRE_TECHNICAL_OFFICER))
        assertTrue(context.activeSpecializedFactors.contains(SpecializedMilitaryFactor.TECHNICAL_ALLOWANCE))
    }

    @Test
    fun testAutoInfersFullMonthLeaveWhenDaysWorkedIsZero() {
        val payslipLeave = createArmyPayslip(tpta = 7200.0, daysWorked = 0)
        val flags = inferer.inferFlags(payslipLeave)
        assertTrue(flags.inferredFullMonthLeave)

        val context = inferer.inferActiveContext(payslipLeave)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.LEAVE_FULL_MONTH))

        val payslipNormal = createArmyPayslip(tpta = 7200.0, daysWorked = 30)
        val flagsNormal = inferer.inferFlags(payslipNormal)
        assertFalse(flagsNormal.inferredFullMonthLeave)

        val contextNormal = inferer.inferActiveContext(payslipNormal)
        assertFalse(contextNormal.activeTileIds.contains(SituationalTileKeys.LEAVE_FULL_MONTH))
    }

    @Test
    fun testCombinedAmcHospitalInference() {
        val amcPayslip =
            createArmyPayslip(
                basicPay = 121200.0,
                npa = 24240.0,
                tpta = 7200.0,
                hra = 24240.0,
            )
        val flags = inferer.inferFlags(amcPayslip)
        assertTrue(flags.inferredNpaActive)
        assertTrue(flags.inferredPeaceHigher)

        val context = inferer.inferActiveContext(amcPayslip)
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.CADRE_AMC_NPA))
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
        assertTrue(context.activeTileIds.contains(SituationalTileKeys.HOUSE_FAMILY_SPR))
        assertTrue(context.activeSpecializedFactors.contains(SpecializedMilitaryFactor.NON_PRACTICING_ALLOWANCE_AMC))
    }
}
