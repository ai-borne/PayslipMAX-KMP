package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DsopTaxShieldAuditorTest {
    private val auditor = DsopTaxShieldAuditor()

    @Test
    fun testNormalSubscriptionUnder5LCapIsClean() {
        val result =
            auditor.auditSubscription(
                monthlySubscription = 40000.0,
                monthsToRetirement = 24,
            )

        assertEquals(480000.0, result.annualProjectedSubscription)
        assertEquals(0.0, result.excessContribution)
        assertEquals(0.0, result.taxableInterest)
        assertEquals(0.0, result.estimatedTaxDrag)
        assertFalse(result.hasTaxExposure)
        assertFalse(result.hasRetirementAlarm)
        assertTrue(result.discrepancies.isEmpty())
    }

    @Test
    fun testExact5LCapBoundaryHasZeroExcess() {
        val request =
            DsopTaxShieldRequest(
                annualSubscription = 500000.0,
                monthlySubscription = 0.0,
            )
        val result = auditor.audit(request)

        assertEquals(0.0, result.excessContribution)
        assertEquals(0.0, result.taxableInterest)
        assertFalse(result.hasTaxExposure)
        assertTrue(result.discrepancies.isEmpty())
    }

    @Test
    fun test600kAnnualSubscriptionMatchesSimulationBenchmark() {
        // Benchmark from test_simulation_scenarios.py:
        // Monthly: 50,000 -> Annual: 600,000
        // Excess: 100,000
        // Taxable Interest @ 7.1%: 7,100.00
        // Tax Drag @ 31.2%: 2,215.20
        val result =
            auditor.auditSubscription(
                monthlySubscription = 50000.0,
                monthsToRetirement = 12,
            )

        assertTrue(result.hasTaxExposure)
        assertEquals(600000.0, result.annualProjectedSubscription)
        assertEquals(100000.0, result.excessContribution)
        assertEquals(7100.0, result.taxableInterest, 0.01)
        assertEquals(2215.20, result.estimatedTaxDrag, 0.01)
        assertFalse(result.hasRetirementAlarm)

        assertEquals(1, result.discrepancies.size)
        val disc = result.discrepancies.first()
        assertEquals("TAX_EXPOSURE_DSOPF_5L_CAP", disc.id)
        assertEquals(DiscrepancyType.TAX_EXPOSURE, disc.type)
        assertEquals(DiscrepancySeverity.WARNING, disc.severity)
        assertEquals(100000.0, disc.drawnAmount)
        assertEquals(500000.0, disc.entitledAmount)
        assertEquals(-7100.0, disc.netDue, 0.01)
        assertTrue(disc.authority.contains("Section 10(11)"))
        assertTrue(disc.explanation.contains("₹7,100") || disc.explanation.contains("7100"))
    }

    @Test
    fun testCustomInterestRate() {
        val request =
            DsopTaxShieldRequest(
                monthlySubscription = 50000.0,
                interestRate = 0.075,
            )
        val result = auditor.audit(request)

        assertEquals(100000.0, result.excessContribution)
        assertEquals(7500.0, result.taxableInterest, 0.01)
    }

    @Test
    fun testRetirementIn180DaysDoesNotTriggerStoppageAlarm() {
        // 6 months = 180 days (> 90 days)
        val result =
            auditor.auditSubscription(
                monthlySubscription = 30000.0,
                monthsToRetirement = 6,
            )

        assertFalse(result.hasRetirementAlarm)
        assertFalse(result.isRetirementStoppageViolated)
    }

    @Test
    fun testRetirementWithin60DaysTriggersMandatoryRule14StoppageAlarm() {
        val request =
            DsopTaxShieldRequest(
                monthlySubscription = 50000.0,
                daysToRetirement = 60,
            )
        val result = auditor.audit(request)

        assertTrue(result.hasRetirementAlarm)
        assertTrue(result.isRetirementStoppageViolated)

        val stoppageDisc = result.discrepancies.firstOrNull { it.id.contains("RETIREMENT_STOPPAGE") }
        assertNotNull(stoppageDisc)
        assertEquals(DiscrepancySeverity.CRITICAL, stoppageDisc.severity)
        assertTrue(stoppageDisc.authority.contains("Rule 14"))
        assertTrue(stoppageDisc.explanation.contains("60 days"))
        assertTrue(stoppageDisc.recommendedAction.contains("Part II Order"))
    }

    @Test
    fun testRetirementWithin3MonthsWithZeroSubscriptionIsClean() {
        val request =
            DsopTaxShieldRequest(
                monthlySubscription = 0.0,
                annualSubscription = 0.0,
                daysToRetirement = 45,
            )
        val result = auditor.audit(request)

        // Subscription already stopped -> no violation
        assertFalse(result.isRetirementStoppageViolated)
        assertFalse(result.hasRetirementAlarm)
        assertTrue(result.discrepancies.isEmpty())
    }

    @Test
    fun testCalendarDateRetirementCalculation() {
        // exactly 61 days between dates
        val request =
            DsopTaxShieldRequest(
                monthlySubscription = 40000.0,
                retirementDate = "2026-11-15",
                asOfDate = "2026-09-15",
            )
        val result = auditor.audit(request)

        assertEquals(61, result.daysToRetirement)
        assertTrue(result.isRetirementStoppageViolated)
    }

    @Test
    fun testCombinedExcessAndRetirementStoppageProducesBothDiscrepancies() {
        // 2 months triggers stoppage violation, 50k monthly = 600k annual excess
        val result =
            auditor.auditSubscription(
                monthlySubscription = 50000.0,
                monthsToRetirement = 2,
            )

        assertTrue(result.hasTaxExposure)
        assertTrue(result.hasRetirementAlarm)
        assertEquals(2, result.discrepancies.size)

        val types = result.discrepancies.map { it.id }
        assertTrue(types.any { it.contains("5L_CAP") })
        assertTrue(types.any { it.contains("RETIREMENT_STOPPAGE") })
    }
}
