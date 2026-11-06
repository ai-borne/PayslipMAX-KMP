package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.HubStatus
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2CodeScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2Entry
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2HomeScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2RibbonScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankNoticeHost
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.part2Entry
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The Guide's side of Learn about Part II. [entry] is what the Guide Home row follows (HIDDEN while the dark-launch flag is off or the
 * bundle ships no Part II section, so nothing is hidden, it is absent). [model] is the one Learn model, null exactly when the entry is hidden.
 */
internal class Part2Host(
    val entry: Part2Entry,
    val model: Part2LookupViewModel?,
    val bank: BankViewModel? = null,
    val access: BankAccess = BankAccess.Locked,
) {
    /** Opens the Part II Orders hub from Guide Home. */
    fun openHub(navState: GuideNavState) {
        navState.push(Part2Destination.Hub)
    }

    /** Opens the Orders Bank from the hub on [filter]: the whole list for the card, the chip's own list for the chip, never a filter left over from before. */
    fun openBank(
        navState: GuideNavState,
        filter: BankFilter = BankFilter.ALL,
    ) {
        bank?.setFilter(filter)
        navState.push(Part2Destination.Bank)
    }

    /** Opens Learn from the hub with an empty search. */
    fun open(navState: GuideNavState) {
        model?.clear()
        navState.push(Part2Destination.Home)
    }
}

/**
 * Builds the [Part2Host]. A null [factory] is the flag being off, and then the model is never made. The model is also handed to
 * [navState], which sits above the lock screen, so a lock can drop the typed search (see [GuideNavState.discardTransientForms]).
 */
@Composable
internal fun rememberPart2Host(
    factory: (() -> Part2LookupViewModel)?,
    bankFactory: (() -> BankViewModel)?,
    access: BankAccess,
    bundle: GuideBundle,
    navState: GuideNavState,
): Part2Host {
    val entry = part2Entry(enabled = factory != null, bundle = bundle)
    val model = remember(entry) { if (entry == Part2Entry.OPEN) factory?.invoke() else null }
    val bank = remember(entry) { if (entry == Part2Entry.OPEN) bankFactory?.invoke() else null }
    SideEffect {
        navState.part2Lookup = model
        navState.part2Bank = bank
    }
    return Part2Host(entry, model, bank, access)
}

/** The Part II level on top, with the Bank's save and delete messages over its foot. */
@Composable
internal fun Part2LevelWithNotices(
    navState: GuideNavState,
    host: Part2Host,
    listState: LazyListState,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        GuidePart2Level(navState, host, listState)
        host.bank?.let { bank -> BankNoticeHost(bank, onAttach = { navState.push(Part2Destination.Attach(it)) }, modifier = Modifier.align(Alignment.BottomCenter)) }
    }
}

/** The Part II level on top of the stack. With the entry hidden (flag off after a restore) every Part II level is cut instead of showing anything. */
@Composable
internal fun GuidePart2Level(
    navState: GuideNavState,
    host: Part2Host,
    listState: LazyListState,
) {
    val model = host.model
    val destination = navState.current as? Part2Destination
    if (model == null || destination == null) {
        // Every Part II level goes at once: popping one would leave the next Part II level under it to be cut a frame later.
        LaunchedEffect(Unit) { while (navState.current is Part2Destination && navState.pop()) Unit }
        GuideLoading()
        return
    }
    val onBack: () -> Unit = { navState.pop() }
    val onOpenCode: (String) -> Unit = { navState.push(Part2Destination.Code(it)) }
    val onPickCasualty: (String) -> Unit = { openCasualty(navState, host, it) }
    val bank = host.bank
    when (destination) {
        Part2Destination.Hub -> Part2HubLevel(navState, host, onBack)
        Part2Destination.Home -> Part2HomeRoute(navState, model, host.access, listState, onPickCasualty)
        is Part2Destination.Ribbon -> model.ribbon(destination.ribbonId)?.let { Part2RibbonScreen(it, onBack, onPickCasualty, listState) } ?: GuideLoading()
        is Part2Destination.Code -> model.code(destination.code)?.let { Part2CodeScreen(it, onBack, onOpenCode, { addToBank(navState, host, destination.code) }, listState) } ?: GuideLoading()
        Part2Destination.Bank -> if (bank == null) GuideLoading() else GuideBankLevel(navState, host, bank, listState)
        is Part2Destination.Entry -> if (bank == null) GuideLoading() else GuideEntryLevel(navState, bank, destination.entryId, host.access)
        is Part2Destination.Attach -> if (bank == null) GuideLoading() else GuideAttachLevel(navState, host, bank, destination)
        is Part2Destination.Order -> if (bank == null) GuideLoading() else GuideOrderLevel(navState, bank, destination.orderId, host.access)
        Part2Destination.Shelf -> if (bank == null) GuideLoading() else GuideShelfLevel(navState, bank, listState)
    }
}

/** Where the Part II file stands, shared by the hub and Guide Home's tile: [HubStatus.Unknown] (so nothing is claimed) while there is no Bank or it has not been read. */
@Composable
internal fun rememberHubStatus(bank: BankViewModel?): State<HubStatus> = (bank?.hub ?: remember { MutableStateFlow(HubStatus.Unknown) }).collectAsState()

/** The hub: with the Bank present it shows where the file stands; without it (flag off) it is the two rows alone. */
@Composable
private fun Part2HubLevel(
    navState: GuideNavState,
    host: Part2Host,
    onBack: () -> Unit,
) {
    val bank = host.bank
    val status by rememberHubStatus(bank)
    val hero =
        bank?.let {
            HubHeroActions(
                canAdd = host.access.canAdd,
                onAdd = { host.open(navState) },
                onUnlock = host.access.onUnlock,
                onOpenMissing = { host.openBank(navState, BankFilter.NOT_ATTACHED) },
            )
        }
    Part2HubScreen(onBack, { host.open(navState) }, { host.openBank(navState) }, status, hero)
}

/**
 * A tap on a casualty in a search result or a ribbon. Opened from "Add a casualty" on the attach form, Learn is a picker for that order,
 * so the tap goes straight to the Add form (the form names the casualty; the casualty page stays one Back away from everywhere else).
 * An officer who may not add, or Learn opened any other way, still gets the casualty page.
 */
private fun openCasualty(
    navState: GuideNavState,
    host: Part2Host,
    code: String,
) {
    if (navState.startedFromAttach && host.access.canAdd) addToBank(navState, host, code) else navState.push(Part2Destination.Code(code))
}

/** The casualty page's Add: a new entry for that code when the officer may add, the upgrade offer when not. */
private fun addToBank(
    navState: GuideNavState,
    host: Part2Host,
    code: String,
) {
    if (!host.access.canAdd) return host.access.onUnlock()
    host.bank?.beginNew(code)?.let { navState.push(Part2Destination.Entry(it)) }
}

@Composable
private fun Part2HomeRoute(
    navState: GuideNavState,
    model: Part2LookupViewModel,
    access: BankAccess,
    listState: LazyListState,
    onOpenCode: (String) -> Unit,
) {
    val query by model.query.collectAsState()
    val ribbons = remember(model) { model.ribbons() }
    val result = remember(query, model) { model.search(query) }
    Part2HomeScreen(
        ribbons = ribbons,
        query = query,
        result = result,
        onQueryChange = model::onQueryChange,
        onOpenRibbon = { navState.push(Part2Destination.Ribbon(it)) },
        onOpenCode = onOpenCode,
        onBack = { navState.pop() },
        listState = listState,
        picking = navState.startedFromAttach && access.canAdd,
    )
}
