package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuidePreClaimField
import com.payslipmax.pdfparser.guide.preclaim.CtgEstimate
import com.payslipmax.pdfparser.guide.preclaim.PreClaimElement
import com.payslipmax.pdfparser.guide.preclaim.PreClaimIssue
import com.payslipmax.pdfparser.guide.preclaim.Unavailable
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPreClaimWorksheet
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The worksheet flow without a screen (plan 22, P6): which questions each move asks, where the profile comes from, and that every
 * typed value is cleaned or checked before it is held. The sums are the calculators' own tests; here the point is the flow around them.
 */
class PreClaimViewModelTest : PreClaimViewModelTestBase() {
    private fun ids(fields: List<GuidePreClaimField>) = fields.map { it.id }

    @Test
    fun eachMoveAsksOnlyTheQuestionsThePortalAsksForIt() =
        test { model ->
            // The portal's temporary-duty form has no Re-Employed or Spouse Employed box (plan 21), so asking them would be inventing a question.
            val pd = model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }
            assertEquals(listOf("spr", "re_employed", "marital_status", "spouse_in_army"), ids(pd.questions))

            val td = model.edit { chooseMove(PreClaimMoveType.TEMPORARY_DUTY) }
            assertEquals(listOf("spr", "marital_status"), ids(td.questions))

            val ltc = model.edit { chooseMove(PreClaimMoveType.LEAVE_TRAVEL) }
            assertEquals(listOf("spr", "re_employed", "marital_status", "spouse_in_army"), ids(ltc.questions))
        }

    @Test
    fun beforeAMoveIsChosenTheWorksheetOffersTheMovesTheBundleHasAndNothingElse() =
        test { model ->
            val frame = model.worksheet()

            assertEquals(PreClaimMoveType.entries, frame.availableMoves)
            assertNull(frame.sheet)
            assertEquals(emptyList(), frame.estimates)
        }

    @Test
    fun profileBoxesStartFromTheLatestPayslipAndEveryOneIsEditable() =
        test { model ->
            val frame = model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }
            assertEquals(PreClaimProfileText("13A", "149000", "46", "15500", "0"), frame.profile, "owner decision 1: the payslip is the default")

            val edited = model.edit { editBasicPay("120000") }
            assertEquals("120000", edited.profile.basicPay)
            assertEquals("13A", edited.profile.level, "an edit to one box leaves the others on the payslip")

            val back = model.edit { resetProfileToPayslip() }
            assertEquals("149000", back.profile.basicPay)
        }

    @Test
    fun withNoPayslipEveryBoxIsEmptyAndTheOfficerTypesAllOfThem() {
        withNoPayslip()
        test { model ->
            val empty = model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }
            assertEquals(PreClaimProfileText("", "", "", "", ""), empty.profile)

            model.edit {
                chooseLevel("13A")
                editBasicPay("149000")
                editDaPercent("46")
            }
            val typed = model.worksheet()

            assertEquals(119200L, assertIs<CtgEstimate>(typed.estimates.first()).rupees, "typed values give the same CTG as the payslip's")
        }
    }

    @Test
    fun theFormReadsThePayslipsOnlyWhileAScreenCollects() =
        runTest(dispatcher) {
            val model = PreClaimViewModel(guide(), crash, dispatcher)
            testScheduler.advanceUntilIdle()
            assertEquals(0, payslips.subscriptions, "a ViewModel nobody observes never decrypts a payslip")
            model.close()
        }

    @Test
    fun choosingAMoveWritesOneBreadcrumbWithTheMoveTypeAndNothingTheOfficerTypes() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.TEMPORARY_DUTY) }
            model.edit { chooseMove(PreClaimMoveType.TEMPORARY_DUTY) }
            model.edit {
                editBasicPay("149000")
                setRoadKm("2881")
                setNightlyRate("4500")
            }

            assertEquals(listOf("preclaim_opened_TD"), crash.logs, "one breadcrumb, once per move chosen")
            assertTrue(crash.keys.isEmpty() && crash.exceptions.isEmpty(), "no key and no exception from the worksheet")
        }

    @Test
    fun changingTheMoveStartsAFreshFormButKeepsTheProfileEdits() =
        test { model ->
            model.edit {
                chooseMove(PreClaimMoveType.PERMANENT_DUTY)
                editBasicPay("120000")
                setRoadKm("500")
                setTick("spr", true)
            }

            val td = model.edit { chooseMove(PreClaimMoveType.TEMPORARY_DUTY) }

            assertEquals("120000", td.profile.basicPay, "the officer's own basic pay is still theirs")
            assertEquals("", td.form.roadKm)
            assertEquals(emptySet(), td.form.ticks)
            assertEquals(listOf("preclaim_opened_PD", "preclaim_opened_TD"), crash.logs)
        }

    @Test
    fun typedNumbersKeepOnlyDigitsAndNeverMoreThanTheLargestAllowedValueNeeds() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.TEMPORARY_DUTY) }

            val frame =
                model.edit {
                    editBasicPay("Rs 1,49,000.50")
                    editDaPercent("4600")
                    setRoadKm("2,881 km")
                    setNightlyRate("12345678")
                    setAwayHours("123")
                    setAwayMinutes("4x5")
                    setNights("99999")
                }

            assertEquals("149000", frame.profile.basicPay, "digits only, six at most (the basic bound is 500000)")
            assertEquals("460", frame.profile.daPercent)
            assertEquals("2881", frame.form.roadKm)
            assertEquals("123456", frame.form.nightlyRate)
            assertEquals("12", frame.form.awayHours)
            assertEquals("45", frame.form.awayMinutes)
            assertEquals("999", frame.form.nights)
        }

    @Test
    fun aNumberInRangeOfTheDigitGuardButOverTheLimitShowsAnIssueInsteadOfBeingTrimmed() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }

            val frame =
                model.edit {
                    setRoadKm("99999")
                    chooseOriginState("Assam")
                }

            val baggage = frame.outcome<Unavailable>(PreClaimElement.BAGGAGE_ROAD)
            assertEquals(PreClaimIssue.KM_OUT_OF_RANGE, baggage.issue, "a slipped digit is said out loud, never silently shortened")
        }

    @Test
    fun anIdTheBundleDoesNotOfferIsIgnoredAndNeverReachesACalculator() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.TEMPORARY_DUTY) }
            val before = model.worksheet().form

            val frame =
                model.edit {
                    chooseLevel("99Z")
                    chooseOriginState("Gujarat")
                    chooseTrRule("tr999")
                    setTick("not_a_field", true)
                    setTick("tr_rule", true)
                }

            assertEquals(before, frame.form, "a made-up level, a near-miss state spelling, an unknown rule and a non-tick id change nothing")
            assertEquals("13A", frame.profile.level)
        }

    @Test
    fun theGuideFailingToLoadOrAnOlderBundleWithNoWorksheetLeavesTheWorksheetUnavailable() {
        test(guide(GuideLoadResult.Failed(GuideLoadError.MALFORMED))) { model ->
            assertEquals(PreClaimUiState.Unavailable, model.state.value)
        }
        test(withBundle(SyntheticGuideBundle.withFigures())) { model ->
            assertEquals(PreClaimUiState.Unavailable, model.state.value, "a bundle from before the worksheet shipped")
        }
    }

    @Test
    fun theOpenWorksheetReadsThePayslipsOnceAndNotOnEveryKeystroke() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }
            model.edit { editBasicPay("1") }
            model.edit { editBasicPay("12") }
            model.edit { setRoadKm("500") }

            assertEquals(1, payslips.subscriptions, "reading and decrypting the payslips is per open screen, never per edit")
            model.close()
        }

    @Test
    fun hoursAboveTwentyFourAndMinutesAboveFiftyNineAreIgnoredSoTheBoxKeepsWhatItHad() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.TEMPORARY_DUTY) }

            val frame =
                model.edit {
                    setAwayHours("7")
                    setAwayHours("79")
                    setAwayMinutes("5")
                    setAwayMinutes("59")
                    setAwayMinutes("99")
                }

            assertEquals("7", frame.form.awayHours, "79 hours is not a duration; the 7 stays")
            assertEquals("59", frame.form.awayMinutes, "99 is ignored, 59 stays")
            assertEquals("24", model.edit { setAwayHours("24") }.form.awayHours, "24 is the limit and is allowed")
        }

    @Test
    fun retryAfterAFailedLoadBringsTheWorksheetBack() {
        val repository = FakeGuideRepository(GuideLoadResult.Failed(GuideLoadError.READ_FAILED))
        test(GuideViewModel(repository, crash, dispatcher, profiles = payslips)) { model ->
            assertEquals(PreClaimUiState.Unavailable, model.state.value)

            repository.result = GuideLoadResult.Loaded(SyntheticPreClaimWorksheet.bundle())
            model.retry()
            testScheduler.advanceUntilIdle()

            assertIs<PreClaimUiState.Ready>(model.state.value)
        }
    }
}
