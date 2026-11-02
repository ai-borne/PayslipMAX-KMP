package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the officer must be told in words when the phone is too full to scan (and the camera must not start), and warned while a scan
 * still works, on the attach form where the scan begins. Without the words a refused scan would look like a broken button.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2FreeSpaceUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }
    private val scanner = FakePageScanner(PageScan.Pages(listOf(FakeScannedPage(byteArrayOf(1, 2, 3)))))

    private fun openAttach(): GuideNavState =
        show(GuideNavState(listOf(Part2Destination.Bank, Part2Destination.Attach())), part2Bank = { bank }, bankAccess = BankAccess(true) { }, scanner = scanner)

    @Test
    fun aNearlyFullPhoneSaysSoAndNeverStartsTheScanner() {
        data.space.free = Part2Limits.FREE_SPACE_REFUSE_BYTES - 1
        openAttach()

        tap(OrderStrings.scanButton)

        see(OrderStrings.noSpace()).assertIsDisplayed()
        assertEquals(emptyList(), scanner.limits)
    }

    @Test
    fun aPhoneRunningLowWarnsAndStillScans() {
        data.space.free = Part2Limits.FREE_SPACE_WARN_BYTES - 1
        openAttach()

        tap(OrderStrings.scanButton)

        see(OrderStrings.lowSpace()).assertIsDisplayed()
        see(OrderStrings.pagesReady(1)).assertIsDisplayed()
    }

    @Test
    fun addingPagesToAnOrderOnAPhoneRunningLowWarnsAndStillStoresThem() {
        runBlocking {
            data.repository.saveOrder(SyntheticBank.order("o1"))
            data.repository.saveEntry(SyntheticBank.entry("e1", orderId = "o1"))
        }
        data.space.free = Part2Limits.FREE_SPACE_WARN_BYTES - 1
        show(GuideNavState(listOf(Part2Destination.Bank, Part2Destination.Order("o1"))), part2Bank = { bank }, bankAccess = BankAccess(true) { }, scanner = scanner)

        tap(OrderStrings.addPages)

        see(OrderStrings.lowSpace()).assertIsDisplayed()
        assertEquals(1, runBlocking { data.repository.pages("o1").size })
    }

    @Test
    fun addingPagesToAnOrderOnANearlyFullPhoneSaysSoAndStoresNothing() {
        runBlocking { data.repository.saveOrder(SyntheticBank.order("o1")) }
        data.space.free = Part2Limits.FREE_SPACE_REFUSE_BYTES - 1
        show(GuideNavState(listOf(Part2Destination.Bank, Part2Destination.Order("o1"))), part2Bank = { bank }, bankAccess = BankAccess(true) { }, scanner = scanner)

        tap(OrderStrings.addPages)

        see(OrderStrings.noSpace()).assertIsDisplayed()
        assertEquals(emptyList(), scanner.limits)
        assertEquals(0, runBlocking { data.repository.pages("o1").size })
    }

    @Test
    fun aPhoneWithRoomShowsNeitherLine() {
        openAttach()

        tap(OrderStrings.scanButton)

        assertEquals(0, count(OrderStrings.lowSpace()).fetchSemanticsNodes().size)
        assertEquals(0, count(OrderStrings.noSpace()).fetchSemanticsNodes().size)
    }
}
