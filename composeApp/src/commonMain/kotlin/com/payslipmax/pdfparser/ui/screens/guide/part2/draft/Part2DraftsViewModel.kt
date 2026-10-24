package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Part2DraftEngine
import com.payslipmax.pdfparser.part2.domain.Part2DraftSummary
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.ui.UI_STOP_TIMEOUT_MS
import com.payslipmax.pdfparser.ui.screens.guide.GuideUiState
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.utils.localUtcOffsetMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One saved draft in the list: what it asks for, its span (or that it is not finished) and when it was last edited. */
data class DraftRowUi(
    val id: String,
    val title: String,
    val line: String,
)

/** The drafts list. [hasAny] is true whenever the device holds a readable draft, even before the Guide has loaded to word its row. */
data class DraftsUiState(
    val rows: List<DraftRowUi>,
    val unreadable: Boolean,
    val hasAny: Boolean,
) {
    override fun toString(): String = "DraftsUiState(rows=${rows.size})"

    companion object {
        val Empty = DraftsUiState(emptyList(), unreadable = false, hasAny = false)
    }
}

/**
 * The saved drafts, newest edit first, each worded from the bundle's Part II section (so nothing is shown until the Guide has loaded and
 * ships one, though [DraftsUiState.hasAny] counts a readable draft either way: the entry decision needs that). A draft the device
 * cannot read is counted, never listed and never deleted. No telemetry: nothing about a draft is logged.
 */
class Part2DraftsViewModel(
    guide: GuideViewModel,
    private val repository: Part2Repository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val localOffset: (atMillis: Long) -> Long = ::localUtcOffsetMillis,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var built: Pair<Part2Lookup, Part2DraftEngine>? = null

    val state: StateFlow<DraftsUiState> =
        combine(guide.uiState, repository.observeDrafts()) { ui, stored ->
            val engine = ((ui as? GuideUiState.Ready)?.bundle)?.part2?.let(::engineFor)
            DraftsUiState(
                rows = if (engine == null) emptyList() else stored.items.map { row(Part2DraftSummary.of(it, engine)) },
                unreadable = stored.unreadable > 0,
                hasAny = stored.items.isNotEmpty(),
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(UI_STOP_TIMEOUT_MS), DraftsUiState.Empty)

    fun delete(id: String) {
        scope.launch { repository.deleteDraft(id) }
    }

    private fun engineFor(section: Part2Lookup): Part2DraftEngine {
        built?.takeIf { it.first === section }?.let { return it.second }
        return Part2DraftEngine(section, Part2Rules.shipped()).also { built = section to it }
    }

    private fun row(summary: Part2DraftSummary): DraftRowUi {
        val from = summary.from
        val to = summary.to
        val span = if (from != null && to != null) Part2Strings.dateSpan(CivilCalendar.format(from), CivilCalendar.format(to)) else null
        return DraftRowUi(summary.id, Part2Strings.draftRowTitle(summary.codes), Part2Strings.draftRowLine(span, CivilCalendar.format(PreClaimDates.dayOfInstant(summary.updatedAt, localOffset(summary.updatedAt)))))
    }
}
