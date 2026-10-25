package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.ui.screens.guide.Part2Host
import com.payslipmax.pdfparser.ui.screens.guide.addPages
import com.payslipmax.pdfparser.ui.screens.guide.scanOrder
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: capture is the one vault action Premium gates, and the gate lives in the host (Round 13: the data layer is ungated). Whatever a screen
 * draws, an officer whose Premium ended must not be able to open the scanner by any route, and an entitled one must.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Part2VaultHostTest : Part2UiBase() {
    private fun host(canCapture: Boolean) = Part2Host(Part2Entry.OPEN, factory(), Part2DraftEntry.OPEN, canCapture, {}, printer, scanner)

    @Test
    fun anEntitledOfficerScansANewOrderForTenPages() {
        host(true).scanOrder(null) {}
        settle()
        assertEquals(listOf(Part2Limits.PAGES_PER_ORDER_MAX), scanner.limits)
    }

    @Test
    fun anOfficerWithoutTheEntitlementCannotOpenTheScanner() {
        host(false).scanOrder("draft-1") {}
        settle()
        assertEquals(emptyList(), scanner.limits)
        assertNull(runBlocking { repository.order("order-1") }, "not even an empty order is made")
    }

    @Test
    fun morePagesCanBeScannedIntoAnOrderOnlyByAnEntitledOfficer() {
        runBlocking { repository.saveOrder(com.payslipmax.pdfparser.testing.SyntheticPart2Drafts.order("o1")) }
        val order = vault.open("o1")
        settle()
        host(false).addPages(order)
        settle()
        assertEquals(emptyList(), scanner.limits)

        host(true).addPages(order)
        settle()
        assertEquals(listOf(Part2Limits.PAGES_PER_ORDER_MAX), scanner.limits)
        order.close()
    }
}
