package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** How long after the main content appears the Guide bundle starts loading, so it never competes with the first screen. */
internal const val GUIDE_WARM_UP_DELAY_MS = 1_500L

/**
 * Loads the Guide in the background shortly after launch, so the first open of the tab shows the tiles, not a spinner.
 * The parse itself runs off the main thread (see `LazyGuideRepository`); a failure shows in the tab with retry, as before.
 */
@Composable
internal fun GuideWarmUp(viewModel: GuideViewModel = koinInject()) {
    LaunchedEffect(viewModel) { warmUpGuide(viewModel) }
}

internal suspend fun warmUpGuide(
    viewModel: GuideViewModel,
    startDelayMs: Long = GUIDE_WARM_UP_DELAY_MS,
) {
    delay(startDelayMs)
    viewModel.load()
}
