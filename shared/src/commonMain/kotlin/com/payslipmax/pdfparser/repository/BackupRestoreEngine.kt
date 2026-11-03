package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.crypto.SealKey
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.BackupRows
import com.payslipmax.pdfparser.database.GuideNoteEntity
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
    override fun close() = opened.close()
}

/**
 * The restore, split in two (F4): [prepare] does everything that can fail (password, version, every row and page) and writes nothing;
 * [commit] is the one database transaction. A failure is thrown as itself; a row that will not open inside an archive that did is
 * [BackupDamagedException].
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
        val backup = BackupJson.decode(PortableBackup.serializer(), opened.document.decodeToString())
        BackupEnvelope.requireKnownFormat(backup.version)
        val deviceKey = CryptoHelper.getDatabaseSecretKey()
        val total = backup.encryptedPayslips.size + backup.corrections.size
        progress.update(BackupStage.READING, 0, total)
        // Every payslip is decoded and re-sealed for this device before anything is written, so one unreadable row fails here.
        val payslips =
            backup.encryptedPayslips.mapIndexed { i, row ->
                val entity = row.toEntity()
                val domain =
                    try {
                        entity.toDomain(backupKey)
                    } catch (e: Exception) {
                        // Version 1 backups are sealed with the legacy key.
                        entity.toDomain(CryptoHelper.getLegacyFallbackKey())
                    }
                domain.toEncryptedEntity(deviceKey).also { progress.update(BackupStage.READING, i + 1, total) }
            }
        val corrections =
            backup.corrections.mapIndexed { i, row ->
                row.toEntity().toCorrectionList(backupKey).toCorrectionEntity(row.dateStr, deviceKey).also {
                    progress.update(BackupStage.READING, backup.encryptedPayslips.size + i + 1, total)
                }
            }
        // Part II rows are size- and count-checked before any is decoded, and every page is opened once; what fails is skipped and counted.
        val part2 = part2Backup?.restore(backup, opened.pages, backupKey, RestoreMode.REPLACE)
        val rows =
            BackupRows(
                payslips = payslips,
                pdfs = backup.pdfs.map { it.toEntity() },
                drafts = backup.drafts.map { it.toEntity() },
                dismissedDrafts = backup.dismissedDrafts.map { it.toEntity() },
                corrections = corrections,
                guideNotes = notes(backup, backupKey, deviceKey, RestoreMode.REPLACE),
            ).withPart2(part2)
        return PreparedRestore(preview(backup, rows, part2), opened, backup, rows)
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
                    prepared.rows.copy(guideNotes = notes).withPart2(part2)
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
            RestoreReport(part2?.skipped ?: prepared.preview.skipped)
        }

    /** An unreadable note is skipped (see [GuideNoteBackup]); only a MERGE needs the notes the device holds now. */
    private suspend fun notes(
        backup: PortableBackup,
        backupKey: SealKey,
        deviceKey: String,
        mode: RestoreMode,
    ): List<GuideNoteEntity> {
        val existing = if (mode == RestoreMode.MERGE) payslipDao.getAllGuideNotes().first() else emptyList()
        return GuideNoteBackup.forRestore(backup.guideNotes.map { it.toEntity() }, backupKey, deviceKey, existing, mode)
    }

    private fun BackupRows.withPart2(part2: Part2Restore?) =
        copy(part2Entries = part2?.entries.orEmpty(), part2Orders = part2?.orders.orEmpty(), part2Pages = part2?.pages ?: emptySequence())

    private fun preview(
        backup: PortableBackup,
        rows: BackupRows,
        part2: Part2Restore?,
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
        skipped = part2?.skipped ?: 0,
    )

    /** The outer layer opened (a wrong password failed before this), so a row inside that will not open is a damaged file. */
    private inline fun <T> damagedIfUnreadable(block: () -> T): T =
        try {
            block()
        } catch (e: DecryptionFailedException) {
            throw BackupDamagedException()
        }
}
