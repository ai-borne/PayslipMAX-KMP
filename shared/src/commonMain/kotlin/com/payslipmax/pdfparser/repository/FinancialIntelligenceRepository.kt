package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.*
import com.payslipmax.pdfparser.insights.timeline.TptaAbsenceExplainer
import com.payslipmax.pdfparser.logging.Logger
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
     * Deletes a representation draft. If the audit drafts that type of letter, the deletion is remembered
     * so a re-audit does not draft it again while the same finding is still proven.
     */
    suspend fun deleteRepresentationDraft(id: String) =
        withContext(dispatcher) {
            payslipDao.getRepresentationDraftById(id)
                ?.takeIf { it.disputeType in REPRESENTATION_DRAFT_TYPES }
                ?.let { payslipDao.insertDismissedDraft(DismissedDraftEntity(it.disputeMonth, it.disputeType)) }
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
     * neighbouring months this payslip can change (see [reauditNeighbourMonths]).
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

            reauditNeighbourMonths(currentRecord, history, payslip.officer)

            engineResult
        }

    /**
     * Rebuilds the derived data from [payslips] (the single source, corrections already applied): upserts
     * every ledger row first, so each month is audited against the full history, then audits each month once
     * in date order. Letters for still-proven findings are kept with their edits, stale ones are removed and
     * a deleted-letter record still stops re-drafting. Each month's own officer signs its letter.
     */
    open suspend fun rebuildAuditHistory(payslips: List<ParsedPayslip>) =
        withContext(dispatcher) {
            val ordered = payslips.sortedWith(compareBy({ it.year }, { it.monthNum }))
            payslipDao.insertLedgerRecords(ordered.map { it.toLedgerRecordEntity() })
            val history = payslipDao.getAllLedgerRecords().firstOrNull() ?: emptyList()
            val officers = ordered.associate { it.dateStr to it.officer }
            history.forEach { record -> officers[record.dateStr]?.let { auditMonth(record, history, it) } }
        }

    /**
     * One-time repair: rebuilds only when a payslip has no ledger row (a restore from before backups rebuilt
     * derived data), so a complete ledger costs a single read. Never throws and never touches payslips,
     * PDFs, settings or corrections; returns whether a rebuild ran to completion.
     */
    suspend fun repairAuditHistoryIfIncomplete(payslips: List<ParsedPayslip>): Boolean =
        try {
            val stored = withContext(dispatcher) { payslipDao.getAllLedgerRecords().firstOrNull() ?: emptyList() }.map { it.dateStr }.toSet()
            if (payslips.all { it.dateStr in stored }) {
                false
            } else {
                rebuildAuditHistory(payslips)
                true
            }
        } catch (e: Exception) {
            Logger.e("FinancialIntelligenceRepository", "Audit history repair failed", e)
            false
        }

    /**
     * Re-runs the audit for every stored month within [TptaAbsenceExplainer.RELOCATION_WINDOW_MONTHS] of
     * [imported], before or after it — the only months whose verdict a newly imported payslip can change.
     * Earlier months: a TPTA gap held as pending until a later payslip arrived either resolves (its pending
     * row is removed) or surfaces as a proven finding under the same row id. Later months (a backfill of an
     * older payslip): a city sample now exists, so a later month's finding can appear or become held. Driven
     * by the stored ledger records, so a month whose payslip was deleted is never brought back. The officer
     * on the imported payslip signs any letter a newly proven finding drafts (the history is one officer's).
     */
    private suspend fun reauditNeighbourMonths(
        imported: LedgerRecordEntity,
        history: List<LedgerRecordEntity>,
        officer: Officer,
    ) {
        val importedIndex = imported.year * 12 + imported.monthNum - 1
        history
            .filter { kotlin.math.abs(importedIndex - (it.year * 12 + it.monthNum - 1)) in 1..TptaAbsenceExplainer.RELOCATION_WINDOW_MONTHS }
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
        // One letter per month and dispute type: a re-audit never stacks a duplicate. A letter whose finding no
        // longer applies (addressed by a later payslip, or no longer proven) is removed with it; a letter for a
        // still-proven finding is never touched, so the officer's edits survive. Letters of types the audit
        // never drafts are left alone.
        val provenTypes = engineResult.anomalies.filter { it.type in REPRESENTATION_DRAFT_TYPES && it.isProven() }.map { it.type }.toSet()
        val existingDrafts = payslipDao.getAllRepresentationDrafts().firstOrNull() ?: emptyList()
        val stale = existingDrafts.filter { it.disputeMonth == dateStr && it.disputeType in REPRESENTATION_DRAFT_TYPES && it.disputeType !in provenTypes }
        stale.forEach { payslipDao.deleteRepresentationDraft(it.id) }
        val keptDrafts = existingDrafts - stale.toSet()
        // A deletion is remembered only while its finding stands: once the finding is gone, a later new one drafts afresh.
        val dismissed = payslipDao.getAllDismissedDrafts().filter { it.disputeMonth == dateStr }
        dismissed.filter { it.disputeType !in provenTypes }.forEach { payslipDao.deleteDismissedDraft(it.disputeMonth, it.disputeType) }
        val dismissedTypes = dismissed.map { it.disputeType }.toSet()
        engineResult.anomalies.forEach { anomaly ->
            val alreadyDrafted = keptDrafts.any { it.disputeMonth == dateStr && it.disputeType == anomaly.type }
            if (anomaly.type in REPRESENTATION_DRAFT_TYPES && anomaly.isProven() && !alreadyDrafted && anomaly.type !in dismissedTypes) {
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
}
