package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.domain.ParsedPayslip

@Composable
internal fun TopNavBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column {
            Text(
                text = AppStringsPcdao.screenTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = AppStringsPcdao.screenSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MonthSelectorRow(
    availablePayslips: List<ParsedPayslip>,
    selectedPayslip: ParsedPayslip?,
    onSelect: (ParsedPayslip) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentMonthText = selectedPayslip?.let { "${it.monthName} ${it.year}" } ?: "Select Month"

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${AppStringsPcdao.vaultMonthLabel}$currentMonthText",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (availablePayslips.size > 1) {
            MonthSelectorDropdown(
                expanded = expanded,
                availablePayslips = availablePayslips,
                onDismiss = { expanded = false },
                onExpand = { expanded = true },
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun MonthSelectorDropdown(
    expanded: Boolean,
    availablePayslips: List<ParsedPayslip>,
    onDismiss: () -> Unit,
    onExpand: () -> Unit,
    onSelect: (ParsedPayslip) -> Unit,
) {
    Box {
        Text(
            text = "▼ Switch Month",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onExpand() }.padding(4.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            availablePayslips.forEach { payslip ->
                DropdownMenuItem(
                    text = { Text("${payslip.monthName} ${payslip.year}") },
                    onClick = {
                        onSelect(payslip)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
internal fun FeedHeaderSection(
    selectedFilter: FindingFilter,
    onFilterSelected: (FindingFilter) -> Unit,
    onRedressalClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = AppStringsPcdao.feedHeaderTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onRedressalClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = AppStringsPcdao.btnRedressalKit,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        FilterBar(selectedFilter = selectedFilter, onFilterSelected = onFilterSelected)
    }
}

@Composable
private fun FilterBar(
    selectedFilter: FindingFilter,
    onFilterSelected: (FindingFilter) -> Unit,
) {
    val filters =
        listOf(
            FindingFilter.ALL to AppStringsPcdao.filterAll,
            FindingFilter.ENTITLEMENTS to AppStringsPcdao.filterEntitlements,
            FindingFilter.HAZARDS to AppStringsPcdao.filterHazards,
            FindingFilter.ALARMS to AppStringsPcdao.filterAlarms,
            FindingFilter.TAX_SHIELD to AppStringsPcdao.filterShield,
        )
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        filters.forEach { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

@Composable
internal fun EmptyFindingsState() {
    EmptyPlaceholder(
        icon = "🛡️",
        title = AppStringsPcdao.feedEmptyTitle,
        message = AppStringsPcdao.feedEmptyMessage,
    )
}

@Composable
internal fun EmptyVaultState() {
    EmptyPlaceholder(
        icon = "📁",
        title = AppStringsPcdao.vaultEmptyTitle,
        message = AppStringsPcdao.vaultEmptyMessage,
        padding = 32.dp,
    )
}

@Composable
private fun EmptyPlaceholder(
    icon: String,
    title: String,
    message: String,
    padding: Dp = 24.dp,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$icon $title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
