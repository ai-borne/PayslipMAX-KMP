package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * WHY: "Check the HRMS draft" is a level of its own above the slip, so back, a restore after process death and the app lock must treat it
 * as they treat the slip: only the draft's random id is saved, a stale or forged id is never known, the draft's model is held while the
 * officer moves between slip and check (and freed when they leave both), and a draft that is gone takes its check level with it.
 */
class GuideNavPart2HrmsTest {
    private val bundle = SyntheticPart2Lookup.bundle()
    private val without = SyntheticGuideBundle.withPart2().copy(part2 = null)
    private val id = "6f0c8a52-9d3e-4b7a-8e61-2c5d7f90a1b3"

    private fun save(state: GuideNavState): Any = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!!

    private fun model() =
        LeaveDraftViewModel(id, SyntheticPart2Templates.lookup, FakePart2Repository(), LeaveEventEditor.start(SyntheticPart2Templates.GRANT), SyntheticPart2Templates.registry(), { 0L }, StandardTestDispatcher())

    @Test
    fun theCheckLevelRoundTripsAsPlainStringsWithTheIdAndNothingElse() {
        val stack = listOf(Part2Destination.Home, Part2Destination.Slip(id), Part2Destination.Hrms(id))
        val saved = save(GuideNavState(stack)) as List<*>

        assertEquals(listOf("part2|home", "part2|slip|$id", "part2|hrms|$id"), saved)
        assertEquals(listOf(GuideDestination.Home) + stack, GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aMalformedCheckEntryIsCutAndNothingAboveItSurvives() {
        for (bad in listOf("part2|hrms", "part2|hrms|", "part2|hrms|a|b")) {
            assertEquals(listOf(GuideDestination.Home), GuideNavStateSaver.restore(listOf(bad, "card|RB-T1"))!!.stack, "'$bad' must not restore, nor the card above it")
        }
    }

    @Test
    fun theCheckLevelIsKnownOnlyWithAPartTwoSectionAndAPlainId() {
        assertTrue(bundle.knows(Part2Destination.Hrms(id)))
        assertFalse(without.knows(Part2Destination.Hrms(id)), "no Part II section, no check level")
        for (bad in listOf("a b", "<x>", "a|b", "x".repeat(Part2Limits.ID_MAX + 1), "../x")) assertFalse(bundle.knows(Part2Destination.Hrms(bad)), "'$bad' is not an id the app made")
    }

    @Test
    fun movingBetweenTheSlipAndTheCheckKeepsTheDraftsModelAndLeavingBothFreesIt() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Slip(id)))
        val open = model().also { nav.part2Draft = it }

        nav.push(Part2Destination.Hrms(id))
        assertSame(open, nav.part2Draft, "slip to check is one draft")
        nav.pop()
        assertSame(open, nav.part2Draft)
        nav.push(Part2Destination.Hrms(id))
        nav.popToHome()
        assertNull(nav.part2Draft, "the Guide tab re-tap leaves the draft from the check level too")
    }

    @Test
    fun theModelIsHeldWhileOnlyTheCheckLevelOfTheDraftIsOnTheStack() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Hrms(id), Part2Destination.Code("ANYCODE")))
        val open = model().also { nav.part2Draft = it }

        nav.pop()

        assertSame(open, nav.part2Draft, "a restored stack can have the check level without a slip under it")
    }

    @Test
    fun cuttingADraftRemovesItsCheckLevelAndOnlyThatDraftsLevels() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Slip(id), Part2Destination.Hrms(id)))
        nav.cutDraft(id)
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home), nav.stack)

        val other = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Hrms("another-id")))
        other.cutDraft(id)
        assertEquals(Part2Destination.Hrms("another-id"), other.current)
    }

    @Test
    fun theLockDropsTheModelButLeavesTheCheckLevelWhereItWas() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Slip(id), Part2Destination.Hrms(id)))
        nav.part2Draft = model()

        nav.discardTransientForms()

        assertNull(nav.part2Draft)
        assertEquals(Part2Destination.Hrms(id), nav.current)
    }
}
