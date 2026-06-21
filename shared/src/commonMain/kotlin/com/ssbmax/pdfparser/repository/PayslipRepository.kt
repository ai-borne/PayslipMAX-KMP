package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.crypto.CryptoHelper
import com.ssbmax.pdfparser.crypto.getLegacyFallbackKey
import com.ssbmax.pdfparser.database.*
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.parser.PdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PayslipRepository(
    private val payslipDao: PayslipDao,
    private val pdfParser: PdfParser,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default,
) {
    /**
     * Observes all parsed payslips from the local Room database.
     */
    fun getAllPayslips(): Flow<List<ParsedPayslip>> {
        return payslipDao.getAllPayslips().map { entities ->
            try {
                entities.map { it.toDomain() }
            } catch (e: Exception) {
                // If decryption fails completely, clear database to recover self-healing style
                try {
                    clearAll()
                } catch (dbEx: Exception) {
                    // Ignore database delete errors
                }
                emptyList()
            }
        }
    }

    /**
     * Decrypts a PDF payslip, parses it, and inserts it into the database.
     * @param pdfBytes Raw encrypted PDF document.
     * @param password Password for decryption.
     * @param filename Backup source filename for fallback parsing.
     */
    suspend fun importPayslip(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip> =
        withContext(dispatcher) {
            val parseResult = pdfParser.decryptAndParse(pdfBytes, password)
            if (parseResult.isFailure) {
                return@withContext parseResult
            }
            val payslip = parseResult.getOrThrow()

            // Save to offline Room DB
            payslipDao.insertPayslip(payslip.toEncryptedEntity())
            payslipDao.insertPayslipPdf(PayslipPdfEntity(payslip.dateStr, pdfBytes))
            Result.success(payslip)
        }

    /**
     * Retrieves the raw PDF bytes for a specific payslip.
     */
    suspend fun getPayslipPdf(dateStr: String): ByteArray? =
        withContext(dispatcher) {
            payslipDao.getPayslipPdfByDate(dateStr)?.pdfData
        }

    /**
     * Retrieves a payslip by its specific date string (e.g. "08/2024").
     */
    suspend fun getPayslipByDate(dateStr: String): ParsedPayslip? {
        return payslipDao.getPayslipByDate(dateStr)?.toDomain()
    }

    /**
     * Deletes a payslip from local storage.
     */
    suspend fun deletePayslip(dateStr: String) =
        withContext(dispatcher) {
            payslipDao.deletePayslip(dateStr)
            payslipDao.deletePayslipPdf(dateStr)
            payslipDao.deleteLedgerRecord(dateStr)
            payslipDao.deleteAiInsightReportByMonth(dateStr)
            payslipDao.deleteFinancialInsightsByMonth(dateStr)
        }

    /**
     * Clears all local records from database.
     */
    suspend fun clearAll() =
        withContext(dispatcher) {
            payslipDao.clearAll()
            payslipDao.clearAllLedgerRecords()
            payslipDao.clearAllFinancialInsights()
            payslipDao.clearAllRepresentationDrafts()
            payslipDao.clearAllAiInsightReports()
            payslipDao.clearAllPdfs()
        }

    /**
     * Seeds mock data for historical analytics.
     */
    suspend fun seedMockData() {
        com.ssbmax.pdfparser.database.MockDataSeeder.seedDatabase(payslipDao)
    }

    /**
     * Observes the app settings from the database.
     */
    fun getSettingsFlow(): Flow<AppSettingsEntity?> {
        return payslipDao.getSettingsFlow()
    }

    /**
     * Retrieves the current app settings.
     */
    suspend fun getSettings(): AppSettingsEntity? =
        withContext(dispatcher) {
            payslipDao.getSettings()
        }

    /**
     * Saves the app settings.
     */
    suspend fun saveSettings(settings: AppSettingsEntity) =
        withContext(dispatcher) {
            payslipDao.insertSettings(settings)
        }

    /**
     * Clears all settings data.
     */
    suspend fun clearSettings() =
        withContext(dispatcher) {
            payslipDao.clearSettings()
        }

    /**
     * Exports all app data (payslips, PDFs, settings) as an encrypted JSON archive.
     */
    suspend fun exportUniversalBackup(password: String): Result<ByteArray> =
        withContext(dispatcher) {
            try {
                val payslips = payslipDao.getAllPayslips().first()
                val pdfs = payslipDao.getAllPdfs()
                val settings = payslipDao.getSettings()

                val deviceKey = CryptoHelper.getDatabaseSecretKey()
                val exportedPayslips =
                    payslips.map { entity ->
                        entity.toDomain(deviceKey).toEncryptedEntity(password)
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
    suspend fun importUniversalBackup(
        backupBytes: ByteArray,
        password: String,
    ): Result<Unit> =
        withContext(dispatcher) {
            try {
                val decryptResult = CryptoHelper.decrypt(backupBytes, password)
                if (decryptResult.isFailure) {
                    return@withContext Result.failure(
                        decryptResult.exceptionOrNull() ?: Exception("Decryption failed"),
                    )
                }

                val jsonBytes = decryptResult.getOrThrow()
                val jsonStr = jsonBytes.decodeToString()

                val backup = Json.decodeFromString(PortableBackup.serializer(), jsonStr)

                // Restore to Room DB
                payslipDao.clearAll()
                payslipDao.clearSettings()

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

                payslipDao.insertPayslips(databasePayslips)
                backup.pdfs.forEach { pdf ->
                    payslipDao.insertPayslipPdf(pdf)
                }
                backup.settings?.let { settings ->
                    payslipDao.insertSettings(settings)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
