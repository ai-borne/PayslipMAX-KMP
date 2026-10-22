package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.hasAccess
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings

/**
 * The payslip view's free leave section for one statement's [table], with the Premium teaser. The gate is read on tap, as
 * [InsightCard] does: Premium opens Retirement Calculators through [onNavigateTo], anyone else gets the upgrade sheet.
 */
@Composable
internal fun PayslipLeaveSection(
    viewModel: PayslipViewModel,
    table: LeaveAccumulation,
    isEditing: Boolean,
    onNavigateTo: (Screen) -> Unit,
) {
    val summary by viewModel.leaveBankSummary.collectAsState()
    var showUpgradeSheet by remember { mutableStateOf(false) }
    LeaveAccumulationSection(
        ui = leaveSectionUi(table, summary, isEditing),
        onTeaserClick = { openLeaveBankFromTeaser(viewModel::hasAccess, onNavigateTo, onLocked = { showUpgradeSheet = true }) },
    )
    if (showUpgradeSheet) PayslipUpgradeSheet(viewModel = viewModel, onDismiss = { showUpgradeSheet = false })
}

/** Collapsed by default; the teaser stays visible below the header so the value is seen without opening the table. */
@Composable
fun LeaveAccumulationSection(
    ui: LeaveSectionUi,
    onTeaserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, tint = CardTint.Accent) {
        ExpandableDetailSection(title = LeaveBankStrings.sectionTitle, subtitle = ui.subtitle) {
            LeaveAccumulationTable(rows = ui.rows)
        }
        ui.teaser?.let { TeaserRow(text = it, onClick = onTeaserClick) }
    }
}

@Composable
private fun TeaserRow(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = AppDimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** The teaser's target and its gate both come from the Premium catalog (SSOT), so the payslip can't drift from Insights. */
internal fun openLeaveBankFromTeaser(
    hasAccess: (FeatureGate) -> Boolean,
    onNavigate: (Screen) -> Unit,
    onLocked: () -> Unit,
) {
    val target = Screen.RetirementCalculators
    val gate = gateForScreen(target)
    if (gate == null || hasAccess(gate)) onNavigate(target) else onLocked()
}
