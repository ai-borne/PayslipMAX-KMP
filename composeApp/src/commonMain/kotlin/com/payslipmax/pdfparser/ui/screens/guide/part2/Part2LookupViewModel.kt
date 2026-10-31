package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.part2.domain.EntryForm
import com.payslipmax.pdfparser.part2.domain.Part2CodeContent
import com.payslipmax.pdfparser.part2.domain.Part2LookupIndex
import com.payslipmax.pdfparser.part2.domain.Part2RibbonContent
import com.payslipmax.pdfparser.part2.domain.Part2RibbonTile
import com.payslipmax.pdfparser.part2.domain.Part2SearchResult
import com.payslipmax.pdfparser.ui.screens.guide.GuideUiState
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.MAX_SEARCH_QUERY_LENGTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-scoped (a Koin single, like the Guide's own search), so the typed query survives opening a code page and coming back, and a tab
 * switch. The query lives only in this object's memory: it is not saved with the Guide stack, never reaches telemetry (this class has
 * no CrashReporter) and is gone when the app is killed. Everything else is read from the loaded Guide on demand: nothing is shown until
 * the bundle has loaded and ships a Part II section, and the index over it is built once per bundle. Searching is a few hundred plain
 * scans, so it runs on the caller and needs no dispatcher.
 */
class Part2LookupViewModel(private val guide: GuideViewModel) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private var built: Pair<Part2Lookup, Part2LookupIndex>? = null

    fun onQueryChange(text: String) {
        _query.value = text.take(MAX_SEARCH_QUERY_LENGTH)
    }

    /** Called when Part II is opened from Guide Home, so each visit starts empty; coming back from a code page does not call it. */
    fun clear() {
        _query.value = ""
    }

    fun ribbons(): List<Part2RibbonTile> = index()?.ribbons().orEmpty()

    fun ribbon(id: String): Part2RibbonContent? = index()?.ribbon(id)

    fun code(code: String): Part2CodeContent? = index()?.code(code)

    /** The form for adding [code] to the Orders Bank, or null when the loaded bundle does not know the code. */
    fun entryForm(code: String): EntryForm? = index()?.let { EntryForm.of(it, code) }

    fun search(query: String = _query.value): Part2SearchResult = index()?.search(query) ?: Part2SearchResult.Idle

    private fun index(): Part2LookupIndex? {
        val lookup = ((guide.uiState.value as? GuideUiState.Ready)?.bundle)?.part2 ?: return null
        built?.takeIf { it.first === lookup }?.let { return it.second }
        return Part2LookupIndex(lookup).also { built = lookup to it }
    }
}
