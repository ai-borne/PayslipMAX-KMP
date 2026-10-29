package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.payslipmax.pdfparser.ui.platform.rememberNotificationPermission
import com.payslipmax.pdfparser.ui.platform.rememberNotificationSettingsOpener
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2DraftEntry
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2LockedScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.Part2DueRow
import com.payslipmax.pdfparser.ui.screens.guide.part2.reminders.rememberSettingsNoteShown

/**
 * The Due level. Reminders are Premium, so an officer who may not draft (a restored stack, a stale link) sees the upgrade panel, never the
 * list; the list itself is read from the repository by [com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueViewModel]. A row opens its slip.
 * When reminders are off after the officer was asked, the list opens with a note and a button to the system settings (Round 44, item 39).
 */
@Composable
internal fun Part2DueLevel(
    navState: GuideNavState,
    host: Part2Host,
    listState: LazyListState,
) {
    val models = host.models
    val due = models?.due
    if (models == null || due == null || host.draftEntry != Part2DraftEntry.OPEN) {
        Part2LockedScreen(onBack = { navState.pop() }, onUnlock = host.onUnlock)
        return
    }
    val state by due.state.collectAsState()
    val settings = rememberNotificationSettingsOpener()
    val noteShown = rememberSettingsNoteShown(models.reminders, rememberNotificationPermission())
    DueScreen(
        state,
        onOpen = { navState.push(Part2Destination.Slip(it)) },
        onDismiss = { due.dismiss(it.draftId, it.task) },
        onBack = { navState.pop() },
        listState = listState,
        onOpenSettings = if (noteShown) settings::open else null,
    )
}

/** The Part II home's "Due" row: only for an entitled officer, and only while the planner has something to show. */
@Composable
internal fun rememberDueRow(
    navState: GuideNavState,
    host: Part2Host,
): Part2DueRow? {
    val due = host.models?.due ?: return null
    val state by due.state.collectAsState()
    if (host.draftEntry != Part2DraftEntry.OPEN || state.isEmpty) return null
    return Part2DueRow(state.dueCount) { navState.push(Part2Destination.Due) }
}
