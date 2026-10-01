package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppColors
import com.payslipmax.pdfparser.ui.theme.AppDimensions

private const val TONE_FILL_ALPHA = 0.12f

@Composable
private fun verdictToneColor(tone: VerdictTone): Color =
    when (tone) {
        VerdictTone.OK -> MaterialTheme.colorScheme.secondary
        VerdictTone.ISSUE -> MaterialTheme.colorScheme.error
        VerdictTone.WAITING -> MaterialTheme.colorScheme.tertiary
        VerdictTone.LOCKED -> AppColors.Warning
        VerdictTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/**
 * The one-glance answer at the top of Pay Audit (docs/Plan Phase 2 step 0): correct, issue (pay line and
 * rupee amount) or waiting. [onAction] is the card's single call to action, when its tone has one.
 */
@Composable
fun PayAuditVerdictCard(
    copy: VerdictCopy,
    historyLine: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = verdictToneColor(copy.tone)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        color = tone.copy(alpha = TONE_FILL_ALPHA),
        border = BorderStroke(AppDimensions.BorderThin, tone),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSix),
        ) {
            Text(text = copy.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = tone)
            Text(text = copy.headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = copy.subtitle, style = MaterialTheme.typography.bodyMedium)
            copy.cta?.let { Button(onClick = onAction) { Text(it) } }
            if (historyLine != null) {
                HorizontalDivider(color = tone.copy(alpha = 0.3f))
                Text(text = historyLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
