package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.GuideIndex

/**
 * The ways from a card to the rule before it or the rule in force, as card ids and dates only (nothing of the paid half, so
 * a locked card may hold it). [currentRuleId] is set only on a replaced card whose chain ends at another card;
 * [earlierRuleId] and [earlierBefore] only on a card that replaced an older rule.
 */
data class GuideCardHistory(
    val currentRuleId: String?,
    val earlierRuleId: String?,
    val earlierBefore: String?,
) {
    companion object {
        val None = GuideCardHistory(null, null, null)
    }
}

/** One card a change-log line points to; [title] is bundle content. */
data class GuideChangeCardLink(val cardId: String, val title: String)

/** One line of a change-log entry: the maintainer's [text] (bundle content, not UI copy) and the cards it concerns. */
data class GuideChangeLine(val text: String, val cards: List<GuideChangeCardLink>)

data class GuideChangeEntryContent(val date: String, val lines: List<GuideChangeLine>)

/** "What's new in the Guide": every shipped entry, newest first. */
data class GuideWhatsNewContent(val entries: List<GuideChangeEntryContent>)

internal fun GuideIndex.cardHistory(cardId: String): GuideCardHistory {
    val current = history.currentRule(cardId)?.id?.takeIf { it != cardId && history.isReplaced(cardId) }
    val earlier = history.earlierRules(cardId).firstOrNull()?.id
    return if (current == null && earlier == null) GuideCardHistory.None else GuideCardHistory(current, earlier, history.earlierBefore(cardId))
}

/** Null when the bundle has no change entry. A card id the bundle does not hold keeps its line but loses its link. */
internal fun GuideIndex.whatsNewContent(): GuideWhatsNewContent? {
    val entries =
        changeLog.entries.map { entry ->
            GuideChangeEntryContent(
                entry.date,
                entry.items.map { item ->
                    GuideChangeLine(item.text, item.cards.mapNotNull { id -> card(id)?.let { GuideChangeCardLink(it.id, it.title) } })
                },
            )
        }
    return GuideWhatsNewContent(entries).takeIf { entries.isNotEmpty() }
}
