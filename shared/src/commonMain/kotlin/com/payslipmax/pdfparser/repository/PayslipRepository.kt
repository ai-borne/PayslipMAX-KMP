package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.logging.Logger
import com.payslipmax.pdfparser.parser.PdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** Outcome of [PayslipRepository.reparseAllPayslips]: how many of the stored payslips re-parsed cleanly. */
data class ReparseSummary(
    val total: Int,
    val succeeded: Int,
    val failedDates: List<String>,
)

class PayslipRepository(
    private val payslipDao: PayslipDao,
    private val pdfParser: PdfParser,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default,
) {
    /**
     * Observes all parsed payslips from the local Room database.
     */
    fun getAllPayslips(): Flow<List<ParsedPayslip>> =
        combine(
            payslipDao.getAllPayslips(),
            payslipDao.getAllCorrections(),
        ) { entities, corrections ->
            val correctionsByDate = corrections.associateBy { it.dateStr }
            val deviceKey = CryptoHelper.getDatabaseSecretKey()
            entities.mapNotNull { entity ->
                try {
                    val parsed = entity.toDomain(deviceKey)
                    correctionsByDate[entity.dateStr]
                        ?.let { parsed.applyCorrections(it.toCorrectionList(deviceKey)) }
                        ?: parsed
                } catch (e: Exception) {
                    Logger.e("PayslipRepository", "Skipping undecryptable payslip ${entity.dateStr}", e)
                    null
                }
            }
        }.flowOn(dispatcher)

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
     * Re-runs every stored payslip's saved PDF bytes through the current [pdfParser] and overwrites
     * the existing record (REPLACE, keyed by dateStr). [importPayslip] only ever parses a document
     * once, at import time, and [getAllPayslips] just reads back whatever was stored then — so a
     * parser bugfix never reaches an already-imported payslip on its own. This is the maintenance
     * action that closes that gap. A payslip that fails to re-parse (e.g. wrong password) is left
     * untouched rather than deleted, so nothing is ever lost on failure.
     */
    suspend fun reparseAllPayslips(password: String): ReparseSummary =
        withContext(dispatcher) {
            val pdfs = payslipDao.getAllPdfs()
            var succeeded = 0
            val failedDates = mutableListOf<String>()
            for (pdf in pdfs) {
                val payslip = pdfParser.decryptAndParse(pdf.pdfData, password, "${pdf.dateStr}.pdf").getOrNull()
                if (payslip != null) {
                    payslipDao.insertPayslip(payslip.toEncryptedEntity())
                    succeeded++
                } else {
                    failedDates += pdf.dateStr
                }
            }
            ReparseSummary(total = pdfs.size, succeeded = succeeded, failedDates = failedDates)
        }

    /**
     * Retrieves a payslip by its specific date string (e.g. "08/2024").
     */
    suspend fun getPayslipByDate(dateStr: String): ParsedPayslip? {
        val parsed = payslipDao.getPayslipByDate(dateStr)?.toDomain() ?: return null
        val corrections = payslipDao.getCorrectionByDate(dateStr) ?: return parsed
        return parsed.applyCorrections(corrections.toCorrectionList())
    }

    suspend fun getPayslipCorrections(dateStr: String): PayslipCorrectionEntity? =
        withContext(dispatcher) {
            payslipDao.getCorrectionByDate(dateStr)
        }

    suspend fun saveAllCorrections(
        dateStr: String,
        corrections: List<SingleCorrection>,
    ) = withContext(dispatcher) {
        payslipDao.insertCorrection(corrections.toCorrectionEntity(dateStr))
    }

    /**
     * Persists a single user correction for a low-confidence field, encrypted at rest. Corrections are
     * stored separately from the parsed payslip and merged on read, so the original parse is preserved.
     * Re-runs over the existing correction set for the month so multiple fields accumulate.
     */
    suspend fun saveCorrection(
        dateStr: String,
        fieldKey: String,
        newValue: Double,
    ) = withContext(dispatcher) {
        val existing = payslipDao.getCorrectionByDate(dateStr)?.toCorrectionMap() ?: emptyMap()
        val updated = existing + (fieldKey to newValue)
        payslipDao.insertCorrection(updated.toCorrectionEntity(dateStr))
    }

    /**
     * Deletes a payslip from local storage.
     */
    suspend fun deletePayslip(dateStr: String) =
        withContext(dispatcher) {
            payslipDao.deletePayslip(dateStr)
            payslipDao.deleteCorrection(dateStr)
            payslipDao.deletePayslipPdf(dateStr)
            payslipDao.deleteLedgerRecord(dateStr)
            payslipDao.deleteFinancialInsightsByMonth(dateStr)
        }

    /**
     * Clears all local records from database.
     */
    suspend fun clearAll() = withContext(dispatcher) { payslipDao.clearAllUserData() }

    /**
     * Seeds mock data for historical analytics.
     */
    suspend fun seedMockData() = com.payslipmax.pdfparser.database.MockDataSeeder.seedDatabase(payslipDao)

    fun getSettingsFlow(): Flow<AppSettingsEntity?> = payslipDao.getSettingsFlow()

    suspend fun getSettings(): AppSettingsEntity? = withContext(dispatcher) { payslipDao.getSettings() }

    suspend fun saveSettings(settings: AppSettingsEntity) = withContext(dispatcher) { payslipDao.insertSettings(settings) }

    suspend fun clearSettings() = withContext(dispatcher) { payslipDao.clearSettings() }

    /** Number of decryptable payslips currently stored — what a backup will actually contain. */
    suspend fun getStoredPayslipCount(): Int = withContext(dispatcher) { getAllPayslips().first().size }
}
