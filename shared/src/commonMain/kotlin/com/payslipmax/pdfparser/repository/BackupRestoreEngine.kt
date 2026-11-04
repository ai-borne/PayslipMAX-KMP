package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.crypto.SealKey
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.BackupRows
import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.database.toCorrectionEntity
import com.payslipmax.pdfparser.database.toCorrectionList
import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.Part2Restore
import com.payslipmax.pdfparser.repository.backup.BackupEnvelope
import com.payslipmax.pdfparser.repository.backup.BackupJson
import com.payslipmax.pdfparser.repository.backup.OpenedArchive
import com.payslipmax.pdfparser.repository.backup.SettingsBackup
import com.payslipmax.pdfparser.repository.backup.applyTo
import com.payslipmax.pdfparser.repository.backup.openArchive
import com.payslipmax.pdfparser.repository.backup.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException

/**
 * A backup opened, decoded, checked and re-sealed for this device, with nothing written yet (docs/Plan/14a P4). It holds the archive's key
 * and reads pages from the file, so the file must stay open until [close], which wipes the key. [rows] are what a REPLACE writes.
 */
class PreparedRestore internal constructor(
    val preview: RestorePreview,
    internal val opened: OpenedArchive,
    internal val backup: PortableBackup,
    internal val rows: BackupRows,
) : AutoCloseable {
    /** True once [close] has run (the key is wiped); the view model's tests check every exit from a preview reaches it. */
    var isClosed: Boolean = false
        private set

    override fun close() {
        opened.close()
        isClosed = true
    }
}

/**
 * The restore, split in two (F4): [prepare] does everything that can fail (password, version, every row and page) and writes nothing;
 * [commit] is the one database transaction. A failure is thrown as itself. A row that will not open inside an archive that did is skipped
 * and counted per kind (F6, the Part II rule); a file in which no row opens is [BackupDamagedException] (see [requireARowThatOpened]).
 */
internal class BackupRestoreEngine(
    private val payslipDao: PayslipDao,
    private val part2Backup: Part2Backup?,
) {
    suspend fun prepare(
        source: BackupSource,
        password: String,
        progress: BackupProgress,
    ): PreparedRestore {
        progress.update(BackupStage.OPENING, 0, 0)
        // A wrong password, a file that is not a backup or one from a later app fails here, as itself: nothing inside has been read.
        val opened = openArchive(source, password)
        try {
            return damagedIfUnreadable { prepareOpened(opened, progress) }
        } catch (e: Throwable) {
            opened.close()
            throw e
        }
    }

    private suspend fun prepareOpened(
        opened: OpenedArchive,
        progress: BackupProgress,
    ): PreparedRestore {
        val backupKey = opened.key
        val backup = decodeDocument(opened.document)
        BackupEnvelope.requireKnownFormat(backup.version)
        val deviceKey = CryptoHelper.getDatabaseSecretKey()
        val total = backup.encryptedPayslips.size + backup.corrections.size
        progress.update(BackupStage.READING, 0, total)
        // Every payslip and correction is opened and re-sealed for this device before anything is written; one that will not open is
        // skipped and counted (F6). Only the opening is forgiven: a failure to seal for this device is not the file's, so it stays fatal.
        val payslips =
            backup.encryptedPayslips.mapIndexedNotNull { i, row ->
                val domain =
                    openedOrNull {
                        val entity = row.toEntity()
                        try {
                            entity.toDomain(backupKey)
                        } catch (e: Exception) {
                            // Version 1 backups are sealed with the legacy key.
                            entity.toDomain(CryptoHelper.getLegacyFallbackKey())
                        }
                    }
                domain?.toEncryptedEntity(deviceKey).also { progress.update(BackupStage.READING, i + 1, total) }
            }
        val corrections =
            backup.corrections.mapIndexedNotNull { i, row ->
                val list = openedOrNull { row.toEntity().toCorrectionList(backupKey) }
                list?.toCorrectionEntity(row.dateStr, deviceKey).also { progress.update(BackupStage.READING, backup.encryptedPayslips.size + i + 1, total) }
            }
        // Part II rows are size- and count-checked before any is decoded, and every page is opened once; what fails is skipped and counted.
        val part2 = part2Backup?.restore(backup, opened.pages, backupKey, RestoreMode.REPLACE)
        val notes = notes(backup, backupKey, deviceKey, RestoreMode.REPLACE)
        val rows =
            BackupRows(
                payslips = payslips,
                pdfs = backup.pdfs.map { it.toEntity() },
                drafts = backup.drafts.map { it.toEntity() },
                dismissedDrafts = backup.dismissedDrafts.map { it.toEntity() },
                corrections = corrections,
                guideNotes = notes.rows,
            ).withPart2(part2)
        val skipped =
            SkippedRows(
                payslips = backup.encryptedPayslips.size - payslips.size,
                corrections = backup.corrections.size - corrections.size,
                guideNotes = notes.skipped,
            ).withPart2(part2)
        requireARowThatOpened(rows, skipped, backup.part2Drafts.size)
        return PreparedRestore(preview(backup, rows, part2, skipped), opened, backup, rows)
    }

    /**
     * "Every row unreadable" (14a P5) means: every row that is opened one at a time (payslips, corrections, notes, Bank entries, orders and
     * page records) was skipped, and there was at least one. Such a file is damaged, never a restore of nothing (on REPLACE that would wipe
     * the phone). PDFs, letters, deleted-letter records and settings cannot fail one at a time once the document opens, and frozen-branch
     * drafts are never restored, so neither counts here. A page restores only with an order that did, so the orders stand for the pages.
     */
    private fun requireARowThatOpened(
        rows: BackupRows,
        skipped: SkippedRows,
        legacyDrafts: Int,
    ) {
        val opened = rows.payslips.size + rows.corrections.size + rows.guideNotes.size + rows.part2Entries.size + rows.part2Orders.size
        if (opened == 0 && skipped.total - legacyDrafts > 0) throw BackupDamagedException()
    }

    /**
     * Writes [prepared] in one transaction. REPLACE makes the device an exact copy of the backup (every user and derived table emptied
     * first) and keeps the device-only settings (D5); MERGE keeps the device's data and settings and layers the backup on top, so its notes
     * and Part II rows are reconciled here against what the device holds now.
     */
    suspend fun commit(
        prepared: PreparedRestore,
        mode: RestoreMode,
        progress: BackupProgress,
    ): RestoreReport =
        damagedIfUnreadable {
            val merge = mode == RestoreMode.MERGE
            val part2 = if (merge) part2Backup?.restore(prepared.backup, prepared.opened.pages, prepared.opened.key, mode) else null
            val rows =
                if (merge) {
                    val notes = notes(prepared.backup, prepared.opened.key, CryptoHelper.getDatabaseSecretKey(), mode)
                    prepared.rows.copy(guideNotes = notes.rows).withPart2(part2)
                } else {
                    prepared.rows
                }
            val pageTotal = part2?.pageCount ?: prepared.preview.part2Pages
            var written = 0
            val counted = rows.copy(part2Pages = rows.part2Pages.map { it.also { progress.update(BackupStage.PAGES, ++written, pageTotal) } })
            if (merge) {
                payslipDao.mergeBackup(counted)
            } else {
                val device = payslipDao.getSettings() ?: AppSettingsEntity()
                payslipDao.replaceWithBackup(counted, (prepared.backup.settings ?: SettingsBackup()).applyTo(device))
            }
            // A MERGE re-runs Part II against the device (room left, newer rows), so its Part II counts replace the preview's.
            RestoreReport(if (part2 != null) prepared.preview.skipped.withPart2(part2) else prepared.preview.skipped)
        }

    /** An unreadable note is skipped and counted (see [GuideNoteBackup]); only a MERGE needs the notes the device holds now. */
    private suspend fun notes(
        backup: PortableBackup,
        backupKey: SealKey,
        deviceKey: String,
        mode: RestoreMode,
    ): NoteRestore {
        val existing = if (mode == RestoreMode.MERGE) payslipDao.getAllGuideNotes().first() else emptyList()
        return GuideNoteBackup.forRestore(backup.guideNotes.map { it.toEntity() }, backupKey, deviceKey, existing, mode)
    }

    private fun BackupRows.withPart2(part2: Part2Restore?) =
        copy(part2Entries = part2?.entries.orEmpty(), part2Orders = part2?.orders.orEmpty(), part2Pages = part2?.pages ?: emptySequence())

    /** These counts with the Part II ones taken from [part2] (all zero without Part II). */
    private fun SkippedRows.withPart2(part2: Part2Restore?): SkippedRows {
        val counted = part2?.skipped ?: SkippedRows.NONE
        return copy(part2Entries = counted.part2Entries, part2Orders = counted.part2Orders, part2Pages = counted.part2Pages)
    }

    private fun preview(
        backup: PortableBackup,
        rows: BackupRows,
        part2: Part2Restore?,
        skipped: SkippedRows,
    ) = RestorePreview(
        formatVersion = backup.version,
        exportedAt = backup.exportedAt,
        payslips = rows.payslips.size,
        pdfs = rows.pdfs.size,
        corrections = rows.corrections.size,
        guideNotes = rows.guideNotes.size,
        drafts = rows.drafts.size,
        dismissedDrafts = rows.dismissedDrafts.size,
        part2Entries = rows.part2Entries.size,
        part2Orders = rows.part2Orders.size,
        part2Pages = part2?.pageCount ?: 0,
        skipped = skipped,
    )

    /**
     * One row opened with the archive's key, or null when it will not (cipher, hex or JSON): it is skipped and counted, and nothing of it,
     * nor the exception (kotlinx can quote the JSON in its message), is kept or logged.
     */
    private inline fun <T : Any> openedOrNull(open: () -> T): T? =
        try {
            open()
        } catch (e: Exception) {
            null
        }

    /**
     * The document, which opened with the password: one that does not decode is a damaged file. kotlinx's message quotes the JSON (payslip
     * values included), so it is dropped here and never reaches a `Result` (P6, P10 "From P2" item 2).
     */
    private fun decodeDocument(document: ByteArray): PortableBackup =
        try {
            BackupJson.decode(PortableBackup.serializer(), document.decodeToString())
        } catch (e: SerializationException) {
            throw BackupDamagedException()
        }

    /** The outer layer opened (a wrong password failed before this), so a row inside that will not open is a damaged file. */
    private inline fun <T> damagedIfUnreadable(block: () -> T): T =
        try {
            block()
        } catch (e: DecryptionFailedException) {
            throw BackupDamagedException()
        }
}
