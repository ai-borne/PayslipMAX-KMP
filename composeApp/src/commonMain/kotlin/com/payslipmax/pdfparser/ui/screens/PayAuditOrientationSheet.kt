package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditEntryStrings

/** Shows [PayAuditOrientationSheet] on the first Pay Audit open only; dismissing it persists the flag. */
@Composable
fun PayAuditIntroGate(onboardingManager: OnboardingManager) {
    var show by remember { mutableStateOf(onboardingManager.shouldShowPayAuditIntro()) }
    if (show) {
        PayAuditOrientationSheet(
            onDismiss = {
                onboardingManager.onPayAuditIntroDismissed()
                show = false
            },
        )
    }
}

/** First-time orientation for the timeline/evidence model (ported from 1.0's onboarding sheet, copy rewritten). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayAuditOrientationSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(AppDimensions.PaddingMedium).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(PayAuditEntryStrings.orientationTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            PayAuditEntryStrings.orientationPoints.forEach { (heading, body) ->
                Text(heading, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                PayAuditEntryStrings.orientationGlossaryHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("pay_audit_orientation_got_it")) {
                Text(PayAuditEntryStrings.orientationGotIt)
            }
        }
    }
}
