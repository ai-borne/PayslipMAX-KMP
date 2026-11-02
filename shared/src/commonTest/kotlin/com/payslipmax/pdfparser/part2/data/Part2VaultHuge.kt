package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.BackupSink
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePart2Dao
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlin.time.TimeSource

/** A vault at the ceiling: 50 orders of 10 pages, each page the largest allowed (500 KB), so 500 pages and about 256 MB, just under `VAULT_BYTES_MAX`. */
internal const val HUGE_ORDERS = 50
internal const val HUGE_PAGES = HUGE_ORDERS * Part2Limits.PAGES_PER_ORDER_MAX

internal class HugeRestore(
    val restoredPages: Int,
    val restoredBytes: Long,
    val skipped: Int,
    val restoreMs: Long,
)

/**
 * Writes the archive of a full vault into [sink] one sealed page at a time, so the writer never holds more than a page (the vault cannot
 * be built in a fake database: it is larger than the test heap). The metadata document comes from a real export of the same orders, and
 * each page is sealed exactly as the real export seals it. Returns the pages written.
 */
internal suspend fun writeHugeArchive(sink: BackupSink): Int {
    val dao = FakePayslipDao()
    repeat(HUGE_ORDERS) { dao.putOrder(SyntheticBank.order("order-$it")) }
    dao.putPage("seed", "order-0", 0, SyntheticBank.jpeg(64, 0))
    val meta = PageArchive.metaOf(PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2)).export(BACKUP_PASSWORD).getOrThrow())!!
    val writer = PageArchiveWriter(sink)
    writer.begin(meta)
    var seed = 0
    repeat(HUGE_ORDERS) { o ->
        repeat(Part2Limits.PAGES_PER_ORDER_MAX) { p ->
            val sealed = sealPage("page-$o-$p", "order-$o", p, SyntheticBank.jpeg(Part2Limits.PAGE_MAX_BYTES, seed++), BACKUP_PASSWORD)
            writer.add(sealed.id, sealed.orderId, sealed.position, sealed.ciphertext)
        }
    }
    writer.finish(HUGE_PAGES, BACKUP_PASSWORD)
    return HUGE_PAGES
}

/** Restores [source] into a database that counts and drops page bytes (the vault would not fit the heap), through the real service. */
internal suspend fun restoreHuge(source: BackupSource): HugeRestore {
    val part2 = FakePart2Dao(keepPageBytes = false)
    val target = FakePayslipDao(part2)
    val service = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(part2))
    val start = TimeSource.Monotonic.markNow()
    val report = service.restore(source, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()
    return HugeRestore(part2.droppedPages, part2.droppedBytes, report.skipped, start.elapsedNow().inWholeMilliseconds)
}
