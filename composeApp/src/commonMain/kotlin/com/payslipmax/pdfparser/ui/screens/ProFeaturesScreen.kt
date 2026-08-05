package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.launchPurchaseFlow
import com.payslipmax.pdfparser.ui.rememberHasAccess
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

/** How a catalog row reacts to the user's entitlement — drives its trailing widget and click action. */
internal enum class RowMode { OPENABLE, INCLUDED, LOCKED, COMING_SOON }

/**
 * The single decision that gates every catalog row: a coming-soon feature is always inert; a feature the
 * user lacks access to is always [LOCKED] (teaser + upgrade sheet, D4) regardless of its target; only an
 * unlocked, available feature opens ([OPENABLE] with a screen, [INCLUDED] when it lives inside a tab).
 * Kept `internal` (not `private`) so the D4 invariant is regression-tested, not just verified by eye.
 */
internal fun rowMode(
    meta: ProFeatureMeta,
    hasAccess: Boolean,
): RowMode =
    when {
        meta.availability == ProFeatureAvailability.COMING_SOON -> RowMode.COMING_SOON
        !hasAccess -> RowMode.LOCKED
        meta.target != null -> RowMode.OPENABLE
        else -> RowMode.INCLUDED
    }

/**
 * The PRO Features catalog: one row per [com.payslipmax.pdfparser.subscription.FeatureGate], driven by
 * the pure [proFeatureCatalog] SSOT. Unlocked features with a screen open it; locked features always
 * render (teaser, D4) and open [PremiumUpgradeBottomSheet] on tap; coming-soon rows are inert.
 */
@Composable
fun ProFeaturesScreen(
    viewModel: PayslipViewModel,
    onNavigateTo: (Screen) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUpgradeSheet by remember { mutableStateOf(false) }
    val catalog = remember { proFeatureCatalog() }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .detailScreenSafeArea()
                .padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        ScreenBackHeader(
            title = AppStringsPremium.proCatalogTitle,
            subtitle = AppStringsPremium.proCatalogSubtitle,
            onBack = onBack,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            catalog.forEach { meta ->
                ProFeatureRow(
                    meta = meta,
                    hasAccess = viewModel.rememberHasAccess(meta.gate),
                    onOpen = { meta.target?.let(onNavigateTo) },
                    onUpgrade = { showUpgradeSheet = true },
                )
            }
        }
    }

    if (showUpgradeSheet) {
        PremiumUpgradeBottomSheet(
            onDismissRequest = { showUpgradeSheet = false },
            onUnlockClick = { viewModel.launchPurchaseFlow() },
        )
    }
}

@Composable
private fun ProFeatureRow(
    meta: ProFeatureMeta,
    hasAccess: Boolean,
    onOpen: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val mode = rowMode(meta, hasAccess)
    val rowClick: (() -> Unit)? =
        when (mode) {
            RowMode.OPENABLE -> onOpen
            RowMode.LOCKED -> onUpgrade
            RowMode.INCLUDED, RowMode.COMING_SOON -> null
        }
    val dimmed = mode == RowMode.COMING_SOON
    Card(
        modifier = Modifier.fillMaxWidth().let { if (rowClick != null) it.clickable(onClick = rowClick) else it },
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(meta.icon, style = MaterialTheme.typography.titleLarge)
            ProFeatureText(meta = meta, dimmed = dimmed, modifier = Modifier.weight(1f))
            ProFeatureTrailing(mode = mode)
        }
    }
}

@Composable
private fun ProFeatureText(
    meta: ProFeatureMeta,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = if (dimmed) 0.5f else 1f
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        Text(
            text = meta.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
        Text(
            text = meta.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}

@Composable
private fun ProFeatureTrailing(mode: RowMode) {
    when (mode) {
        RowMode.OPENABLE ->
            Text(
                text = "➔",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        RowMode.INCLUDED ->
            Text(
                text = "✓ ${AppStringsPremium.proCatalogIncludedLabel}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        RowMode.LOCKED -> Text(AppStrings.proLockIcon, style = MaterialTheme.typography.titleMedium)
        RowMode.COMING_SOON ->
            Text(
                text = AppStringsPremium.proCatalogComingSoonLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}
