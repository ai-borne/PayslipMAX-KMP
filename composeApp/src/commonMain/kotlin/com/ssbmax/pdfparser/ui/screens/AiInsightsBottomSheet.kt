package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.ssbmax.pdfparser.insights.*
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInsightsBottomSheet(
    aiInsights: String,
    onDismissRequest: () -> Unit,
    onRegenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxHeight(0.85f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimensions.PaddingMedium)
                    .padding(bottom = AppDimensions.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            HeaderRow(onDismissRequest = onDismissRequest, onRegenerateClick = onRegenerateClick)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ReportContent(aiInsights = aiInsights)
        }
    }
}

@Composable
private fun HeaderRow(
    onDismissRequest: () -> Unit,
    onRegenerateClick: () -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRegenerateClick, modifier = Modifier.size(AppDimensions.IconSizeMedium)) {
                Text("🔄", fontSize = AppDimensions.TextSizeLarge)
            }
            Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
            IconButton(onClick = onDismissRequest, modifier = Modifier.size(AppDimensions.IconSizeMedium)) {
                Text("✕", fontSize = AppDimensions.TextSizeLarge)
            }
        }
    }
}

@Composable
private fun ColumnScope.ReportContent(
    aiInsights: String,
) {
    val scrollState = rememberScrollState()
    val parsedReport = remember(aiInsights) { AiInsightReportParser.parse(aiInsights) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
    ) {
        if (parsedReport != null) {
            ParsedReportContent(parsedReport)
        } else {
            LegacyReportContent(aiInsights)
        }
    }
}

@Composable
private fun LegacyReportContent(aiInsights: String) {
    val annotatedText = parseMarkdown(aiInsights)
    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            when {
                // Skip JSON/code artifacts
                trimmed.startsWith("{") || trimmed.startsWith("}") ||
                trimmed.startsWith("[") || trimmed.startsWith("]") ||
                trimmed.startsWith("\"") && trimmed.contains(":") ||
                trimmed.startsWith("Summary") ||
                trimmed.isEmpty() -> {
                    // Skip these lines entirely
                }
                line.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = AppDimensions.TextSizeHuge)) {
                        append(line.substring(2))
                    }
                }
                line.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = AppDimensions.TextSizeExtraLarge)) {
                        append(line.substring(3))
                    }
                }
                line.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = AppDimensions.TextSizeLarge)) {
                        append(line.substring(4))
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    append("•  ")
                    parseInlineStyle(line.substring(2))
                }
                else -> {
                    parseInlineStyle(line)
                }
            }
            if (index < lines.lastIndex && trimmed.isNotEmpty()) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.parseInlineStyle(text: String) {
    var currentIndex = 0
    while (currentIndex < text.length) {
        val startBold = text.indexOf("**", currentIndex)
        if (startBold == -1) {
            append(text.substring(currentIndex))
            break
        }
        append(text.substring(currentIndex, startBold))
        val endBold = text.indexOf("**", startBold + 2)
        if (endBold == -1) {
            append(text.substring(startBold))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(startBold + 2, endBold))
        }
        currentIndex = endBold + 2
    }
}
