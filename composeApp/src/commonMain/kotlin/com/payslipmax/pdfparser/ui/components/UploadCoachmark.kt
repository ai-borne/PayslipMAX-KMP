package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsOnboarding

/**
 * One-time tooltip anchored near the Dashboard's upload FAB, pointing new users at it. Not a
 * generic reusable tooltip system — this serves only the one upload FAB.
 */
@Composable
fun UploadCoachmark(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Popup(
        alignment = Alignment.BottomEnd,
        offset = IntOffset(x = -AppDimensions.PaddingMedium.value.toInt(), y = -AppDimensions.IconSizeDouble.value.toInt()),
    ) {
        Card(
            modifier = modifier.widthIn(max = AppDimensions.LockKeyboardWidth).testTag("upload_coachmark"),
            shape = RoundedCornerShape(AppDimensions.CornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) {
            Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
                Text(
                    text = AppStringsOnboarding.onboardingCoachmarkText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = AppStringsOnboarding.onboardingCoachmarkDismiss,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                }
            }
        }
    }
}
