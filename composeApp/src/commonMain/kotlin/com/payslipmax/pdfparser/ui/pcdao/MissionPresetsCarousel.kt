package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.reconciliation.MissionPresetId
import com.payslipmax.pcdao.reconciliation.SituationalMissionPreset
import com.payslipmax.pcdao.reconciliation.SituationalMissionPresets

/**
 * 1-Tap Mission Presets horizontal strip.
 * Applies pre-configured Army mission profiles (e.g. RR CI Ops, Siachen, AMC Hospital).
 */
@Composable
fun MissionPresetsCarousel(
    activePresetId: MissionPresetId?,
    onPresetSelected: (MissionPresetId) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<SituationalMissionPreset> = SituationalMissionPresets.ALL_PRESETS,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = AppStringsPcdao.presetsHeaderTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = AppStringsPcdao.presetsSimulationHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PRESET_CAROUSEL)
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                PresetChip(
                    preset = preset,
                    isSelected = activePresetId == preset.id,
                    onClick = { onPresetSelected(preset.id) },
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    preset: SituationalMissionPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }
    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Box(
        modifier =
            Modifier
                .testTag("preset_chip_${preset.id.name}")
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = AppStringsPcdao.getPresetLabel(preset.id),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
        )
    }
}
