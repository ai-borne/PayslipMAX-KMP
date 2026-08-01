package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun BoxScope.ReplicaActionDock(
    onViewPdfClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(AppDimensions.PaddingMedium),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.CardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.PaddingSmall),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewPdfDockButton(onViewPdfClick = onViewPdfClick, modifier = Modifier.weight(1f))
            ShareDockButton(onShareClick = onShareClick)
        }
    }
}

@Composable
private fun ViewPdfDockButton(
    onViewPdfClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onViewPdfClick,
        modifier = modifier.heightIn(min = AppDimensions.TouchTargetMinHeight),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
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
                text = AppStrings.historyActionViewOriginal,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ShareDockButton(
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onShareClick,
        modifier = modifier.heightIn(min = AppDimensions.TouchTargetMinHeight),
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
