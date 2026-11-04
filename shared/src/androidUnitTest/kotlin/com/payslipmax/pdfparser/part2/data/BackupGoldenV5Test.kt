package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.MemoryBackupSource
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the file format did not change when the backup began to stream, so a backup an earlier build wrote must still restore. This one
 * (3 synthetic pages, 2 entries, 1 order) was written by the byte-array exporter before the streaming change and is committed as it was;
 * both the byte-array and the streaming restore must open it and give every page back byte for byte.
 */
class BackupGoldenV5Test {
    private val golden = checkNotNull(javaClass.getResourceAsStream("/backup/v5_pages_synthetic.pcda")) { "the golden backup is missing" }.use { it.readBytes() }

    private fun restoreInto(
        dao: FakePayslipDao,
        viaSource: Boolean,
    ) = runBlocking {
        val service = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2))
        val result =
            if (viaSource) {
                service.restore(MemoryBackupSource(golden), Secret.of(BACKUP_PASSWORD), RestoreMode.REPLACE)
            } else {
                service.restore(golden, Secret.of(BACKUP_PASSWORD), RestoreMode.REPLACE)
            }
        result.getOrThrow()
    }

    @Test
    fun anEarlierBackupRestoresEveryRowAndPageThroughBothDoors() {
        for (viaSource in listOf(false, true)) {
            val dao = FakePayslipDao()

            val report = restoreInto(dao, viaSource)

            assertEquals(SkippedRows.NONE, report.skipped, "nothing skipped (viaSource=$viaSource)")
            runBlocking {
                assertEquals(setOf("e1", "e2"), dao.part2.allEntries().map { it.id }.toSet())
                assertEquals(listOf("o1"), dao.part2.allOrders().map { it.id })
                assertEquals(3, dao.part2.allPageRefs().size)
                repeat(3) { assertContentEquals(SyntheticBank.jpeg(20_000, it + 1), dao.part2.page("p$it")!!.openPage(deviceKey)) }
            }
        }
        assertTrue(golden.copyOf(4).decodeToString() == "PCDB", "the fixture is a page archive")
    }
}
