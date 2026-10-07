package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.GuideRepository
import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.guide.model.GuideCase
import com.payslipmax.pdfparser.telemetry.CrashReporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The one telemetry key the Guide writes. Passes [com.payslipmax.pdfparser.telemetry.TelemetrySanitizer]. */
internal const val GUIDE_LOAD_ERROR_KEY = "error_guide_load"

/**
 * App-scoped (a Koin single) owner of the loaded Claim Guide. Nothing is read until the Guide tab first calls
 * [load]; after that the bundle stays in memory, so switching tabs or locking the app never reloads it. A
 * failure is shown with retry and reports only its [GuideLoadError] code, never card text or ids.
 */
class GuideViewModel(
    private val repository: GuideRepository,
    private val crashReporter: CrashReporter,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow<GuideUiState>(GuideUiState.Loading)
    val uiState: StateFlow<GuideUiState> = _uiState
    private var job: Job? = null

    /** Starts the first load; a no-op while one is running or once the bundle is loaded. */
    fun load() {
        if (job != null) return
        job =
            scope.launch {
                when (val result = repository.load()) {
                    is GuideLoadResult.Loaded -> _uiState.value = result.bundle.toReady()
                    is GuideLoadResult.Failed -> fail(result.error)
                }
            }
    }

    fun retry() {
        if (_uiState.value !is GuideUiState.Failed) return
        _uiState.value = GuideUiState.Loading
        job = null
        load()
    }

    fun area(areaId: String): GuideAreaContent? = bundle()?.nav?.firstOrNull { it.id == areaId }?.toContent()

    fun case(caseId: String): GuideCase? = bundle()?.nav?.firstNotNullOfOrNull { area -> area.cases.firstOrNull { it.id == caseId } }

    fun card(cardId: String): GuideCard? = bundle()?.cards?.firstOrNull { it.id == cardId }

    private fun bundle() = (_uiState.value as? GuideUiState.Ready)?.bundle

    private fun fail(error: GuideLoadError) {
        _uiState.value = GuideUiState.Failed(error)
        crashReporter.recordException(
            IllegalStateException("guide_load_failed_${error.name}"),
            mapOf(GUIDE_LOAD_ERROR_KEY to error.name),
        )
    }
}
