package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun GeminiAiInsightsSection(
    payslip: ParsedPayslip,
    isPremiumEnabled: Boolean,
    hasInsights: Boolean,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onClearClick: () -> Unit,
    onUpgradeClick: () -> Unit,
) {
    if (isPremiumEnabled) {
        GeminiAiInsightsActiveCard(
            hasInsights = hasInsights,
            isAiLoading = isAiLoading,
            aiError = aiError,
            onGenerateClick = onGenerateClick,
            onViewInsightsClick = onViewInsightsClick,
            onClearClick = onClearClick,
        )
    } else {
        GeminiAiInsightsLockedCard(onUpgradeClick = onUpgradeClick)
    }
}

@Composable
private fun GeminiAiInsightsLockedCard(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTen),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            ) {
                Text("👑", fontSize = AppDimensions.TextSizeHuge)
                Text(
                    text = AppStrings.settingsAiInsightsLockedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = AppStrings.settingsAiInsightsLockedDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.SpacingTiny),
            )
            Button(onClick = onUpgradeClick, modifier = Modifier.fillMaxWidth()) {
                Text(AppStrings.settingsAiInsightsLockedBtn)
            }
        }
    }
}

@Composable
private fun GeminiAiInsightsActiveCard(
    hasInsights: Boolean,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (androidx.compose.foundation.isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    },
            ),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            ActiveHeader(hasInsights = hasInsights, onClearClick = onClearClick)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            ActiveContent(
                hasInsights = hasInsights,
                isAiLoading = isAiLoading,
                aiError = aiError,
                onGenerateClick = onGenerateClick,
                onViewInsightsClick = onViewInsightsClick,
            )
        }
    }
}

@Composable
private fun ActiveHeader(
    hasInsights: Boolean,
    onClearClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👑", fontSize = AppDimensions.TextSizeHuge, modifier = Modifier.padding(end = AppDimensions.SpacingSmall))
            Column {
                Text(
                    text = AppStrings.settingsAiInsightsLockedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = AppStrings.settingsAiPoweredByProxy,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (hasInsights) {
            IconButton(onClick = onClearClick, modifier = Modifier.size(AppDimensions.IconSizeMedium)) {
                Text("🔄", fontSize = AppDimensions.TextSizeLarge)
            }
        }
    }
}

@Composable
private fun ActiveContent(
    hasInsights: Boolean,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onViewInsightsClick: () -> Unit,
) {
    when {
        isAiLoading -> {
            Box(modifier = Modifier.fillMaxWidth().height(AppDimensions.IconSizeHuge), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        aiError != null -> {
            Text(text = aiError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        hasInsights -> {
            Text(
                text = AppStrings.geminiAiReportReadyDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            Button(onClick = onViewInsightsClick, modifier = Modifier.fillMaxWidth()) {
                Text(AppStrings.geminiAiViewReportBtn)
            }
        }
        else -> {
            Text(
                text = AppStrings.geminiAiAnalyzeDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            Button(onClick = onGenerateClick, modifier = Modifier.fillMaxWidth()) {
                Text(AppStrings.geminiAiAnalyzeBtn)
            }
        }
    }
}
