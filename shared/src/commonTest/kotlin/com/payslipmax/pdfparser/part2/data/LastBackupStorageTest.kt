package com.payslipmax.pdfparser.part2.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: the Bank's "last backup" line must say the truth about the last backup the officer made, and say "never" before the first. It is a
 * device-local number, not part of the encrypted database or of the backup file itself, so a restore onto a new phone does not claim a
 * backup that phone never made.
 */
class LastBackupStorageTest {
    @Test
    fun beforeAnyBackupThereIsNoTime() {
        assertNull(InMemoryLastBackupStorage().load())
    }

    @Test
    fun theLastTimeSavedIsTheOneRead() {
        val storage = InMemoryLastBackupStorage()
        storage.save(1_700_000_000_000L)
        storage.save(1_800_000_000_000L)
        assertEquals(1_800_000_000_000L, storage.load())
    }
}
