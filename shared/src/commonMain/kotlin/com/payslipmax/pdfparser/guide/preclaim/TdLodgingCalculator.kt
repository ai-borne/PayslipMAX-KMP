package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Lodging on temporary duty: the hotel bill, capped at the ceiling for the pay level (the `td_hotel` figure raised by the DA step).
 * The handbook makes the bill mandatory, so the amount is the nightly bill the officer expects to pay, never the ceiling itself; the
 * ceiling only limits it. The ceiling stays exact in paise (Rs 2812.50 at levels 9 to 11, DA 50-99%), so a half rupee is never
 * rounded up before the cap is applied.
 */
object TdLodgingCalculator {
    const val FIGURE_ID = "td_hotel"

    fun estimate(
        figures: GuideFigures?,
        profile: PreClaimProfile,
        days: Int,
        nights: Int?,
        nightlyRateRupees: Long?,
    ): PreClaimOutcome {
        fun no(issue: PreClaimIssue) = Unavailable(PreClaimElement.LODGING, issue)
        PreClaimChecks.daysIssue(days)?.let { return no(it) }
        val ceiling =
            when (val found = DaStepRate.of(figures, FIGURE_ID, profile)) {
                is DaStepRate.Missing -> return no(found.issue)
                is DaStepRate.Found -> found
            }
        if (nights == null) return no(PreClaimIssue.NIGHTS_MISSING)
        if (nights < 0 || nights > days) return no(PreClaimIssue.NIGHTS_OUT_OF_RANGE)
        if (nightlyRateRupees == null) return no(PreClaimIssue.NIGHTLY_RATE_MISSING)
        if (nightlyRateRupees < 1 || nightlyRateRupees > PreClaimLimits.MAX_NIGHTLY_RATE) return no(PreClaimIssue.NIGHTLY_RATE_OUT_OF_RANGE)
        val billPaise = nightlyRateRupees * PreClaimChecks.RUPEE_IN_PAISE
        val capped = billPaise > ceiling.ratePaise
        val perNightPaise = if (capped) ceiling.ratePaise else billPaise
        return LodgingEstimate(PreClaimChecks.rupees(perNightPaise * nights), nights, nightlyRateRupees, ceiling.ratePaise, capped, ceiling.source)
    }
}
