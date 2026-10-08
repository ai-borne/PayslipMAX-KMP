package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * What a card can do for an unlocked user. A locked card gets none (null), so these controls are absent from the locked
 * state, not hidden. [onCopyCite] is null when the card has no cite to copy.
 */
internal class GuideCardActions(
    val pinned: Boolean,
    val onTogglePin: () -> Unit,
    val onShare: () -> Unit,
    val onCopyCite: (() -> Unit)?,
)

/** The actions for [card], or null while it is locked. Nothing runs until a control is tapped. */
@Composable
internal fun rememberGuideCardActions(
    card: GuideCardContent,
    pins: GuidePinsModel,
    platform: GuidePlatform,
): GuideCardActions? {
    val current by pins.pins.collectAsState()
    val full = card.full ?: return null
    val note = card.shareNote() ?: return null
    return GuideCardActions(
        pinned = current.isPinned(card.id),
        onTogglePin = { pins.toggle(card.id) },
        onShare = { platform.share(note, GuideStrings.shareChooserTitle) },
        onCopyCite = full.cite.takeIf { it.isNotBlank() }?.let { cite -> { platform.copy(cite) } },
    )
}

/** Pin and Share, in a row under the answer. */
@Composable
internal fun GuideCardActionRow(actions: GuideCardActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        OutlinedButton(
            onClick = actions.onTogglePin,
            modifier = Modifier.semantics { if (actions.pinned) stateDescription = GuideStrings.pinnedState },
        ) { Text(if (actions.pinned) GuideStrings.unpin else GuideStrings.pin) }
        OutlinedButton(onClick = actions.onShare) { Text(GuideStrings.share) }
    }
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
