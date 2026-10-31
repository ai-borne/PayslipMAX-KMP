package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** WHY: "last backup" must move only when a backup was really produced, never when one failed or was refused. */
class LastBackupExportTest {
    private val dao = FakePayslipDao()
    private val storage = InMemoryLastBackupStorage()
    private var clock = 5_000L
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2), lastBackup = storage, now = { clock })

    @Test
    fun aSuccessfulExportRecordsTheTime() =
        runTest {
            service.export(BACKUP_PASSWORD).getOrThrow()
            clock = 9_000L
            service.export(BACKUP_PASSWORD).getOrThrow()

            assertEquals(9_000L, storage.load())
        }

    @Test
    fun anExportThatFailsLeavesTheOldTimeAlone() =
        runTest {
            storage.save(1L)
            val failing =
                PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2, deviceKey = { error("no key") }), lastBackup = storage, now = { 99L })

            assertEquals(true, failing.export(BACKUP_PASSWORD).isFailure)
            assertEquals(1L, storage.load())
        }
}
