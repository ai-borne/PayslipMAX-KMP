package com.payslipmax.pcdao.reconciliation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SituationalRuleResolverArmyTest {
    private val resolver = SituationalRuleResolver()

    @Test
    fun testAmcNpaCompoundingCalculation() {
        val basicPay = 121200.0
        val npaAmount = resolver.calculateNpaAmount(basicPay)
        assertEquals(24240.0, npaAmount, "NPA must be 20% of Basic Pay")

        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.CADRE_AMC_NPA, SituationalTileKeys.HOUSE_FAMILY_SPR),
                sprCityTier = "X",
                customDaPercent = 54.0,
            )

        val effectiveBase = resolver.calculateEffectivePay(context, basicPay)
        assertEquals(145440.0, effectiveBase, "Effective Base must be Basic + NPA")

        val dearnessAllowance = resolver.calculateDearnessAllowance(context, basicPay)
        assertEquals(78537.60, dearnessAllowance, 0.01, "DA at 54% on ₹1,45,440 base must be ₹78,537.60")

        val hraEntitlement = resolver.resolveHraEntitlement(context, basicPay)
        assertEquals(43632.0, hraEntitlement.entitledMonthly, 0.01, "HRA at 30% on ₹1,45,440 base must be ₹43,632.0")
    }

    @Test
    fun testNpaApexCeilingCap() {
        // Basic ₹2,10,000 + 20% (₹42,000) = ₹2,52,000 -> Exceeds Apex cap of ₹2,37,500
        val basicPay = 210000.0
        val npaAmount = resolver.calculateNpaAmount(basicPay)
        assertEquals(27500.0, npaAmount, "NPA must be capped at ₹27,500 to satisfy ₹2,37,500 Apex limit")
        assertEquals(237500.0, basicPay + npaAmount, "Total must exactly match ₹2,37,500 ceiling")

        // Officer at Level 17 Apex Basic Pay ₹2,37,500 receives ₹0 NPA
        val apexBasic = 237500.0
        assertEquals(0.0, resolver.calculateNpaAmount(apexBasic), "NPA must be 0 for officers at or above Apex ceiling")
    }

    @Test
    fun testRiskAndHardshipAllowancesRates() {
        val contextBelow50 = ActiveSituationalContext(customDaPercent = 46.0)
        val contextAbove50 = ActiveSituationalContext(customDaPercent = 54.0)

        // CFAA (Cell R2H2)
        assertEquals(10500.0, resolver.resolveCfaaEntitlement(contextBelow50).entitledMonthly)
        assertEquals(13125.0, resolver.resolveCfaaEntitlement(contextAbove50).entitledMonthly)

        // CMFAA (Cell R3H2)
        assertEquals(6300.0, resolver.resolveCmfaaEntitlement(contextBelow50).entitledMonthly)
        assertEquals(7875.0, resolver.resolveCmfaaEntitlement(contextAbove50).entitledMonthly)

        // HAFAA (Cell R1H2)
        assertEquals(16900.0, resolver.resolveHafaaEntitlement(contextBelow50).entitledMonthly)
        assertEquals(21125.0, resolver.resolveHafaaEntitlement(contextAbove50).entitledMonthly)

        // Siachen (RH-MAX)
        assertEquals(42500.0, resolver.resolveSiachenEntitlement(contextBelow50).entitledMonthly)
        assertEquals(53125.0, resolver.resolveSiachenEntitlement(contextAbove50).entitledMonthly)
    }

    @Test
    fun testTwoLocationConcessionDualEntitlement() {
        val basicPay = 121200.0
        val tlcContext =
            ActiveSituationalContext(
                activeTileIds =
                    setOf(
                        SituationalTileKeys.POST_FIELD_CFAA,
                        SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION,
                        SituationalTileKeys.HOUSE_FAMILY_SPR,
                    ),
                sprCityTier = "X",
                customDaPercent = 54.0,
            )

        val tlcEntitlement = resolver.resolveTwoLocationConcessionEntitlement(tlcContext, basicPay)
        assertEquals("HOUSING_TLC_001", tlcEntitlement.relevantRuleId)
        assertEquals("HRA_SPR", tlcEntitlement.allowanceKey)
        // 30% of ₹121,200 = ₹36,360
        assertEquals(36360.0, tlcEntitlement.entitledMonthly)

        val allEntitlements = resolver.resolveAllEntitlements(tlcContext, basicPay, msp = 15500.0)
        val keys = allEntitlements.map { it.allowanceKey }.toSet()
        assertTrue(keys.contains("CFAA"), "Must resolve CFAA field allowance")
        assertTrue(keys.contains("HRA_SPR"), "Must legitimately resolve concurrent SPR HRA under TLC")
    }

    @Test
    fun testTwoLocationConcessionPeaceRetentionAndSfAccomm() {
        val basicPay = 121200.0
        val retentionContext =
            ActiveSituationalContext(
                activeTileIds =
                    setOf(
                        SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION,
                        SituationalTileKeys.HOUSE_PEACE_RETENTION,
                    ),
            )
        val retention = resolver.resolveTwoLocationConcessionEntitlement(retentionContext, basicPay)
        assertEquals("TLC_PEACE_RETENTION", retention.allowanceKey)
        assertEquals(0.0, retention.entitledMonthly)

        val sfContext =
            ActiveSituationalContext(
                activeTileIds =
                    setOf(
                        SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION,
                        SituationalTileKeys.HOUSE_SF_ACCOMMODATION,
                    ),
            )
        val sf = resolver.resolveTwoLocationConcessionEntitlement(sfContext, basicPay)
        assertEquals("TLC_SF_ACCOMMODATION", sf.allowanceKey)
        assertEquals(0.0, sf.entitledMonthly)
    }

    @Test
    fun testIslandSpecialDutyAllowanceRates() {
        val basicPay = 100000.0
        val context = ActiveSituationalContext()

        val isda10 = resolver.resolveIslandSpecialDutyAllowance(context, basicPay, tierPercent = 10.0)
        assertEquals(10000.0, isda10.entitledMonthly)

        val isda16 = resolver.resolveIslandSpecialDutyAllowance(context, basicPay, tierPercent = 16.0)
        assertEquals(16000.0, isda16.entitledMonthly)

        val isda20 = resolver.resolveIslandSpecialDutyAllowance(context, basicPay, tierPercent = 20.0)
        assertEquals(20000.0, isda20.entitledMonthly)
    }

    @Test
    fun testTrainingAllowanceRates() {
        val basicPay = 100000.0
        val context = ActiveSituationalContext()

        val academy = resolver.resolveTrainingAllowance(context, basicPay, isNationalAcademy = true)
        assertEquals(24000.0, academy.entitledMonthly)

        val other = resolver.resolveTrainingAllowance(context, basicPay, isNationalAcademy = false)
        assertEquals(12000.0, other.entitledMonthly)
    }

    @Test
    fun testTechnicalPayTiers() {
        val context = ActiveSituationalContext()

        val tier1 = resolver.resolveTechnicalPayEntitlement(context, tier = 1)
        assertEquals(3000.0, tier1.entitledMonthly)
        assertEquals(36000.0, tier1.entitledAnnual)

        val tier2 = resolver.resolveTechnicalPayEntitlement(context, tier = 2)
        assertEquals(4500.0, tier2.entitledMonthly)
        assertEquals(54000.0, tier2.entitledAnnual)
    }

    @Test
    fun testParachuteAllowanceEscalation() {
        val contextBelow50 = ActiveSituationalContext(customDaPercent = 46.0)
        val contextAbove50 = ActiveSituationalContext(customDaPercent = 54.0)

        val paraBelow = resolver.resolveParachuteAllowance(contextBelow50)
        assertEquals(10500.0, paraBelow.entitledMonthly)

        val paraAbove = resolver.resolveParachuteAllowance(contextAbove50)
        assertEquals(13125.0, paraAbove.entitledMonthly)
    }

    @Test
    fun testCompositeTransferGrantWithNpaCompounding() {
        val basicPay = 121200.0
        val contextWithNpa =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.CADRE_AMC_NPA, SituationalTileKeys.TRANSFER_CTG),
            )
        // NPA = ₹24,240, Effective Base = ₹1,45,440. CTG = 80% of ₹1,45,440 = ₹1,16,352.
        val ctg = resolver.resolveCtgEntitlement(contextWithNpa, basicPay)
        assertEquals(116352.0, ctg.entitledAnnual)
    }

    @Test
    fun testResolveAllEntitlementsForMissionPresets() {
        val basicPay = 121200.0
        val msp = 15500.0

        // RR CI Ops Preset context
        val rrContext =
            SituationalMissionPresets.getById(MissionPresetId.RR_CI_OPS).let { preset ->
                ActiveSituationalContext(
                    activeTileIds = preset.tileIds,
                    activeSpecializedFactors = preset.specializedFactors,
                    sprCityTier = preset.sprCityTier,
                    customDaPercent = 54.0,
                )
            }
        val rrEntitlements = resolver.resolveAllEntitlements(rrContext, basicPay, msp)
        val rrKeys = rrEntitlements.map { it.allowanceKey }.toSet()
        assertTrue(rrKeys.contains("CFAA"))
        assertTrue(rrKeys.contains("HRA_SPR"))

        // AMC Hospital Preset context
        val amcContext =
            SituationalMissionPresets.getById(MissionPresetId.AMC_HOSPITAL).let { preset ->
                ActiveSituationalContext(
                    activeTileIds = preset.tileIds,
                    activeSpecializedFactors = preset.specializedFactors,
                    customDaPercent = 54.0,
                )
            }
        val amcEntitlements = resolver.resolveAllEntitlements(amcContext, basicPay, msp)
        val amcKeys = amcEntitlements.map { it.allowanceKey }.toSet()
        assertTrue(amcKeys.contains("NPA"))
        assertTrue(amcKeys.contains("TPTA"))
    }
}
