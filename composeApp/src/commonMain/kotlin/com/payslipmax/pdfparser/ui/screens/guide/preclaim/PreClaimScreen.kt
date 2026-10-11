package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.GuideLoading
import com.payslipmax.pdfparser.ui.screens.guide.GuidePlatform
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/**
 * The worksheet. The back arrow is the only way up on iOS (a tab has no edge swipe); on Android system back does the same through the
 * Guide's `BackHandler`. Everything shown comes from [model]'s frame, the bundle's own text, or [PreClaimStrings].
 */
@Composable
internal fun PreClaimScreen(
    model: PreClaimViewModel,
    platform: GuidePlatform,
    onBack: () -> Unit,
    onOpenCard: (cardId: String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val ui by model.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = PreClaimStrings.screenTitle, subtitle = PreClaimStrings.screenSubtitle, onBack = onBack)
        when (val current = ui) {
            PreClaimUiState.Loading -> GuideLoading()
            PreClaimUiState.Unavailable -> PreClaimUnavailable(onRetry = model::retry)
            is PreClaimUiState.Ready -> PreClaimWorksheetList(current.worksheet, model, platform, onOpenCard, listState)
        }
    }
}

@Composable
private fun PreClaimWorksheetList(
    ws: PreClaimWorksheet,
    model: PreClaimViewModel,
    platform: GuidePlatform,
    onOpenCard: (String) -> Unit,
    listState: LazyListState,
) {
    val typed by model.typed.collectAsState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        if (typed.move == null) {
            preClaimMoveItems(ws, model)
        } else {
            preClaimChosenItems(ws, typed, model, platform, onOpenCard)
        }
    }
}

/** The order of a chosen move's sections: the officer, the answers, the journey, the estimates, then the portal form to copy from. */
private fun LazyListScope.preClaimChosenItems(
    ws: PreClaimWorksheet,
    typed: PreClaimFormState,
    model: PreClaimViewModel,
    platform: GuidePlatform,
    onOpenCard: (String) -> Unit,
) {
    item(key = "move") { PreClaimMoveHeader(ws.moveTitles[typed.move?.key].orEmpty(), onChange = model::backToMoves) }
    preClaimProfileItems(ws, typed, model)
    preClaimQuestionItems(ws, typed, model)
    preClaimJourneyItems(ws, typed, model, onOpenCard)
    preClaimEstimateItems(ws)
    preClaimFieldItems(ws, platform)
    preClaimDocumentItems(ws)
    preClaimNoteItems(ws)
}

/** The move chooser: one button per move the bundle ships, titled by the bundle. */
private fun LazyListScope.preClaimMoveItems(
    ws: PreClaimWorksheet,
    model: PreClaimViewModel,
) {
    item(key = "choose") { Text(PreClaimStrings.chooseMove, style = MaterialTheme.typography.titleMedium) }
    ws.availableMoves.forEach { move ->
        item(key = "move-${move.key}") {
            Button(onClick = { model.chooseMove(move) }, modifier = Modifier.fillMaxWidth()) { Text(ws.moveTitles[move.key].orEmpty()) }
        }
    }
}

@Composable
private fun PreClaimMoveHeader(
    title: String,
    onChange: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onChange) { Text(PreClaimStrings.changeMove) }
    }
}

/** One message with a Retry that goes to the Guide's own retry; a bundle with no worksheet reads the same. */
@Composable
internal fun PreClaimUnavailable(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AppDimensions.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(PreClaimStrings.unavailableTitle, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(PreClaimStrings.unavailableBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(GuideStrings.retry) }
    }
}
