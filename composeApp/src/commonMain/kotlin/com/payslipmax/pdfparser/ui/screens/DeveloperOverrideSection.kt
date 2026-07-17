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
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.devOverride
import com.payslipmax.pdfparser.ui.setDevOverride
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/**
 * Debug-only 3-state entitlement override (D1/D2). Renders nothing in release: the whole section is
 * skipped when [isDebugBuild] is false, so there is no public premium toggle in shipped builds — the
 * only release unlock path is [PremiumUpgradeBottomSheet]'s Unlock button.
 */
@Composable
fun DeveloperOverrideSection(viewModel: PayslipViewModel) {
    if (!isDebugBuild()) return
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
    }
}

private fun devOverrideLabel(option: DevOverride): String =
    when (option) {
        DevOverride.FOLLOW_FLAG -> AppStrings.settingsDevOverrideFollowFlag
        DevOverride.FORCE_PRO -> AppStrings.settingsDevOverrideForcePro
        DevOverride.FORCE_FREE -> AppStrings.settingsDevOverrideForceFree
    }
