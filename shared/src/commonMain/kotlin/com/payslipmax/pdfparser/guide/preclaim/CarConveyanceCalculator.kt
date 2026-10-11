package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.FigureModes
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Own-car conveyance on a move: the distance times the Road Mileage Allowance of the state the journey starts in (PCDA(O)
 * Pune Wing Circular no. 10, 2024-09-03). The state must match the table's own spelling, so a picker built from the bundle can
 * never miss and a free-typed near miss is refused instead of being priced at nil.
 */
object CarConveyanceCalculator {
    const val FIGURE_ID = "rma"

    fun estimate(
        figures: GuideFigures?,
        originState: String?,
        roadKm: Int?,
    ): PreClaimOutcome {
        fun no(issue: PreClaimIssue) = Unavailable(PreClaimElement.CAR_CONVEYANCE, issue)
        val figure =
            figures?.figures?.get(FIGURE_ID)?.takeIf { it.mode == FigureModes.RATE_BY_STATE } ?: return no(PreClaimIssue.FIGURE_MISSING)
        if (originState.isNullOrBlank()) return no(PreClaimIssue.STATE_MISSING)
        val rate = figure.rates[originState] ?: return no(PreClaimIssue.STATE_NOT_LISTED)
        PreClaimChecks.kmIssue(roadKm)?.let { return no(it) }
        val km = checkNotNull(roadKm)
        val ratePaise = rate * PreClaimChecks.RUPEE_IN_PAISE
        val source = PreClaimSource(FIGURE_ID, figure.effectiveFrom, figure.assumption, daPercent = null)
        return PerKmEstimate(PreClaimElement.CAR_CONVEYANCE, PreClaimChecks.rupees(ratePaise * km), ratePaise, km, source)
    }
}
