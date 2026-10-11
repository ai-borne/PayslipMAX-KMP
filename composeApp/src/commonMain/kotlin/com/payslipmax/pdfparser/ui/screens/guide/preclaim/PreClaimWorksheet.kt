package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideOptionGroup
import com.payslipmax.pdfparser.guide.model.GuidePreClaimField
import com.payslipmax.pdfparser.guide.model.GuidePreClaimLimits
import com.payslipmax.pdfparser.guide.model.GuidePreClaimMove
import com.payslipmax.pdfparser.guide.model.GuideUploadRule
import com.payslipmax.pdfparser.guide.preclaim.PreClaimNote
import com.payslipmax.pdfparser.guide.preclaim.PreClaimOutcome

/** What the worksheet screen draws. Loading and failure are the Guide's; [Unavailable] also covers a bundle that ships no worksheet. */
sealed interface PreClaimUiState {
    data object Loading : PreClaimUiState

    data object Unavailable : PreClaimUiState

    data class Ready(val worksheet: PreClaimWorksheet) : PreClaimUiState
}

/**
 * One frame of the worksheet. [sheet] is the chosen move's fields, documents and notes straight from the bundle (all text is
 * content); [questions] are its basic-information fields; [estimates] are the calculators' outcomes in element order, never summed
 * and never a total (plan 21, F1). [copyValues] holds, per field id, the plain value the officer may copy into that portal box,
 * one box at a time: there is no copy-all.
 */
data class PreClaimWorksheet(
    val availableMoves: List<PreClaimMoveType>,
    /** Each available move's title from the bundle (content, not app copy), by move key. */
    val moveTitles: Map<String, String>,
    val form: PreClaimFormState,
    val sheet: GuidePreClaimMove?,
    val questions: List<GuidePreClaimField>,
    val options: Map<String, GuideOptionGroup>,
    val upload: GuideUploadRule,
    val limits: GuidePreClaimLimits,
    val profile: PreClaimProfileText,
    val levelChoices: List<String>,
    val stateChoices: List<String>,
    val estimates: List<PreClaimOutcome>,
    /** Inclusive day count of the dates on a temporary-duty move; null when they are not usable. */
    val days: Int?,
    /** Nights as shown: the typed number, else the day count. */
    val nightsText: String,
    val notes: Set<PreClaimNote>,
    val copyValues: Map<String, String>,
    /** The Guide card the chosen leave-travel rule links to, if one is chosen. */
    val ruleCardId: String?,
)
