package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (Phase 11 item 56): the Settings screen refused Back Up when the officer had no payslips, although drafts and scanned orders
 * existed, so a vault could not be backed up and a restore replaced it without asking. The screen needs the number of Part II items
 * from the service that actually writes them, and the service must really back up and restore a device that holds only Part II data.
 */
class Part2BackupItemCountTest {
    private val source = FakePayslipDao()
    private val sourceService = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2))

    @Test
    fun draftsAndOrdersAreCountedAndPagesAddNothingBecauseTheyBelongToAnOrder() =
        runTest {
            source.putDraft(SyntheticPart2Drafts.fullDraft("d1", 1))
            source.putDraft(SyntheticPart2Drafts.fullDraft("d2", 2))
            source.putOrder(SyntheticPart2Drafts.order("o1"))
            source.putPage("p1", "o1", 0, SyntheticPart2Drafts.jpeg(2000))
            source.putPage("p2", "o1", 1, SyntheticPart2Drafts.jpeg(2000))

            assertEquals(3, sourceService.part2ItemCount())
        }

    @Test
    fun anEmptyDeviceCountsZero() =
        runTest {
            assertEquals(0, sourceService.part2ItemCount())
        }

    @Test
    fun aServiceWithoutPart2WiredInCountsZero() =
        runTest {
            val bare = PayslipBackupService(source, Dispatchers.Unconfined)
            source.putDraft(SyntheticPart2Drafts.fullDraft("d1", 1))

            assertEquals(0, bare.part2ItemCount())
        }

    @Test
    fun aDeviceWithOnlyPartTwoDataBacksUpAndRestoresOntoAFreshDevice() =
        runTest {
            source.putDraft(SyntheticPart2Drafts.fullDraft("d1", 1))
            source.putOrder(SyntheticPart2Drafts.order("o1"))
            source.putPage("p1", "o1", 0, SyntheticPart2Drafts.jpeg(2000))
            val target = FakePayslipDao()
            val targetService = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2))

            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            val report = targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(2, targetService.part2ItemCount())
            assertEquals(1, target.part2.allPageRefs().size)
            assertTrue(report.skipped == 0, "nothing in a clean archive is skipped")
        }
}
