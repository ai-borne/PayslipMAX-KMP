package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * WHY: backups real phones wrote (the owner's own, from the Play and App Store builds and from debug builds) are the strongest evidence
 * that the restore path reads what is in the wild. They hold real payslips, so they are never committed and never run in CI: opt in on
 * this machine with `-Dpayslip.localBackups=<folder> -Dpayslip.localBackups.password=<password>` (docs/Plan/14a, D7). Without both it
 * returns at once. Failures name a file by its index only, and nothing a file holds is printed.
 */
class LocalBackupRestoreTest {
    @Test
    fun everyLocalBackupRestoresCompletely() =
        runTest {
            val dir = System.getProperty("payslip.localBackups") ?: return@runTest
            val password = System.getProperty("payslip.localBackups.password") ?: return@runTest
            val files = File(dir).listFiles { f -> f.extension == "pcda" }.orEmpty().sortedBy { it.name }
            assertTrue(files.isNotEmpty(), "no .pcda files in the local backup folder")
            files.forEachIndexed { i, file ->
                val dao = FakePayslipDao()
                val service = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2))
                val report =
                    service
                        .restore(file.readBytes(), password, RestoreMode.REPLACE)
                        .getOrElse { fail("local backup #$i did not restore: ${it::class.simpleName}") }
                assertTrue(dao.getAllPayslips().first().isNotEmpty(), "local backup #$i restored no payslips")
                assertEquals(SkippedRows.NONE, report.skipped, "local backup #$i skipped rows")
            }
        }
}
