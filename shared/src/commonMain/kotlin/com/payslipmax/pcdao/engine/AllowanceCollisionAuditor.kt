package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pdfparser.domain.ParsedPayslip

class AllowanceCollisionAuditor {
    fun audit(request: AllowanceCollisionRequest): AllowanceCollisionResult {
        val checkers =
            listOf(
                MilitaryCollisionCheckers::checkLeaveFullMonthTpta,
                MilitaryCollisionCheckers::checkTptaFieldCollision,
                MilitaryCollisionCheckers::checkFieldRationCollision,
                MilitaryCollisionCheckers::checkSdaTlaCollision,
                MilitaryCollisionCheckers::checkHraAccommodationCollision,
                MilitaryCollisionCheckers::checkFlyingSpecialForcesCollision,
                MilitaryCollisionCheckers::checkRhFieldCollision,
                MilitaryCollisionCheckers::checkDsopRetirementStoppage,
            )

        val discrepancies = checkers.mapNotNull { it(request) }
        val principalTotal = discrepancies.sumOf { it.drawnAmount }
        val penalTotal = discrepancies.sumOf { (-it.netDue) - it.drawnAmount }.coerceAtLeast(0.0)
        val exposureTotal = discrepancies.sumOf { -it.netDue }.coerceAtLeast(0.0)

        return AllowanceCollisionResult(
            discrepancies = discrepancies,
            totalPrincipalRecovery = principalTotal,
            totalPenalInterest = penalTotal,
            totalRecoveryExposure = exposureTotal,
            criticalHazardsCount = discrepancies.count { it.severity == DiscrepancySeverity.CRITICAL },
            warningHazardsCount = discrepancies.count { it.severity == DiscrepancySeverity.WARNING },
        )
    }

    fun auditClaims(
        activeAllowances: Collection<String>,
        basicPay: Double = 0.0,
        daRate: Double = 0.50,
        monthsOverdrawn: Int = 6,
        payLevel: String = "10",
    ): List<AuditDiscrepancy> {
        val req =
            AllowanceCollisionRequest(
                activeAllowanceCodes = activeAllowances.toSet(),
                basicPay = basicPay,
                daRate = daRate,
                defaultOverdrawnMonths = monthsOverdrawn,
                payLevel = payLevel,
            )
        return audit(req).discrepancies
    }

    fun auditSituational(
        payslip: ParsedPayslip,
        context: ActiveSituationalContext,
    ): AllowanceCollisionResult {
        val codes = mutableSetOf<String>()
        codes.addAll(context.activeTileIds)

        mapTileCodes(context.activeTileIds, codes)

        val claims = mutableListOf<AllowanceClaim>()
        val tptaTotal = payslip.earnings.transportAllowance + payslip.earnings.transportAllowanceDa
        if (tptaTotal > 0.0) {
            val tptaCode =
                if (codes.contains(AllowanceCollisionCodes.TPTA_OTHER)) {
                    AllowanceCollisionCodes.TPTA_OTHER
                } else {
                    AllowanceCollisionCodes.TPTA_HIGHER_CITY
                }
            codes.add(tptaCode)
            claims.add(AllowanceClaim(code = tptaCode, monthlyAmount = tptaTotal, monthsDrawn = 1))
        }

        if (payslip.earnings.rationMoney > 0.0) {
            codes.add(AllowanceCollisionCodes.RATION_MONEY_ALLOWANCE)
            claims.add(AllowanceClaim(code = AllowanceCollisionCodes.RATION_MONEY_ALLOWANCE, monthlyAmount = payslip.earnings.rationMoney, monthsDrawn = 1))
        }

        if (payslip.deductions.dsopSubscription > 0.0) {
            codes.add(AllowanceCollisionCodes.DSOP_SUBSCRIPTION)
        }

        val daRate = (context.customDaPercent ?: context.inferredFlags.inferredDaPercent) / 100.0
        val monthsToRetire = context.monthsToRetirement ?: if (codes.contains(SituationalTileKeys.RETIRE_NEAR)) 2 else null

        val req =
            AllowanceCollisionRequest(
                claims = claims,
                activeAllowanceCodes = codes,
                basicPay = payslip.earnings.basicPay,
                daRate = if (daRate > 0) daRate else 0.50,
                defaultOverdrawnMonths = 6,
                payLevel = context.inferredFlags.inferredRankLevel ?: "10",
                monthsToRetirement = monthsToRetire,
                dsopMonthly = payslip.deductions.dsopSubscription,
                rationMoneyMonthly = payslip.earnings.rationMoney,
                tptaMonthly = tptaTotal,
            )

        return audit(req)
    }

    private fun mapTileCodes(
        tiles: Set<String>,
        codes: MutableSet<String>,
    ) {
        if (tiles.contains(SituationalTileKeys.POST_PEACE_HIGHER)) codes.add(AllowanceCollisionCodes.TPTA_HIGHER_CITY)
        if (tiles.contains(SituationalTileKeys.POST_PEACE_OTHER)) codes.add(AllowanceCollisionCodes.TPTA_OTHER)
        if (tiles.contains(SituationalTileKeys.POST_FIELD_HAFAA)) codes.add(AllowanceCollisionCodes.HAFAA)
        if (tiles.contains(SituationalTileKeys.POST_FIELD_CFAA)) codes.add(AllowanceCollisionCodes.CFAA)
        if (tiles.contains(SituationalTileKeys.POST_FIELD_CMFAA)) codes.add(AllowanceCollisionCodes.CMFAA)
        if (tiles.contains(SituationalTileKeys.POST_SIACHEN)) codes.add(AllowanceCollisionCodes.SIACHEN)
        if (tiles.contains(SituationalTileKeys.HOUSE_GOVT_MQ)) codes.add(AllowanceCollisionCodes.GOVT_ACCOMM_ALLOTTED)
        if (tiles.contains(SituationalTileKeys.HOUSE_FAMILY_SPR)) codes.add(AllowanceCollisionCodes.HRA_CLAIMED)
        if (tiles.contains(SituationalTileKeys.POST_SDA_NE)) codes.add(AllowanceCollisionCodes.SDA)
        if (tiles.contains(SituationalTileKeys.LEAVE_FULL_MONTH)) codes.add(AllowanceCollisionCodes.LEAVE_FULL_MONTH)
        if (tiles.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION)) codes.add(AllowanceCollisionCodes.TWO_LOCATION_CONCESSION)
        if (tiles.contains(SituationalTileKeys.CADRE_AMC_NPA)) codes.add(AllowanceCollisionCodes.CADRE_AMC_NPA)
    }

    companion object {
        const val PENAL_INTEREST_RATE = MilitaryCollisionCheckers.PENAL_INTEREST_RATE
    }
}
