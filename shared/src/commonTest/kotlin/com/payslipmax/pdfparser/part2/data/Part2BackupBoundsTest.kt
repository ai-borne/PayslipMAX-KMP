package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: a restored file is untrusted input; each row costs a key derivation and a decryption, and a page is up to 500 KB. A crafted
 * archive must not be able to make the app decode a million rows or a gigabyte of pages. Every check here uses a row that is VALID
 * but over a limit (or beyond a count), so the only reason it can be skipped is the limit, and the size is judged from the row's
 * length before anything is decoded or decrypted. What is skipped is counted, never fatal.
 */
class Part2BackupBoundsTest {
    private val device = FakePayslipDao()
    private val backup = Part2Backup(device.part2)

    private suspend fun restore(
        entries: List<Part2RowBackup> = emptyList(),
        orders: List<Part2RowBackup> = emptyList(),
        pages: List<RawPage> = emptyList(),
        mode: RestoreMode = RestoreMode.REPLACE,
        using: Part2Backup = backup,
    ): Part2Restore {
        val bytes = archive(entries, orders, pages)
        return using.restore(decodeArchive(bytes), bytes, BACKUP_PASSWORD, mode)
    }

    private fun validJson(pad: Int) = """{"v":1,"id":"big","code":"SYNCODE1","from":{"y":2026,"m":3,"d":1},"createdAt":1,"updatedAt":2,"pad":"${"x".repeat(pad)}"}"""

    private fun sealedRow(
        id: String,
        json: String,
    ) = Part2RowBackup(id, text(CryptoHelper.encrypt(json.encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()))

    private fun oversizedPage(): RawPage {
        // sealPage refuses to make an oversized page, so it is framed by hand, as a crafted file would be.
        val frame = byteArrayOf(1, 2) + "p2".encodeToByteArray() + byteArrayOf(2) + "o1".encodeToByteArray() + SyntheticBank.jpeg(Part2Limits.PAGE_MAX_BYTES + 1)
        return RawPage("p2", "o1", 0, CryptoHelper.encrypt(frame, BACKUP_PASSWORD).getOrThrow())
    }

    @Test
    fun aValidEntryOverThePayloadCeilingIsSkippedAndTheSameEntryWithinItIsRestored() =
        runTest {
            val within = restore(entries = listOf(sealedRow("big", validJson(10))))
            val over = restore(entries = listOf(sealedRow("big", validJson(Part2Limits.PAYLOAD_MAX_BYTES))))

            assertEquals(listOf("big"), within.entries.map { it.id })
            assertEquals(0, within.skipped)
            assertEquals(emptyList(), over.entries.map { it.id })
            assertEquals(1, over.skipped)
        }

    @Test
    fun aValidPageOverThePageCeilingIsSkippedAndTheSamePageWithinItIsRestored() =
        runTest {
            val order = orderRow(SyntheticBank.order("o1"))
            val ok = restore(orders = listOf(order), pages = listOf(pageRow("p1", "o1", 0, SyntheticBank.jpeg(Part2Limits.PAGE_MAX_BYTES))))

            val over = restore(orders = listOf(order), pages = listOf(oversizedPage()))

            assertEquals(listOf("p1"), ok.pages.map { it.id }.toList())
            assertEquals(emptyList(), over.pages.map { it.id }.toList())
            assertEquals(1, over.skipped)
        }

    @Test
    fun rowsBeyondTheEntryCountAreNeverExaminedEvenWhenTheLastOneIsValid() =
        runTest {
            val rows = List(Part2Limits.ENTRIES_MAX) { junkEntry(it) } + entryRow(SyntheticBank.entry("last-valid"))
            val first = listOf(entryRow(SyntheticBank.entry("first-valid"))) + List(Part2Limits.ENTRIES_MAX) { junkEntry(it) }

            val lastValid = restore(entries = rows)
            val firstValid = restore(entries = first)

            assertEquals(emptyList(), lastValid.entries.map { it.id })
            assertEquals(Part2Limits.ENTRIES_MAX + 1, lastValid.skipped)
            assertEquals(listOf("first-valid"), firstValid.entries.map { it.id })
            assertEquals(Part2Limits.ENTRIES_MAX, firstValid.skipped)
        }

    @Test
    fun rowsBeyondTheOrderAndPageCountsAreNeverExamined() =
        runTest {
            val junkOrders = List(Part2Limits.ORDERS_MAX) { Part2RowBackup("junk-$it", "!!") }
            val validOrder = orderRow(SyntheticBank.order("o1"))
            val surplusOrder = restore(orders = junkOrders + validOrder)
            val junkPages = List(Part2Limits.ORDERS_MAX * Part2Limits.PAGES_PER_ORDER_MAX) { junkPage(it) }
            val validPage = pageRow("p1", "o1", 0, SyntheticBank.jpeg(100))
            val surplusPage = restore(orders = listOf(validOrder), pages = junkPages + validPage)

            assertEquals(emptyList(), surplusOrder.orders.map { it.id })
            assertEquals(emptyList(), surplusPage.pages.map { it.id }.toList())
            assertEquals(junkPages.size + 1, surplusPage.skipped)
        }

    @Test
    fun onMergeANewEntryOnlyFitsInTheRoomTheDeviceHasLeft() =
        runTest {
            repeat(Part2Limits.ENTRIES_MAX - 1) { device.part2.upsertEntry(Part2EntryEntity("held-$it", byteArrayOf(1))) }

            val result = restore(entries = listOf(entryRow(SyntheticBank.entry("n1")), entryRow(SyntheticBank.entry("n2"))), mode = RestoreMode.MERGE)

            assertEquals(listOf("n1"), result.entries.map { it.id })
            assertEquals(1, result.skipped)
        }

    @Test
    fun aPageStopsAtTheOrdersTenPagesCountingTheOnesTheDeviceAlreadyHolds() =
        runTest {
            device.putOrder(SyntheticBank.order("o1"))
            repeat(9) { device.putPage("held-$it", "o1", it, SyntheticBank.jpeg(50)) }
            val pages = (1..3).map { pageRow("new-$it", "o1", 20 + it, SyntheticBank.jpeg(50)) }

            val result = restore(pages = pages, mode = RestoreMode.MERGE)

            assertEquals(listOf("new-1"), result.pages.map { it.id }.toList())
            assertEquals(2, result.skipped)
        }

    @Test
    fun aPageWhoseOrderIsNowhereIsSkipped() =
        runTest {
            val result = restore(pages = listOf(pageRow("p1", "ghost", 0, SyntheticBank.jpeg(50))))

            assertEquals(emptyList(), result.pages.map { it.id }.toList())
            assertEquals(1, result.skipped)
        }

    @Test
    fun aPageOfAnOrderThatCannotBeReadIsSkippedToo() =
        runTest {
            val result =
                restore(
                    orders = listOf(Part2RowBackup("o1", text(byteArrayOf(1, 2, 3)))),
                    pages = listOf(pageRow("p1", "o1", 0, SyntheticBank.jpeg(50))),
                )

            assertEquals(emptyList(), result.pages.map { it.id }.toList())
            assertEquals(2, result.skipped)
        }

    @Test
    fun pagesStopAtTheVaultCeilingAndTheDevicesOwnPagesCountAgainstIt() =
        runTest {
            val small = Part2Backup(device.part2, vaultBytesMax = 2500)
            val order = orderRow(SyntheticBank.order("o1"))
            val pages = (1..3).map { pageRow("p$it", "o1", it, SyntheticBank.jpeg(1000, it)) }

            val replace = restore(orders = listOf(order), pages = pages, using = small)
            assertEquals(listOf("p1", "p2"), replace.pages.map { it.id }.toList())
            assertEquals(1, replace.skipped)

            device.putOrder(SyntheticBank.order("o1"))
            device.putPage("held", "o1", 0, SyntheticBank.jpeg(1000, 9))
            val merge = restore(pages = pages, mode = RestoreMode.MERGE, using = small)
            assertEquals(listOf("p1"), merge.pages.map { it.id }.toList())
            assertEquals(2, merge.skipped)
        }

    @Test
    fun aRowOverItsLimitIsNeverDecodedOrDecryptedOnlyJudgedByItsLength() =
        runTest {
            var decoded = 0
            var opened = 0
            val counting =
                Part2Backup(
                    device.part2,
                    decodeText = { text ->
                        decoded++
                        kotlin.io.encoding.Base64.decode(text)
                    },
                    openPage = { page, key ->
                        opened++
                        page.openPage(key)
                    },
                )
            val surplus = List(Part2Limits.ENTRIES_MAX) { junkEntry(it) } + entryRow(SyntheticBank.entry("last"))

            val result =
                restore(
                    entries = listOf(sealedRow("big", validJson(Part2Limits.PAYLOAD_MAX_BYTES))) + surplus,
                    orders = listOf(orderRow(SyntheticBank.order("o1"))),
                    pages = listOf(oversizedPage(), pageRow("p1", "o1", 0, SyntheticBank.jpeg(100))),
                    using = counting,
                )

            // Decoded: the 999 examined junk entries (cheap, and they fail) and the one order. Never the oversized entry or the surplus past
            // the count. Opened (decrypted) once: the good page. Never the oversized one.
            assertEquals(Part2Limits.ENTRIES_MAX - 1 + 1, decoded)
            assertEquals(1, opened)
            assertEquals(listOf("p1"), result.pages.map { it.id }.toList())
            assertEquals(2, opened, "the page is re-encrypted for this device only when it is written, one page at a time")
        }
}
