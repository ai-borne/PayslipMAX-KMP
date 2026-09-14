package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.payslipmax.pdfparser.ui.PayslipUiState
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GemmaModelStrings

/**
 * Modern, slim, non-blocking capsule banner for Tier 6 Gemma model background installation.
 * Conforms to Android & iOS guidelines:
 * - Gentle, non-alarming palette (surfaceVariant / secondaryContainer, never error red).
 * - Compact height with single-row layout and integrated progress indicator.
 * - Dismissible via [onDismiss] so users have full agency without stopping background download.
 * - Hides harmless developer sideload errors (Error -15).
 */
@Composable
fun BaseModelDownloadBanner(
    uiState: PayslipUiState,
    modifier: Modifier = Modifier,
    onResumeDownload: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (!isModelBannerVisible(uiState)) return

    val hasError = uiState.modelDownloadError != null && !uiState.modelDownloadError.contains("-15")
    val isPausedOrError = uiState.isWaitingForWifi || hasError
    val containerColor =
        if (isPausedOrError) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isPausedOrError) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = AppDimensions.PaddingMedium, vertical = AppDimensions.SpacingTiny),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        border = BorderStroke(AppDimensions.BorderThin, contentColor.copy(alpha = 0.12f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = AppDimensions.PaddingMedium, vertical = AppDimensions.SpacingSmall)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
                modifier = Modifier.fillMaxWidth(),
            ) {
                BannerHeader(uiState = uiState, hasError = hasError, contentColor = contentColor, modifier = Modifier.weight(1f))
                BannerActions(
                    uiState = uiState,
                    hasError = hasError,
                    contentColor = contentColor,
                    onResumeDownload = onResumeDownload,
                    onDismiss = onDismiss,
                )
            }
            if (uiState.isDownloadingModel) {
                LinearProgressIndicator(
                    progress = { uiState.modelDownloadProgress },
                    modifier = Modifier.fillMaxWidth().height(AppDimensions.SpacingTiny).padding(top = AppDimensions.SpacingTiny),
                )
            }
        }
    }
}

@Composable
private fun BannerHeader(
    uiState: PayslipUiState,
    hasError: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (uiState.isWaitingForWifi || hasError) Icons.Outlined.Info else Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier.size(AppDimensions.IconSizeMedium),
            tint = contentColor,
        )
        Column {
            Text(
                text = resolveBannerTitle(uiState, hasError),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
            Text(
                text = resolveBannerSubtitle(uiState, hasError),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun BannerActions(
    uiState: PayslipUiState,
    hasError: Boolean,
    contentColor: Color,
    onResumeDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        if (uiState.isWaitingForWifi || hasError) {
            FilledTonalButton(
                onClick = onResumeDownload,
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier = Modifier.height(AppDimensions.SpacingDouble),
            ) {
                Text(
                    text =
                        if (uiState.isWaitingForWifi) {
                            GemmaModelStrings.gemmaModelDownloadCellularAction
                        } else {
                            GemmaModelStrings.gemmaModelRetryAction
                        },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        IconButton(
            onClick = onDismiss,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = GemmaModelStrings.gemmaModelDismissAction,
                modifier = Modifier.size(AppDimensions.IconSizeSmall),
                tint = contentColor.copy(alpha = 0.7f),
            )
        }
    }
}

internal fun isModelBannerVisible(uiState: PayslipUiState): Boolean {
    val hasError = uiState.modelDownloadError != null && !uiState.modelDownloadError.contains("-15")
    return !uiState.isModelBannerDismissed && (uiState.isDownloadingModel || uiState.isWaitingForWifi || hasError)
}

internal fun resolveBannerTitle(
    uiState: PayslipUiState,
    hasError: Boolean,
): String =
    when {
        hasError -> GemmaModelStrings.gemmaModelPausedTitle
        uiState.isWaitingForWifi -> GemmaModelStrings.gemmaModelWaitingForWifiTitle
        uiState.modelDownloadProgress > 0f ->
            "${GemmaModelStrings.gemmaModelDownloadingTitle} (${(uiState.modelDownloadProgress * 100).toInt()}%)"
        else -> GemmaModelStrings.gemmaModelDownloadingTitle
    }

internal fun resolveBannerSubtitle(
    uiState: PayslipUiState,
    hasError: Boolean,
): String =
    when {
        hasError -> GemmaModelStrings.gemmaModelPausedSubtitle
        uiState.isWaitingForWifi -> GemmaModelStrings.gemmaModelWaitingForWifiSubtitle
        else -> GemmaModelStrings.gemmaModelDownloadBannerMessage
    }
