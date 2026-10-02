package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServiceTimelineBuilderTest {
    private fun payslip(
        year: Int,
        month: Int,
        basic: Double,
        daPercent: Double = 17.0,
        msp: Double = 15500.0,
        tpta: Double = 0.0,
        tptaDa: Double = 0.0,
        riskHardship: Double = 0.0,
        field: Double = 0.0,
        licenseFee: Double = 0.0,
        needsReview: Boolean = false,
    ) = ParsedPayslip(
        file = "t.pdf",
        year = year,
        monthNum = month,
        monthName = "",
        dateStr = "$month/$year",
        officer = Officer("N", "A", "P"),
        earnings =
            Earnings(
                basicPay = basic,
                militaryServicePay = msp,
                dearnessAllowance = ((basic + msp) * daPercent / 100.0).roundToInt().toDouble(),
                transportAllowance = tpta,
                transportAllowanceDa = tptaDa,
                riskHardshipAllowance = riskHardship,
                fieldAllowance = field,
            ),
        deductions = Deductions(licenseFee = licenseFee),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(0.0, 0.0, 0.0),
        taxAndSavings = null,
        needsReview = needsReview,
    )

    private fun build(vararg payslips: ParsedPayslip) = ServiceTimelineBuilder.build(payslips.toList())

    @Test
    fun oneStageRiseInTheSameLevelIsAnIncrementNotAPromotion() {
        val timeline = build(payslip(2018, 6, 82800.0), payslip(2018, 7, 85300.0))
        val event = timeline.events.single()
        assertEquals(TimelineEventType.INCREMENT, event.type)
        assertEquals(PayMonth(2018, 7), event.month)
        assertEquals(listOf(7, 8), timeline.months.map { it.stage })
    }

    @Test
    fun aJumpIntoTheNextLevelIsAPromotionEvenThoughLevel11AlsoHoldsAHigherCell() {
        val timeline = build(payslip(2019, 9, 90500.0), payslip(2019, 10, 121200.0))
        val event = timeline.events.single()
        assertEquals(TimelineEventType.PROMOTION, event.type)
        assertEquals(PayLevel.L11, event.fromLevel)
        assertEquals(PayLevel.L12A, event.toLevel)
        assertEquals(1, timeline.months.last().stage)
    }

    @Test
    fun aCellSharedByTwoLevelsStaysInThePreviousLevel() {
        // 61300 is Level 10 stage 4 and Level 10B stage 1. A Lieutenant gaining one stage is not a Captain.
        val timeline = build(payslip(2018, 1, 59500.0), payslip(2018, 2, 61300.0))
        assertEquals(PayLevel.L10, timeline.months.last().level)
        assertEquals(TimelineEventType.INCREMENT, timeline.events.single().type)
    }

    @Test
    fun aSharedCellWithNoHistoryIsLeftUnplacedRatherThanGuessed() {
        val month = build(payslip(2018, 2, 61300.0)).months.single()
        assertNull(month.level)
        assertNull(month.stage)
    }

    @Test
    fun monthsThatCannotBeTrustedAreExcluded() {
        val timeline =
            build(
                payslip(2018, 1, 85300.0, needsReview = true),
                payslip(2018, 2, 0.0),
                // arrears-inflated basic, not a matrix cell
                payslip(2018, 3, 113470.0),
                // 6th CPC pay
                payslip(2018, 4, 31590.0),
                payslip(2018, 5, 85300.0),
            )
        assertEquals(listOf(PayMonth(2018, 5)), timeline.months.map { it.month })
    }

    @Test
    fun duplicateMonthsAreCountedOnceAndOrderIsChronological() {
        val timeline = build(payslip(2018, 8, 85300.0), payslip(2018, 7, 85300.0), payslip(2018, 8, 85300.0))
        assertEquals(listOf(PayMonth(2018, 7), PayMonth(2018, 8)), timeline.months.map { it.month })
    }

    @Test
    fun daPercentIsTheRateActuallyApplied() {
        assertEquals(58, build(payslip(2025, 10, 144700.0, daPercent = 58.0)).months.single().daPercent)
    }

    @Test
    fun daThatIsNotAWholePercentIsNotReported() {
        // DA carrying folded-in arrears would otherwise show up as a bogus rate.
        assertNull(build(payslip(2025, 10, 144700.0, daPercent = 61.4)).months.single().daPercent)
    }

    @Test
    fun tptaCityIsRecoveredFromTptaThatIncludesItsDa() {
        val higher = build(payslip(2020, 1, 124800.0, tpta = 8424.0)).months.single()
        val other = build(payslip(2019, 10, 121200.0, tpta = 4212.0)).months.single()
        assertEquals(TptaCityClass.HIGHER, higher.tptaCity)
        assertEquals(TptaCityClass.OTHER, other.tptaCity)
    }

    @Test
    fun tptaCityIsRecoveredFromTheBaseWhenTptaDaIsPrintedSeparately() {
        val month = build(payslip(2025, 10, 144700.0, daPercent = 58.0, tpta = 3600.0, tptaDa = 2088.0)).months.single()
        assertEquals(TptaCityClass.OTHER, month.tptaCity)
    }

    @Test
    fun tptaCityIsNullWhenTptaIsAbsentOrNotARecognisedRate() {
        assertNull(build(payslip(2020, 1, 124800.0)).months.single().tptaCity)
        assertNull(build(payslip(2020, 1, 124800.0, tpta = 5000.0)).months.single().tptaCity)
    }

    @Test
    fun quartersOccupancyFollowsTheLicenceFee() {
        val timeline = build(payslip(2018, 1, 85300.0, licenseFee = 748.0), payslip(2018, 2, 85300.0))
        assertEquals(listOf(true, false), timeline.months.map { it.occupiesQuarters })
    }

    @Test
    fun postingSpansAreRunsOfConsecutiveMonthsAndAMissingMonthEndsTheRun() {
        val timeline =
            build(
                payslip(2018, 1, 85300.0, riskHardship = 20300.0),
                payslip(2018, 2, 85300.0, riskHardship = 20300.0),
                payslip(2018, 3, 85300.0),
                payslip(2018, 4, 85300.0, riskHardship = 20300.0),
                // May payslip absent
                payslip(2018, 6, 85300.0, riskHardship = 20300.0),
            )
        assertEquals(
            listOf(
                PostingSpan(PostingKind.RISK_HARDSHIP, PayMonth(2018, 1), PayMonth(2018, 2)),
                PostingSpan(PostingKind.RISK_HARDSHIP, PayMonth(2018, 4), PayMonth(2018, 4)),
                PostingSpan(PostingKind.RISK_HARDSHIP, PayMonth(2018, 6), PayMonth(2018, 6)),
            ),
            timeline.postings,
        )
    }

    @Test
    fun fieldAllowanceFormsItsOwnPostingSpan() {
        val timeline = build(payslip(2018, 1, 85300.0, field = 9000.0), payslip(2018, 2, 85300.0, field = 9000.0))
        assertEquals(listOf(PostingSpan(PostingKind.FIELD, PayMonth(2018, 1), PayMonth(2018, 2))), timeline.postings)
    }
}
