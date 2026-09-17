package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/**
 * Fullscreen privacy overlay displayed when the app transitions away from the foreground
 * (e.g. App Switcher / Recent Apps, cold launch, or backgrounding).
 *
 * Prevents sensitive financial information from appearing in OS window previews and snapshots,
 * mirroring the iOS `scenePhase != .active` behavior while keeping in-app screenshots functional.
 */
@Composable
fun PayslipMaxProtectedOverlay(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = AppStrings.appProtectedShieldDesc,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppDimensions.IconSizeDouble),
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            Text(
                text = AppStrings.appProtectedTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
