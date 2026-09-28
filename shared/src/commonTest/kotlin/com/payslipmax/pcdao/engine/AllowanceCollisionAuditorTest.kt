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
    fun testTptaFieldCollisionMatchesVerifiedSimulationBenchmark() {
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

        assertTrue(result.hasHazards, "Should detect hazard for TPTA + HAFAA")
        assertEquals(1, result.discrepancies.size)

        val disc = result.discrepancies.first()
        assertEquals("HAZARD_TPTA_FIELD_COLLISION", disc.id)
        assertEquals(DiscrepancyType.RECOVERY_HAZARD, disc.type)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        assertEquals("ALLOWANCE_TPTA_003", disc.relevantRuleId)

        // Exact benchmark calculation matching test_simulation_scenarios.py:
        // Monthly = 7200 * 1.60 = 11,520
        // Principal = 11,520 * 6 = 69,120
        // 18% Penal Interest = 69,120 * 0.18 = 12,441.6
        // Total Recovery = 81,561.6
        assertEquals(69120.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(12441.6, result.totalPenalInterest, 0.01)
        assertEquals(81561.6, result.totalRecoveryExposure, 0.01)
        assertEquals(-81561.6, disc.netDue, 0.01)
        assertTrue(disc.authority.contains("12630/Tpt.A"))
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
        assertEquals("HAZARD_TPTA_FIELD_COLLISION", hazards.first().id)
        assertEquals(-81561.6, hazards.first().netDue, 0.01)
    }

    @Test
    fun testTptaSiachenCollision() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes = setOf(AllowanceCollisionCodes.TPTA_PEACE, AllowanceCollisionCodes.SIACHEN),
                    daRate = 0.50,
                    defaultOverdrawnMonths = 6,
                ),
            )

        assertEquals(1, result.discrepancies.size)
        val disc = result.discrepancies.first()
        assertEquals("HAZARD_TPTA_FIELD_COLLISION", disc.id)
        // Rate: 7200 * 1.50 = 10,800. For 6 mos: 64,800. Penal: 11,664. Total: 76,464
        assertEquals(64800.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(11664.0, result.totalPenalInterest, 0.01)
        assertEquals(76464.0, result.totalRecoveryExposure, 0.01)
    }

    @Test
    fun testTptaOtherPlacesRate() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes = setOf(AllowanceCollisionCodes.TPTA_OTHER, AllowanceCollisionCodes.CFAA),
                    daRate = 0.50,
                    defaultOverdrawnMonths = 6,
                    payLevel = "11",
                ),
            )

        // Other places rate for Level 11 is 3600.
        // Monthly = 3600 * 1.50 = 5400.
        // Principal = 5400 * 6 = 32,400.
        // Penal = 32,400 * 0.18 = 5,832.
        // Total = 38,232.
        assertEquals(32400.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(5832.0, result.totalPenalInterest, 0.01)
        assertEquals(38232.0, result.totalRecoveryExposure, 0.01)
    }

    @Test
    fun testTptaMajorGeneralRate() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes = setOf(AllowanceCollisionCodes.TPTA_HIGHER_CITY, AllowanceCollisionCodes.HAFAA),
                    daRate = 0.50,
                    defaultOverdrawnMonths = 6,
                    payLevel = "14",
                ),
            )

        // Major General (Level 14+) Higher City rate is 15,750.
        // Monthly = 15,750 * 1.50 = 23,625.
        // Principal = 23,625 * 6 = 141,750.
        // Penal = 141,750 * 0.18 = 25,515.
        // Total = 167,265.
        assertEquals(141750.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(25515.0, result.totalPenalInterest, 0.01)
        assertEquals(167265.0, result.totalRecoveryExposure, 0.01)
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
        assertTrue(result.criticalHazardsCount >= 2)
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

        // Custom amount: 12000 * 3 = 36,000 principal
        // Penal interest: 36,000 * 0.18 = 6,480
        // Total: 42,480
        assertEquals(36000.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(6480.0, result.totalPenalInterest, 0.01)
        assertEquals(42480.0, result.totalRecoveryExposure, 0.01)
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
