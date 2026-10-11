package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuidePreClaim
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.CtgCase
import com.payslipmax.pdfparser.guide.preclaim.PreClaimChoices
import com.payslipmax.pdfparser.guide.preclaim.PreClaimLimits
import com.payslipmax.pdfparser.guide.preclaim.PreClaimProfile
import com.payslipmax.pdfparser.telemetry.CrashReporter
import com.payslipmax.pdfparser.ui.UI_STOP_TIMEOUT_MS
import com.payslipmax.pdfparser.ui.screens.guide.GuideUiState
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * One TA/DA pre-claim worksheet, created when the screen opens and [close]d when it leaves, so the form dies with the screen, a lock
 * or process death (never saved, never sent). It reads the loaded Claim Guide for the worksheet and the figures, takes the officer's
 * level, basic, DA, MSP and NPA from the latest payslip only while a screen collects [state] (every value editable; fully manual
 * with no payslip), and hands the typed answers to the pure calculators in `shared`. Every setter cleans or checks its input and
 * ignores an id the bundle does not offer.
 *
 * Telemetry: one breadcrumb when a move is chosen, carrying the move type and nothing the officer typed. No input is logged,
 * reported or put in an exception message; a test scans for that.
 */
class PreClaimViewModel(
    private val guide: GuideViewModel,
    private val crashReporter: CrashReporter,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val form = MutableStateFlow(PreClaimFormState())

    /**
     * The typed form, updated in the same step as a setter, which [state] (combined on another dispatcher) is not. The text boxes bind
     * to this so a fast typist never sees a box reset to a stale value; everything else the screen draws comes from [state].
     */
    val typed: StateFlow<PreClaimFormState> get() = form

    init {
        guide.load()
    }

    /** Frames combine the Guide, the form and the payslip defaults; the payslip is read once per subscription, not while nobody looks. */
    val state: StateFlow<PreClaimUiState> =
        combine(guide.uiState, form, defaults()) { ui, formState, defaults ->
            when (ui) {
                GuideUiState.Loading -> PreClaimUiState.Loading
                is GuideUiState.Failed -> PreClaimUiState.Unavailable
                is GuideUiState.Ready -> PreClaimWorksheetBuilder.build(ui.bundle, formState, defaults)?.let { PreClaimUiState.Ready(it) } ?: PreClaimUiState.Unavailable
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(UI_STOP_TIMEOUT_MS), PreClaimUiState.Loading)

    private fun defaults(): Flow<PreClaimProfile> =
        flow { emit(PreClaimProfile.from(guide.profileFlow().firstOrNull())) }.onStart { emit(PreClaimProfile.from(null)) }

    private fun bundle(): GuideBundle? = (guide.uiState.value as? GuideUiState.Ready)?.bundle

    private fun section(): GuidePreClaim? = bundle()?.preclaim

    private fun fieldsOf(move: PreClaimMoveType?) = move?.let { section()?.moves?.get(it.key)?.fields }.orEmpty()

    // ---- move ----

    /** Starts a fresh worksheet for [move]; only the officer's profile edits carry over. A move the bundle lacks is ignored. */
    fun chooseMove(move: PreClaimMoveType) {
        val before = form.value
        if (before.move == move || section()?.moves?.containsKey(move.key) != true) return
        form.value = PreClaimFormState(move = move, edits = before.edits)
        crashReporter.log("$BREADCRUMB${move.key}")
    }

    /** After the Guide failed to load; the worksheet shows [PreClaimUiState.Unavailable] until it has. A no-op in any other state. */
    fun retry() = guide.retry()

    fun backToMoves() = form.update { PreClaimFormState(edits = it.edits) }

    /** Drops the form and stops the collection; the screen calls it on leaving. */
    fun close() {
        form.value = PreClaimFormState()
        scope.cancel()
    }

    // ---- the officer's profile ----

    fun chooseLevel(level: String?) {
        if (level != null && level !in PreClaimChoices.levels(bundle()?.figures)) return
        form.update { it.copy(edits = it.edits.copy(level = level ?: "")) }
    }

    fun editBasicPay(raw: String) = form.update { it.copy(edits = it.edits.copy(basic = PreClaimInput.digits(raw, PreClaimLimits.MAX_BASIC_PAY))) }

    fun editDaPercent(raw: String) = form.update { it.copy(edits = it.edits.copy(da = PreClaimInput.digits(raw, PreClaimLimits.MAX_DA_PERCENT.toLong()))) }

    fun editMsp(raw: String) = form.update { it.copy(edits = it.edits.copy(msp = PreClaimInput.digits(raw, PreClaimLimits.MAX_BASIC_PAY))) }

    fun editNpa(raw: String) = form.update { it.copy(edits = it.edits.copy(npa = PreClaimInput.digits(raw, PreClaimLimits.MAX_BASIC_PAY))) }

    /** Back to the payslip's values for all five boxes. */
    fun resetProfileToPayslip() = form.update { it.copy(edits = ProfileEdits()) }

    // ---- questions and ticks ----

    /** Ticks or unticks [fieldId] when the current move has a tick field of that id. The Andaman Nicobar tick pre-selects the island CTG case until the officer picks a case. */
    fun setTick(
        fieldId: String,
        ticked: Boolean,
    ) {
        val current = form.value
        if (fieldsOf(current.move).none { it.id == fieldId && it.kind == PreClaimFieldIds.KIND_TICK }) return
        val ticks = if (ticked) current.ticks + fieldId else current.ticks - fieldId
        val islandFollows = fieldId == PreClaimFieldIds.ANDAMAN_NICOBAR && !current.ctgCaseChosen
        val case = if (islandFollows) (if (ticked) CtgCase.ISLAND else CtgCase.NORMAL) else current.ctgCase
        form.value = current.copy(ticks = ticks, ctgCase = case)
    }

    fun setMaritalStatus(raw: String) {
        val limit = section()?.limits?.textChars ?: return
        form.update { it.copy(maritalStatus = PreClaimInput.text(raw, limit)) }
    }

    fun chooseCtgCase(case: CtgCase) = form.update { it.copy(ctgCase = case, ctgCaseChosen = true) }

    // ---- journey ----

    fun setRoadKm(raw: String) = form.update { it.copy(roadKm = PreClaimInput.digits(raw, PreClaimInput.MAX_KM)) }

    /** [state] must be a name from the RMA table; anything else is ignored. null clears the choice. */
    fun chooseOriginState(state: String?) {
        if (state != null && state !in PreClaimChoices.states(bundle()?.figures)) return
        form.update { it.copy(originState = state) }
    }

    /** Picks the move's "TR Rule" option (temporary duty: which rule; leave travel: the rule picker). An id the move's list lacks is ignored. */
    fun chooseTrRule(optionId: String?) {
        val sheet = form.value.move?.let { section()?.moves?.get(it.key) } ?: return
        val group = section()?.options?.get(PreClaimWorksheetBuilder.ruleGroup(sheet)) ?: return
        if (optionId != null && group.options.none { it.id == optionId }) return
        form.update { it.copy(trRuleId = optionId) }
    }

    fun setFromDate(date: CalendarDate?) = form.update { it.copy(from = date) }

    fun setToDate(date: CalendarDate?) = form.update { it.copy(to = date) }

    // ---- temporary duty ----

    /** Hours 0 to 24: a keystroke that would pass 24 is ignored and the box keeps what it had. */
    fun setAwayHours(raw: String) = form.update { it.copy(awayHours = PreClaimInput.clock(raw, PreClaimInput.MAX_HOURS, it.awayHours)) }

    /** Minutes 0 to 59, same rule. */
    fun setAwayMinutes(raw: String) = form.update { it.copy(awayMinutes = PreClaimInput.clock(raw, PreClaimInput.MAX_MINUTES, it.awayMinutes)) }

    /** Types nights; clearing the box returns to the day count. */
    fun setNights(raw: String) = form.update { it.copy(nights = PreClaimInput.digits(raw, PreClaimInput.MAX_NIGHTS).ifEmpty { null }) }

    fun setNightlyRate(raw: String) = form.update { it.copy(nightlyRate = PreClaimInput.digits(raw, PreClaimLimits.MAX_NIGHTLY_RATE)) }

    fun setHoldsRmaSanction(holds: Boolean?) = form.update { it.copy(holdsRmaSanction = holds) }

    companion object {
        /** The one telemetry text this flow writes, followed by the move type (`PD`, `TD`, `LTC`); no number, name or value is ever added. */
        const val BREADCRUMB = "preclaim_opened_"
    }
}
