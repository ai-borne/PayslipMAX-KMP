package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.*
import com.payslipmax.pdfparser.insights.timeline.TptaAbsenceExplainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

open class FinancialIntelligenceRepository(
    private val payslipDao: PayslipDao,
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
     * updates database records, and automatically triggers representation drafts. It then re-audits the
     * earlier months this payslip can change (see [reauditEarlierMonths]).
     */
    open suspend fun processPayslipAndRunAnalysis(
        payslip: ParsedPayslip,
    ): EngineResult =
        withContext(dispatcher) {
            val currentRecord = payslip.toLedgerRecordEntity()

            // 1. Save Ledger Record
            payslipDao.insertLedgerRecord(currentRecord)

            // 2. Fetch history for analysis, 3-5. audit the month and store its insights and drafts
            val history = payslipDao.getAllLedgerRecords().firstOrNull() ?: emptyList()
            val engineResult = auditMonth(currentRecord, history, payslip.officer)

            reauditEarlierMonths(currentRecord, history, payslip.officer)

            engineResult
        }

    /**
     * Re-runs the audit for every stored month in the [TptaAbsenceExplainer.RELOCATION_WINDOW_MONTHS]
     * before [imported] — the only months whose verdict a newly imported payslip can change: a TPTA gap
     * held as pending until a later payslip arrived either resolves (its pending row is removed) or
     * surfaces as a proven finding under the same row id. Driven by the stored ledger records, so a month
     * whose payslip was deleted is never brought back. The officer on the imported payslip signs any
     * letter a newly proven finding drafts (the history is one officer's).
     */
    private suspend fun reauditEarlierMonths(
        imported: LedgerRecordEntity,
        history: List<LedgerRecordEntity>,
        officer: Officer,
    ) {
        val importedIndex = imported.year * 12 + imported.monthNum - 1
        history
            .filter { importedIndex - (it.year * 12 + it.monthNum - 1) in 1..TptaAbsenceExplainer.RELOCATION_WINDOW_MONTHS }
            .forEach { auditMonth(it, history, officer) }
    }

    /** Audits [record] against [history], reconciles its stored insights and drafts any newly proven letter. */
    private suspend fun auditMonth(
        record: LedgerRecordEntity,
        history: List<LedgerRecordEntity>,
        officer: Officer,
    ): EngineResult {
        val dateStr = record.dateStr
        val previousRecord =
            history.firstOrNull {
                val isPrevYear = it.year == record.year && it.monthNum == record.monthNum - 1
                val isDecToJan = it.year == record.year - 1 && it.monthNum == 12 && record.monthNum == 1
                isPrevYear || isDecToJan
            }

        // 3. Run Deterministic Intelligence Engine
        val engineResult =
            DeterministicIntelligenceEngine.analyze(
                current = record,
                previous = previousRecord,
                history = history,
            )

        // Prioritize anomalies to prevent dashboard clutter
        val prioritizedAnomalies = InsightPrioritizationEngine.prioritize(engineResult.anomalies)

        // 4. Reconcile this month's stored insights with the fresh run: rows that no longer apply (a held
        // finding that resolved) are removed, the rest are rewritten in the current wording and keep their
        // age and the officer's archive choice.
        val stored = payslipDao.getFinancialInsightsByMonth(dateStr).associateBy { it.id }
        val deterministicInsights =
            prioritizedAnomalies.map { anomaly ->
                val id = CryptoHelper.sha256("${anomaly.month}-${anomaly.type}-${anomaly.field}")
                FinancialInsightEntity(
                    id = id,
                    monthStr = dateStr,
                    category = mapAnomalyTypeToCategory(anomaly.type),
                    title = mapAnomalyTypeToTitle(anomaly.type),
                    contentMarkdown = anomaly.description,
                    severity = mapAnomalyTypeToSeverity(anomaly.type),
                    createdAt = stored[id]?.createdAt ?: CryptoHelper.getCurrentTimeMillis(),
                    isArchived = stored[id]?.isArchived ?: false,
                )
            }
        (stored.keys - deterministicInsights.map { it.id }.toSet()).forEach { payslipDao.deleteFinancialInsight(it) }
        payslipDao.insertFinancialInsights(deterministicInsights)

        // 5. Generate Representation Drafts locally for claims discrepancies that are actually proven
        // (payslips supply both expected and actual amounts, and a verified authority is cited) —
        // an unproven heuristic (e.g. a bare SALARY_LOSS or an uncited MISSING_ALLOWANCE) never drafts
        // a formal complaint letter, and neither does a held (pending) finding, which is never proven.
        // One letter per month and dispute type: a re-audit never stacks a duplicate, and never deletes a
        // letter the officer may already have edited or sent.
        val existingDrafts = payslipDao.getAllRepresentationDrafts().firstOrNull() ?: emptyList()
        engineResult.anomalies.forEach { anomaly ->
            val alreadyDrafted = existingDrafts.any { it.disputeMonth == dateStr && it.disputeType == anomaly.type }
            if (anomaly.type in REPRESENTATION_DRAFT_TYPES && anomaly.isProven() && !alreadyDrafted) {
                val draft =
                    RepresentationDraftGenerator.generateRepresentationDraft(
                        disputeMonth = dateStr,
                        disputeType = anomaly.type,
                        amount = anomaly.amount,
                        officer = officer,
                        expected = anomaly.expected!!,
                        actual = anomaly.actual!!,
                        authority = anomaly.authority!!,
                    )
                payslipDao.insertRepresentationDraft(draft)
            }
        }

        return engineResult
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
            riskHardshipAllowance = earnings.riskHardshipAllowance,
            fieldAllowance = earnings.fieldAllowance,
            licenseFee = deductions.licenseFee,
            furnitureRent = deductions.furnitureRent,
            arrearsDa = earnings.arrearsDa,
            arrearsTpta = earnings.arrearsTpta,
            arrearsTptaDa = earnings.arrearsTptaDa,
            adjTpta = earnings.adjTpta,
            adjMsp = earnings.adjMsp,
            needsReview = needsReview,
        )
    }

    private fun mapAnomalyTypeToCategory(type: String): String {
        return when (type) {
            "SALARY_LOSS", "DEBIT_RECOVERY", "INCREMENT_MISSED", "MSP_SHORTFALL" -> "SALARY_LOSS"
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
            "INCREMENT_MISSED" -> "Annual Increment Not Applied"
            "MSP_SHORTFALL" -> "Military Service Pay Shortfall"
            else -> "Financial Advisory"
        }
    }

    private fun mapAnomalyTypeToSeverity(type: String): String = AnomalySeverityMapper.severityOf(type).toPersistedString()
}
