package com.ssbmax.pdfparser.testing

import com.ssbmax.pdfparser.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePayslipDao : PayslipDao {
    private val database = MutableStateFlow<Map<String, EncryptedPayslipEntity>>(emptyMap())
    private val pdfDatabase = MutableStateFlow<Map<String, PayslipPdfEntity>>(emptyMap())

    override suspend fun insertPayslip(payslip: EncryptedPayslipEntity) {
        database.value = database.value + (payslip.dateStr to payslip)
    }

    override suspend fun insertPayslips(payslips: List<EncryptedPayslipEntity>) {
        database.value = database.value + payslips.associateBy { it.dateStr }
    }

    override fun getAllPayslips(): Flow<List<EncryptedPayslipEntity>> {
        return database.map {
            it.values.toList().sortedWith(
                compareBy<EncryptedPayslipEntity> { it.year }.thenBy { it.monthNum },
            )
        }
    }

    override suspend fun getPayslipByDate(dateStr: String): EncryptedPayslipEntity? {
        return database.value[dateStr]
    }

    override suspend fun deletePayslip(dateStr: String) {
        database.value = database.value - dateStr
    }

    override suspend fun clearAll() {
        database.value = emptyMap()
        pdfDatabase.value = emptyMap()
    }

    override suspend fun insertPayslipPdf(pdf: PayslipPdfEntity) {
        pdfDatabase.value = pdfDatabase.value + (pdf.dateStr to pdf)
    }

    override suspend fun getPayslipPdfByDate(dateStr: String): PayslipPdfEntity? {
        return pdfDatabase.value[dateStr]
    }

    override suspend fun getAllPdfs(): List<PayslipPdfEntity> {
        return pdfDatabase.value.values.toList()
    }

    override suspend fun deletePayslipPdf(dateStr: String) {
        pdfDatabase.value = pdfDatabase.value - dateStr
    }

    private val settingsDatabase = MutableStateFlow<com.ssbmax.pdfparser.database.AppSettingsEntity?>(null)

    override fun getSettingsFlow(): Flow<com.ssbmax.pdfparser.database.AppSettingsEntity?> {
        return settingsDatabase
    }

    override suspend fun getSettings(): com.ssbmax.pdfparser.database.AppSettingsEntity? {
        return settingsDatabase.value
    }

    override suspend fun insertSettings(settings: com.ssbmax.pdfparser.database.AppSettingsEntity) {
        settingsDatabase.value = settings
    }

    override suspend fun clearSettings() {
        settingsDatabase.value = null
    }

    private val ledgerDatabase = MutableStateFlow<Map<String, LedgerRecordEntity>>(emptyMap())
    private val insightsDatabase = MutableStateFlow<Map<String, FinancialInsightEntity>>(emptyMap())
    private val draftsDatabase = MutableStateFlow<Map<String, RepresentationDraftEntity>>(emptyMap())

    override suspend fun insertLedgerRecord(record: LedgerRecordEntity) {
        ledgerDatabase.value = ledgerDatabase.value + (record.dateStr to record)
    }

    override suspend fun insertLedgerRecords(records: List<LedgerRecordEntity>) {
        ledgerDatabase.value = ledgerDatabase.value + records.associateBy { it.dateStr }
    }

    override fun getAllLedgerRecords(): Flow<List<LedgerRecordEntity>> {
        return ledgerDatabase.map {
            it.values.toList().sortedWith(
                compareBy<LedgerRecordEntity> { it.year }.thenBy { it.monthNum },
            )
        }
    }

    override suspend fun getLedgerRecordByDate(dateStr: String): LedgerRecordEntity? {
        return ledgerDatabase.value[dateStr]
    }

    override suspend fun deleteLedgerRecord(dateStr: String) {
        ledgerDatabase.value = ledgerDatabase.value - dateStr
    }

    override suspend fun clearAllLedgerRecords() {
        ledgerDatabase.value = emptyMap()
    }

    override suspend fun insertFinancialInsight(insight: FinancialInsightEntity) {
        insightsDatabase.value = insightsDatabase.value + (insight.id to insight)
    }

    override suspend fun insertFinancialInsights(insights: List<FinancialInsightEntity>) {
        insightsDatabase.value = insightsDatabase.value + insights.associateBy { it.id }
    }

    override fun getAllFinancialInsights(): Flow<List<FinancialInsightEntity>> {
        return insightsDatabase.map {
            it.values.toList().sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getFinancialInsightsByMonth(monthStr: String): List<FinancialInsightEntity> {
        return insightsDatabase.value.values.filter { it.monthStr == monthStr }
    }

    override suspend fun deleteFinancialInsight(id: String) {
        insightsDatabase.value = insightsDatabase.value - id
    }

    override suspend fun clearAllFinancialInsights() {
        insightsDatabase.value = emptyMap()
    }

    override suspend fun insertRepresentationDraft(draft: RepresentationDraftEntity) {
        draftsDatabase.value = draftsDatabase.value + (draft.id to draft)
    }

    override fun getAllRepresentationDrafts(): Flow<List<RepresentationDraftEntity>> {
        return draftsDatabase.map {
            it.values.toList().sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getRepresentationDraftById(id: String): RepresentationDraftEntity? {
        return draftsDatabase.value[id]
    }

    override suspend fun deleteRepresentationDraft(id: String) {
        draftsDatabase.value = draftsDatabase.value - id
    }

    override suspend fun clearAllRepresentationDrafts() {
        draftsDatabase.value = emptyMap()
    }
}
