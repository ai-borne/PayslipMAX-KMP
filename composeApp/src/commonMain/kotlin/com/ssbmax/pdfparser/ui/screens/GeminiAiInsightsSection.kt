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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun GeminiAiInsightsSection(
    payslip: ParsedPayslip,
    isPremiumEnabled: Boolean,
    aiInsights: String?,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onClearClick: () -> Unit,
    onUpgradeClick: () -> Unit,
) {
    if (isPremiumEnabled) {
        GeminiAiInsightsActiveCard(
            aiInsights = aiInsights,
            isAiLoading = isAiLoading,
            aiError = aiError,
            onGenerateClick = onGenerateClick,
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
    aiInsights: String?,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors =
            CardDefaults.cardColors(
                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                },
            ),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            ActiveHeader(aiInsights = aiInsights, onClearClick = onClearClick)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            ActiveContent(
                aiInsights = aiInsights,
                isAiLoading = isAiLoading,
                aiError = aiError,
                onGenerateClick = onGenerateClick,
            )
        }
    }
}

@Composable
private fun ActiveHeader(
    aiInsights: String?,
    onClearClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👑", fontSize = AppDimensions.TextSizeHuge, modifier = Modifier.padding(end = AppDimensions.SpacingSmall))
            Text(
                text = AppStrings.settingsAiInsightsLockedTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (aiInsights != null) {
            IconButton(onClick = onClearClick, modifier = Modifier.size(AppDimensions.IconSizeMedium)) {
                Text("🔄", fontSize = AppDimensions.TextSizeLarge)
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
        aiInsights != null -> {
            Text(text = aiInsights, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
