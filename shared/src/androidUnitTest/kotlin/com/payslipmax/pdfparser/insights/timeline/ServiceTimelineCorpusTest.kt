package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.parser.corpus.CorpusExpected
import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 1 gate: from the payslips alone, the timeline reconstructs one real officer's known 2017-2026
 * history (de-identified corpus): Level 11 with DNI in July, promotion to Level 12A in Oct 2019, DNI in
 * January afterwards, three R&H postings, higher-rate-city TPTA in 2020-21, and quarters occupancy.
 */
class ServiceTimelineCorpusTest {
    private val timeline: ServiceTimeline =
        ServiceTimelineBuilder.build(
            CorpusFixtures.loadIndex().map { CorpusFixtures.loadExpected(it) }.map { it.toParsedPayslip() },
        )

    private fun at(
        year: Int,
        month: Int,
    ) = timeline.months.single { it.month == PayMonth(year, month) }

    @Test
    fun placesEveryPostSeventhCpcMonthOnTheMatrix() {
        assertTrue(timeline.months.all { it.month >= PayMonth(2017, 6) }, "pre-7th CPC pay must be excluded")
        val unplaced = timeline.months.filter { it.level == null || it.stage == null }
        assertTrue(unplaced.isEmpty(), "Unplaced months: ${unplaced.map { it.month }}")
        // Feb and Mar 2022 have no parsed basic pay in the corpus, so they are excluded, not guessed.
        assertEquals(setOf(PayMonth(2022, 2), PayMonth(2022, 3)), missingMonthsFrom(PayMonth(2017, 6), PayMonth(2026, 4)))
    }

    @Test
    fun reconstructsLevel11ThenPromotionToLevel12AInOctober2019() {
        assertEquals(PayLevel.L11, at(2017, 6).level)
        assertEquals(PayLevel.L11, at(2019, 9).level)
        assertEquals(PayLevel.L12A, at(2019, 10).level)
        assertEquals(PayLevel.L12A, at(2026, 4).level)
        val promotion = timeline.events.single { it.type == TimelineEventType.PROMOTION }
        assertEquals(PayMonth(2019, 10), promotion.month)
    }

    @Test
    fun reconstructsDniInJulyAsLevel11ThenInJanuaryAsLevel12A() {
        val increments = timeline.events.filter { it.type == TimelineEventType.INCREMENT }.map { it.month }
        val expected =
            listOf(PayMonth(2017, 7), PayMonth(2018, 7), PayMonth(2019, 7)) +
                (2020..2026).map { PayMonth(it, 1) }
        assertEquals(expected, increments)
    }

    @Test
    fun readsTheDaRateAppliedEachMonth() {
        assertEquals(4, at(2017, 6).daPercent)
        assertEquals(17, at(2019, 10).daPercent)
        assertEquals(50, at(2024, 3).daPercent)
        assertEquals(60, at(2026, 4).daPercent)
        val missing = timeline.months.filter { it.daPercent == null }
        assertTrue(missing.isEmpty(), "DA rate unreadable in: ${missing.map { it.month }}")
    }

    @Test
    fun findsThreeRiskAndHardshipPostings() {
        val spans = timeline.postings.filter { it.kind == PostingKind.RISK_HARDSHIP }
        assertEquals(
            listOf(
                PostingSpan(PostingKind.RISK_HARDSHIP, PayMonth(2018, 1), PayMonth(2018, 5)),
                PostingSpan(PostingKind.RISK_HARDSHIP, PayMonth(2018, 8), PayMonth(2019, 7)),
                PostingSpan(PostingKind.RISK_HARDSHIP, PayMonth(2024, 10), PayMonth(2026, 4)),
            ),
            spans,
        )
    }

    @Test
    fun findsHigherRateCityTptaFrom2020ThroughEarly2022() {
        assertEquals(TptaCityClass.OTHER, at(2019, 10).tptaCity)
        for (month in (PayMonth(2020, 1).index..PayMonth(2022, 1).index)) {
            val m = at(month / 12, month % 12 + 1)
            assertEquals(TptaCityClass.HIGHER, m.tptaCity, "${m.month} should be higher-rate-city TPTA")
        }
        assertEquals(TptaCityClass.OTHER, at(2022, 6).tptaCity)
        assertEquals(TptaCityClass.OTHER, at(2026, 4).tptaCity)
    }

    @Test
    fun readsQuartersOccupancyFromTheLicenceFee() {
        assertTrue(at(2017, 12).occupiesQuarters)
        assertTrue(!at(2018, 6).occupiesQuarters)
        assertTrue(at(2020, 6).occupiesQuarters)
        assertTrue(!at(2026, 4).occupiesQuarters)
        assertNotNull(timeline.months.firstOrNull { it.occupiesQuarters })
    }

    private fun missingMonthsFrom(
        from: PayMonth,
        to: PayMonth,
    ): Set<PayMonth> {
        val present = timeline.months.map { it.month.index }.toSet()
        return (from.index..to.index).filter { it !in present }.map { PayMonth(it / 12, it % 12 + 1) }.toSet()
    }

    private fun CorpusExpected.toParsedPayslip() =
        ParsedPayslip(
            file = filename,
            year = year,
            monthNum = monthNum,
            monthName = "",
            dateStr = "${monthNum.toString().padStart(2, '0')}/$year",
            officer = officer,
            earnings = earnings,
            deductions = deductions,
            ledgerBalances = LedgerBalances(),
            summary = summary,
            taxAndSavings = taxAndSavings,
        )
}
