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
import kotlin.test.assertTrue

class PayLineChangeExplainerTest {
    private fun payslip(
        year: Int,
        month: Int,
        basic: Double,
        daPercent: Double = 17.0,
        msp: Double = 15500.0,
        tpta: Double = 3600.0,
        tptaDa: Double = 0.0,
        riskHardship: Double = 0.0,
        field: Double = 0.0,
        hra: Double = 0.0,
        licenseFee: Double = 0.0,
        arrearsDa: Double = 0.0,
        arrearsTptaDa: Double = 0.0,
        arrearsTpta: Double = 0.0,
        npa: Double = 0.0,
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
                houseRentAllowance = hra,
                arrearsDa = arrearsDa,
                arrearsTptaDa = arrearsTptaDa,
                arrearsTpta = arrearsTpta,
                nonPracticingAllowance = npa,
            ),
        deductions = Deductions(licenseFee = licenseFee),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(0.0, 0.0, 0.0),
        taxAndSavings = null,
    )

    private fun explainLast(vararg payslips: ParsedPayslip): List<ChangeExplanation> {
        val history = payslips.toList()
        val timeline = ServiceTimelineBuilder.build(history)
        return PayLineChangeExplainer.explain(history.last(), history, timeline)
    }

    private fun List<ChangeExplanation>.reasonFor(field: String) = single { it.field == field }.reason

    @Test
    fun annualIncrementExplainsTheBasicPayRise() {
        val changes = explainLast(payslip(2018, 6, 82800.0), payslip(2018, 7, 85300.0))
        assertTrue(changes.reasonFor("basicPay")!!.contains("increment", ignoreCase = true))
    }

    @Test
    fun promotionExplainsTheBasicPayRise() {
        val changes = explainLast(payslip(2019, 9, 90500.0), payslip(2019, 10, 121200.0))
        assertTrue(changes.reasonFor("basicPay")!!.contains("Promotion"))
    }

    @Test
    fun aDaRiseOnAnIncrementMonthSaysInPlainWordsThatDaIsAShareOfBasicPay() {
        val changes = explainLast(payslip(2018, 6, 82800.0), payslip(2018, 7, 85300.0))
        assertEquals("Dearness Allowance (DA) is a percentage of your pay, which went up with your annual increment", changes.reasonFor("dearnessAllowance"))
    }

    @Test
    fun aDaRiseOnAPromotionMonthNamesThePromotion() {
        val changes = explainLast(payslip(2019, 9, 90500.0), payslip(2019, 10, 121200.0))
        assertEquals("Dearness Allowance (DA) is a percentage of your pay, which went up with your promotion", changes.reasonFor("dearnessAllowance"))
    }

    @Test
    fun aSingleMonthOfArrearsIsNamedAsOneMonthNotARange() {
        val changes =
            explainLast(
                payslip(2018, 6, 85300.0, daPercent = 17.0),
                payslip(2018, 8, 85300.0, daPercent = 21.0, arrearsDa = 2500.0),
            )
        assertEquals("Dearness Allowance (DA) rate changed from 17% to 21%; these are the arrears for July 2018", changes.reasonFor("arrearsDa"))
    }

    @Test
    fun aDaRateRiseExplainsTheDaAmountChange() {
        val changes = explainLast(payslip(2018, 1, 85300.0, daPercent = 17.0), payslip(2018, 2, 85300.0, daPercent = 21.0))
        assertEquals("Dearness Allowance (DA) rate changed from 17% to 21%", changes.reasonFor("dearnessAllowance"))
    }

    @Test
    fun daArrearsOnTheRiseMonthAreExplainedWithTheEffectiveDateRange() {
        val changes =
            explainLast(
                payslip(2018, 6, 85300.0, daPercent = 17.0),
                payslip(2018, 9, 85300.0, daPercent = 21.0, arrearsDa = 5000.0),
            )
        val reason = changes.reasonFor("arrearsDa")
        assertTrue(reason != null && reason.endsWith("these are the arrears for July to August 2018"), "was: $reason")
    }

    @Test
    fun aPostingChangeExplainsBothTheRiskHardshipStartAndTheTptaDrop() {
        val changes =
            explainLast(
                payslip(2018, 1, 85300.0, tpta = 3600.0),
                payslip(2018, 2, 85300.0, tpta = 0.0, riskHardship = 20300.0),
            )
        assertTrue(changes.reasonFor("riskHardshipAllowance")!!.contains("posting", ignoreCase = true))
        assertTrue(changes.reasonFor("transportAllowance")!!.contains("posting", ignoreCase = true))
    }

    /**
     * WHY: found on a real officer's Nov 2024 payslip. A Risk & Hardship posting began the same month DA
     * rose 50%→53%, and TPTA moved 5400→5508 = exactly 5400 × 1.53/1.50. That is TPTA following DA, not a
     * posting change; labelling it "posting change" would mislead the user about why their pay moved.
     */
    @Test
    fun tptaMovingInProportionToDaIsExplainedAsFollowingDaEvenInAPostingChangeMonth() {
        val changes =
            explainLast(
                payslip(2024, 10, 140500.0, daPercent = 50.0, tpta = 5400.0),
                payslip(2024, 11, 140500.0, daPercent = 53.0, tpta = 5508.0, riskHardship = 21125.0),
            )
        assertEquals("Transport Allowance (TPTA) moves with DA, which changed from 50% to 53%", changes.reasonFor("transportAllowance"))
    }

    @Test
    fun aPostingChangeExplainsBaseTptaArrearsNotLinkedToADaRise() {
        val changes =
            explainLast(
                payslip(2018, 1, 85300.0, tpta = 3600.0),
                payslip(2018, 2, 85300.0, tpta = 0.0, riskHardship = 20300.0),
                payslip(2018, 3, 85300.0, tpta = 0.0, riskHardship = 20300.0, arrearsTpta = 3600.0),
            )
        val reason = changes.reasonFor("arrearsTpta")
        assertTrue(reason != null && reason.contains("posting", ignoreCase = true), "was: $reason")
    }

    @Test
    fun aOneOffTptaArrearsPaymentIsExplainedWithoutNeedingAPostingChange() {
        val changes = explainLast(payslip(2018, 1, 85300.0, arrearsTpta = 3600.0), payslip(2018, 2, 85300.0, arrearsTpta = 0.0))
        assertEquals("Arrears were paid last month and do not repeat", changes.reasonFor("arrearsTpta"))
    }

    @Test
    fun tptaArrearsWithNoPostingChangeIsUnexplained() {
        val changes = explainLast(payslip(2018, 1, 85300.0), payslip(2018, 2, 85300.0, arrearsTpta = 1200.0))
        assertNull(changes.reasonFor("arrearsTpta"))
    }

    @Test
    fun anIncrementExplainsTheNpaRiseThatTracksBasicPay() {
        val changes =
            explainLast(
                payslip(2018, 6, 82800.0, npa = 16560.0),
                payslip(2018, 7, 85300.0, npa = 17060.0),
            )
        assertEquals("Non-Practicing Allowance (NPA) is a percentage of your basic pay, which went up with your annual increment", changes.reasonFor("nonPracticingAllowance"))
    }

    @Test
    fun npaStartingFromZeroIsUnexplainedEligibilityIsNotModeled() {
        val changes = explainLast(payslip(2018, 1, 85300.0, npa = 0.0), payslip(2018, 2, 85300.0, npa = 17060.0))
        assertNull(changes.reasonFor("nonPracticingAllowance"))
    }

    @Test
    fun quartersExplainTheHraDropAndTheLicenceFeeStart() {
        val changes =
            explainLast(
                payslip(2018, 1, 85300.0, hra = 5000.0, licenseFee = 0.0),
                payslip(2018, 2, 85300.0, hra = 0.0, licenseFee = 748.0),
            )
        assertEquals("Government quarters taken", changes.reasonFor("houseRentAllowance"))
        assertEquals("Government quarters taken", changes.reasonFor("licenseFee"))
    }

    @Test
    fun aFieldPostingSpanExplainsTheFieldAllowanceStart() {
        val changes = explainLast(payslip(2018, 1, 85300.0, field = 0.0), payslip(2018, 2, 85300.0, field = 9000.0))
        assertTrue(changes.reasonFor("fieldAllowance")!!.contains("Field posting change"))
    }

    @Test
    fun aChangeWithNoStructuralRuleComesBackUnexplainedRatherThanGuessed() {
        // MSP dropping to zero without a Level 14 promotion has no rule to explain it.
        val changes = explainLast(payslip(2018, 1, 85300.0, msp = 15500.0), payslip(2018, 2, 85300.0, msp = 0.0))
        assertNull(changes.reasonFor("militaryServicePay"))
    }

    @Test
    fun aMonthNeedingReviewIsNotExplained() {
        val history =
            listOf(
                payslip(2018, 1, 85300.0),
                payslip(2018, 2, 85300.0, daPercent = 21.0).copy(needsReview = true),
            )
        val timeline = ServiceTimelineBuilder.build(history)
        assertTrue(PayLineChangeExplainer.explain(history.last(), history, timeline).isEmpty())
    }

    @Test
    fun aTransitionFollowingAnUntrustedMonthFallsBackToTheLastTrustedMonth() {
        // Feb needs review, so it is excluded from the timeline; the Mar transition should explain
        // against Jan (the last trusted month) instead of being skipped, and diff Mar's DA against Jan's
        // — not against Feb's untrustworthy figures.
        val jan = payslip(2018, 1, 85300.0, daPercent = 17.0)
        val feb = payslip(2018, 2, 85300.0, daPercent = 17.0).copy(needsReview = true)
        val mar = payslip(2018, 3, 85300.0, daPercent = 21.0)
        val history = listOf(jan, feb, mar)
        val timeline = ServiceTimelineBuilder.build(history)
        val changes = PayLineChangeExplainer.explain(mar, history, timeline)
        assertEquals("Dearness Allowance (DA) rate changed from 17% to 21%", changes.reasonFor("dearnessAllowance"))
    }

    @Test
    fun theFirstStoredMonthHasNoPreviousToCompareAgainst() {
        val timeline = ServiceTimelineBuilder.build(listOf(payslip(2018, 1, 85300.0)))
        assertTrue(PayLineChangeExplainer.explain(payslip(2018, 1, 85300.0), emptyList(), timeline).isEmpty())
    }

    @Test
    fun explainAllCoversEveryTransitionInTheHistoryNotJustTheLatestOne() {
        val history =
            listOf(
                payslip(2018, 6, 82800.0),
                payslip(2018, 7, 85300.0),
                payslip(2019, 9, 90500.0),
                payslip(2019, 10, 121200.0),
            )
        val timeline = ServiceTimelineBuilder.build(history)
        val changes = PayLineChangeExplainer.explainAll(history, timeline)
        val basicPayChanges = changes.filter { it.field == "basicPay" }
        assertEquals(3, basicPayChanges.size, "One tracked basicPay change per consecutive pair across 4 stored months")
        assertTrue(basicPayChanges.single { it.month == PayMonth(2018, 7) }.reason!!.contains("increment", ignoreCase = true))
        assertTrue(basicPayChanges.single { it.month == PayMonth(2019, 10) }.reason!!.contains("Promotion"))
    }

    // Added in the plain-wording phase: reasons that previously showed enum names, "admissible" or arrows.
    @Test
    fun theTptaCityClassReasonNamesTheCitiesInWordsNotEnumNames() {
        val changes =
            explainLast(
                payslip(2019, 11, 85300.0, tpta = 3600.0 * 1.17),
                payslip(2019, 12, 85300.0, tpta = 7200.0 * 1.17),
            )
        assertEquals("Transport Allowance city class changed from other cities to higher-rate cities", changes.reasonFor("transportAllowance"))
    }

    @Test
    fun mspAbsentFromLevelFourteenIsExplainedWithoutTheWordAdmissible() {
        val changes = explainLast(payslip(2020, 1, 85300.0, msp = 15500.0), payslip(2020, 2, 144200.0, msp = 0.0))
        assertEquals("Military Service Pay is not paid from Level 14", changes.reasonFor("militaryServicePay"))
    }

    @Test
    fun theTptaDaArrearsReasonNamesTheLineItArrearsCovers() {
        val changes =
            explainLast(
                payslip(2018, 6, 85300.0, daPercent = 17.0),
                payslip(2018, 9, 85300.0, daPercent = 21.0, arrearsTptaDa = 600.0, tptaDa = 400.0),
            )
        assertEquals(
            "Dearness Allowance (DA) rate changed from 17% to 21%; these are the arrears of DA on Transport Allowance for July to August 2018",
            changes.reasonFor("arrearsTptaDa"),
        )
    }
}
