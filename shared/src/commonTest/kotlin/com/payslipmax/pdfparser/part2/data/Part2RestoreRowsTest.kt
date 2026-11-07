package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.PasswordKey
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: [restoreRows] is where an untrusted backup meets the device's own rows. The officer's existing entries must never be replaced by
 * an OLDER copy on MERGE, a duplicate id in a hostile file must not win twice, and the device's caps must hold. These are checked on
 * the function itself (the backup-level tests only see its totals), with the exact ids each rule keeps or drops.
 */
class Part2RestoreRowsTest {
    private val backupKey = PasswordKey(BACKUP_PASSWORD)

    private fun restore(
        rows: List<Part2RowBackup>,
        device: List<Part2EntryEntity> = emptyList(),
        merge: Boolean = false,
        kind: RowKind<Part2RowBackup, BankEntry, Part2EntryEntity> = entryKind,
    ) = restoreRows(kind, rows, device, backupKey, deviceKey, merge) { Base64.decode(it) }

    private fun ids(pass: RowPass<Part2EntryEntity>) = pass.rows.map { it.id }

    private fun onDevice(
        id: String,
        updatedAt: Long,
    ) = SyntheticBank.entry(id, updatedAt).toEntity(deviceKey)

    private fun entryKindCappedAt(max: Int) =
        RowKind(max, entryKind.id, entryKind.data, entryKind.deviceId, entryKind.fromBackup, entryKind.fromDevice, entryKind.itemId, entryKind.updatedAt, entryKind.seal)

    @Test
    fun readableRowsAreKeptAndReSealedForThisDevice() {
        val pass = restore(listOf(entryRow(SyntheticBank.entry("e1")), entryRow(SyntheticBank.entry("e2"))))

        assertEquals(listOf("e1", "e2"), ids(pass))
        assertEquals(0, pass.skipped)
        assertEquals(setOf("e1", "e2"), pass.kept)
        assertEquals("e1", pass.rows.first().toEntry(deviceKey).id, "the stored row opens with the DEVICE key, not the backup password")
    }

    @Test
    fun anUnreadableRowAnIllShapedIdAndADuplicateAreSkippedAndCounted() {
        val rows =
            listOf(
                entryRow(SyntheticBank.entry("e1")),
                junkEntry(1),
                Part2RowBackup("bad/id", entryRow(SyntheticBank.entry("x")).data),
                entryRow(SyntheticBank.entry("e1", updatedAt = 9_000L)),
            )

        val pass = restore(rows)

        assertEquals(listOf("e1"), ids(pass))
        assertEquals(3, pass.skipped, "undecodable + bad id + the second e1")
        assertEquals(1_000L, pass.rows.single().toEntry(deviceKey).updatedAt, "the first copy of a duplicated id wins")
    }

    @Test
    fun onMergeANewerDeviceRowIsKeptAndNotCountedAsSkipped() {
        val pass = restore(listOf(entryRow(SyntheticBank.entry("e1", updatedAt = 1_000L))), listOf(onDevice("e1", 5_000L)), merge = true)

        assertEquals(emptyList(), ids(pass), "the older backup copy must not overwrite the officer's newer edit")
        assertEquals(0, pass.skipped, "leaving a newer row alone is not a failure")
        assertEquals(setOf("e1"), pass.heldIds)
    }

    @Test
    fun onMergeATieGoesToTheBackupAndAnOlderDeviceRowIsReplaced() {
        val tie = restore(listOf(entryRow(SyntheticBank.entry("e1", updatedAt = 5_000L))), listOf(onDevice("e1", 5_000L)), merge = true)
        val older = restore(listOf(entryRow(SyntheticBank.entry("e1", updatedAt = 9_000L))), listOf(onDevice("e1", 5_000L)), merge = true)

        assertEquals(listOf("e1"), ids(tie))
        assertEquals(listOf("e1"), ids(older))
    }

    @Test
    fun onReplaceTheDeviceRowsAreNotConsultedSoANewerOneDoesNotBlockTheBackup() {
        val pass = restore(listOf(entryRow(SyntheticBank.entry("e1", updatedAt = 1_000L))), listOf(onDevice("e1", 5_000L)), merge = false)

        assertEquals(listOf("e1"), ids(pass))
    }

    @Test
    fun rowsPastTheKindsMaximumAreCountedWithoutBeingRead() {
        val capped = entryKindCappedAt(2)
        val rows = (1..3).map { entryRow(SyntheticBank.entry("e$it")) }

        val pass = restore(rows, kind = capped)

        assertEquals(listOf("e1", "e2"), ids(pass))
        assertEquals(1, pass.skipped)
    }

    @Test
    fun aNewRowFitsOnlyInTheRoomTheDeviceHasLeftButAnExistingIdAlwaysFits() {
        val capped = entryKindCappedAt(2)
        val device = listOf(onDevice("d1", 1_000L))

        val pass = restore(listOf(entryRow(SyntheticBank.entry("n1")), entryRow(SyntheticBank.entry("n2"))), device, merge = true, kind = capped)
        val sameId = restore(listOf(entryRow(SyntheticBank.entry("d1", updatedAt = 9_000L))), device, merge = true, kind = capped)

        assertEquals(listOf("n1"), ids(pass), "one slot was left")
        assertEquals(1, pass.skipped)
        assertEquals(listOf("d1"), ids(sameId), "updating a row the device holds needs no room")
    }
}
