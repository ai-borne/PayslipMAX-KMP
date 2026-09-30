package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.reconciliation.SituationalCategory

/**
 * Horizontal scrollable category tab bar for the Situational Matrix.
 * Displays 6 military operational categories with unobscured smooth scrolling.
 */
@Composable
fun SituationalCategoryTabBar(
    selectedCategory: SituationalCategory,
    onCategorySelected: (SituationalCategory) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val categories = SituationalCategory.values()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { cat ->
            CategoryTabChip(
                category = cat,
                isSelected = selectedCategory == cat,
                onClick = { onCategorySelected(cat) },
            )
        }
    }
}

@Composable
private fun CategoryTabChip(
    category: SituationalCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val title =
        when (category) {
            SituationalCategory.POSTING_OPS -> AppStringsPcdao.tabPostingOps
            SituationalCategory.HOUSING_TLC -> AppStringsPcdao.tabHousingTlc
            SituationalCategory.CHILDREN_CEA -> AppStringsPcdao.tabChildrenCea
            SituationalCategory.DUTY_COURSES_LEAVE -> AppStringsPcdao.tabDutyLeave
            SituationalCategory.CAREER_CADRES -> AppStringsPcdao.tabCareerCadres
            SituationalCategory.FUNDS_RETIREMENT -> AppStringsPcdao.tabFundsRetirement
        }
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
        )
    }
}
