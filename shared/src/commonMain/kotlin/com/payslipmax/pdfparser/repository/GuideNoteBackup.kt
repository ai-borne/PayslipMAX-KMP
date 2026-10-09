package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.GuideNoteEntity
import com.payslipmax.pdfparser.database.GuideNoteUnreadableException
import com.payslipmax.pdfparser.database.toEntity
import com.payslipmax.pdfparser.database.toNote
import com.payslipmax.pdfparser.guide.domain.GuideNote

/**
 * How the user's private Guide notes move in and out of a `.pcda` archive (backup v4). Owner decisions, 2026-10-09:
 * a note that cannot be read is skipped and everything else still restores; on MERGE the newer `updatedAt` wins for the
 * same card (a tie goes to the backup); notes are included for everyone, because the Premium gate on backup sits in the
 * Settings screen, not in the data layer. Nothing here logs, and no exception carries a note: the text is private.
 */
internal object GuideNoteBackup {
    /** Device rows re-encrypted under the backup [password]; a row this device cannot read is left out, like an unreadable payslip. */
    fun forExport(
        rows: List<GuideNoteEntity>,
        deviceKey: String,
        password: String,
    ): List<GuideNoteEntity> = readable(rows, deviceKey).map { it.toEntity(password) }

    /**
     * The rows to write for a restore, already re-encrypted under [deviceKey]. [existing] is what the device holds now and
     * is consulted only for MERGE; REPLACE writes every readable backup note.
     */
    fun forRestore(
        backupRows: List<GuideNoteEntity>,
        password: String,
        deviceKey: String,
        existing: List<GuideNoteEntity>,
        mode: RestoreMode,
    ): List<GuideNoteEntity> {
        val incoming = readable(backupRows, password)
        val onDevice = if (mode == RestoreMode.MERGE) readable(existing, deviceKey).associateBy { it.cardId } else emptyMap()
        return incoming
            .filter { backupNote -> onDevice[backupNote.cardId]?.let { it.updatedAt <= backupNote.updatedAt } ?: true }
            .map { it.toEntity(deviceKey) }
    }

    private fun readable(
        rows: List<GuideNoteEntity>,
        key: String,
    ): List<GuideNote> =
        rows.mapNotNull { row ->
            try {
                row.toNote(key)
            } catch (e: GuideNoteUnreadableException) {
                null
            }
        }
}
