package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2DraftEntry
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.DraftStatus
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.HrmsCheckScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2LockedScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.SlipPreviewActions
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.SlipPreviewScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.actions
import kotlinx.coroutines.launch

/**
 * The levels of a draft: the upgrade panel, the form and the slip preview. The form is only ever drawn for an entitled user: a free
 * user, or one whose Premium ended, reaching a form level by any route (a restored stack, a stale link) sees the upgrade panel, and a
 * lapsed user keeps the slip, which is read-only. The draft's model is held in the nav state while one of its levels is on the stack.
 */
@Composable
internal fun Part2DraftLevel(
    navState: GuideNavState,
    host: Part2Host,
    destination: Part2Destination,
    listState: LazyListState,
) {
    val onBack: () -> Unit = { navState.pop() }
    val allowed =
        when (destination) {
            is Part2Destination.Draft -> host.draftEntry == Part2DraftEntry.OPEN
            is Part2Destination.Slip, is Part2Destination.Hrms -> host.draftEntry != Part2DraftEntry.LOCKED
            else -> false
        }
    if (!allowed) {
        // Premium coming back (the upgrade sheet just succeeded) leaves the panel, so the officer is not stranded on "locked".
        LaunchedEffect(host.draftEntry) { if (destination == Part2Destination.Locked && host.draftEntry == Part2DraftEntry.OPEN) navState.pop() }
        Part2LockedScreen(onBack, host.onUnlock)
        return
    }
    val id = checkNotNull(destination.draftId)
    val model = rememberDraftModel(navState, host, id) ?: return GuideLoading()
    val state by model.state.collectAsState()
    when (state.status) {
        DraftStatus.LOADING -> GuideLoading()
        DraftStatus.GONE -> {
            // The repository no longer holds this draft (deleted elsewhere, or never saved): every level of it goes, as a restored stack must not show it.
            LaunchedEffect(id) { navState.cutDraft(id) }
            GuideLoading()
        }
        DraftStatus.READY ->
            when (destination) {
                is Part2Destination.Draft -> DraftForm(navState, model, id, listState)
                is Part2Destination.Hrms -> HrmsCheckScreen(state, onBack, listState)
                else -> SlipLevel(navState, host, model, id, listState)
            }
    }
}

/** The model for draft [id]: the one the nav state holds, or one read back from the repository (after a lock or process death). */
@Composable
private fun rememberDraftModel(
    navState: GuideNavState,
    host: Part2Host,
    id: String,
): LeaveDraftViewModel? {
    navState.part2Draft?.takeIf { it.id == id }?.let { return it }
    val opened = host.models?.drafting?.open(id) ?: return null
    navState.discardPart2Draft()
    navState.part2Draft = opened
    return opened
}

@Composable
private fun DraftForm(
    navState: GuideNavState,
    model: LeaveDraftViewModel,
    id: String,
    listState: LazyListState,
) {
    val state by model.state.collectAsState()
    LeaveDraftScreen(state, model.actions(), onSeeSlip = { navState.push(Part2Destination.Slip(id)) }, onBack = { navState.pop() }, listState = listState)
}

@Composable
private fun SlipLevel(
    navState: GuideNavState,
    host: Part2Host,
    model: LeaveDraftViewModel,
    id: String,
    listState: LazyListState,
) {
    val state by model.state.collectAsState()
    val scope = rememberCoroutineScope()
    val actions =
        SlipPreviewActions(
            // Edit goes back to the form below when the officer came from it, so Back never walks a form-preview-form loop.
            onEdit = { if (navState.stack.getOrNull(navState.stack.size - 2) == Part2Destination.Draft(id)) navState.pop() else navState.push(Part2Destination.Draft(id)) },
            onTick = model::acknowledge,
            onDelete = { model.delete { scope.launch { navState.cutDraft(id) } } },
            onPrint = { model.print(host.printer) },
            onHrms = { navState.push(Part2Destination.Hrms(id)) },
        )
    SlipPreviewScreen(state, canEdit = host.draftEntry == Part2DraftEntry.OPEN, actions = actions, onBack = { navState.pop() }, listState = listState)
}
