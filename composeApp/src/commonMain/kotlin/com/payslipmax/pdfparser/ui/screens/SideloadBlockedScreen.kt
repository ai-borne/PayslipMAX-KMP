package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/**
 * Non-dismissible security blocking screen displayed when the app detects
 * an untrusted sideloaded installation or signature tampering.
 */
@Composable
fun SideloadBlockedScreen(
    reason: String? = null,
    onOpenStore: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SideloadBlockedHeader(reason = reason)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingDouble))
            SideloadBlockedAction(onOpenStore = onOpenStore)
        }
    }
}

@Composable
private fun SideloadBlockedHeader(reason: String?) {
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = AppStrings.sideloadBlockedTitle,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(AppDimensions.IconSizeHuge),
    )

    Spacer(modifier = Modifier.height(AppDimensions.SpacingHuge))

    Text(
        text = AppStrings.sideloadBlockedTitle,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

    Text(
        text = AppStrings.sideloadBlockedMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
    )

    if (!reason.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
        Text(
            text = reason,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SideloadBlockedAction(onOpenStore: (() -> Unit)?) {
    val uriHandler = LocalUriHandler.current

    Button(
        onClick = {
            onOpenStore?.invoke() ?: run {
                try {
                    uriHandler.openUri(AppStrings.playStoreUrl)
                } catch (_: Exception) {
                }
            }
        },
        modifier = Modifier.fillMaxWidth(0.8f),
    ) {
        Text(text = AppStrings.sideloadGetOfficialAppButton)
    }
}
