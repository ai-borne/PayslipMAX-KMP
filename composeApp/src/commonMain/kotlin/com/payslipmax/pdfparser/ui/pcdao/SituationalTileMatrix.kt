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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
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
) {
    FlatBorderedCard(modifier = modifier.fillMaxWidth(), tint = CardTint.Neutral, contentSpacing = 8.dp) {
        Column(modifier = Modifier.padding(4.dp)) {
            MatrixHeader()
            CategoryTabBar(selectedCategory = selectedCategory, onCategorySelected = onCategorySelected)
            CategoryTilesList(
                tiles = getTilesForCategory(selectedCategory, activeContext, autoInferredTileIds),
                onToggleTile = onToggleTile,
            )
            SpecializedFactorsRow(
                activeFactorsCount = activeContext.activeSpecializedFactors.size,
                onOpenSheet = onOpenAddFactorSheet,
            )
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
        Text(
            text = AppStringsPcdao.matrixHeaderTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier =
                Modifier
                    .background(Color(0xFFA855F7).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = AppStringsPcdao.matrixActiveBadge,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC084FC),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CategoryTabBar(
    selectedCategory: SituationalCategory,
    onCategorySelected: (SituationalCategory) -> Unit,
) {
    val categories = SituationalCategory.values()
    val titles =
        listOf(
            AppStringsPcdao.tabPosting,
            AppStringsPcdao.tabHousing,
            AppStringsPcdao.tabChildrenCea,
            AppStringsPcdao.tabCareer,
            AppStringsPcdao.tabFundsLtc,
        )
    ScrollableTabRow(
        selectedTabIndex = selectedCategory.ordinal,
        edgePadding = 0.dp,
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
    ) {
        categories.forEachIndexed { index, cat ->
            Tab(
                selected = selectedCategory == cat,
                onClick = { onCategorySelected(cat) },
                text = {
                    Text(
                        text = titles.getOrElse(index) { cat.name },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                    )
                },
            )
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
            RadioButton(
                selected = tile.isSelected,
                onClick = null,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = tile.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (tile.isAutoInferred) {
                    AutoDetectedBadge()
                }
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
        Text(
            text = AppStringsPcdao.tileAutoDetectedBadge,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold,
        )
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

private fun getTilesForCategory(
    category: SituationalCategory,
    context: ActiveSituationalContext,
    autoInferred: Set<String>,
): List<SituationalTile> {
    val active = context.activeTileIds
    return when (category) {
        SituationalCategory.POSTING ->
            listOf(
                SituationalTile(SituationalTileKeys.POST_FIELD_HAFAA, category, "🏔️ HAFAA Field Area", "Serving in Highly Active Field Area", "₹16,900/mo", autoInferred.contains(SituationalTileKeys.POST_FIELD_HAFAA), active.contains(SituationalTileKeys.POST_FIELD_HAFAA)),
                SituationalTile(SituationalTileKeys.POST_PEACE_HIGHER, category, "🏙️ Peace (Pune / Higher UA)", "Higher Rate City (20 UA Cities)", "₹7,200 + DA", autoInferred.contains(SituationalTileKeys.POST_PEACE_HIGHER), active.contains(SituationalTileKeys.POST_PEACE_HIGHER), isRadioStyle = true),
                SituationalTile(SituationalTileKeys.POST_PEACE_OTHER, category, "🌾 Peace (Other Locations)", "Standard Peace TPTA", "₹3,600 + DA", autoInferred.contains(SituationalTileKeys.POST_PEACE_OTHER), active.contains(SituationalTileKeys.POST_PEACE_OTHER), isRadioStyle = true),
                SituationalTile(SituationalTileKeys.POST_SIACHEN, category, "❄️ Siachen Glacier", "RH-MAX deployment zone", "₹42,500 + 25% = ₹53,125", autoInferred.contains(SituationalTileKeys.POST_SIACHEN), active.contains(SituationalTileKeys.POST_SIACHEN)),
                SituationalTile(SituationalTileKeys.POST_SDA_NE, category, "🌿 North-East SDA", "Special Duty Allowance", "10% of Basic Pay", autoInferred.contains(SituationalTileKeys.POST_SDA_NE), active.contains(SituationalTileKeys.POST_SDA_NE)),
            )
        SituationalCategory.HOUSING ->
            listOf(
                SituationalTile(SituationalTileKeys.HOUSE_GOVT_MQ, category, "🏢 Govt Married Accomm", "License Fee deducted, no HRA", "License Fee", autoInferred.contains(SituationalTileKeys.HOUSE_GOVT_MQ), active.contains(SituationalTileKeys.HOUSE_GOVT_MQ)),
                SituationalTile(SituationalTileKeys.HOUSE_FAMILY_SPR, category, "📍 Family at SPR", "Selected Place of Residence", "20%/30% HRA", autoInferred.contains(SituationalTileKeys.HOUSE_FAMILY_SPR), active.contains(SituationalTileKeys.HOUSE_FAMILY_SPR)),
                SituationalTile(SituationalTileKeys.HOUSE_LIVING_OUT_NAC, category, "📜 Living Out on NAC", "Non-Availability Certificate issued", "Full Station HRA", autoInferred.contains(SituationalTileKeys.HOUSE_LIVING_OUT_NAC), active.contains(SituationalTileKeys.HOUSE_LIVING_OUT_NAC)),
                SituationalTile(SituationalTileKeys.HOUSE_GOVT_CONVEYANCE, category, "🚗 Govt Conveyance Provided", "Unit transport allocated", "Bars TPTA", autoInferred.contains(SituationalTileKeys.HOUSE_GOVT_CONVEYANCE), active.contains(SituationalTileKeys.HOUSE_GOVT_CONVEYANCE)),
            )
        SituationalCategory.CHILDREN_CEA ->
            listOf(
                SituationalTile(SituationalTileKeys.CEA_NONE, category, "0 School Children", "No education claims active", "₹0", autoInferred.contains(SituationalTileKeys.CEA_NONE), active.contains(SituationalTileKeys.CEA_NONE), isRadioStyle = true),
                SituationalTile(SituationalTileKeys.CEA_ONE_CHILD, category, "🎒 1 Child in Day School", "Class Nursery to XII", "₹33,750/yr", autoInferred.contains(SituationalTileKeys.CEA_ONE_CHILD), active.contains(SituationalTileKeys.CEA_ONE_CHILD), isRadioStyle = true),
                SituationalTile(SituationalTileKeys.CEA_TWO_CHILDREN, category, "🎒 2 Children in Day School", "Standard 2-child entitlement", "₹67,500/yr", autoInferred.contains(SituationalTileKeys.CEA_TWO_CHILDREN), active.contains(SituationalTileKeys.CEA_TWO_CHILDREN), isRadioStyle = true),
                SituationalTile(SituationalTileKeys.CEA_HOSTEL, category, "🏫 Child in Hostel", "Hostel Subsidy (Escalated 25%)", "₹1,01,250/yr", autoInferred.contains(SituationalTileKeys.CEA_HOSTEL), active.contains(SituationalTileKeys.CEA_HOSTEL)),
            )
        SituationalCategory.CAREER_PROMOTION ->
            listOf(
                SituationalTile(SituationalTileKeys.PROMOTION_ACTIVE, category, "⭐ Substantive Promotion Due", "Triggers Rule 10/11 Fixation", "Option 1 vs 2", autoInferred.contains(SituationalTileKeys.PROMOTION_ACTIVE), active.contains(SituationalTileKeys.PROMOTION_ACTIVE)),
                SituationalTile(SituationalTileKeys.RETIRE_NEAR, category, "⏳ Retiring in < 3 Months", "Superannuation / Release", "DSOP Stop Alarm", autoInferred.contains(SituationalTileKeys.RETIRE_NEAR), active.contains(SituationalTileKeys.RETIRE_NEAR)),
                SituationalTile(SituationalTileKeys.DNI_SCHEDULED, category, "📅 Annual Increment: 1 July", "Scheduled DNI cycle", "Next Increment", autoInferred.contains(SituationalTileKeys.DNI_SCHEDULED), active.contains(SituationalTileKeys.DNI_SCHEDULED)),
            )
        SituationalCategory.FUNDS_LTC ->
            listOf(
                SituationalTile(SituationalTileKeys.AVAILED_LTC, category, "🎫 Availed LTC Concession", "10-day leave encashment eligible", "10 Days Pay+DA", autoInferred.contains(SituationalTileKeys.AVAILED_LTC), active.contains(SituationalTileKeys.AVAILED_LTC)),
                SituationalTile(SituationalTileKeys.DSOP_HIGH_PACING, category, "📈 DSOP > ₹5 Lakh Annual", "Sec 10(11) tax-exempt limit", "Tax Drag Alert", autoInferred.contains(SituationalTileKeys.DSOP_HIGH_PACING), active.contains(SituationalTileKeys.DSOP_HIGH_PACING)),
                SituationalTile(SituationalTileKeys.TRANSFER_CTG, category, "🧳 Permanent Transfer (CTG)", "Composite Transfer Grant", "80% Basic Pay", autoInferred.contains(SituationalTileKeys.TRANSFER_CTG), active.contains(SituationalTileKeys.TRANSFER_CTG)),
            )
    }
}
