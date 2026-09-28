package com.payslipmax.pcdao.reconciliation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SituationalRuleResolverTest {
    private val resolver = SituationalRuleResolver()

    @Test
    fun testDaEscalationAt60PercentThreshold() {
        val rates = resolver.calculateEscalatedRates(60.0)
        assertTrue(rates.isEscalated)
        assertEquals(33750.0, rates.ceaAnnualPerChild)
        assertEquals(2812.5, rates.ceaMonthlyPerChild)
        assertEquals(101250.0, rates.hostelSubsidyAnnual)
        assertEquals(8437.5, rates.hostelSubsidyMonthly)
        assertEquals(25000.0, rates.dressAllowanceAnnual)
        assertEquals(30.0, rates.hraRates["X"])
        assertEquals(20.0, rates.hraRates["Y"])
        assertEquals(10.0, rates.hraRates["Z"])
        assertEquals(53125.0, rates.siachenMonthlyRate)
        assertEquals(21125.0, rates.hafaMonthlyRate)
    }

    @Test
    fun testDaEscalationAt46PercentThreshold() {
        val rates = resolver.calculateEscalatedRates(46.0)
        assertFalse(rates.isEscalated)
        assertEquals(27000.0, rates.ceaAnnualPerChild)
        assertEquals(2250.0, rates.ceaMonthlyPerChild)
        assertEquals(81000.0, rates.hostelSubsidyAnnual)
        assertEquals(6750.0, rates.hostelSubsidyMonthly)
        assertEquals(20000.0, rates.dressAllowanceAnnual)
        assertEquals(27.0, rates.hraRates["X"])
        assertEquals(18.0, rates.hraRates["Y"])
        assertEquals(9.0, rates.hraRates["Z"])
        assertEquals(42500.0, rates.siachenMonthlyRate)
        assertEquals(16900.0, rates.hafaMonthlyRate)
    }

    @Test
    fun testDaEscalationBelow25Percent() {
        val rates = resolver.calculateEscalatedRates(17.0)
        assertFalse(rates.isEscalated)
        assertEquals(24.0, rates.hraRates["X"])
        assertEquals(16.0, rates.hraRates["Y"])
        assertEquals(8.0, rates.hraRates["Z"])
    }

    @Test
    fun testResolveHigherRateTptaEntitlement() {
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_PEACE_HIGHER),
                customDaPercent = 60.0,
            )
        val entitlement = resolver.resolveTptaEntitlement(context, basicPay = 149000.0)
        assertEquals(11520.0, entitlement.entitledMonthly)
        assertEquals(138240.0, entitlement.entitledAnnual)
        assertEquals("ALLOWANCE_TPTA_001", entitlement.relevantRuleId)
    }

    @Test
    fun testResolveDivyangOfficerTptaEntitlement() {
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_PEACE_HIGHER),
                activeSpecializedFactors = setOf(SpecializedMilitaryFactor.DIVYANG_OFFICER),
                customDaPercent = 60.0,
            )
        val entitlement = resolver.resolveTptaEntitlement(context, basicPay = 149000.0)
        assertEquals(23040.0, entitlement.entitledMonthly)
    }

    @Test
    fun testResolveSprHraEntitlement() {
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.HOUSE_FAMILY_SPR),
                sprCityTier = "Y",
                customDaPercent = 60.0,
            )
        val entitlement = resolver.resolveHraEntitlement(context, basicPay = 149000.0)
        assertEquals(29800.0, entitlement.entitledMonthly)
        assertEquals(357600.0, entitlement.entitledAnnual)
        assertEquals("HRA_SPR_001", entitlement.relevantRuleId)
    }

    @Test
    fun testResolveCeaAndHostelEntitlements() {
        val dayContext =
            ActiveSituationalContext(
                numberOfChildrenCea = 2,
                hasHostelChild = false,
                customDaPercent = 60.0,
            )
        val dayEntitlement = resolver.resolveEducationEntitlement(dayContext)
        assertEquals(67500.0, dayEntitlement.entitledAnnual)

        val hostelContext =
            ActiveSituationalContext(
                numberOfChildrenCea = 2,
                hasHostelChild = true,
                customDaPercent = 60.0,
            )
        val hostelEntitlement = resolver.resolveEducationEntitlement(hostelContext)
        assertEquals(202500.0, hostelEntitlement.entitledAnnual)
    }

    @Test
    fun testResolveLtcEncashment() {
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.AVAILED_LTC),
                customDaPercent = 60.0,
            )
        val ltc = resolver.resolveLtcEncashment(context, basicPay = 149000.0, msp = 15500.0)
        assertEquals(87733.33, ltc.entitledAnnual, 0.05)
    }

    @Test
    fun testResolveCompositeTransferGrant() {
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.TRANSFER_CTG),
            )
        val ctg = resolver.resolveCtgEntitlement(context, basicPay = 149000.0)
        assertEquals(119200.0, ctg.entitledAnnual)
    }
}
