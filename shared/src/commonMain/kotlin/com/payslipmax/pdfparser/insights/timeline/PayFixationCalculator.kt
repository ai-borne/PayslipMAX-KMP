package com.payslipmax.pdfparser.insights.timeline

/** One promotion pay-fixation option's outcome: the pay it fixes at, when the next increment follows, and the running total. */
data class PayFixationOption(
    val fixedPay: Int,
    val firstIncrementDate: PayMonth,
    val monthlyPay: List<Pair<PayMonth, Int>>,
    val total: Int,
)

/** Option 1 vs Option 2 (Army Officers Pay Rules 2017, Rule 10 & 11) compared over the same window. */
data class PayFixationComparison(
    val option1: PayFixationOption,
    val option2: PayFixationOption,
    val recommendedOption: Int,
)

/**
 * The pay-fixation option calculator (Pay Audit Phase 6): on promotion, an officer elects, once and
 * finally, between fixation from the date of promotion (Option 1) or from their next DNI in the lower
 * level (Option 2) — SRO 12(E) dated 03 May 2017. Ported from the worked reference in
 * `scripts/pcdao_factory/test_simulation_scenarios.py::calculate_pay_fixation_36mo`, with one correction:
 * the reference rounds "6 months after promotion/DNI" to the next 1 Jan/1 Jul cycle with a bug at the
 * exact boundary (a promotion or DNI that falls in January or July itself is rounded a full cycle too
 * late). [PayMonth.nextIncrementCycle] fixes that; the ported worked example (Level 10 stage 8 to Level
 * 11, promoted March, DNI in July) is unaffected by the bug and is [PayFixationCalculatorTest]'s gate.
 */
object PayFixationCalculator {
    private const val DEFAULT_WINDOW_MONTHS = 36
    private const val MONTHS_TO_FIRST_INCREMENT = 6
    private const val MONTHS_PER_YEAR = 12

    fun compare(
        fromLevel: PayLevel,
        fromStage: Int,
        toLevel: PayLevel,
        promotionMonth: PayMonth,
        dniMonth: Int,
        windowMonths: Int = DEFAULT_WINDOW_MONTHS,
    ): PayFixationComparison {
        require(dniMonth == 1 || dniMonth == 7) { "dniMonth must be 1 (January) or 7 (July)" }

        val fromCells = PayMatrix.levelCells(fromLevel)
        val toCells = PayMatrix.levelCells(toLevel)
        val fromIndex = fromStage - 1
        val window = (0 until windowMonths).map { promotionMonth.plusMonths(it) }

        val option1 = option1(fromCells, toCells, fromIndex, promotionMonth, window)
        val option2 = option2(fromCells, toCells, fromIndex, promotionMonth, dniMonth, window)
        val recommended = if (option2.total > option1.total) 2 else 1

        return PayFixationComparison(option1, option2, recommended)
    }

    private fun option1(
        fromCells: List<Int>,
        toCells: List<Int>,
        fromIndex: Int,
        promotionMonth: PayMonth,
        window: List<PayMonth>,
    ): PayFixationOption {
        val incrementInLower = fromCells[minOf(fromIndex + 1, fromCells.lastIndex)]
        val fixedPay = findCellOrNextHigher(toCells, incrementInLower)
        val fixedCellIndex = toCells.indexOf(fixedPay)
        val firstIncrementDate = promotionMonth.plusMonths(MONTHS_TO_FIRST_INCREMENT).nextIncrementCycle()

        val monthlyPay =
            window.map { month ->
                val pay =
                    if (month < firstIncrementDate) {
                        fixedPay
                    } else {
                        toCells[cellIndexAfterAnnualSteps(fixedCellIndex, firstIncrementDate, month, toCells.lastIndex)]
                    }
                month to pay
            }
        return PayFixationOption(fixedPay, firstIncrementDate, monthlyPay, monthlyPay.sumOf { it.second })
    }

    private fun option2(
        fromCells: List<Int>,
        toCells: List<Int>,
        fromIndex: Int,
        promotionMonth: PayMonth,
        dniMonth: Int,
        window: List<PayMonth>,
    ): PayFixationOption {
        val preDniPay = findCellOrNextHigher(toCells, fromCells[fromIndex])
        val twoIncrementsInLower = fromCells[minOf(fromIndex + 2, fromCells.lastIndex)]
        val postDniFixedPay = findCellOrNextHigher(toCells, twoIncrementsInLower)
        val postDniCellIndex = toCells.indexOf(postDniFixedPay)

        val dniDate = nextCycleAtOrAfter(promotionMonth, dniMonth)
        val firstIncrementDate = dniDate.plusMonths(MONTHS_TO_FIRST_INCREMENT).nextIncrementCycle()

        val monthlyPay =
            window.map { month ->
                val pay =
                    when {
                        month < dniDate -> preDniPay
                        month < firstIncrementDate -> postDniFixedPay
                        else -> toCells[cellIndexAfterAnnualSteps(postDniCellIndex, firstIncrementDate, month, toCells.lastIndex)]
                    }
                month to pay
            }
        return PayFixationOption(postDniFixedPay, firstIncrementDate, monthlyPay, monthlyPay.sumOf { it.second })
    }

    /** The officer's own next DNI month ([dniMonth], 1 or 7) at or after [from]. */
    private fun nextCycleAtOrAfter(
        from: PayMonth,
        dniMonth: Int,
    ): PayMonth {
        val sameYear = PayMonth(from.year, dniMonth)
        return if (sameYear >= from) sameYear else PayMonth(from.year + 1, dniMonth)
    }

    /** Index in [toCells] after the annual increments due between [firstIncrementDate] and [month] (both on cycle dates). */
    private fun cellIndexAfterAnnualSteps(
        fromCellIndex: Int,
        firstIncrementDate: PayMonth,
        month: PayMonth,
        lastCellIndex: Int,
    ): Int {
        val monthsSinceFirst = month.index - firstIncrementDate.index
        val steps = 1 + (monthsSinceFirst / MONTHS_PER_YEAR)
        return minOf(fromCellIndex + steps, lastCellIndex)
    }

    private fun findCellOrNextHigher(
        cells: List<Int>,
        target: Int,
    ): Int = cells.firstOrNull { it >= target } ?: cells.last()
}
