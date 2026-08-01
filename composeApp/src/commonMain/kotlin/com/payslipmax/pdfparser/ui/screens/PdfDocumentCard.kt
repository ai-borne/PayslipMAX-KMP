package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

fun formatPdfSourceChipLabel(fileName: String): String =
    fileName.ifBlank { "Statement.pdf" }

@Composable
fun PdfSourceChip(
    fileName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayLabel = formatPdfSourceChipLabel(fileName)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingTiny),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(AppDimensions.SpacingTiny / 2))
                        .padding(horizontal = AppDimensions.SpacingTiny, vertical = AppDimensions.BorderThin),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = AppStrings.pdfCardBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = AppStrings.pdfCardOpenCdesc,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppDimensions.IconSizeSmall),
            )
        }
    }
}

@Composable
fun PdfDocumentCard(
    payslip: ParsedPayslip,
    modifier: Modifier = Modifier,
    onViewPdfClick: (String) -> Unit,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onViewPdfClick(payslip.dateStr) },
        shape = RoundedCornerShape(AppDimensions.PaddingSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PdfIconBox()
            Spacer(modifier = Modifier.width(AppDimensions.SpacingLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payslip.file,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = AppStrings.pdfCardTapHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = AppStrings.pdfCardOpenCdesc,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PdfIconBox(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(AppDimensions.IconSizeExtraLarge)
                .background(MaterialTheme.colorScheme.error, RoundedCornerShape(AppDimensions.SpacingTiny)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AppStrings.pdfCardBadge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = AppDimensions.TextSizeMedium,
        )
    }
}
