package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.ui.theme.AppDimensions

@Composable
fun DsopSimulatorSection(
    initialContribution: Double,
    modifier: Modifier = Modifier,
) {
    var monthlyContribution by remember { mutableStateOf(initialContribution.coerceIn(6000.0, 100000.0).toFloat()) }
    val annualContribution = monthlyContribution * 12
    val exceedsLimit = annualContribution > 500000f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = "DSOP Compound Simulator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Monthly Subscription",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "₹${formatAmount(monthlyContribution.toDouble())}/mo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
            }

            Slider(
                value = monthlyContribution,
                onValueChange = { monthlyContribution = it },
                valueRange = 6000f..100000f,
                steps = 94,
                colors =
                    SliderDefaults.colors(
                        thumbColor = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        activeTrackColor = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (exceedsLimit) {
                TaxWarningTooltip()
            } else {
                TaxSafeTooltip()
            }

            Spacer(modifier = Modifier.height(12.dp))
            ProjectionRow(monthlyContribution.toDouble())
        }
    }
}

@Composable
private fun TaxWarningTooltip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚠️", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = "Tax Warning: Annual DSOP contributions above ₹5 Lakhs (₹41,666/mo) attract income tax on interest earned. Stay below ₹41,666/mo to keep gains 100% tax-free.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TaxSafeTooltip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✅", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = "Tax optimized: Annual contribution is below ₹5 Lakhs. All DSOP interest remains 100% tax-free under Section 10(11) of the Income Tax Act.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ProjectionRow(monthly: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val calculated5 = remember(monthly) { calculateProjectedSavings(monthly, 5) }
        val calculated10 = remember(monthly) { calculateProjectedSavings(monthly, 10) }
        val calculated15 = remember(monthly) { calculateProjectedSavings(monthly, 15) }

        ProjectionCard("5 Years", calculated5, Modifier.weight(1f))
        ProjectionCard("10 Years", calculated10, Modifier.weight(1f))
        ProjectionCard("15 Years", calculated15, Modifier.weight(1f))
    }
}

@Composable
private fun ProjectionCard(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "₹${formatShortAmount(value)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun calculateProjectedSavings(
    monthly: Double,
    years: Int,
    rate: Double = 0.071,
): Double {
    var balance = 0.0
    val annualContribution = monthly * 12.0
    for (i in 1..years) {
        balance = (balance + annualContribution) * (1.0 + rate)
    }
    return balance
}

private fun formatShortAmount(value: Double): String {
    return when {
        value >= 10000000.0 -> "${(value / 10000000.0).toString().take(4)} Cr"
        value >= 100000.0 -> "${(value / 100000.0).toString().take(4)} L"
        else -> value.toLong().toString()
    }
}
