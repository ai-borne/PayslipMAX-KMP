package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.FigureModes
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Food on temporary duty: the pay level's daily lump sum (the `food_rate` figure, MoD letter 12630/Mov C/242/D(Mov)/2017) raised by
 * the DA step. Owner decision 2026-10-11: every day of a multi-day move is paid in full; the share by time away (30% under six
 * hours, 70% to twelve, 100% over) applies only to a trip that starts and ends the same day, and comes from the `td_food_hours`
 * figure, so this code holds no percent.
 */
object TdFoodCalculator {
    const val FIGURE_ID = "food_rate"
    const val SHARE_FIGURE_ID = "td_food_hours"

    fun estimate(
        figures: GuideFigures?,
        profile: PreClaimProfile,
        days: Int,
        minutesAway: Int?,
    ): PreClaimOutcome {
        fun no(issue: PreClaimIssue) = Unavailable(PreClaimElement.FOOD, issue)
        PreClaimChecks.daysIssue(days)?.let { return no(it) }
        val rate =
            when (val found = DaStepRate.of(figures, FIGURE_ID, profile)) {
                is DaStepRate.Missing -> return no(found.issue)
                is DaStepRate.Found -> found
            }
        val share =
            if (days == 1) {
                when (val found = sameDayShare(figures, minutesAway)) {
                    is Share.Missing -> return no(found.issue)
                    is Share.Percent -> found.value
                }
            } else {
                PreClaimChecks.FULL_SHARE_PERCENT
            }
        val rupees = PreClaimChecks.rupees(rate.ratePaise * days, share)
        return PerDayEstimate(PreClaimElement.FOOD, rupees, rate.ratePaise, days, share, rate.source)
    }

    private sealed interface Share {
        data class Percent(val value: Int) : Share

        data class Missing(val issue: PreClaimIssue) : Share
    }

    private fun sameDayShare(
        figures: GuideFigures?,
        minutesAway: Int?,
    ): Share {
        if (minutesAway == null) return Share.Missing(PreClaimIssue.MINUTES_MISSING)
        if (minutesAway < 1 || minutesAway > PreClaimLimits.MINUTES_IN_DAY) return Share.Missing(PreClaimIssue.MINUTES_OUT_OF_RANGE)
        val figure = figures?.figures?.get(SHARE_FIGURE_ID)?.takeIf { it.mode == FigureModes.TIME_SHARE && it.timeShares.isNotEmpty() }
        val share = figure?.timeShares?.lastOrNull { it.fromMinutes <= minutesAway } ?: return Share.Missing(PreClaimIssue.FIGURE_MISSING)
        return Share.Percent(share.percent)
    }
}
