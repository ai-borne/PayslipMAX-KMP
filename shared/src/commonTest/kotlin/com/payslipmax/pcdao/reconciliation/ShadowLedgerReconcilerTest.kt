package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShadowLedgerReconcilerTest {
    private val reconciler = ShadowLedgerReconciler()

    private fun createColRathorePayslip(): ParsedPayslip {
        return ParsedPayslip(
            file = "col_rathore_march_2026.pdf",
            year = 2026,
            monthNum = 3,
            monthName = "March",
            dateStr = "2026-03-31",
            officer = Officer(name = "Col R. S. Rathore", accountNo = "01/142/987654", pan = "ABCDE1234F"),
            earnings =
                Earnings(
                    basicPay = 149000.0,
                    dearnessAllowance = 89400.0,
                    militaryServicePay = 15500.0,
                    transportAllowance = 3600.0,
                    transportAllowanceDa = 2160.0,
                    houseRentAllowance = 0.0,
                    childrenEducationAllowance = 0.0,
                    fieldAllowance = 0.0,
                    riskHardshipAllowance = 0.0,
                ),
            deductions =
                Deductions(
                    dsopSubscription = 40000.0,
                    licenseFee = 0.0,
                ),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 259660.0, totalDeductions = 40000.0, netRemittance = 219660.0),
            taxAndSavings = null,
        )
    }

    @Test
    fun testReconcileColRathoreUnderpayments() {
        val payslip = createColRathorePayslip()
        val context =
            ActiveSituationalContext(
                activeTileIds =
                    setOf(
                        SituationalTileKeys.POST_PEACE_HIGHER,
                        SituationalTileKeys.HOUSE_FAMILY_SPR,
                        SituationalTileKeys.CEA_TWO_CHILDREN,
                        SituationalTileKeys.AVAILED_LTC,
                    ),
                numberOfChildrenCea = 2,
                sprCityTier = "Y",
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(payslip, context)

        // 1. TPTA underpayment: Entitled ₹11,520 vs Credited ₹5,760 = ₹5,760/mo -> ₹69,120/yr
        val tptaDisc = result.discrepancies.firstOrNull { it.id == "DISC_UNDERPAY_TPTA" }
        assertTrue(tptaDisc != null, "TPTA discrepancy should be present")
        assertEquals(DiscrepancyType.UNDERPAYMENT, tptaDisc.type)
        assertEquals(69120.0, tptaDisc.annualImpact)

        // 2. SPR HRA underpayment: Entitled 20% of 149000 = ₹29,800/mo -> ₹3,57,600/yr
        val hraDisc = result.discrepancies.firstOrNull { it.id == "DISC_UNDERPAY_HRA_SPR" }
        assertTrue(hraDisc != null, "SPR HRA discrepancy should be present")
        assertEquals(357600.0, hraDisc.annualImpact)

        // 3. CEA for 2 children at 60% DA: ₹33,750 * 2 = ₹67,500/yr
        val ceaDisc = result.discrepancies.firstOrNull { it.id == "DISC_UNDERPAY_CEA" }
        assertTrue(ceaDisc != null, "CEA discrepancy should be present")
        assertEquals(67500.0, ceaDisc.annualImpact)

        // 4. LTC 10 days encashment: (149000 + 15500) * 1.6 / 30 * 10 = ₹87,733.33
        val ltcDisc = result.discrepancies.firstOrNull { it.id == "DISC_UNDERPAY_LTC_ENCASHMENT" }
        assertTrue(ltcDisc != null, "LTC discrepancy should be present")
        assertEquals(87733.33, ltcDisc.annualImpact, 0.05)

        // Total Unclaimed
        assertTrue(result.totalUnclaimedAnnual > 580000.0)
        assertEquals(0.0, result.totalRecoveryHazard)
    }

    @Test
    fun testReconcileTptaFieldAreaAdvisoryAlarm() {
        val payslip = createColRathorePayslip()
        val context =
            ActiveSituationalContext(
                activeTileIds =
                    setOf(
                        SituationalTileKeys.POST_PEACE_HIGHER,
                        SituationalTileKeys.POST_FIELD_HAFAA,
                    ),
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(payslip, context)
        val alarm = result.discrepancies.firstOrNull { it.type == DiscrepancyType.FORFEITURE_RISK }
        assertTrue(alarm != null, "Advisory alarm should be present for TPTA + Field Area")
        assertEquals(DiscrepancySeverity.WARNING, alarm.severity)
        assertEquals(0.0, alarm.netDue, 0.01)
        // TPTA in field area must NOT inflate the recovery hazard counter
        assertEquals(0.0, result.totalRecoveryHazard, 0.01)
    }

    @Test
    fun testReconcileHighDsopAndPreRetirementStoppage() {
        // Annual 6,00,000 -> 1,00,000 excess
        val payslip =
            createColRathorePayslip().copy(
                deductions = Deductions(dsopSubscription = 50000.0),
            )
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.DSOP_HIGH_PACING, SituationalTileKeys.RETIRE_NEAR),
                monthsToRetirement = 2,
            )

        val result = reconciler.reconcile(payslip, context)

        val taxExposure = result.discrepancies.firstOrNull { it.type == DiscrepancyType.TAX_EXPOSURE }
        assertTrue(taxExposure != null, "Tax exposure should be present")
        assertEquals(2215.20, taxExposure.annualImpact, 0.01)

        val stoppage = result.discrepancies.firstOrNull { it.id == "DISC_DSOP_RETIREMENT_STOPPAGE" }
        assertTrue(stoppage != null, "Stoppage alarm should be present")
        assertEquals(DiscrepancySeverity.CRITICAL, stoppage.severity)
        assertTrue(result.criticalAlarmCount >= 1)
    }

    @Test
    fun testReconcilePartIiDo2RejectionAlert() {
        val payslip = createColRathorePayslip()
        val context =
            ActiveSituationalContext(
                rejectedDo2Number = "381",
                rejectedDo2Date = "2026-03-28",
            )

        val result = reconciler.reconcile(payslip, context)
        val rejection = result.discrepancies.firstOrNull { it.id == "DISC_DO2_REJECTION" }
        assertTrue(rejection != null, "DO2 rejection alert should be present")
        assertEquals(DiscrepancySeverity.CRITICAL, rejection.severity)
        assertEquals("Rejected DO2 No. 381", rejection.title)
    }

    @Test
    fun testReconcileCleanPayslipReturnsZeroGaps() {
        // Fully entitled ₹11,520
        val payslip =
            createColRathorePayslip().copy(
                earnings =
                    Earnings(
                        basicPay = 149000.0,
                        dearnessAllowance = 89400.0,
                        militaryServicePay = 15500.0,
                        transportAllowance = 7200.0,
                        transportAllowanceDa = 4320.0,
                    ),
            )
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_PEACE_HIGHER),
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(payslip, context)
        assertEquals(0.0, result.totalUnclaimedAnnual)
        assertEquals(0.0, result.totalRecoveryHazard)
        assertEquals(0, result.criticalAlarmCount)
    }

    @Test
    fun testReconcileHafaaCreditedShowsZeroDiscrepancy() {
        val payslip =
            createColRathorePayslip().copy(
                earnings =
                    Earnings(
                        basicPay = 149000.0,
                        dearnessAllowance = 89400.0,
                        militaryServicePay = 15500.0,
                        riskHardshipAllowance = 21125.0,
                    ),
            )
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_FIELD_HAFAA),
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(payslip, context)
        val hafaaDisc = result.discrepancies.firstOrNull { it.id == "DISC_UNDERPAY_HAFAA" }
        assertTrue(hafaaDisc == null, "When HAFAA is credited at entitled rate (21125), no discrepancy should be generated")
        assertEquals(0.0, result.totalUnclaimedAnnual)
    }

    @Test
    fun testReconcileHafaaUnderpaidShowsExactArrearsDifference() {
        val payslip =
            createColRathorePayslip().copy(
                earnings =
                    Earnings(
                        basicPay = 149000.0,
                        dearnessAllowance = 89400.0,
                        militaryServicePay = 15500.0,
                        riskHardshipAllowance = 16900.0,
                    ),
            )
        val context =
            ActiveSituationalContext(
                activeTileIds = setOf(SituationalTileKeys.POST_FIELD_HAFAA),
                customDaPercent = 60.0,
            )

        val result = reconciler.reconcile(payslip, context)
        val hafaaDisc = result.discrepancies.firstOrNull { it.id == "DISC_UNDERPAY_HAFAA" }
        assertTrue(hafaaDisc != null, "HAFAA underpaid discrepancy should be flagged when credited 16900 vs entitled 21125")
        assertEquals(21125.0, hafaaDisc.entitledAmount)
        assertEquals(16900.0, hafaaDisc.drawnAmount)
        assertEquals(4225.0, hafaaDisc.monthlyImpact)
        assertEquals(50700.0, hafaaDisc.annualImpact)
    }
}
