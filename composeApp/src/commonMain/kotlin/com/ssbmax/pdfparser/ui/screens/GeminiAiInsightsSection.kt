package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions

@Composable
fun GeminiAiInsightsSection(
    payslip: ParsedPayslip,
    isPremiumEnabled: Boolean,
    aiInsights: String?,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    if (!isPremiumEnabled) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "AI Chartered Accountant Audit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (aiInsights != null) {
                    IconButton(onClick = onClearClick, modifier = Modifier.size(24.dp)) {
                        Text("🔄", fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            when {
                isAiLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                aiError != null -> {
                    Text(
                        text = aiError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                aiInsights != null -> {
                    Text(
                        text = aiInsights,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                else -> {
                    Text(
                        text = "Generate professional tax saving suggestions, investment recommendations, and error audits using Gemini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onGenerateClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Analyze Payslip with Gemini AI")
                    }
                }
            }
        }
    }
}
