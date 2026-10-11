package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.screens.guide.GuideLoading
import com.payslipmax.pdfparser.ui.screens.guide.GuidePlatform
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/**
 * Where the worksheet destination is drawn. An unlocked user gets the worksheet the Guide tab holds for them ([model], see
 * `PreClaimHost`); a locked user gets the upgrade panel and no ViewModel exists, so no payslip is read for them.
 */
@Composable
internal fun PreClaimRoute(
    entry: PreClaimEntry,
    model: PreClaimViewModel?,
    platform: GuidePlatform,
    onBack: () -> Unit,
    onUnlock: () -> Unit,
    onOpenCard: (cardId: String) -> Unit,
    listState: LazyListState,
) {
    when {
        entry != PreClaimEntry.OPEN -> PreClaimLockedScreen(onBack, onUnlock)
        model == null -> GuideLoading()
        else -> PreClaimScreen(model, platform, onBack, onOpenCard, listState)
    }
}

/** What a free user sees: the header, what Premium adds for this worksheet, and the upgrade button. Nothing about their pay is read. */
@Composable
internal fun PreClaimLockedScreen(
    onBack: () -> Unit,
    onUnlock: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = PreClaimStrings.screenTitle, subtitle = PreClaimStrings.screenSubtitle, onBack = onBack)
        FlatBorderedCardShape(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            ) {
                Text(PreClaimStrings.lockedTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                Text(PreClaimStrings.lockedBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onUnlock) { Text(GuideStrings.unlock) }
            }
        }
    }
}
