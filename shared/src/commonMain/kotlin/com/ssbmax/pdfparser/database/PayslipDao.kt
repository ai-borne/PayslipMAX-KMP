package com.ssbmax.pdfparser.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PayslipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslip(payslip: EncryptedPayslipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslips(payslips: List<EncryptedPayslipEntity>)

    @Query("SELECT * FROM encrypted_payslips ORDER BY year ASC, monthNum ASC")
    fun getAllPayslips(): Flow<List<EncryptedPayslipEntity>>

    @Query("SELECT * FROM encrypted_payslips WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getPayslipByDate(dateStr: String): EncryptedPayslipEntity?

    @Query("DELETE FROM encrypted_payslips WHERE dateStr = :dateStr")
    suspend fun deletePayslip(dateStr: String)

    @Query("DELETE FROM encrypted_payslips")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslipPdf(pdf: PayslipPdfEntity)

    @Query("SELECT * FROM payslip_pdfs WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getPayslipPdfByDate(dateStr: String): PayslipPdfEntity?

    @Query("SELECT * FROM payslip_pdfs")
    suspend fun getAllPdfs(): List<PayslipPdfEntity>

    @Query("DELETE FROM payslip_pdfs WHERE dateStr = :dateStr")
    suspend fun deletePayslipPdf(dateStr: String)

    @Query("DELETE FROM payslip_pdfs")
    suspend fun clearAllPdfs()

    @Query("SELECT * FROM app_settings WHERE id = 0 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 0 LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearSettings()

    // LedgerRecord queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerRecord(record: LedgerRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerRecords(records: List<LedgerRecordEntity>)

    @Query("SELECT * FROM ledger_records ORDER BY year ASC, monthNum ASC")
    fun getAllLedgerRecords(): Flow<List<LedgerRecordEntity>>

    @Query("SELECT * FROM ledger_records WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getLedgerRecordByDate(dateStr: String): LedgerRecordEntity?

    @Query("DELETE FROM ledger_records WHERE dateStr = :dateStr")
    suspend fun deleteLedgerRecord(dateStr: String)

    @Query("DELETE FROM ledger_records")
    suspend fun clearAllLedgerRecords()

    // FinancialInsight queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialInsight(insight: FinancialInsightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialInsights(insights: List<FinancialInsightEntity>)

    @Query("SELECT * FROM financial_insights ORDER BY createdAt DESC")
    fun getAllFinancialInsights(): Flow<List<FinancialInsightEntity>>

    @Query("SELECT * FROM financial_insights WHERE monthStr = :monthStr")
    suspend fun getFinancialInsightsByMonth(monthStr: String): List<FinancialInsightEntity>

    @Query("DELETE FROM financial_insights WHERE id = :id")
    suspend fun deleteFinancialInsight(id: String)

    @Query("DELETE FROM financial_insights WHERE monthStr = :monthStr")
    suspend fun deleteFinancialInsightsByMonth(monthStr: String)

    @Query("DELETE FROM financial_insights")
    suspend fun clearAllFinancialInsights()

    // RepresentationDraft queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepresentationDraft(draft: RepresentationDraftEntity)

    @Query("SELECT * FROM representation_drafts ORDER BY createdAt DESC")
    fun getAllRepresentationDrafts(): Flow<List<RepresentationDraftEntity>>

    @Query("SELECT * FROM representation_drafts WHERE id = :id LIMIT 1")
    suspend fun getRepresentationDraftById(id: String): RepresentationDraftEntity?

    @Query("DELETE FROM representation_drafts WHERE id = :id")
    suspend fun deleteRepresentationDraft(id: String)

    @Query("DELETE FROM representation_drafts")
    suspend fun clearAllRepresentationDrafts()

    // AiInsightReport queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiInsightReport(report: AiInsightReportEntity)

    @Query("SELECT * FROM ai_insight_reports WHERE payslipMonth = :monthStr LIMIT 1")
    suspend fun getAiInsightReportByMonth(monthStr: String): AiInsightReportEntity?

    @Query("SELECT * FROM ai_insight_reports ORDER BY generatedDate DESC")
    fun getAllAiInsightReports(): Flow<List<AiInsightReportEntity>>

    @Query("DELETE FROM ai_insight_reports WHERE id = :id")
    suspend fun deleteAiInsightReport(id: String)

    @Query("DELETE FROM ai_insight_reports WHERE payslipMonth = :monthStr")
    suspend fun deleteAiInsightReportByMonth(monthStr: String)

    @Query("DELETE FROM ai_insight_reports")
    suspend fun clearAllAiInsightReports()
}
