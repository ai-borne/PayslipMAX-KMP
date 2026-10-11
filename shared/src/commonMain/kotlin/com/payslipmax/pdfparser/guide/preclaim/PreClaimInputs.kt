package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.domain.GuideProfile
import kotlin.math.roundToLong

/**
 * The officer's values the worksheet works from. Each one defaults from the latest payslip and the officer may change it; with
 * no payslip they are all typed (owner decision 2026-10-11). Whole rupees: pay-matrix cells and allowances are whole numbers.
 * Held only in the worksheet's state, never stored or logged.
 */
data class PreClaimProfile(
    val level: String?,
    val basicPay: Long?,
    val daPercent: Int?,
    val mspAmount: Long?,
    val npaAmount: Long?,
) {
    companion object {
        fun from(profile: GuideProfile?) =
            PreClaimProfile(
                level = profile?.level,
                basicPay = profile?.basicPay?.roundToLong(),
                daPercent = profile?.daPercent,
                mspAmount = profile?.mspAmount?.roundToLong(),
                npaAmount = profile?.npaAmount?.roundToLong(),
            )
    }
}

/** The answers a permanent-duty move needs. A null km or state leaves only the elements that need it unavailable. */
data class PermanentDutyRequest(
    val profile: PreClaimProfile,
    val ctgCase: CtgCase,
    val roadKm: Int?,
    val originState: String?,
)
