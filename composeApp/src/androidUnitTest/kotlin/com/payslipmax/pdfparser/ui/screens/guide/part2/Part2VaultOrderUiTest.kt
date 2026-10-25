package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertTrue

/**
 * WHY: one published order's screen is where the number and date are typed and pages are read. What is typed is saved, a page is decoded only
 * while it is shown and dropped when hidden or when the screen is left, and an officer whose Premium ended reads and deletes but cannot change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2VaultOrderUiTest : Part2VaultUiBase() {
    @Test
    fun theNumberTypedOnTheOrderScreenIsSavedAndShownInTheList() {
        heldOrder("o1", no = null, date = null)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        typeNumber("A/77")
        back()

        assertEquals("A/77", runBlocking { repository.order("o1") }?.orderNo)
        see(Part2VaultStrings.orderNumberOnly("A/77")).assertIsDisplayed()
    }

    @Test
    fun aMarkupNumberIsKeptAsTypedAndNamed() {
        heldOrder("o1", no = null, date = null)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        typeNumber("<b>")

        see(Part2Strings.fieldIssue(Part2Strings.orderNoLabel, com.payslipmax.pdfparser.part2.domain.DraftIssueReason.MARKUP)).assertIsDisplayed()
    }

    @Test
    fun aPickedDateIsClearedThroughTheCalendarAndSaved() {
        heldOrder("o1", date = CalendarDate(2026, 10, 12))
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        tap("${Part2Strings.orderDateLabel}: 12/10/2026")
        press(PreClaimStrings.dateClear)

        assertNull(runBlocking { repository.order("o1") }?.orderDate)
        see("${Part2Strings.orderDateLabel}: ${PreClaimStrings.dateChoose}").assertIsDisplayed()
    }

    @Test
    fun aLapsedOfficerReadsAnOrderWithoutBoxesOrAddPagesAndStillDeletesIt() {
        heldOrder("o1")
        val nav = show(part2Unlocked = false)
        openPartTwoHome()

        tap(homeRow())

        see(Part2VaultStrings.orderViewOnlyNote).assertIsDisplayed()
        assertFalse(anyTextBox(), "nothing can be typed")
        assertFalse(has(Part2VaultStrings.addPages))
        assertTrue(has("A/12"), "but the number and date can be read")
        assertTrue(has("12/10/2026"))
        press(Part2VaultStrings.deleteOrder)
        press(Part2Strings.deleteConfirm)
        assertNull(runBlocking { repository.order("o1") })
        assertEquals(Part2Destination.Home, nav.current, "deleting leaves the order's screen")
    }

    @Test
    fun aPageIsShownDecodedFromItsBytesAndTheBytesAreZeroWhenItIsDone() {
        heldOrder("o1", pages = 2)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        tap(Part2VaultStrings.viewPage(2))

        assertEquals(1, decodes)
        assertTrue(assertNotNull(handed).all { it == 0.toByte() }, "the viewer got the page and it was wiped after")
        see(Part2VaultStrings.hidePage).assertIsDisplayed()
        press(Part2VaultStrings.hidePage)
        assertFalse(has(Part2VaultStrings.hidePage))
    }

    @Test
    fun aPageThatCannotBeDecodedSaysSo() {
        heldOrder("o1")
        decodable = false
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        tap(Part2VaultStrings.viewPage(1))

        see(Part2VaultStrings.pageUnreadable).assertIsDisplayed()
    }

    @Test
    fun morePagesCanBeAddedUntilTheOrderHoldsTen() {
        heldOrder("o1", pages = 9)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))
        scanOf(1)

        tap(Part2VaultStrings.addPages)

        assertEquals(listOf(1), scanner.limits)
        assertFalse(has(Part2VaultStrings.addPages), "ten pages: no more")
        assertEquals(10, runBlocking { repository.pages("o1") }.size)
        see(Part2VaultStrings.pagesSaved(1)).assertIsDisplayed()
    }

    @Test
    fun anOrderTheDeviceNoLongerHoldsIsLeftForThePartTwoHome() {
        val nav = show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("ghost"))))
        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun anOrderLevelRestoredAfterProcessDeathShowsWhatWasSaved() {
        heldOrder("o1", no = "A/12", date = null)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))
        see("A/12").assertIsDisplayed()
        composeRule.onNodeWithText(Part2Strings.orderNoLabel).assertIsDisplayed()
    }

    @Test
    fun aFreeOfficerWithNoOrderWhoReachesAnOrderLevelIsSentBack() {
        val nav = show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("ghost"))), part2Unlocked = false)
        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun theAddPagesButtonIsDisabledWhileAScanIsOpen() {
        heldOrder("o1")
        scanOf(1)
        scanner.gate = CompletableDeferred()
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        tap(Part2VaultStrings.addPages)
        see(Part2VaultStrings.addPages).assertIsNotEnabled()

        scanner.gate?.complete(Unit)
        settle()
        see(Part2VaultStrings.addPages).assertIsEnabled()
    }

    @Test
    fun aWriteTheDeviceRefusesIsSaidOnTheOrderScreen() {
        heldOrder("o1", no = null, date = null)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))
        repository.orderWriteResult = Part2WriteResult.TOO_MANY

        typeNumber("A")

        see(Part2VaultStrings.saveFailed).assertIsDisplayed()
    }

    @Test
    fun whatAScanOnTheOrderScreenSaysIsShownThereAndCanBeDismissed() {
        heldOrder("o1")
        scanner.next = PageScan.Failed(ScanFailure.UNAVAILABLE)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        tap(Part2VaultStrings.addPages)
        see(Part2VaultStrings.scanNeedsConnection).assertIsDisplayed()
        press(Part2VaultStrings.noticeDismiss)

        assertFalse(has(Part2VaultStrings.scanNeedsConnection))
    }

    @Test
    fun aShownPageIsDroppedFromTheScreenWhenItIsHidden() {
        heldOrder("o1", pages = 2)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))

        tap(Part2VaultStrings.viewPage(2))
        composeRule.onAllNodesWithContentDescription(Part2VaultStrings.pageLabel(2)).assertCountEquals(1)

        press(Part2VaultStrings.hidePage)
        composeRule.onAllNodesWithContentDescription(Part2VaultStrings.pageLabel(2)).assertCountEquals(0)
    }

    @Test
    fun anOrderLevelOpenedDirectlyClearsWhatAKilledScanLeftBehind() {
        heldOrder("o1")
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))
        assertEquals(1, scanner.leftoversDiscarded, "a restored order level never visited the Part II home")
    }

    @Test
    fun leavingTheOrderScreenDuringAScanStopsItAndStoresNothingMore() {
        heldOrder("o1")
        scanOf(1)
        scanner.gate = CompletableDeferred()
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))))
        tap(Part2VaultStrings.addPages)

        back()
        scanner.gate?.complete(Unit)
        settle()

        assertEquals(1, runBlocking { repository.pages("o1") }.size, "the page the order already held is all it holds")
    }

    @Test
    fun aLapsedOfficerReadingAnOrderWithoutANumberIsToldItWasNeverTyped() {
        heldOrder("o1", no = null, date = null)
        show(GuideNavState(listOf(GuideDestination.Home, Part2Destination.Home, Part2Destination.Order("o1"))), part2Unlocked = false)
        see(Part2VaultStrings.numberNotTyped).assertIsDisplayed()
    }
}
