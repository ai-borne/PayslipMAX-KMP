package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.guide.domain.GuideNote

const val BACKUP_PASSWORD = "BackupPass#1"

fun goodEntity(dateStr: String) = createMockPayslip(dateStr).toEncryptedEntity(BACKUP_PASSWORD)

fun backupOf(dateStr: String): ByteArray =
    backupBytes(
        PortableBackup(2, listOf(goodEntity(dateStr)), listOf(PayslipPdfEntity(dateStr, byteArrayOf(9))), null),
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
