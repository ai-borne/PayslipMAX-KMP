package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.CtgEstimate
import com.payslipmax.pdfparser.guide.preclaim.LodgingEstimate
import com.payslipmax.pdfparser.guide.preclaim.NotPayable
import com.payslipmax.pdfparser.guide.preclaim.PerDayEstimate
import com.payslipmax.pdfparser.guide.preclaim.PreClaimElement
import com.payslipmax.pdfparser.guide.preclaim.Unavailable
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideProfileProvider
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimMoveType
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimUiState
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimWorksheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The worksheet ViewModel (plan 22, P6) run on the real shipped bundle, on each platform (`GuideBundleAndroidContractTest`,
 * `GuideLoaderIosPerfTest`). The ViewModel reads the bundle's own field and option ids by name, so this is what fails if
 * `preclaim.json` renames one of them: the questions each move asks, the tick that pre-selects the island CTG case, the rule ids,
 * and the owner's claim A and voucher reduced to numbers. Driven by hand on a test scheduler so it needs no `runTest`.
 */
object PreClaimFlowContract {
    private val officer = GuideProfile(2026, 6, "13A", 149000.0, 46, null, null, mspAmount = 15500.0, npaAmount = 0.0)

    fun assertTheWorksheetFlowRunsOnTheShippedBundle(bundle: GuideBundle) {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val crash = FakeCrashReporter()
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), crash, dispatcher, profiles = FakeGuideProfileProvider(officer))
        val model = PreClaimViewModel(guide, crash, dispatcher)
        val collector = CoroutineScope(dispatcher).launch { model.state.collect {} }
        scheduler.advanceUntilIdle()

        fun frame(change: PreClaimViewModel.() -> Unit = {}): PreClaimWorksheet {
            model.change()
            scheduler.advanceUntilIdle()
            return assertIs<PreClaimUiState.Ready>(model.state.value).worksheet
        }

        fun questions(move: PreClaimMoveType) = frame { chooseMove(move) }.questions.map { it.id }

        assertEquals(PreClaimMoveType.entries, frame().availableMoves)
        assertEquals(listOf("spr", "re_employed", "marital_status", "spouse_in_army"), questions(PreClaimMoveType.PERMANENT_DUTY))
        assertEquals(listOf("spr", "marital_status"), questions(PreClaimMoveType.TEMPORARY_DUTY), "the portal's temporary-duty form has no Re-Employed or Spouse Employed box")
        assertEquals(listOf("spr", "re_employed", "marital_status", "spouse_in_army"), questions(PreClaimMoveType.LEAVE_TRAVEL))
        assertEquals(listOf("preclaim_opened_PD", "preclaim_opened_TD", "preclaim_opened_LTC"), crash.logs)

        assertPermanentDuty(frame { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }, ::frame)
        assertTemporaryDuty(::frame)
        assertLeaveTravel(::frame)
        collector.cancel()
        model.close()
    }

    private fun assertPermanentDuty(
        start: PreClaimWorksheet,
        frame: (PreClaimViewModel.() -> Unit) -> PreClaimWorksheet,
    ) {
        assertEquals(34, start.stateChoices.size)
        assertEquals(16, start.levelChoices.size)
        assertEquals("149000", start.profile.basicPay)
        val normal =
            frame {
                setRoadKm("2881")
                chooseOriginState("Assam")
            }
        assertEquals(119200L, assertIs<CtgEstimate>(normal.estimates[0]).rupees, "the owner's voucher: 80% of Rs 1,49,000")
        assertEquals(149000L, assertIs<CtgEstimate>(frame { setTick("andaman_nicobar", true) }.estimates[0]).rupees, "the Andaman Nicobar tick pre-selects the island case")
        assertEquals("tick", start.sheet?.fields?.single { it.id == "andaman_nicobar" }?.kind)
        assertTrue(frame { setTick("spouse_in_army", true) }.estimates[0] is Unavailable, "both spouses serving: CTG is a note")
    }

    private fun assertTemporaryDuty(frame: (PreClaimViewModel.() -> Unit) -> PreClaimWorksheet) {
        val claimA =
            frame {
                chooseMove(PreClaimMoveType.TEMPORARY_DUTY)
                chooseTrRule("tr119")
                setFromDate(CalendarDate(2024, 2, 1))
                setToDate(CalendarDate(2024, 3, 28))
                setNightlyRate("4500")
            }
        assertEquals(57_000L, assertIs<PerDayEstimate>(claimA.estimates.single { it.element == PreClaimElement.FOOD }).rupees)
        assertEquals(256_500L, assertIs<LodgingEstimate>(claimA.estimates.single { it.element == PreClaimElement.LODGING }).rupees, "nights default to the 57 days")
        assertEquals(
            mapOf("basic_pay" to "149000", "npa" to "0", "msp" to "15500", "total_absence" to "57", "food_charges" to "57000", "accommodation" to "256500"),
            claimA.copyValues,
            "each portal box the app can fill, by the bundle's own field id",
        )
        val course = frame { chooseTrRule("tr124") }
        assertTrue(course.estimates.take(3).all { it is NotPayable }, "the option id the flow reads is still Rule 124")
        assertEquals("0", course.copyValues["food_charges"])
    }

    private fun assertLeaveTravel(frame: (PreClaimViewModel.() -> Unit) -> PreClaimWorksheet) {
        val start = frame { chooseMove(PreClaimMoveType.LEAVE_TRAVEL) }
        assertEquals(emptyList(), start.estimates, "leave travel computes nothing")
        assertNull(start.ruleCardId)
        val picker = assertNotNull(start.options["ltc_rule"]).options
        for (option in picker) {
            assertEquals(option.card, frame { chooseTrRule(option.id) }.ruleCardId, "picking ${option.id} links its card")
        }
        assertEquals(8, picker.size)
    }
}
