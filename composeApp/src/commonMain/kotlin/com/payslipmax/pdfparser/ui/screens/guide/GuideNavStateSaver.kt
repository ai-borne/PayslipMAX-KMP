package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

private const val SEPARATOR = '|'
private const val AREA = "area"
private const val CASE = "case"
private const val CARD = "card"
private const val SEARCH = "search"

/**
 * Saves the Guide stack across process death as plain strings, one per level above Home (the
 * `PayAuditSavedState` pattern). Only ids are written, never card text or a search query. Restore runs before
 * the bundle is loaded, so here it only cuts at the first entry it cannot read; [GuideNavState.retainKnown]
 * checks the ids against the bundle once loading ends.
 */
val GuideNavStateSaver: Saver<GuideNavState, Any> =
    listSaver<GuideNavState, Any>(
        save = { state -> state.stack.mapNotNull(::encode) },
        restore = { saved ->
            val decoded = saved.map { (it as? String)?.let(::decode) }
            GuideNavState(decoded.takeWhile { it != null }.filterNotNull())
        },
    )

/** Home is implicit at the bottom of every stack, so it is never written. */
private fun encode(destination: GuideDestination): String? =
    when (destination) {
        GuideDestination.Home -> null
        GuideDestination.Search -> SEARCH
        is GuideDestination.Area -> "$AREA$SEPARATOR${destination.areaId}"
        is GuideDestination.Case -> listOfNotNull(CASE, destination.caseId, destination.facet).joinToString(SEPARATOR.toString())
        is GuideDestination.Card -> "$CARD$SEPARATOR${destination.cardId}"
    }

private fun decode(entry: String): GuideDestination? {
    val parts = entry.split(SEPARATOR)
    val id = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
    return when (parts[0]) {
        SEARCH -> GuideDestination.Search.takeIf { parts.size == 1 }
        AREA -> id?.takeIf { parts.size == 2 }?.let(GuideDestination::Area)
        CASE -> id?.takeIf { parts.size <= 3 }?.let { GuideDestination.Case(it, parts.getOrNull(2)?.takeIf(String::isNotEmpty)) }
        CARD -> id?.takeIf { parts.size == 2 }?.let(GuideDestination::Card)
        else -> null
    }
}
