package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.platform.rememberPageScanner
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.AttachActions
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.AttachOrderScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.AttachOutcome
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.LocalPageImageDecoder
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.LocalPageScanner
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.OrderActions
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.OrderNotices
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.OrderScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.OrdersShelfScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.PageReader
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.PlatformPageImageDecoder
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.launch

/** The attach flow. A lapsed officer is offered Premium instead: attaching adds data, and the Bank's rule is view, delete and back up only. */
@Composable
internal fun GuideAttachLevel(
    navState: GuideNavState,
    host: Part2Host,
    bank: BankViewModel,
    destination: Part2Destination.Attach,
) {
    if (!host.access.canAdd) return AttachLocked(host.access, onBack = { navState.pop() })
    val vm = bank.attach
    val scanner = LocalPageScanner.current ?: rememberPageScanner()
    val scope = rememberCoroutineScope()
    LaunchedEffect(destination) { vm.ensure(destination.entryId) }
    val state by vm.state.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    val actions =
        AttachActions(
            message = message,
            nameOf = bank::nameOf,
            onScan = { scope.launch { vm.scan(scanner) } },
            onDiscard = vm::discardScan,
            onAddCasualty = { host.open(navState) },
            onSave = {
                vm.save { outcome ->
                    message = OrderNotices.outcome(outcome)
                    finished(navState, bank, outcome)
                }
            },
            onBack = {
                vm.close()
                navState.pop()
            },
        )
    AttachOrderScreen(vm, state, actions)
}

/** A saved order replaces the form with its own screen, which says so if a page was dropped. */
private fun finished(
    navState: GuideNavState,
    bank: BankViewModel,
    outcome: AttachOutcome,
) {
    if (outcome !is AttachOutcome.Saved) return
    bank.orderModel(outcome.orderId).note(CaptureResult.Saved(outcome.saved, outcome.notSaved))
    navState.pop()
    navState.push(Part2Destination.Order(outcome.orderId))
}

@Composable
private fun AttachLocked(
    access: BankAccess,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = OrderStrings.attachTitle, onBack = onBack)
        Column(modifier = Modifier.padding(Part2ListPadding)) {
            Text(OrderStrings.upgradeToAttach)
            Button(onClick = access.onUnlock, modifier = Modifier.fillMaxWidth()) { Text(OrdersBankStrings.upgrade) }
        }
    }
}

/** One scanned order. An order the store no longer has closes its level instead of showing a blank page. */
@Composable
internal fun GuideOrderLevel(
    navState: GuideNavState,
    bank: BankViewModel,
    orderId: String,
    access: BankAccess,
) {
    val vm = remember(orderId) { bank.orderModel(orderId) }
    val scanner = LocalPageScanner.current ?: rememberPageScanner()
    val scope = rememberCoroutineScope()
    val decoder = LocalPageImageDecoder.current ?: PlatformPageImageDecoder
    val state by vm.state.collectAsState()
    // A restored order level may sit over a scan the dead process left behind.
    LaunchedEffect(orderId) { if (!bank.attach.holdsScan()) scanner.discardLeftovers() }
    LaunchedEffect(state.loaded, state.order) { if (state.loaded && state.order == null) navState.pop() }
    if (!state.loaded || state.order == null) return GuideLoading()
    val actions =
        OrderActions(
            canEdit = access.canAdd,
            nameOf = bank::nameOf,
            onAddPages = { scope.launch { vm.addPages(scanner) } },
            onUnlock = vm::unlink,
            onDeletePage = vm::deletePage,
            onDeleteOrder = { vm.deleteOrder { navState.pop() } },
            onOpenEntry = { navState.push(Part2Destination.Entry(it)) },
            onBack = { navState.pop() },
            readPage = PageReader { id -> vm.page(id) { decoder.decode(it) } },
        )
    OrderScreen(state, actions)
}

@Composable
internal fun GuideShelfLevel(
    navState: GuideNavState,
    bank: BankViewModel,
    listState: LazyListState,
) {
    val state by bank.state.collectAsState()
    OrdersShelfScreen(
        shelf = state.shelf,
        onOpen = { navState.push(Part2Destination.Order(it)) },
        onLens = { orders -> if (!orders) navState.pop() },
        onBack = { navState.pop() },
        listState = listState,
    )
}
