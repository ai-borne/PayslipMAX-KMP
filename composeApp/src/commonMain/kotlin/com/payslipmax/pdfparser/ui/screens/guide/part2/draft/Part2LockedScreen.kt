package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.screens.guide.GuideLabelPill
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2DraftEntry
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** What a user without Premium sees when they ask to draft: what Premium adds and the upgrade button. Nothing of theirs is read or stored. */
@Composable
internal fun Part2LockedScreen(
    onBack: () -> Unit,
    onUnlock: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = Part2Strings.formTitle, subtitle = Part2Strings.homeSubtitle, onBack = onBack)
        FlatBorderedCardShape(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            ) {
                Text(Part2Strings.lockedTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                Text(Part2Strings.lockedBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onUnlock) { Text(GuideStrings.unlock) }
            }
        }
    }
}

/** The "Draft this order" row under a code: for a user who may not draft it carries the Premium label and leads to the upgrade panel. */
@Composable
internal fun Part2DraftButton(
    entry: Part2DraftEntry,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(Part2Strings.draftThisOrder, modifier = Modifier.weight(1f))
        if (entry != Part2DraftEntry.OPEN) GuideLabelPill(Part2Strings.draftPremium)
    }
}
