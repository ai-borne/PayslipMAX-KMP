package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.GuideIndex
import com.payslipmax.pdfparser.guide.domain.GuideShareLabels
import com.payslipmax.pdfparser.guide.domain.GuideShareParts
import com.payslipmax.pdfparser.guide.domain.GuideShareText
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings

private val ShareLabels =
    GuideShareLabels(
        header = GuideStrings.shareHeader,
        answer = GuideStrings.shareAnswer,
        keyPoints = GuideStrings.shareKeyPoints,
        attach = GuideStrings.shareAttach,
        watchOut = GuideStrings.shareWatchOut,
        authority = GuideStrings.shareAuthority,
        unverified = GuideStrings.shareUnverified,
        replacedOn = GuideMaintenanceStrings.shareReplacedOn,
        replacedApplies = GuideMaintenanceStrings.shareReplacedApplies,
    )

/**
 * The claim note for this card, or null for a locked card (it holds no key points or cite to share). Built from the
 * card's public text and cite only: the personal figure and details are not in [GuideShareParts].
 */
internal fun GuideCardContent.shareNote(): String? =
    full?.let {
        GuideShareText.build(GuideShareParts(title, answer, it.body.key, it.body.attach, it.body.watch, it.cite, trust.unverified, trust.replacedUntil?.let(GuideMaintenanceStrings::day)), ShareLabels)
    }

/** Guide Home's pinned rows, in the order given; an id the bundle does not hold is skipped. */
internal fun GuideIndex.pinnedRows(cardIds: List<String>): List<GuideFeedRow> =
    cardIds.mapNotNull { id ->
        card(id)?.let { GuideFeedRow(it.id, it.title, it.answer, bundle.facets[it.facet].orEmpty(), alsoHomeTitle = null, trust = trust(it)) }
    }
