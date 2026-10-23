package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.ui.components.AccentStripeCard
import com.payslipmax.pdfparser.ui.components.AccentStripeCardShape
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2CaseScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2CodeScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2DraftEntry
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2Entry
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2HomeScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2RibbonScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.DraftsUiState
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftActions
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftList
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2Models
import com.payslipmax.pdfparser.ui.screens.guide.part2.part2DraftEntry
import com.payslipmax.pdfparser.ui.screens.guide.part2.part2Entry
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The Guide's side of Part II Orders. [entry] is what the Guide Home row follows (HIDDEN while the dark-launch flag is off or the bundle
 * ships no Part II section, so nothing is hidden, it is absent). [models] are the lookup, the drafts list and the maker of one draft,
 * null exactly when the entry is hidden. [draftEntry] is what drafting shows this user (see [part2DraftEntry]); [onUnlock] opens the upgrade sheet.
 */
internal class Part2Host(
    val entry: Part2Entry,
    val models: Part2Models?,
    val draftEntry: Part2DraftEntry,
    val onUnlock: () -> Unit,
) {
    val model: Part2LookupViewModel? get() = models?.lookup

    /** Opens Part II from Guide Home with an empty search. */
    fun open(navState: GuideNavState) {
        model?.clear()
        navState.push(Part2Destination.Home)
    }

    /** Starts a draft for [code]; a user who may not draft goes to the upgrade panel and no draft model is made. */
    fun startDraft(
        navState: GuideNavState,
        code: String,
    ) {
        if (draftEntry != Part2DraftEntry.OPEN) return navState.push(Part2Destination.Locked)
        val draft = models?.drafting?.start(code) ?: return
        navState.discardPart2Draft()
        navState.part2Draft = draft
        navState.push(Part2Destination.Draft(draft.id))
    }
}

/** Builds the [Part2Host]. A null [factory] is the flag being off, and then the models are never made. */
@Composable
internal fun rememberPart2Host(
    factory: (() -> Part2Models)?,
    bundle: GuideBundle,
    access: GuideAccess,
): Part2Host {
    val entry = part2Entry(enabled = factory != null, bundle = bundle)
    val models = remember(entry) { if (entry == Part2Entry.OPEN) factory?.invoke() else null }
    val none = remember { MutableStateFlow(DraftsUiState.Empty) }
    val drafts by (models?.drafts?.state ?: none).collectAsState()
    return Part2Host(entry, models, part2DraftEntry(access.part2Unlocked, drafts.hasAny), access.onUnlock)
}

/** The Guide Home row that opens Part II. */
@Composable
internal fun Part2EntryRow(onClick: () -> Unit) {
    AccentStripeCard(
        accent = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(AccentStripeCardShape).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Decoration(Part2Strings.entryEmoji, AppDimensions.TextSizeHuge)
            Column(modifier = Modifier.weight(1f)) {
                Text(Part2Strings.entryTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(Part2Strings.entrySubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Decoration(AppStrings.rowArrow, AppDimensions.TextSizeLarge, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The Part II level on top of the stack. With the entry hidden (flag off after a restore) the stack is cut back instead of showing anything. */
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
    when (destination) {
        Part2Destination.Home -> Part2HomeRoute(navState, model, host.models?.drafts, listState)
        is Part2Destination.Ribbon ->
            model.ribbon(destination.ribbonId)?.let { Part2RibbonScreen(it, onBack, { id -> navState.push(Part2Destination.Case(id)) }, listState) } ?: GuideLoading()
        is Part2Destination.Case -> model.case(destination.caseId)?.let { Part2CaseScreen(it, onBack, onOpenCode, Part2DraftActions(host.draftEntry) { code -> host.startDraft(navState, code) }, listState) } ?: GuideLoading()
        is Part2Destination.Code -> model.code(destination.code)?.let { Part2CodeScreen(it, onBack, listState) } ?: GuideLoading()
        Part2Destination.Locked, is Part2Destination.Draft, is Part2Destination.Slip -> Part2DraftLevel(navState, host, destination, listState)
    }
}

@Composable
private fun Part2HomeRoute(
    navState: GuideNavState,
    model: Part2LookupViewModel,
    drafts: Part2DraftsViewModel?,
    listState: LazyListState,
) {
    val none = remember { MutableStateFlow(DraftsUiState.Empty) }
    val saved by (drafts?.state ?: none).collectAsState()
    val query by model.query.collectAsState()
    val ribbons = remember(model) { model.ribbons() }
    val result = remember(query, model) { model.search(query) }
    Part2HomeScreen(
        ribbons = ribbons,
        query = query,
        result = result,
        onQueryChange = model::onQueryChange,
        onOpenRibbon = { navState.push(Part2Destination.Ribbon(it)) },
        onOpenCode = { navState.push(Part2Destination.Code(it)) },
        onBack = { navState.pop() },
        drafts = Part2DraftList(saved, onOpen = { navState.push(Part2Destination.Slip(it)) }, onDelete = { drafts?.delete(it) }),
        listState = listState,
    )
}
