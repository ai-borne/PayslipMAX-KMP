package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.payslipmax.pdfparser.guide.model.GuideBundle

/** A level of the Claim Guide's own stack. Ids stay strings (never enums), so new areas are data-only changes. */
sealed interface GuideDestination {
    data object Home : GuideDestination

    data class Area(val areaId: String) : GuideDestination

    /** [facet] is the chosen facet chip, kept here so it survives restore (used from phase E3). */
    data class Case(val caseId: String, val facet: String? = null) : GuideDestination

    data class Card(val cardId: String) : GuideDestination

    data object Search : GuideDestination
}

/**
 * The Guide's stack inside its tab. The app's [com.payslipmax.pdfparser.Screen] enum carries no arguments, so
 * the Guide keeps its own stack of ids rather than pushing app details. [GuideDestination.Home] is always at
 * the bottom; the stack changes only through [push], [pop] and [popToHome]. Backed by [mutableStateOf] so
 * Compose recomposes on every change.
 */
@Stable
class GuideNavState(initialAboveHome: List<GuideDestination> = emptyList()) {
    var stack: List<GuideDestination> by mutableStateOf(listOf(GuideDestination.Home) + initialAboveHome.filterNot { it == GuideDestination.Home })
        private set

    val current: GuideDestination get() = stack.last()

    /** True when back has a Guide level to return to; false at Home, so Android back leaves the tab. */
    val canPop: Boolean get() = stack.size > 1

    fun push(destination: GuideDestination) {
        if (destination == GuideDestination.Home) popToHome() else stack = stack + destination
    }

    /** @return true if a level was popped, false at Home (a no-op). */
    fun pop(): Boolean {
        if (!canPop) return false
        stack = stack.dropLast(1)
        return true
    }

    fun popToHome() {
        stack = listOf(GuideDestination.Home)
    }

    /**
     * Cuts the stack at the first destination [bundle] does not know, and everything above it (the
     * `AppNavStateSaver` rule: never rebuild an order the user did not create). Run once the bundle has loaded,
     * because a restore can happen before it does.
     */
    fun retainKnown(bundle: GuideBundle) {
        val kept = stack.takeWhile(bundle::knows)
        if (kept.size != stack.size) stack = kept.ifEmpty { listOf(GuideDestination.Home) }
    }
}

/** Whether this bundle still has the area, case, facet or card a destination names. */
fun GuideBundle.knows(destination: GuideDestination): Boolean =
    when (destination) {
        GuideDestination.Home, GuideDestination.Search -> true
        is GuideDestination.Area -> nav.any { it.id == destination.areaId }
        is GuideDestination.Case ->
            nav.any { area -> area.cases.any { it.id == destination.caseId } } &&
                (destination.facet == null || destination.facet in facets)
        is GuideDestination.Card -> cards.any { it.id == destination.cardId }
    }
