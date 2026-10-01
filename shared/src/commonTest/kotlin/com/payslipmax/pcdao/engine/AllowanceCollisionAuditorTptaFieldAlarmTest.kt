package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AllowanceCollisionAuditorTptaFieldAlarmTest {
    private val auditor = AllowanceCollisionAuditor()

    @Test
    fun testTptaSiachenCollisionProducesAdvisoryAlarm() {
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
        assertEquals(MilitaryCollisionCheckers.ID_ALARM_TPTA_FIELD, disc.id)
        assertEquals(0.0, disc.netDue, 0.01)
        assertEquals(0.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(0.0, result.totalPenalInterest, 0.01)
        assertEquals(0.0, result.totalRecoveryExposure, 0.01)
    }

    @Test
    fun testTptaOtherPlacesRateAdvisoryAlarm() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes = setOf(AllowanceCollisionCodes.TPTA_OTHER, AllowanceCollisionCodes.CFAA),
                    daRate = 0.50,
                    defaultOverdrawnMonths = 6,
                    payLevel = "11",
                ),
            )

        assertEquals(1, result.discrepancies.size)
        val disc = result.discrepancies.first()
        assertEquals(MilitaryCollisionCheckers.ID_ALARM_TPTA_FIELD, disc.id)
        assertEquals(0.0, disc.netDue, 0.01)
        assertEquals(0.0, result.totalRecoveryExposure, 0.01)
        assertEquals(32400.0, disc.drawnAmount, 0.01)
    }

    @Test
    fun testTptaMajorGeneralRateAdvisoryAlarm() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes = setOf(AllowanceCollisionCodes.TPTA_HIGHER_CITY, AllowanceCollisionCodes.HAFAA),
                    daRate = 0.50,
                    defaultOverdrawnMonths = 6,
                    payLevel = "14",
                ),
            )

        assertEquals(1, result.discrepancies.size)
        val disc = result.discrepancies.first()
        assertEquals(MilitaryCollisionCheckers.ID_ALARM_TPTA_FIELD, disc.id)
        assertEquals(0.0, disc.netDue, 0.01)
        assertEquals(0.0, result.totalRecoveryExposure, 0.01)
        assertEquals(141750.0, disc.drawnAmount, 0.01)
    }

    @Test
    fun testTptaFieldAdvisoryAlarmStatutoryTextAndActions() {
        val result =
            auditor.audit(
                AllowanceCollisionRequest(
                    activeAllowanceCodes = setOf(AllowanceCollisionCodes.TPTA_HIGHER_CITY, AllowanceCollisionCodes.HAFAA),
                    daRate = 0.60,
                    defaultOverdrawnMonths = 6,
                ),
            )

        val disc = result.discrepancies.first()
        assertEquals(DiscrepancyType.FORFEITURE_RISK, disc.type)
        assertEquals(DiscrepancySeverity.WARNING, disc.severity)
        assertTrue(disc.authority.contains("TR-230(B)"))
        assertTrue(disc.authority.contains("12630/Tpt.A/Mov C/246/D(Mov)/17"))
        assertTrue(disc.explanation.contains("Government conveyance"))
        assertTrue(disc.recommendedAction.contains("Non-Availability Certificate"))
    }
}
