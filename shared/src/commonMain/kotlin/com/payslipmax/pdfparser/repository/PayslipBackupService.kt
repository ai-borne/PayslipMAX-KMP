package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.SealKey
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.logging.Logger
import com.payslipmax.pdfparser.part2.data.LastBackupStorage
import com.payslipmax.pdfparser.part2.data.PageArchiveWriter
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.space.FreeSpaceGate
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.repository.backup.CorrectionBackupRow
import com.payslipmax.pdfparser.repository.backup.newArchive
import com.payslipmax.pdfparser.repository.backup.toBackupRow
import com.payslipmax.pdfparser.repository.backup.toSettingsBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Exports and restores the portable, password-encrypted `.pcda` backup archive. Split out of
 * [PayslipRepository] so the backup rules (what travels, how a restore reconciles) live in one place.
 */
class PayslipBackupService(
    private val payslipDao: PayslipDao,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default,
    private val payslipRepository: PayslipRepository? = null,
    private val intelligence: FinancialIntelligenceRepository? = null,
    // Null only in tests that do not care about Part II; the Koin graph always passes it (Part2WiringTest), so a build cannot drop the Bank.
    private val part2Backup: Part2Backup? = null,
    // Where the time of the last successful export is kept (device-local, never in the archive); null where nobody shows it.
    private val lastBackup: LastBackupStorage? = null,
    private val now: () -> Long = { currentTimeMillis() },
    // Free space on the device's volume, checked before a backup is written or a restore starts; unknown never blocks (FreeSpaceGate).
    private val space: FreeSpaceGate = FreeSpaceGate.Unknown,
) {
    private val engine = BackupRestoreEngine(payslipDao, part2Backup)

    /** How many Orders Bank entries and vault orders a backup would carry; 0 where Part II is not wired in. */
    suspend fun part2ItemCount(): Int = withContext(dispatcher) { part2Backup?.itemCount() ?: 0 }

    /**
     * Exports everything the user created (payslips, PDFs, settings, letters, deleted-letter records,
     * corrections, private Guide notes, Orders Bank entries, orders and pages) as an encrypted archive. Derived data (ledger, insights) is not exported.
     */
    suspend fun export(password: String): Result<ByteArray> {
        // The archive's bytes only, for callers that hold a backup in memory (tests): no disk is involved, so there is nothing to read
        // back, and nothing is recorded. A backup the officer keeps goes through [backUp].
        val sink = MemoryBackupSink()
        return exportTo(sink, password).map { sink.toByteArray() }
    }

    /**
     * Writes the archive into [files]' export file, reads it back from there ([verify]) and only then records the backup time (F4). A
     * failure leaves no file: [BackupFiles.write] deletes it, so the share sheet can never offer a backup that did not read back. A device
     * without room for the file (about the vault's size) and the margin is refused first, with nothing written (P6).
     */
    suspend fun backUp(
        files: BackupFiles,
        password: String,
        progress: BackupProgress = BackupProgress.NONE,
    ): Result<Unit> {
        val vault = runCatching { withContext(dispatcher) { part2Backup?.vaultBytes() ?: 0L } }.getOrElse { return Result.failure(it) }
        if (!space.hasRoomFor(vault)) return Result.failure(BackupStorageFullException())
        val at = now()
        return files
            .write(verify = { source -> verify(source, password, progress) }) { sink -> exportTo(sink, password, at, progress) }
            .onSuccess { lastBackup?.save(at) }
    }

    /**
     * A dry-run restore of a just-written file (every page opened once, nothing written or kept). A fresh backup must read back whole: a
     * failure, or any row of any kind the restore would leave out (an unreadable row is skipped, not fatal), is [BackupVerifyException].
     */
    private suspend fun verify(
        source: BackupSource,
        password: String,
        progress: BackupProgress,
    ): Result<Unit> =
        open(source, password) { _, done, total -> progress.update(BackupStage.VERIFYING, done, total) }
            .mapCatching { prepared -> prepared.use { check(it.preview.skipped == SkippedRows.NONE) } }
            .recoverCatching { throw BackupVerifyException() }

    /**
     * The same archive written to [sink] piece by piece, so a backup of any size never exists as one array. The file is written as it goes:
     * when this returns a failure the sink holds a partial file, which the caller discards. Nothing is recorded or checked here: see [backUp].
     */
    suspend fun exportTo(
        sink: BackupSink,
        password: String,
        exportedAt: Long = now(),
        progress: BackupProgress = BackupProgress.NONE,
    ): Result<Unit> =
        withContext(dispatcher) {
            // Format 6: one key for the whole archive, derived once here from a fresh salt; every row and page below is sealed with it.
            val archive =
                try {
                    newArchive(password)
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }
            try {
                val payslips = payslipDao.getAllPayslips().first()
                val pdfs = payslipDao.getAllPdfs()
                val settings = payslipDao.getSettings()

                val deviceKey = CryptoHelper.getDatabaseSecretKey()
                val backupKey = archive.key
                // Skip any row that can't be decrypted (e.g. a stale/legacy-key or corrupt row) rather
                // than failing the whole backup — matches the read path (getAllPayslips), so a backup
                // contains exactly the payslips the user can actually see.
                progress.update(BackupStage.READING, 0, payslips.size)
                val exportedPayslips =
                    payslips.mapNotNull { entity ->
                        try {
                            entity.toDomain(deviceKey).toEncryptedEntity(backupKey).toBackupRow()
                        } catch (e: Exception) {
                            // A fixed line: the month is row metadata and the exception can quote the payslip (P6).
                            Logger.w(TAG, "A payslip that would not open was left out of the backup")
                            null
                        }
                    }

                val part2 = part2Backup?.export(backupKey)
                val backup =
                    PortableBackup(
                        version = PortableBackup.CURRENT_VERSION,
                        encryptedPayslips = exportedPayslips,
                        pdfs = pdfs.map { it.toBackupRow() },
                        // Preferences only: the device-only settings (D5) are not on SettingsBackup, so they cannot be exported.
                        settings = settings?.toSettingsBackup(),
                        drafts = payslipDao.getAllRepresentationDrafts().first().map { it.toBackupRow() },
                        dismissedDrafts = payslipDao.getAllDismissedDrafts().map { it.toBackupRow() },
                        corrections = exportCorrections(deviceKey, backupKey),
                        guideNotes =
                            GuideNoteBackup
                                .forExport(payslipDao.getAllGuideNotes().first(), deviceKey, backupKey)
                                .map { it.toBackupRow() },
                        part2Entries = part2?.entries.orEmpty(),
                        part2Orders = part2?.orders.orEmpty(),
                        exportedAt = exportedAt,
                    )
                progress.update(BackupStage.READING, payslips.size, payslips.size)

                val jsonStr = Json.encodeToString(PortableBackup.serializer(), backup)
                val jsonBytes = jsonStr.encodeToByteArray()

                // The plaintext header, then a page archive (always, so a reader sees one shape): this document followed by one binary record
                // per page, streamed to the sink a page at a time, so a full vault is never copied through a JSON string or held in one array.
                val meta = backupKey.seal(jsonBytes).getOrThrow()
                sink.write(archive.header)
                val writer = PageArchiveWriter(sink)
                writer.begin(meta)
                val orderIds = part2?.orders.orEmpty().map { it.id }.toSet()
                val pages = part2Backup?.writePages(writer, backupKey, orderIds) { done, total -> progress.update(BackupStage.PAGES, done, total) }
                writer.finish(pages ?: 0, backupKey)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                archive.close()
            }
        }

    /** Corrections move from the device key to the backup's key so the archive is self-contained. */
    private suspend fun exportCorrections(
        deviceKey: String,
        backupKey: SealKey,
    ): List<CorrectionBackupRow> =
        payslipDao.getAllCorrections().first().mapNotNull { entity ->
            try {
                entity.toCorrectionList(deviceKey).toCorrectionEntity(entity.dateStr, backupKey).toBackupRow()
            } catch (e: Exception) {
                Logger.w(TAG, "Corrections that would not open were left out of the backup")
                null
            }
        }

    /**
     * Decrypts and imports a universal backup archive.
     */
    suspend fun restore(
        backupBytes: ByteArray,
        password: String,
        mode: RestoreMode = RestoreMode.REPLACE,
    ): Result<RestoreReport> = restore(MemoryBackupSource(backupBytes), password, mode)

    /** The same restore from a file read by range: only the document and one page at a time are ever in memory. The caller closes [source] afterwards. */
    suspend fun restore(
        source: BackupSource,
        password: String,
        mode: RestoreMode = RestoreMode.REPLACE,
    ): Result<RestoreReport> = prepare(source, password).mapCatching { prepared -> prepared.use { commit(it, mode).getOrThrow() } }

    /**
     * Opens, decodes and checks the whole backup (every row and page) and re-seals it for this device, writing nothing (F4); the preview
     * says what a [commit] will write. [source] must stay open until the result is closed. A device without room for what the commit
     * writes is refused first ([BackupStorageFullException], P6): the rows go into the database's write-ahead log and then its file, so
     * about [RESTORE_WRITES] times the file's size, plus the margin every write keeps free.
     */
    suspend fun prepare(
        source: BackupSource,
        password: String,
        progress: BackupProgress = BackupProgress.NONE,
    ): Result<PreparedRestore> {
        if (!space.hasRoomFor(RESTORE_WRITES * source.size)) return Result.failure(BackupStorageFullException())
        return open(source, password, progress)
    }

    /** [prepare] without the space check: the read-back of a just-written backup writes nothing. */
    private suspend fun open(
        source: BackupSource,
        password: String,
        progress: BackupProgress,
    ): Result<PreparedRestore> =
        withContext(dispatcher) {
            try {
                Result.success(engine.prepare(source, password, progress))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Writes what [prepare] checked, in one transaction, then rebuilds the ledger and insights. The caller closes [prepared]. */
    suspend fun commit(
        prepared: PreparedRestore,
        mode: RestoreMode,
        progress: BackupProgress = BackupProgress.NONE,
    ): Result<RestoreReport> =
        withContext(dispatcher) {
            try {
                engine.commit(prepared, mode, progress).also { rebuildDerivedData() }.let { Result.success(it) }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * The ledger and insights are never stored in a backup, so they are rebuilt from the restored payslips
     * (corrections applied, as Pay Audit shows them). The restore is already committed by now, so a failure
     * here is logged without PII and left to the startup repair instead of failing the restore.
     */
    private suspend fun rebuildDerivedData() {
        val repository = payslipRepository ?: return
        val intelligence = intelligence ?: return
        try {
            intelligence.rebuildAuditHistory(repository.getAllPayslips().first())
        } catch (e: Exception) {
            Logger.e(TAG, "Rebuilding ledger and insights after restore failed")
        }
    }

    private companion object {
        const val TAG = "PayslipBackupService"

        /** A restore writes its rows about twice before space is given back: the write-ahead log, then the database file. */
        const val RESTORE_WRITES = 2
    }
}
