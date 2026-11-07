package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (docs/Plan/14a P6, F5): the officer's line and the Crashlytics count both come from this one classification, so it must follow the
 * failure's type (and the type of what it wraps), never its text: a message can carry a path, a row or the crypto library's words, and a
 * message that happens to read "wrong password" must not pick the wrong-password line. The codes are what dashboards count, so they are
 * pinned as literals: renaming an enum constant, or R8 renaming it, must not change them.
 */
class BackupFailureTest {
    private val byType: List<Pair<Throwable, BackupFailure>> =
        listOf(
            DecryptionFailedException() to BackupFailure.WRONG_PASSWORD,
            NotABackupException() to BackupFailure.NOT_A_BACKUP,
            UnsupportedBackupVersionException(7) to BackupFailure.NEWER_VERSION,
            BackupDamagedException() to BackupFailure.DAMAGED,
            SerializationException("Unexpected JSON token at offset 12: {\"basicPay\":98765.43}") to BackupFailure.DAMAGED,
            BackupTooLargeException() to BackupFailure.TOO_LARGE,
            BackupStorageFullException() to BackupFailure.STORAGE_FULL,
            BackupVerifyException() to BackupFailure.VERIFY_FAILED,
            BackupFileException() to BackupFailure.WRITE_FAILED,
            IllegalStateException("anything else") to BackupFailure.UNKNOWN,
        )

    @Test
    fun everyFailureTypeHasItsOwnCode() {
        for ((error, expected) in byType) assertEquals(expected, BackupFailure.of(error), "for ${error::class.simpleName}")
        assertEquals(BackupFailure.UNKNOWN, BackupFailure.of(null), "a failure with no reason at all")
    }

    @Test
    fun aKnownFailureWrappedInAnotherIsStillFound() {
        for ((error, expected) in byType) {
            val once = RuntimeException("wrapper", error)
            val twice = IllegalStateException("outer", once)
            assertEquals(expected, BackupFailure.of(once), "once, for ${error::class.simpleName}")
            assertEquals(expected, BackupFailure.of(twice), "twice, for ${error::class.simpleName}")
        }
    }

    @Test
    fun theOutermostKnownTypeWinsOverWhatItWraps() {
        // The decode failed, whatever it met on the way: the document is what the officer's file got wrong.
        assertEquals(BackupFailure.DAMAGED, BackupFailure.of(SerializationException("bad", BackupStorageFullException())))
        assertEquals(BackupFailure.STORAGE_FULL, BackupFailure.of(RuntimeException("wrapper", BackupStorageFullException())))
    }

    @Test
    fun aMessageNeverDecidesTheCode() {
        for ((error, _) in byType.filter { it.second != BackupFailure.UNKNOWN }) {
            // The same words in a type nobody knows are still unknown.
            assertEquals(BackupFailure.UNKNOWN, BackupFailure.of(IllegalStateException(error.message)), "text of ${error::class.simpleName}")
        }
        assertEquals(BackupFailure.UNKNOWN, BackupFailure.of(Exception("Wrong password, or this isn't a PayslipMax backup")))
    }

    @Test
    fun theCodesAreTheStableValuesDashboardsCount() {
        assertEquals(
            listOf(
                "wrong_password", "not_a_backup", "newer_version", "damaged", "too_large",
                "storage_full", "verify_failed", "write_failed", "unknown",
            ),
            BackupFailure.entries.map { it.code },
        )
        assertEquals(listOf("backup", "restore"), BackupOp.entries.map { it.code })
    }
}
