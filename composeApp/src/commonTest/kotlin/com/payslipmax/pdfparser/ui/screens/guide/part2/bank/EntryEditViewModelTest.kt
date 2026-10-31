package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.EntryProblem
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the form must write nothing until the officer taps Add, must refuse what the casualty's own columns refuse, must keep an existing
 * entry's order link and creation time when it is edited, and must forget everything on a lock. A full Bank and a failed write are
 * answered in words, never by losing the form.
 */
class EntryEditViewModelTest : BankTestBase() {
    private suspend fun newForm(code: String = "FIRSTCODE"): EntryEditViewModel {
        val model = bank()
        return model.openEditor(model.beginNew(code)!!)!!
    }

    private fun EntryEditViewModel.fillValid() {
        setFrom(CalendarDate(2026, 3, 1))
        setData(1, "Ravi")
    }

    @Test
    fun nothingIsWrittenUntilAddAndAnIncompleteFormIsRefusedByField() =
        runTest {
            val editor = newForm()
            editor.setNote("a note")

            var outcome: SaveOutcome? = null
            editor.save { outcome = it }

            assertEquals(SaveOutcome.INVALID, outcome)
            assertEquals(EntryProblem.MISSING, editor.state.value.problems[0])
            assertEquals(EntryProblem.MISSING, editor.state.value.problems[3])
            assertNull(repository.entry(editor.id))
        }

    @Test
    fun aCompleteFormIsSavedAsANewEntryNotAttachedToAnyOrder() =
        runTest {
            val editor = newForm()
            editor.fillValid()

            var outcome: SaveOutcome? = null
            editor.save { outcome = it }

            assertEquals(SaveOutcome.SAVED, outcome)
            val saved = repository.entry(editor.id)!!
            assertEquals(Triple("FIRSTCODE", CalendarDate(2026, 3, 1), null), Triple(saved.code, saved.from, saved.orderId))
            assertEquals(Pair(now, now), Pair(saved.createdAt, saved.updatedAt))
        }

    @Test
    fun problemsClearAsTheOfficerFixesThemAfterAFirstAttempt() =
        runTest {
            val editor = newForm()
            editor.save { }
            assertTrue(editor.state.value.problems.isNotEmpty())

            editor.fillValid()

            assertEquals(emptyMap(), editor.state.value.problems)
        }

    @Test
    fun editingKeepsTheOrderLinkAndTheCreationTimeAndMovesTheEditTime() =
        runTest {
            repository.saveOrder(SyntheticBank.order("o1"))
            repository.saveEntry(SyntheticBank.entry("e1", updatedAt = 500, orderId = "o1", code = "FIRSTCODE"))
            val editor = bank().openEditor("e1")!!
            now = 9_000L

            editor.setData(1, "Changed")
            var outcome: SaveOutcome? = null
            editor.save { outcome = it }

            assertEquals(SaveOutcome.SAVED, outcome)
            val saved = repository.entry("e1")!!
            assertEquals(Triple("o1", 0L, 9_000L), Triple(saved.orderId, saved.createdAt, saved.updatedAt))
            assertEquals("Changed", saved.data[1])
        }

    @Test
    fun overLongTextIsCutOneOverTheLimitAndRefused() =
        runTest {
            val editor = newForm()
            editor.fillValid()

            editor.setNote("x".repeat(10_000))

            assertEquals(Part2Limits.TEXT_MAX + 1, editor.state.value.input.note.length)
            var outcome: SaveOutcome? = null
            editor.save { outcome = it }
            assertEquals(SaveOutcome.INVALID, outcome)
        }

    @Test
    fun aFullBankAndAFailedWriteAreAnsweredNotLost() =
        runTest {
            val editor = newForm()
            editor.fillValid()
            repeat(Part2Limits.ENTRIES_MAX) { repository.saveEntry(SyntheticBank.entry("fill-$it")) }

            var outcome: SaveOutcome? = null
            editor.save { outcome = it }

            assertEquals(SaveOutcome.FULL, outcome)
            assertEquals("Ravi", editor.state.value.input.data[1], "the form is still there")
        }

    @Test
    fun deleteRemovesTheEntry() =
        runTest {
            repository.saveEntry(SyntheticBank.entry("e1", code = "FIRSTCODE"))
            var done = false

            bank().openEditor("e1")!!.delete { done = true }

            assertTrue(done)
            assertNull(repository.entry("e1"))
        }

    @Test
    fun closeBlanksTheFormForALock() =
        runTest {
            val editor = newForm()
            editor.fillValid()
            editor.setNote("private")

            editor.close()

            assertEquals(emptyMap(), editor.state.value.problems)
            assertNull(editor.state.value.input.from)
            assertEquals("", editor.state.value.input.note)
        }
}
