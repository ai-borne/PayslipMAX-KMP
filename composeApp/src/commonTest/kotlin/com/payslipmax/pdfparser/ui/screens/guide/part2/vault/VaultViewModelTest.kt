package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.StoredPart2
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the vault list is where a published order is found again and deleted. Each row says what the officer needs to tell orders apart
 * (number, date, pages, the day it was added), a row the device cannot read is counted but never shown, and a ghost left by a killed scan is
 * cleaned up without touching a real order.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VaultViewModelTest : VaultListTestBase() {
    @Test
    fun anEmptyVaultListsNothing() =
        check { d ->
            val vault = model(d)
            assertEquals(VaultUiState.Empty, vault.state.value)
        }

    @Test
    fun theListWordsEachOrderWithItsNumberDatePageCountAndAddedDay() =
        check { d ->
            saveOrder("a", no = "A/12", date = CalendarDate(2026, 10, 12), pages = 2, draftId = "draft-9")
            val vault = model(d)

            val row = vault.state.value.rows.single()
            assertEquals("a", row.id)
            assertEquals("draft-9", row.draftId)
            assertEquals(Part2VaultStrings.orderTitle("A/12", "12/10/2026"), row.title)
            assertEquals("2 pages · added 14/11/2023", row.line, "added is the creation day (the fixture creates it 1000 ms before the edit)")
            assertTrue(vault.state.value.hasAny)
        }

    @Test
    fun anOrderWithoutItsNumberOrDateSaysWhatIsMissing() =
        check { d ->
            saveOrder("n", no = "N/1", date = null, updatedAt = 3)
            saveOrder("u", no = null, date = null, updatedAt = 2)
            val vault = model(d)

            assertEquals(listOf(Part2VaultStrings.orderNumberOnly("N/1"), Part2VaultStrings.numberNotTyped), vault.state.value.rows.map { it.title })
        }

    @Test
    fun theRowsKeepTheRepositorysNewestEditFirst() =
        check { d ->
            saveOrder("old", updatedAt = 10)
            saveOrder("new", updatedAt = 20)
            assertEquals(listOf("new", "old"), model(d).state.value.rows.map { it.id })
        }

    @Test
    fun theAddedDayIsTheDayWhereTheDeviceIs() =
        check { d ->
            saveOrder("o")
            val utc = model(d).state.value.rows.single().line
            val india = model(d, offset = 19_800_000L).state.value.rows.single().line
            assertEquals(Part2VaultStrings.orderRowLine(0, "14/11/2023"), utc)
            assertEquals(Part2VaultStrings.orderRowLine(0, "15/11/2023"), india, "22:13 UTC on the 14th is already the 15th at +5:30")
        }

    @Test
    fun anOrderTheDeviceCannotReadIsCountedNeverListed() =
        check { d ->
            saveOrder("good")
            val unreadable =
                object : Part2Repository by repo {
                    override fun observeOrders(): Flow<StoredPart2<VaultOrder>> = repo.observeOrders().map { StoredPart2(it.items, unreadable = 2) }
                }
            val vault = model(d, unreadable)
            assertTrue(vault.state.value.unreadable)
            assertEquals(listOf("good"), vault.state.value.rows.map { it.id })
            assertFalse(model(d).state.value.unreadable)
        }

    // ---- scanning a new order ----

    @Test
    fun deletingAnOrderRemovesItAndItsPages() =
        check { d ->
            saveOrder("a", pages = 2)
            saveOrder("b")
            val vault = model(d)

            vault.delete("a")
            advanceUntilIdle()

            assertEquals(listOf("b"), vault.state.value.rows.map { it.id })
            assertEquals(emptyList(), repo.pages("a"))
        }

    @Test
    fun theListCountsPagesAnOrderGainedAfterItWasSaved() =
        check { d ->
            saveOrder("a", pages = 1)
            val vault = model(d)
            assertEquals(Part2VaultStrings.orderRowLine(1, "14/11/2023"), vault.state.value.rows.single().line)

            val order = vault.open("a")
            advanceUntilIdle()
            order.addPages(scanner(pages(2)))
            advanceUntilIdle()

            assertEquals(Part2VaultStrings.orderRowLine(3, "14/11/2023"), vault.state.value.rows.single().line, "page changes do not touch the order row, so the model refreshes the count itself")
            order.close()
        }

    @Test
    fun openingTheVaultClearsWhatAKilledScanLeftBehind() =
        check { d ->
            val vault = model(d)
            val scan = scanner()
            vault.opened(scan)
            advanceUntilIdle()
            assertEquals(1, scan.leftoversDiscarded)
        }

    @Test
    fun openingTheVaultRemovesGhostOrdersButKeepsEveryRealOne() =
        check { d ->
            saveOrder("ghost", no = null, date = null)
            saveOrder("scanned", no = null, date = null, pages = 1)
            saveOrder("typed", no = "N/1", date = null)
            saveOrder("dated", no = null, date = CalendarDate(2026, 1, 1))
            val vault = model(d)

            vault.opened(scanner())
            advanceUntilIdle()

            assertEquals(setOf("scanned", "typed", "dated"), vault.state.value.rows.map { it.id }.toSet(), "only an order with no page and nothing typed is a ghost")
        }

    @Test
    fun theStateHidesTheOrdersInAString() =
        check { d ->
            saveOrder("secret", no = "MARKER_ORDER_NO")
            val text = model(d).state.value.toString()
            assertFalse("MARKER_ORDER_NO" in text || "secret" in text, text)
        }
}
