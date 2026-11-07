package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.summary.TdFoodTiming
import com.payslipmax.pdfparser.guide.summary.TdLevelGroup
import com.payslipmax.pdfparser.guide.summary.TdTaxi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The words on the TD summary table (plan 24, P4). Why they matter: an officer reads an amount off these lines and may claim it, so a
 * half rupee must show, a ceiling must say it is "up to", and the time bands must name the hours the rule uses, not minutes.
 */
class TdSummaryWordingTest {
    private val timings = listOf(TdFoodTiming(0, 30), TdFoodTiming(360, 70), TdFoodTiming(721, 100))

    @Test
    fun levelsNameTheFirstAndLastOfTheBand() {
        assertEquals("Levels 9 to 11", TdSummaryWording.levels(listOf("9", "10", "10A", "10B", "11")))
        assertEquals("Levels 12 to 13B", TdSummaryWording.levels(listOf("12", "12A", "12B", "13", "13A", "13B")))
        assertEquals("Level 14", TdSummaryWording.levels(listOf("14")))
    }

    @Test
    fun aHalfRupeeIsNeverRoundedAndACeilingSaysUpTo() {
        assertEquals("Up to ₹2,812.50 a night", TdSummaryWording.hotel(281250L))
        assertEquals("Up to ₹9,375 a night", TdSummaryWording.hotel(937500L))
        assertEquals("₹1,125 a day", TdSummaryWording.food(112500L))
    }

    @Test
    fun taxiIsACapOrTheBundlesOwnLimitText() {
        assertEquals("Non-AC taxi up to ₹422.50 a day", TdSummaryWording.taxi(TdTaxi.Amount(42250L)))
        assertEquals("AC taxi, up to 50 km a day within the city", TdSummaryWording.taxi(TdTaxi.Limit("AC taxi, up to 50 km a day within the city")))
    }

    @Test
    fun foodTimeBandsAreNamedInHoursFromTheBoundaryMinutes() {
        // 360 minutes is 6 hours; 721 is "over 12 hours", so the middle band is 6 to 12 hours.
        val labels = TdSummaryWording.timingLines(timings, listOf(33750L, 78750L, 112500L))
        assertEquals(listOf("Under 6 hours: ₹337.50", "6 to 12 hours: ₹787.50", "Over 12 hours: ₹1,125"), labels)
    }

    @Test
    fun aSingleShareIsJustTheWholeDay() {
        assertEquals(listOf("Any time away: ₹1,125"), TdSummaryWording.timingLines(listOf(TdFoodTiming(0, 100)), listOf(112500L)))
    }

    @Test
    fun noFoodAmountMeansNoTimingLines() {
        assertEquals(emptyList(), TdSummaryWording.timingLines(timings, emptyList()))
    }

    @Test
    fun theBandReadsAsPlainWords() {
        assertEquals("Rates at DA 50 to 99%", TdSummaryWording.band(50, 99))
    }

    @Test
    fun aRowReadsAsOneSentenceForTalkBackAndNamesTheOwnLevel() {
        val group = TdLevelGroup(listOf("12", "12A", "12B", "13", "13A", "13B"), 562500L, TdTaxi.Limit("AC taxi, up to 50 km a day within the city"), 125000L, listOf(37500L, 87500L, 125000L))
        val own = TdSummaryWording.description(group, timings, isOwn = true)
        assertTrue(own.startsWith("Your level. Levels 12 to 13B."), own)
        assertTrue("Up to ₹5,625 a night" in own && "₹1,250 a day" in own, own)
        assertTrue(TdSummaryWording.description(group, timings, isOwn = false).startsWith("Levels 12 to 13B."))
    }
}
