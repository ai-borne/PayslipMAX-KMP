package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun WellnessMeterSection(
    payslip: ParsedPayslip,
    modifier: Modifier = Modifier,
) {
    val gross = payslip.summary.grossPay
    val savings = payslip.deductions.dsopSubscription + payslip.deductions.agif
    val rate = if (gross > 0) (savings / gross) else 0.0
    val ratePercent = (rate * 100.0).coerceIn(0.0, 100.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WellnessTextColumn(ratePercent)
            WellnessIndicator(ratePercent.toFloat() / 100f)
        }
    }
}

@Composable
private fun RowScope.WellnessTextColumn(ratePercent: Double) {
    Column(modifier = Modifier.weight(1f).padding(end = AppDimensions.SpacingLarge)) {
        Text(
            text = AppStrings.insightsSavingsRateTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = AppStrings.insightsSavingsRateTarget + ratePercent.toString().take(4) + AppStrings.insightsSavingsRateSuffix,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WellnessIndicator(progress: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(AppDimensions.IconSizeHuge)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.secondary,
            strokeWidth = AppDimensions.SpacingSix,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
