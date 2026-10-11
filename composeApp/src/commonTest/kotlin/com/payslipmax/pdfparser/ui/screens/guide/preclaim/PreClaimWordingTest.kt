package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.preclaim.CtgEstimate
import com.payslipmax.pdfparser.guide.preclaim.LimitNote
import com.payslipmax.pdfparser.guide.preclaim.LodgingEstimate
import com.payslipmax.pdfparser.guide.preclaim.NilReason
import com.payslipmax.pdfparser.guide.preclaim.NotPayable
import com.payslipmax.pdfparser.guide.preclaim.PerDayEstimate
import com.payslipmax.pdfparser.guide.preclaim.PerKmEstimate
import com.payslipmax.pdfparser.guide.preclaim.PreClaimElement
import com.payslipmax.pdfparser.guide.preclaim.PreClaimIssue
import com.payslipmax.pdfparser.guide.preclaim.PreClaimNote
import com.payslipmax.pdfparser.guide.preclaim.PreClaimSource
import com.payslipmax.pdfparser.guide.preclaim.Unavailable
import com.payslipmax.pdfparser.ui.theme.PreClaimIssueStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The words around the calculators' numbers. What matters: a ceiling is never rounded in the text (Rs 2,812.50 stays 2,812.50, because
 * showing 2,813 would overstate it); every line that carries a rate says "Estimate", the rule and the date; a rule given in words shows
 * no amount; a nil is worded as nil with its reason; and every reason a line can be empty has its own sentence.
 */
class PreClaimWordingTest {
    private val source = PreClaimSource("td_hotel", "2017-07-01", "the most a night can cost.", daPercent = 50)

    @Test
    fun aRateKeepsItsPaiseSoACeilingIsNotOverstated() {
        assertEquals("₹2,812.50", PreClaimWording.rate(281_250))
        assertEquals("₹422.50", PreClaimWording.rate(42_250))
        assertEquals("₹62.50", PreClaimWording.rate(6_250))
        assertEquals("₹450", PreClaimWording.rate(45_000))
        assertEquals("₹5.05", PreClaimWording.rate(505), "a single-digit paise keeps its leading zero")
    }

    @Test
    fun everyRateLineSaysEstimateTheRuleAndTheDate() {
        val lodging = LodgingEstimate(rupees = 256_500, nights = 57, claimedPerNight = 4500, ceilingPaise = 450_000, capped = false, source = source)
        val line = PreClaimWording.line(lodging)
        assertEquals("Hotel", line.title)
        assertEquals("₹2,56,500", line.amount)
        assertEquals("Estimate. Rule: the most a night can cost. As of 2017-07-01.", line.source)
    }

    @Test
    fun theSourceLineReadsTheSameWhetherOrNotTheFigureRuleEndsInAFullStop() {
        val expected = "Estimate. Rule: full day, before taxes. As of 2017-07-01."
        assertEquals(expected, PreClaimStrings.estimateSource("full day, before taxes", "2017-07-01"), "the shipped rules carry no full stop")
        assertEquals(expected, PreClaimStrings.estimateSource("full day, before taxes.", "2017-07-01"))
        assertEquals(expected, PreClaimStrings.estimateSource("full day, before taxes. ", "2017-07-01"))
    }

    @Test
    fun aCappedHotelBillSaysTheLimitWasUsedAndAnUncappedOneDoesNot() {
        fun detail(capped: Boolean) = PreClaimWording.line(LodgingEstimate(5625, 2, 4000, 281_250, capped, source)).detail
        assertTrue(detail(true).contains("₹2,812.50"))
        assertTrue(detail(true).contains("over the limit"))
        assertFalse(detail(false).contains("over the limit"))
    }

    @Test
    fun aShortTripSaysItIsAShareOfTheDayAndAFullDayDoesNot() {
        fun detail(share: Int) = PreClaimWording.line(PerDayEstimate(PreClaimElement.FOOD, 300, 100_000, 1, share, source)).detail
        assertTrue(detail(30).contains("30%"))
        assertFalse(detail(100).contains("%"))
        assertEquals("1 day at ₹1,000 a day", detail(100))
    }

    @Test
    fun theTransferGrantNamesItsPercentBasicAndDivisor() {
        fun detail(divisor: Int) = PreClaimWording.line(CtgEstimate(119_200, 80, divisor, 149_000, source)).detail
        assertEquals("80% of basic pay ₹1,49,000", detail(1))
        assertEquals("80% of basic pay ₹1,49,000, divided by 3", detail(3))
    }

    @Test
    fun aPerKmLineShowsTheDistanceAndTheExactRate() {
        val line = PreClaimWording.line(PerKmEstimate(PreClaimElement.BAGGAGE_ROAD, 180_063, 6_250, 2881, source))
        assertEquals("2881 km at ₹62.50 a km", line.detail)
        assertEquals("₹1,80,063", line.amount)
    }

    @Test
    fun aRuleGivenInWordsShowsNoAmountAndNoRupeeFigure() {
        val line = PreClaimWording.line(LimitNote(PreClaimElement.LOCAL_TAXI, "AC taxi, up to 50 km a day within the city", source))
        assertNull(line.amount)
        assertTrue(line.detail.contains("up to 50 km a day"))
        assertFalse(line.detail.contains("₹"), "a limit with no amount must not look like one")
        assertNotNull(line.source)
    }

    @Test
    fun aNilByRuleSaysNilAndWhyAndCarriesNoRate() {
        val rule124 = PreClaimWording.line(NotPayable(PreClaimElement.FOOD, NilReason.RULE_124_NO_DA))
        assertEquals("Nil", rule124.amount)
        assertTrue(rule124.detail.contains("Rule 124"))
        assertNull(rule124.source)
        val noSanction = PreClaimWording.line(NotPayable(PreClaimElement.CAR_CONVEYANCE, NilReason.NO_RULE_40_SANCTION))
        assertTrue(noSanction.detail.contains("Rule 40 Note 2"))
    }

    @Test
    fun aGapShowsNoAmountAndTellsTheOfficerWhatToFix() {
        val line = PreClaimWording.line(Unavailable(PreClaimElement.CTG, PreClaimIssue.BASIC_MISSING))
        assertNull(line.amount)
        assertEquals("Enter your basic pay.", line.detail)
    }

    @Test
    fun everyIssueHasItsOwnSentenceSoNoGapIsLeftBlankOrDuplicated() {
        val sentences = PreClaimIssue.entries.map { PreClaimIssueStrings.of(it) }
        assertTrue(sentences.all { it.isNotBlank() })
        assertEquals(sentences.size, sentences.toSet().size, "two issues share a sentence")
    }

    @Test
    fun everyElementAndEveryNoteHasItsOwnWords() {
        assertEquals(PreClaimElement.entries.size, PreClaimElement.entries.map(PreClaimStrings::element).toSet().size)
        assertTrue(PreClaimNote.entries.all { PreClaimStrings.note(it).isNotBlank() })
        assertTrue(NilReason.entries.all { PreClaimStrings.nilReason(it).startsWith("Nil") })
    }

    @Test
    fun theWordsNeverClaimATotalOrAnAdvance() {
        val all = listOf(PreClaimStrings.estimatesNote, PreClaimStrings.estimatesTitle, PreClaimStrings.screenSubtitle).joinToString(" ").lowercase()
        assertTrue(all.contains("estimate"))
        assertTrue(all.contains("portal sets the final advance"))
        assertFalse(all.contains("total"), "the app never shows a total (plan 21, F1)")
    }
}
