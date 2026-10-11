package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Permanent-duty estimates end to end: the shipped figures, the officer's [GuideProfile] and the worksheet answers go in, one
 * outcome per element comes out. The sample is the owner's issued advance voucher (basic 149000, 2881 km, CTG 119200). The
 * portal's advance total could not be reproduced (plan 21, F1), so the calculator must never offer a total.
 */
class PreClaimCalculatorTest {
    private val figures = SyntheticGuideFigures.figures
    private val guideProfile =
        GuideProfile(2026, 6, "14", 149000.0, 60, null, null, mspAmount = 15500.0, npaAmount = 0.0)

    private fun request(
        profile: PreClaimProfile = PreClaimProfile.from(guideProfile),
        case: CtgCase = CtgCase.NORMAL,
        km: Int? = 2881,
        state: String? = "Assam",
    ) = PermanentDutyRequest(profile, case, km, state)

    @Test
    fun aPermanentDutyMoveGetsItsThreeElementsInOrder() {
        assertEquals(
            listOf(PreClaimElement.CTG, PreClaimElement.BAGGAGE_ROAD, PreClaimElement.CAR_CONVEYANCE),
            PreClaimCalculator.permanentDuty(figures, request()).map { it.element },
            "the element list as a whole is pinned in TemporaryDutyCalculatorTest: a total or an advance line needs the owner (plan 21, F1)",
        )
    }

    @Test
    fun theVoucherSampleGivesEachElementFromTheProfileAndTheAnswers() {
        val (ctg, baggage, conveyance) = PreClaimCalculator.permanentDuty(figures, request())

        assertEquals(119200L, assertIs<CtgEstimate>(ctg).rupees)
        assertEquals(180063L, assertIs<PerKmEstimate>(baggage).rupees)
        assertEquals(86430L, assertIs<PerKmEstimate>(conveyance).rupees)
    }

    @Test
    fun anElementWithMissingInputsIsUnavailableWhileTheOthersStillCompute() {
        val outcomes = PreClaimCalculator.permanentDuty(figures, request(km = null, state = null))

        assertEquals(PreClaimIssue.KM_MISSING, assertIs<Unavailable>(outcomes[1]).issue)
        assertEquals(PreClaimIssue.STATE_MISSING, assertIs<Unavailable>(outcomes[2]).issue)
        assertEquals(119200L, assertIs<CtgEstimate>(outcomes[0]).rupees, "CTG does not depend on the km or the state")
    }

    @Test
    fun withNoPayslipEveryValueIsTypedByTheOfficerAndTheSameRulesApply() {
        val manual = PreClaimProfile(level = "14", basicPay = 149000, daPercent = 60, mspAmount = null, npaAmount = null)

        val typed = PreClaimCalculator.permanentDuty(figures, request(profile = manual))
        val fromPayslip = PreClaimCalculator.permanentDuty(figures, request())

        assertEquals(fromPayslip, typed, "a typed profile and the same values from a payslip give identical estimates")
    }

    @Test
    fun withNeitherPayslipNorTypedValuesTheProfileDependentElementsHideInsteadOfGuessing() {
        val empty = PreClaimProfile.from(null)

        val (ctg, baggage, conveyance) = PreClaimCalculator.permanentDuty(figures, request(profile = empty))

        assertEquals(PreClaimIssue.BASIC_MISSING, assertIs<Unavailable>(ctg).issue)
        assertEquals(PreClaimIssue.LEVEL_MISSING, assertIs<Unavailable>(baggage).issue)
        assertIs<PerKmEstimate>(conveyance)
    }

    @Test
    fun noFiguresInTheAppMeansThreeUnavailableElements() {
        val outcomes = PreClaimCalculator.permanentDuty(null, request())

        assertEquals(List(3) { PreClaimIssue.FIGURE_MISSING }, outcomes.map { assertIs<Unavailable>(it).issue })
    }

    @Test
    fun theProfileFromAPayslipRoundsBasicToTheRupeeAndKeepsEveryDefault() {
        val profile = PreClaimProfile.from(guideProfile.copy(basicPay = 149000.4))

        assertEquals(PreClaimProfile("14", 149000L, 60, 15500L, 0L), profile)
    }

    @Test
    fun theProfileFromNoPayslipIsAllNullForTheOfficerToFill() {
        val profile = PreClaimProfile.from(null)

        assertNull(profile.level)
        assertNull(profile.basicPay)
        assertNull(profile.daPercent)
        assertNull(profile.mspAmount)
        assertNull(profile.npaAmount)
    }
}
