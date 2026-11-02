package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.part2.domain.HubStatus
import com.payslipmax.pdfparser.ui.components.AccentStripeCard
import com.payslipmax.pdfparser.ui.components.AccentStripeCardShape
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/**
 * The Part II Orders hub (Guide Home's one Part II tile opens it): where the officer's file stands (the hero, only when the Bank is there),
 * then Learn about Part II and the Orders Bank, one row each, then how it works while the file is empty. Learn is free for everyone and
 * the Bank opens for everyone (adding is Premium). The back header is the only way up on iOS; on Android system back does the same.
 */
@Composable
internal fun Part2HubScreen(
    onBack: () -> Unit,
    onOpenLearn: () -> Unit,
    onOpenBank: () -> Unit,
    status: HubStatus = HubStatus.Unknown,
    hero: HubHeroActions? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = LearnStrings.hubTitle, subtitle = LearnStrings.homeSubtitle, onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            if (hero != null) item(key = "hero") { Part2HubHero(status, hero) }
            item(key = "learn") { Part2HubRow(LearnStrings.entryEmoji, LearnStrings.entryTitle, LearnStrings.entrySubtitle, onOpenLearn) }
            item(key = "bank") { Part2HubRow(OrdersBankStrings.entryEmoji, OrdersBankStrings.entryTitle, OrdersBankStrings.entrySubtitle, onOpenBank) }
            if (hero != null && status == HubStatus.Empty) item(key = "how") { Part2HubHowItWorks() }
        }
    }
}

@Composable
private fun Part2HubRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    AccentStripeCard(
        accent = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(AccentStripeCardShape).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Decoration(emoji, AppDimensions.TextSizeHuge)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Decoration(AppStrings.rowArrow, AppDimensions.TextSizeLarge, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
