package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import com.payslipmax.pdfparser.guide.domain.GuideSearchNotes
import com.payslipmax.pdfparser.guide.domain.GuideSearchScope
import com.payslipmax.pdfparser.guide.domain.GuideTrust
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The longest query kept; anything typed or pasted past this is cut, so input to the scan is always bounded. */
internal const val MAX_SEARCH_QUERY_LENGTH = 100

/** A search result: the card's title and one-line answer, its trust chips, and the title of the case it is homed in. */
data class GuideSearchRow(
    val cardId: String,
    val title: String,
    val answer: String,
    val caseTitle: String,
    val trust: GuideTrust,
    /** True when the card is here only because a query word is in the user's note; the note itself is never in the row. */
    val matchedInNote: Boolean = false,
)

/** What the search screen shows below the field. */
sealed interface GuideSearchState {
    /** Nothing typed yet. */
    data object Idle : GuideSearchState

    /** Typed, but too short to search (one letter). */
    data object TooShort : GuideSearchState

    /** Matches, best first; an empty list is "no results". */
    data class Results(val rows: List<GuideSearchRow>, val unlockedScope: Boolean) : GuideSearchState
}

/**
 * The state as the screen may show it for the user's current access. Results are found under the scope that was set
 * when they were computed; if the entitlement has changed since (a purchase, or a revoked Premium), they are held back
 * until the model has searched again under the new scope, so a stale wider result list is never drawn.
 */
fun GuideSearchState.visibleTo(unlocked: Boolean): GuideSearchState = if (this is GuideSearchState.Results && unlockedScope != unlocked) GuideSearchState.Idle else this

/**
 * App-scoped (a Koin single), so the query and results survive opening a card and coming back, and a tab switch.
 * The query lives only in this object's memory: it is not saved with the Guide stack, never reaches telemetry (this
 * class has no [com.payslipmax.pdfparser.telemetry.CrashReporter]) and is gone when the app is killed (owner
 * decision 2026-10-07). Results are derived on [dispatcher] from the loaded Guide, so typing never waits on the
 * index being built.
 */
class GuideSearchViewModel(
    guide: GuideViewModel,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** Where the user's notes come from (M7); null searches the Guide only. Notes are read only for an unlocked user. */
    notes: GuideNotesViewModel? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // Locked until the screen says otherwise, so a missing call can only ever search less, never leak a paid field.
    private val _unlocked = MutableStateFlow(false)

    // A locked user never subscribes to the notes at all, so their words are not even decrypted for a search.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val noteWords: Flow<GuideSearchNotes> =
        _unlocked.flatMapLatest { unlocked -> if (unlocked && notes != null) notes.state.map { it.searchNotes } else flowOf(GuideSearchNotes.None) }

    val state: StateFlow<GuideSearchState> =
        combine(_query, guide.uiState, _unlocked, noteWords) { query, ui, unlocked, words -> search(query, ui, unlocked, words) }
            .stateIn(scope, SharingStarted.Eagerly, GuideSearchState.Idle)

    /** Free users search titles and rule numbers only; Premium searches every field (see [GuideSearchScope]). */
    fun setUnlocked(unlocked: Boolean) {
        _unlocked.value = unlocked
    }

    fun onQueryChange(text: String) {
        _query.value = text.take(MAX_SEARCH_QUERY_LENGTH)
    }

    /** Called when search is opened from Home, so each visit starts empty; coming back from a card does not call it. */
    fun clear() {
        _query.value = ""
    }

    private fun search(
        query: String,
        ui: GuideUiState,
        unlocked: Boolean,
        noteWords: GuideSearchNotes,
    ): GuideSearchState =
        when {
            ui !is GuideUiState.Ready || query.isBlank() -> GuideSearchState.Idle
            !GuideSearchIndex.isSearchable(query) -> GuideSearchState.TooShort
            else ->
                GuideSearchState.Results(
                    unlockedScope = unlocked,
                    rows =
                        ui.searchIndex.search(query, if (unlocked) GuideSearchScope.FULL else GuideSearchScope.PREVIEW, noteWords).map { hit ->
                            GuideSearchRow(
                                hit.card.id,
                                hit.card.title,
                                hit.card.answer,
                                ui.index.case(hit.card.nav)?.title.orEmpty(),
                                ui.index.trust(hit.card),
                                hit.matchedInNote,
                            )
                        },
                )
        }
}
