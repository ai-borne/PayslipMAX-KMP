package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings
import com.ssbmax.pdfparser.ui.theme.InsightsStrings

/** Returns the first non-header, non-JSON paragraph from AI insights, truncated to [maxLength]. */
internal fun extractAiSummary(
    insights: String,
    maxLength: Int = InsightsStrings.aiSummaryMaxLength,
): String {
    val first =
        insights.lines()
            .map { it.trim() }
            .firstOrNull {
                it.isNotEmpty() &&
                !it.startsWith("#") &&
                !it.startsWith("---") &&
                !it.startsWith("{") &&
                !it.startsWith("[") &&
                !it.startsWith("}") &&
                !it.startsWith("]") &&
                !it.startsWith("```") &&
                !it.startsWith("\"") &&
                !it.startsWith("Summary") &&
                !it.contains("\":") // skip JSON key-value lines
            }
            ?.removePrefix("- ")
            ?.removePrefix("* ")
            ?.trim()
            ?: return ""
    return if (first.length > maxLength) "${first.take(maxLength)}…" else first
}

@Composable
fun GeminiAiInsightsSection(
    aiInsights: String?,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GeminiAiInsightsActiveCard(
        aiInsights = aiInsights,
        isAiLoading = isAiLoading,
        aiError = aiError,
        onGenerateClick = onGenerateClick,
        onViewInsightsClick = onViewInsightsClick,
        onClearClick = onClearClick,
        modifier = modifier,
    )
}

@Composable
private fun GeminiAiInsightsActiveCard(
    aiInsights: String?,
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
                    if (isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    },
            ),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            ActiveHeader(hasInsights = aiInsights != null, onClearClick = onClearClick)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            ActiveContent(
                aiInsights = aiInsights,
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
            Text(AppStrings.proTeaserCrownIcon, fontSize = AppDimensions.TextSizeHuge, modifier = Modifier.padding(end = AppDimensions.SpacingSmall))
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
                Text(AppStrings.aiRegenerateIcon, fontSize = AppDimensions.TextSizeLarge)
            }
        }
    }
}

@Composable
private fun ActiveContent(
    aiInsights: String?,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onViewInsightsClick: () -> Unit,
) {
    when {
        isAiLoading -> AiLoadingIndicator()
        aiError != null -> {
            Text(text = aiError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(AppDimensions.SpacingMedium))
            OutlinedButton(onClick = onGenerateClick, modifier = Modifier.fillMaxWidth()) {
                Text(AppStrings.geminiAiAnalyzeBtn)
            }
        }
        aiInsights != null -> AiInsightsSummary(aiInsights = aiInsights, onViewClick = onViewInsightsClick)
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

@Composable
private fun AiLoadingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(AppDimensions.IconSizeSmall))
        Text(
            text = AppStrings.aiAuditAnalyzingDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiInsightsSummary(
    aiInsights: String,
    onViewClick: () -> Unit,
) {
    val summary = extractAiSummary(aiInsights)
    if (summary.isNotEmpty()) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
    }
    Button(onClick = onViewClick, modifier = Modifier.fillMaxWidth()) {
        Text(AppStrings.geminiAiViewReportBtn)
    }
}
