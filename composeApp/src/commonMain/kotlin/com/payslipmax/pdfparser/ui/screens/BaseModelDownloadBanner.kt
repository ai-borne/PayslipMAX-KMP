package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.PayslipUiState
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GemmaModelStrings

/**
 * Persistent, non-blocking status for the Tier 6 base model's mandatory background install —
 * unconditional and independent of the "Use Local Gemma AI Model" toggle, which now only controls
 * AI Insights source. Purely informational: on Android, Play's own consent dialogs
 * (`showConfirmationDialog`/`showCellularDataConfirmation`) are the sole permission ask; this banner
 * never duplicates that prompt. Disappears on success; shows a compact error state on failure.
 * Tiers 1–5 keep running underneath regardless of this banner's state.
 */
@Composable
fun BaseModelDownloadBanner(
    uiState: PayslipUiState,
    modifier: Modifier = Modifier,
) {
    val isVisible = uiState.isDownloadingModel || uiState.modelDownloadError != null
    if (!isVisible) return

    val isError = uiState.modelDownloadError != null
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = uiState.modelDownloadError ?: GemmaModelStrings.gemmaModelDownloadBannerMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (uiState.isDownloadingModel) {
                LinearProgressIndicator(
                    progress = { uiState.modelDownloadProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingTiny),
                )
            }
            Text(
                text = "${GemmaModelStrings.gemmaLicenseNoticeTitle}: ${GemmaModelStrings.gemmaTermsOfUseNotice}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = AppDimensions.SpacingTiny),
            )
        }
    }
}
