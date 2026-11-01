package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a scanned order is data the officer cannot recreate, and the attach flow holds a plaintext scan. So the order and the shelf come
 * back after a process death by id only, a malformed one is cut, and the attach level is never saved (its scan cannot survive), while a
 * lock deletes the scan the attach flow holds.
 */
class GuideNavOrdersTest : BankTestBase() {
    private val bundle = SyntheticGuidePart2.bundle()

    private fun save(state: GuideNavState): List<*> = with(GuideNavStateSaver) { SaverScope { true }.save(state) }!! as List<*>

    @Test
    fun theOrderAndShelfLevelsRoundTripAsIdsOnly() {
        val stack = listOf(Part2Destination.Home, Part2Destination.Bank, Part2Destination.Shelf, Part2Destination.Order("o-1"))

        val saved = save(GuideNavState(stack))

        assertEquals(listOf("part2|home", "part2|bank", "part2|shelf", "part2|order|o-1"), saved)
        assertEquals(listOf(GuideDestination.Home) + stack, GuideNavStateSaver.restore(saved)!!.stack)
    }

    @Test
    fun aMalformedOrderOrShelfIsCutAndNothingAboveItSurvives() {
        for (bad in listOf("part2|shelf|x", "part2|order", "part2|order|", "part2|order|a|b", "part2|order|a/b", "part2|order|../x")) {
            assertEquals(listOf(GuideDestination.Home), GuideNavStateSaver.restore(listOf(bad, "card|RB-T1"))!!.stack, "'$bad'")
        }
    }

    @Test
    fun theAttachLevelIsNeverSavedBecauseItsScanCannotSurviveTheProcess() {
        val stack = listOf(Part2Destination.Home, Part2Destination.Bank, Part2Destination.Attach("e-1"))

        val saved = save(GuideNavState(stack))

        assertEquals(listOf("part2|home", "part2|bank"), saved)
        assertFalse(saved.any { it.toString().contains("attach") || it.toString().contains("e-1") })
    }

    @Test
    fun aBundleThatShipsPartTwoKnowsTheOrderShelfAndAttachLevels() {
        assertTrue(bundle.knows(Part2Destination.Shelf) && bundle.knows(Part2Destination.Attach()))
        assertTrue(bundle.knows(Part2Destination.Order("o-1")))
        assertFalse(bundle.knows(Part2Destination.Order("a b")))
    }

    @Test
    fun aLockDeletesTheScanTheAttachFlowHolds() =
        runTest {
            repository.saveEntry(SyntheticBank.entry("e1"))
            val held = FakeScannedPage(byteArrayOf(1, 2, 3))
            val model = bank()
            val nav = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Bank, Part2Destination.Attach()))
            nav.part2Bank = model
            model.attach.begin()
            model.attach.scan(FakePageScanner(PageScan.Pages(listOf(held))))
            assertTrue(model.attach.holdsScan())

            nav.discardTransientForms()

            assertTrue(held.discarded)
            assertFalse(model.attach.holdsScan())
            assertEquals(0, model.attach.state.value.pages)
        }
}
