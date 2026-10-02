package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.logging.Logger
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
) {
    /**
     * Exports all app data (payslips, PDFs, settings) as an encrypted JSON archive.
     */
    suspend fun export(password: String): Result<ByteArray> =
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
                            entity.toDomain(deviceKey).toEncryptedEntity(password)
                        } catch (e: Exception) {
                            Logger.e("PayslipBackupService", "Skipping undecryptable payslip ${entity.dateStr} during backup", e)
                            null
                        }
                    }

                val backup =
                    PortableBackup(
                        version = 2,
                        encryptedPayslips = exportedPayslips,
                        pdfs = pdfs,
                        settings = settings,
                    )

                val jsonStr = Json.encodeToString(PortableBackup.serializer(), backup)
                val jsonBytes = jsonStr.encodeToByteArray()

                CryptoHelper.encrypt(jsonBytes, password)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Decrypts and imports a universal backup archive.
     */
    suspend fun restore(
        backupBytes: ByteArray,
        password: String,
        mode: RestoreMode = RestoreMode.REPLACE,
    ): Result<Unit> =
        withContext(dispatcher) {
            try {
                val decryptResult = CryptoHelper.decrypt(backupBytes, password)
                if (decryptResult.isFailure) {
                    return@withContext Result.failure(
                        decryptResult.exceptionOrNull() ?: Exception("Decryption failed"),
                    )
                }

                val jsonStr = decryptResult.getOrThrow().decodeToString()
                val backup = Json.decodeFromString(PortableBackup.serializer(), jsonStr)

                // Decode and re-encrypt every payslip *before* touching the database: one unreadable row
                // then fails the restore with the device's data untouched, instead of after a wipe.
                val deviceKey = CryptoHelper.getDatabaseSecretKey()
                val databasePayslips =
                    backup.encryptedPayslips.map { entity ->
                        val domainModel =
                            try {
                                entity.toDomain(password)
                            } catch (e: Exception) {
                                // Fallback: Version 1 backups are encrypted with the legacy key
                                entity.toDomain(CryptoHelper.getLegacyFallbackKey())
                            }
                        domainModel.toEncryptedEntity(deviceKey)
                    }

                // REPLACE makes the device an exact copy of the backup (every user and derived table is
                // emptied first); MERGE keeps the device's existing data and its own settings and layers
                // the backup on top, overwriting only same-date payslips. Either way it is one transaction.
                if (mode == RestoreMode.REPLACE) {
                    // Capture this device's own entitlement before the swap so a restored backup can
                    // never grant (or revoke) PRO — entitlement must never travel inside a backup file.
                    val deviceEntitlement = payslipDao.getSettings()?.isPremiumEnabled ?: false
                    val restoredSettings = backup.settings ?: AppSettingsEntity()
                    payslipDao.replaceWithBackup(
                        databasePayslips,
                        backup.pdfs,
                        restoredSettings.copy(isPremiumEnabled = deviceEntitlement),
                    )
                } else {
                    payslipDao.mergeBackup(databasePayslips, backup.pdfs)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
