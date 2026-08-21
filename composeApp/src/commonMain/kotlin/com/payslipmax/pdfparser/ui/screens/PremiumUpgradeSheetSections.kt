@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.LegalStrings

@Composable
internal fun UpgradeHeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        Text(AppStrings.premiumTeaserCrownIcon, fontSize = AppDimensions.FontSizeEmojiMedium)
        Text(
            text = AppStrings.settingsPremiumPlanTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = AppStrings.settingsPremiumPlanDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun UpgradeBenefitsSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppDimensions.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = AppStrings.settingsPremiumPlanBullet1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsPremiumPlanBullet2,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsPremiumPlanBullet3,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = AppStrings.settingsPremiumPlanBullet4,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun UpgradePricingSection(price: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo),
    ) {
        Text(
            text = price,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = AppStrings.settingsPremiumPlanBillingNote,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun UpgradeActionsSection(
    isPurchasing: Boolean,
    isRestoring: Boolean,
    onUnlockClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val isBusy = isPurchasing || isRestoring
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Button(
            onClick = onUnlockClick,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isPurchasing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimensions.IconSizeMedium),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(AppStrings.settingsPremiumUpgradeBtn)
            }
        }
        OutlinedButton(
            onClick = onRestoreClick,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isRestoring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimensions.IconSizeMedium),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(AppStrings.settingsRestorePurchasesTitle)
            }
        }
        TextButton(
            onClick = onCloseClick,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(AppStrings.btnCancel)
        }
    }
}

@Composable
internal fun UpgradeLegalFooter(
    onTermsClick: (() -> Unit)?,
    onPrivacyClick: (() -> Unit)?,
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = AppStrings.settingsTermsOfUse,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier =
                Modifier.clickable {
                    if (onTermsClick != null) onTermsClick() else uriHandler.openUri(LegalStrings.termsOfUseUrl)
                },
        )
        Text(
            text = AppStrings.legalSeparator,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = AppStrings.settingsHelpPrivacyTitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier =
                Modifier.clickable {
                    if (onPrivacyClick != null) onPrivacyClick() else uriHandler.openUri(LegalStrings.privacyPolicyUrl)
                },
        )
    }
}
