package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.payslipmax.pcdao.reconciliation.SpecializedFactorEntry
import com.payslipmax.pcdao.reconciliation.SpecializedMilitaryFactor

@Composable
fun AddFactorBottomSheet(
    activeFactors: Set<SpecializedMilitaryFactor>,
    onToggleFactor: (SpecializedMilitaryFactor) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalog = getFactorCatalog(activeFactors)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetHeader()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(catalog) { entry ->
                        FactorCatalogRow(entry = entry, onToggle = { onToggleFactor(entry.factor) })
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.APPLY_FACTORS_BUTTON),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = AppStringsPcdao.btnApplyFactors,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = AppStringsPcdao.sheetTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = AppStringsPcdao.sheetSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FactorCatalogRow(
    entry: SpecializedFactorEntry,
    onToggle: () -> Unit,
) {
    val borderColor = if (entry.isActive) Color(0xFF34D399) else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (entry.isActive) Color(0xFF34D399).copy(alpha = 0.08f) else Color.Transparent
    val isMarcos = entry.factor == SpecializedMilitaryFactor.MARCOS_SPECIAL_FORCES

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
                .background(bgColor, RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = entry.rateDescription, style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Medium)
            Text(text = entry.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${AppStringsPcdao.authorityPrefix}${entry.statutoryAuthority}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        Checkbox(
            checked = entry.isActive,
            onCheckedChange = { onToggle() },
            modifier =
                Modifier
                    .padding(start = 6.dp)
                    .then(if (isMarcos) Modifier.testTag(TestTags.MARCOS_CHECKBOX) else Modifier),
        )
    }
}

private fun getFactorCatalog(active: Set<SpecializedMilitaryFactor>): List<SpecializedFactorEntry> =
    listOf(
        SpecializedFactorEntry(SpecializedMilitaryFactor.SIACHEN_GLACIER, "❄️ Siachen Glacier (RH-MAX)", "Deployed in Saltoro / Siachen operational zone", "₹42,500/mo (₹53,125 escalated)", "MoD Order 1(16)/2017/D(Pay)", active.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER)),
        SpecializedFactorEntry(SpecializedMilitaryFactor.HIGH_ALTITUDE_III, "⛰️ High Altitude (HAFAA)", "Serving in Highly Active Field Area", "₹16,900/mo", "MoD Order 8(3)/2000/D(Pay)", active.contains(SpecializedMilitaryFactor.HIGH_ALTITUDE_III)),
        SpecializedFactorEntry(SpecializedMilitaryFactor.MARCOS_SPECIAL_FORCES, "🪖 MARCOS / Special Forces Pay", "Serving in Special Forces Airborne / Marine unit", "₹25,000/mo", "Army Rule 103", active.contains(SpecializedMilitaryFactor.MARCOS_SPECIAL_FORCES)),
        SpecializedFactorEntry(SpecializedMilitaryFactor.DIVYANG_CHILD, "♿ Divyang Child (Double CEA)", "Special child with benchmark disability", "₹67,500/child/yr", "DoPT OM 14028/3/2014-Estt.", active.contains(SpecializedMilitaryFactor.DIVYANG_CHILD)),
        SpecializedFactorEntry(SpecializedMilitaryFactor.DIVYANG_OFFICER, "♿ Divyang Officer (Double TPTA)", "Officer with physical disability", "Double TPTA rate", "DoPT OM 21/5/2017-E.II(B)", active.contains(SpecializedMilitaryFactor.DIVYANG_OFFICER)),
        SpecializedFactorEntry(SpecializedMilitaryFactor.GALLANTRY_AWARD, "🎖️ Gallantry Award Allowance", "Param Vir, Maha Vir, Vir Chakra, Sena Medal", "Tax-exempt allowance", "Sec 10(18) Income Tax Act", active.contains(SpecializedMilitaryFactor.GALLANTRY_AWARD)),
        SpecializedFactorEntry(SpecializedMilitaryFactor.COMPOSITE_TRANSFER_GRANT, "🧳 Composite Transfer Grant (CTG)", "Permanent transfer claim within 180 days", "80% of Basic Pay", "MoD Order 19030/1/2017", active.contains(SpecializedMilitaryFactor.COMPOSITE_TRANSFER_GRANT)),
        SpecializedFactorEntry(SpecializedMilitaryFactor.FLYING_ALLOWANCE, "✈️ Flying Allowance (Army Aviation)", "Qualified Aviator pilot / navigator", "₹25,000/mo", "MoD Order 1(16)/2017", active.contains(SpecializedMilitaryFactor.FLYING_ALLOWANCE)),
        SpecializedFactorEntry(SpecializedMilitaryFactor.TECHNICAL_ALLOWANCE, "⚡ Technical Allowance", "Corps of Signals / EME qualified officer", "₹3,000 - ₹4,500/mo", "Special Army Order 2/S/98", active.contains(SpecializedMilitaryFactor.TECHNICAL_ALLOWANCE)),
    )
