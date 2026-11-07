package com.payslipmax.pdfparser.guide.model

import kotlinx.serialization.Serializable

/** One dated entry of the change log (`authoring/changes.txt`): what changed in the Guide on [date] (YYYY-MM-DD). */
@Serializable
data class GuideChange(
    val date: String,
    val items: List<GuideChangeItem>,
)

/** One line of a change-log entry, with the cards it concerns (may be empty). Content, not UI copy. */
@Serializable
data class GuideChangeItem(
    val text: String,
    val cards: List<String> = emptyList(),
)
