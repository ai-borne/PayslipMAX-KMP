package com.payslipmax.pdfparser.nav

import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.testing.FakePart2Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * WHY (plan, Navigation and process death): a notification tap carries only a reminder kind and a draft id. The launcher activity is
 * exported, so any app can send it those extras: they are untrusted text, shape-checked when offered and checked against the database
 * when taken. A tap that is forged, stale or about a draft that is gone must do nothing, and a tap that arrives while locked must wait.
 */
class PendingPart2LinkTest {
    private val repository = FakePart2Repository()
    private val pending = PendingPart2Link(repository)

    private suspend fun saveDraft(id: String) = repository.saveDraft(Part2Draft(id, LeaveEventInput(emptyList()), emptyMap(), 1, 1, null))

    @Test
    fun aKnownKindAndAPlainIdAreHeldUntilTaken() =
        runTest {
            saveDraft("draft-1")

            pending.offer("RETURN", "draft-1")

            assertEquals(Part2Link(ReminderKind.RETURN, "draft-1"), pending.link.value)
            assertEquals(Part2Link(ReminderKind.RETURN, "draft-1"), pending.take())
            assertNull(pending.link.value, "taken means gone")
        }

    @Test
    fun everyKindACallerCanNameIsAKindThePlannerKnows() {
        for (kind in ReminderKind.entries) {
            pending.offer(kind.name, "d1")
            assertEquals(kind, pending.link.value?.kind)
        }
    }

    @Test
    fun aKindThatIsNotOneOfOursIsIgnored() {
        for (kind in listOf(null, "", "return", "RETURN ", "OTHER", "ADD_ORDER;DROP")) {
            pending.offer(kind, "d1")
            assertNull(pending.link.value, "kind=$kind")
        }
    }

    @Test
    fun anIdThatCouldCarryAPathOrMarkupIsIgnored() {
        val tooLong = "a".repeat(65)
        for (id in listOf(null, "", " ", "../x", "a/b", "<b>", "d 1", "d1\n", "d1;", tooLong)) {
            pending.offer("RETURN", id)
            assertNull(pending.link.value, "id=$id")
        }
    }

    @Test
    fun aBadOfferDoesNotEraseAGoodOneAndALaterGoodOneReplacesIt() {
        pending.offer("RETURN", "first")

        pending.offer("RETURN", "../x")
        assertEquals("first", pending.link.value?.draftId)

        pending.offer("YEAR_END", "second")
        assertEquals(Part2Link(ReminderKind.YEAR_END, "second"), pending.link.value)
    }

    @Test
    fun aLinkToADraftThatIsGoneIsDroppedWhenTaken() =
        runTest {
            pending.offer("RETURN", "missing")

            assertNull(pending.take())
            assertNull(pending.link.value, "a stale tap is cleared, not kept for later")
        }

    @Test
    fun takingWithNothingPendingGivesNothing() =
        runTest {
            assertNull(pending.take())
        }

    @Test
    fun aLinkIsHeldAcrossAnyNumberOfReadsUntilTaken() =
        runTest {
            saveDraft("draft-1")
            pending.offer("ADD_ORDER", "draft-1")

            assertNotNull(pending.link.value)
            assertNotNull(pending.link.value, "reading does not consume; the lock screen may be up for a long time")
            assertNotNull(pending.take())
        }

    @Test
    fun theTextFormHidesTheDraftId() {
        pending.offer("RETURN", "draft-secret-id")

        assertFalse("draft-secret-id" in pending.link.value.toString())
        assertFalse("draft-secret-id" in pending.toString())
    }
}
