package com.payslipmax.pdfparser.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.payslipmax.pdfparser.ui.theme.AppColors
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

enum class HealthStatus(val label: String) {
    EXCELLENT(InsightsStrings.healthStatusExcellent),
    HEALTHY(InsightsStrings.healthStatusHealthy),
    FAIR(InsightsStrings.healthStatusFair),
    NEEDS_ATTENTION(InsightsStrings.healthStatusNeedsAttention),
    CRITICAL(InsightsStrings.healthStatusCritical),
}

fun statusFor(score: Int): HealthStatus =
    when {
        score >= 90 -> HealthStatus.EXCELLENT
        score >= 75 -> HealthStatus.HEALTHY
        score >= 60 -> HealthStatus.FAIR
        score >= 40 -> HealthStatus.NEEDS_ATTENTION
        else -> HealthStatus.CRITICAL
    }

@Composable
fun statusColor(status: HealthStatus): Color =
    when (status) {
        HealthStatus.EXCELLENT -> MaterialTheme.colorScheme.primary
        HealthStatus.HEALTHY -> MaterialTheme.colorScheme.secondary
        HealthStatus.FAIR -> AppColors.Warning
        HealthStatus.NEEDS_ATTENTION -> AppColors.Caution
        HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error
    }
