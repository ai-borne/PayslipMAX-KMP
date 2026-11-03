package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.seedDeviceMonth
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: a `.pcda` file outlives the app that wrote it, and older apps read files from newer ones. Its keys are a contract with every app
 * version in the wild, so they are pinned here, and the wire format is made of backup rows only (F1): a Room column added for the
 * database, or a Kotlin rename, must never change what a backup file holds. Keys are today's, byte for byte, except the device-only
 * settings that D5 takes out of the file.
 */
class BackupWireKeysTest {
    @Test
    fun everyExportedRowCarriesExactlyItsPinnedKeys() =
        runTest {
            val dao = FakePayslipDao()
            dao.seedDeviceMonth("08/2024")
            // Every column set away from its default (default values are not written), the device-only ones included: none may show.
            dao.insertSettings(
                LOCKED_PREMIUM_DEVICE.copy(
                    appTheme = "dark",
                    profileName = "Synthetic Officer",
                    profileCdaNumber = "CDA-0000",
                    profilePanNumber = "AAAAA0000A",
                    useLocalAi = true,
                ),
            )

            val document = exportedDocument(dao, BACKUP_PASSWORD)

            for ((array, keys) in ROW_KEYS) {
                val rows = document.getValue(array).jsonArray
                assertTrue(rows.isNotEmpty(), "$array exported nothing, so its keys were not checked")
                rows.forEach { assertEquals(keys, it.jsonObject.keys, array) }
            }
            assertEquals(SETTINGS_KEYS, document.getValue("settings").jsonObject.keys, "settings")
        }

    @Test
    fun theWireFormatIsMadeOfBackupRowsWithPinnedKeysAndNoDatabaseType() {
        val classes = wireClasses(PortableBackup.serializer().descriptor)

        val fromDatabase = classes.keys.filter { it.startsWith("com.payslipmax.pdfparser.database.") }
        assertEquals(emptyList(), fromDatabase, "a Room entity in the wire format makes every DB column part of the file format")
        assertEquals(WIRE_CLASSES, classes)
    }

    private companion object {
        const val BACKUP = "com.payslipmax.pdfparser.repository.backup"
        const val PART2 = "com.payslipmax.pdfparser.part2.data"
        val PAYSLIP_KEYS = setOf("dateStr", "year", "monthNum", "monthName", "ciphertext")
        val PDF_KEYS = setOf("dateStr", "pdfData")
        val SETTINGS_KEYS = setOf("appTheme", "profileName", "profileCdaNumber", "profilePanNumber", "useLocalAi")
        val DRAFT_KEYS = setOf("id", "disputeMonth", "disputeType", "recipient", "subject", "bodyText", "createdAt")
        val DISMISSED_KEYS = setOf("disputeMonth", "disputeType")
        val CORRECTION_KEYS = setOf("dateStr", "ciphertext")
        val NOTE_KEYS = setOf("cardId", "ciphertext")

        val ROW_KEYS =
            mapOf(
                "encryptedPayslips" to PAYSLIP_KEYS,
                "pdfs" to PDF_KEYS,
                "drafts" to DRAFT_KEYS,
                "dismissedDrafts" to DISMISSED_KEYS,
                "corrections" to CORRECTION_KEYS,
                "guideNotes" to NOTE_KEYS,
            )

        val WIRE_CLASSES =
            mapOf(
                "com.payslipmax.pdfparser.repository.PortableBackup" to
                    setOf("version", "settings", "part2Entries", "part2Orders", "part2Drafts", "exportedAt") + ROW_KEYS.keys,
                "$BACKUP.PayslipBackupRow" to PAYSLIP_KEYS,
                "$BACKUP.PdfBackupRow" to PDF_KEYS,
                "$BACKUP.SettingsBackup" to SETTINGS_KEYS,
                "$BACKUP.DraftBackupRow" to DRAFT_KEYS,
                "$BACKUP.DismissedDraftBackupRow" to DISMISSED_KEYS,
                "$BACKUP.CorrectionBackupRow" to CORRECTION_KEYS,
                "$BACKUP.GuideNoteBackupRow" to NOTE_KEYS,
                "$PART2.Part2RowBackup" to setOf("id", "data"),
                "$PART2.Part2LegacyDraft" to setOf("id"),
            )
    }
}
