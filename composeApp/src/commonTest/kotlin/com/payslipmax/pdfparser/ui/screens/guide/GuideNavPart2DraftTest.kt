package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticPart2Lookup
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPreClaimWorksheet
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimFormState
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * WHY: a draft's form and slip are Guide levels like any other, so back, a restore after process death and the app lock must treat them
 * so, with two extra promises: only a random id is ever saved (the answers live in the encrypted repository), and the lock drops the
 * in-memory form but leaves the Guide's stack where it was, so the officer comes back to the same draft, read again from the database.
 */
class GuideNavPart2DraftTest {
    private val bundle = SyntheticPart2Lookup.bundle()
    private val without = SyntheticGuideBundle.withPart2().copy(part2 = null)
    private val id = "6f0c8a52-9d3e-4b7a-8e61-2c5d7f90a1b3"

    private fun save(state: GuideNavState): Any = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!!

    private fun model(draftId: String = id) =
        LeaveDraftViewModel(draftId, SyntheticPart2Templates.lookup, FakePart2Repository(), LeaveEventEditor.start(SyntheticPart2Templates.GRANT), SyntheticPart2Templates.registry(), { 0L }, StandardTestDispatcher())

    @Test
    fun theThreeNewLevelsRoundTripAsPlainStringsWithTheIdAndNothingElse() {
        val stack = listOf(Part2Destination.Home, Part2Destination.Draft(id), Part2Destination.Slip(id), Part2Destination.Locked)
        val saved = save(GuideNavState(stack)) as List<*>
        assertEquals(listOf("part2|home", "part2|draft|$id", "part2|slip|$id", "part2|locked"), saved)
        assertEquals(listOf(GuideDestination.Home) + stack, GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aMalformedDraftEntryIsCutAndNothingAboveItSurvives() {
        for (bad in listOf("part2|draft", "part2|draft|", "part2|draft|a|b", "part2|slip", "part2|slip|", "part2|slip|a|b", "part2|locked|x")) {
            assertEquals(listOf(GuideDestination.Home), GuideNavStateSaver.restore(listOf(bad, "card|RB-T1"))!!.stack, "'$bad' must not restore, nor the card above it")
        }
    }

    @Test
    fun aDraftLevelIsKnownOnlyWithAPartTwoSectionAndAPlainId() {
        for (level in listOf(Part2Destination.Draft(id), Part2Destination.Slip(id), Part2Destination.Locked)) {
            assertTrue(bundle.knows(level), "$level")
            assertFalse(without.knows(level), "no Part II section, no draft level: $level")
        }
        for (bad in listOf("a b", "<x>", "a|b", "x".repeat(Part2Limits.ID_MAX + 1), "../x")) {
            assertFalse(bundle.knows(Part2Destination.Draft(bad)), "'$bad' is not an id the app made")
            assertFalse(bundle.knows(Part2Destination.Slip(bad)))
        }
    }

    @Test
    fun aRestoredStackIsCutAtADraftLevelThatCannotBeKnown() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft("a b"), Part2Destination.Slip(id)))
        nav.retainKnown(bundle)
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home), nav.stack)
    }

    @Test
    fun theLockClosesTheFormButKeepsTheStack() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft(id)))
        val open = model().also { nav.part2Draft = it }
        nav.discardTransientForms()
        assertNull(nav.part2Draft)
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Draft(id)), nav.stack, "the officer comes back to the same level")
        open.setNumber(0, "year", "2026")
        assertEquals("", (open.state.value.sections[0].fields.first { it.slotId == "year" }.value as com.payslipmax.pdfparser.ui.screens.guide.part2.draft.FieldValue.Digits).text, "a closed form takes no edits")
    }

    @Test
    fun theLockAlsoDropsTheTaDaWorksheetAsItAlwaysDid() {
        // why: the lock line in App now calls one function for every held form, so it must still drop the worksheet it used to drop alone.
        val nav = GuideNavState(listOf(GuideDestination.PreClaim("PD")))
        val dispatcher = StandardTestDispatcher()
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticPreClaimWorksheet.bundle())), FakeCrashReporter(), dispatcher)
        val held = PreClaimViewModel(guide, FakeCrashReporter(), dispatcher).also { nav.preClaimModel = it }
        held.setRoadKm("100")
        nav.discardTransientForms()
        assertNull(nav.preClaimModel)
        assertEquals(PreClaimFormState(), held.typed.value, "and its form is emptied")
    }

    @Test
    fun leavingBothDraftLevelsClosesTheFormButMovingBetweenThemKeepsIt() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft(id)))
        val open = model().also { nav.part2Draft = it }
        nav.push(Part2Destination.Slip(id))
        assertSame(open, nav.part2Draft, "form to preview is one draft")
        nav.pop()
        assertSame(open, nav.part2Draft)
        nav.pop()
        assertNull(nav.part2Draft, "back to Part II home leaves the draft")
        nav.push(Part2Destination.Draft(id))
        nav.part2Draft = model()
        nav.popToHome()
        assertNull(nav.part2Draft, "the Guide tab re-tap leaves it too")
    }

    @Test
    fun cuttingADraftRemovesItsLevelsAndOnlyThose() {
        val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Slip(id), Part2Destination.Draft(id)))
        nav.cutDraft(id)
        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home), nav.stack)
        val other = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Draft("another-id")))
        other.cutDraft(id)
        assertEquals(Part2Destination.Draft("another-id"), other.current)
        assertNotNull(other.part2Draft ?: Unit)
    }
}
