package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * WHY: a bad scan should not cost the whole order, so a page can be deleted on its own after asking. An order's only page takes the order
 * with it (an order with no page is nothing to keep), and an officer whose Premium ended can delete a page as they can delete an order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2VaultPageDeleteUiTest : Part2VaultUiBase() {
    @Test
    fun aPageIsDeletedAfterAskingAndTheOrderKeepsTheRest() {
        heldOrder("o1", pages = 2)
        orderLevel()

        tap(Part2VaultStrings.deletePage)
        see(Part2VaultStrings.deletePageTitle).assertIsDisplayed()
        see(Part2VaultStrings.deletePageBody).assertIsDisplayed()
        press(Part2Strings.deleteCancel)
        assertEquals(2, runBlocking { repository.pages("o1") }.size, "Cancel keeps both pages")

        tap(Part2VaultStrings.deletePage)
        press(Part2Strings.deleteConfirm)

        assertEquals(1, runBlocking { repository.pages("o1") }.size)
        assertNotNull(runBlocking { repository.order("o1") })
        assertFalse(has(Part2VaultStrings.viewPage(2)), "one page is left, so the second row is gone")
    }

    @Test
    fun deletingTheOnlyPageSaysTheOrderGoesWithItAndLeavesTheScreen() {
        heldOrder("o1", pages = 1)
        val nav = orderLevel()

        tap(Part2VaultStrings.deletePage)
        see(Part2VaultStrings.deleteLastPageBody).assertIsDisplayed()
        press(Part2Strings.deleteConfirm)

        assertNull(runBlocking { repository.order("o1") })
        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun anOfficerWhosePremiumEndedCanStillDeleteAPage() {
        heldOrder("o1", pages = 2)
        orderLevel(unlocked = false)

        tap(Part2VaultStrings.deletePage)
        press(Part2Strings.deleteConfirm)

        assertEquals(1, runBlocking { repository.pages("o1") }.size)
    }
}
