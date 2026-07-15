package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

fun calculateMomChanges(
    current: LedgerRecordEntity,
    previous: LedgerRecordEntity?,
): List<MoMChange> {
    val list = mutableListOf<MoMChange>()
    if (previous != null) {
        if (current.basicPay != previous.basicPay) {
            list.add(MoMChange("Basic Pay", previous.basicPay, current.basicPay, true))
        }
        if (current.dearnessAllowance != previous.dearnessAllowance) {
            list.add(MoMChange("Dearness Allowance", previous.dearnessAllowance, current.dearnessAllowance, true))
        }
        if (current.militaryServicePay != previous.militaryServicePay) {
            list.add(MoMChange("Military Service Pay", previous.militaryServicePay, current.militaryServicePay, true))
        }
        if (current.transportAllowance != previous.transportAllowance) {
            list.add(MoMChange("Transport Allowance", previous.transportAllowance, current.transportAllowance, true))
        }
        if (current.houseRentAllowance != previous.houseRentAllowance) {
            list.add(MoMChange("House Rent Allowance", previous.houseRentAllowance, current.houseRentAllowance, true))
        }
        if (current.dsopSubscription != previous.dsopSubscription) {
            list.add(MoMChange("DSOP Subscription", previous.dsopSubscription, current.dsopSubscription, false))
        }
        if (current.incomeTax != previous.incomeTax) {
            list.add(MoMChange("Income Tax", previous.incomeTax, current.incomeTax, false))
        }
    }
    return list
}

@Composable
fun MomChangesGrid(
    current: LedgerRecordEntity,
    previous: LedgerRecordEntity?,
    modifier: Modifier = Modifier,
) {
    val changes = remember(current, previous) { calculateMomChanges(current, previous) }

    if (changes.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.padding(AppDimensions.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            ) {
                Text(
                    text = AppStrings.changesThisMonthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                changes.forEach { change ->
                    MomChangeRow(change = change)
                }
            }
        }
    }
}

@Composable
private fun MomChangeRow(change: MoMChange) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = change.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = formatCurrency(change.prevValue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatCurrency(change.currValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
