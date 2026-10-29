package com.payslipmax.pdfparser.ui.screens.guide.part2.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.payslipmax.pdfparser.part2.reminders.NotificationPermission
import kotlinx.coroutines.launch

/**
 * Whether the "reminders are off" note shows now. Worked out when the screen appears and again every time the app comes back to the front, so
 * the note is gone after the officer turned reminders on in the system settings. Nothing is stored: it is asked afresh each time, so there is
 * nothing to lose with the process.
 */
@Composable
fun rememberSettingsNoteShown(
    model: ReminderPromptModel,
    permission: NotificationPermission,
): Boolean {
    var shown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(model, permission) {
        scope.launch { shown = model.settingsNoteShown(permission) }
        onPauseOrDispose { }
    }
    return shown
}
