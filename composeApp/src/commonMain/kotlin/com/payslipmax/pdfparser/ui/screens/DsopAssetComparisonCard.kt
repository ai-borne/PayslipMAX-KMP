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
import com.payslipmax.pdfparser.insights.ProjectionMath
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

internal data class AssetProjectionResult(
    val assetName: String,
    val returnRateLabel: String,
    val grossBalance: Double,
    val netTaxFreeBalance: Double,
    val taxDeduction: Double,
    val isTaxFree: Boolean,
)

internal fun calculateAssetProjections(
    initialBalance: Double,
    monthlyContribution: Double,
    years: Int = 15,
): List<AssetProjectionResult> {
    val dsopResult = ProjectionMath.calculateProjection(initialBalance, monthlyContribution, years).projectedBalance

    val equityGross = calculateCompoundValue(initialBalance, monthlyContribution, years, 0.12)
    val equityInvested = initialBalance + (monthlyContribution * 12 * years)
    val equityGains = (equityGross - equityInvested).coerceAtLeast(0.0)
    val equityTaxableGains = (equityGains - 125000.0).coerceAtLeast(0.0)
    val equityTax = equityTaxableGains * 0.125
    val equityNet = equityGross - equityTax

    val goldGross = calculateCompoundValue(initialBalance, monthlyContribution, years, 0.095)
    val goldInvested = initialBalance + (monthlyContribution * 12 * years)
    val goldGains = (goldGross - goldInvested).coerceAtLeast(0.0)
    val goldTax = goldGains * 0.125
    val goldNet = goldGross - goldTax

    val fdGross = calculateCompoundValue(initialBalance, monthlyContribution, years, 0.07)
    val fdNetRate = 0.07 * (1.0 - 0.30)
    val fdNet = calculateCompoundValue(initialBalance, monthlyContribution, years, fdNetRate)

    return listOf(
        AssetProjectionResult(
            assetName = AppStrings.dsopAssetDsopLabel,
            returnRateLabel = "7.1% Tax-Free",
            grossBalance = dsopResult,
            netTaxFreeBalance = dsopResult,
            taxDeduction = 0.0,
            isTaxFree = true,
        ),
        AssetProjectionResult(
            assetName = AppStrings.dsopAssetEquitiesLabel,
            returnRateLabel = "12.0% CAGR",
            grossBalance = equityGross,
            netTaxFreeBalance = equityNet,
            taxDeduction = equityTax,
            isTaxFree = false,
        ),
        AssetProjectionResult(
            assetName = AppStrings.dsopAssetGoldLabel,
            returnRateLabel = "9.5% CAGR",
            grossBalance = goldGross,
            netTaxFreeBalance = goldNet,
            taxDeduction = goldTax,
            isTaxFree = false,
        ),
        AssetProjectionResult(
            assetName = AppStrings.dsopAssetFdLabel,
            returnRateLabel = "7.0% (30% Tax)",
            grossBalance = fdGross,
            netTaxFreeBalance = fdNet,
            taxDeduction = fdGross - fdNet,
            isTaxFree = false,
        ),
    )
}

internal fun calculateCompoundValue(
    initialBalance: Double,
    monthlyContribution: Double,
    years: Int,
    annualRate: Double,
): Double {
    var balance = initialBalance
    repeat(years) {
        val annualDeposit = monthlyContribution * 12
        balance = (balance + annualDeposit) * (1.0 + annualRate)
    }
    return balance
}

@Composable
fun DsopAssetComparisonCard(
    initialBalance: Double,
    monthlyContribution: Double,
    modifier: Modifier = Modifier,
) {
    val projections =
        remember(initialBalance, monthlyContribution) {
            calculateAssetProjections(initialBalance, monthlyContribution, 15)
        }
    val maxNet = projections.maxOfOrNull { it.netTaxFreeBalance } ?: 1.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = AppStrings.dsopComparisonTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTwo))
            Text(
                text = AppStrings.dsopComparisonSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))

            projections.forEach { item ->
                AssetRowItem(item = item, maxNet = maxNet)
                Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            Text(
                text = AppStrings.dsopAssetTaxFreeNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun AssetRowItem(
    item: AssetProjectionResult,
    maxNet: Double,
) {
    val fraction = (item.netTaxFreeBalance / maxNet).toFloat().coerceIn(0.1f, 1.0f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.assetName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (item.isTaxFree) FontWeight.Bold else FontWeight.Normal,
                    color = if (item.isTaxFree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "₹${formatShortAmount(item.netTaxFreeBalance)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isTaxFree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(AppDimensions.SpacingSix))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(AppDimensions.SpacingSix),
            color = if (item.isTaxFree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
    }
}

private fun formatShortAmount(value: Double): String {
    return when {
        value >= 10000000.0 -> "${(value / 10000000.0).toString().take(4)} Cr"
        value >= 100000.0 -> "${(value / 100000.0).toString().take(4)} L"
        else -> value.toLong().toString()
    }
}
