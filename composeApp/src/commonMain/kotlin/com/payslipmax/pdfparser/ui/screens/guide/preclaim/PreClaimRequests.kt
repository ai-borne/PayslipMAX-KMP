package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.preclaim.PermanentDutyRequest
import com.payslipmax.pdfparser.guide.preclaim.PreClaimProfile
import com.payslipmax.pdfparser.guide.preclaim.TemporaryDutyRequest

/**
 * Turns the typed form into the calculators' requests. Pure and without side effects. A box left empty becomes null, which the
 * calculators answer with a "missing" issue for the elements that need it and nothing else.
 */
internal object PreClaimRequests {
    private const val MINUTES_PER_HOUR = 60

    fun permanentDuty(
        form: PreClaimFormState,
        profile: PreClaimProfile,
    ) = PermanentDutyRequest(
        profile = profile,
        ctgCase = form.ctgCase,
        roadKm = form.roadKm.toIntOrNull(),
        originState = form.originState,
        bothSpousesServing = PreClaimFieldIds.SPOUSE_IN_ARMY in form.ticks,
    )

    /** [days] is the inclusive day count of the dates, the default for nights until the officer types a number. */
    fun temporaryDuty(
        form: PreClaimFormState,
        profile: PreClaimProfile,
        days: Int?,
    ) = TemporaryDutyRequest(
        profile = profile,
        trRuleId = form.trRuleId,
        from = form.from,
        to = form.to,
        minutesAway = minutesAway(form),
        nights = form.nights?.toIntOrNull() ?: days,
        nightlyRateRupees = form.nightlyRate.toLongOrNull(),
        holdsRmaSanction = form.holdsRmaSanction,
        roadKm = form.roadKm.toIntOrNull(),
        originState = form.originState,
    )

    /** Null when neither box is filled; a blank half counts as zero, so "6" hours alone is six hours. */
    private fun minutesAway(form: PreClaimFormState): Int? {
        if (form.awayHours.isEmpty() && form.awayMinutes.isEmpty()) return null
        return (form.awayHours.toIntOrNull() ?: 0) * MINUTES_PER_HOUR + (form.awayMinutes.toIntOrNull() ?: 0)
    }
}
