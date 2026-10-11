package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Composite Transfer Grant is a percent of basic pay (MoD letter 12630/Mov C/242/D(Mov)/2017, para 3(ii)); the officer picks
 * the case (normal, local move, island) and the app applies no km threshold of its own. Amounts below are the oracle: the
 * first is the owner's issued advance voucher (basic 149000, CTG 119200). NPA and MSP are never part of the basic.
 */
class CtgCalculatorTest {
    private val figures = SyntheticGuideFigures.figures

    private fun profile(
        basic: Long? = 149000,
        msp: Long? = 15500,
        npa: Long? = 30000,
    ) = PreClaimProfile(level = "14", basicPay = basic, daPercent = 60, mspAmount = msp, npaAmount = npa)

    private fun ctg(
        case: CtgCase,
        profile: PreClaimProfile = profile(),
    ) = CtgCalculator.estimate(figures, profile, case)

    @Test
    fun aNormalMoveIsEightyPercentOfBasicAsOnTheOwnersVoucher() {
        val estimate = assertIs<CtgEstimate>(ctg(CtgCase.NORMAL))

        assertEquals(119200L, estimate.rupees)
        assertEquals(80, estimate.percentOfBasic)
        assertEquals(1, estimate.divisor)
    }

    @Test
    fun anIslandMoveIsAHundredPercent() {
        val estimate = assertIs<CtgEstimate>(ctg(CtgCase.ISLAND))

        assertEquals(149000L, estimate.rupees)
        assertEquals(100, estimate.percentOfBasic)
    }

    @Test
    fun aLocalMoveIsOneThirdOfTheEightyPercentRoundedToTheRupee() {
        val estimate = assertIs<CtgEstimate>(ctg(CtgCase.LOCAL_MOVE))

        assertEquals(39733L, estimate.rupees, "119200 / 3 = 39733.33")
        assertEquals(80 to 3, estimate.percentOfBasic to estimate.divisor)
    }

    @Test
    fun theAmountIsRoundedToTheNearestRupeeNotTruncated() {
        assertEquals(80001L, assertIs<CtgEstimate>(ctg(CtgCase.NORMAL, profile(basic = 100001))).rupees, "80000.8 rounds up")
        assertEquals(26667L, assertIs<CtgEstimate>(ctg(CtgCase.LOCAL_MOVE, profile(basic = 100000))).rupees, "26666.67 rounds up")
    }

    @Test
    fun mspAndNpaNeverChangeTheGrant() {
        val without = assertIs<CtgEstimate>(ctg(CtgCase.NORMAL, profile(msp = 0, npa = 0)))
        val with = assertIs<CtgEstimate>(ctg(CtgCase.NORMAL, profile(msp = 15500, npa = 30000)))
        val unknown = assertIs<CtgEstimate>(ctg(CtgCase.NORMAL, profile(msp = null, npa = null)))

        assertEquals(without.rupees, with.rupees, "the letter takes basic pay only")
        assertEquals(without.rupees, unknown.rupees)
        assertEquals(149000L, with.basicPay)
    }

    @Test
    fun theEstimateNamesTheFigureAndTheDateItAppliesFrom() {
        val estimate = assertIs<CtgEstimate>(ctg(CtgCase.NORMAL))

        assertEquals("ctg", estimate.source.figureId)
        assertEquals("2017-07-01", estimate.source.effectiveFrom)
        assertEquals(figures.figures.getValue("ctg").assumption, estimate.source.assumption)
        assertEquals(PreClaimElement.CTG, estimate.element)
    }

    @Test
    fun noBasicPayMeansNoEstimateNotAGuess() {
        assertEquals(Unavailable(PreClaimElement.CTG, PreClaimIssue.BASIC_MISSING), ctg(CtgCase.NORMAL, profile(basic = null)))
    }

    @Test
    fun aBasicOutsideAnyPayMatrixIsRejectedAsATypingSlip() {
        for (bad in listOf(0L, -149000L, PreClaimLimits.MAX_BASIC_PAY + 1)) {
            assertEquals(Unavailable(PreClaimElement.CTG, PreClaimIssue.BASIC_OUT_OF_RANGE), ctg(CtgCase.NORMAL, profile(basic = bad)), "$bad")
        }
        assertIs<CtgEstimate>(ctg(CtgCase.NORMAL, profile(basic = PreClaimLimits.MAX_BASIC_PAY)))
    }

    @Test
    fun noFiguresMeansNoEstimate() {
        assertEquals(Unavailable(PreClaimElement.CTG, PreClaimIssue.FIGURE_MISSING), CtgCalculator.estimate(null, profile(), CtgCase.NORMAL))
        val withoutCtg = figures.copy(figures = figures.figures - "ctg")
        assertEquals(Unavailable(PreClaimElement.CTG, PreClaimIssue.FIGURE_MISSING), CtgCalculator.estimate(withoutCtg, profile(), CtgCase.NORMAL))
    }

    @Test
    fun aCaseTheBundleDoesNotShipIsUnavailableWhileTheNormalCaseStillWorks() {
        val normalOnly = figures.copy(figures = figures.figures + ("ctg" to figures.figures.getValue("ctg").copy(islandPercent = 0, localMoveDivisor = 0)))

        assertEquals(Unavailable(PreClaimElement.CTG, PreClaimIssue.CASE_NOT_IN_FIGURE), CtgCalculator.estimate(normalOnly, profile(), CtgCase.ISLAND))
        assertEquals(Unavailable(PreClaimElement.CTG, PreClaimIssue.CASE_NOT_IN_FIGURE), CtgCalculator.estimate(normalOnly, profile(), CtgCase.LOCAL_MOVE))
        assertEquals(119200L, assertIs<CtgEstimate>(CtgCalculator.estimate(normalOnly, profile(), CtgCase.NORMAL)).rupees)
    }
}
