package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.database.PayslipDao
import com.ssbmax.pdfparser.database.toDomain
import com.ssbmax.pdfparser.database.toEntity
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.parser.PdfParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PayslipRepository(
    private val payslipDao: PayslipDao,
    private val pdfParser: PdfParser
) {
    /**
     * Observes all parsed payslips from the local Room database.
     */
    fun getAllPayslips(): Flow<List<ParsedPayslip>> {
        return payslipDao.getAllPayslips().map { entities ->
            entities.map { it.toDomain() }
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
        filename: String
    ): Result<ParsedPayslip> {
        val parseResult = pdfParser.decryptAndParse(pdfBytes, password)
        if (parseResult.isFailure) {
            return parseResult
        }
        val payslip = parseResult.getOrThrow()
        
        // Save to offline Room DB
        payslipDao.insertPayslip(payslip.toEntity())
        return Result.success(payslip)
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
    suspend fun deletePayslip(dateStr: String) {
        payslipDao.deletePayslip(dateStr)
    }

    /**
     * Clears all local records from database.
     */
    suspend fun clearAll() {
        payslipDao.clearAll()
    }

    /**
     * Seeds mock data for historical analytics.
     */
    suspend fun seedMockData() {
        com.ssbmax.pdfparser.database.MockDataSeeder.seedDatabase(payslipDao)
    }
}
