package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType

class AllowanceCollisionAuditor {
    fun audit(request: AllowanceCollisionRequest): AllowanceCollisionResult {
        val checkers =
            listOf(
                ::checkTptaFieldCollision,
                ::checkSdaTlaCollision,
                ::checkHraAccommodationCollision,
                ::checkFlyingSpecialForcesCollision,
                ::checkRhFieldCollision,
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

    private fun checkTptaFieldCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val hasTpta = codes.any { it in TPTA_CODES }
        val hasField = codes.any { it in FIELD_CODES }
        if (!hasTpta || !hasField) return null

        val isHigher = codes.contains(AllowanceCollisionCodes.TPTA_HIGHER_CITY) || codes.contains(AllowanceCollisionCodes.TPTA_PEACE)
        val customClaim = request.claims.firstOrNull { it.code in TPTA_CODES }
        val monthlyAmount = customClaim?.monthlyAmount?.takeIf { it > 0 } ?: resolveTptaMonthly(request.payLevel, isHigher, request.daRate)
        val months = customClaim?.monthsDrawn?.takeIf { it > 0 } ?: request.defaultOverdrawnMonths

        val principal = monthlyAmount * months
        val penalInterest = principal * PENAL_INTEREST_RATE
        val totalExposure = principal + penalInterest

        return AuditDiscrepancy(
            id = "HAZARD_TPTA_FIELD_COLLISION",
            title = "Transport Allowance (TPTA) Drawn Concurrently with Field Deployment",
            type = DiscrepancyType.RECOVERY_HAZARD,
            severity = DiscrepancySeverity.CRITICAL,
            monthlyImpact = monthlyAmount,
            annualImpact = monthlyAmount * 12,
            drawnAmount = principal,
            entitledAmount = 0.0,
            netDue = -totalExposure,
            authority = "GoI MoD letter No. 12630/Tpt.A/Mov C/246/D(Mov)/17 dated 15 Sept 2017; TR-230(B)",
            explanation = "Government conveyance is provided in field deployments. Concurrent TPTA credit triggers 18% penal recovery.",
            recommendedAction = "Cease TPTA drawing via Unit Part II Order casualty publication to arrest compounding penal interest debit.",
            relevantRuleId = "ALLOWANCE_TPTA_003",
        )
    }

    private fun checkSdaTlaCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        if (!codes.contains(AllowanceCollisionCodes.SDA) || !codes.contains(AllowanceCollisionCodes.TLA)) return null

        val sdaMonthly = if (request.basicPay > 0) request.basicPay * 0.10 else 10000.0
        val tlaMonthly = 10500.0 // Canonical Category I TLA benchmark
        val lowerMonthly = minOf(sdaMonthly, tlaMonthly)
        val months = request.defaultOverdrawnMonths
        val principal = lowerMonthly * months
        val penalInterest = principal * PENAL_INTEREST_RATE
        val totalExposure = principal + penalInterest

        return AuditDiscrepancy(
            id = "HAZARD_SDA_TLA_COLLISION",
            title = "Special Duty Allowance (SDA) & Tough Location Allowance (TLA) Mutual Exclusion",
            type = DiscrepancyType.RECOVERY_HAZARD,
            severity = DiscrepancySeverity.WARNING,
            monthlyImpact = lowerMonthly,
            annualImpact = lowerMonthly * 12,
            drawnAmount = principal,
            entitledAmount = 0.0,
            netDue = -totalExposure,
            authority = "Special Compensatory Allowances Regulations, MoD 2017; Rule SDA_MUTUAL_EXCLUSION",
            explanation = "Under 7th CPC rules, officer must elect the higher of SDA or TLA. Drawing both triggers recovery of the lesser claim.",
            recommendedAction = "Exercise written option for the higher allowance; refund/adjust concurrent credit with PCDA(O).",
            relevantRuleId = "ALLOWANCE_SDA_002",
        )
    }

    private fun checkHraAccommodationCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val hasHra = codes.contains(AllowanceCollisionCodes.HRA_CLAIMED) || codes.contains(AllowanceCollisionCodes.HRA_PEACE)
        val hasAccom = codes.contains(AllowanceCollisionCodes.GOVT_ACCOMM_ALLOTTED) || codes.contains(AllowanceCollisionCodes.MARRIED_QUARTERS)
        if (!hasHra || !hasAccom) return null

        val hraMonthly = if (request.basicPay > 0) request.basicPay * 0.20 else 16000.0
        val months = request.defaultOverdrawnMonths
        val principal = hraMonthly * months
        val penalInterest = principal * PENAL_INTEREST_RATE
        val totalExposure = principal + penalInterest

        return AuditDiscrepancy(
            id = "HAZARD_HRA_ACCOMM_COLLISION",
            title = "HRA Claimed Concurrently with Allotted Govt Married Accommodation",
            type = DiscrepancyType.RECOVERY_HAZARD,
            severity = DiscrepancySeverity.CRITICAL,
            monthlyImpact = hraMonthly,
            annualImpact = hraMonthly * 12,
            drawnAmount = principal,
            entitledAmount = 0.0,
            netDue = -totalExposure,
            authority = "MoD Letter No. 1(25)/2017/D(Pay/Services); All India Service Rules, HRA Chapter 10",
            explanation = "Claiming HRA while occupying Govt accommodation is an illegal concurrent claim subject to full recovery + 18% penal rate.",
            recommendedAction = "Publish Part II Order cancelling HRA from accommodation allotment date; remit overpayment to PCDA(O).",
            relevantRuleId = "HRA_ACCOMM_001",
        )
    }

    private fun checkFlyingSpecialForcesCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val hasFlying = codes.contains(AllowanceCollisionCodes.FLYING_PAY)
        val hasSf = codes.contains(AllowanceCollisionCodes.SPECIAL_FORCES_PAY)
        if (!hasFlying || !hasSf) return null

        val rateBase = if (request.daRate >= 0.50) 31250.0 else 25000.0
        val months = request.defaultOverdrawnMonths
        val principal = rateBase * months
        val penalInterest = principal * PENAL_INTEREST_RATE
        val totalExposure = principal + penalInterest

        return AuditDiscrepancy(
            id = "HAZARD_FLYING_SF_COLLISION",
            title = "Flying Allowance & Special Forces Allowance Mutual Exclusion",
            type = DiscrepancyType.RECOVERY_HAZARD,
            severity = DiscrepancySeverity.CRITICAL,
            monthlyImpact = rateBase,
            annualImpact = rateBase * 12,
            drawnAmount = principal,
            entitledAmount = 0.0,
            netDue = -totalExposure,
            authority = "MoD letter No. 1(16)/2017/D(Pay/Services) & B/36389/AG/PS3(b)/82/S/D(Pay/Services) dated 18 Sept 2017",
            explanation = "Flying Allowance and Special Forces Allowance are mutually exclusive operational allowances.",
            recommendedAction = "Retain authorized qualification allowance; submit casualty order adjusting duplicate operational claim.",
            relevantRuleId = "SPECIAL_ALLOWANCE_MUTUAL_EXCLUSION_001",
        )
    }

    private fun checkRhFieldCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val hasRh = codes.contains(AllowanceCollisionCodes.RH_MATRIX_ALLOWANCE)
        val hasField = codes.any { it in FIELD_CODES }
        if (!hasRh || !hasField) return null

        val estimatedMonthly = 10500.0
        val principal = estimatedMonthly * request.defaultOverdrawnMonths
        val penalInterest = principal * PENAL_INTEREST_RATE
        val totalExposure = principal + penalInterest

        return AuditDiscrepancy(
            id = "HAZARD_RH_FIELD_COLLISION",
            title = "Risk & Hardship Matrix Allowance Concurrent with Field Allowance at Same Station",
            type = DiscrepancyType.RECOVERY_HAZARD,
            severity = DiscrepancySeverity.CRITICAL,
            monthlyImpact = estimatedMonthly,
            annualImpact = estimatedMonthly * 12,
            drawnAmount = principal,
            entitledAmount = 0.0,
            netDue = -totalExposure,
            authority = "Pay and Allowances Handbook 2023, p. 131; Rule RH_CONCURRENT_001",
            explanation = "R&H allowance cannot be drawn concurrently with CFAA/CMFAA/HAFA at the same station.",
            recommendedAction = "Review unit deployment order; adjust duplicate field claim with PCDA(O).",
            relevantRuleId = "RH_CONCURRENT_001",
        )
    }

    private fun resolveTptaMonthly(
        level: String,
        isHigherCity: Boolean,
        daRate: Double,
    ): Double {
        val baseRate =
            when {
                isMajorGeneralOrAbove(level) -> if (isHigherCity) 15750.0 else 7200.0
                else -> if (isHigherCity) 7200.0 else 3600.0
            }
        return baseRate * (1.0 + daRate)
    }

    private fun isMajorGeneralOrAbove(level: String): Boolean {
        val num = level.filter { it.isDigit() }.toIntOrNull() ?: 10
        return num >= 14
    }

    companion object {
        const val PENAL_INTEREST_RATE = 0.18
        private val TPTA_CODES =
            setOf(
                AllowanceCollisionCodes.TPTA_PEACE,
                AllowanceCollisionCodes.TPTA_HIGHER_CITY,
                AllowanceCollisionCodes.TPTA_OTHER,
            )
        private val FIELD_CODES =
            setOf(
                AllowanceCollisionCodes.HAFAA,
                AllowanceCollisionCodes.CFAA,
                AllowanceCollisionCodes.CMFAA,
                AllowanceCollisionCodes.SIACHEN,
            )
    }
}
