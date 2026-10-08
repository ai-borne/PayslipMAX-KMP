package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.payslipmax.pdfparser.guide.domain.GuideProfile

/**
 * The officer's profile for one card, read from the stored payslips only while that card is on screen, unlocked and has a
 * "your figure" line. A locked card, or one with no figure, never touches the payslips, and the profile is dropped as soon
 * as the card leaves the screen.
 */
@Composable
internal fun rememberGuideProfile(
    viewModel: GuideViewModel,
    cardId: String,
    unlocked: Boolean,
): GuideProfile? {
    val wanted = unlocked && viewModel.hasFigure(cardId)
    return produceState<GuideProfile?>(initialValue = null, viewModel, cardId, wanted) {
        if (wanted) viewModel.profileFlow().collect { value = it } else value = null
    }.value
}
