package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.part2.domain.HubStatus
import com.payslipmax.pdfparser.ui.components.AccentStripeCard
import com.payslipmax.pdfparser.ui.components.AccentStripeCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BankColors
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** What the hub's top card can do. Adding goes the way the Bank's own Add goes (Learn's search), and a lapsed officer is offered Premium instead. */
internal class HubHeroActions(
    val canAdd: Boolean,
    val onAdd: () -> Unit,
    val onUnlock: () -> Unit,
    val onOpenMissing: () -> Unit,
    val onOpenStays: () -> Unit = {},
)

/** The hub's top card: where the officer's file stands. Nothing at all until the first read finishes, so "empty" is never said too early. */
@Composable
internal fun Part2HubHero(
    status: HubStatus,
    actions: HubHeroActions,
) {
    when (status) {
        HubStatus.Unknown -> Unit
        HubStatus.Empty -> HeroCard(part2StatusAccent(status)) { EmptyHero(actions) }
        is HubStatus.Filing -> HeroCard(part2StatusAccent(status)) { FilingHero(status, actions) }
    }
}

/** The stripe colour for where the file stands: the Bank's "waiting" colour while copies are missing, its "attached" colour when none are, the brand blue otherwise. */
@Composable
internal fun part2StatusAccent(status: HubStatus): Color =
    when {
        status is HubStatus.Filing && status.notAttached > 0 -> BankColors.waiting()
        status is HubStatus.Filing -> BankColors.attached()
        else -> MaterialTheme.colorScheme.primary
    }

@Composable
private fun HeroCard(
    accent: Color,
    content: @Composable () -> Unit,
) {
    AccentStripeCard(accent = accent) {
        Column(modifier = Modifier.weight(1f).padding(AppDimensions.PaddingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            content()
            PrivacyLine()
        }
    }
}

@Composable
private fun EmptyHero(actions: HubHeroActions) {
    Text(OrdersBankStrings.hubEmptyTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(OrdersBankStrings.hubEmptyBody, style = MaterialTheme.typography.bodyMedium)
    Button(onClick = if (actions.canAdd) actions.onAdd else actions.onUnlock, modifier = Modifier.fillMaxWidth()) {
        Text(if (actions.canAdd) OrdersBankStrings.hubAddFirst else OrdersBankStrings.upgrade)
    }
}

@Composable
private fun FilingHero(
    status: HubStatus.Filing,
    actions: HubHeroActions,
) {
    Text(OrdersBankStrings.hubFileTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(OrdersBankStrings.hubNoted(status.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(OrdersBankStrings.hubAttachedLine(status.attached, status.total), style = MaterialTheme.typography.bodyMedium)
    if (status.notAttached > 0) HubChip(OrdersBankStrings.hubNotAttached(status.notAttached), actions.onOpenMissing)
    if (status.unregularised > 0) HubChip(OrdersBankStrings.hubStays(status.unregularised, status.timeBarred), actions.onOpenStays)
}

/** A thing still to look at, in the Bank's own "waiting" colour; it opens the Bank on exactly those entries. */
@Composable
private fun HubChip(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(AccentStripeCardShape).clickable(role = Role.Button, onClick = onClick).padding(vertical = AppDimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = BankColors.waiting(), modifier = Modifier.weight(1f))
        Decoration(AppStrings.rowArrow, AppDimensions.TextSizeLarge, BankColors.waiting())
    }
}

@Composable
private fun PrivacyLine() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Decoration(OrdersBankStrings.hubLockEmoji, AppDimensions.TextSizeLarge)
        Text(OrdersBankStrings.hubPrivacy, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The three steps, always shown under the rows (owner decision 2026-11-06: they stay once a casualty is saved). */
@Composable
internal fun Part2HubHowItWorks() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(OrdersBankStrings.hubHowTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        (1..HOW_IT_WORKS_STEPS).forEach { Text(OrdersBankStrings.hubStep(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

private const val HOW_IT_WORKS_STEPS = 3
