package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.DeterministicIntelligenceEngine
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias PayAuditEngine = (current: ParsedPayslip, previous: ParsedPayslip?, history: List<ParsedPayslip>) -> EngineResult

/**
 * Owns Pay Audit's selected month, engine runs and derived UI state (docs/Plan Phase 2 U7), so composables
 * only render [uiState]. Engine runs happen on [dispatcher], which must be single-threaded: the per-month
 * result cache is touched only there, while the inputs live in a [MutableStateFlow] snapshot that public
 * calls (main thread) can update safely. Each month is analysed at most once per payslip list.
 */
class PayAuditViewModel(
    private val engine: PayAuditEngine = { current, previous, history -> DeterministicIntelligenceEngine.analyze(current, previous, history) },
    dispatcher: CoroutineDispatcher = defaultPayAuditDispatcher(),
) {
    private data class Inputs(
        val payslips: List<ParsedPayslip> = emptyList(),
        val hasAccess: Boolean = false,
        val selected: PayMonth? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(PayAuditUiState())
    val uiState: StateFlow<PayAuditUiState> = _uiState

    private val inputs = MutableStateFlow(Inputs())
    private var job: Job? = null

    // Confined to the single-threaded [scope] dispatcher.
    private var cachedFor: List<ParsedPayslip> = emptyList()
    private val results = HashMap<PayMonth, EngineResult>()

    fun setInputs(
        payslips: List<ParsedPayslip>,
        hasAccess: Boolean,
        requestedMonth: PayMonth? = null,
    ) {
        val sorted = payslips.sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum })
        val available = sorted.map { PayMonth(it.year, it.monthNum) }
        val current = inputs.value.selected
        val selected =
            if (requestedMonth != null) {
                requestedMonth.takeIf { it in available } ?: available.lastOrNull()
            } else {
                current?.takeIf { it in available } ?: available.lastOrNull()
            }
        inputs.value = Inputs(sorted, hasAccess, selected)
        _uiState.update { it.copy(availableMonths = available, isLocked = !hasAccess, selectedMonth = selected) }
        refresh()
    }

    fun selectMonth(month: PayMonth) {
        if (month !in _uiState.value.availableMonths) return
        inputs.update { it.copy(selected = month) }
        _uiState.update { it.copy(selectedMonth = month) }
        refresh()
    }

    fun selectTab(tab: PayAuditTab) = _uiState.update { it.copy(tab = tab) }

    /** Cancels any in-flight analysis; call when the screen leaves composition. */
    fun dispose() = scope.cancel()

    private fun refresh() {
        job?.cancel()
        job =
            scope.launch {
                val snapshot = inputs.value
                if (snapshot.payslips != cachedFor) {
                    results.clear()
                    cachedFor = snapshot.payslips
                }
                val current = snapshot.payslips.firstOrNull { PayMonth(it.year, it.monthNum) == snapshot.selected }
                if (current == null) {
                    _uiState.update { PayAuditUiState(tab = it.tab, isLocked = !snapshot.hasAccess) }
                    return@launch
                }
                publishMonth(snapshot, current)
                publishHistory(snapshot)
            }
    }

    private fun publishMonth(
        snapshot: Inputs,
        current: ParsedPayslip,
    ) {
        val result = resultFor(snapshot.payslips, current)
        val all = classifyPayAuditFindings(result.anomalies)
        val visible = if (snapshot.hasAccess) all else PayAuditMonthFindings()
        val total = all.issues.size + all.waiting.size + all.verified.size
        _uiState.update {
            it.copy(
                verdict = buildPayAuditVerdict(all, snapshot.hasAccess, payAuditLinesChecked(current)),
                findings = visible,
                hiddenFindingCount = total - (visible.issues.size + visible.waiting.size + visible.verified.size),
                changes = result.changeExplanations,
                allChanges = result.allChangeExplanations,
                timeline = result.timeline,
                incrementPrediction = result.incrementPrediction,
                dsopRoom = result.dsopRoom,
            )
        }
    }

    private fun publishHistory(snapshot: Inputs) {
        var issues = 0
        for (slip in snapshot.payslips) {
            scope.coroutineContext.ensureActive()
            issues += classifyPayAuditFindings(resultFor(snapshot.payslips, slip).anomalies).issues.size
        }
        _uiState.update { it.copy(history = PayAuditHistorySummary(snapshot.payslips.size, issues)) }
    }

    private fun resultFor(
        payslips: List<ParsedPayslip>,
        slip: ParsedPayslip,
    ): EngineResult = results.getOrPut(PayMonth(slip.year, slip.monthNum)) { engine(slip, previousOf(payslips, slip), payslips) }

    private fun previousOf(
        payslips: List<ParsedPayslip>,
        current: ParsedPayslip,
    ): ParsedPayslip? {
        val (year, month) = if (current.monthNum == 1) current.year - 1 to 12 else current.year to current.monthNum - 1
        return payslips.firstOrNull { it.year == year && it.monthNum == month }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun defaultPayAuditDispatcher(): CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)
