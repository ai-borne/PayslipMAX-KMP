package com.payslipmax.pdfparser.insights.timeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Gate for [PayFixationCalculator]: Case A replicates the worked example in
 * `scripts/pcdao_factory/test_simulation_scenarios.py::test_pay_fixation_scenarios` (Level 10 stage 8 to
 * Level 11, promoted 15 March 2026, DNI 1 July) — the officer's own annotation notes the expected fixed
 * pay of both options and that Option 2 nets more over 36 months near a DNI.
 */
class PayFixationCalculatorTest {
    @Test
    fun officialWorkedExample_level10StageEightToLevel11_promotedMarch_dniJuly() {
        val result =
            PayFixationCalculator.compare(
                fromLevel = PayLevel.L10,
                fromStage = 8,
                toLevel = PayLevel.L11,
                promotionMonth = PayMonth(2026, 3),
                dniMonth = 7,
            )

        assertEquals(71500, result.option1.fixedPay)
        assertEquals(73600, result.option2.fixedPay)
        assertTrue(result.option2.total > result.option1.total, "Option 2 should net more over 36 months near a DNI")
        assertEquals(2, result.recommendedOption)
    }

    @Test
    fun option1FirstIncrementFollowsSixMonthsAfterPromotion_roundedToNextCycle() {
        val result =
            PayFixationCalculator.compare(
                fromLevel = PayLevel.L10,
                fromStage = 8,
                toLevel = PayLevel.L11,
                promotionMonth = PayMonth(2026, 3),
                dniMonth = 7,
            )

        // 15 Mar 2026 + 6 months = Sep 2026; next 1 Jan/1 Jul cycle on or after that is 1 Jan 2027.
        assertEquals(PayMonth(2027, 1), result.option1.firstIncrementDate)
    }

    @Test
    fun option2FirstIncrementFollowsSixMonthsAfterTheOfficersOwnDni() {
        val result =
            PayFixationCalculator.compare(
                fromLevel = PayLevel.L10,
                fromStage = 8,
                toLevel = PayLevel.L11,
                promotionMonth = PayMonth(2026, 3),
                dniMonth = 7,
            )

        // DNI is 1 Jul 2026 (the next July at/after promotion); +6 months = Jan 2027, already a cycle date.
        assertEquals(PayMonth(2027, 1), result.option2.firstIncrementDate)
    }

    @Test
    fun aPromotionExactlyOnACycleMonthDoesNotSkipAFullCycle() {
        // Regression for the ported reference's boundary bug: a promotion in January itself must round
        // "6 months later" (July) up to July of the SAME year, not January of the next year.
        val result =
            PayFixationCalculator.compare(
                fromLevel = PayLevel.L10,
                fromStage = 8,
                toLevel = PayLevel.L11,
                promotionMonth = PayMonth(2026, 1),
                dniMonth = 7,
            )

        assertEquals(PayMonth(2026, 7), result.option1.firstIncrementDate)
    }

    @Test
    fun thereafterIncrementsRecurEveryTwelveMonths() {
        val result =
            PayFixationCalculator.compare(
                fromLevel = PayLevel.L10,
                fromStage = 8,
                toLevel = PayLevel.L11,
                promotionMonth = PayMonth(2026, 3),
                dniMonth = 7,
                windowMonths = 36,
            )

        val payAt = result.option1.monthlyPay.toMap()
        val firstIncrement = result.option1.firstIncrementDate
        val payAtFirst = payAt.getValue(firstIncrement)
        val payTwelveMonthsLater = payAt.getValue(firstIncrement.plusMonths(12))
        val payElevenMonthsLater = payAt.getValue(firstIncrement.plusMonths(11))

        assertTrue(payTwelveMonthsLater > payAtFirst, "a further increment must have landed exactly 12 months later")
        assertEquals(payAtFirst, payElevenMonthsLater, "no increment should land before the 12-month mark")
    }

    @Test
    fun theTopStageOfALevelHasNoFurtherIncrement() {
        val topStage = PayMatrix.levelCells(PayLevel.L17).size
        val result =
            PayFixationCalculator.compare(
                fromLevel = PayLevel.L16,
                fromStage = 4,
                toLevel = PayLevel.L17,
                promotionMonth = PayMonth(2026, 3),
                dniMonth = 7,
                windowMonths = 36,
            )

        assertTrue(result.option1.monthlyPay.all { it.second == PayMatrix.payAt(PayLevel.L17, topStage) })
    }
}
