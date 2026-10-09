package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.PayslipUpgradeSheet
import org.koin.compose.getKoin
import org.koin.compose.koinInject

/**
 * `Screen.GuideCard` as both platforms host it: the Guide's card for the pending target, wired to the app's entitlement and
 * to the same upgrade sheet the Guide tab uses. With the Guide off nothing can reach this, so it pops (R8 drops the rest).
 */
@Composable
fun GuideCardRoute(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
) {
    if (!isGuideEnabled()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var showUpgradeSheet by remember { mutableStateOf(false) }
    val unlocked = viewModel.rememberGuideUnlocked()
    GuideCardHost(access = GuideAccess(unlocked, onUnlock = { showUpgradeSheet = true }), onBack = onBack)
    if (showUpgradeSheet) PayslipUpgradeSheet(viewModel, onDismiss = { showUpgradeSheet = false })
}

/**
 * The card a Pay Audit finding asked for, drawn on its own (no Guide stack, so no breadcrumb). Back is the host's Back, so
 * it returns to the finding. No target (a restore after process death), a card the bundle does not hold, or a bundle that
 * will not load all pop instead of leaving a blank screen; the Guide tab is where a load failure offers retry.
 */
@Composable
internal fun GuideCardHost(
    access: GuideAccess,
    onBack: () -> Unit,
    viewModel: GuideViewModel = koinInject(),
    platform: GuidePlatform = rememberGuidePlatform(),
) {
    val target by viewModel.pendingCard.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val cardId = target
    val ready = state as? GuideUiState.Ready
    val profile = rememberGuideProfile(viewModel, cardId.orEmpty(), access.isUnlocked && cardId != null)
    val card = if (cardId != null && ready != null) viewModel.card(cardId, access.isUnlocked, profile) else null
    val leave = cardId == null || state is GuideUiState.Failed || (ready != null && card == null)
    LaunchedEffect(leave) { if (leave) onBack() }
    if (card == null) {
        if (!leave) GuideLoading()
    } else {
        val actions = rememberGuideCardActions(card, viewModel, platform)
        GuideCardScreen(
            card,
            crumbs = emptyList(),
            onCrumb = {},
            onBack = onBack,
            onUnlock = access.onUnlock,
            actions = actions,
            // No Guide stack here: the history link swaps the pending card, and Back still returns to the Pay Audit finding.
            onOpenCard = viewModel::openCard,
        )
    }
}

/**
 * The Pay Audit finding's link, or null while the Guide is off (no link at all). A locked user is offered the upgrade sheet
 * through [onLocked] and never reaches a card; an unlocked one goes through [GuideViewModel.openCard], then [onOpened] pushes
 * `Screen.GuideCard`.
 */
@Composable
fun rememberOpenGuideCard(
    viewModel: PayslipViewModel,
    onOpened: () -> Unit,
    onLocked: () -> Unit,
    enabled: Boolean = isGuideEnabled(),
): ((cardId: String) -> Unit)? {
    if (!enabled) return null
    // Resolved on tap, not on composition: Pay Audit renders without the Guide's DI until someone follows a link.
    val koin = getKoin()
    val unlocked = viewModel.rememberGuideUnlocked()
    return { cardId -> openGuideCardFromFinding(cardId, unlocked, koin.get(), onLocked, onOpened) }
}

internal fun openGuideCardFromFinding(
    cardId: String,
    unlocked: Boolean,
    guide: GuideViewModel,
    onLocked: () -> Unit,
    onOpened: () -> Unit,
) {
    if (!unlocked) {
        onLocked()
        return
    }
    guide.openCard(cardId)
    onOpened()
}
