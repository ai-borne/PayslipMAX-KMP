package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/** Temporary duty: the rule, the dates, a short trip's time away, hotel nights and bill, and the own-car question. */
@Composable
internal fun PreClaimTemporaryDutySection(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    PreClaimSectionFrame(PreClaimStrings.journeyTitle) {
        PreClaimRulePicker(ws, typed, model)
        PreClaimDateBox(PreClaimStrings.fromDate, typed.from, model::setFromDate)
        PreClaimDateBox(PreClaimStrings.toDate, typed.to, model::setToDate)
        ws.days?.let { PreClaimQuietText("${PreClaimStrings.daysAway}: ${PreClaimStrings.daysCount(it)}") }
        if (typed.from != null && typed.from == typed.to) PreClaimAwayTime(typed, model)
        PreClaimTextBox(PreClaimStrings.nights, typed.nights ?: ws.nightsText, model::setNights)
        PreClaimTextBox(PreClaimStrings.nightlyRate, typed.nightlyRate, model::setNightlyRate)
        PreClaimQuietText(PreClaimStrings.sanctionQuestion)
        PreClaimChipChoice(listOf(true to PreClaimStrings.yes, false to PreClaimStrings.no), typed.holdsRmaSanction, model::setHoldsRmaSanction)
        if (typed.holdsRmaSanction == true) PreClaimRoadJourney(ws, typed, model)
    }
}

@Composable
private fun PreClaimAwayTime(
    typed: PreClaimFormState,
    model: PreClaimViewModel,
) {
    PreClaimQuietText(PreClaimStrings.awayHelp)
    PreClaimTextBox(PreClaimStrings.awayHours, typed.awayHours, model::setAwayHours)
    PreClaimTextBox(PreClaimStrings.awayMinutes, typed.awayMinutes, model::setAwayMinutes)
}
