package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.launch

private const val COUNT_ITEM_KEY = "count"

/**
 * Search by word or rule number. The keyboard opens only on a fresh, empty search; coming back from a card keeps the
 * query, the results and the scroll place ([listState] is restored from the Guide stack) and leaves the keyboard shut.
 */
@Composable
internal fun GuideSearchScreen(
    query: String,
    state: GuideSearchState,
    onQueryChange: (String) -> Unit,
    onOpenCard: (cardId: String) -> Unit,
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    notedCards: Set<String> = emptySet(),
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = GuideStrings.searchTitle, onBack = onBack)
        GuideSearchField(
            query = query,
            // A new query starts at the top, not wherever the last results were scrolled to.
            onQueryChange = {
                onQueryChange(it)
                scope.launch { listState.scrollToItem(0) }
            },
        )
        GuideSearchBody(state, onOpenCard, listState, notedCards)
    }
}

@Composable
private fun GuideSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { if (query.isEmpty()) focus.requestFocus() }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(GuideStrings.searchPlaceholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = GuideStrings.searchClear) }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimensions.PaddingMedium).focusRequester(focus),
    )
}

@Composable
private fun GuideSearchBody(
    state: GuideSearchState,
    onOpenCard: (cardId: String) -> Unit,
    listState: LazyListState,
    notedCards: Set<String>,
) {
    when (state) {
        GuideSearchState.Idle -> GuideSearchMessage(GuideStrings.searchHint)
        GuideSearchState.TooShort -> GuideSearchMessage(GuideStrings.searchTooShort)
        is GuideSearchState.Results ->
            if (state.rows.isEmpty()) {
                GuideSearchMessage(GuideStrings.searchNone)
            } else {
                GuideSearchResults(state.rows, onOpenCard, listState, notedCards)
            }
    }
}

@Composable
private fun GuideSearchResults(
    rows: List<GuideSearchRow>,
    onOpenCard: (cardId: String) -> Unit,
    listState: LazyListState,
    notedCards: Set<String>,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        item(key = COUNT_ITEM_KEY) {
            Text(
                GuideStrings.resultCount(rows.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Read out when the count changes, so a screen-reader user hears the effect of typing.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        // The card-row pill carries the case the card lives in; a result is never an "also relevant here" link.
        items(rows, key = { it.cardId }) { row ->
            val mark =
                when {
                    row.matchedInNote -> GuideNoteMark.MATCHED_IN_NOTE
                    row.cardId in notedCards -> GuideNoteMark.HAS_NOTE
                    else -> GuideNoteMark.NONE
                }
            GuideCardRowView(GuideFeedRow(row.cardId, row.title, row.answer, row.caseTitle, alsoHomeTitle = null, trust = row.trust), onClick = { onOpenCard(row.cardId) }, noteMark = mark)
        }
    }
}

@Composable
private fun GuideSearchMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(AppDimensions.PaddingMedium),
    )
}
