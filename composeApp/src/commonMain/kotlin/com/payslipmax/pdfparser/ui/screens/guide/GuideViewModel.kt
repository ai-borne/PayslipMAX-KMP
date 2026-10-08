package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.GuideRepository
import com.payslipmax.pdfparser.guide.data.InMemoryGuidePinsStorage
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.domain.GuideProfileProvider
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.telemetry.CrashReporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
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
    private val nowMillis: () -> Long = ::currentTimeMillis,
    private val profiles: GuideProfileProvider = GuideProfileProvider.None,
    /** The user's pins (E8). Memory-only by default, so a screen test needs no storage. */
    val pins: GuidePinsModel = GuidePinsModel(InMemoryGuidePinsStorage()),
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow<GuideUiState>(GuideUiState.Loading)
    val uiState: StateFlow<GuideUiState> = _uiState
    private val _pendingCard = MutableStateFlow<String?>(null)

    /**
     * The card a Pay Audit finding asked to open (E7). Memory only, never saved: after process death it is null and a
     * restored `Screen.GuideCard` pops itself. It holds a card id, which is never sent anywhere.
     */
    val pendingCard: StateFlow<String?> = _pendingCard
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

    /**
     * The one way to open a card from outside the Guide tab: records the target and makes sure the bundle is loading.
     * Callers check entitlement first (`guideUnlocked`); a locked user is sent to the upgrade sheet instead.
     */
    fun openCard(cardId: String) {
        _pendingCard.value = cardId
        load()
    }

    fun retry() {
        if (_uiState.value !is GuideUiState.Failed) return
        _uiState.value = GuideUiState.Loading
        job = null
        load()
    }

    fun area(areaId: String): GuideAreaContent? = index()?.area(areaId)?.toContent()

    /** The case's feed filtered by [facet]; a facet its chips could not show is ignored. */
    fun feed(
        caseId: String,
        facet: String?,
    ): GuideFeedContent? = index()?.feedContent(caseId, facet)

    /**
     * The card; its key points, cite, details and "your figure" are in the result only when [unlocked] (see
     * [GuideCardContent]). The [profile] is used only for that figure and is never kept here.
     */
    fun card(
        cardId: String,
        unlocked: Boolean,
        profile: GuideProfile? = null,
    ): GuideCardContent? = index()?.cardContent(cardId, unlocked, nowMillis(), profile)

    /** True when the card has a "your figure" line, so a screen reads the payslips for that card and no other. */
    fun hasFigure(cardId: String): Boolean = index()?.bundle?.figures?.figures?.values?.any { it.card == cardId } == true

    /** The officer's profile, read from the stored payslips only while a screen collects it. */
    fun profileFlow(): Flow<GuideProfile?> = profiles.profile()

    /** Guide Home's pinned rows for [cardIds], skipping any id the bundle no longer holds. */
    fun pinnedRows(cardIds: List<String>): List<GuideFeedRow> = index()?.pinnedRows(cardIds).orEmpty()

    fun crumbs(stack: List<GuideDestination>): List<GuideCrumb> = index()?.crumbs(stack).orEmpty()

    private fun index() = (_uiState.value as? GuideUiState.Ready)?.index

    private fun fail(error: GuideLoadError) {
        _uiState.value = GuideUiState.Failed(error)
        crashReporter.recordException(
            IllegalStateException("guide_load_failed_${error.name}"),
            mapOf(GUIDE_LOAD_ERROR_KEY to error.name),
        )
    }
}
