package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.WaitingAge
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the list the officer reads is derived: status from whether the order exists, age from the day, never stored. The view model must
 * show the entries the store holds, change with the chip, send an entry back to "not attached" when its order is deleted, and report an
 * unreadable row instead of hiding it. A new entry exists only as an id and a casualty until Add: nothing the officer typed is held here.
 */
class BankViewModelTest : BankTestBase() {
    @Test
    fun anEmptyBankIsLoadedAndEmptyWithZeroCounts() =
        runTest {
            val state = bank().state.first { it.loaded }

            assertEquals(emptyList(), state.view.years)
            assertEquals(Triple(0, 0, 0), Triple(state.view.all, state.view.notAttached, state.view.attached))
            assertNull(state.lastBackup)
        }

    @Test
    fun entriesAreListedByStatusAndTheChipFiltersThem() =
        runTest {
            repository.saveOrder(SyntheticBank.order("o1"))
            repository.saveEntry(SyntheticBank.entry("waiting", from = CalendarDate(2026, 4, 1)))
            repository.saveEntry(SyntheticBank.entry("done", orderId = "o1", from = CalendarDate(2026, 5, 1)))
            val model = bank()

            val all = model.state.first { it.loaded }
            assertEquals(listOf("done", "waiting"), all.view.years.single().rows.map { it.entry.id })
            assertEquals(BankStatus.ATTACHED, all.view.years.single().rows.first().status)

            model.setFilter(BankFilter.NOT_ATTACHED)
            val waiting = model.state.first { it.filter == BankFilter.NOT_ATTACHED }
            assertEquals(listOf("waiting"), waiting.view.years.single().rows.map { it.entry.id })
            assertEquals(WaitingAge.Months(6), waiting.view.years.single().rows.single().age, "1 April to 30 October is six whole months")
            assertEquals(Triple(2, 1, 1), Triple(waiting.view.all, waiting.view.notAttached, waiting.view.attached), "counts stay for the whole Bank")
        }

    @Test
    fun deletingAnOrderSendsItsEntriesBackToNotAttachedWithoutTouchingThem() =
        runTest {
            repository.saveOrder(SyntheticBank.order("o1"))
            repository.saveEntry(SyntheticBank.entry("e1", orderId = "o1"))
            val model = bank()
            assertEquals(BankStatus.ATTACHED, model.state.first { it.loaded }.view.years.single().rows.single().status)

            repository.deleteOrder("o1")

            assertEquals(BankStatus.NOT_ATTACHED, model.state.first { it.loaded }.view.years.single().rows.single().status)
            assertEquals("o1", repository.entry("e1")?.orderId, "no write to the entry")
        }

    @Test
    fun anUnreadableRowIsCountedNotHidden() =
        runTest {
            repository.unreadableOrders = 2

            assertEquals(2, bank().state.first { it.loaded }.unreadable)
        }

    @Test
    fun theLastBackupDayIsShownOnceThereHasBeenOne() =
        runTest {
            lastBackup.save(1_788_000_000_000L)

            assertTrue(bank().state.first { it.loaded }.lastBackup != null)
        }

    @Test
    fun aNewEntryIsPendingUntilSavedAndAnUnknownCodeStartsNothing() {
        val model = bank()

        val id = model.beginNew("FIRSTCODE")
        assertEquals("new-1", id)
        assertTrue(model.isPending("new-1"))
        assertNull(model.beginNew("NOSUCHCODE"))
        assertFalse(model.isPending("new-2"))

        model.markSaved("new-1")
        assertFalse(model.isPending("new-1"))
    }

    @Test
    fun anEditorOpensForANewEntryAndForAStoredOneAndNotForAMissingOne() =
        runTest {
            repository.saveEntry(SyntheticBank.entry("stored", code = "SECONDCODE"))
            val model = bank()
            val id = model.beginNew("FIRSTCODE")!!

            val fresh = model.openEditor(id)!!
            assertTrue(fresh.isNew)
            assertEquals("FIRSTCODE", fresh.form.code)
            val stored = model.openEditor("stored")!!
            assertFalse(stored.isNew)
            assertEquals("SECONDCODE", stored.form.code)
            assertNull(model.openEditor("missing"))
        }

    @Test
    fun aLockDropsWhatWasTypedAndKeepsOnlyTheCasualtyOfANewEntry() =
        runTest {
            val model = bank()
            val id = model.beginNew("FIRSTCODE")!!
            model.openEditor(id)!!.setNote("typed before the lock")

            model.discardForms()

            assertTrue(model.isPending(id), "the casualty is still chosen")
            assertEquals("", model.openEditor(id)!!.state.value.input.note, "but nothing typed survives")
        }

    @Test
    fun existsSeesPendingAndStoredEntriesOnly() =
        runTest {
            repository.saveEntry(SyntheticBank.entry("stored"))
            val model = bank()
            val id = model.beginNew("FIRSTCODE")!!

            assertTrue(model.exists("stored") && model.exists(id))
            repository.deleteEntry("stored")
            assertFalse(model.exists("stored"))
        }

    @Test
    fun deleteRemovesTheEntryAndCallsBack() =
        runTest {
            repository.saveEntry(SyntheticBank.entry("e1"))
            var done = false

            bank().delete("e1") { done = true }

            assertTrue(done)
            assertNull(repository.entry("e1"))
        }
}
