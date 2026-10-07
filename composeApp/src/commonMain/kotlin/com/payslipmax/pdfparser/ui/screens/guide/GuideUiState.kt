package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.model.GuideArea
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideCase

/** The Guide tab's load state. Every screen below Home derives its content from [Ready.bundle]. */
sealed interface GuideUiState {
    data object Loading : GuideUiState

    /** Shown with a retry button, never a blank tab. */
    data class Failed(val error: GuideLoadError) : GuideUiState

    data class Ready(val bundle: GuideBundle, val areas: List<GuideAreaTile>) : GuideUiState
}

/** An area tile on Guide Home. Titles are content from the bundle, not UI copy. */
data class GuideAreaTile(val id: String, val title: String, val caseCount: Int)

/** A case tile: [subtitle] is the rule-number line from the bundle (may be empty). */
data class GuideCaseTile(val id: String, val title: String, val subtitle: String, val cardCount: Int)

data class GuideAreaContent(val id: String, val title: String, val cases: List<GuideCaseTile>)

internal fun GuideArea.toTile(): GuideAreaTile = GuideAreaTile(id, title, cases.size)

internal fun GuideArea.toContent(): GuideAreaContent = GuideAreaContent(id, title, cases.map(GuideCase::toTile))

/** The count is every card the case's feed lists: those homed here plus the "also relevant here" links. */
internal fun GuideCase.toTile(): GuideCaseTile = GuideCaseTile(id, title, sub, cards.size + also.size)

internal fun GuideBundle.toReady(): GuideUiState.Ready = GuideUiState.Ready(this, nav.map(GuideArea::toTile))
