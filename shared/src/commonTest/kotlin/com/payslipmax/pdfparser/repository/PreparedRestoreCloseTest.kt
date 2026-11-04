package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.putEntry
import com.payslipmax.pdfparser.part2.data.putOrder
import com.payslipmax.pdfparser.part2.data.putPage
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P7, P10 "From P4" item 4): a prepared restore holds the archive's key while its preview is on screen, so the view model
 * must close it on every exit, and its tests must be able to see that it did. [PreparedRestore.isClosed] says so, and it is only true once
 * the key is wiped: a closed restore can no longer open a page of the file.
 */
class PreparedRestoreCloseTest {
    @Test
    fun aClosedPreparedRestoreSaysSoAndItsKeyNoLongerOpensAPage() =
        runTest {
            val dao =
                FakePayslipDao().apply {
                    putEntry(SyntheticBank.entry("e1", 1))
                    putOrder(SyntheticBank.order("o1"))
                    putPage("p0", "o1", 0, SyntheticBank.jpeg(4_000, 0))
                }
            val service = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2))
            val bytes = service.export(PASSWORD).getOrThrow()
            val prepared = service.prepare(MemoryBackupSource(bytes), PASSWORD).getOrThrow()

            assertFalse(prepared.isClosed, "a restore just prepared is open")
            prepared.close()

            assertTrue(prepared.isClosed)
            assertTrue(service.commit(prepared, RestoreMode.REPLACE).isFailure, "a wiped key opens no page")
        }

    private companion object {
        const val PASSWORD = "Pass#12345"
    }
}
