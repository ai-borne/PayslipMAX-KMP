package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun TopNavBar(
    onBack: () -> Unit,
    onGuideClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppStringsPcdao.screenTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = AppStringsPcdao.screenSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onGuideClick != null) {
            IconButton(
                onClick = onGuideClick,
                modifier = Modifier.testTag("pcdao_guide_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = AppStringsPcdao.onboardingTopBarGuideDesc,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
internal fun FeedHeaderSection(
    selectedFilter: FindingFilter,
    onFilterSelected: (FindingFilter) -> Unit,
    onRedressalClick: () -> Unit,
    isUnlocked: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = AppStringsPcdao.feedHeaderTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onRedressalClick,
                modifier = Modifier.testTag(TestTags.REDRESSAL_KIT_BUTTON),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = if (isUnlocked) AppStringsPcdao.btnRedressalKit else AppStringsPcdao.proRedressalLocked,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        FilterBar(selectedFilter = selectedFilter, onFilterSelected = onFilterSelected)
    }
}

@Composable
private fun FilterBar(
    selectedFilter: FindingFilter,
    onFilterSelected: (FindingFilter) -> Unit,
) {
    val filters =
        listOf(
            FindingFilter.ALL to AppStringsPcdao.filterAll,
            FindingFilter.ENTITLEMENTS to AppStringsPcdao.filterEntitlements,
            FindingFilter.HAZARDS to AppStringsPcdao.filterHazards,
            FindingFilter.ALARMS to AppStringsPcdao.filterAlarms,
            FindingFilter.TAX_SHIELD to AppStringsPcdao.filterShield,
        )
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        filters.forEach { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

@Composable
internal fun EmptyFindingsState() {
    EmptyPlaceholder(
        icon = "🛡️",
        title = AppStringsPcdao.feedEmptyTitle,
        message = AppStringsPcdao.feedEmptyMessage,
    )
}

@Composable
internal fun EmptyVaultState() {
    EmptyPlaceholder(
        icon = "📁",
        title = AppStringsPcdao.vaultEmptyTitle,
        message = AppStringsPcdao.vaultEmptyMessage,
        padding = 32.dp,
    )
}

@Composable
private fun EmptyPlaceholder(
    icon: String,
    title: String,
    message: String,
    padding: Dp = 24.dp,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$icon $title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
