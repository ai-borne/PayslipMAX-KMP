package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideChange
import com.payslipmax.pdfparser.guide.model.GuideChangeItem

/**
 * [SyntheticGuideBundle] after one rule change, because no real card is replaced yet: [OLD_CARD] is replaced by [NEW_CARD]
 * from [EFFECTIVE] (the old card keeps its data, leaves the case and has no home; the new card takes its place), and the
 * change log holds a latest entry on [LATEST] (naming both cards and [CLARIFIED_CARD]) and an older one on [OLDER_ENTRY].
 * Built by copy, so the plain synthetic bundle stays a bundle with no changes.
 */
object SyntheticGuideRuleChange {
    const val OLD_CARD = "RB-T9"
    const val NEW_CARD = "RB-T11"
    const val CLARIFIED_CARD = "RB-T10"
    const val OLDER_ENTRY_CARD = "RB-P1"
    const val OLD_EFFECTIVE = "2020-01-01"
    const val EFFECTIVE = "2026-11-15"
    const val LATEST = "2026-11-15"
    const val OLDER_ENTRY = "2026-06-01"
    const val LATEST_TEXT = "Home town LTC rule moved to the new pay commission"
    const val CLARIFIED_TEXT = "Home town LTC wording made clearer"
    const val OLDER_TEXT = "House rent note reworded"
    const val HOME_CASE = "ltc-home"

    fun bundle(): GuideBundle {
        val base = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle
        val old = base.cards.first { it.id == OLD_CARD }
        val replaced = old.copy(effective = OLD_EFFECTIVE, replacedBy = NEW_CARD, until = EFFECTIVE, nav = "")
        val successor =
            old.copy(
                id = NEW_CARD,
                title = "Synthetic card $NEW_CARD?",
                answer = "A one-line answer for $NEW_CARD.",
                key = listOf("A short key point for $NEW_CARD"),
                details = "Longer details for $NEW_CARD.",
                effective = EFFECTIVE,
            )
        val cards = base.cards.map { if (it.id == OLD_CARD) replaced else it } + successor
        val nav =
            base.nav.map { area ->
                area.copy(cases = area.cases.map { if (it.id == HOME_CASE) it.copy(cards = listOf(NEW_CARD, CLARIFIED_CARD)) else it })
            }
        val changes =
            listOf(
                GuideChange(LATEST, listOf(GuideChangeItem(LATEST_TEXT, listOf(NEW_CARD, OLD_CARD)), GuideChangeItem(CLARIFIED_TEXT, listOf(CLARIFIED_CARD)))),
                GuideChange(OLDER_ENTRY, listOf(GuideChangeItem(OLDER_TEXT, listOf(OLDER_ENTRY_CARD)))),
            )
        return base.copy(nav = nav, cards = cards, changes = changes)
    }
}
