package com.payslipmax.pdfparser.guide.preclaim

/** Checks and arithmetic every element shares, so each bound and each rounding has one home. */
internal object PreClaimChecks {
    private const val PAISE_PER_RUPEE = 100L
    private const val HALF_RUPEE_PAISE = 50L
    private const val PERCENT = 100L

    /** A day paid whole; any smaller share comes from the `td_food_hours` figure. */
    const val FULL_SHARE_PERCENT = 100

    /** The issue with a road distance, or null when it is a whole number of km from 1 to the bound. */
    fun kmIssue(km: Int?): PreClaimIssue? =
        when {
            km == null -> PreClaimIssue.KM_MISSING
            km < 1 || km > PreClaimLimits.MAX_ROAD_KM -> PreClaimIssue.KM_OUT_OF_RANGE
            else -> null
        }

    /** A total in paise as whole rupees, half a rupee up. Only a total is ever rounded; a rate stays in paise. */
    fun rupees(paise: Long): Long = (paise + HALF_RUPEE_PAISE) / PAISE_PER_RUPEE

    /** A total in paise, times [percent] of it, as whole rupees, half a rupee up: a share of a rate is rounded once, on the total. */
    fun rupees(
        paise: Long,
        percent: Int,
    ): Long = (paise * percent + HALF_RUPEE_PAISE * PERCENT) / (PAISE_PER_RUPEE * PERCENT)

    /** The issue with a count of days of duty, or null when it is from 1 to the bound. */
    fun daysIssue(days: Int): PreClaimIssue? = if (days < 1 || days > PreClaimLimits.MAX_DUTY_DAYS) PreClaimIssue.DAYS_OUT_OF_RANGE else null

    const val RUPEE_IN_PAISE = PAISE_PER_RUPEE
}
