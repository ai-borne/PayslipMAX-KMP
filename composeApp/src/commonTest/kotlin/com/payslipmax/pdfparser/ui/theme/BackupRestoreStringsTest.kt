package com.payslipmax.pdfparser.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the officer reads these two lines when a restore fails, so they must say what to do in plain words and never carry library
 * vocabulary or digits. The first is the sentence the owner approved (DECISIONS Round 40/41).
 */
class BackupRestoreStringsTest {
    private val lines = listOf(BackupRestoreStrings.wrongPasswordOrNotABackup, BackupRestoreStrings.restoreFailedGeneric)

    @Test
    fun theWrongPasswordLineIsTheOneTheOwnerApproved() {
        assertEquals("Wrong password, or this isn't a PayslipMax backup", BackupRestoreStrings.wrongPasswordOrNotABackup)
    }

    @Test
    fun noLineLooksLikeLibraryOrDeveloperText() {
        for (line in lines) {
            assertTrue(line.isNotBlank() && line.length in 20..120, "'$line' is a short sentence")
            assertTrue(line.none { it.isDigit() }, "'$line' holds no digits or codes")
            for (word in listOf("error:", "OPENSSL", "BAD_DECRYPT", "Cipher", "Exception", "GCM", "AES", "status")) {
                assertTrue(word !in line, "'$line' must not say '$word'")
            }
        }
    }

    @Test
    fun theTwoLinesDiffer() = assertTrue(lines[0] != lines[1])
}
