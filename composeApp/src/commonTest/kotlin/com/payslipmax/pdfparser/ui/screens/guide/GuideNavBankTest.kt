package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the Bank's levels live in the Guide's own stack, so back, the tab re-tap, a lock and a restore after process death must treat them like
 * any other level. Only ids are saved, never a casualty, a date or a word; an entry the store no longer has cuts the stack there; an entry
 * that was never saved cannot come back; and a lock drops whatever was typed in an open form.
 */
class GuideNavBankTest : BankTestBase() {
    private val bundle = SyntheticGuidePart2.bundle()
    private val stack = listOf(Part2Destination.Home, Part2Destination.Bank, Part2Destination.Entry("e-1"))

    private fun save(state: GuideNavState): List<*> = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!! as List<*>

    @Test
    fun theBankLevelsRoundTripAsPlainStringsWithIdsOnly() {
        val saved = save(GuideNavState(stack))
        assertEquals(listOf("part2|home", "part2|bank", "part2|entry|e-1"), saved)
        assertEquals(listOf(GuideDestination.Home) + stack, GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aMalformedBankEntryIsCutAndNothingAboveItSurvives() {
        for (bad in listOf("part2|bank|x", "part2|entry", "part2|entry|", "part2|entry|a|b", "part2|entry|a/b", "part2|entry|../x")) {
            assertEquals(listOf(GuideDestination.Home), GuideNavStateSaver.restore(listOf(bad, "card|RB-T1"))!!.stack, "'$bad'")
        }
    }

    @Test
    fun aBundleThatShipsPartTwoKnowsTheBankAndAnyPlainEntryId() {
        assertTrue(bundle.knows(Part2Destination.Bank))
        assertTrue(bundle.knows(Part2Destination.Entry("e-1")))
        assertFalse(bundle.knows(Part2Destination.Entry("a b")))
    }

    @Test
    fun anEntryTheStoreNoLongerHasCutsTheStackThereAndKeepsWhatIsBelow() {
        val nav = GuideNavState(stack + Part2Destination.Code("FIRSTCODE"))

        nav.retainKnownEntries { false }

        assertEquals(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Bank), nav.stack)
        val kept = GuideNavState(stack)
        kept.retainKnownEntries { true }
        assertEquals(listOf(GuideDestination.Home) + stack, kept.stack)
    }

    @Test
    fun aNewEntryThatWasNeverSavedIsNotWrittenToTheSavedStack() =
        runTest {
            val model = bank()
            val id = model.beginNew("FIRSTCODE")!!
            val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Code("FIRSTCODE"), Part2Destination.Entry(id))).also { it.part2Bank = model }

            assertEquals(listOf("part2|home", "part2|code|FIRSTCODE"), save(nav), "after process death the officer is back on the casualty page")
            model.markSaved(id)
            assertEquals("part2|entry|$id", save(nav).last())
        }

    @Test
    fun aLockDropsTheOpenEntryFormButKeepsThePlace() =
        runTest {
            val model = bank()
            val id = model.beginNew("FIRSTCODE")!!
            val editor = model.openEditor(id)!!
            editor.setNote("private words")
            val nav = GuideNavState(listOf(Part2Destination.Bank, Part2Destination.Entry(id))).also { it.part2Bank = model }

            nav.discardTransientForms()

            assertEquals("", model.openEditor(id)!!.state.value.input.note)
            assertEquals(listOf(GuideDestination.Home, Part2Destination.Bank, Part2Destination.Entry(id)), nav.stack)
        }

    @Test
    fun aLockWithNoBankModelBoundIsHarmless() {
        GuideNavState(stack).discardTransientForms()
    }
}
