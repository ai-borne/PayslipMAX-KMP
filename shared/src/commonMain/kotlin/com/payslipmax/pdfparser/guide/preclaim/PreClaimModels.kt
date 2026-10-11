package com.payslipmax.pdfparser.guide.preclaim

/**
 * What a pre-claim estimate can be. There is deliberately no total and no advance: the portal sets the advance and adjusts it
 * at the final claim (plan 21, F1), so the app shows each element it can compute, labelled an estimate, and nothing summed.
 */
enum class PreClaimElement { CTG, BAGGAGE_ROAD, CAR_CONVEYANCE, FOOD, LODGING, LOCAL_TAXI }

/** Which Composite Transfer Grant rule applies. The officer picks it; the app applies no km threshold of its own. */
enum class CtgCase { NORMAL, LOCAL_MOVE, ISLAND }

/** Why an element has no estimate. Plain codes so the UI words them; shared code holds no copy. */
enum class PreClaimIssue {
    FIGURE_MISSING,
    CASE_NOT_IN_FIGURE,
    BASIC_MISSING,
    BASIC_OUT_OF_RANGE,
    LEVEL_MISSING,
    LEVEL_NOT_COVERED,
    DA_MISSING,
    DA_OUT_OF_RANGE,
    KM_MISSING,
    KM_OUT_OF_RANGE,
    STATE_MISSING,
    STATE_NOT_LISTED,
    DATES_MISSING,
    DATE_INVALID,
    DATES_REVERSED,
    DAYS_OUT_OF_RANGE,
    MINUTES_MISSING,
    MINUTES_OUT_OF_RANGE,
    NIGHTS_MISSING,
    NIGHTS_OUT_OF_RANGE,
    NIGHTLY_RATE_MISSING,
    NIGHTLY_RATE_OUT_OF_RANGE,
    TR_RULE_MISSING,
    SANCTION_UNANSWERED,
}

/** Why a daily or conveyance element is nil by rule, which is an answer and not a gap. */
enum class NilReason { RULE_124_NO_DA, NO_RULE_40_SANCTION }

/** Something the worksheet should say next to the estimates rather than compute. */
enum class PreClaimNote {
    OVER_180_DAYS,
}

/**
 * Bounds on typed input. They catch a slipped digit, they are not rules: a basic above the apex pay-matrix cell (Rs 2,50,000)
 * by a wide margin, a DA far past any revision, and a road journey longer than any in India.
 */
object PreClaimLimits {
    const val MAX_BASIC_PAY = 500_000L
    const val MAX_DA_PERCENT = 300
    const val MAX_ROAD_KM = 10_000
    const val MAX_DUTY_DAYS = 730
    const val MAX_NIGHTLY_RATE = 100_000L
    const val MINUTES_IN_DAY = 1440
}

/**
 * Two rules the calculator reads by name. Both are pinned against the shipped data: the option id by a contract test on the real
 * bundle, the day count by the TR 2014 rules (123 and 114 Note 4: no daily allowance after 180 days at one place).
 */
object PreClaimRules {
    /** The TD "TR Rule" option whose label says DA is not admissible (`preclaim.json` option `tr124`). */
    const val NO_DA_RULE_OPTION_ID = "tr124"
    const val DA_CAP_DAYS = 180
}

/** Where an estimate's rate came from, so a line can say "Estimate, rule, as of". [daPercent] is null when the rate does not move with DA. */
data class PreClaimSource(
    val figureId: String,
    val effectiveFrom: String,
    val assumption: String,
    val daPercent: Int?,
)

sealed interface PreClaimOutcome {
    val element: PreClaimElement
}

/** A Composite Transfer Grant: [percentOfBasic] of [basicPay], divided by [divisor] (3 for a local move, else 1), to the rupee. */
data class CtgEstimate(
    val rupees: Long,
    val percentOfBasic: Int,
    val divisor: Int,
    val basicPay: Long,
    val source: PreClaimSource,
) : PreClaimOutcome {
    override val element: PreClaimElement get() = PreClaimElement.CTG
}

/** An amount that is [km] times a rate. The rate is kept in paise (Rs 62.50 is 6250) and only the total is rounded to the rupee. */
data class PerKmEstimate(
    override val element: PreClaimElement,
    val rupees: Long,
    val ratePaise: Long,
    val km: Int,
    val source: PreClaimSource,
) : PreClaimOutcome

/**
 * An amount per day, or a part of one: [ratePaise] a day (exact, Rs 422.50 is 42250) for [days] days, at [sharePercent] of it.
 * Food and the local taxi cap use it. Only the total [rupees] is rounded.
 */
data class PerDayEstimate(
    override val element: PreClaimElement,
    val rupees: Long,
    val ratePaise: Long,
    val days: Int,
    val sharePercent: Int,
    val source: PreClaimSource,
) : PreClaimOutcome

/**
 * The hotel bill the officer expects, capped: [rupees] is [nights] times the lesser of [claimedPerNight] and the ceiling
 * ([ceilingPaise] a night, exact: Rs 2812.50 is 281250). [capped] says the bill was above the ceiling.
 */
data class LodgingEstimate(
    val rupees: Long,
    val nights: Int,
    val claimedPerNight: Long,
    val ceilingPaise: Long,
    val capped: Boolean,
    val source: PreClaimSource,
) : PreClaimOutcome {
    override val element: PreClaimElement get() = PreClaimElement.LODGING
}

/** A rule given in words with no rupee figure (a taxi limit for levels 12 and above). It carries no amount, by design. */
data class LimitNote(
    override val element: PreClaimElement,
    val text: String,
    val source: PreClaimSource,
) : PreClaimOutcome

/** The rule makes this element nil, so the worksheet says so instead of leaving it blank. */
data class NotPayable(
    override val element: PreClaimElement,
    val reason: NilReason,
) : PreClaimOutcome

data class Unavailable(
    override val element: PreClaimElement,
    val issue: PreClaimIssue,
) : PreClaimOutcome
