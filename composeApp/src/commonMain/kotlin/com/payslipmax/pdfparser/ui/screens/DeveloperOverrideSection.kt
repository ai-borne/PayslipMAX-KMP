package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.isDebugBuild
import com.payslipmax.pdfparser.subscription.isTestFlightBuild
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.devOverride
import com.payslipmax.pdfparser.ui.setDevOverride
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/**
 * Debug/TestFlight-only 3-state entitlement override (D1/D2). Renders nothing in App Store
 * production: the whole section is skipped unless [isDebugBuild] or [isTestFlightBuild] is true, so
 * there is no public premium toggle in a real production install — the only production unlock path
 * is [PremiumUpgradeBottomSheet]'s Unlock button. The TestFlight eligibility exists specifically so
 * Phase 7 of `docs/Launch/08_ios_monetization_phaseplan.md` can flip `FORCE_FREE` here to reach the
 * real paywall for a sandbox purchase test, without touching the production free-launch flag.
 */
@Composable
fun DeveloperOverrideSection(viewModel: PayslipViewModel) {
    if (!isDebugBuild() && !isTestFlightBuild()) return
    val override by viewModel.devOverride.collectAsState()
    SettingsCategoryHeader(title = AppStrings.settingsDevOverrideTitle)
    SettingsCategoryCard {
        Column(modifier = Modifier.fillMaxWidth().padding(AppDimensions.PaddingMedium)) {
            Text(
                text = AppStrings.settingsDevOverrideDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DevOverride.values().forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = override == option,
                        onClick = { viewModel.setDevOverride(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = DevOverride.values().size),
                    ) {
                        Text(devOverrideLabel(option))
                    }
                }
            }
        }
        ReminderDelaySwitch() // debug builds with Part II on only; composes nothing otherwise
    }
}

private fun devOverrideLabel(option: DevOverride): String =
    when (option) {
        DevOverride.FOLLOW_FLAG -> AppStrings.settingsDevOverrideFollowFlag
        DevOverride.FORCE_PRO -> AppStrings.settingsDevOverrideForcePremium
        DevOverride.FORCE_FREE -> AppStrings.settingsDevOverrideForceFree
    }
