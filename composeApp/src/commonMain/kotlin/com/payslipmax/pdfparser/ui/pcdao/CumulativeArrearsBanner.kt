package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.timeline.CumulativeArrearsRollup
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import kotlin.math.roundToLong

@Composable
fun CumulativeArrearsBanner(
    rollup: CumulativeArrearsRollup,
    isCumulativeActive: Boolean,
    isUnlocked: Boolean,
    onToggleView: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusMedium))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
                )
                .padding(AppDimensions.PaddingMedium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            CumulativeBannerHeader(
                isCumulativeActive = isCumulativeActive,
                onToggleView = onToggleView,
            )
            CumulativeBannerAmountRow(
                rollup = rollup,
                isUnlocked = isUnlocked,
                onUpgradeClick = onUpgradeClick,
            )
        }
    }
}

@Composable
private fun CumulativeBannerHeader(
    isCumulativeActive: Boolean,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = AppStringsPcdao.cumulativeBannerTitle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
        )
        CumulativeViewToggleChip(
            isCumulativeActive = isCumulativeActive,
            onClick = onToggleView,
        )
    }
}

@Composable
private fun CumulativeBannerAmountRow(
    rollup: CumulativeArrearsRollup,
    isUnlocked: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalFormatted = rollup.totalUnderpaidArrears.roundToLong().toString()
    val displayAmount = if (isUnlocked) "₹$totalFormatted" else AppStringsPcdao.maskedAmount

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(
                text = displayAmount,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val subText =
                "${AppStringsPcdao.cumulativeMonthPrefix}${rollup.auditedMonthCount}" +
                    "${AppStringsPcdao.cumulativeMonthSuffix} (${rollup.startMonthDateStr} – ${rollup.endMonthDateStr})"
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!isUnlocked) {
            Text(
                text = AppStringsPcdao.proBadge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onUpgradeClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CumulativeViewToggleChip(
    isCumulativeActive: Boolean,
    onClick: () -> Unit,
) {
    val label = if (isCumulativeActive) AppStringsPcdao.cumulativeViewToggleAll else AppStringsPcdao.cumulativeViewToggleSelected
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusSmall))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
