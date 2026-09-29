package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AllowanceCollisionAuditorArmyTest {
    private val auditor = AllowanceCollisionAuditor()

    private fun createArmyPayslip(
        tpta: Double = 0.0,
        tptaDa: Double = 0.0,
        rationMoney: Double = 0.0,
        dsop: Double = 0.0,
        basicPay: Double = 121200.0,
    ): ParsedPayslip {
        return ParsedPayslip(
            file = "army_officer_test.pdf",
            year = 2026,
            monthNum = 3,
            monthName = "March",
            dateStr = "2026-03-31",
            officer = Officer(name = "Maj Vikram Batra", accountNo = "01/123/456789", pan = "ABCDE1234F"),
            earnings =
                Earnings(
                    basicPay = basicPay,
                    dearnessAllowance = basicPay * 0.54,
                    militaryServicePay = 15500.0,
                    transportAllowance = tpta,
                    transportAllowanceDa = tptaDa,
                    rationMoney = rationMoney,
                ),
            deductions = Deductions(dsopSubscription = dsop),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = basicPay * 1.6, totalDeductions = dsop, netRemittance = basicPay * 1.6 - dsop),
            taxAndSavings = null,
        )
    }

    @Test
    fun testFullMonthLeaveTptaCollisionTriggersRecoveryHazardWithPenalInterest() {
        val request =
            AllowanceCollisionRequest(
                activeAllowanceCodes =
                    setOf(
                        SituationalTileKeys.LEAVE_FULL_MONTH,
                        AllowanceCollisionCodes.TPTA_HIGHER_CITY,
                    ),
                tptaMonthly = 11088.0,
                daRate = 0.54,
                payLevel = "10",
            )

        val result = auditor.audit(request)

        assertTrue(result.hasHazards, "Drawing TPTA during full calendar month leave must trigger hazard")
        assertEquals(1, result.discrepancies.size)

        val disc = result.discrepancies.first()
        assertEquals("HAZARD_LEAVE_TPTA_001", disc.id)
        assertEquals(DiscrepancyType.RECOVERY_HAZARD, disc.type)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        assertEquals("ALLOWANCE_TPTA_LEAVE_001", disc.relevantRuleId)

        // Exact statutory math:
        // Principal = ₹11,088.0 (1 month TPTA)
        // 18% Statutory Penal Interest = 11,088 * 0.18 = ₹1,995.84
        // Total Recovery Exposure = ₹13,083.84
        assertEquals(11088.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(1995.84, result.totalPenalInterest, 0.01)
        assertEquals(13083.84, result.totalRecoveryExposure, 0.01)
        assertEquals(-13083.84, disc.netDue, 0.01)
        assertTrue(disc.authority.contains("TR-230(B)") || disc.authority.contains("Rule 230(B)"))
    }

    @Test
    fun testFullMonthLeaveTptaViaSituationalAudit() {
        // TPTA credited = 7200 + 3888 (54% DA) = 11,088.0
        val payslip = createArmyPayslip(tpta = 7200.0, tptaDa = 3888.0)
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.LEAVE_FULL_MONTH),
                customDaPercent = 54.0,
            )

        val result = auditor.auditSituational(payslip, context)

        assertTrue(result.hasHazards)
        val disc = result.discrepancies.firstOrNull { it.id == "HAZARD_LEAVE_TPTA_001" }
        assertNotNull(disc)
        assertEquals(11088.0, disc.drawnAmount, 0.01)
        assertEquals(11088.0, result.totalPrincipalRecovery, 0.01)
        assertEquals(1995.84, result.totalPenalInterest, 0.01)
        assertEquals(13083.84, result.totalRecoveryExposure, 0.01)
    }

    @Test
    fun testFieldCfaaTptaCollisionTriggersRecoveryHazard() {
        val request =
            AllowanceCollisionRequest(
                activeAllowanceCodes =
                    setOf(
                        SituationalTileKeys.POST_FIELD_CFAA,
                        AllowanceCollisionCodes.TPTA_HIGHER_CITY,
                    ),
                daRate = 0.50,
                defaultOverdrawnMonths = 6,
                payLevel = "10",
            )

        val result = auditor.audit(request)

        assertTrue(result.hasHazards, "Field deployment (CFAA) + TPTA must trigger recovery hazard")
        val disc = result.discrepancies.firstOrNull { it.id == "HAZARD_TPTA_FIELD_COLLISION" }
        assertNotNull(disc)
        assertEquals(DiscrepancyType.RECOVERY_HAZARD, disc.type)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        assertEquals("ALLOWANCE_TPTA_003", disc.relevantRuleId)
    }

    @Test
    fun testFieldCmfaaTptaCollisionTriggersRecoveryHazard() {
        val request =
            AllowanceCollisionRequest(
                activeAllowanceCodes =
                    setOf(
                        SituationalTileKeys.POST_FIELD_CMFAA,
                        AllowanceCollisionCodes.TPTA_OTHER,
                    ),
                daRate = 0.50,
                defaultOverdrawnMonths = 3,
                payLevel = "11",
            )

        val result = auditor.audit(request)

        assertTrue(result.hasHazards, "Modified Field (CMFAA) + TPTA must trigger recovery hazard")
        val disc = result.discrepancies.firstOrNull { it.id == "HAZARD_TPTA_FIELD_COLLISION" }
        assertNotNull(disc)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
    }

    @Test
    fun testTwoLocationConcessionFieldPlusSprHraDoesNotTriggerFalseHazard() {
        val request =
            AllowanceCollisionRequest(
                activeAllowanceCodes =
                    setOf(
                        SituationalTileKeys.POST_FIELD_CFAA,
                        SituationalTileKeys.HOUSE_FAMILY_SPR,
                        SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION,
                    ),
                basicPay = 121200.0,
                daRate = 0.54,
            )

        val result = auditor.audit(request)

        assertFalse(result.hasHazards, "Two-Location Concession with Field + SPR HRA is valid statutory entitlement")
        assertEquals(0, result.discrepancies.size)
        assertEquals(0.0, result.totalRecoveryExposure, 0.01)
    }

    @Test
    fun testCadreAmcNpaDoesNotTriggerFalseHazard() {
        val request =
            AllowanceCollisionRequest(
                activeAllowanceCodes =
                    setOf(
                        SituationalTileKeys.CADRE_AMC_NPA,
                        SituationalTileKeys.POST_PEACE_HIGHER,
                        AllowanceCollisionCodes.TPTA_HIGHER_CITY,
                    ),
                basicPay = 121200.0,
                daRate = 0.54,
            )

        val result = auditor.audit(request)

        assertFalse(result.hasHazards, "AMC Non-Practicing Allowance must never trigger false collision hazard")
        assertEquals(0, result.discrepancies.size)
        assertEquals(0.0, result.totalRecoveryExposure, 0.01)
    }

    @Test
    fun testFieldDeploymentWithCashRationMoneyTriggersRecoveryHazard() {
        val payslip = createArmyPayslip(rationMoney = 4200.0)
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_FIELD_CFAA),
            )

        val result = auditor.auditSituational(payslip, context)

        assertTrue(result.hasHazards, "Field deployment with cash RMA must trigger ration recovery hazard")
        val disc = result.discrepancies.firstOrNull { it.id == "HAZARD_FIELD_RATION_COLLISION" }
        assertNotNull(disc)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        assertEquals("ALLOWANCE_RATION_001", disc.relevantRuleId)
        assertTrue(disc.authority.contains("174(B)"))
    }

    @Test
    fun testMandatoryRule14DsopStoppageAlarm() {
        val payslip = createArmyPayslip(dsop = 45000.0)
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_PEACE_HIGHER),
                monthsToRetirement = 2,
            )

        val result = auditor.auditSituational(payslip, context)

        assertTrue(result.hasHazards, "Active DSOP within 3 months of retirement must trigger Rule 14 alarm")
        val disc = result.discrepancies.firstOrNull { it.id == "HAZARD_DSOP_STOPPAGE_001" }
        assertNotNull(disc)
        assertEquals(DiscrepancySeverity.CRITICAL, disc.severity)
        assertEquals("FUNDS_DSOP_002", disc.relevantRuleId)
        assertTrue(disc.authority.contains("Rule 14"))
    }
}
