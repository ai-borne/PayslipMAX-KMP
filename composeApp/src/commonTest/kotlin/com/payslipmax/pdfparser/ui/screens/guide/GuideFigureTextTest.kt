package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.FigureBasis
import com.payslipmax.pdfparser.guide.domain.PersonalFigure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the officer reads must say where the number came from: which payslip month it is based on, which DA, the date of the
 * rate, and the assumption the card could not know. Copy is in [com.payslipmax.pdfparser.ui.theme.GuideStrings]; these tests
 * pin the facts each line must carry, not the wording.
 */
class GuideFigureTextTest {
    private fun basis(
        da: Int? = 60,
        effectiveFrom: String = "2017-07-01",
        assumption: String = "",
    ) = FigureBasis(2026, 6, da, effectiveFrom, assumption)

    @Test
    fun foodLineNamesTheAmountTheLevelTheBaseTheDaStepAndTheAssumption() {
        val figure = PersonalFigure.FoodRate(1500L, 1200L, 25, "14", basis(assumption = "full day (over 12 hours away), before taxes"))

        val text = figureText(figure)

        assertEquals("₹1,500 a day", text.headline)
        for (fact in listOf("Level 14", "₹1,200", "25%", "full day (over 12 hours away), before taxes")) {
            assertTrue(fact in text.detail, "'$fact' missing from '${text.detail}'")
        }
    }

    @Test
    fun foodLineBelowTheFirstDaStepDoesNotMentionAStep() {
        val figure = PersonalFigure.FoodRate(1200L, 1200L, 0, "14", basis(da = 40))

        assertFalse("%" in figureText(figure).detail, "no DA step has applied yet")
    }

    @Test
    fun everyLineSaysWhichPayslipMonthAndDaItIsBasedOnAndWhenTheRateStarted() {
        val footnote = figureText(PersonalFigure.Hra(30, "X", basis(effectiveFrom = "2024-01-01"))).footnote

        for (fact in listOf("Jun 2026", "DA 60%", "Jan 2024")) {
            assertTrue(fact in footnote, "'$fact' missing from '$footnote'")
        }
    }

    @Test
    fun ctgNeedsNoDaSoItsFootnoteDoesNotInventOne() {
        val figure = PersonalFigure.Ctg(115360L, 80, basis(da = null, assumption = "move of 20 km or more"))

        val text = figureText(figure)

        assertEquals("About ₹1,15,360", text.headline)
        assertTrue("80%" in text.detail && "move of 20 km or more" in text.detail)
        assertFalse("DA" in text.footnote)
        assertTrue("Jun 2026" in text.footnote && "Jul 2017" in text.footnote)
    }

    @Test
    fun transportLineNamesTheBasePlusDaAndTheCityClass() {
        val higher = figureText(PersonalFigure.Transport(11520L, 7200L, "12A", "HIGHER", basis()))
        val flat = figureText(PersonalFigure.Transport(25200L, 15750L, "14", null, basis()))

        assertEquals("₹11,520 a month", higher.headline)
        for (fact in listOf("Level 12A", "₹7,200", "60%")) assertTrue(fact in higher.detail, "'$fact' missing from '${higher.detail}'")
        assertFalse(higher.detail == flat.detail, "the city class is part of the higher-rate line")
        assertTrue("Level 14" in flat.detail && "₹15,750" in flat.detail)
    }

    @Test
    fun hraLineRestatesTheRateAndTheClassItFits() {
        val text = figureText(PersonalFigure.Hra(30, "X", basis()))

        assertEquals("30% of basic pay", text.headline)
        assertTrue("X" in text.detail && "60%" in text.detail)
    }
}
