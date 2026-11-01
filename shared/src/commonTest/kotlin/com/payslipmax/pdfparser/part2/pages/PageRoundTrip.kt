package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers

/** What came out of the whole chain: the capture's answer, the pages the first device holds, and the pages a second device holds after a restore. */
internal class RoundTrip(
    val captured: CaptureResult,
    val onDevice: List<ByteArray>,
    val afterRestore: List<ByteArray>,
)

/**
 * The chain a scanned page travels: a scanner's raw pages, a real [encoder], the encrypted repository, a `.pcda` backup and a restore
 * onto a second device. Shared by the Android and iOS tests so both prove the same promise with their own codec.
 */
internal suspend fun roundTrip(
    encoder: PageEncoder,
    rawPages: List<ByteArray>,
): RoundTrip {
    val source = FakePayslipDao()
    val sourceRepository = RoomPart2Repository(dao = source.part2, dispatcher = Dispatchers.Unconfined)
    sourceRepository.saveOrder(SyntheticBank.order("o1"))
    val scanner = FakePageScanner(PageScan.Pages(rawPages.map { FakeScannedPage(it) }))
    val captured = PageCapture(scanner, encoder, sourceRepository, Dispatchers.Unconfined).capture("o1")

    val archive = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2)).export(BACKUP_PASSWORD).getOrThrow()
    val target = FakePayslipDao()
    PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2)).restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

    return RoundTrip(captured, pagesOf(sourceRepository), pagesOf(RoomPart2Repository(dao = target.part2, dispatcher = Dispatchers.Unconfined)))
}

private suspend fun pagesOf(repository: RoomPart2Repository): List<ByteArray> = repository.pages("o1").map { repository.readPage(it.id) { bytes -> bytes.copyOf() }!! }
