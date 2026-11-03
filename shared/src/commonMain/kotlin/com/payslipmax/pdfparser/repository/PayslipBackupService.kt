package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.logging.Logger
import com.payslipmax.pdfparser.part2.data.LastBackupStorage
import com.payslipmax.pdfparser.part2.data.PageArchive
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.repository.backup.BackupJson
import com.payslipmax.pdfparser.repository.backup.CorrectionBackupRow
import com.payslipmax.pdfparser.repository.backup.SettingsBackup
import com.payslipmax.pdfparser.repository.backup.applyTo
import com.payslipmax.pdfparser.repository.backup.toBackupRow
import com.payslipmax.pdfparser.repository.backup.toEntity
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
) {
    /** How many Orders Bank entries and vault orders a backup would carry; 0 where Part II is not wired in. */
    suspend fun part2ItemCount(): Int = withContext(dispatcher) { part2Backup?.itemCount() ?: 0 }

    /**
     * Exports everything the user created (payslips, PDFs, settings, letters, deleted-letter records,
     * corrections, private Guide notes, Orders Bank entries, orders and pages) as an encrypted archive. Derived data (ledger, insights) is not exported.
     */
    suspend fun export(password: String): Result<ByteArray> {
        val sink = MemoryBackupSink()
        return exportTo(sink, password).map { sink.toByteArray() }
    }

    /**
     * The same archive written to [sink] piece by piece, so a backup of any size never exists as one array. The file is written as it goes:
     * when this returns a failure the sink holds a partial file, which the caller discards, and no backup time is recorded.
     */
    suspend fun exportTo(
        sink: BackupSink,
        password: String,
    ): Result<Unit> =
        withContext(dispatcher) {
            try {
                val payslips = payslipDao.getAllPayslips().first()
                val pdfs = payslipDao.getAllPdfs()
                val settings = payslipDao.getSettings()

                val deviceKey = CryptoHelper.getDatabaseSecretKey()
                // Skip any row that can't be decrypted (e.g. a stale/legacy-key or corrupt row) rather
                // than failing the whole backup — matches the read path (getAllPayslips), so a backup
                // contains exactly the payslips the user can actually see.
                val exportedPayslips =
                    payslips.mapNotNull { entity ->
                        try {
                            entity.toDomain(deviceKey).toEncryptedEntity(password).toBackupRow()
                        } catch (e: Exception) {
                            Logger.e("PayslipBackupService", "Skipping undecryptable payslip ${entity.dateStr} during backup", e)
                            null
                        }
                    }

                val part2 = part2Backup?.export(password)
                val backup =
                    PortableBackup(
                        version = PortableBackup.CURRENT_VERSION,
                        encryptedPayslips = exportedPayslips,
                        pdfs = pdfs.map { it.toBackupRow() },
                        // Preferences only: the device-only settings (D5) are not on SettingsBackup, so they cannot be exported.
                        settings = settings?.toSettingsBackup(),
                        drafts = payslipDao.getAllRepresentationDrafts().first().map { it.toBackupRow() },
                        dismissedDrafts = payslipDao.getAllDismissedDrafts().map { it.toBackupRow() },
                        corrections = exportCorrections(deviceKey, password),
                        guideNotes =
                            GuideNoteBackup
                                .forExport(payslipDao.getAllGuideNotes().first(), deviceKey, password)
                                .map { it.toBackupRow() },
                        part2Entries = part2?.entries.orEmpty(),
                        part2Orders = part2?.orders.orEmpty(),
                    )

                val jsonStr = Json.encodeToString(PortableBackup.serializer(), backup)
                val jsonBytes = jsonStr.encodeToByteArray()

                // With scanned pages the archive is this document followed by one binary record per page (PageArchive), streamed to the sink
                // a page at a time, so a full vault is never copied through a JSON string or held in one array. Without pages it is the one blob.
                CryptoHelper.encrypt(jsonBytes, password)
                    .mapCatching { meta -> if (part2Backup?.writeArchive(sink, meta, password) != true) sink.write(meta) }
                    .onSuccess { lastBackup?.save(now()) }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Corrections move from the device key to the backup password so the archive is self-contained. */
    private suspend fun exportCorrections(
        deviceKey: String,
        password: String,
    ): List<CorrectionBackupRow> =
        payslipDao.getAllCorrections().first().mapNotNull { entity ->
            try {
                entity.toCorrectionList(deviceKey).toCorrectionEntity(entity.dateStr, password).toBackupRow()
            } catch (e: Exception) {
                Logger.e("PayslipBackupService", "Skipping undecryptable corrections for ${entity.dateStr} during backup", e)
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
    ): Result<RestoreReport> =
        withContext(dispatcher) {
            try {
                val decryptResult = CryptoHelper.decrypt(PageArchive.metaOf(source) ?: wholeBlob(source), password)
                if (decryptResult.isFailure) {
                    return@withContext Result.failure(
                        decryptResult.exceptionOrNull() ?: Exception("Decryption failed"),
                    )
                }

                val jsonStr = decryptResult.getOrThrow().decodeToString()
                val backup = BackupJson.decode(PortableBackup.serializer(), jsonStr)
                if (backup.version > PortableBackup.CURRENT_VERSION) {
                    return@withContext Result.failure(UnsupportedBackupVersionException(backup.version))
                }

                // Decode and re-encrypt every payslip *before* touching the database: one unreadable row
                // then fails the restore with the device's data untouched, instead of after a wipe.
                val deviceKey = CryptoHelper.getDatabaseSecretKey()
                val databasePayslips =
                    backup.encryptedPayslips.map { row ->
                        val entity = row.toEntity()
                        val domainModel =
                            try {
                                entity.toDomain(password)
                            } catch (e: Exception) {
                                // Fallback: Version 1 backups are encrypted with the legacy key
                                entity.toDomain(CryptoHelper.getLegacyFallbackKey())
                            }
                        domainModel.toEncryptedEntity(deviceKey)
                    }
                // Part II rows are size- and count-checked before any is decoded; what fails is skipped and counted, never fatal.
                val part2 = part2Backup?.restore(backup, source, password, mode)
                val rows =
                    BackupRows(
                        payslips = databasePayslips,
                        pdfs = backup.pdfs.map { it.toEntity() },
                        drafts = backup.drafts.map { it.toEntity() },
                        dismissedDrafts = backup.dismissedDrafts.map { it.toEntity() },
                        corrections =
                            backup.corrections.map { it.toEntity().toCorrectionList(password).toCorrectionEntity(it.dateStr, deviceKey) },
                        guideNotes = restoredNotes(backup, password, deviceKey, mode),
                        part2Entries = part2?.entries.orEmpty(),
                        part2Orders = part2?.orders.orEmpty(),
                        part2Pages = part2?.pages ?: emptySequence(),
                    )

                // REPLACE makes the device an exact copy of the backup (every user and derived table is
                // emptied first); MERGE keeps the device's existing data and its own settings and layers
                // the backup on top, overwriting only same-date payslips. Either way it is one transaction.
                if (mode == RestoreMode.REPLACE) {
                    // The backup's preferences go on top of this device's own settings row, so the lock, PIN hash, telemetry consent
                    // and entitlement stay the device's (D5); a file without settings resets the preferences to their defaults.
                    val device = payslipDao.getSettings() ?: AppSettingsEntity()
                    payslipDao.replaceWithBackup(rows, (backup.settings ?: SettingsBackup()).applyTo(device))
                } else {
                    payslipDao.mergeBackup(rows)
                }

                rebuildDerivedData()
                Result.success(RestoreReport(part2?.skipped ?: 0))
            } catch (e: DecryptionFailedException) {
                // The outer layer opened (a wrong password was returned above), so a row inside that will not open is a damaged file.
                Result.failure(BackupDamagedException())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** A file that is not a page archive is the one encrypted blob (payslips and their PDFs), which is read whole; no real one is this large. */
    private fun wholeBlob(source: BackupSource): ByteArray {
        if (source.size > BLOB_MAX_BYTES) throw BackupDamagedException()
        return source.read(0, source.size.toInt())
    }

    /** An unreadable note is skipped (see [GuideNoteBackup]); only a MERGE needs the notes the device holds now. */
    private suspend fun restoredNotes(
        backup: PortableBackup,
        password: String,
        deviceKey: String,
        mode: RestoreMode,
    ): List<GuideNoteEntity> {
        val existing = if (mode == RestoreMode.MERGE) payslipDao.getAllGuideNotes().first() else emptyList()
        return GuideNoteBackup.forRestore(backup.guideNotes.map { it.toEntity() }, password, deviceKey, existing, mode)
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
            Logger.e("PayslipBackupService", "Rebuilding ledger and insights after restore failed", e)
        }
    }
}

/** The most a backup without scanned pages may be: payslips and their PDFs, a few megabytes in real use. */
private const val BLOB_MAX_BYTES = 128L * 1024 * 1024
