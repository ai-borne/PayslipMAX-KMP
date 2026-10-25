package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the order screen offers "Cancel this order" only when the slip can quote the order (a number and a real date), and says where a
 * cancellation stands as the drafts and orders change, because the cancel slip is printed and the published order scanned while the screen
 * may stay open. Everything is read from the repository, never kept as text. Synthetic orders only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VaultCancelStateTest : VaultOrderTestBase() {
    private val date = CalendarDate(2026, 6, 20)

    private fun cancelSlip(
        id: String = "slip",
        no: String = "0123/26",
        printed: Boolean = true,
    ) = Part2Draft(
        id,
        LeaveEventInput(listOf(OccurrenceRequest("CANCEL", mapOf("cancelled" to SlotValue.RefValue(OrderRef(no, date)))))),
        emptyMap(),
        1,
        1,
        if (printed) PrintedSnapshot(1_700_000_000_000L, emptyMap(), emptyList()) else null,
    )

    @Test
    fun anOrderCanBeCancelledOnlyOnceItsNumberAndDateAreTyped() =
        check { d ->
            stored(no = null, date = null)
            val vault = model(d)
            assertFalse(vault.state.value.quotable)

            vault.setNumber("0123/26")
            assertFalse(vault.state.value.quotable, "a number alone cannot be quoted")
            vault.setDate(date)
            assertTrue(vault.state.value.quotable)
            vault.setNumber("")
            assertFalse(vault.state.value.quotable, "clearing the number takes the offer back")
            vault.close()
        }

    @Test
    fun anOrderNobodyHasTriedToCancelHasNoNote() =
        check { d ->
            stored(no = "0123/26", date = date)
            repo.saveDraft(cancelSlip(printed = false))
            val vault = model(d)

            assertNull(vault.state.value.cancelNote)
            vault.close()
        }

    @Test
    fun aPrintedCancelSlipIsNotedAndNotedLiveWhenItIsPrintedWhileTheScreenIsOpen() =
        check { d ->
            stored(no = "0123/26", date = date)
            val vault = model(d)
            assertNull(vault.state.value.cancelNote)

            repo.saveDraft(cancelSlip())
            advanceUntilIdle()

            assertEquals("Cancel slip printed on 14/11/2023.", vault.state.value.cancelNote)
            vault.close()
        }

    @Test
    fun theScannedCancelOrderLinkedToTheSlipTurnsTheNoteIntoTheProof() =
        check { d ->
            stored(no = "0123/26", date = date)
            repo.saveDraft(cancelSlip())
            val vault = model(d)

            repo.saveOrder(SyntheticPart2Drafts.order("o-cancel", "slip", "9/26", CalendarDate(2026, 7, 1)))
            advanceUntilIdle()

            assertEquals("Cancelled by order 9/26 dated 01/07/2026.", vault.state.value.cancelNote)
            vault.close()
        }

    @Test
    fun changingTheNumberMovesTheOrderAwayFromTheSlipThatQuotedTheOldOne() =
        check { d ->
            stored(no = "0123/26", date = date)
            repo.saveDraft(cancelSlip())
            val vault = model(d)
            assertTrue(vault.state.value.cancelNote != null)

            vault.setNumber("0999/26")
            advanceTimeBy(1_000)
            advanceUntilIdle()

            assertNull(vault.state.value.cancelNote)
            vault.close()
        }

    private fun plainDraft(id: String) = Part2Draft(id, LeaveEventInput(listOf(OccurrenceRequest("GTDAL"))), emptyMap(), 1, 1)

    @Test
    fun anOrderCanBeRedraftedOnlyWhileItsLinkedDraftExistsAndIsNotItselfACancelSlip() =
        check { d ->
            stored(no = "0123/26", date = date, draftId = "src")
            val none = model(d)
            assertFalse(none.state.value.redraftable, "the draft is not on the device")
            none.close()

            repo.saveDraft(plainDraft("src"))
            val plain = model(d)
            assertTrue(plain.state.value.redraftable)
            plain.close()

            repo.saveDraft(cancelSlip(id = "src", printed = false))
            val cancel = model(d)
            assertFalse(cancel.state.value.redraftable, "a cancel slip has no leave order to copy")
            cancel.close()
        }

    @Test
    fun linkingToAnotherDraftChangesWhetherTheOrderCanBeRedrafted() =
        check { d ->
            stored(no = "0123/26", date = date, draftId = null)
            repo.saveDraft(plainDraft("plain"))
            repo.saveDraft(cancelSlip(id = "slip", printed = false))
            val vault = model(d)
            assertFalse(vault.state.value.redraftable)

            vault.setDraft("plain")
            advanceUntilIdle()
            assertTrue(vault.state.value.redraftable)

            vault.setDraft("slip")
            advanceUntilIdle()
            assertFalse(vault.state.value.redraftable)
            vault.close()
        }
}
