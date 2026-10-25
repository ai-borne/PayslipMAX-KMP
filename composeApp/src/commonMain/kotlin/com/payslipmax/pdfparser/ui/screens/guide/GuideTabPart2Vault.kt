package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2DraftEntry
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.CancelOutcome
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.DraftsUiState
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.LocalPageImageDecoder
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.Part2VaultList
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.PlatformPageImageDecoder
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.SlipOrders
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultNotices
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultOrderActions
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultOrderScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultOrderStatus
import com.payslipmax.pdfparser.ui.screens.guide.part2.vault.VaultUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** What the Part II home shows of the vault, wired to the host: scanning opens the new order's screen once a page is stored. */
@Composable
internal fun rememberVaultList(
    navState: GuideNavState,
    host: Part2Host,
): Part2VaultList {
    val vault = host.models?.vault
    val scope = rememberCoroutineScope()
    val state by (vault?.state ?: remember { MutableStateFlow(VaultUiState.Empty) }).collectAsState()
    val notice by (vault?.notice ?: remember { MutableStateFlow<String?>(null) }).collectAsState()
    return Part2VaultList(
        state = state,
        notice = notice,
        canScan = host.canCapture,
        onScan = { host.scanOrder(null) { id -> scope.launch { navState.push(Part2Destination.Order(id)) } } },
        onOpen = { navState.push(Part2Destination.Order(it)) },
        onDelete = { vault?.delete(it) },
        onDismissNotice = { vault?.dismissNotice() },
    )
}

/**
 * Starts cancelling vault order [orderId] (para 11): opens the cancel slip, which the draft models find saved by its id. Only an entitled
 * officer can ([Part2Host.draftEntry], the entitlement that lets one draft); a cancel that could not start says why in the vault's notice.
 */
internal fun Part2Host.cancelOrder(
    navState: GuideNavState,
    scope: CoroutineScope,
    orderId: String,
    redraft: Boolean,
) {
    val models = models ?: return
    if (draftEntry != Part2DraftEntry.OPEN) return
    scope.launch {
        when (val outcome = models.drafting.cancel(orderId, redraft)) {
            is CancelOutcome.Started -> {
                models.vault.say(null)
                navState.discardPart2Draft()
                navState.push(Part2Destination.Slip(outcome.draftId))
            }
            is CancelOutcome.Refused -> models.vault.say(VaultNotices.cancelRefusal(outcome.why))
        }
    }
}

/** What a draft's slip shows of the vault: the orders linked to [draftId], and adding the published one (scanned, linked, opened). */
@Composable
internal fun rememberSlipOrders(
    navState: GuideNavState,
    host: Part2Host,
    draftId: String,
): SlipOrders {
    val list = rememberVaultList(navState, host)
    val scope = rememberCoroutineScope()
    return SlipOrders(
        rows = list.state.rows.filter { it.draftId == draftId },
        canAdd = list.canScan,
        busy = list.state.busy,
        notice = list.notice,
        onAdd = { host.scanOrder(draftId) { id -> scope.launch { navState.push(Part2Destination.Order(id)) } } },
        onOpen = list.onOpen,
        onDismissNotice = list.onDismissNotice,
    )
}

/**
 * One order of the vault, read-only unless the officer may capture (a lapsed officer reads and deletes). An order the repository does
 * not hold, for whoever asks, cuts the level. The order's model lives with this level: leaving it (the app lock
 * included) writes a change still waiting and drops the model, and with it any page picture on the screen.
 */
@Composable
internal fun Part2OrderLevel(
    navState: GuideNavState,
    host: Part2Host,
    destination: Part2Destination.Order,
    listState: LazyListState,
) {
    val id = destination.orderId
    val vault = host.models?.vault
    if (vault == null) {
        LaunchedEffect(id) { navState.cutOrder(id) }
        return GuideLoading()
    }
    val model = remember(id) { vault.open(id) }
    DisposableEffect(model) { onDispose { model.close() } }
    // A level restored after process death never visited the Part II home, so it clears a killed scan's leftovers itself.
    LaunchedEffect(id) { host.scanner?.discardLeftovers() }
    val state by model.state.collectAsState()
    val notice by vault.notice.collectAsState()
    val drafts by (host.models?.drafts?.state ?: remember { MutableStateFlow(DraftsUiState.Empty) }).collectAsState()
    val scope = rememberCoroutineScope()
    val decoder = LocalPageImageDecoder.current ?: PlatformPageImageDecoder
    when (state.status) {
        VaultOrderStatus.LOADING -> GuideLoading()
        VaultOrderStatus.GONE -> {
            LaunchedEffect(id) { navState.cutOrder(id) }
            GuideLoading()
        }
        VaultOrderStatus.READY -> {
            val below = navState.stack.getOrNull(navState.stack.size - 2)
            val actions =
                VaultOrderActions(
                    onNumber = model::setNumber,
                    onDate = model::setDate,
                    onAddPages = { host.addPages(model) },
                    loadPage = { pageId -> model.view(pageId, decoder::decode) },
                    onLink = model::setDraft,
                    onDeletePage = { pageId -> model.deletePage(pageId) { scope.launch { navState.cutOrder(id) } } },
                    // From the slip the draft is one step back, so Back never walks a slip-order-slip loop.
                    onCancel = { host.cancelOrder(navState, scope, id, redraft = false) },
                    onRedraft = { host.cancelOrder(navState, scope, id, redraft = true) },
                    onOpenDraft = { state.draftId?.let { draft -> if (below == Part2Destination.Slip(draft)) navState.pop() else navState.push(Part2Destination.Slip(draft)) } },
                    onDelete = { model.delete { scope.launch { navState.cutOrder(id) } } },
                    onDismissNotice = vault::dismissNotice,
                )
            VaultOrderScreen(state, notice, canEdit = host.canCapture, drafts = drafts.rows, actions = actions, onBack = { navState.pop() }, listState = listState)
        }
    }
}
