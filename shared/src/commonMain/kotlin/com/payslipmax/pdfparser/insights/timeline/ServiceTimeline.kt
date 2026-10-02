package com.payslipmax.pdfparser.insights.timeline

/** A calendar month, ordered chronologically. */
data class PayMonth(val year: Int, val month: Int) : Comparable<PayMonth> {
    val index: Int get() = year * 12 + month - 1

    override fun compareTo(other: PayMonth): Int = index.compareTo(other.index)

    fun isRightAfter(other: PayMonth): Boolean = index == other.index + 1
}

/** Transport Allowance city class, recovered from the TPTA base rate (Level 9+ officers). */
enum class TptaCityClass(val baseRate: Double) {
    HIGHER(7200.0),
    OTHER(3600.0),
}

enum class PostingKind { RISK_HARDSHIP, FIELD }

enum class TimelineEventType { INCREMENT, PROMOTION }

/**
 * One month of the officer's service, read purely from that month's payslip.
 * [level] and [stage] are null when the basic pay sits in more than one pay-matrix level and nothing
 * earlier in the history settles which; [daPercent] is null when DA / (Basic + MSP) is not a whole
 * percent (arrears folded in, or MSP not paid); [tptaCity] is null when TPTA is absent or unrecognised.
 */
data class TimelineMonth(
    val month: PayMonth,
    val basicPay: Double,
    val level: PayLevel?,
    val stage: Int?,
    val daPercent: Int?,
    val tptaCity: TptaCityClass?,
    val occupiesQuarters: Boolean,
)

/** A pay change first seen in [month]: a one-stage increment (DNI) or a move to a higher level. */
data class TimelineEvent(
    val month: PayMonth,
    val type: TimelineEventType,
    val fromLevel: PayLevel?,
    val toLevel: PayLevel?,
    val fromBasicPay: Double,
    val toBasicPay: Double,
)

/** Consecutive months (inclusive) in which the allowance for [kind] was paid. */
data class PostingSpan(
    val kind: PostingKind,
    val first: PayMonth,
    val last: PayMonth,
)

data class ServiceTimeline(
    val months: List<TimelineMonth>,
    val events: List<TimelineEvent>,
    val postings: List<PostingSpan>,
) {
    fun monthAt(month: PayMonth): TimelineMonth? = months.firstOrNull { it.month == month }
}
