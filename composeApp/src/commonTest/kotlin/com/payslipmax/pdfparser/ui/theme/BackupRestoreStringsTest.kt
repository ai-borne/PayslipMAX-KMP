package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.repository.BackupFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the officer reads one of these lines when a backup or restore fails, so each must say what to do in plain words and never carry
 * library vocabulary or digits, and each `BackupFailure` has its own line (docs/Plan/14a P6). The wrong-password line is the sentence the
 * owner approved (DECISIONS Round 40/41); the rest are reviewed in the P8 walk (D13).
 */
class BackupRestoreStringsTest {
    private val lines = BackupFailure.entries.map { BackupRestoreStrings.failure(it) }

    @Test
    fun theWrongPasswordLineIsTheOneTheOwnerApproved() {
        assertEquals("Wrong password, or this isn't a PayslipMax backup", BackupRestoreStrings.wrongPasswordOrNotABackup)
        assertEquals(BackupRestoreStrings.wrongPasswordOrNotABackup, BackupRestoreStrings.failure(BackupFailure.WRONG_PASSWORD))
    }

    @Test
    fun everyFailureHasItsOwnLine() {
        assertEquals(
            listOf(
                BackupRestoreStrings.wrongPasswordOrNotABackup,
                BackupRestoreStrings.notABackup,
                BackupRestoreStrings.newerVersion,
                BackupRestoreStrings.damaged,
                BackupRestoreStrings.tooLarge,
                BackupRestoreStrings.storageFull,
                BackupRestoreStrings.verifyFailed,
                BackupRestoreStrings.writeFailed,
                BackupRestoreStrings.failedGeneric,
            ),
            lines,
        )
        assertEquals(lines.size, lines.toSet().size, "no two failures read the same")
    }

    @Test
    fun noLineLooksLikeLibraryOrDeveloperText() {
        for (line in lines) {
            assertTrue(line.isNotBlank() && line.length in 20..120, "'$line' is a short sentence")
            assertTrue(line.none { it.isDigit() }, "'$line' holds no digits or codes")
            for (word in listOf("error:", "OPENSSL", "BAD_DECRYPT", "Cipher", "Exception", "GCM", "AES", "status", "_", "/")) {
                assertTrue(word !in line, "'$line' must not say '$word'")
            }
        }
    }
}
