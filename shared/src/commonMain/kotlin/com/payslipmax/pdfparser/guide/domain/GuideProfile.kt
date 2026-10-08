package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.ServiceTimelineBuilder

/**
 * What the "your figure" line knows about the officer, all read from the latest usable payslip ([year], [month]) and kept
 * on the device. Plain types, so the resolver does not depend on Pay Audit. Every field is null when the payslip does not
 * settle it, and the resolver then shows nothing instead of guessing.
 */
data class GuideProfile(
    val year: Int,
    val month: Int,
    /** Pay level label as printed, for example "12A". */
    val level: String?,
    val basicPay: Double?,
    /** DA as a whole percent of Basic + MSP, as the payslip applied it. */
    val daPercent: Int?,
    /** Transport Allowance class, `HIGHER` or `OTHER`, recovered from the payslip's TPTA base; null at level 14 and above. */
    val tptaClass: String?,
    val hraAmount: Double?,
)

/**
 * Builds the profile through Pay Audit's own [ServiceTimelineBuilder], so level, DA and the TPTA class are read once, by
 * one piece of code, and the Guide can never disagree with Pay Audit. Only the HRA amount, which the timeline does not
 * keep, is read from the same month's payslip.
 */
object GuideProfileBuilder {
    fun from(history: List<ParsedPayslip>): GuideProfile? {
        val latest = ServiceTimelineBuilder.build(history).months.lastOrNull() ?: return null
        val payslip = history.firstOrNull { it.year == latest.month.year && it.monthNum == latest.month.month && !it.needsReview }
        return GuideProfile(
            year = latest.month.year,
            month = latest.month.month,
            level = latest.level?.label,
            basicPay = latest.basicPay,
            daPercent = latest.daPercent,
            tptaClass = latest.tptaCity?.name,
            hraAmount = payslip?.earnings?.houseRentAllowance,
        )
    }
}
