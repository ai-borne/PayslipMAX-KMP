package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.goodEntity
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: a phone that ran the frozen feature/part2-orders branch can hold a v5 backup whose Part II section has drafts. The Orders Bank has no
 * drafts, but the officer's scanned orders and pages in that file are real and must come back, and the drafts left out must be counted so the
 * officer is told, not silently shorted.
 */
class Part2LegacyBackupTest {
    private val device = FakePayslipDao()
    private val service = PayslipBackupService(device, Dispatchers.Unconfined, part2Backup = Part2Backup(device.part2))

    /** The metadata document as the frozen branch wrote it: a `part2Drafts` list beside `part2Orders`, then its pages as binary records. */
    private fun frozenBranchArchive(
        drafts: Int,
        pages: List<RawPage>,
    ): ByteArray {
        val current = PortableBackup(5, listOf(goodEntity("08/2024")), emptyList(), null, part2Orders = listOf(orderRow(SyntheticBank.order("o1"))))
        val json = Json.encodeToString(PortableBackup.serializer(), current)
        val oldDrafts = (1..drafts).joinToString(",") { """{"id":"old-$it","data":"QUJD"}""" }
        val withDrafts = "{\"part2Drafts\":[$oldDrafts]," + json.removePrefix("{")
        val meta = CryptoHelper.encrypt(withDrafts.encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()
        if (pages.isEmpty()) return meta
        val writer = PageArchiveWriter(meta.size + pages.sumOf { it.sealed.size + 200 } + 400)
        writer.begin(meta)
        pages.forEach { writer.add(it.id, it.orderId, it.position, it.sealed) }
        return writer.finish(pages.size, BACKUP_PASSWORD)
    }

    @Test
    fun aFrozenBranchFileRestoresItsOrdersAndPagesAndCountsEveryDraftAsSkipped() =
        runTest {
            val jpeg = SyntheticBank.jpeg(2_000, 4)
            val bytes = frozenBranchArchive(drafts = 3, pages = listOf(pageRow("p1", "o1", 0, jpeg)))

            val report = service.restore(bytes, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(3, report.skipped, "the three drafts are the only thing left out")
            assertEquals(listOf("o1"), device.part2.allOrders().map { it.id })
            assertEquals(listOf("p1"), device.part2.allPageRefs().map { it.id })
            assertEquals(emptyList(), device.part2.allEntries().map { it.id }, "no draft is turned into an entry")
        }

    @Test
    fun aFrozenBranchFileWithoutPagesIsTheSameOneBlobAndCountsItsDraftsToo() =
        runTest {
            val report = service.restore(frozenBranchArchive(drafts = 2, pages = emptyList()), BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            assertEquals(2, report.skipped)
            assertEquals(listOf("o1"), device.part2.allOrders().map { it.id })
        }
}
