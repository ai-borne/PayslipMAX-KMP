package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Local conveyance on temporary duty, capped per day. Pay levels 9 to 11 have a rupee cap (the `td_taxi` figure raised by the DA
 * step, Rs 422.50 at DA 50-99%); levels 12 and above have a rule in words only, which is returned as a [LimitNote] with no amount
 * (register 23, owner approved 2026-10-11), so a figure the rules do not give is never invented.
 */
object TdTaxiCalculator {
    const val FIGURE_ID = "td_taxi"

    fun estimate(
        figures: GuideFigures?,
        profile: PreClaimProfile,
        days: Int,
    ): PreClaimOutcome {
        PreClaimChecks.daysIssue(days)?.let { return Unavailable(PreClaimElement.LOCAL_TAXI, it) }
        val rate =
            when (val found = DaStepRate.of(figures, FIGURE_ID, profile, allowLimitBand = true)) {
                is DaStepRate.Missing -> return Unavailable(PreClaimElement.LOCAL_TAXI, found.issue)
                is DaStepRate.Found -> found
            }
        if (rate.band.base <= 0) return LimitNote(PreClaimElement.LOCAL_TAXI, rate.band.limit, rate.source)
        val rupees = PreClaimChecks.rupees(rate.ratePaise * days)
        return PerDayEstimate(PreClaimElement.LOCAL_TAXI, rupees, rate.ratePaise, days, PreClaimChecks.FULL_SHARE_PERCENT, rate.source)
    }
}
