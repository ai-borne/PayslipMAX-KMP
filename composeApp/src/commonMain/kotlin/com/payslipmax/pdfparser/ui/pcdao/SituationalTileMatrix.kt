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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.MissionPresetId
import com.payslipmax.pcdao.reconciliation.SituationalCategory
import com.payslipmax.pcdao.reconciliation.SituationalTile
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCard

@Composable
fun SituationalTileMatrix(
    selectedCategory: SituationalCategory,
    activeContext: ActiveSituationalContext,
    autoInferredTileIds: Set<String>,
    onCategorySelected: (SituationalCategory) -> Unit,
    onToggleTile: (String) -> Unit,
    onOpenAddFactorSheet: () -> Unit,
    modifier: Modifier = Modifier,
    activePresetId: MissionPresetId? = null,
    onPresetSelected: (MissionPresetId) -> Unit = {},
) {
    FlatBorderedCard(modifier = modifier.fillMaxWidth(), tint = CardTint.Neutral, contentSpacing = 8.dp) {
        Column(modifier = Modifier.padding(4.dp)) {
            MatrixHeader()
            MissionPresetsCarousel(activePresetId, onPresetSelected, Modifier.padding(bottom = 6.dp))
            SpecializedFactorsRow(activeContext.activeSpecializedFactors.size, onOpenAddFactorSheet)
            SituationalCategoryTabBar(selectedCategory, onCategorySelected)
            CategoryTilesList(CategoryTileCatalog.getTilesForCategory(selectedCategory, activeContext, autoInferredTileIds), onToggleTile)
        }
    }
}

@Composable
private fun MatrixHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(AppStringsPcdao.matrixHeaderTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Box(
            modifier =
                Modifier
                    .background(Color(0xFFA855F7).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(AppStringsPcdao.matrixActiveBadge, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryTilesList(
    tiles: List<SituationalTile>,
    onToggleTile: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tiles.forEach { tile ->
            TileRow(tile = tile, onClick = { onToggleTile(tile.id) })
        }
    }
}

@Composable
private fun TileRow(
    tile: SituationalTile,
    onClick: () -> Unit,
) {
    val borderColor = if (tile.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (tile.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
    val isPeace = tile.id == SituationalTileKeys.POST_PEACE_HIGHER

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (isPeace) Modifier.testTag(TestTags.POSTING_PEACE_TILE) else Modifier)
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
                .background(bgColor, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tile.isRadioStyle) {
            RadioButton(selected = tile.isSelected, onClick = null, modifier = Modifier.padding(end = 6.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = tile.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (tile.isAutoInferred) AutoDetectedBadge()
            }
            Text(text = tile.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = tile.valuePreview,
            style = MaterialTheme.typography.labelMedium,
            color = if (tile.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun AutoDetectedBadge() {
    Box(
        modifier =
            Modifier
                .background(Color(0xFF38BDF8).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(AppStringsPcdao.tileAutoDetectedBadge, style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpecializedFactorsRow(
    activeFactorsCount: Int,
    onOpenSheet: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (activeFactorsCount > 0) {
            Text(
                text = "${AppStringsPcdao.specializedFactorsActivePrefix}$activeFactorsCount",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Bold,
            )
        } else {
            Box {}
        }
        Button(
            onClick = onOpenSheet,
            modifier = Modifier.testTag(TestTags.ADD_FACTOR_BUTTON),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Text(
                text = AppStringsPcdao.addFactorButtonLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
