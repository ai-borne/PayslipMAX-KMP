package com.payslipmax.pdfparser.ui.screens.guide.part2.reminders

import com.payslipmax.pdfparser.part2.reminders.NotificationPermission
import com.payslipmax.pdfparser.part2.reminders.ReminderPromptStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The one-time ask for the notification permission (DECISIONS Rounds 10 and 28). The first time the officer has a reminder, an explainer of
 * ours is shown; "Allow" then shows the system's dialog and "Not now" is a decline. Either way the answer is recorded in the
 * [ReminderPromptStore] and the officer is never asked again (the Due list still works without notifications). The answer is recorded
 * before the system dialog opens, so a kill while it is up cannot lead to a second ask. A device with nothing to ask (older Android,
 * or already decided) is recorded as asked with no prompt. The permission itself comes from the screen: on Android the system dialog
 * needs an Activity. Nothing about a draft reaches this model.
 */
class ReminderPromptModel(
    val hasReminders: StateFlow<Boolean>,
    private val store: ReminderPromptStore,
) {
    private val _visible = MutableStateFlow(false)

    /** Whether our explainer is showing. */
    val visible: StateFlow<Boolean> = _visible

    /** The host calls this when [hasReminders] turns true: shows the explainer once, or settles a device that has nothing to ask. */
    suspend fun onRemindersExist(permission: NotificationPermission) {
        if (!hasReminders.value || store.wasAsked()) return
        if (permission.canAsk()) _visible.value = true else store.markAsked()
    }

    /** "Allow": records the ask, hides the explainer and shows the system's dialog. */
    suspend fun allow(permission: NotificationPermission) {
        store.markAsked()
        _visible.value = false
        permission.request()
    }

    /** Whether the Due screen should show the "reminders are off" note now (see [ReminderSettingsNote]). Reads only: it asks nothing and records nothing. */
    suspend fun settingsNoteShown(permission: NotificationPermission): Boolean = ReminderSettingsNote.isShown(store.wasAsked(), permission.isAllowed())

    /** "Not now" (or dismissing the explainer): a decline, final. */
    fun notNow() {
        store.markAsked()
        _visible.value = false
    }
}
