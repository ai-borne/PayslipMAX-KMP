package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The longest query kept; anything typed or pasted past this is cut, so input to the scan is always bounded. */
internal const val MAX_SEARCH_QUERY_LENGTH = 100

/** A search result: the card's title and one-line answer, and the title of the case it is homed in. */
data class GuideSearchRow(val cardId: String, val title: String, val answer: String, val caseTitle: String)

/** What the search screen shows below the field. */
sealed interface GuideSearchState {
    /** Nothing typed yet. */
    data object Idle : GuideSearchState

    /** Typed, but too short to search (one letter). */
    data object TooShort : GuideSearchState

    /** Matches, best first; an empty list is "no results". */
    data class Results(val rows: List<GuideSearchRow>) : GuideSearchState
}

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
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val state: StateFlow<GuideSearchState> =
        combine(_query, guide.uiState) { query, ui -> search(query, ui) }
            .stateIn(scope, SharingStarted.Eagerly, GuideSearchState.Idle)

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
    ): GuideSearchState =
        when {
            ui !is GuideUiState.Ready || query.isBlank() -> GuideSearchState.Idle
            !GuideSearchIndex.isSearchable(query) -> GuideSearchState.TooShort
            else ->
                GuideSearchState.Results(
                    ui.searchIndex.search(query).map { hit ->
                        GuideSearchRow(hit.card.id, hit.card.title, hit.card.answer, ui.index.case(hit.card.nav)?.title.orEmpty())
                    },
                )
        }
}
