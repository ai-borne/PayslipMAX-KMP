package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

private const val SEPARATOR = '|'
private const val AREA = "area"
private const val CASE = "case"
private const val CARD = "card"
private const val SEARCH = "search"
private const val CHANGES = "changes"
private const val REMOVED_NOTES = "removednotes"
private const val SCROLL = "scroll"

/**
 * Saves the Guide stack across process death as plain strings, one per level above Home (the
 * `PayAuditSavedState` pattern), followed by one `scroll|level|index|offset` entry for each level not at its top.
 * Only ids and numbers are written, never card text or a search query. Restore runs before the bundle is loaded,
 * so here it only cuts at the first entry it cannot read; [GuideNavState.retainKnown] checks the ids against the
 * bundle once loading ends. A scroll entry that cannot be read is dropped on its own.
 */
val GuideNavStateSaver: Saver<GuideNavState, Any> =
    listSaver<GuideNavState, Any>(
        save = { state ->
            state.stack.mapNotNull(::encode) +
                state.savedScrolls.map { (level, scroll) -> listOf(SCROLL, level, scroll.index, scroll.offset).joinToString(SEPARATOR.toString()) }
        },
        restore = { saved ->
            val (scrollEntries, levels) = saved.map { it as? String }.partition { it?.startsWith("$SCROLL$SEPARATOR") == true }
            val decoded = levels.map { it?.let(::decode) }
            val scrolls = scrollEntries.mapNotNull { it?.let(::decodeScroll) }.toMap()
            GuideNavState(decoded.takeWhile { it != null }.filterNotNull(), scrolls)
        },
    )

private fun decodeScroll(entry: String): Pair<Int, GuideScroll>? {
    val numbers = entry.split(SEPARATOR).drop(1).map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
    if (numbers.size != 3) return null
    return numbers[0] to GuideScroll(numbers[1], numbers[2])
}

/** Home is implicit at the bottom of every stack, so it is never written. */
private fun encode(destination: GuideDestination): String? =
    when (destination) {
        GuideDestination.Home -> null
        GuideDestination.Search -> SEARCH
        GuideDestination.Changes -> CHANGES
        GuideDestination.RemovedNotes -> REMOVED_NOTES
        is GuideDestination.Area -> "$AREA$SEPARATOR${destination.areaId}"
        is GuideDestination.Case -> listOfNotNull(CASE, destination.caseId, destination.facet).joinToString(SEPARATOR.toString())
        is GuideDestination.Card -> "$CARD$SEPARATOR${destination.cardId}"
    }

private fun decode(entry: String): GuideDestination? {
    val parts = entry.split(SEPARATOR)
    val id = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
    return when (parts[0]) {
        SEARCH -> GuideDestination.Search.takeIf { parts.size == 1 }
        CHANGES -> GuideDestination.Changes.takeIf { parts.size == 1 }
        REMOVED_NOTES -> GuideDestination.RemovedNotes.takeIf { parts.size == 1 }
        AREA -> id?.takeIf { parts.size == 2 }?.let(GuideDestination::Area)
        CASE -> id?.takeIf { parts.size <= 3 }?.let { GuideDestination.Case(it, parts.getOrNull(2)?.takeIf(String::isNotEmpty)) }
        CARD -> id?.takeIf { parts.size == 2 }?.let(GuideDestination::Card)
        else -> null
    }
}
