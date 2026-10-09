package com.payslipmax.pdfparser.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    // PayslipCorrection queries (Phase 5 — per-field user corrections, merged on read)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: PayslipCorrectionEntity)

    @Query("SELECT * FROM payslip_corrections")
    fun getAllCorrections(): Flow<List<PayslipCorrectionEntity>>

    @Query("SELECT * FROM payslip_corrections WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getCorrectionByDate(dateStr: String): PayslipCorrectionEntity?

    @Query("DELETE FROM payslip_corrections WHERE dateStr = :dateStr")
    suspend fun deleteCorrection(dateStr: String)

    @Query("DELETE FROM payslip_corrections")
    suspend fun clearAllCorrections()

    // Personal Claim Guide notes (encrypted rows; part of the same restore transaction as everything else the user created)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuideNote(note: GuideNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuideNotes(notes: List<GuideNoteEntity>)

    @Query("SELECT * FROM guide_notes")
    fun getAllGuideNotes(): Flow<List<GuideNoteEntity>>

    @Query("DELETE FROM guide_notes WHERE cardId = :cardId")
    suspend fun deleteGuideNote(cardId: String)

    @Query("DELETE FROM guide_notes")
    suspend fun clearAllGuideNotes()

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

    // Dismissed (deleted-by-the-officer) letters
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDismissedDraft(dismissed: DismissedDraftEntity)

    @Query("SELECT * FROM dismissed_drafts")
    suspend fun getAllDismissedDrafts(): List<DismissedDraftEntity>

    @Query("DELETE FROM dismissed_drafts WHERE disputeMonth = :disputeMonth AND disputeType = :disputeType")
    suspend fun deleteDismissedDraft(
        disputeMonth: String,
        disputeType: String,
    )

    @Query("DELETE FROM dismissed_drafts")
    suspend fun clearAllDismissedDrafts()

    /**
     * Empties every table that holds user or derived data. The single list of those tables, shared by
     * "clear all data" and the REPLACE restore; app settings are deliberately excluded because they carry
     * the device's own entitlement.
     */
    @Transaction
    suspend fun clearAllUserData() {
        clearAll()
        clearAllCorrections()
        clearAllGuideNotes()
        clearAllLedgerRecords()
        clearAllFinancialInsights()
        clearAllRepresentationDrafts()
        clearAllDismissedDrafts()
        clearAllPdfs()
    }

    /**
     * REPLACE restore as one transaction: either the device ends up an exact copy of the backup or, if
     * any write fails, it is left exactly as it was.
     */
    @Transaction
    suspend fun replaceWithBackup(
        rows: BackupRows,
        settings: AppSettingsEntity,
    ) {
        clearAllUserData()
        clearSettings()
        insertSettings(settings)
        mergeBackup(rows)
    }

    /** MERGE restore as one transaction: the backup's rows are layered on top of what the device holds. */
    @Transaction
    suspend fun mergeBackup(rows: BackupRows) {
        insertPayslips(rows.payslips)
        rows.pdfs.forEach { insertPayslipPdf(it) }
        rows.drafts.forEach { insertRepresentationDraft(it) }
        rows.dismissedDrafts.forEach { insertDismissedDraft(it) }
        rows.corrections.forEach { insertCorrection(it) }
        insertGuideNotes(rows.guideNotes)
    }
}
