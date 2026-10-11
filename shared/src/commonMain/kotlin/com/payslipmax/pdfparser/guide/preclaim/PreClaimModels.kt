package com.payslipmax.pdfparser.guide.preclaim

/**
 * What a pre-claim estimate can be. There is deliberately no total and no advance: the portal sets the advance and adjusts it
 * at the final claim (plan 21, F1), so the app shows each element it can compute, labelled an estimate, and nothing summed.
 */
enum class PreClaimElement { CTG, BAGGAGE_ROAD, CAR_CONVEYANCE }

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
}

/**
 * Bounds on typed input. They catch a slipped digit, they are not rules: a basic above the apex pay-matrix cell (Rs 2,50,000)
 * by a wide margin, a DA far past any revision, and a road journey longer than any in India.
 */
object PreClaimLimits {
    const val MAX_BASIC_PAY = 500_000L
    const val MAX_DA_PERCENT = 300
    const val MAX_ROAD_KM = 10_000
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

data class Unavailable(
    override val element: PreClaimElement,
    val issue: PreClaimIssue,
) : PreClaimOutcome
