package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: the order number and date are the only things the officer types about a published order, and the slip quotes them. Every edit
 * goes through this editor so `Part2Limits` holds whatever the screen sends, and the order's edit time only moves when something changed
 * (it breaks the tie between two orders on one date, so typing the same number again must not reorder them).
 */
class VaultOrderEditorTest {
    private val start = VaultOrderEditor.blank("order-1", "draft-1", now = 100L)

    @Test
    fun aNewOrderIsAScanWithNothingTypedYet() {
        assertEquals(VaultOrder("order-1", "draft-1", null, null, 100L, 100L), start)
        assertNull(VaultOrderEditor.blank("order-2", null, 5L).draftId, "an order scanned from the vault has no draft")
    }

    @Test
    fun aTypedNumberIsKeptAndMarksTheEditTime() {
        val edited = VaultOrderEditor.setNumber(start, "A/12/2026", now = 200L)
        assertEquals("A/12/2026", edited.orderNo)
        assertEquals(200L, edited.updatedAt)
        assertEquals(100L, edited.createdAt, "creation time never moves")
        assertEquals("draft-1", edited.draftId)
    }

    @Test
    fun aNumberLongerThanTheLimitIsCutAtTheLimit() {
        val edited = VaultOrderEditor.setNumber(start, "n".repeat(Part2Limits.ORDER_NO_MAX + 5), now = 200L)
        assertEquals(Part2Limits.ORDER_NO_MAX, edited.orderNo?.length)
        assertEquals(Part2Limits.ORDER_NO_MAX, VaultOrderEditor.setNumber(start, "n".repeat(Part2Limits.ORDER_NO_MAX), now = 200L).orderNo?.length, "exactly the limit is kept whole")
    }

    @Test
    fun aBlankNumberIsNoNumber() {
        val typed = VaultOrderEditor.setNumber(start, "N/1", now = 200L)
        assertNull(VaultOrderEditor.setNumber(typed, "", now = 300L).orderNo)
        assertNull(VaultOrderEditor.setNumber(typed, "   ", now = 300L).orderNo, "spaces alone are not a number")
    }

    @Test
    fun typingTheSameNumberAgainChangesNothing() {
        val typed = VaultOrderEditor.setNumber(start, "N/1", now = 200L)
        assertEquals(typed, VaultOrderEditor.setNumber(typed, "N/1", now = 999L))
        assertEquals(start, VaultOrderEditor.setNumber(start, "", now = 999L), "clearing a number that is already clear changes nothing")
    }

    @Test
    fun aDateOnTheCalendarIsKeptAndADateOffItIsRefused() {
        val dated = VaultOrderEditor.setDate(start, CalendarDate(2026, 2, 28), now = 200L)
        assertEquals(CalendarDate(2026, 2, 28), dated.orderDate)
        assertEquals(200L, dated.updatedAt)
        assertEquals(dated, VaultOrderEditor.setDate(dated, CalendarDate(2026, 2, 31), now = 300L), "31 February changes nothing")
    }

    @Test
    fun aDateCanBeClearedAndSettingItAgainChangesNothing() {
        val dated = VaultOrderEditor.setDate(start, CalendarDate(2026, 3, 1), now = 200L)
        assertEquals(dated, VaultOrderEditor.setDate(dated, CalendarDate(2026, 3, 1), now = 999L))
        val cleared = VaultOrderEditor.setDate(dated, null, now = 300L)
        assertNull(cleared.orderDate)
        assertEquals(300L, cleared.updatedAt)
        assertEquals(start, VaultOrderEditor.setDate(start, null, now = 999L))
    }

    @Test
    fun aNumberWithMarkupIsNamedSoTheScreenCanSayWhy() {
        // why: it is stored as typed (a half-typed value must save), but VaultOrders will not quote it, so the officer is told.
        val markup = VaultOrderEditor.setNumber(start, "N<1>", now = 200L)
        assertEquals(DraftIssueReason.MARKUP, VaultOrderEditor.numberIssue(markup))
        assertEquals(DraftIssueReason.MARKUP, VaultOrderEditor.numberIssue(VaultOrderEditor.setNumber(start, "N/1 > 2", now = 200L)), "a lone > is refused too")
        assertNull(VaultOrderEditor.numberIssue(VaultOrderEditor.setNumber(start, "N/12", now = 200L)))
        assertNull(VaultOrderEditor.numberIssue(start), "no number is not a refusal: it is just not typed yet")
    }

    @Test
    fun anOrderIsLinkedToADraftByItsIdAndTheEditTimeMoves() {
        val linked = VaultOrderEditor.setDraft(VaultOrderEditor.blank("order-1", null, now = 100L), "draft-7", now = 200L)
        assertEquals("draft-7", linked.draftId)
        assertEquals(200L, linked.updatedAt)
        assertEquals(100L, linked.createdAt)
    }

    @Test
    fun linkingToAnotherDraftReplacesTheOldOneAndNullUnlinks() {
        val first = VaultOrderEditor.setDraft(start, "draft-7", now = 200L)
        assertEquals("draft-8", VaultOrderEditor.setDraft(first, "draft-8", now = 300L).draftId)
        val unlinked = VaultOrderEditor.setDraft(first, null, now = 300L)
        assertNull(unlinked.draftId)
        assertEquals(300L, unlinked.updatedAt)
    }

    @Test
    fun linkingToWhatItIsAlreadyLinkedToChangesNothing() {
        assertEquals(start, VaultOrderEditor.setDraft(start, "draft-1", now = 999L), "the fixture order already belongs to draft-1")
        val free = VaultOrderEditor.blank("order-2", null, now = 100L)
        assertEquals(free, VaultOrderEditor.setDraft(free, null, now = 999L))
    }

    @Test
    fun anIdTheAppDidNotMakeIsNeverLinked() {
        for (bad in listOf("", "a b", "<x>", "a|b", "x".repeat(Part2Limits.ID_MAX + 1))) {
            assertEquals(start, VaultOrderEditor.setDraft(start, bad, now = 999L), "'$bad' is not an id")
        }
    }
}
