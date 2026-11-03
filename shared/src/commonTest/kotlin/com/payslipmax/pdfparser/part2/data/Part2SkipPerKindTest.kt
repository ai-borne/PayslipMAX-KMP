package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (docs/Plan/14a P5, F6): Part II always skipped and counted an unreadable row, but as one number, so the officer could not be told
 * whether an entry, an order or a scanned page was lost. Each kind is now counted on its own, in the same [SkippedRows] as payslips.
 */
class Part2SkipPerKindTest {
    private val device = FakePayslipDao()
    private val service = PayslipBackupService(device, Dispatchers.Unconfined, part2Backup = Part2Backup(device.part2))

    private suspend fun restore(bytes: ByteArray) = service.restore(bytes, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

    @Test
    fun oneUnreadableEntryAmongThreeIsCountedAsAnEntry() =
        runTest {
            val entries = listOf(entryRow(SyntheticBank.entry("e1")), junkEntry(2), entryRow(SyntheticBank.entry("e3")))

            assertEquals(SkippedRows(part2Entries = 1), restore(archive(entries = entries)).skipped)
            assertEquals(listOf("e1", "e3"), device.part2.allEntries().map { it.id }.sorted())
        }

    @Test
    fun oneUnreadableOrderAmongThreeIsCountedAsAnOrder() =
        runTest {
            val orders = listOf(orderRow(SyntheticBank.order("o1")), Part2RowBackup("o2", "!!"), orderRow(SyntheticBank.order("o3")))

            assertEquals(SkippedRows(part2Orders = 1), restore(archive(orders = orders)).skipped)
            assertEquals(listOf("o1", "o3"), device.part2.allOrders().map { it.id }.sorted())
        }

    @Test
    fun oneUnreadablePageAmongThreeIsCountedAsAPage() =
        runTest {
            val pages =
                listOf(
                    pageRow("p1", "o1", 0, SyntheticBank.jpeg(500, 1)),
                    pageRow("p2", "o1", 1, SyntheticBank.jpeg(500, 2), password = "some other password"),
                    pageRow("p3", "o1", 2, SyntheticBank.jpeg(500, 3)),
                )

            assertEquals(SkippedRows(part2Pages = 1), restore(archive(orders = listOf(orderRow(SyntheticBank.order("o1"))), pages = pages)).skipped)
            assertEquals(listOf("p1", "p3"), device.part2.allPageRefs().map { it.id }.sorted())
        }

    @Test
    fun aMergeReportsWhatTheMergeLeftOutNotWhatThePreviewCounted() =
        runTest {
            // The preview reads the file alone (both entries fit); the merge then finds room on the device for one, and says so.
            repeat(Part2Limits.ENTRIES_MAX - 1) { device.part2.upsertEntry(Part2EntryEntity("held-$it", byteArrayOf(1))) }
            val bytes = archive(entries = listOf(entryRow(SyntheticBank.entry("n1")), entryRow(SyntheticBank.entry("n2"))))

            val report = service.restore(bytes, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            assertEquals(SkippedRows(part2Entries = 1), report.skipped)
        }
}
