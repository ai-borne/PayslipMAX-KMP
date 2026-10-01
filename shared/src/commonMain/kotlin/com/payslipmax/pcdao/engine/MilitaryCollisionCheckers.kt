package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys

internal object MilitaryCollisionCheckers {
    const val PENAL_INTEREST_RATE = 0.18
    const val ID_ALARM_TPTA_FIELD = AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE
    const val ID_HAZARD_TPTA_FIELD_LEGACY = AllowanceCollisionCodes.HAZARD_TPTA_FIELD_COLLISION
    private const val DEFAULT_RMA_MONTHLY = 4200.0

    fun isTptaFieldDiscrepancy(id: String): Boolean =
        id == ID_ALARM_TPTA_FIELD || id == ID_HAZARD_TPTA_FIELD_LEGACY

    private val TPTA_CODES =
        setOf(
            AllowanceCollisionCodes.TPTA_PEACE,
            AllowanceCollisionCodes.TPTA_HIGHER_CITY,
            AllowanceCollisionCodes.TPTA_OTHER,
            SituationalTileKeys.POST_PEACE_HIGHER,
            SituationalTileKeys.POST_PEACE_OTHER,
            "TPTA",
        )

    private val FIELD_CODES =
        setOf(
            AllowanceCollisionCodes.HAFAA, AllowanceCollisionCodes.CFAA,
            AllowanceCollisionCodes.CMFAA, AllowanceCollisionCodes.SIACHEN,
            AllowanceCollisionCodes.HIGH_ALTITUDE, SituationalTileKeys.POST_FIELD_HAFAA,
            SituationalTileKeys.POST_FIELD_CFAA, SituationalTileKeys.POST_FIELD_CMFAA,
            SituationalTileKeys.POST_SIACHEN,
        )

    private val LEAVE_CODES =
        setOf(AllowanceCollisionCodes.LEAVE_FULL_MONTH, SituationalTileKeys.LEAVE_FULL_MONTH, "LEAVE_FULL_MONTH", "leave_full_month")

    private val RATION_CODES =
        setOf(AllowanceCollisionCodes.RATION_MONEY_ALLOWANCE, "RMA", "RATION_MONEY", "ration_money")

    private val TLC_CODES =
        setOf(AllowanceCollisionCodes.TWO_LOCATION_CONCESSION, SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION, "TLC")

    private val DSOP_CODES =
        setOf(AllowanceCollisionCodes.DSOP_SUBSCRIPTION, SituationalTileKeys.DSOP_HIGH_PACING, "DSOP")

    private val RETIRE_NEAR_CODES =
        setOf(AllowanceCollisionCodes.RETIREMENT_WITHIN_3_MONTHS, SituationalTileKeys.RETIRE_NEAR, "RETIRE_NEAR")

    fun checkLeaveFullMonthTpta(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val hasLeave = codes.any { it in LEAVE_CODES }
        val hasTpta = codes.any { it in TPTA_CODES } || request.tptaMonthly > 0 || request.claims.any { it.code in TPTA_CODES }
        if (!hasLeave || !hasTpta) return null

        val isHigher = isHigherCity(codes)
        val customClaim = request.claims.firstOrNull { it.code in TPTA_CODES }
        val monthly =
            request.tptaMonthly.takeIf { it > 0 }
                ?: customClaim?.monthlyAmount?.takeIf { it > 0 }
                ?: resolveTptaMonthly(request.payLevel, isHigher, request.daRate)
        val months = customClaim?.monthsDrawn?.takeIf { it > 0 } ?: 1

        return buildHazard(
            id = "HAZARD_LEAVE_TPTA_001",
            title = "Transport Allowance (TPTA) Drawn During Full Calendar Month Leave",
            severity = DiscrepancySeverity.CRITICAL,
            monthly = monthly,
            months = months,
            authority = "Travel Regulations Rule 230(B); GoI MoD letter No. 1(25)/2017/D(Pay/Services)",
            explanation = "Under Travel Regulations Rule 230(B), Transport Allowance is strictly inadmissible for any calendar month during which the officer is entirely absent on leave. Concurrent drawing of TPTA triggers principal recovery plus 18% penal interest.",
            action = "Publish Part II Order adjusting leave absence and remit overdrawn TPTA to PCDA(O) Pune.",
            ruleId = "ALLOWANCE_TPTA_LEAVE_001",
        )
    }

    fun checkTptaFieldCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val hasTpta = codes.any { it in TPTA_CODES } || request.tptaMonthly > 0 || request.claims.any { it.code in TPTA_CODES }
        val hasField = codes.any { it in FIELD_CODES }
        if (!hasTpta || !hasField) return null

        val isHigher = isHigherCity(codes)
        val customClaim = request.claims.firstOrNull { it.code in TPTA_CODES }
        val monthly = customClaim?.monthlyAmount?.takeIf { it > 0 } ?: resolveTptaMonthly(request.payLevel, isHigher, request.daRate)
        val months = customClaim?.monthsDrawn?.takeIf { it > 0 } ?: request.defaultOverdrawnMonths
        val principal = monthly * months

        return AuditDiscrepancy(
            id = ID_ALARM_TPTA_FIELD,
            title = "Transport Allowance (TPTA) in Field Area — Audit Advisory",
            type = DiscrepancyType.FORFEITURE_RISK,
            severity = DiscrepancySeverity.WARNING,
            monthlyImpact = monthly,
            annualImpact = monthly * 12.0,
            drawnAmount = principal,
            entitledAmount = principal,
            netDue = 0.0,
            authority = "Travel Regulations Rule 230(B) (TR-230(B)); GoI MoD letter No. 12630/Tpt.A/Mov C/246/D(Mov)/17 dated 15 Sept 2017",
            explanation = "Under TR-230(B) and MoD letter dated 15 Sept 2017, TPTA is inadmissible in field deployments only when Government conveyance is provided. Officers without Govt conveyance may draw TPTA provided Non-Availability Certificate (NAC) / Part II Order casualty is on record with PCDA(O) Pune.",
            recommendedAction = "Verify if Government conveyance was allotted during deployment. If not allotted, ensure Unit Part II Order casualty with Non-Availability Certificate (NAC) is submitted to PCDA(O) to prevent retrospective recovery objection.",
            relevantRuleId = "ALLOWANCE_TPTA_003",
        )
    }

    fun checkFieldRationCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val hasField = codes.any { it in FIELD_CODES }
        val hasRation = codes.any { it in RATION_CODES } || request.rationMoneyMonthly > 0 || request.claims.any { it.code in RATION_CODES }
        if (!hasField || !hasRation) return null

        val customClaim = request.claims.firstOrNull { it.code in RATION_CODES }
        val monthly = request.rationMoneyMonthly.takeIf { it > 0 } ?: customClaim?.monthlyAmount?.takeIf { it > 0 } ?: DEFAULT_RMA_MONTHLY
        val months = customClaim?.monthsDrawn?.takeIf { it > 0 } ?: request.defaultOverdrawnMonths

        return buildHazard(
            id = "HAZARD_FIELD_RATION_COLLISION",
            title = "Free Field Ration Concurrently Drawn with Cash Ration Money Allowance (RMA)",
            severity = DiscrepancySeverity.CRITICAL,
            monthly = monthly,
            months = months,
            authority = "Rule 174(B) Defence Services Regulations (DSR) Pay & Allowances; MoD Ration Instructions",
            explanation = "Free rations in kind are provided in field/operational deployment. Concurrent drawing of cash RMA is prohibited under Rule 174(B) DSR Pay and triggers full recovery.",
            action = "Cease cash RMA credit via Part II Order casualty; remit overdrawn ration allowance to PCDA(O) Pune.",
            ruleId = "ALLOWANCE_RATION_001",
        )
    }

    fun checkSdaTlaCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val hasSda = codes.contains(AllowanceCollisionCodes.SDA) || codes.contains(SituationalTileKeys.POST_SDA_NE)
        val hasTla = codes.contains(AllowanceCollisionCodes.TLA)
        if (!hasSda || !hasTla) return null

        val sdaMonthly = if (request.basicPay > 0) request.basicPay * 0.10 else 10000.0
        val tlaMonthly = 10500.0
        val lowerMonthly = minOf(sdaMonthly, tlaMonthly)

        return buildHazard(
            id = "HAZARD_SDA_TLA_COLLISION",
            title = "Special Duty Allowance (SDA) & Tough Location Allowance (TLA) Mutual Exclusion",
            severity = DiscrepancySeverity.WARNING,
            monthly = lowerMonthly,
            months = request.defaultOverdrawnMonths,
            authority = "Special Compensatory Allowances Regulations, MoD 2017; Rule SDA_MUTUAL_EXCLUSION",
            explanation = "Under 7th CPC rules, officer must elect the higher of SDA or TLA. Drawing both triggers recovery of the lesser claim.",
            action = "Exercise written option for the higher allowance; refund/adjust concurrent credit with PCDA(O).",
            ruleId = "ALLOWANCE_SDA_002",
        )
    }

    fun checkHraAccommodationCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val isTlc = codes.any { it in TLC_CODES }
        val hasPeaceMq = codes.contains(AllowanceCollisionCodes.MARRIED_QUARTERS) || codes.contains(SituationalTileKeys.HOUSE_GOVT_MQ)

        if (isTlc && !hasPeaceMq) return null

        val hasHra = codes.contains(AllowanceCollisionCodes.HRA_CLAIMED) || codes.contains(AllowanceCollisionCodes.HRA_PEACE) || codes.contains(SituationalTileKeys.HOUSE_FAMILY_SPR)
        val hasAccom = codes.contains(AllowanceCollisionCodes.GOVT_ACCOMM_ALLOTTED) || hasPeaceMq
        if (!hasHra || !hasAccom) return null

        val monthly = if (request.basicPay > 0) request.basicPay * 0.20 else 16000.0

        return buildHazard(
            id = "HAZARD_HRA_ACCOMM_COLLISION",
            title = "HRA Claimed Concurrently with Allotted Govt Married Accommodation",
            severity = DiscrepancySeverity.CRITICAL,
            monthly = monthly,
            months = request.defaultOverdrawnMonths,
            authority = "MoD Letter No. 1(25)/2017/D(Pay/Services); All India Service Rules, HRA Chapter 10",
            explanation = "Claiming HRA while occupying Govt accommodation is an illegal concurrent claim subject to full recovery + 18% penal rate.",
            action = "Publish Part II Order cancelling HRA from accommodation allotment date; remit overpayment to PCDA(O).",
            ruleId = "HRA_ACCOMM_001",
        )
    }

    fun checkFlyingSpecialForcesCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        if (!codes.contains(AllowanceCollisionCodes.FLYING_PAY) || !codes.contains(AllowanceCollisionCodes.SPECIAL_FORCES_PAY)) return null

        val rateBase = if (request.daRate >= 0.50) 31250.0 else 25000.0

        return buildHazard(
            id = "HAZARD_FLYING_SF_COLLISION",
            title = "Flying Allowance & Special Forces Allowance Mutual Exclusion",
            severity = DiscrepancySeverity.CRITICAL,
            monthly = rateBase,
            months = request.defaultOverdrawnMonths,
            authority = "MoD letter No. 1(16)/2017/D(Pay/Services) & B/36389/AG/PS3(b)/82/S/D(Pay/Services) dated 18 Sept 2017",
            explanation = "Flying Allowance and Special Forces Allowance are mutually exclusive operational allowances.",
            action = "Retain authorized qualification allowance; submit casualty order adjusting duplicate operational claim.",
            ruleId = "SPECIAL_ALLOWANCE_MUTUAL_EXCLUSION_001",
        )
    }

    fun checkRhFieldCollision(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        if (!codes.contains(AllowanceCollisionCodes.RH_MATRIX_ALLOWANCE) || !codes.any { it in FIELD_CODES }) return null

        return buildHazard(
            id = "HAZARD_RH_FIELD_COLLISION",
            title = "Risk & Hardship Matrix Allowance Concurrent with Field Allowance at Same Station",
            severity = DiscrepancySeverity.CRITICAL,
            monthly = 10500.0,
            months = request.defaultOverdrawnMonths,
            authority = "Pay and Allowances Handbook 2023, p. 131; Rule RH_CONCURRENT_001",
            explanation = "R&H allowance cannot be drawn concurrently with CFAA/CMFAA/HAFA at the same station.",
            action = "Review unit deployment order; adjust duplicate field claim with PCDA(O).",
            ruleId = "RH_CONCURRENT_001",
        )
    }

    fun checkDsopRetirementStoppage(request: AllowanceCollisionRequest): AuditDiscrepancy? {
        val codes = request.activeAllowanceCodes
        val monthsToRetire = request.monthsToRetirement ?: if (codes.any { it in RETIRE_NEAR_CODES }) 3 else null
        val dsopMonthly =
            request.dsopMonthly.takeIf { it > 0 }
                ?: request.claims.firstOrNull { it.code in DSOP_CODES }?.monthlyAmount
                ?: if (codes.any { it in DSOP_CODES }) 40000.0 else 0.0

        if (monthsToRetire == null || monthsToRetire > 3 || dsopMonthly <= 0.0) return null

        return AuditDiscrepancy(
            id = "HAZARD_DSOP_STOPPAGE_001",
            title = "Mandatory DSOP Subscription Stoppage (3 Months Prior to Retirement)",
            type = DiscrepancyType.RECOVERY_HAZARD,
            severity = DiscrepancySeverity.CRITICAL,
            monthlyImpact = dsopMonthly,
            annualImpact = dsopMonthly * 3.0,
            drawnAmount = dsopMonthly,
            entitledAmount = 0.0,
            netDue = 0.0,
            authority = "Rule 14 DSOP Fund Rules (1933 / revised 2017); Chapter 19 Pay & Allowances Handbook",
            explanation = "Under Rule 14 of DSOP Fund Rules, deductions must cease 3 months prior to retirement to allow final settlement and prevent pension delays.",
            recommendedAction = "Submit casualty Part II Order stopping DSOP deductions immediately to PCDA(O) Pune.",
            relevantRuleId = "FUNDS_DSOP_002",
        )
    }

    private fun buildHazard(
        id: String,
        title: String,
        severity: DiscrepancySeverity,
        monthly: Double,
        months: Int,
        authority: String,
        explanation: String,
        action: String,
        ruleId: String,
    ): AuditDiscrepancy {
        val principal = monthly * months
        val penalInterest = principal * PENAL_INTEREST_RATE
        val exposure = principal + penalInterest
        return AuditDiscrepancy(
            id = id,
            title = title,
            type = DiscrepancyType.RECOVERY_HAZARD,
            severity = severity,
            monthlyImpact = monthly,
            annualImpact = monthly * 12,
            drawnAmount = principal,
            entitledAmount = 0.0,
            netDue = -exposure,
            authority = authority,
            explanation = explanation,
            recommendedAction = action,
            relevantRuleId = ruleId,
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

    private fun isHigherCity(codes: Set<String>): Boolean =
        codes.contains(AllowanceCollisionCodes.TPTA_HIGHER_CITY) ||
            codes.contains(AllowanceCollisionCodes.TPTA_PEACE) ||
            codes.contains(SituationalTileKeys.POST_PEACE_HIGHER)

    private fun isMajorGeneralOrAbove(level: String): Boolean =
        (level.filter { it.isDigit() }.toIntOrNull() ?: 10) >= 14
}
