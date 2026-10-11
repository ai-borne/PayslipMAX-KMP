package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Estimates for a permanent-duty move, one outcome per [PreClaimElement] in that order, each independent: a missing km hides
 * baggage and conveyance but not the CTG. Never sums them (the portal sets the advance, plan 21 F1), never logs or stores an
 * input, and never reads the network: it is a pure function of the shipped figures and the officer's values.
 */
object PreClaimCalculator {
    fun permanentDuty(
        figures: GuideFigures?,
        request: PermanentDutyRequest,
    ): List<PreClaimOutcome> =
        listOf(
            CtgCalculator.estimate(figures, request.profile, request.ctgCase),
            RoadBaggageCalculator.estimate(figures, request.profile, request.roadKm),
            CarConveyanceCalculator.estimate(figures, request.originState, request.roadKm),
        )
}
