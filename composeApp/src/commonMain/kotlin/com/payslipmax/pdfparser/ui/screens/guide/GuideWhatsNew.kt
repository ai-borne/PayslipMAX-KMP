package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings

/**
 * The Guide Home row that opens the change log, named for the month of its newest entry. While the entry is [isNew] it is a banner:
 * a NEW chip, how many changes the entry lists ([changeCount]) and a dismiss button. Once seen it is the quiet one-line row, so the
 * change log stays one tap away without asking for attention.
 */
@Composable
internal fun GuideWhatsNewRow(
    latestDate: String,
    onClick: () -> Unit,
    isNew: Boolean = false,
    changeCount: Int = 0,
    onDismiss: () -> Unit = {},
) {
    GuideTileFrame(onClick = onClick, tint = if (isNew) CardTint.Accent else CardTint.Neutral) {
        Row(
            modifier = Modifier.padding(AppDimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Decoration(GuideMaintenanceStrings.whatsNewEmoji, AppDimensions.TextSizeLarge)
            Column(modifier = Modifier.weight(1f)) {
                Text(GuideMaintenanceStrings.whatsNewRow(latestDate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (isNew && changeCount > 0) {
                    Text(GuideMaintenanceStrings.whatsNewSummary(changeCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isNew) {
                Text(
                    GuideMaintenanceStrings.whatsNewChip,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 50)).padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTwo),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(AppDimensions.IconSizeMedium)) {
                    Icon(Icons.Default.Close, contentDescription = GuideMaintenanceStrings.whatsNewDismiss, modifier = Modifier.size(AppDimensions.IconSizeSmall))
                }
            } else {
                Decoration(AppStrings.rowArrow, AppDimensions.TextSizeLarge, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * "What's new in the Guide": each dated entry's lines, newest first, with a link to every card a line names. The text is
 * the maintainer's own words from the bundle; the links open cards through the Guide stack, so Back returns to this list.
 */
@Composable
internal fun GuideWhatsNewScreen(
    content: GuideWhatsNewContent,
    onBack: () -> Unit,
    onOpenCard: (cardId: String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = GuideMaintenanceStrings.whatsNewTitle, subtitle = GuideMaintenanceStrings.whatsNewSubtitle, onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            items(content.entries, key = { it.date }) { entry -> GuideChangeEntryView(entry, onOpenCard) }
        }
    }
}

@Composable
private fun GuideChangeEntryView(
    entry: GuideChangeEntryContent,
    onOpenCard: (cardId: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(
            GuideMaintenanceStrings.day(entry.date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        entry.lines.forEach { line ->
            Text(line.text, style = MaterialTheme.typography.bodyMedium)
            line.cards.forEach { link -> TextButton(onClick = { onOpenCard(link.cardId) }) { Text(link.title) } }
        }
    }
}
