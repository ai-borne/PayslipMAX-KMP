package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.engine.FixationOption
import com.payslipmax.pcdao.engine.PayFixationResult
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCard
import com.payslipmax.pdfparser.ui.screens.formatCurrency

/**
 * Interactive Army Pay Rules 2017 (Rule 10 & 11) Pay Fixation Optimizer Card.
 * Dynamically rendered when substantive promotion is selected.
 */
@Composable
fun PayFixationCard(
    result: PayFixationResult,
    modifier: Modifier = Modifier,
    isUnlocked: Boolean = true,
    onUpgradeClick: () -> Unit = {},
) {
    FlatBorderedCard(
        modifier = modifier.fillMaxWidth(),
        tint = CardTint.Accent,
        contentSpacing = 8.dp,
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            FixationHeader()
            Text(
                text = "${AppStringsPcdao.fixationPromotionPrompt} Level ${result.fromLevel} → Level ${result.toLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            FixationComparisonGrid(
                result = result,
                isUnlocked = isUnlocked,
                onUpgradeClick = onUpgradeClick,
            )
            FixationUrgentCallout()
        }
    }
}

@Composable
private fun FixationHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = AppStringsPcdao.fixationCardTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .background(Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = AppStringsPcdao.fixationElectionBadge,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC7D2FE),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FixationComparisonGrid(
    result: PayFixationResult,
    isUnlocked: Boolean,
    onUpgradeClick: () -> Unit,
) {
    val isOpt2Winner = result.recommendedOption == FixationOption.OPTION_2
    val opt1Total =
        if (isUnlocked) {
            "${AppStringsPcdao.fixation36MonthPrefix}${formatCurrency(result.opt1Total36Months.toDouble())}"
        } else {
            AppStringsPcdao.fixationOptionLocked
        }
    val opt2Total =
        if (isUnlocked) {
            "${AppStringsPcdao.fixation36MonthPrefix}${formatCurrency(result.opt2Total36Months.toDouble())}"
        } else {
            AppStringsPcdao.fixationOptionLocked
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FixationOptionBox(
            title = AppStringsPcdao.fixationOpt1Title,
            payValue = "${formatCurrency(result.opt1FixedPay.toDouble())}/mo",
            total36Mo = opt1Total,
            isWinner = !isOpt2Winner,
            winnerBadge = null,
            isUnlocked = isUnlocked,
            onUpgradeClick = onUpgradeClick,
            modifier = Modifier.weight(1f),
        )
        FixationOptionBox(
            title = AppStringsPcdao.fixationOpt2Title,
            payValue = "${formatCurrency(result.opt2PostDniFixedPay.toDouble())}/mo",
            total36Mo = opt2Total,
            isWinner = isOpt2Winner,
            winnerBadge = "${AppStringsPcdao.fixationRecommendedBadge} (+${formatCurrency(result.cumulativeDelta.toDouble())})",
            isUnlocked = isUnlocked,
            onUpgradeClick = onUpgradeClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FixationOptionBox(
    title: String,
    payValue: String,
    total36Mo: String,
    isWinner: Boolean,
    winnerBadge: String?,
    isUnlocked: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isWinner) Color(0xFF34D399) else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isWinner) Color(0xFF34D399).copy(alpha = 0.08f) else Color.Transparent

    Column(
        modifier =
            modifier
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
                .background(bgColor, RoundedCornerShape(8.dp))
                .then(if (!isUnlocked) Modifier.clickable(onClick = onUpgradeClick) else Modifier)
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (winnerBadge != null) {
            Text(
                text = winnerBadge,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = payValue,
            style = MaterialTheme.typography.titleMedium,
            color = if (isWinner) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = total36Mo,
            style = MaterialTheme.typography.bodySmall,
            color = if (!isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (!isUnlocked) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun FixationUrgentCallout() {
    Text(
        text = AppStringsPcdao.fixationUrgentCallout,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFFBBF24),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 4.dp),
    )
}
