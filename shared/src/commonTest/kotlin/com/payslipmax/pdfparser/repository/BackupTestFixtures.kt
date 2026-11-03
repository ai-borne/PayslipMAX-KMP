package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.SealKey
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.part2.data.PageArchive
import com.payslipmax.pdfparser.repository.backup.BackupEnvelope
import com.payslipmax.pdfparser.repository.backup.BackupJson
import com.payslipmax.pdfparser.repository.backup.PdfBackupRow
import com.payslipmax.pdfparser.repository.backup.openArchive
import com.payslipmax.pdfparser.repository.backup.toBackupRow

const val BACKUP_PASSWORD = "BackupPass#1"

/** One synthetic payslip as a backup carries it: encrypted with the backup password. */
fun goodRow(dateStr: String) = createMockPayslip(dateStr).toEncryptedEntity(BACKUP_PASSWORD).toBackupRow()

fun backupOf(dateStr: String): ByteArray =
    backupBytes(
        PortableBackup(2, listOf(goodRow(dateStr)), listOf(PdfBackupRow(dateStr, byteArrayOf(9))), null),
    )

fun backupBytes(backup: PortableBackup): ByteArray =
    CryptoHelper
        .encrypt(
            kotlinx.serialization.json.Json
                .encodeToString(PortableBackup.serializer(), backup)
                .encodeToByteArray(),
            BACKUP_PASSWORD,
        ).getOrThrow()

/** One month of everything a device holds: the payslip, its PDF, and all that is derived from or added to it (plus one private Guide note). */
suspend fun PayslipDao.seedDeviceMonth(dateStr: String) {
    insertPayslip(createMockPayslip(dateStr).toEncryptedEntity())
    insertPayslipPdf(PayslipPdfEntity(dateStr, byteArrayOf(1)))
    insertCorrection(mapOf("basicPay" to 1.0).toCorrectionEntity(dateStr))
    insertGuideNote(GuideNote.of("RB-DEVICE-$dateStr".replace('/', '-'), "device note", "rev00001", 1L)!!.toEntity())
    insertLedgerRecord(ledgerRow(dateStr))
    insertFinancialInsight(FinancialInsightEntity("i-$dateStr", dateStr, "TAX", "t", "c", "INFO", 1L))
    insertRepresentationDraft(
        RepresentationDraftEntity("d-$dateStr", dateStr, "MISSING_HRA", "PCDA_O_PUNE", "s", "b", 1L),
    )
    insertDismissedDraft(DismissedDraftEntity(dateStr, "MISSING_HRA"))
}

private fun ledgerRow(dateStr: String) =
    LedgerRecordEntity(
        dateStr = dateStr,
        year = dateStr.substringAfter('/').toInt(),
        monthNum = dateStr.substringBefore('/').toInt(),
        basicPay = 1.0,
        dearnessAllowance = 1.0,
        militaryServicePay = 1.0,
        transportAllowance = 1.0,
        transportAllowanceDa = 1.0,
        houseRentAllowance = 1.0,
        grossPay = 1.0,
        dsopSubscription = 1.0,
        incomeTax = 1.0,
        netPay = 1.0,
    )

/** An export as a restore opens it: its document, and the key its rows are sealed with (since format 6, the archive's one key). */
internal class OpenedBackup(
    val backup: PortableBackup,
    val key: SealKey,
)

internal fun openBackup(
    archive: ByteArray,
    password: String = BACKUP_PASSWORD,
): OpenedBackup {
    val opened = openArchive(MemoryBackupSource(archive), password)
    return OpenedBackup(BackupJson.decode(PortableBackup.serializer(), opened.document.decodeToString()), opened.key)
}

/** The page archive's document inside a v6 file (after its plaintext header), or null when [file] is not one. */
internal fun pageArchiveMetaOfV6(file: ByteArray): ByteArray? =
    BackupEnvelope.read(MemoryBackupSource(file))?.let { PageArchive.metaOf(MemoryBackupSource(file).from(BackupEnvelope.HEADER_BYTES.toLong())) }

/** A read-back step that accepts any written file, for tests about the file itself rather than its contents. */
internal val readsBack: suspend (BackupSource) -> Result<Unit> = { Result.success(Unit) }
