package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideFigure
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The island percent and the local-move divisor of the CTG figure decide a rupee amount on a pre-claim, so a bad pair must
 * stop at load, not turn into a wrong estimate: a zero divisor would crash the sum, an island percent below the normal one
 * would shrink an island officer's grant. `figures.py` enforced the same limits when the bundle was built.
 */
class GuideCtgCaseValidationTest {
    private val valid: GuideBundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON)).bundle
    private val ctg: GuideFigure = SyntheticGuideFigures.figures.figures.getValue("ctg")

    private fun problems(edit: (GuideFigure) -> GuideFigure): List<String> {
        val figures = SyntheticGuideFigures.figures
        val onPersonalCard = edit(ctg).copy(card = SyntheticGuideBundle.PERSONAL_CARD)
        return GuideBundleValidator.validate(valid.copy(figures = figures.copy(figures = mapOf("ctg" to onPersonalCard))))
    }

    @Test
    fun theShippedCtgFigureCarriesBothCasesAndIsValid() {
        assertEquals(100 to 3, ctg.islandPercent to ctg.localMoveDivisor)
        assertEquals(emptyList(), problems { it })
    }

    @Test
    fun aFigureThatLeavesBothCasesOutIsValid() {
        assertEquals(emptyList(), problems { it.copy(islandPercent = 0, localMoveDivisor = 0) })
    }

    @Test
    fun anIslandPercentBelowTheNormalOneOrAbove100IsRejected() {
        for (bad in listOf(1, 79, 101, -5)) {
            val found = problems { it.copy(islandPercent = bad) }
            assertTrue(found.any { "figure ctg: island_percent" in it }, "$bad: $found")
        }
    }

    @Test
    fun aLocalMoveDivisorOutsideTwoToTenIsRejected() {
        for (bad in listOf(1, 11, -3)) {
            val found = problems { it.copy(localMoveDivisor = bad) }
            assertTrue(found.any { "figure ctg: local_move_divisor" in it }, "$bad: $found")
        }
    }
}
