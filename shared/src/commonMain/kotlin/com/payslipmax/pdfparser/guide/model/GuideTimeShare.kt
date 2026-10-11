package com.payslipmax.pdfparser.guide.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * From [fromMinutes] of time away (up to the next share's start) a same-day trip draws [percent] of the full day's food rate. Whole
 * minutes so an edge is exact: 6 hours is 360, 12 hours is 720, and "over 12 hours" starts at 721.
 */
@Serializable
data class GuideTimeShare(
    @SerialName("from_minutes") val fromMinutes: Int,
    val percent: Int,
)
