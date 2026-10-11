package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPreClaimWorksheet
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimFormState
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The worksheet is a level of the Guide's own stack, so back, the tab re-tap and a restore after process death treat it like any other
 * level. Only the move key is saved: the typed form is never persisted (plan 22, P6), so a restore opens a blank worksheet.
 */
class GuideNavPreClaimTest {
    private val withWorksheet = SyntheticPreClaimWorksheet.bundle()
    private val withoutWorksheet = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle

    private fun save(state: GuideNavState): Any = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!!

    @Test
    fun theWorksheetLevelWithAndWithoutAMoveRoundTripsAsPlainStrings() {
        val stack = listOf(GuideDestination.PreClaim(), GuideDestination.Card("RB-T9"), GuideDestination.PreClaim("TD"))
        val saved = save(GuideNavState(stack)) as List<*>
        assertTrue(saved.all { it is String }, "only plain strings are saved")
        assertEquals(listOf(GuideDestination.Home) + stack, GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aWorksheetEntryWithTooManyPartsIsCutAndNothingAboveItSurvives() {
        val restored = GuideNavStateSaver.restore(listOf("preclaim|PD|extra", "card|RB-T1"))!!
        assertEquals(listOf(GuideDestination.Home), restored.stack)
    }

    @Test
    fun theWorksheetIsKnownOnlyToABundleThatShipsItAndOnlyForAMoveItHas() {
        assertTrue(withWorksheet.knows(GuideDestination.PreClaim()))
        assertTrue(withWorksheet.knows(GuideDestination.PreClaim("LTC")))
        assertFalse(withWorksheet.knows(GuideDestination.PreClaim("XYZ")), "a move the bundle lacks is not a place to restore to")
        assertFalse(withoutWorksheet.knows(GuideDestination.PreClaim()))
    }

    @Test
    fun aRestoredWorksheetLevelIsCutBackWhenTheBundleNoLongerShipsOne() {
        val nav = GuideNavState(listOf(GuideDestination.Area("travel"), GuideDestination.PreClaim("PD")))
        nav.retainKnown(withoutWorksheet)
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Area("travel")), nav.stack)
    }

    @Test
    fun backAndTheTabReTapLeaveTheWorksheetLikeAnyOtherLevel() {
        val nav = GuideNavState(listOf(GuideDestination.Card("RB-T9"), GuideDestination.PreClaim("LTC")))
        assertTrue(nav.pop())
        assertEquals(GuideDestination.Card("RB-T9"), nav.current)
        nav.push(GuideDestination.PreClaim("LTC"))
        nav.popToHome()
        assertEquals(listOf(GuideDestination.Home), nav.stack, "re-tapping the tab goes home")
    }

    private fun worksheet() =
        PreClaimViewModel(
            GuideViewModel(com.payslipmax.pdfparser.testing.FakeGuideRepository(GuideLoadResult.Loaded(withWorksheet)), com.payslipmax.pdfparser.testing.FakeCrashReporter(), dispatcher),
            com.payslipmax.pdfparser.testing.FakeCrashReporter(),
            dispatcher,
        )

    private val dispatcher = kotlinx.coroutines.test.StandardTestDispatcher()

    @Test
    fun theHeldWorksheetIsDiscardedWhenTheStackLeavesTheWorksheetAndNotBefore() {
        val nav = GuideNavState(listOf(GuideDestination.PreClaim("PD")))
        val held = worksheet().also { nav.preClaimModel = it }
        held.setRoadKm("2881")
        nav.push(GuideDestination.Card("RB-T9"))
        assertEquals(held, nav.preClaimModel, "reading a rule card from the worksheet keeps it")
        assertEquals("2881", held.typed.value.roadKm)
        nav.pop()
        assertEquals(held, nav.preClaimModel, "back to the worksheet keeps it")
        nav.pop()
        assertEquals(null, nav.preClaimModel, "leaving the worksheet drops it")
        assertEquals(PreClaimFormState(), held.typed.value, "and empties its form")
    }

    @Test
    fun aLockOrTheTabReTapDiscardsTheHeldWorksheetAndItsForm() {
        val nav = GuideNavState(listOf(GuideDestination.PreClaim("PD")))
        val held = worksheet().also { nav.preClaimModel = it }
        held.setRoadKm("100")
        nav.discardPreClaim()
        assertEquals(null, nav.preClaimModel)
        assertEquals(PreClaimFormState(), held.typed.value)
        val again = worksheet().also { nav.preClaimModel = it }
        again.setRoadKm("200")
        nav.popToHome()
        assertEquals(PreClaimFormState(), again.typed.value)
    }

    @Test
    fun theHeldWorksheetIsNeverWrittenToSavedState() {
        val nav = GuideNavState(listOf(GuideDestination.PreClaim("PD")))
        nav.preClaimModel = worksheet().also { it.setRoadKm("2881") }
        val saved = save(nav) as List<*>
        assertEquals(listOf("preclaim|PD"), saved.map { it.toString() }.filter { it.startsWith("preclaim") })
        assertEquals(null, GuideNavStateSaver.restore(saved)!!.preClaimModel)
    }
}
