package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun OfflineStatusPill(
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Surface(
        shape = RoundedCornerShape(AppDimensions.SpacingMedium),
        color =
            if (isDark) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            },
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTiny),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text("✔", fontSize = AppDimensions.TextSizeSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                text = AppStrings.settingsOfflineFirst,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun PrivacyCard(
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isDark) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    },
            ),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🛡️",
                fontSize = AppDimensions.TextSizeHuge,
                modifier = Modifier.padding(end = AppDimensions.SpacingMedium),
            )
            Column {
                Text(
                    text = AppStrings.settingsOfflineSecureTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpacingTwo))
                Text(
                    text = AppStrings.settingsOfflineSecureDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.PaddingMedium)
                .padding(top = AppDimensions.SpacingMedium, bottom = AppDimensions.SpacingTiny),
    )
}

@Composable
fun SettingsCategoryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        content = content,
    )
}

@Composable
fun SettingsRow(
    icon: String,
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(horizontal = AppDimensions.PaddingMedium, vertical = AppDimensions.SpacingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = AppDimensions.SpacingSmall),
        ) {
            Text(icon, fontSize = AppDimensions.TextSizeExtraLarge, modifier = Modifier.padding(end = AppDimensions.SpacingMedium))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Text("➔", fontSize = AppDimensions.TextSizeLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
