package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
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
        drafts: List<Part2DraftBackup> = emptyList(),
        orders: List<Part2OrderBackup> = emptyList(),
        pages: List<RawPage> = emptyList(),
        mode: RestoreMode = RestoreMode.REPLACE,
        using: Part2Backup = backup,
    ): Part2Restore {
        val bytes = archive(drafts, orders, pages)
        return using.restore(decodeArchive(bytes), bytes, BACKUP_PASSWORD, mode)
    }

    private fun validJson(pad: Int) = """{"v":1,"id":"big","createdAt":1,"updatedAt":2,"requests":[],"pad":"${"x".repeat(pad)}"}"""

    private fun sealedRow(
        id: String,
        json: String,
    ) = Part2DraftBackup(id, text(CryptoHelper.encrypt(json.encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()))

    private fun oversizedPage(): RawPage {
        // sealPage refuses to make an oversized page, so it is framed by hand, as a crafted file would be.
        val frame = byteArrayOf(1, 2) + "p2".encodeToByteArray() + byteArrayOf(2) + "o1".encodeToByteArray() + SyntheticPart2Drafts.jpeg(Part2Limits.PAGE_MAX_BYTES + 1)
        return RawPage("p2", "o1", 0, CryptoHelper.encrypt(frame, BACKUP_PASSWORD).getOrThrow())
    }

    @Test
    fun aValidDraftOverThePayloadCeilingIsSkippedAndTheSameDraftWithinItIsRestored() =
        runTest {
            val within = restore(drafts = listOf(sealedRow("big", validJson(10))))
            val over = restore(drafts = listOf(sealedRow("big", validJson(Part2Limits.PAYLOAD_MAX_BYTES))))

            assertEquals(listOf("big"), within.drafts.map { it.id })
            assertEquals(0, within.skipped)
            assertEquals(emptyList(), over.drafts.map { it.id })
            assertEquals(1, over.skipped)
        }

    @Test
    fun aValidPageOverThePageCeilingIsSkippedAndTheSamePageWithinItIsRestored() =
        runTest {
            val order = orderRow(SyntheticPart2Drafts.order("o1"))
            val ok = restore(orders = listOf(order), pages = listOf(pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(Part2Limits.PAGE_MAX_BYTES))))

            val over = restore(orders = listOf(order), pages = listOf(oversizedPage()))

            assertEquals(listOf("p1"), ok.pages.map { it.id }.toList())
            assertEquals(emptyList(), over.pages.map { it.id }.toList())
            assertEquals(1, over.skipped)
        }

    @Test
    fun rowsBeyondTheDraftCountAreNeverExaminedEvenWhenTheLastOneIsValid() =
        runTest {
            val rows = List(Part2Limits.DRAFTS_MAX) { junkDraft(it) } + draftRow(SyntheticPart2Drafts.fullDraft("last-valid"))
            val first = listOf(draftRow(SyntheticPart2Drafts.fullDraft("first-valid"))) + List(Part2Limits.DRAFTS_MAX) { junkDraft(it) }

            val lastValid = restore(drafts = rows)
            val firstValid = restore(drafts = first)

            assertEquals(emptyList(), lastValid.drafts.map { it.id })
            assertEquals(Part2Limits.DRAFTS_MAX + 1, lastValid.skipped)
            assertEquals(listOf("first-valid"), firstValid.drafts.map { it.id })
            assertEquals(Part2Limits.DRAFTS_MAX, firstValid.skipped)
        }

    @Test
    fun rowsBeyondTheOrderAndPageCountsAreNeverExamined() =
        runTest {
            val junkOrders = List(Part2Limits.ORDERS_MAX) { Part2OrderBackup("junk-$it", "!!") }
            val validOrder = orderRow(SyntheticPart2Drafts.order("o1"))
            val surplusOrder = restore(orders = junkOrders + validOrder)
            val junkPages = List(Part2Limits.ORDERS_MAX * Part2Limits.PAGES_PER_ORDER_MAX) { junkPage(it) }
            val validPage = pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(100))
            val surplusPage = restore(orders = listOf(validOrder), pages = junkPages + validPage)

            assertEquals(emptyList(), surplusOrder.orders.map { it.id })
            assertEquals(emptyList(), surplusPage.pages.map { it.id }.toList())
            assertEquals(junkPages.size + 1, surplusPage.skipped)
        }

    @Test
    fun onMergeANewDraftOnlyFitsInTheRoomTheDeviceHasLeft() =
        runTest {
            repeat(Part2Limits.DRAFTS_MAX - 1) { device.part2.upsertDraft(Part2DraftEntity("held-$it", byteArrayOf(1))) }

            val result = restore(drafts = listOf(draftRow(SyntheticPart2Drafts.fullDraft("n1")), draftRow(SyntheticPart2Drafts.fullDraft("n2"))), mode = RestoreMode.MERGE)

            assertEquals(listOf("n1"), result.drafts.map { it.id })
            assertEquals(1, result.skipped)
        }

    @Test
    fun aPageStopsAtTheOrdersTenPagesCountingTheOnesTheDeviceAlreadyHolds() =
        runTest {
            device.putOrder(SyntheticPart2Drafts.order("o1"))
            repeat(9) { device.putPage("held-$it", "o1", it, SyntheticPart2Drafts.jpeg(50)) }
            val pages = (1..3).map { pageRow("new-$it", "o1", 20 + it, SyntheticPart2Drafts.jpeg(50)) }

            val result = restore(pages = pages, mode = RestoreMode.MERGE)

            assertEquals(listOf("new-1"), result.pages.map { it.id }.toList())
            assertEquals(2, result.skipped)
        }

    @Test
    fun aPageWhoseOrderIsNowhereIsSkipped() =
        runTest {
            val result = restore(pages = listOf(pageRow("p1", "ghost", 0, SyntheticPart2Drafts.jpeg(50))))

            assertEquals(emptyList(), result.pages.map { it.id }.toList())
            assertEquals(1, result.skipped)
        }

    @Test
    fun aPageOfAnOrderThatCannotBeReadIsSkippedToo() =
        runTest {
            val result =
                restore(
                    orders = listOf(Part2OrderBackup("o1", text(byteArrayOf(1, 2, 3)))),
                    pages = listOf(pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(50))),
                )

            assertEquals(emptyList(), result.pages.map { it.id }.toList())
            assertEquals(2, result.skipped)
        }

    @Test
    fun pagesStopAtTheVaultCeilingAndTheDevicesOwnPagesCountAgainstIt() =
        runTest {
            val small = Part2Backup(device.part2, vaultBytesMax = 2500)
            val order = orderRow(SyntheticPart2Drafts.order("o1"))
            val pages = (1..3).map { pageRow("p$it", "o1", it, SyntheticPart2Drafts.jpeg(1000, it)) }

            val replace = restore(orders = listOf(order), pages = pages, using = small)
            assertEquals(listOf("p1", "p2"), replace.pages.map { it.id }.toList())
            assertEquals(1, replace.skipped)

            device.putOrder(SyntheticPart2Drafts.order("o1"))
            device.putPage("held", "o1", 0, SyntheticPart2Drafts.jpeg(1000, 9))
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
            val surplus = List(Part2Limits.DRAFTS_MAX) { junkDraft(it) } + draftRow(SyntheticPart2Drafts.fullDraft("last"))

            val result =
                restore(
                    drafts = listOf(sealedRow("big", validJson(Part2Limits.PAYLOAD_MAX_BYTES))) + surplus,
                    orders = listOf(orderRow(SyntheticPart2Drafts.order("o1"))),
                    pages = listOf(oversizedPage(), pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(100))),
                    using = counting,
                )

            // Decoded: the 999 examined junk drafts (cheap, and they fail) and the one order. Never the oversized draft or the surplus past
            // the count. Opened (decrypted) once: the good page. Never the oversized one.
            assertEquals(Part2Limits.DRAFTS_MAX - 1 + 1, decoded)
            assertEquals(1, opened)
            assertEquals(listOf("p1"), result.pages.map { it.id }.toList())
            assertEquals(2, opened, "the page is re-encrypted for this device only when it is written, one page at a time")
        }
}
