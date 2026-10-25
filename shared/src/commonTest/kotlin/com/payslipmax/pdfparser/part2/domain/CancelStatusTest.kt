package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What the vault says about a published order that someone has cancelled (DECISIONS Round 22), worked out from the drafts and orders on the
 * device, never stored. A slip not yet handed over has cancelled nothing; a printed slip is a step; the published cancel order, scanned and
 * linked to its slip, is the proof. All numbers and dates are synthetic.
 */
class CancelStatusTest {
    private val cancelledOn = CalendarDate(2026, 6, 20)
    private val target = VaultOrder("o-target", null, "0123/26", cancelledOn, 1, 1)

    private fun quoting(
        code: String,
        no: String = "0123/26",
        date: CalendarDate = cancelledOn,
    ) = LeaveEventInput(listOf(OccurrenceRequest(code, mapOf("cancelled" to SlotValue.RefValue(OrderRef(no, date))))))

    private fun slip(
        id: String,
        event: LeaveEventInput,
        printedAt: Long? = null,
    ) = Part2Draft(id, event, emptyMap(), 1, 1, printedAt?.let { PrintedSnapshot(it, emptyMap(), emptyList()) })

    private fun published(
        id: String,
        draftId: String?,
        no: String? = "9/26",
        date: CalendarDate? = CalendarDate(2026, 7, 1),
    ) = VaultOrder(id, draftId, no, date, 2, 2)

    private fun status(
        drafts: List<Part2Draft>,
        orders: List<VaultOrder> = emptyList(),
        of: VaultOrder = target,
    ) = CancelStatus.of(of, drafts, orders + of)

    @Test
    fun anOrderNoOneHasTriedToCancelHasNoStatus() {
        assertNull(status(emptyList()))
        assertNull(status(listOf(slip("d", LeaveEventInput(listOf(OccurrenceRequest("GTDAL"))), printedAt = 5))))
    }

    @Test
    fun aCancelSlipNotYetPrintedHasCancelledNothing() {
        // why: an order must not read as cancelled because a draft exists; the clerk has not been handed anything.
        assertNull(status(listOf(slip("d", quoting("CANCEL")))))
    }

    @Test
    fun aPrintedCancelSlipIsAStepAndSaysWhenItWasPrinted() {
        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 50), status(listOf(slip("d", quoting("CANCEL"), printedAt = 50))))
    }

    @Test
    fun theNewestPrintedSlipIsTheOneReported() {
        val drafts = listOf(slip("old", quoting("CANCEL"), printedAt = 50), slip("new", quoting("CANCEL"), printedAt = 90))
        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 90), status(drafts))
    }

    @Test
    fun thePublishedCancelOrderLinkedToItsSlipProvesTheCancellation() {
        val drafts = listOf(slip("d", quoting("CANCEL"), printedAt = 50))
        val proof = published("o-cancel", draftId = "d")

        assertEquals(CancelState.CancelledBy(CancelScope.ALL, OrderRef("9/26", CalendarDate(2026, 7, 1))), status(drafts, listOf(proof)))
    }

    @Test
    fun theProofCountsEvenWhenTheSlipWasNeverMarkedPrinted() {
        // why: the officer may have printed from another device; a published cancel order in the vault is stronger than a print record.
        assertEquals(CancelState.CancelledBy(CancelScope.ALL, OrderRef("9/26", CalendarDate(2026, 7, 1))), status(listOf(slip("d", quoting("CANCEL"))), listOf(published("o", "d"))))
    }

    @Test
    fun aScannedOrderWithoutItsNumberAndDateYetIsNotYetAReference() {
        val drafts = listOf(slip("d", quoting("CANCEL"), printedAt = 50))

        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 50), status(drafts, listOf(published("o", "d", no = null))))
        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 50), status(drafts, listOf(published("o", "d", date = null))))
    }

    @Test
    fun theLatestPublishedCancelOrderIsTheOneNamed() {
        val drafts = listOf(slip("d", quoting("CANCEL"), printedAt = 50))
        val orders = listOf(published("early", "d", "1/26", CalendarDate(2026, 7, 1)), published("late", "d", "2/26", CalendarDate(2026, 8, 1)))

        assertEquals(CancelState.CancelledBy(CancelScope.ALL, OrderRef("2/26", CalendarDate(2026, 8, 1))), status(drafts, orders))
    }

    @Test
    fun aSlipQuotingAnotherOrderOrAnotherDateDoesNotCountForThisOne() {
        assertNull(status(listOf(slip("d", quoting("CANCEL", no = "0124/26"), printedAt = 50))))
        assertNull(status(listOf(slip("d", quoting("CANCEL", date = CalendarDate(2026, 6, 21)), printedAt = 50))))
    }

    @Test
    fun theNumberIsMatchedAsTypedWithoutTheSpacesAroundIt() {
        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 50), status(listOf(slip("d", quoting("CANCEL", no = " 0123/26 "), printedAt = 50))))
    }

    @Test
    fun anOrderLinkedToSomeOtherDraftIsNotProof() {
        val drafts = listOf(slip("d", quoting("CANCEL"), printedAt = 50), slip("other", LeaveEventInput(listOf(OccurrenceRequest("GTDAL")))))

        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 50), status(drafts, listOf(published("o", "other"))))
    }

    @Test
    fun aSlipThatCancelsOneOccurrenceOnlySaysSoAndCancelsNotTheWholeOrder() {
        // why: CANCELS cancels one occurrence only (p.227 note 7), so the order must not read as cancelled outright.
        assertEquals(CancelState.SlipPrinted(CancelScope.ONE, 50), status(listOf(slip("d", quoting("CANCELS"), printedAt = 50))))
        assertEquals(
            CancelState.CancelledBy(CancelScope.ONE, OrderRef("9/26", CalendarDate(2026, 7, 1))),
            status(listOf(slip("d", quoting("CANCELS"), printedAt = 50)), listOf(published("o", "d"))),
        )
    }

    @Test
    fun aCancelOfTheWholeOrderOutranksACancelOfOneOccurrence() {
        val one = slip("one", quoting("CANCELS"), printedAt = 90)
        val all = slip("all", quoting("CANCEL"), printedAt = 50)

        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 50), status(listOf(one, all)))
        assertEquals(CancelState.CancelledBy(CancelScope.ALL, OrderRef("9/26", CalendarDate(2026, 7, 1))), status(listOf(one, all), listOf(published("o", "all"))))
    }

    @Test
    fun aPublishedProofBeatsAPrintedSlipEvenWhenThePrintedOneIsNewer() {
        // why: proof that the clerk published the cancel outranks a print record of another slip, whatever their order.
        val proven = slip("proven", quoting("CANCEL"), printedAt = 50)
        val newer = slip("newer", quoting("CANCEL"), printedAt = 90)

        assertEquals(CancelState.CancelledBy(CancelScope.ALL, OrderRef("9/26", CalendarDate(2026, 7, 1))), status(listOf(newer, proven), listOf(published("o", "proven"))))
    }

    @Test
    fun aSlipHoldingBothKindsOfCancelForTheOrderCoversTheWholeOrder() {
        val both = Part2Draft("both", LeaveEventInput(quoting("CANCELS").requests + quoting("CANCEL").requests), emptyMap(), 1, 1, PrintedSnapshot(50, emptyMap(), emptyList()))

        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 50), status(listOf(both)))
    }

    @Test
    fun anOrderCannotProveItsOwnCancellationByBeingLinkedToTheSlip() {
        // why: linking the cancelled order itself to its cancel slip is a slip of the finger, not a published cancel order.
        val drafts = listOf(slip("d", quoting("CANCEL"), printedAt = 50))

        assertEquals(CancelState.SlipPrinted(CancelScope.ALL, 50), status(drafts, of = target.copy(draftId = "d")))
    }

    @Test
    fun anOrderThatCannotBeQuotedHasNoStatus() {
        // why: a slip can only quote an order by its number and date, so an unnumbered scan can be matched by nothing.
        val unnumbered = VaultOrder("o", null, null, cancelledOn, 1, 1)
        assertNull(status(listOf(slip("d", quoting("CANCEL"), printedAt = 50)), of = unnumbered))
    }
}
