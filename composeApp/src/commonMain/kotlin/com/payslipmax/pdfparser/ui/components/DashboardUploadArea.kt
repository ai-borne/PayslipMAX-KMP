package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.ui.theme.AppDimensions

/**
 * The Dashboard's upload FAB plus its one-time coachmark, as a single unit anchored to the
 * bottom-end of the enclosing [Box][androidx.compose.foundation.layout.Box]. `coachmarkEnabled`
 * lets the caller suppress the coachmark while another overlay (the upload dialog) is open.
 */
@Composable
fun BoxScope.DashboardUploadArea(
    onboardingManager: OnboardingManager,
    onUploadClick: () -> Unit,
    coachmarkEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var coachmarkDismissed by remember { mutableStateOf(!onboardingManager.shouldShowCoachmark()) }

    FloatingActionButton(
        onClick = onUploadClick,
        modifier = modifier.align(Alignment.BottomEnd).padding(AppDimensions.PaddingMedium).testTag("upload_fab"),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(Icons.Default.Add, contentDescription = "Import Payslip")
    }
    if (coachmarkEnabled && !coachmarkDismissed) {
        UploadCoachmark(
            onDismiss = {
                onboardingManager.onCoachmarkDismissed()
                coachmarkDismissed = true
            },
        )
    }
}
