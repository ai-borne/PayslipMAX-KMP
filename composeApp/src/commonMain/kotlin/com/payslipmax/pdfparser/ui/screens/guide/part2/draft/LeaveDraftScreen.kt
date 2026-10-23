package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListSpacing
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimQuietText
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/**
 * The leave form: one section per occurrence, each asking what its orders print, then a way to add another occurrence, then the way on
 * to the slip. Stateless: it draws [state] and calls [actions]; the answers are saved by the model as they are typed.
 */
@Composable
internal fun LeaveDraftScreen(
    state: LeaveDraftUiState,
    actions: LeaveDraftActions,
    onSeeSlip: () -> Unit,
    onBack: () -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = Part2Strings.formTitle, subtitle = Part2Strings.formSubtitle, onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Part2ListSpacing) {
            items(state.sections, key = { it.index }) { RequestSectionView(it, actions) }
            item(key = "add") { AddOccurrenceView(state.addable, actions.onAdd) }
            item(key = "footer") { LeaveDraftFooter(state, onSeeSlip) }
        }
    }
}

/** What is still needed, whether the draft could be saved, and the way on to the slip (enabled once the answers are complete). */
@Composable
private fun LeaveDraftFooter(
    state: LeaveDraftUiState,
    onSeeSlip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (state.saveFailed) Text(Part2Strings.saveFailed, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        if (state.missing.isNotEmpty()) PreClaimQuietText(Part2Strings.stillNeeded(state.missing))
        Button(onClick = onSeeSlip, enabled = state.slip != null, modifier = Modifier.fillMaxWidth()) { Text(Part2Strings.seeSlip) }
    }
}
