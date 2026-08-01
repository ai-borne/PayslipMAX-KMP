package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun StatementSourceFooterCard(
    payslip: ParsedPayslip,
    onViewPdfClick: (String) -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            StatementCardHeader(fileName = payslip.file)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            StatementCardActionRow(
                onViewPdfClick = { onViewPdfClick(payslip.dateStr) },
                onShareClick = onShareClick,
            )
        }
    }
}

@Composable
private fun StatementCardHeader(
    fileName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        Box(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(AppDimensions.SpacingTiny / 2))
                    .padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTiny),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = AppStrings.pdfCardBadge,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = AppStrings.pdfCardTapHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatementCardActionRow(
    onViewPdfClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onViewPdfClick,
            modifier = Modifier.weight(1f).heightIn(min = AppDimensions.TouchTargetMinHeight),
            shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        ) {
            Text(
                text = AppStrings.historyActionViewOriginal,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        OutlinedButton(
            onClick = onShareClick,
            modifier = Modifier.heightIn(min = AppDimensions.TouchTargetMinHeight),
            shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = AppStrings.historyActionShareSummary,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = AppStrings.pdfCardOpenCdesc,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
