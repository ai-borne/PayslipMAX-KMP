package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.AuditDiscrepancy
import kotlinx.serialization.Serializable

@Serializable
data class AllowanceClaim(
    val code: String,
    val name: String = code,
    val monthlyAmount: Double = 0.0,
    val monthsDrawn: Int = 1,
    val station: String? = null,
)

@Serializable
data class AllowanceCollisionRequest(
    val claims: List<AllowanceClaim> = emptyList(),
    val activeAllowanceCodes: Set<String> = claims.map { it.code }.toSet(),
    val basicPay: Double = 0.0,
    val daRate: Double = 0.50,
    val defaultOverdrawnMonths: Int = 6,
    val payLevel: String = "10",
)

@Serializable
data class AllowanceCollisionResult(
    val discrepancies: List<AuditDiscrepancy>,
    val totalPrincipalRecovery: Double,
    val totalPenalInterest: Double,
    val totalRecoveryExposure: Double,
    val criticalHazardsCount: Int,
    val warningHazardsCount: Int,
) {
    val hasHazards: Boolean get() = discrepancies.isNotEmpty()
}

object AllowanceCollisionCodes {
    // Transport Allowance
    const val TPTA_PEACE = "TPTA_PEACE"
    const val TPTA_HIGHER_CITY = "TPTA_HIGHER_CITY"
    const val TPTA_OTHER = "TPTA_OTHER"

    // Field & High Altitude
    const val HAFAA = "HAFAA"
    const val CFAA = "CFAA"
    const val CMFAA = "CMFAA"
    const val SIACHEN = "SIACHEN"
    const val HIGH_ALTITUDE = "HIGH_ALTITUDE"

    // Regional & Tough Location
    const val SDA = "SDA"
    const val TLA = "TLA"

    // Housing & Accommodation
    const val HRA_CLAIMED = "HRA_CLAIMED"
    const val HRA_PEACE = "HRA_PEACE"
    const val GOVT_ACCOMM_ALLOTTED = "GOVT_ACCOMM_ALLOTTED"
    const val MARRIED_QUARTERS = "MARRIED_QUARTERS"

    // Specialized & Operational
    const val FLYING_PAY = "FLYING_PAY"
    const val SPECIAL_FORCES_PAY = "SPECIAL_FORCES_PAY"
    const val RH_MATRIX_ALLOWANCE = "RH_MATRIX_ALLOWANCE"

    // Valid Non-Colliding Additions
    const val GALLANTRY_AWARD = "GALLANTRY_AWARD"
    const val CEA = "CEA"
    const val HOSTEL_SUBSIDY = "HOSTEL_SUBSIDY"
}
