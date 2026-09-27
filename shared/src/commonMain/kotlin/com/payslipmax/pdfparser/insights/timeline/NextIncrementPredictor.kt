package com.payslipmax.pdfparser.insights.timeline

/** When and to how much the officer's next annual increment (DNI) is expected to move Basic Pay. */
data class NextIncrementPrediction(
    val date: PayMonth,
    val predictedBasicPay: Double,
    val level: PayLevel,
    val currentStage: Int,
)

/**
 * Predicts the next DNI from the [ServiceTimeline] alone (Pay Audit Phase 6) — no user input. Anchored on
 * the most recent INCREMENT or PROMOTION event: an increment recurs exactly 12 months later (it is
 * already on a 1 Jan/1 Jul cycle date); a promotion's first increment in the new level follows 6 months
 * later, rounded up to the next cycle date (Rule 10/11, mirrored by [IncrementAuditor] and
 * [PayFixationCalculator]'s Option 1). Returns null when the latest month is untrusted (unplaced
 * level/stage), there is no prior INCREMENT/PROMOTION event to anchor on, or the officer is already at
 * the top stage of their level.
 */
object NextIncrementPredictor {
    private const val MONTHS_PER_YEAR = 12
    private const val MONTHS_TO_FIRST_INCREMENT_AFTER_PROMOTION = 6

    fun predict(timeline: ServiceTimeline): NextIncrementPrediction? {
        val latest = timeline.months.maxByOrNull { it.month } ?: return null
        val level = latest.level ?: return null
        val stage = latest.stage ?: return null
        val lastEvent = timeline.events.lastOrNull { it.month <= latest.month } ?: return null

        val nextDate =
            when (lastEvent.type) {
                TimelineEventType.INCREMENT -> lastEvent.month.plusMonths(MONTHS_PER_YEAR)
                TimelineEventType.PROMOTION ->
                    lastEvent.month.plusMonths(MONTHS_TO_FIRST_INCREMENT_AFTER_PROMOTION).nextIncrementCycle()
            }
        val predictedPay = PayMatrix.payAt(level, stage + 1)?.toDouble() ?: return null

        return NextIncrementPrediction(date = nextDate, predictedBasicPay = predictedPay, level = level, currentStage = stage)
    }
}
