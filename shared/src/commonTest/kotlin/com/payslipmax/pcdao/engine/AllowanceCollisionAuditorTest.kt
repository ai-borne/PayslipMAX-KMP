package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AllowanceCollisionAuditorTest {
    private val auditor = AllowanceCollisionAuditor()

    @Test
    fun testTptaFieldCollisionProducesAdvisoryAlarmWithZeroRecovery() {
        val request =
            AllowanceCollisionRequest(
                activeAllowanceCodes =
                    setOf(
                        AllowanceCollisionCodes.TPTA_HIGHER_CITY,
                        AllowanceCollisionCodes.HAFAA,
                    ),
                daRate = 0.60,
                defaultOverdrawnMonths = 6,
                payLevel = "10",
            )

        val result = auditor.audit(request)

        assertTrue(result.hasHazards, "Should detect alarm discrepancy for TPTA + HAFAA")
        assertEquals(1, result.discrepancies.size)

        val disc = result.discrepancies.first()
        assertEquals(MilitaryCollisionCheckers.ID_ALARM_TPTA_FIELD, disc.id)
        assertEquals(DiscrepancyType.FORFEITURE_RISK, disc.type)
        assertEquals(DiscrepancySeverity.WARNING, disc.severity)
        assertEquals("ALLOWANCE_TPTA_003", disc.relevantRuleId)

        // TPTA in Field Area is an Audit Advisory Alarm, NOT a penal recovery hazard.
        // Monthly = 7200 * 1.60 = 11,520; 6 mos = 69,120.
        // Recovery exposure, principal, and penal interest must all be strictly 0.0.
        assertEquals(0.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(0.0, result.totalPenalInterest, 0.01)
        assertEquals(0.0, result.totalRecoveryExposure, 0.01)
        assertEquals(0.0, disc.netDue, 0.01)
        assertEquals(69120.0, disc.drawnAmount, 0.01)
        assertEquals(69120.0, disc.entitledAmount, 0.01)
        assertTrue(disc.authority.contains("12630/Tpt.A"))
        assertTrue(disc.authority.contains("TR-230(B)"))
        assertTrue(disc.explanation.contains("Government conveyance"))
    }

    @Test
    fun testConvenienceAuditClaimsMethod() {
        val hazards =
            auditor.auditClaims(
                activeAllowances = listOf(AllowanceCollisionCodes.TPTA_HIGHER_CITY, AllowanceCollisionCodes.HAFAA),
                daRate = 0.60,
                monthsOverdrawn = 6,
            )

        assertEquals(1, hazards.size)
        assertEquals(MilitaryCollisionCheckers.ID_ALARM_TPTA_FIELD, hazards.first().id)
        assertEquals(0.0, hazards.first().netDue, 0.01)
    }

    @Test
    fun testSdaTlaCollision() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes = setOf(AllowanceCollisionCodes.SDA, AllowanceCollisionCodes.TLA),
                    basicPay = 100000.0,
                ),
            )

        assertEquals(1, result.discrepancies.size)
        val disc = result.discrepancies.first()
        assertEquals("HAZARD_SDA_TLA_COLLISION", disc.id)
        assertEquals("ALLOWANCE_SDA_002", disc.relevantRuleId)
        assertTrue(disc.authority.contains("Special Compensatory Allowances Regulations"))
    }

    @Test
    fun testHraAccommodationCollision() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes =
                        setOf(
                            AllowanceCollisionCodes.HRA_CLAIMED,
                            AllowanceCollisionCodes.GOVT_ACCOMM_ALLOTTED,
                        ),
                    basicPay = 80000.0,
                    daRate = 0.50,
                ),
            )

        assertEquals(1, result.discrepancies.size)
        val disc = result.discrepancies.first()
        assertEquals("HAZARD_HRA_ACCOMM_COLLISION", disc.id)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        assertEquals("HRA_ACCOMM_001", disc.relevantRuleId)
        assertTrue(disc.authority.contains("HRA Chapter 10"))
    }

    @Test
    fun testFlyingPaySpecialForcesCollision() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes =
                        setOf(
                            AllowanceCollisionCodes.FLYING_PAY,
                            AllowanceCollisionCodes.SPECIAL_FORCES_PAY,
                        ),
                    daRate = 0.50,
                ),
            )

        assertEquals(1, result.discrepancies.size)
        val disc = result.discrepancies.first()
        assertEquals("HAZARD_FLYING_SF_COLLISION", disc.id)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        assertEquals("SPECIAL_ALLOWANCE_MUTUAL_EXCLUSION_001", disc.relevantRuleId)
        assertTrue(disc.authority.contains("MoD letter No. 1(16)/2017/D(Pay/Services)"))
    }

    @Test
    fun testRhMatrixFieldCollision() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes =
                        setOf(
                            AllowanceCollisionCodes.RH_MATRIX_ALLOWANCE,
                            AllowanceCollisionCodes.CFAA,
                        ),
                ),
            )

        assertEquals(1, result.discrepancies.size)
        val disc = result.discrepancies.first()
        assertEquals("HAZARD_RH_FIELD_COLLISION", disc.id)
        assertEquals("RH_CONCURRENT_001", disc.relevantRuleId)
    }

    @Test
    fun testMultipleSimultaneousCollisionsAggregateExposure() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes =
                        setOf(
                            AllowanceCollisionCodes.TPTA_HIGHER_CITY,
                            AllowanceCollisionCodes.HAFAA,
                            AllowanceCollisionCodes.HRA_CLAIMED,
                            AllowanceCollisionCodes.GOVT_ACCOMM_ALLOTTED,
                            AllowanceCollisionCodes.SDA,
                            AllowanceCollisionCodes.TLA,
                        ),
                    basicPay = 80000.0,
                    daRate = 0.50,
                    defaultOverdrawnMonths = 6,
                ),
            )

        assertEquals(3, result.discrepancies.size)
        assertEquals(1, result.criticalHazardsCount)
        assertEquals(2, result.warningHazardsCount)
        assertTrue(result.totalRecoveryExposure > 100000.0)
    }

    @Test
    fun testExplicitAllowanceClaimsWithCustomAmounts() {
        val claims =
            listOf(
                AllowanceClaim(
                    code = AllowanceCollisionCodes.TPTA_HIGHER_CITY,
                    name = "Transport Allowance",
                    monthlyAmount = 12000.0,
                    monthsDrawn = 3,
                ),
                AllowanceClaim(
                    code = AllowanceCollisionCodes.HAFAA,
                    name = "HAFAA Field Allowance",
                    monthlyAmount = 16900.0,
                    monthsDrawn = 3,
                ),
            )

        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    claims = claims,
                    defaultOverdrawnMonths = 3,
                ),
            )

        // Custom amount: 12000 * 3 = 36,000 for TPTA + HAFAA advisory alarm (netDue = 0.0, zero recovery)
        assertEquals(0.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(0.0, result.totalPenalInterest, 0.01)
        assertEquals(0.0, result.totalRecoveryExposure, 0.01)
        val disc = result.discrepancies.first()
        assertEquals(MilitaryCollisionCheckers.ID_ALARM_TPTA_FIELD, disc.id)
        assertEquals(0.0, disc.netDue, 0.01)
        assertEquals(36000.0, disc.drawnAmount, 0.01)
    }

    @Test
    fun testRecoveryHazardsSeparatedFromAdvisoryAlarms() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes =
                        setOf(
                            AllowanceCollisionCodes.TPTA_HIGHER_CITY,
                            AllowanceCollisionCodes.HAFAA,
                            AllowanceCollisionCodes.LEAVE_FULL_MONTH,
                        ),
                    daRate = 0.50,
                    defaultOverdrawnMonths = 1,
                    tptaMonthly = 10800.0,
                ),
            )

        // 2 discrepancies: 1 alarm (TPTA+Field, netDue=0) and 1 hazard (TPTA+Leave, netDue=-12744)
        assertEquals(2, result.discrepancies.size)
        assertEquals(10800.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(1944.0, result.totalPenalInterest, 0.01)
        assertEquals(12744.0, result.totalRecoveryExposure, 0.01)
    }

    @Test
    fun testValidConcurrentAllowancesProduceZeroWarnings() {
        val validCombinations =
            listOf(
                listOf(AllowanceCollisionCodes.SIACHEN, AllowanceCollisionCodes.GALLANTRY_AWARD),
                listOf(AllowanceCollisionCodes.HAFAA, AllowanceCollisionCodes.HIGH_ALTITUDE),
                listOf(AllowanceCollisionCodes.TPTA_PEACE, AllowanceCollisionCodes.HRA_PEACE),
                listOf(AllowanceCollisionCodes.FLYING_PAY, AllowanceCollisionCodes.GALLANTRY_AWARD),
                listOf(AllowanceCollisionCodes.CEA, AllowanceCollisionCodes.HOSTEL_SUBSIDY),
                listOf("BASIC_PAY", "MILITARY_SERVICE_PAY", "DA"),
            )

        for (combo in validCombinations) {
            val result = auditor.audit(AllowanceCollisionRequest(activeAllowanceCodes = combo.toSet()))
            assertFalse(result.hasHazards, "Combination $combo should NOT have hazards")
            assertEquals(0, result.discrepancies.size, "Combination $combo should produce 0 discrepancies")
            assertEquals(0.0, result.totalRecoveryExposure, 0.01)
        }
    }
}
