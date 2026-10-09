package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.payslipmax.pdfparser.guide.domain.GuideSuggestion
import com.payslipmax.pdfparser.ui.screens.SupportMessageDialog
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * What a card can do for an unlocked user. A locked card gets none (null), so these controls are absent from the locked
 * state, not hidden. [onCopyCite] is null when the card has no cite to copy. [onSuggest] opens the user's mail app with the
 * typed correction ready (M5); the app sends nothing itself.
 */
internal class GuideCardActions(
    val pinned: Boolean,
    val onTogglePin: () -> Unit,
    val onShare: () -> Unit,
    val onCopyCite: (() -> Unit)?,
    val onSuggest: (text: String) -> Unit,
)

/** The actions for [card], or null while it is locked. Nothing runs until a control is tapped. */
@Composable
internal fun rememberGuideCardActions(
    card: GuideCardContent,
    viewModel: GuideViewModel,
    platform: GuidePlatform,
): GuideCardActions? {
    val pins = viewModel.pins
    val current by pins.pins.collectAsState()
    val full = card.full ?: return null
    val note = card.shareNote() ?: return null
    return GuideCardActions(
        pinned = current.isPinned(card.id),
        onTogglePin = { pins.toggle(card.id) },
        onShare = { platform.share(note, GuideStrings.shareChooserTitle) },
        onCopyCite = full.cite.takeIf { it.isNotBlank() }?.let { cite -> { platform.copy(cite) } },
        onSuggest = { text -> viewModel.suggest(card.id, text, platform) },
    )
}

/** Pin, Share and Suggest a correction, in a row under the answer that wraps at large text sizes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GuideCardActionRow(actions: GuideCardActions) {
    var suggesting by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        OutlinedButton(
            onClick = actions.onTogglePin,
            modifier = Modifier.semantics { if (actions.pinned) stateDescription = GuideStrings.pinnedState },
        ) { Text(if (actions.pinned) GuideStrings.unpin else GuideStrings.pin) }
        OutlinedButton(onClick = actions.onShare) { Text(GuideStrings.share) }
        OutlinedButton(onClick = { suggesting = true }) { Text(GuideMaintenanceStrings.suggest) }
    }
    if (suggesting) GuideSuggestionDialog(onDismiss = { suggesting = false }, onSend = actions.onSuggest)
}

/** The Suggest a correction dialog: the shared support dialog with the Guide's copy. The text lives only while it is open. */
@Composable
private fun GuideSuggestionDialog(
    onDismiss: () -> Unit,
    onSend: (text: String) -> Unit,
) {
    SupportMessageDialog(
        title = GuideMaintenanceStrings.suggestDialogTitle,
        notice = GuideMaintenanceStrings.suggestNotice,
        fieldLabel = GuideMaintenanceStrings.suggestFieldLabel,
        confirmLabel = GuideMaintenanceStrings.suggestOpenEmail,
        maxLength = GuideSuggestion.MAX_TEXT_LENGTH,
        onDismiss = onDismiss,
        onConfirm = onSend,
    )
}

/** Copies the cite and says so; the label resets when the card (or its cite) changes. */
@Composable
internal fun GuideCopyCiteButton(onCopy: () -> Unit) {
    var copied by remember { mutableStateOf(false) }
    TextButton(onClick = {
        onCopy()
        copied = true
    }) { Text(if (copied) GuideStrings.citeCopied else GuideStrings.copyCite) }
}
