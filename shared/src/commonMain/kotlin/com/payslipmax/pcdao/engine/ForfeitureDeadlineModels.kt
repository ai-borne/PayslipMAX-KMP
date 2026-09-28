package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.AuditDiscrepancy
import kotlinx.serialization.Serializable

@Serializable
enum class MilitaryClaimType(val defaultDeadlineDays: Int, val displayName: String) {
    SPR_DECLARATION(60, "Selected Place of Residence (SPR) Declaration"),
    LTC_CLAIM(30, "Leave Travel Concession (LTC) Claim"),
    TEMPORARY_DUTY(60, "Temporary Duty (TD / PDM) Travel Claim"),
    COMPOSITE_TRANSFER_GRANT(180, "Composite Transfer Grant (CTG) Claim"),
    ;

    companion object {
        fun fromCode(code: String): MilitaryClaimType? =
            when (code.uppercase().trim()) {
                "SPR", "SPR_DECLARATION", "HRA_SPR" -> SPR_DECLARATION
                "LTC", "LTC_CLAIM" -> LTC_CLAIM
                "TD", "TEMPORARY_DUTY", "TD_TOUR", "TEMPORARY_DUTY_NO_ADVANCE" -> TEMPORARY_DUTY
                "CTG", "COMPOSITE_TRANSFER_GRANT", "TRANSFER_TA" -> COMPOSITE_TRANSFER_GRANT
                else -> null
            }
    }
}

@Serializable
enum class DeadlineStatus {
    ON_TRACK,
    EXPIRING_SOON,
    TIME_BARRED_FORFEITED,
}

@Serializable
data class MilitaryClaimItem(
    val claimId: String,
    val claimType: MilitaryClaimType,
    val eventDate: String? = null,
    val daysElapsed: Int? = null,
    val hasAdvanceBeenDrawn: Boolean = false,
    val advanceAmount: Double = 0.0,
    val estimatedClaimAmount: Double = 0.0,
    val remarks: String? = null,
)

@Serializable
data class ForfeitureAuditRequest(
    val claims: List<MilitaryClaimItem> = emptyList(),
    val asOfDate: String? = null,
)

@Serializable
data class ForfeitureClaimResult(
    val claimId: String,
    val claimType: MilitaryClaimType,
    val deadlineDays: Int,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val status: DeadlineStatus,
    val condonationRequired: Boolean,
    val condonationInstructions: String? = null,
    val discrepancy: AuditDiscrepancy? = null,
)

@Serializable
data class ForfeitureDeadlineResult(
    val claimResults: List<ForfeitureClaimResult>,
    val discrepancies: List<AuditDiscrepancy>,
    val timeBarredCount: Int,
    val expiringSoonCount: Int,
    val onTrackCount: Int,
    val totalForfeitureExposure: Double,
) {
    val hasUrgentDeadlines: Boolean get() = timeBarredCount > 0 || expiringSoonCount > 0
}
