package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.DismissedDraftEntity
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.GuideNoteEntity
import com.payslipmax.pdfparser.database.PayslipCorrectionEntity
import com.payslipmax.pdfparser.database.PayslipPdfEntity
import com.payslipmax.pdfparser.database.RepresentationDraftEntity

// The only place a database row and a backup row meet (BackupWireRows.kt). Each mapper names its fields, so a column added to an entity
// stays out of the file until someone adds it here, with a default, on purpose (14 invariant 5).

fun EncryptedPayslipEntity.toBackupRow() = PayslipBackupRow(dateStr, year, monthNum, monthName, ciphertext)

fun PayslipBackupRow.toEntity() = EncryptedPayslipEntity(dateStr, year, monthNum, monthName, ciphertext)

fun PayslipPdfEntity.toBackupRow() = PdfBackupRow(dateStr, pdfData)

fun PdfBackupRow.toEntity() = PayslipPdfEntity(dateStr, pdfData)

fun RepresentationDraftEntity.toBackupRow() = DraftBackupRow(id, disputeMonth, disputeType, recipient, subject, bodyText, createdAt)

fun DraftBackupRow.toEntity() = RepresentationDraftEntity(id, disputeMonth, disputeType, recipient, subject, bodyText, createdAt)

fun DismissedDraftEntity.toBackupRow() = DismissedDraftBackupRow(disputeMonth, disputeType)

fun DismissedDraftBackupRow.toEntity() = DismissedDraftEntity(disputeMonth, disputeType)

fun PayslipCorrectionEntity.toBackupRow() = CorrectionBackupRow(dateStr, ciphertext)

fun CorrectionBackupRow.toEntity() = PayslipCorrectionEntity(dateStr, ciphertext)

fun GuideNoteEntity.toBackupRow() = GuideNoteBackupRow(cardId, ciphertext)

fun GuideNoteBackupRow.toEntity() = GuideNoteEntity(cardId, ciphertext)

/** The officer's preferences only; the device-only settings (D5) are never read here. */
fun AppSettingsEntity.toSettingsBackup() = SettingsBackup(appTheme, profileName, profileCdaNumber, profilePanNumber, useLocalAi)

/**
 * [device] with this backup's preferences on top. Everything D5 keeps on the device (lock, PIN hash, telemetry consent, Premium) and the
 * row id come from [device], so no restore can lock the officer out, change their consent or move an entitlement.
 */
fun SettingsBackup.applyTo(device: AppSettingsEntity) =
    device.copy(
        appTheme = appTheme,
        profileName = profileName,
        profileCdaNumber = profileCdaNumber,
        profilePanNumber = profilePanNumber,
        useLocalAi = useLocalAi,
    )
