package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Baggage by road: a rate per km by pay level, raised by the DA step (Handbook on Travelling Allowances 2023; MoD letter
 * 12630/Mov C/242/D(Mov)/2017, para 2(iv)). The rate stays exact in paise (Rs 62.50 at DA 50-99%); only the total is rounded.
 */
object RoadBaggageCalculator {
    const val FIGURE_ID = "baggage_road"

    fun estimate(
        figures: GuideFigures?,
        profile: PreClaimProfile,
        roadKm: Int?,
    ): PreClaimOutcome {
        fun no(issue: PreClaimIssue) = Unavailable(PreClaimElement.BAGGAGE_ROAD, issue)
        val rate =
            when (val found = DaStepRate.of(figures, FIGURE_ID, profile)) {
                is DaStepRate.Missing -> return no(found.issue)
                is DaStepRate.Found -> found
            }
        PreClaimChecks.kmIssue(roadKm)?.let { return no(it) }
        val km = checkNotNull(roadKm)
        return PerKmEstimate(PreClaimElement.BAGGAGE_ROAD, PreClaimChecks.rupees(rate.ratePaise * km), rate.ratePaise, km, rate.source)
    }
}
