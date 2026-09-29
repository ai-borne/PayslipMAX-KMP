package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Interactive Level Selector for Pay Fixation What-If Sandbox simulation.
 */
@Composable
fun FixationSandboxSelector(
    fromLevel: String,
    toLevel: String,
    onLevelSelected: (fromLevel: String, toLevel: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = AppStringsPcdao.fixationSandboxTitle,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        LevelSelectorRow(
            label = AppStringsPcdao.fixationFromLevelLabel,
            levels = listOf("10", "11", "12A", "13", "13A"),
            selectedLevel = fromLevel,
            onSelect = { newFrom ->
                val newTo = resolveDefaultTargetLevel(newFrom)
                onLevelSelected(newFrom, newTo)
            },
        )
        LevelSelectorRow(
            label = AppStringsPcdao.fixationToLevelLabel,
            levels = resolveAvailableTargetLevels(fromLevel),
            selectedLevel = toLevel,
            onSelect = { newTo ->
                onLevelSelected(fromLevel, newTo)
            },
        )
    }
}

@Composable
private fun LevelSelectorRow(
    label: String,
    levels: List<String>,
    selectedLevel: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        levels.forEach { level ->
            val isSelected = level == selectedLevel
            LevelChip(
                level = level,
                isSelected = isSelected,
                onClick = { onSelect(level) },
            )
        }
    }
}

@Composable
private fun LevelChip(
    level: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bgColor =
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val textColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier =
            Modifier
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "${AppStringsPcdao.fixationLevelPrefix}$level",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun resolveDefaultTargetLevel(from: String): String =
    when (from) {
        "10" -> "11"
        "11" -> "12A"
        "12A" -> "13"
        "13" -> "13A"
        "13A" -> "14"
        else -> "11"
    }

private fun resolveAvailableTargetLevels(from: String): List<String> =
    when (from) {
        "10" -> listOf("10B", "11")
        "11" -> listOf("12A")
        "12A" -> listOf("13")
        "13" -> listOf("13A", "14")
        "13A" -> listOf("14")
        else -> listOf("11", "12A", "13", "14")
    }
