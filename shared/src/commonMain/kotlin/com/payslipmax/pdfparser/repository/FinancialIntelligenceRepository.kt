package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.auth.AuthTokenProvider
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

open class FinancialIntelligenceRepository(
    private val payslipDao: PayslipDao,
    private val geminiProxyService: GeminiProxyService,
    private val authTokenProvider: AuthTokenProvider = AuthTokenProvider(),
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default,
) {
    /**
     * Observes all ledger records.
     */
    fun getAllLedgerRecords(): Flow<List<LedgerRecordEntity>> {
        return payslipDao.getAllLedgerRecords()
    }

    /**
     * Observes all financial insights.
     */
    fun getAllFinancialInsights(): Flow<List<FinancialInsightEntity>> {
        return payslipDao.getAllFinancialInsights()
    }

    /**
     * Observes all representation drafts.
     */
    fun getAllRepresentationDrafts(): Flow<List<RepresentationDraftEntity>> {
        return payslipDao.getAllRepresentationDrafts()
    }

    /**
     * Retrieves a representation draft by its ID.
     */
    suspend fun getRepresentationDraftById(id: String): RepresentationDraftEntity? =
        withContext(dispatcher) {
            payslipDao.getRepresentationDraftById(id)
        }

    /**
     * Deletes a financial insight.
     */
    suspend fun deleteFinancialInsight(id: String) =
        withContext(dispatcher) {
            payslipDao.deleteFinancialInsight(id)
        }

    /**
     * Deletes a representation draft.
     */
    suspend fun deleteRepresentationDraft(id: String) =
        withContext(dispatcher) {
            payslipDao.deleteRepresentationDraft(id)
        }

    /**
     * Inserts or updates a representation draft.
     */
    suspend fun insertRepresentationDraft(draft: RepresentationDraftEntity) =
        withContext(dispatcher) {
            payslipDao.insertRepresentationDraft(draft)
        }

    /**
     * Saves a parsed payslip into the ledger, executes the local deterministic checks,
     * updates database records, and automatically triggers representation drafts.
     */
    open suspend fun processPayslipAndRunAnalysis(
        payslip: ParsedPayslip,
    ): EngineResult =
        withContext(dispatcher) {
            val dateStr = payslip.dateStr
            val currentRecord = payslip.toLedgerRecordEntity()

            // 1. Save Ledger Record
            payslipDao.insertLedgerRecord(currentRecord)

            // Invalidate cached AI insights for this month if any exist to ensure prompt updates
            payslipDao.getFinancialInsightsByMonth(dateStr)
                .filter { it.category == "NARRATIVE" }
                .forEach { payslipDao.deleteFinancialInsight(it.id) }

            // 2. Fetch history for analysis
            val history = payslipDao.getAllLedgerRecords().firstOrNull() ?: emptyList()
            val previousRecord =
                history.firstOrNull {
                    val isPrevYear = it.year == currentRecord.year && it.monthNum == currentRecord.monthNum - 1
                    val isDecToJan = it.year == currentRecord.year - 1 && it.monthNum == 12 && currentRecord.monthNum == 1
                    isPrevYear || isDecToJan
                }

            // 3. Run Deterministic Intelligence Engine
            val engineResult =
                DeterministicIntelligenceEngine.analyze(
                    current = currentRecord,
                    previous = previousRecord,
                    history = history,
                )

            // Prioritize anomalies to prevent dashboard clutter
            val prioritizedAnomalies = InsightPrioritizationEngine.prioritize(engineResult.anomalies)

            // 4. Save prioritized deterministic insights to Database
            val deterministicInsights =
                prioritizedAnomalies.map { anomaly ->
                    FinancialInsightEntity(
                        id = CryptoHelper.sha256("${anomaly.month}-${anomaly.type}-${anomaly.field}"),
                        monthStr = dateStr,
                        category = mapAnomalyTypeToCategory(anomaly.type),
                        title = mapAnomalyTypeToTitle(anomaly.type),
                        contentMarkdown = anomaly.description,
                        severity = mapAnomalyTypeToSeverity(anomaly.type),
                        createdAt = CryptoHelper.getCurrentTimeMillis(),
                    )
                }
            payslipDao.insertFinancialInsights(deterministicInsights)

            // 5. Generate Representation Drafts locally for claims discrepancies
            engineResult.anomalies.forEach { anomaly ->
                if (anomaly.type == "MISSING_ALLOWANCE" || anomaly.type == "TPTA_ENTITLEMENT" || anomaly.type == "SALARY_LOSS") {
                    val draft =
                        RepresentationDraftGenerator.generateRepresentationDraft(
                            disputeMonth = dateStr,
                            disputeType = anomaly.type,
                            amount = anomaly.amount,
                            officer = payslip.officer,
                        )
                    payslipDao.insertRepresentationDraft(draft)
                }
            }

            engineResult
        }

    /**
     * Calls Gemini via proxy or local Gemma provider based on settings, and saves the response.
     * Auth token is fetched internally from [AuthTokenProvider] — callers
     * do not need to know about Firebase Auth.
     */
    open suspend fun generateNarrativeInsights(
        payslip: ParsedPayslip,
        engineResult: EngineResult,
    ): Result<String> =
        withContext(dispatcher) {
            val authToken = authTokenProvider.getIdToken()
            val history = payslipDao.getAllLedgerRecords().firstOrNull() ?: emptyList()
            val sanitizedPayslip = RedactionSanitizer.redact(payslip)

            val settings = payslipDao.getSettings() ?: AppSettingsEntity()
            val useLocalAi = settings.useLocalAi

            val cloudProvider = GeminiCloudProvider(geminiProxyService)
            val manager =
                AIProviderManager(
                    cloudProvider = cloudProvider,
                    localProvider = LocalGemmaProvider(),
                    useLocalAi = useLocalAi,
                )

            val payload =
                PromptPayload(
                    currentMonthRawText = "",
                    sanitizedJsonData = "",
                    historicalSummaryText = "",
                    anomaliesCount = engineResult.anomalies.size,
                    sanitizedPayslip = sanitizedPayslip,
                    engineResult = engineResult,
                    history = history,
                    authToken = authToken,
                )

            val result = manager.generateInsights(payload)

            if (result.isSuccess) {
                val narrative = result.getOrThrow()
                val insight =
                    FinancialInsightEntity(
                        id = CryptoHelper.sha256("${payslip.dateStr}-NARRATIVE"),
                        monthStr = payslip.dateStr,
                        category = "NARRATIVE",
                        title = "Monthly Financial Audit & Advice",
                        contentMarkdown = narrative,
                        severity = "INFO",
                        createdAt = CryptoHelper.getCurrentTimeMillis(),
                    )
                payslipDao.insertFinancialInsight(insight)

                val report =
                    AiInsightReportEntity(
                        id = CryptoHelper.sha256("${payslip.dateStr}-AI_REPORT"),
                        payslipMonth = payslip.dateStr,
                        generatedDate = CryptoHelper.getCurrentTimeMillis(),
                        reportJSON = narrative,
                        reportVersion = "1.0.0",
                    )
                payslipDao.insertAiInsightReport(report)
            }

            result
        }

    /**
     * Retrieves the cached AI narrative report from local database if it exists.
     */
    open suspend fun getCachedAiInsights(monthStr: String): String? =
        withContext(dispatcher) {
            payslipDao.getAiInsightReportByMonth(monthStr)?.reportJSON
                ?: payslipDao.getFinancialInsightsByMonth(monthStr)
                    .firstOrNull { it.category == "NARRATIVE" }
                    ?.contentMarkdown
        }

    /**
     * Retrieves all cached AI narrative reports.
     */
    fun getAllAiInsightReports(): Flow<List<AiInsightReportEntity>> {
        return payslipDao.getAllAiInsightReports()
    }

    private fun ParsedPayslip.toLedgerRecordEntity(): LedgerRecordEntity {
        return LedgerRecordEntity(
            dateStr = dateStr,
            year = year,
            monthNum = monthNum,
            basicPay = earnings.basicPay,
            dearnessAllowance = earnings.dearnessAllowance,
            militaryServicePay = earnings.militaryServicePay,
            transportAllowance = earnings.transportAllowance,
            transportAllowanceDa = earnings.transportAllowanceDa,
            houseRentAllowance = earnings.houseRentAllowance,
            grossPay = summary.grossPay,
            dsopSubscription = deductions.dsopSubscription,
            incomeTax = deductions.incomeTax,
            netPay = summary.netRemittance,
        )
    }

    private fun mapAnomalyTypeToCategory(type: String): String {
        return when (type) {
            "SALARY_LOSS", "DEBIT_RECOVERY" -> "SALARY_LOSS"
            "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "ARREARS_AUDIT" -> "ALLOWANCE"
            "DEDUCTION_SPIKE", "RENT_RECOVERY_RISK", "TAX_PROJECTION" -> "TAX"
            "DSOP_COMPLIANCE", "DSOP_MILESTONE" -> "RETIREMENT"
            else -> "INFO"
        }
    }

    private fun mapAnomalyTypeToTitle(type: String): String {
        return when (type) {
            "SALARY_LOSS" -> "Salary Reduction Detected"
            "MISSING_ALLOWANCE" -> "Missing Pay Allowance"
            "TPTA_ENTITLEMENT" -> "TPTA Entitlement Advisory"
            "DEDUCTION_SPIKE" -> "Deduction Spike Alert"
            "DSOP_COMPLIANCE" -> "DSOP Subscription Advisory"
            "RENT_RECOVERY_RISK" -> "Quarters Rent Recovery Risk"
            "DEBIT_RECOVERY" -> "Unexpected Debit Recovery"
            "DSOP_MILESTONE" -> "DSOP Milestone Credited"
            "TAX_PROJECTION" -> "Income Tax Cycle Projection"
            "ARREARS_AUDIT" -> "Dearness Allowance Arrears Verified"
            else -> "Financial Advisory"
        }
    }

    private fun mapAnomalyTypeToSeverity(type: String): String {
        return when (type) {
            "SALARY_LOSS", "DSOP_COMPLIANCE", "RENT_RECOVERY_RISK", "DEBIT_RECOVERY" -> "CRITICAL"
            "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "DEDUCTION_SPIKE", "TAX_PROJECTION" -> "WARNING"
            else -> "INFO"
        }
    }
}
