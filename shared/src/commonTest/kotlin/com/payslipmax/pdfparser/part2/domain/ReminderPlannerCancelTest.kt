package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 11 item 31, the 6c hand-off: "none after cancellation" reads `CancelStatus` (a printed cancel slip, then the published cancel
 * order linked to it), never a stored flag. A cancel of the whole order (CANCEL) voids the draft the order was linked to; a cancel of one
 * occurrence (CANCELS) must not stop the order's other reminders; the corrected draft is an ordinary draft. Dates and numbers are synthetic.
 */
class ReminderPlannerCancelTest : ReminderTestBase() {
    private val printed = at(date(2026, 9, 20))
    private val now = at(date(2026, 10, 2))
    private val orderDate = date(2026, 6, 20)
    private val grant = leave(date(2026, 10, 5), date(2026, 10, 9))
    private val leaveDraft = draft("d1", grant, printedAt = printed)
    private val published = order("o1", "d1", no = "0123/26", on = orderDate)
    private val keptKinds = setOf(ReminderKind.RETURN, ReminderKind.YEAR_END)

    private fun cancelAll(printedAt: Long? = printed + 1) = cancelSlip("c1", "CANCEL", "0123/26", orderDate, printedAt)

    private fun cancelOne(printedAt: Long? = printed + 1) = cancelSlip("c2", "CANCELS", "0123/26", orderDate, printedAt)

    private fun kindsOf(
        id: String,
        drafts: List<Part2Draft>,
        orders: List<VaultOrder> = listOf(published),
    ) = plan(drafts, orders, now).all().filter { it.draftId == id }.map { it.kind }.toSet()

    private fun kinds(
        drafts: List<Part2Draft>,
        orders: List<VaultOrder> = listOf(published),
    ) = kindsOf("d1", drafts, orders)

    @Test
    fun withNoCancelThePublishedLeaveStillRemindsOfItsReturnAndItsYearEnd() {
        // why: the baseline the cancel cases are measured against; a leave with a published order keeps these two.
        assertEquals(keptKinds, kinds(listOf(leaveDraft)))
    }

    @Test
    fun aPrintedCancelSlipVoidsTheDraftItsOrderCameFromAtOnce() {
        assertEquals(emptySet(), kinds(listOf(leaveDraft, cancelAll())))
    }

    @Test
    fun thePublishedCancelOrderKeepsItVoid() {
        val proof = order("o2", "c1", no = "77/26", on = date(2026, 7, 9))
        assertEquals(emptySet(), kinds(listOf(leaveDraft, cancelAll()), listOf(published, proof)))
    }

    @Test
    fun aCancelSlipNotYetHandedOverCancelsNothing() {
        assertEquals(keptKinds, kinds(listOf(leaveDraft, cancelAll(printedAt = null))))
    }

    @Test
    fun aCancelOfOneOccurrenceStopsNothingOfTheOrdersOtherReminders() {
        // why: CANCELS covers one occurrence only (note 7 p.227); the leave, its return and its year end are still the officer's to do.
        assertEquals(keptKinds, kinds(listOf(leaveDraft, cancelOne())))
        val proof = order("o2", "c2", no = "77/26", on = date(2026, 7, 9))
        assertEquals(keptKinds, kinds(listOf(leaveDraft, cancelOne()), listOf(published, proof)))
    }

    @Test
    fun aCancelOfTheWholeOrderOutranksACancelOfOneOccurrenceOnTheSameOrder() {
        assertEquals(emptySet(), kinds(listOf(leaveDraft, cancelOne(), cancelAll())))
    }

    @Test
    fun aCancelNamingAnotherOrderVoidsNothing() {
        val other = cancelSlip("c3", "CANCEL", "0999/26", orderDate, printed + 1)
        val otherDate = cancelSlip("c4", "CANCEL", "0123/26", date(2026, 6, 21), printed + 1)
        assertEquals(keptKinds, kinds(listOf(leaveDraft, other, otherDate)))
    }

    @Test
    fun straySpacesAroundTheQuotedNumberDoNotHideTheCancel() {
        // why: matching is by the order's number and date as the clerk will cancel it.
        val spaced = cancelSlip("c5", "CANCEL", " 0123/26 ", orderDate, printed + 1)
        assertEquals(emptySet(), kinds(listOf(leaveDraft, spaced)))
    }

    @Test
    fun aCancelledDraftHasNoAddTheOrderReminderAtAll() {
        // A published order already ends that reminder, so for a draft that was cancelled there is nothing for the cancel itself to add:
        // a cancel needs the order's number and date, which is what "published" means. This pins the combination so a change to either rule
        // that brought the nagging back for a cancelled draft would fail here.
        assertTrue(ReminderKind.ADD_ORDER !in kinds(listOf(leaveDraft, cancelAll())))
        assertTrue(ReminderKind.ADD_ORDER !in kinds(listOf(leaveDraft, cancelOne())))
    }

    @Test
    fun theCancelSlipItselfIsAnOrdinaryPrintedDraftWithItsOwnAddTheOrderSeries() {
        val plan = plan(listOf(leaveDraft, cancelAll()), listOf(published), now)
        assertEquals(6, plan.all().count { it.draftId == "c1" && it.kind == ReminderKind.ADD_ORDER })
        assertEquals(setOf(ReminderKind.ADD_ORDER), kindsOf("c1", listOf(leaveDraft, cancelAll())))
    }

    @Test
    fun thePublishedCancelOrderEndsTheCancelSlipsSeriesButNeverVoidsTheCancelSlip() {
        val proof = order("o2", "c1", no = "77/26", on = date(2026, 7, 9))
        val drafts = listOf(leaveDraft, cancelAll())
        assertEquals(emptySet(), kindsOf("c1", drafts, listOf(published, proof)))
        assertEquals(emptySet(), kinds(drafts, listOf(published, proof)))
    }

    @Test
    fun theCorrectedDraftIsAnOrdinaryDraftWithItsOwnReminders() {
        val corrected = draft("d2", CancelDrafts.redraftEvent(leaveDraft.event).requests.first(), printedAt = printed + 2)
        val drafts = listOf(leaveDraft, cancelAll(), corrected)
        assertEquals(setOf(ReminderKind.RETURN, ReminderKind.YEAR_END, ReminderKind.ADD_ORDER), kindsOf("d2", drafts))
        assertEquals(emptySet(), kinds(drafts))
    }

    @Test
    fun theCorrectedDraftNotYetPrintedHasNoReminders() {
        val corrected = draft("d2", CancelDrafts.redraftEvent(leaveDraft.event).requests.first())
        assertEquals(emptySet(), kindsOf("d2", listOf(leaveDraft, cancelAll(), corrected)))
    }

    @Test
    fun aVoidDraftDoesNotAnswerAnotherDraftsYearEnd() {
        // why: an accumulation order that was itself cancelled never happened, so the year still needs one.
        val accumulation = draft("acc", request("ACCLVE", year(2026)), printedAt = printed)
        val accOrder = order("oa", "acc", no = "0555/26", on = orderDate)
        val cancelAcc = cancelSlip("ca", "CANCEL", "0555/26", orderDate, printed + 1)
        val other = draft("d2", grant, printedAt = printed)
        val plan = plan(listOf(other, accumulation, cancelAcc), listOf(accOrder), now)
        assertEquals(listOf(2026), plan.of(ReminderKind.YEAR_END).map { it.year })
    }

    @Test
    fun aVoidRejoiningDoesNotAnswerTheReturn() {
        val rejoin = draft("r1", request("REJOINAL", on("rejoined_on", date(2026, 10, 10))))
        val rejoinOrder = order("or", "r1", no = "0777/26", on = orderDate)
        val cancelRejoin = cancelSlip("cr", "CANCEL", "0777/26", orderDate, printed + 1)
        val plan = plan(listOf(leaveDraft, rejoin, cancelRejoin), listOf(published, rejoinOrder), now)
        assertEquals(listOf("return:d1"), plan.of(ReminderKind.RETURN).map { it.key })
    }

    @Test
    fun aRejoiningWhoseCancelIsOnlyOneOccurrenceStillAnswersTheReturn() {
        val rejoin = draft("r1", request("REJOINAL", on("rejoined_on", date(2026, 10, 10))))
        val rejoinOrder = order("or", "r1", no = "0777/26", on = orderDate)
        val cancelOneOccurrence = cancelSlip("cr", "CANCELS", "0777/26", orderDate, printed + 1)
        val plan = plan(listOf(leaveDraft, rejoin, cancelOneOccurrence), listOf(published, rejoinOrder), now)
        assertTrue(plan.of(ReminderKind.RETURN).isEmpty())
    }
}
