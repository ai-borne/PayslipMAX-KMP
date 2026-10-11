package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.domain.PersonalFigureResolver
import com.payslipmax.pdfparser.guide.model.FigureModes
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Baggage by road: a rate per km by pay level, raised by the DA step (Handbook on Travelling Allowances 2023; MoD letter
 * 12630/Mov C/242/D(Mov)/2017, para 2(iv)). The step is [PersonalFigureResolver.stepPercent], the one DA-step rule. The rate
 * stays exact in paise (Rs 62.50 at DA 50-99%); only the total is rounded.
 */
object RoadBaggageCalculator {
    const val FIGURE_ID = "baggage_road"
    private const val PERCENT = 100L

    fun estimate(
        figures: GuideFigures?,
        profile: PreClaimProfile,
        roadKm: Int?,
    ): PreClaimOutcome {
        fun no(issue: PreClaimIssue) = Unavailable(PreClaimElement.BAGGAGE_ROAD, issue)
        val figure = figures?.figures?.get(FIGURE_ID)?.takeIf { it.mode == FigureModes.DA_STEP } ?: return no(PreClaimIssue.FIGURE_MISSING)
        val level = profile.level ?: return no(PreClaimIssue.LEVEL_MISSING)
        val da = profile.daPercent ?: return no(PreClaimIssue.DA_MISSING)
        if (da < 0 || da > PreClaimLimits.MAX_DA_PERCENT) return no(PreClaimIssue.DA_OUT_OF_RANGE)
        PreClaimChecks.kmIssue(roadKm)?.let { return no(it) }
        val band = PersonalFigureResolver.bandForLevel(figure, level)?.takeIf { it.base > 0 } ?: return no(PreClaimIssue.LEVEL_NOT_COVERED)
        val step = PersonalFigureResolver.stepPercent(figures, da) ?: return no(PreClaimIssue.FIGURE_MISSING)
        val km = checkNotNull(roadKm)
        // base rupees x (100 + step) is the rate in paise: Rs 50 x 125 = 6250 paise.
        val ratePaise = band.base.toLong() * (PERCENT + step)
        val source = PreClaimSource(FIGURE_ID, figure.effectiveFrom, figure.assumption, da)
        return PerKmEstimate(PreClaimElement.BAGGAGE_ROAD, PreClaimChecks.rupees(ratePaise * km), ratePaise, km, source)
    }
}
