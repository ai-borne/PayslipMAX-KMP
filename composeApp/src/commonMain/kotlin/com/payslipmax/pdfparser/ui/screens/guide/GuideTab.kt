package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import org.koin.compose.koinInject

/**
 * The Guide tab's root. Loads the bundle on first open (never at launch), shows loading or an error with retry,
 * and otherwise draws the top of [navState] inline, so the bottom bar stays on both platforms. [navState] is
 * hoisted to `App`, so switching tabs and locking the app keep the user's place. [access] decides whether a card's
 * paid half and the full-text search are open.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
fun GuideTab(
    navState: GuideNavState,
    access: GuideAccess,
    viewModel: GuideViewModel = koinInject(),
    searchViewModel: GuideSearchViewModel = koinInject(),
) {
    LaunchedEffect(viewModel) { viewModel.load() }
    val state by viewModel.uiState.collectAsState()
    when (val current = state) {
        GuideUiState.Loading -> GuideLoading()
        is GuideUiState.Failed -> GuideLoadFailed(onRetry = viewModel::retry)
        is GuideUiState.Ready -> {
            // A stack restored before the bundle loaded is checked here, once the ids can be.
            LaunchedEffect(current.bundle) { navState.retainKnown(current.bundle) }
            // Android back pops the Guide stack before it leaves the tab; at Guide Home it stays disabled.
            BackHandler(enabled = navState.canPop) { navState.pop() }
            GuideDestinationContent(navState, current, access, viewModel, searchViewModel)
        }
    }
}

@Composable
private fun GuideDestinationContent(
    navState: GuideNavState,
    ready: GuideUiState.Ready,
    access: GuideAccess,
    viewModel: GuideViewModel,
    searchViewModel: GuideSearchViewModel,
) {
    val onBack: () -> Unit = { navState.pop() }
    when (val destination = navState.current) {
        GuideDestination.Home ->
            GuideHomeScreen(
                ready.areas,
                onOpenArea = { navState.push(GuideDestination.Area(it)) },
                // Each visit to search starts empty; coming back from a card (a pop) does not pass through here.
                onOpenSearch = {
                    searchViewModel.clear()
                    navState.push(GuideDestination.Search)
                },
            )
        is GuideDestination.Area ->
            viewModel.area(destination.areaId)?.let { area ->
                GuideAreaScreen(area, onBack, onOpenCase = { navState.push(GuideDestination.Case(it)) }, rememberGuideListState(navState))
            } ?: GuideLoading()
        is GuideDestination.Case -> GuideFeedRoute(navState, destination, viewModel)
        is GuideDestination.Card ->
            viewModel.card(destination.cardId, access.isUnlocked)?.let { card ->
                GuideCardScreen(card, viewModel.crumbs(navState.stack), navState::upTo, onBack, access.onUnlock, rememberGuideListState(navState))
            } ?: GuideLoading()
        GuideDestination.Search -> GuideSearchRoute(navState, access, searchViewModel, onBack)
    }
}

@Composable
private fun GuideSearchRoute(
    navState: GuideNavState,
    access: GuideAccess,
    searchViewModel: GuideSearchViewModel,
    onBack: () -> Unit,
) {
    // Free users search titles and rule numbers only; the model starts locked, so this can only widen the search.
    LaunchedEffect(access.isUnlocked) { searchViewModel.setUnlocked(access.isUnlocked) }
    val query by searchViewModel.query.collectAsState()
    val state by searchViewModel.state.collectAsState()
    GuideSearchScreen(
        query = query,
        state = state,
        onQueryChange = searchViewModel::onQueryChange,
        onOpenCard = { navState.push(GuideDestination.Card(it)) },
        onBack = onBack,
        listState = rememberGuideListState(navState),
    )
}

@Composable
private fun GuideFeedRoute(
    navState: GuideNavState,
    destination: GuideDestination.Case,
    viewModel: GuideViewModel,
) {
    val feed = remember(destination) { viewModel.feed(destination.caseId, destination.facet) } ?: return GuideLoading()
    GuideFeedScreen(
        feed = feed,
        crumbs = viewModel.crumbs(navState.stack),
        onCrumb = navState::upTo,
        onBack = { navState.pop() },
        onSelectFacet = navState::selectFacet,
        onOpenCard = { navState.push(GuideDestination.Card(it)) },
        listState = rememberGuideListState(navState),
    )
}

/**
 * The current level's list position, restored from [navState] (which outlives this tab's composition) and written
 * back as the user scrolls. A new level, or a new facet, starts from its own saved place or the top.
 */
@Composable
private fun rememberGuideListState(navState: GuideNavState): LazyListState {
    val level = navState.stack.lastIndex
    val destination = navState.current
    val listState = remember(level, destination) { navState.currentScroll.let { LazyListState(it.index, it.offset) } }
    LaunchedEffect(listState) {
        snapshotFlow { GuideScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .collect { navState.saveScroll(it, level) }
    }
    return listState
}

@Composable
private fun GuideLoading() {
    Column(
        modifier = Modifier.fillMaxSize().semantics { contentDescription = GuideStrings.loading },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun GuideLoadFailed(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AppDimensions.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(GuideStrings.loadFailedTitle, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            GuideStrings.loadFailedBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text(GuideStrings.retry) }
    }
}
