package com.payslipmax.pdfparser.insights.timeline

internal data class LevelStage(val level: PayLevel, val stage: Int)

/**
 * Places a basic pay on the pay matrix. Several levels share cells (e.g. 10 and 10B), so a cell alone
 * cannot always name the level; the officer's previous position settles it: staying put or moving one
 * stage up is the same level, anything else is a promotion to the next higher level holding that cell.
 */
internal object LevelResolver {
    fun resolve(
        basicPay: Double,
        previous: LevelStage?,
    ): LevelStage? {
        val candidates = PayMatrix.levelsContaining(basicPay)
        if (candidates.isEmpty()) return null
        val level = pick(candidates, basicPay, previous) ?: return null
        return PayMatrix.stageOf(level, basicPay)?.let { LevelStage(level, it) }
    }

    private fun pick(
        candidates: List<PayLevel>,
        basicPay: Double,
        previous: LevelStage?,
    ): PayLevel? {
        if (previous == null) return candidates.singleOrNull()
        val higher = candidates.firstOrNull { it > previous.level }
        if (previous.level !in candidates) return higher ?: candidates.singleOrNull()
        val step = (PayMatrix.stageOf(previous.level, basicPay) ?: return null) - previous.stage
        return if (step in 0..1) previous.level else higher ?: previous.level
    }
}
