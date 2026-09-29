package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.timeline.CareerMilestone
import com.payslipmax.pcdao.timeline.MilestoneType
import com.payslipmax.pdfparser.ui.theme.AppDimensions

@Composable
fun CareerMilestonesCard(
    milestones: List<CareerMilestone>,
    modifier: Modifier = Modifier,
) {
    if (milestones.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    val displayedMilestones = if (isExpanded || milestones.size <= 1) milestones else milestones.take(1)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
                )
                .padding(AppDimensions.PaddingMedium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            MilestoneCardHeader()

            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                displayedMilestones.forEach { milestone ->
                    MilestoneRow(milestone = milestone)
                }
            }

            if (milestones.size > 1) {
                MilestoneExpandToggle(
                    isExpanded = isExpanded,
                    totalCount = milestones.size,
                    onToggle = { isExpanded = !isExpanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun MilestoneCardHeader() {
    Text(
        text = AppStringsPcdao.careerMilestoneTitle,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = AppStringsPcdao.careerMilestoneSubtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MilestoneExpandToggle(
    isExpanded: Boolean,
    totalCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onToggle,
        modifier = modifier,
    ) {
        Text(
            text =
                if (isExpanded) {
                    AppStringsPcdao.milestoneShowLess
                } else {
                    AppStringsPcdao.formatMilestoneShowAll(totalCount)
                },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MilestoneRow(
    milestone: CareerMilestone,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusSmall))
                .background(MaterialTheme.colorScheme.surface)
                .padding(AppDimensions.PaddingSmall),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = milestoneIcon(milestone.type),
            style = MaterialTheme.typography.titleMedium,
        )
        MilestoneContent(
            title = milestone.title,
            description = milestone.description,
            modifier = Modifier.weight(1f),
        )
        MilestoneBadge(isAlert = milestone.isAlert)
    }
}

@Composable
private fun MilestoneContent(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MilestoneBadge(
    isAlert: Boolean,
    modifier: Modifier = Modifier,
) {
    val badgeColor = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val badgeTextColor = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    val badgeLabel = if (isAlert) AppStringsPcdao.milestoneAlertBadge else AppStringsPcdao.milestoneVerifiedBadge

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = badgeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = badgeTextColor,
        )
    }
}

private fun milestoneIcon(type: MilestoneType): String =
    when (type) {
        MilestoneType.ANNUAL_INCREMENT_VERIFIED -> "✅"
        MilestoneType.ANNUAL_INCREMENT_MISSING -> "🚨"
        MilestoneType.DA_REVISION_CREDITED -> "📈"
        MilestoneType.DA_ARREARS_SPIKE -> "💰"
    }
