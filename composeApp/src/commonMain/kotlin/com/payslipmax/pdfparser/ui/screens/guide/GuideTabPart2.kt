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
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2Entry
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2HomeScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2RibbonScreen
import com.payslipmax.pdfparser.ui.screens.guide.part2.part2Entry
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/**
 * The Guide's side of Part II Orders. [entry] is what the Guide Home row follows (HIDDEN while the dark-launch flag is off or the bundle
 * ships no Part II section, so nothing is hidden, it is absent). [model] is the one lookup model, null exactly when the entry is hidden.
 */
internal class Part2Host(
    val entry: Part2Entry,
    val model: Part2LookupViewModel?,
) {
    /** Opens Part II from Guide Home with an empty search. */
    fun open(navState: GuideNavState) {
        model?.clear()
        navState.push(Part2Destination.Home)
    }
}

/** Builds the [Part2Host]. A null [factory] is the flag being off, and then the model is never made. */
@Composable
internal fun rememberPart2Host(
    factory: (() -> Part2LookupViewModel)?,
    bundle: GuideBundle,
): Part2Host {
    val entry = part2Entry(enabled = factory != null, bundle = bundle)
    val model = remember(entry) { if (entry == Part2Entry.OPEN) factory?.invoke() else null }
    return Part2Host(entry, model)
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
        Part2Destination.Home -> Part2HomeRoute(navState, model, listState)
        is Part2Destination.Ribbon ->
            model.ribbon(destination.ribbonId)?.let { Part2RibbonScreen(it, onBack, { id -> navState.push(Part2Destination.Case(id)) }, listState) } ?: GuideLoading()
        is Part2Destination.Case -> model.case(destination.caseId)?.let { Part2CaseScreen(it, onBack, onOpenCode, listState) } ?: GuideLoading()
        is Part2Destination.Code -> model.code(destination.code)?.let { Part2CodeScreen(it, onBack, listState) } ?: GuideLoading()
    }
}

@Composable
private fun Part2HomeRoute(
    navState: GuideNavState,
    model: Part2LookupViewModel,
    listState: LazyListState,
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
        onOpenCode = { navState.push(Part2Destination.Code(it)) },
        onBack = { navState.pop() },
        listState = listState,
    )
}
