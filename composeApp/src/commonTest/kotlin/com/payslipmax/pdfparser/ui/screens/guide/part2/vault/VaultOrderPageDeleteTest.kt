package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.pages.PageScan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a bad scan should not cost the whole order, so one page can be deleted on its own. The order's only page takes the order with it, a page
 * of another order is never touched, and nothing is deleted while a scan is filling the order or after the screen was left.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VaultOrderPageDeleteTest : VaultOrderTestBase() {
    @Test
    fun deletingOnePageKeepsTheOthersRenumbersThemAndTellsTheList() =
        check { d ->
            stored(pages = 3)
            val vault = model(d)
            val ids = vault.state.value.pages.map { it.id }
            var gone = false

            vault.deletePage(ids[1]) { gone = true }
            advanceUntilIdle()

            assertEquals(listOf(ids[0], ids[2]), vault.state.value.pages.map { it.id })
            assertEquals(listOf("Page 1", "Page 2"), vault.state.value.pages.map { it.label })
            assertEquals(2, repo.pages("order-1").size)
            assertFalse(gone, "the order still holds pages")
            assertNotNull(repo.order("order-1"))
            assertEquals(1, changes)
        }

    @Test
    fun deletingAFullOrdersPageMakesRoomForAnother() =
        check { d ->
            stored(pages = Part2Limits.PAGES_PER_ORDER_MAX)
            val vault = model(d)
            assertFalse(vault.state.value.canAddPages)
            vault.deletePage(vault.state.value.pages.first().id) {}
            advanceUntilIdle()
            assertTrue(vault.state.value.canAddPages)
        }

    @Test
    fun deletingTheOnlyPageDeletesTheOrder() =
        check { d ->
            stored(pages = 1)
            val vault = model(d)
            var gone = false

            vault.deletePage(vault.state.value.pages.single().id) { gone = true }
            advanceUntilIdle()

            assertTrue(gone)
            assertNull(repo.order("order-1"), "an order with no page is nothing to keep")
            assertEquals(emptyList(), repo.pages("order-1"))
        }

    @Test
    fun aPageOfAnotherOrderIsNeverDeletedThroughThisOne() =
        check { d ->
            stored(pages = 2)
            stored("order-2", pages = 1)
            val vault = model(d)
            val other = repo.pages("order-2").single().id

            vault.deletePage(other) {}
            advanceUntilIdle()

            assertEquals(1, repo.pages("order-2").size)
            assertEquals(2, vault.state.value.pages.size)
        }

    @Test
    fun aPageIsNotDeletedWhileAScanIsOpenOrAfterTheScreenWasLeft() =
        check { d ->
            stored(pages = 2)
            val vault = model(d)
            val first = vault.state.value.pages.first().id
            val open = OpenScanner()
            vault.addPages(open)
            advanceUntilIdle()

            vault.deletePage(first) {}
            advanceUntilIdle()
            assertEquals(2, repo.pages("order-1").size, "busy: the scan owns the order's pages")

            open.finish(PageScan.Cancelled)
            advanceUntilIdle()
            vault.close()
            vault.deletePage(first) {}
            advanceUntilIdle()
            assertEquals(2, repo.pages("order-1").size, "closed: the screen has left")
        }

    @Test
    fun deletingOneOfTwoPagesLeavesTheOrderWithTheOther() =
        check { d ->
            stored(pages = 2)
            val vault = model(d)
            var gone = false
            vault.deletePage(vault.state.value.pages.first().id) { gone = true }
            advanceUntilIdle()
            assertFalse(gone)
            assertEquals(1, repo.pages("order-1").size)
            assertNotNull(repo.order("order-1"))
        }

    @Test
    fun aPageDeletedAfterTheOrderWasDeletedDoesNothingMore() =
        check { d ->
            stored(pages = 1)
            val vault = model(d)
            val only = vault.state.value.pages.single().id
            var gone = 0
            vault.delete { gone++ }
            advanceUntilIdle()
            vault.deletePage(only) { gone++ }
            advanceUntilIdle()
            assertEquals(1, gone, "the order was already deleted; the screen is told once")
        }
}
