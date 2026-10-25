package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.DraftIssueReason
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: one published order is where the officer types its number and date (the two things a slip quotes). What they type is saved as they
 * go, never once per key; a deleted order never comes back from a write that was still waiting; leaving the screen keeps what was typed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VaultOrderViewModelTest : VaultOrderTestBase() {
    @Test
    fun anOrderIsLoadedWithItsNumberDateAndPages() =
        check { d ->
            stored(no = "A/12", date = CalendarDate(2026, 10, 12), pages = 2)
            repo.draft("draft-1")
            val state = model(d).state.value

            assertEquals(VaultOrderStatus.READY, state.status)
            assertEquals("A/12", state.orderNo)
            assertEquals(CalendarDate(2026, 10, 12), state.orderDate)
            assertEquals(listOf(Part2VaultStrings.pageLabel(1), Part2VaultStrings.pageLabel(2)), state.pages.map { it.label })
            assertEquals(listOf(1, 2), state.pages.map { it.number })
            assertTrue(state.canAddPages)
            assertNull(state.numberError)
            assertFalse(state.saveFailed)
        }

    @Test
    fun anOrderTheRepositoryDoesNotHoldIsGone() =
        check { d ->
            assertEquals(VaultOrderStatus.GONE, model(d, "nope").state.value.status)
        }

    @Test
    fun anOrderNotLoadedYetIsLoading() =
        check { d ->
            stored()
            val vault = VaultOrderViewModel("order-1", repo, encoder, {}, {}, { 0L }, d)
            assertEquals(VaultOrderStatus.LOADING, vault.state.value.status)
        }

    @Test
    fun theDraftLinkIsOfferedOnlyWhileTheDraftStillExists() =
        check { d ->
            stored(draftId = "draft-1")
            assertNull(model(d).state.value.draftId, "no such draft is saved, so there is nothing to open")

            repo.saveDraft(SyntheticPart2Drafts.emptyDraft("draft-1"))
            assertEquals("draft-1", model(d).state.value.draftId)
            stored("free", draftId = null)
            assertNull(model(d, "free").state.value.draftId)
        }

    @Test
    fun aTypedNumberIsSavedOnceAfterThePauseNotOncePerKey() =
        check { d ->
            stored()
            val vault = model(d)

            "A/12".forEachIndexed { i, _ -> vault.setNumber("A/12".take(i + 1)) }
            assertEquals("A/12", vault.state.value.orderNo)
            advanceTimeBy(299)
            assertEquals(0, repo.orderSaves, "nothing is written before the pause")
            advanceTimeBy(2)
            advanceUntilIdle()

            assertEquals(1, repo.orderSaves, "four keys, one write")
            assertEquals("A/12", repo.order("order-1")?.orderNo)
            assertEquals(9_000L, repo.order("order-1")?.updatedAt)
            vault.close()
        }

    @Test
    fun aPickedDateIsSavedAndTypingNothingNewWritesNothing() =
        check { d ->
            stored()
            val vault = model(d)

            vault.setDate(CalendarDate(2026, 10, 12))
            advanceUntilIdle()
            assertEquals(CalendarDate(2026, 10, 12), repo.order("order-1")?.orderDate)
            assertEquals(1, repo.orderSaves)

            vault.setDate(CalendarDate(2026, 10, 12))
            vault.setNumber("")
            advanceUntilIdle()
            assertEquals(1, repo.orderSaves, "setting what is already there is not a change")
            vault.close()
        }

    @Test
    fun aNumberLongerThanTheLimitIsCutAndNumberWithMarkupIsSavedButNamed() =
        check { d ->
            stored()
            val vault = model(d)

            vault.setNumber("n".repeat(Part2Limits.ORDER_NO_MAX + 9))
            assertEquals(Part2Limits.ORDER_NO_MAX, vault.state.value.orderNo.length)

            vault.setNumber("N<1>")
            advanceUntilIdle()
            assertEquals(Part2Strings.fieldIssue(Part2Strings.orderNoLabel, DraftIssueReason.MARKUP), vault.state.value.numberError)
            assertEquals("N<1>", repo.order("order-1")?.orderNo, "a half-typed value is kept; it is only never quoted")
            vault.setNumber("N/1")
            assertNull(vault.state.value.numberError)
            vault.close()
        }

    @Test
    fun aRefusedWriteIsShownAndClearsWhenTheNextOneWorks() =
        check { d ->
            stored()
            val vault = model(d)
            repo.refuseOrders = Part2WriteResult.TOO_LARGE
            vault.setNumber("A")
            advanceUntilIdle()
            assertTrue(vault.state.value.saveFailed)

            repo.refuseOrders = null
            vault.setNumber("AB")
            advanceUntilIdle()
            assertFalse(vault.state.value.saveFailed)
            vault.close()
        }

    @Test
    fun leavingTheScreenSavesWhatWasTypedLessThanAPauseAgo() =
        check { d ->
            stored()
            val vault = model(d)
            vault.setNumber("A/1")
            vault.close()
            advanceUntilIdle()

            assertEquals("A/1", repo.order("order-1")?.orderNo)
        }

    @Test
    fun leavingTheScreenAfterEverythingWasSavedWritesNothingMore() =
        check { d ->
            stored()
            val vault = model(d)
            vault.setNumber("A/1")
            advanceUntilIdle()
            repo.orderSaves = 0
            vault.close()
            advanceUntilIdle()
            assertEquals(0, repo.orderSaves)
        }

    @Test
    fun deletingTheOrderRemovesItAndItsPagesAndNothingWaitingIsWrittenBack() =
        check { d ->
            stored(pages = 2)
            val vault = model(d)
            vault.setNumber("A/1")
            var done = false

            vault.delete { done = true }
            advanceUntilIdle()

            assertTrue(done)
            assertNull(repo.order("order-1"), "the number waiting to be written must not bring the order back")
            assertEquals(emptyList(), repo.pages("order-1"))
            assertEquals(1, changes, "the list is told its page counts changed")
        }

    // ---- pages ----

    @Test
    fun aClosedScreenTakesNoMoreEdits() =
        check { d ->
            stored()
            val vault = model(d)
            vault.close()
            vault.setNumber("A/1")
            vault.setDate(CalendarDate(2026, 1, 1))
            advanceUntilIdle()

            assertEquals("", vault.state.value.orderNo)
            assertNull(repo.order("order-1")?.orderNo)
        }

    @Test
    fun anEditTypedAfterDeleteDoesNotBringTheOrderBack() =
        check { d ->
            stored()
            val vault = model(d)
            vault.delete {}
            vault.setNumber("A/1")
            advanceUntilIdle()

            assertNull(repo.order("order-1"))
        }

    @Test
    fun leavingTheScreenAfterDeletingItDoesNotWriteWhatWasWaiting() =
        check { d ->
            stored()
            val vault = model(d)
            vault.setNumber("A/1")
            vault.delete {}
            advanceUntilIdle()
            vault.close()
            advanceUntilIdle()

            assertNull(repo.order("order-1"), "the screen's own close after a delete must not write the order back")
        }

    @Test
    fun leavingTheScreenTwiceWritesItOnce() =
        check { d ->
            stored()
            val vault = model(d)
            vault.setNumber("A/1")
            vault.close()
            advanceUntilIdle()
            repo.orderSaves = 0
            vault.close()
            advanceUntilIdle()
            assertEquals(0, repo.orderSaves)
        }
}
