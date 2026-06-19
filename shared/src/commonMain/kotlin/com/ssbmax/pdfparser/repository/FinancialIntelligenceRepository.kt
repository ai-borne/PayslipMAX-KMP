package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.auth.AuthTokenProvider
import com.ssbmax.pdfparser.crypto.CryptoHelper
import com.ssbmax.pdfparser.database.*
import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.insights.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class FinancialIntelligenceRepository(
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
    suspend fun processPayslipAndRunAnalysis(
        payslip: ParsedPayslip,
    ): EngineResult =
        withContext(dispatcher) {
            val dateStr = payslip.dateStr
            val currentRecord = payslip.toLedgerRecordEntity()

            // 1. Save Ledger Record
            payslipDao.insertLedgerRecord(currentRecord)

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

            // 4. Save deterministic insights to Database
            val deterministicInsights =
                engineResult.anomalies.map { anomaly ->
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
                        generateRepresentationDraft(
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
     * Calls Gemini via proxy for narrative insights and saves the response.
     * Auth token is fetched internally from [AuthTokenProvider] — callers
     * do not need to know about Firebase Auth.
     */
    suspend fun generateNarrativeInsights(
        payslip: ParsedPayslip,
        engineResult: EngineResult,
    ): Result<String> =
        withContext(dispatcher) {
            val authToken = authTokenProvider.getIdToken()
            val history = payslipDao.getAllLedgerRecords().firstOrNull() ?: emptyList()
            val sanitizedPayslip = RedactionSanitizer.redact(payslip)

            val result =
                geminiProxyService.getNarrativeInsights(
                    sanitizedPayslip = sanitizedPayslip,
                    engineResult = engineResult,
                    history = history,
                    authToken = authToken,
                )

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
            }

            result
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
            "SALARY_LOSS" -> "SALARY_LOSS"
            "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT" -> "ALLOWANCE"
            "DEDUCTION_SPIKE" -> "TAX"
            "DSOP_COMPLIANCE" -> "RETIREMENT"
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
            else -> "Financial Advisory"
        }
    }

    private fun mapAnomalyTypeToSeverity(type: String): String {
        return when (type) {
            "SALARY_LOSS", "DSOP_COMPLIANCE" -> "CRITICAL"
            "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "DEDUCTION_SPIKE" -> "WARNING"
            else -> "INFO"
        }
    }

    private fun generateRepresentationDraft(
        disputeMonth: String,
        disputeType: String,
        amount: Double,
        officer: Officer,
    ): RepresentationDraftEntity {
        val id = CryptoHelper.sha256("$disputeMonth-$disputeType-${CryptoHelper.getCurrentTimeMillis()}")
        val componentName =
            when (disputeType) {
                "MISSING_ALLOWANCE" -> "HRA / Allowance"
                "TPTA_ENTITLEMENT" -> "Transport Allowance (TPTA)"
                "SALARY_LOSS" -> "Net Pay"
                else -> disputeType
            }
        val subject = "Representation regarding Non-Admissibility of $componentName"
        val body =
            """
            To,
            The Principal Controller of Defence Accounts (Officers)
            Golibar Maidan, Pune - 411001
            
            SUBJECT: REPRESENTATION REGARDING NON-ADMISSIBILITY OF $componentName FOR THE MONTH OF $disputeMonth
            
            Sir/Madam,
            
            1.  I have the honour to submit that my monthly payslip for $disputeMonth indicates that my $componentName has not been correctly credited / has been adjusted.
            
            2.  My service particular details are as follows:
                (a) Personal Number    : [Service Number]
                (b) Rank               : [Rank]
                (c) Name               : [Officer Name]
                (d) CDA Account Number : [CDA Account No]
            
            3.  Discrepancy Details:
                (a) Component name     : $componentName
                (b) Discrepancy month  : $disputeMonth
                (c) Estimated amount   : Rs. ${amount.toInt()}
            
            4.  It is requested that the admissibility of the above component may please be verified and the necessary arrears credited to my account.
            
            5.  Thanking you.
            
            Yours faithfully,
            
            [Officer Name]
            [Rank]
            """.trimIndent()

        return RepresentationDraftEntity(
            id = id,
            disputeMonth = disputeMonth,
            disputeType = disputeType,
            recipient = "PCDA_O_PUNE",
            subject = subject,
            bodyText = body,
            createdAt = CryptoHelper.getCurrentTimeMillis(),
        )
    }
}
