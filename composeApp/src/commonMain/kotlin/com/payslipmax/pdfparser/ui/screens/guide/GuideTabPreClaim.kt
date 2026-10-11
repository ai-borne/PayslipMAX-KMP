package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimEntry
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimMoveType
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimRelatedCards
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimRoute
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.preClaimEntry
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/**
 * The Guide's side of the TA/DA worksheet. [entry] is what the Home row and the cards show (HIDDEN while the dark-launch flag is off, so
 * nothing is hidden, it is absent). [model] is the one worksheet, kept in [GuideNavState.preClaimModel] while a worksheet level is on the
 * stack: it survives a tab switch and reading a rule card, and is discarded on a lock or when the stack leaves the worksheet. Null for a
 * free user, so no payslip is read for them.
 */
internal class PreClaimHost(
    val entry: PreClaimEntry,
    val model: PreClaimViewModel?,
)

/** Builds the [PreClaimHost]. A null [factory] is the flag being off. The first worksheet level's move, if it names one, is applied once, at creation. */
@Composable
internal fun rememberPreClaimHost(
    factory: (() -> PreClaimViewModel)?,
    access: GuideAccess,
    navState: GuideNavState,
): PreClaimHost {
    val entry = if (factory == null) PreClaimEntry.HIDDEN else preClaimEntry(access, enabled = true)
    val level = navState.stack.firstNotNullOfOrNull { it as? GuideDestination.PreClaim }
    if (level == null || factory == null || entry != PreClaimEntry.OPEN) {
        navState.discardPreClaim()
    } else if (navState.preClaimModel == null) {
        navState.preClaimModel = factory().also { created -> PreClaimMoveType.entries.firstOrNull { it.key == level.move }?.let(created::chooseMove) }
    }
    return PreClaimHost(entry, navState.preClaimModel)
}

/** The Guide Home row that opens the worksheet; a free user sees the same row marked Premium and lands on the upgrade panel. */
@Composable
internal fun PreClaimEntryRow(
    locked: Boolean,
    onClick: () -> Unit,
) {
    GuideTileFrame(onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(PreClaimStrings.entryTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (locked) Text(PreClaimStrings.entryPremium, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(PreClaimStrings.entrySubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** What a related card's button does: open the worksheet straight on the card's move. */
internal class PrepareClaim(val onPrepare: () -> Unit)

/**
 * The card's button, or null when the flag is off, the card leads to no worksheet, or the officer is already inside a worksheet (they
 * came here from it, and Back returns to it). Reads the loaded bundle; nothing is stored.
 */
@Composable
internal fun rememberPrepareClaim(
    navState: GuideNavState,
    viewModel: GuideViewModel,
    cardId: String,
    entry: PreClaimEntry,
): PrepareClaim? {
    if (entry == PreClaimEntry.HIDDEN || navState.stack.any { it is GuideDestination.PreClaim }) return null
    val bundle = (viewModel.uiState.value as? GuideUiState.Ready)?.bundle ?: return null
    val move = remember(bundle, cardId) { PreClaimRelatedCards.moveFor(bundle, cardId) } ?: return null
    return remember(move) { PrepareClaim { navState.push(GuideDestination.PreClaim(move.key)) } }
}

@Composable
internal fun PrepareClaimButton(prepare: PrepareClaim) {
    Button(onClick = prepare.onPrepare, modifier = Modifier.fillMaxWidth()) { Text(PreClaimStrings.prepareThisClaim) }
}

/** The worksheet level. With the entry hidden (flag off after a restore) the stack is cut back instead of showing anything. */
@Composable
internal fun GuidePreClaimLevel(
    navState: GuideNavState,
    access: GuideAccess,
    platform: GuidePlatform,
    host: PreClaimHost,
    listState: LazyListState,
) {
    if (host.entry == PreClaimEntry.HIDDEN) {
        LaunchedEffect(Unit) { navState.pop() }
        GuideLoading()
        return
    }
    PreClaimRoute(
        entry = host.entry,
        model = host.model,
        platform = platform,
        onBack = { navState.pop() },
        onUnlock = access.onUnlock,
        onOpenCard = { navState.push(GuideDestination.Card(it)) },
        listState = listState,
    )
}
