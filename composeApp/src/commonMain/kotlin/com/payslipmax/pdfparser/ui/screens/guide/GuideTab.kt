package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
 * hoisted to `App`, so switching tabs and locking the app keep the user's place.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
fun GuideTab(
    navState: GuideNavState,
    viewModel: GuideViewModel = koinInject(),
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
            GuideDestinationContent(navState, current, viewModel)
        }
    }
}

@Composable
private fun GuideDestinationContent(
    navState: GuideNavState,
    ready: GuideUiState.Ready,
    viewModel: GuideViewModel,
) {
    val onBack: () -> Unit = { navState.pop() }
    when (val destination = navState.current) {
        GuideDestination.Home -> GuideHomeScreen(ready.areas, onOpenArea = { navState.push(GuideDestination.Area(it)) })
        is GuideDestination.Area ->
            viewModel.area(destination.areaId)?.let { area ->
                GuideAreaScreen(area, onBack = onBack, onOpenCase = { navState.push(GuideDestination.Case(it)) })
            } ?: GuideLoading()
        is GuideDestination.Case ->
            viewModel.case(destination.caseId)?.let { GuidePlaceholderScreen(it.title, it.sub, onBack) } ?: GuideLoading()
        is GuideDestination.Card ->
            viewModel.card(destination.cardId)?.let { GuidePlaceholderScreen(it.title, null, onBack) } ?: GuideLoading()
        GuideDestination.Search -> GuidePlaceholderScreen(GuideStrings.searchTitle, null, onBack)
    }
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
