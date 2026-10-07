package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.guide.domain.GuideTrust
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideColors
import com.payslipmax.pdfparser.ui.theme.GuideStrings

private val ChipShape = RoundedCornerShape(percent = 50)

/**
 * The trust chips wherever a card appears: Rates as of, Amended, Unverified point and No official source. They are
 * flags and a date, free for everyone, and they wrap on a narrow screen rather than cut a label. Nothing is drawn
 * for a card without a chip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GuideTrustChips(
    trust: GuideTrust,
    modifier: Modifier = Modifier,
) {
    if (!trust.hasAny) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSix),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSix),
    ) {
        trust.ratesAsOf?.let { TrustChip(GuideStrings.chipRatesAsOf(it)) }
        if (trust.amended) TrustChip(GuideStrings.chipAmended)
        if (trust.unverified) TrustChip(GuideStrings.chipUnverified, isWarning = true)
        if (trust.noOfficialSource) TrustChip(GuideStrings.chipNoOfficialSource)
    }
}

/** The line under the chips for a card still being checked, and the nudge for old rates; both in the Watch-out amber. */
@Composable
internal fun GuideTrustNotices(
    trust: GuideTrust,
    ratesStale: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (trust.unverified) Notice(GuideStrings.unverifiedWarning)
        if (ratesStale) trust.ratesAsOf?.let { Notice(GuideStrings.staleRatesNudge(it)) }
    }
}

@Composable
private fun TrustChip(
    text: String,
    isWarning: Boolean = false,
) {
    val warning = GuideColors.watchOut()
    val frame =
        if (isWarning) {
            Modifier.border(BorderStroke(AppDimensions.BorderThin, warning), ChipShape)
        } else {
            Modifier.background(MaterialTheme.colorScheme.surfaceVariant, ChipShape)
        }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (isWarning) warning else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = frame.padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTwo),
    )
}

@Composable
private fun Notice(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = GuideColors.watchOut())
}
