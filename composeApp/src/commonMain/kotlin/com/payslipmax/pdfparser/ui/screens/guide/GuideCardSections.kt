package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideColors
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * A card's body in the template order: (Your figure,) Key points, Attach, Watch out, Authority, then Details collapsed. An empty
 * section is left out. Placeholder bullets are already split off into [GuideCardFull.body], so none is drawn raw. A
 * card with no [GuideCardContent.full] (the free preview) gets the unlock panel in place of the body.
 */
internal fun LazyListScope.guideCardSections(
    card: GuideCardContent,
    onUnlock: () -> Unit,
    onCopyCite: (() -> Unit)? = null,
) {
    val full = card.full
    if (full == null) {
        item(key = "locked") { GuideLockedPanel(onUnlock) }
        return
    }
    guideYourFigure(full.figure)
    bulletSection("key", GuideStrings.sectionKeyPoints, full.body.key)
    bulletSection("attach", GuideStrings.sectionAttach, full.body.attach)
    bulletSection("watch", GuideStrings.sectionWatchOut, full.body.watch, isWarning = true)
    if (full.cite.isNotBlank()) item(key = "cite") { GuideCite(full.cite, onCopyCite) }
    if (full.details.isNotBlank()) item(key = "details") { GuideDetails(card.id, full.details) }
}

private fun LazyListScope.bulletSection(
    key: String,
    title: String,
    bullets: List<String>,
    isWarning: Boolean = false,
) {
    if (bullets.isEmpty()) return
    item(key = key) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            SectionHeading(title, if (isWarning) GuideColors.watchOut() else MaterialTheme.colorScheme.onSurfaceVariant)
            bullets.forEach { Bullet(it) }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    color: Color,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(GuideStrings.bullet, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clearAndSetSemantics {})
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The authority, in monospace like a rule reference, with a rule down its side and, when unlocked, a Copy cite button. */
@Composable
private fun GuideCite(
    cite: String,
    onCopyCite: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { SectionHeading(GuideStrings.sectionAuthority, MaterialTheme.colorScheme.onSurfaceVariant) }
            onCopyCite?.let { GuideCopyCiteButton(it) }
        }
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Box(modifier = Modifier.width(AppDimensions.BorderMedium).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            Text(cite, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Collapsed until tapped; stays open for this card across recomposition and restore. */
@Composable
private fun GuideDetails(
    cardId: String,
    details: String,
) {
    var expanded by rememberSaveable(cardId) { mutableStateOf(false) }
    val state = if (expanded) GuideStrings.detailsShown else GuideStrings.detailsHidden
    FlatBorderedCardShape {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .semantics { stateDescription = state }
                    .padding(AppDimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(GuideStrings.details, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        if (expanded) {
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = AppDimensions.SpacingMedium, end = AppDimensions.SpacingMedium, bottom = AppDimensions.SpacingMedium),
            )
        }
    }
}
