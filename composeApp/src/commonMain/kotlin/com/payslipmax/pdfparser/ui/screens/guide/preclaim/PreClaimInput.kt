package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.preclaim.PreClaimLimits

/**
 * Cleans what the officer types before it is held, so the form can never contain a letter in a number, a control character in a
 * text, or more characters than any valid value needs. These are length guards; whether a value is in range is the calculators'
 * answer (an issue code), so a slipped digit still shows a message instead of being silently trimmed.
 */
internal object PreClaimInput {
    /** Digits only, no longer than the largest allowed value ([maxValue]) is written. */
    fun digits(
        raw: String,
        maxValue: Long,
    ): String = raw.filter { it in '0'..'9' }.take(maxValue.toString().length)

    /** Digits for a clock box: the new text if its value is at most [maxValue], else [previous] (a keystroke that would pass the limit is ignored). */
    fun clock(
        raw: String,
        maxValue: Long,
        previous: String,
    ): String {
        val cleaned = raw.filter { it in '0'..'9' }.take(maxValue.toString().length)
        return if ((cleaned.toLongOrNull() ?: 0L) <= maxValue) cleaned else previous
    }

    /** Free text for a worksheet field, without control characters, cut to the bundle's limit. */
    fun text(
        raw: String,
        maxChars: Int,
    ): String = raw.filterNot { it.isISOControl() }.take(maxChars)

    /** A trip away is typed as hours and minutes; the calculator bounds the total at one day. */
    const val MAX_HOURS = 24L
    const val MAX_MINUTES = 59L
    const val MAX_KM = PreClaimLimits.MAX_ROAD_KM.toLong()
    const val MAX_NIGHTS = PreClaimLimits.MAX_DUTY_DAYS.toLong()
}
